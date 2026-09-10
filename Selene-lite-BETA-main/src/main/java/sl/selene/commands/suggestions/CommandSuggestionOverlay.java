package sl.selene.commands.suggestions;

import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.MathHelper;
import sl.selene.ui.Colors;

@Environment(EnvType.CLIENT)
public final class CommandSuggestionOverlay {

   private CommandSuggestionOverlay() {
   }

   public static CommandSuggestionOverlay.Layout render(
      DrawContext context,
      TextRenderer textRenderer,
      TextFieldWidget chatField,
      int screenWidth,
      List<CommandSuggestions.SuggestionEntry> entries,
      int selectionIndex,
      int scrollOffset,
      int maxVisible,
      int mouseX,
      int mouseY
   ) {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(textRenderer, "textRenderer");
      Objects.requireNonNull(chatField, "chatField");
      if (entries != null && !entries.isEmpty()) {
         CommandSuggestionOverlay.Layout layout = calculateLayout(textRenderer, chatField, screenWidth, entries, scrollOffset, maxVisible);
         if (layout == null) {
            return null;
         } else {
            drawBorders(context, layout, entries.size());
            context.fill(layout.areaX(), layout.areaY(), layout.areaRight(), layout.areaBottom(), -805306368);
            int themeColor = Colors.getClientPrimary();

            for (int visibleIndex = 0; visibleIndex < layout.visibleCount(); visibleIndex++) {
               int entryIndex = layout.scrollOffset() + visibleIndex;
               if (entryIndex >= entries.size()) {
                  break;
               }

               CommandSuggestions.SuggestionEntry entry = entries.get(entryIndex);
               int rowY = layout.areaY() + visibleIndex * layout.rowHeight();
               boolean isSelected = entryIndex == selectionIndex;
               int primaryColor = isSelected ? themeColor : (entry.accent() ? -1 : -2039584);
               context.drawTextWithShadow(textRenderer, entry.primaryText(), layout.contentX(), rowY + 2, primaryColor);
               if (layout.hasDescription() && !entry.description().isEmpty()) {
                  int descriptionX = layout.contentX() + layout.primaryColumnWidth() + 8;
                  int descriptionColor = isSelected ? themeColor : -6643546;
                  context.drawTextWithShadow(textRenderer, entry.description(), descriptionX, rowY + 2, descriptionColor);
               }
            }

            return layout;
         }
      } else {
         return null;
      }
   }

   private static void drawBorders(DrawContext context, CommandSuggestionOverlay.Layout layout, int totalEntries) {
      if (layout.scrollOffset() > 0) {
         context.fill(layout.areaX(), layout.areaY() - 1, layout.areaRight(), layout.areaY(), -1879048192);
      }

      if (layout.scrollOffset() + layout.visibleCount() < totalEntries) {
         context.fill(layout.areaX(), layout.areaBottom(), layout.areaRight(), layout.areaBottom() + 1, -1879048192);
      }
   }

   private static CommandSuggestionOverlay.Layout calculateLayout(
      TextRenderer textRenderer, TextFieldWidget chatField, int screenWidth, List<CommandSuggestions.SuggestionEntry> entries, int scrollOffset, int maxVisible
   ) {
      if (entries.isEmpty()) {
         return null;
      } else {
         int effectiveMaxVisible = Math.max(1, maxVisible);
         int clampedOffset = MathHelper.clamp(scrollOffset, 0, Math.max(entries.size() - 1, 0));
         int primaryWidth = 0;
         int descriptionWidth = 0;
         boolean hasDescription = false;

         for (CommandSuggestions.SuggestionEntry entry : entries) {
            primaryWidth = Math.max(primaryWidth, textRenderer.getWidth(entry.primaryText()));
            if (!entry.description().isEmpty()) {
               hasDescription = true;
               descriptionWidth = Math.max(descriptionWidth, textRenderer.getWidth(entry.description()));
            }
         }

         int contentWidth = primaryWidth + (hasDescription ? 8 + descriptionWidth : 0);
         int areaWidth = contentWidth + 8;
         int availableWidth = Math.max(0, screenWidth - 8);
         if (areaWidth > availableWidth) {
            areaWidth = availableWidth;
            if (hasDescription) {
               int maxPrimaryWidth = availableWidth - 8;
               if (maxPrimaryWidth < 0) {
                  maxPrimaryWidth = 0;
               }

               primaryWidth = Math.min(primaryWidth, maxPrimaryWidth);
               descriptionWidth = Math.max(0, availableWidth - 8 - primaryWidth - 8);
            }
         }

         int contentX = MathHelper.clamp(chatField.getX(), 4, screenWidth - areaWidth - 4) + 4;
         int areaX = contentX - 4;
         int visibleCount = Math.min(entries.size() - clampedOffset, effectiveMaxVisible);
         if (visibleCount <= 0) {
            clampedOffset = Math.max(0, entries.size() - effectiveMaxVisible);
            visibleCount = Math.min(entries.size(), effectiveMaxVisible);
         }

         int areaHeight = visibleCount * 12;
         int areaY = Math.max(4, chatField.getY() - 4 - areaHeight);
         return new CommandSuggestionOverlay.Layout(
            areaX, areaY, areaWidth, 12, visibleCount, primaryWidth, descriptionWidth, contentX, clampedOffset, hasDescription
         );
      }
   }

   @Environment(EnvType.CLIENT)
   public record Layout(
      int areaX,
      int areaY,
      int areaWidth,
      int rowHeight,
      int visibleCount,
      int primaryColumnWidth,
      int descriptionColumnWidth,
      int contentX,
      int scrollOffset,
      boolean hasDescription
   ) {
      public Layout(
         int areaX,
         int areaY,
         int areaWidth,
         int rowHeight,
         int visibleCount,
         int primaryColumnWidth,
         int descriptionColumnWidth,
         int contentX,
         int scrollOffset,
         boolean hasDescription
      ) {
         if (visibleCount < 0) {
            throw new IllegalArgumentException("visibleCount must be non-negative");
         } else {
            this.areaX = areaX;
            this.areaY = areaY;
            this.areaWidth = areaWidth;
            this.rowHeight = rowHeight;
            this.visibleCount = visibleCount;
            this.primaryColumnWidth = primaryColumnWidth;
            this.descriptionColumnWidth = descriptionColumnWidth;
            this.contentX = contentX;
            this.scrollOffset = scrollOffset;
            this.hasDescription = hasDescription;
         }
      }

      public int areaRight() {
         return this.areaX + this.areaWidth;
      }

      public int areaBottom() {
         return this.areaY + this.visibleCount * this.rowHeight;
      }

      public boolean contains(double mouseX, double mouseY) {
         return mouseX >= this.areaX && mouseX <= this.areaRight() && mouseY >= this.areaY && mouseY < this.areaBottom();
      }

      public int entryIndexAt(double mouseX, double mouseY) {
         if (this.contains(mouseX, mouseY) && this.visibleCount != 0) {
            int relative = MathHelper.clamp((int)((mouseY - this.areaY) / this.rowHeight), 0, this.visibleCount - 1);
            return this.scrollOffset + relative;
         } else {
            return -1;
         }
      }
   }
}