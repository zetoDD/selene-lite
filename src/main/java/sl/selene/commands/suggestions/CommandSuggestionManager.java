package sl.selene.commands.suggestions;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.config.ConfigCommandSuggestions;

@Environment(EnvType.CLIENT)
public final class CommandSuggestionManager {
   private static final List<CommandSuggestionProvider> PROVIDERS = List.of(
         ConfigCommandSuggestions.getInstance(),
         SocialCommandSuggestions.getInstance()
   );

   private CommandSuggestionManager() {
   }

   public static boolean supportsInput(String input) {
      if (".".equals(input)) {
         return true;
      } else {
         for (CommandSuggestionProvider provider : PROVIDERS) {
            if (provider.supportsInput(input) || provider.matchesAliasPrefix(input)) {
               return true;
            }
         }

         return false;
      }
   }

   public static CommandSuggestions.SuggestionSet collect(String input) {
      if (input == null) {
         return null;
      } else if (".".equals(input)) {
         return CommandSuggestions.of(collectAliasEntries(input));
      } else {
         for (CommandSuggestionProvider provider : PROVIDERS) {
            if (provider.supportsInput(input)) {
               CommandSuggestions.SuggestionSet set = provider.collect(input);
               if (set != null && !set.isEmpty()) {
                  return set;
               }
            }
         }

         return input.startsWith(".") ? CommandSuggestions.of(collectAliasEntries(input)) : null;
      }
   }

   private static List<CommandSuggestions.SuggestionEntry> collectAliasEntries(String input) {
      List<CommandSuggestions.SuggestionEntry> entries = new ArrayList<>();

      for (CommandSuggestionProvider provider : PROVIDERS) {
         List<CommandSuggestions.SuggestionEntry> providerEntries = provider.collectAliasSuggestions(input);
         if (providerEntries != null && !providerEntries.isEmpty()) {
            entries.addAll(providerEntries);
         }
      }

      return entries.isEmpty() ? List.of() : List.copyOf(entries);
   }
}