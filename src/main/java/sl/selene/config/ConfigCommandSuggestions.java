package sl.selene.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;
import sl.selene.cfg.Config;
import sl.selene.cfg.ConfigManager;
import sl.selene.commands.suggestions.CommandSuggestionProvider;
import sl.selene.commands.suggestions.CommandSuggestions;

@Environment(EnvType.CLIENT)
public final class ConfigCommandSuggestions implements CommandSuggestionProvider {
   private static final ConfigCommandSuggestions INSTANCE = new ConfigCommandSuggestions();

   private ConfigCommandSuggestions() {
   }

   public static ConfigCommandSuggestions getInstance() {
      return INSTANCE;
   }

   @Override
   public List<String> aliases() {
      return ConfigCommand.getInstance().getCommandAliases();
   }

   @Override
   public boolean supportsInput(String input) {
      return findAlias(input) != null || ".".equals(input) || this.matchesAliasPrefix(input);
   }

   @Override
   public CommandSuggestions.SuggestionSet collect(String input) {
      ConfigCommandSuggestions.AliasMatch alias = findAlias(input);
      if (alias == null) {
         return null;
      } else {
         String argsPortion = input.substring(alias.aliasEnd());
         if (argsPortion.isEmpty()) {
            return CommandSuggestions.of(buildCommandEntries("", 0));
         } else {
            int leadingSpaces = countLeadingWhitespace(argsPortion);
            String trimmedArgs = argsPortion.substring(leadingSpaces);
            if (trimmedArgs.isEmpty()) {
               return CommandSuggestions.of(buildCommandEntries("", leadingSpaces));
            } else {
               String firstToken = nextToken(trimmedArgs);
               boolean hasAdditionalCharacters = trimmedArgs.length() > firstToken.length();
               if (!hasAdditionalCharacters) {
                  return CommandSuggestions.of(buildCommandEntries(firstToken, leadingSpaces));
               } else {
                  String afterFirstToken = trimmedArgs.substring(firstToken.length());
                  int spacesAfterFirstToken = countLeadingWhitespace(afterFirstToken);
                  if (spacesAfterFirstToken == 0) {
                     return CommandSuggestions.of(buildCommandEntries(firstToken, leadingSpaces));
                  } else {
                     String remaining = afterFirstToken.substring(spacesAfterFirstToken);
                     if (remaining.isEmpty()) {
                        return CommandSuggestions.of(buildArgumentEntries(firstToken, ""));
                     } else {
                        String secondToken = nextToken(remaining);
                        return remaining.length() > secondToken.length() ? null : CommandSuggestions.of(buildArgumentEntries(firstToken, secondToken));
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public List<CommandSuggestions.SuggestionEntry> collectAliasSuggestions(String input) {
      if (input == null) {
         return List.of();
      } else if (".".equals(input)) {
         return buildAllCommandEntries();
      } else {
         return !input.startsWith(".") ? List.of() : buildPartialAliasEntries(input);
      }
   }

   private static List<CommandSuggestions.SuggestionEntry> buildCommandEntries(String partialToken, int leadingSpaces) {
      String normalized = partialToken.toLowerCase(Locale.ROOT);
      Map<String, ConfigCommand.CommandMetadata> metadataMap = ConfigCommand.getInstance().getCommandMetadata();
      List<CommandSuggestions.SuggestionEntry> entries = new ArrayList<>();

      for (Entry<String, ConfigCommand.CommandMetadata> entry : metadataMap.entrySet()) {
         String command = entry.getKey();
         if (normalized.isEmpty() || command.startsWith(normalized)) {
            entries.add(createCommandSuggestion(partialToken, leadingSpaces, command, entry.getValue()));
         }
      }

      return entries;
   }

   private static List<CommandSuggestions.SuggestionEntry> buildAllCommandEntries() {
      List<String> aliases = ConfigCommand.getInstance().getCommandAliases();
      List<CommandSuggestions.SuggestionEntry> entries = new ArrayList<>();

      for (String alias : aliases) {
         entries.add(new CommandSuggestions.SuggestionEntry(alias, "Config command alias", alias.substring(1), false));
      }

      return entries;
   }

   private static List<CommandSuggestions.SuggestionEntry> buildPartialAliasEntries(String partialInput) {
      List<String> aliases = ConfigCommand.getInstance().getCommandAliases();
      List<CommandSuggestions.SuggestionEntry> entries = new ArrayList<>();

      for (String alias : aliases) {
         if (alias.startsWith(partialInput)) {
            String completion = alias.substring(partialInput.length());
            entries.add(new CommandSuggestions.SuggestionEntry(alias, "Config command alias", completion, false));
         }
      }

      return entries;
   }

   private static CommandSuggestions.SuggestionEntry createCommandSuggestion(
      String partialToken, int leadingSpaces, String command, ConfigCommand.CommandMetadata metadata
   ) {
      StringBuilder completion = new StringBuilder();
      if (partialToken.isEmpty() && leadingSpaces == 0) {
         completion.append(' ');
      }

      if (partialToken.isEmpty()) {
         completion.append(command);
      } else {
         completion.append(command.substring(partialToken.length()));
      }

      if (metadata.argumentType() != ConfigCommand.ArgumentType.NONE) {
         if (completion.length() == 0) {
            completion.append(' ');
         } else if (completion.charAt(completion.length() - 1) != ' ') {
            completion.append(' ');
         }
      }

      return new CommandSuggestions.SuggestionEntry(metadata.usage(), metadata.description(), normalizeCompletionSuffix(completion.toString()), false);
   }

   private static List<CommandSuggestions.SuggestionEntry> buildArgumentEntries(String commandToken, String partialArgument) {
      String command = resolveCommand(commandToken);
      if (command == null) {
         return List.of();
      } else {
         ConfigCommand.CommandMetadata metadata = ConfigCommand.getInstance().getCommandMetadata().get(command);
         if (metadata == null) {
            return List.of();
         } else {
            return switch (metadata.argumentType()) {
               case NONE -> List.of();
               case NEW_CONFIG_NAME -> buildNewConfigNameEntry(partialArgument);
               case EXISTING_CONFIG_NAME -> buildExistingConfigEntries(partialArgument);
            };
         }
      }
   }

   private static List<CommandSuggestions.SuggestionEntry> buildNewConfigNameEntry(String partialArgument) {
      return !partialArgument.isEmpty()
         ? List.of()
         : List.of(new CommandSuggestions.SuggestionEntry("<name>", "Enter a unique name for the new config", null, false));
   }

   private static List<CommandSuggestions.SuggestionEntry> buildExistingConfigEntries(String partialArgument) {
      if (Selene.get.configManager == null) {
         return List.of();
      } else {
         ConfigManager configManager = Selene.get.configManager;
         List<Config> configs = configManager.getContents();
         if (configs.isEmpty()) {
            return List.of();
         } else {
            String normalizedPartial = partialArgument.toLowerCase(Locale.ROOT);
            return configs.stream()
               .sorted(
                  Comparator.<Config, String>comparing(config -> config.getName().toLowerCase(Locale.ROOT))
                     .thenComparing(Comparator.comparing(Config::getName))
               )
               .map(Config::getName)
               .filter(name -> normalizedPartial.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(normalizedPartial))
               .map(name -> {
                  String description = "Saved config";
                  return new CommandSuggestions.SuggestionEntry(name, description, normalizeCompletionSuffix(name.substring(partialArgument.length())), false);
               })
               .toList();
         }
      }
   }

   private static ConfigCommandSuggestions.AliasMatch findAlias(String input) {
      if (input == null) {
         return null;
      } else {
         for (String alias : ConfigCommand.getInstance().getCommandAliases()) {
            if (input.length() >= alias.length() && input.regionMatches(true, 0, alias, 0, alias.length())) {
               if (input.length() == alias.length()) {
                  return new ConfigCommandSuggestions.AliasMatch(alias.length());
               }

               char next = input.charAt(alias.length());
               if (Character.isWhitespace(next)) {
                  return new ConfigCommandSuggestions.AliasMatch(alias.length());
               }
            }
         }

         return null;
      }
   }

   private static int countLeadingWhitespace(String value) {
      int index = 0;

      while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
         index++;
      }

      return index;
   }

   private static String nextToken(String value) {
      int index = 0;

      while (index < value.length() && !Character.isWhitespace(value.charAt(index))) {
         index++;
      }

      return value.substring(0, index);
   }

   private static String resolveCommand(String commandToken) {
      for (String command : ConfigCommand.getInstance().getSubCommands()) {
         if (command.equalsIgnoreCase(commandToken)) {
            return command;
         }
      }

      return null;
   }

   private static String normalizeCompletionSuffix(String suffix) {
      if (suffix == null) {
         return null;
      } else {
         return suffix.isEmpty() ? null : suffix;
      }
   }

   @Override
   public boolean matchesAliasPrefix(String input) {
      if (input != null && input.startsWith(".")) {
         for (String alias : ConfigCommand.getInstance().getCommandAliases()) {
            if (alias.startsWith(input)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Environment(EnvType.CLIENT)
   private record AliasMatch(int aliasEnd) {
   }
}