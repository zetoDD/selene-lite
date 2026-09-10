package sl.selene.ui.gui.widget.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class ColorPickerPopupManager {
   private static final ColorPickerPopupManager INSTANCE = new ColorPickerPopupManager();
   private final List<ColorPickerPopupManager.QueuedPopup> queue = new ArrayList<>();
   private final List<ColorPickerWidget> activePopups = new ArrayList<>();
   private final Map<ColorPickerWidget, ColorPickerPopupManager.QueuedPopup> lastKnownParams = new HashMap<>();

   private ColorPickerPopupManager() {
   }

   public static ColorPickerPopupManager getInstance() {
      return INSTANCE;
   }

   public synchronized void queue(ColorPickerWidget widget, float alphaMultiplier, float expansionProgress) {
      Objects.requireNonNull(widget, "widget");
      ColorPickerPopupManager.QueuedPopup popup = new ColorPickerPopupManager.QueuedPopup(widget, alphaMultiplier, expansionProgress);
      this.lastKnownParams.put(widget, popup);
      if (!this.activePopups.contains(widget)) {
         this.activePopups.add(widget);
      }

      if (!(alphaMultiplier <= 0.0F)) {
         this.queue.removeIf(entry -> entry.widget == widget);
         this.queue.add(popup);
      }
   }

   public synchronized void render(Renderer2D renderer) {
      Objects.requireNonNull(renderer, "renderer");
      List<ColorPickerPopupManager.QueuedPopup> snapshot = new ArrayList<>(this.queue);
      this.queue.clear();

      for (ColorPickerPopupManager.QueuedPopup popup : snapshot) {
         this.activePopups.remove(popup.widget);
         this.activePopups.add(popup.widget);
      }

      List<ColorPickerWidget> toRemove = new ArrayList<>();

      for (ColorPickerWidget widget : this.activePopups) {
         float openProgress = widget.getPopupAnimationProgress();
         if (openProgress > 0.001F) {
            ColorPickerPopupManager.QueuedPopup lastParams = this.lastKnownParams.get(widget);
            if (lastParams != null) {
               widget.renderPopupContents(renderer, lastParams.alphaMultiplier(), lastParams.expansionProgress());
            } else {
               widget.renderPopupContents(renderer, 1.0F, 1.0F);
            }
         } else {
            toRemove.add(widget);
            this.lastKnownParams.remove(widget);
         }
      }

      this.activePopups.removeAll(toRemove);
   }

   public synchronized boolean handleMouseClick(double mouseX, double mouseY, int button) {
      boolean blocking = false;

      for (ColorPickerWidget widget : this.activePopups) {
         if (widget.isBlockingInteractions()) {
            blocking = true;
            if (widget.handleOverlayClick(mouseX, mouseY, button)) {
               return true;
            }

            if (widget.handleMouseClick(mouseX, mouseY, button)) {
               return true;
            }
         }
      }

      return blocking;
   }

   public synchronized boolean handleMouseScroll(double mouseX, double mouseY, double horizontal, double vertical) {
      boolean blocking = false;

      for (ColorPickerWidget widget : this.activePopups) {
         if (widget.isBlockingInteractions()) {
            blocking = true;
            if (widget.handleOverlayScroll(mouseX, mouseY, horizontal, vertical)) {
               return true;
            }
         }
      }

      return blocking;
   }

   @Environment(EnvType.CLIENT)
   private record QueuedPopup(ColorPickerWidget widget, float alphaMultiplier, float expansionProgress) {
   }
}