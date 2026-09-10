package sl.selene.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CommandManager {
   private static final CommandManager INSTANCE = new CommandManager();
   private final Map<String, CommandManager.RegisteredCommand> commandsByName = new LinkedHashMap<>();
   private final Map<String, CommandManager.RegisteredCommand> aliases = new LinkedHashMap<>();

   private CommandManager() {
   }

   public static CommandManager getInstance() {
      return INSTANCE;
   }

   public synchronized void register(Command command) {
      Objects.requireNonNull(command, "command");
      String canonicalName = this.normalize(command.name());
      if (this.commandsByName.containsKey(canonicalName)) {
         throw new IllegalStateException("Command '" + command.name() + "' already registered");
      } else {
         List<String> aliasList = command.aliases();
         if (aliasList != null && !aliasList.isEmpty()) {
            List<String> normalizedAliases = new ArrayList<>(aliasList.size());

            for (String alias : aliasList) {
               if (alias == null || alias.isBlank()) {
                  throw new IllegalArgumentException("Command '" + command.name() + "' contains invalid alias");
               }

               String normalizedAlias = this.normalize(alias);
               if (this.aliases.containsKey(normalizedAlias)) {
                  throw new IllegalStateException("Alias '" + alias + "' already bound to command '" + this.aliases.get(normalizedAlias).command().name() + "'");
               }

               normalizedAliases.add(alias);
            }

            CommandManager.RegisteredCommand registration = new CommandManager.RegisteredCommand(command, List.copyOf(normalizedAliases));
            this.commandsByName.put(canonicalName, registration);

            for (String alias : normalizedAliases) {
               this.aliases.put(this.normalize(alias), registration);
            }
         } else {
            throw new IllegalArgumentException("Command '" + command.name() + "' must declare at least one alias");
         }
      }
   }

   public synchronized Optional<CommandManager.RegisteredCommand> findByAlias(String alias) {
      return alias == null ? Optional.empty() : Optional.ofNullable(this.aliases.get(this.normalize(alias)));
   }

   public synchronized CommandManager.RegisteredCommand getByName(String name) {
      return name == null ? null : this.commandsByName.get(this.normalize(name));
   }

   public synchronized List<CommandManager.RegisteredCommand> getRegisteredCommands() {
      return List.copyOf(this.commandsByName.values());
   }

   public boolean handleMessage(String message) {
      if (message == null) {
         return false;
      } else {
         String trimmed = message.trim();
         if (trimmed.isEmpty()) {
            return false;
         } else {
            CommandManager.RegisteredCommand match = null;
            String matchedAlias = null;
            synchronized (this) {
               for (Entry<String, CommandManager.RegisteredCommand> entry : this.aliases.entrySet()) {
                  String normalizedAlias = entry.getKey();
                  CommandManager.RegisteredCommand registration = entry.getValue();
                  String rawAlias = registration.resolveAlias(normalizedAlias);
                  if (rawAlias != null && this.startsWithAlias(trimmed, rawAlias)) {
                     match = registration;
                     matchedAlias = rawAlias;
                     break;
                  }
               }
            }

            if (match != null && matchedAlias != null) {
               String arguments = trimmed.length() > matchedAlias.length() ? trimmed.substring(matchedAlias.length()).trim() : "";
               CommandContext context = new CommandContext(match.command(), matchedAlias, message, arguments);

               try {
                  match.command().execute(context, arguments);
               } catch (CommandException var12) {
                  context.sendError(var12.getMessage());
               }

               return true;
            } else {
               return false;
            }
         }
      }
   }

   private boolean startsWithAlias(String message, String alias) {
      if (message.length() < alias.length()) {
         return false;
      } else {
         return !message.regionMatches(true, 0, alias, 0, alias.length())
            ? false
            : message.length() == alias.length() || Character.isWhitespace(message.charAt(alias.length()));
      }
   }

   private String normalize(String value) {
      return value.toLowerCase(Locale.ROOT);
   }

   @Environment(EnvType.CLIENT)
   public record RegisteredCommand(Command command, List<String> aliases) {
      public RegisteredCommand(Command command, List<String> aliases) {
         command = Objects.requireNonNull(command, "command");
         aliases = aliases == null ? List.of() : List.copyOf(aliases);
         this.command = command;
         this.aliases = aliases;
      }

      public List<String> aliases() {
         return Collections.unmodifiableList(this.aliases);
      }

      public String resolveAlias(String normalizedAlias) {
         for (String alias : this.aliases) {
            if (alias.equalsIgnoreCase(normalizedAlias)) {
               return alias;
            }
         }

         return null;
      }
   }
}