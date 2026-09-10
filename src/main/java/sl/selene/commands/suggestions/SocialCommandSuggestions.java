package sl.selene.commands.suggestions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.commands.Command;
import sl.selene.config.friend.FriendCommand;
import sl.selene.config.target.TargetCommand;

@Environment(EnvType.CLIENT)
public final class SocialCommandSuggestions implements CommandSuggestionProvider {
   private static final SocialCommandSuggestions INSTANCE = new SocialCommandSuggestions();
   private static final List<Command> COMMANDS = List.of(FriendCommand.getInstance(), TargetCommand.getInstance());
   private static final List<String> SUB_COMMANDS = List.of("add", "remove", "list");
   private static final String NICK_PLACEHOLDER = "<name>";

   private SocialCommandSuggestions() {
   }

   public static SocialCommandSuggestions getInstance() {
      return INSTANCE;
   }

   @Override
   public List<String> aliases() {
      List<String> all = new ArrayList<>();
      for (Command command : COMMANDS) {
         all.addAll(command.aliases());
      }
      return List.copyOf(all);
   }

   @Override
   public boolean supportsInput(String input) {
      return findAlias(input) != null || ".".equals(input) || this.matchesAliasPrefix(input);
   }

   @Override
   public CommandSuggestions.SuggestionSet collect(String input) {
      AliasMatch alias = findAlias(input);
      if (alias == null) {
         return null;
      }

      String argsPortion = input.substring(alias.aliasEnd());
      if (argsPortion.isEmpty()) {
         return CommandSuggestions.of(buildSubCommandEntries("", 0));
      }

      int leadingSpaces = countLeadingWhitespace(argsPortion);
      String trimmedArgs = argsPortion.substring(leadingSpaces);
      if (trimmedArgs.isEmpty()) {
         return CommandSuggestions.of(buildSubCommandEntries("", leadingSpaces));
      }

      String firstToken = nextToken(trimmedArgs);
      boolean hasAdditionalCharacters = trimmedArgs.length() > firstToken.length();
      if (!hasAdditionalCharacters) {
         return CommandSuggestions.of(buildSubCommandEntries(firstToken, leadingSpaces));
      }

      String afterFirstToken = trimmedArgs.substring(firstToken.length());
      int spacesAfterFirstToken = countLeadingWhitespace(afterFirstToken);
      if (spacesAfterFirstToken == 0) {
         return CommandSuggestions.of(buildSubCommandEntries(firstToken, leadingSpaces));
      }

      String remaining = afterFirstToken.substring(spacesAfterFirstToken);
      if (remaining.isEmpty()) {
         return CommandSuggestions.of(buildNickPlaceholderEntries());
      }

      String secondToken = nextToken(remaining);
      return remaining.length() > secondToken.length() ? null : CommandSuggestions.of(buildNickPlaceholderEntries());
   }

   @Override
   public List<CommandSuggestions.SuggestionEntry> collectAliasSuggestions(String input) {
      if (input == null) {
         return List.of();
      }

      if (".".equals(input)) {
         return buildAllAliasEntries();
      }

      return !input.startsWith(".") ? List.of() : buildPartialAliasEntries(input);
   }

   @Override
   public boolean matchesAliasPrefix(String input) {
      if (input != null && input.startsWith(".")) {
         for (String alias : this.aliases()) {
            if (alias.startsWith(input)) {
               return true;
            }
         }
      }

      return false;
   }

   private static List<CommandSuggestions.SuggestionEntry> buildAllAliasEntries() {
      List<CommandSuggestions.SuggestionEntry> entries = new ArrayList<>();

      for (Command command : COMMANDS) {
         for (String alias : command.aliases()) {
            entries.add(new CommandSuggestions.SuggestionEntry(alias, command.description(), alias.substring(1), false));
         }
      }

      return entries;
   }

   private static List<CommandSuggestions.SuggestionEntry> buildPartialAliasEntries(String partialInput) {
      List<CommandSuggestions.SuggestionEntry> entries = new ArrayList<>();

      for (Command command : COMMANDS) {
         for (String alias : command.aliases()) {
            if (alias.startsWith(partialInput)) {
               String completion = alias.substring(partialInput.length());
               entries.add(new CommandSuggestions.SuggestionEntry(alias, command.description(), completion, false));
            }
         }
      }

      return entries;
   }

   private static List<CommandSuggestions.SuggestionEntry> buildSubCommandEntries(String partialToken, int leadingSpaces) {
      String normalized = partialToken.toLowerCase(Locale.ROOT);
      List<CommandSuggestions.SuggestionEntry> entries = new ArrayList<>();

      for (String subCommand : SUB_COMMANDS) {
         if (normalized.isEmpty() || subCommand.startsWith(normalized)) {
            StringBuilder completion = new StringBuilder();
            if (partialToken.isEmpty() && leadingSpaces == 0) {
               completion.append(' ');
            }

            if (partialToken.isEmpty()) {
               completion.append(subCommand);
            } else {
               completion.append(subCommand.substring(partialToken.length()));
            }

            completion.append(' ');
            String suffix = normalizeCompletionSuffix(completion.toString());
            entries.add(new CommandSuggestions.SuggestionEntry(subCommand, "Subcommand", suffix, false));
         }
      }

      return entries;
   }

   private static List<CommandSuggestions.SuggestionEntry> buildNickPlaceholderEntries() {
      return List.of(new CommandSuggestions.SuggestionEntry(NICK_PLACEHOLDER, "Player name", null, false));
   }

   private static AliasMatch findAlias(String input) {
      if (input == null) {
         return null;
      }

      for (Command command : COMMANDS) {
         for (String alias : command.aliases()) {
            if (input.length() >= alias.length() && input.regionMatches(true, 0, alias, 0, alias.length())) {
               if (input.length() == alias.length()) {
                  return new AliasMatch(alias.length());
               }

               char next = input.charAt(alias.length());
               if (Character.isWhitespace(next)) {
                  return new AliasMatch(alias.length());
               }
            }
         }
      }

      return null;
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

   private static String normalizeCompletionSuffix(String suffix) {
      return suffix == null || suffix.isEmpty() ? null : suffix;
   }

   @Environment(EnvType.CLIENT)
   private record AliasMatch(int aliasEnd) {
   }
}