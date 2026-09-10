package sl.selene.commands.suggestions;

import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CommandSuggestions {
   private CommandSuggestions() {
   }

   public static CommandSuggestions.SuggestionSet of(List<CommandSuggestions.SuggestionEntry> entries) {
      if (entries != null && !entries.isEmpty()) {
         List<CommandSuggestions.SuggestionEntry> immutableEntries = List.copyOf(entries);
         String suffix = immutableEntries.get(0).completionSuffix();
         return new CommandSuggestions.SuggestionSet(suffix, immutableEntries);
      } else {
         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   public record SuggestionEntry(String primaryText, String description, String completionSuffix, boolean accent) {
      public SuggestionEntry(String primaryText, String description, String completionSuffix, boolean accent) {
         primaryText = Objects.requireNonNull(primaryText, "primaryText");
         description = description == null ? "" : description;
         this.primaryText = primaryText;
         this.description = description;
         this.completionSuffix = completionSuffix;
         this.accent = accent;
      }
   }

   @Environment(EnvType.CLIENT)
   public record SuggestionSet(String suggestionSuffix, List<CommandSuggestions.SuggestionEntry> entries) {
      public SuggestionSet(String suggestionSuffix, List<CommandSuggestions.SuggestionEntry> entries) {
         Objects.requireNonNull(entries, "entries");
         this.suggestionSuffix = suggestionSuffix;
         this.entries = entries;
      }

      public boolean isEmpty() {
         return this.entries.isEmpty();
      }
   }
}