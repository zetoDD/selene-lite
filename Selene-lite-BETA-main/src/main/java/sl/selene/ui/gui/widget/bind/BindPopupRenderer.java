package sl.selene.ui.gui.widget.bind;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.bind.BindingMode;
import sl.selene.module.impl.client.MenuSettingsModule;
import sl.selene.ui.Colors;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontObject;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.utils.Color;

@Environment(EnvType.CLIENT)
public final class BindPopupRenderer {

   private BindPopupRenderer() {
   }

   public static BindPopupRenderer.Layout computeLayout(BindPopupModel model, float x, float y, float valueBlockHeight) {
      Objects.requireNonNull(model, "model");
      float sanitizedValueHeight = Math.max(0.0F, valueBlockHeight);
      boolean hasValueBlock = sanitizedValueHeight > 0.001F;
      float valueHeaderHeight = 0.0F;
      float valueContentPadding = hasValueBlock ? 0.0F : 0.0F;
      float totalValueBlockHeight = hasValueBlock ? valueHeaderHeight + sanitizedValueHeight + valueContentPadding : 0.0F;
      float height = 124.0F + totalValueBlockHeight;
      BindPopupRenderer.Rect bounds = new BindPopupRenderer.Rect(x, y, 334.0F, height);
      BindPopupRenderer.Rect header = new BindPopupRenderer.Rect(x, y, 334.0F, 0.0F);
      BindPopupRenderer.Rect bindBlock = new BindPopupRenderer.Rect(x + 18.0F, y, 298.0F, 62.0F);
      BindPopupRenderer.Rect modesBlock = new BindPopupRenderer.Rect(bindBlock.x(), bindBlock.bottom(), bindBlock.width(), 62.0F);
      BindPopupRenderer.Rect valueBlock = new BindPopupRenderer.Rect(bindBlock.x(), modesBlock.bottom(), bindBlock.width(), totalValueBlockHeight);
      float valueContentTop = valueBlock.y() + (hasValueBlock ? 0.0F : 0.0F);
      BindPopupRenderer.Rect valueContent = hasValueBlock
         ? new BindPopupRenderer.Rect(bounds.x(), valueContentTop, 334.0F, sanitizedValueHeight)
         : new BindPopupRenderer.Rect(bounds.x(), valueBlock.y(), 334.0F, 0.0F);
      float fieldX = bounds.x() + bounds.width() - 18.0F - 156.0F;
      float fieldY = bindBlock.y() + (bindBlock.height() - 34.0F) * 0.5F;
      BindPopupRenderer.Rect field = new BindPopupRenderer.Rect(fieldX, fieldY, 156.0F, 34.0F);
      float buttonY = modesBlock.y() + (modesBlock.height() - 38.0F) * 0.5F;
      BindPopupRenderer.Rect toggleButton = new BindPopupRenderer.Rect(bindBlock.x(), buttonY, 143.0F, 38.0F);
      BindPopupRenderer.Rect holdButton = new BindPopupRenderer.Rect(toggleButton.right() + 12.0F, buttonY, 143.0F, 38.0F);
      float bindLabelBaseline = bindBlock.y() + bindBlock.height() * 0.5F + 5.0F;
      float modeLabelBaseline = modesBlock.y() + 27.0F;
      float valueLabelBaseline = hasValueBlock ? valueBlock.y() + 27.0F : 0.0F;
      float titleBaseline = header.y() + 22.0F;
      float subtitleBaseline = titleBaseline + 20.0F;
      return new BindPopupRenderer.Layout(
         bounds,
         header,
         bindBlock,
         modesBlock,
         valueBlock,
         valueContent,
         field,
         toggleButton,
         holdButton,
         titleBaseline,
         subtitleBaseline,
         bindLabelBaseline,
         modeLabelBaseline,
         valueLabelBaseline,
         valueHeaderHeight,
         sanitizedValueHeight
      );
   }

   public static BindPopupRenderer.Rect resolveFieldRect(BindPopupRenderer.Layout layout, Renderer2D renderer, String keyLabel) {
      Objects.requireNonNull(layout, "layout");
      Objects.requireNonNull(renderer, "renderer");
      String label = keyLabel == null ? "" : keyLabel;
      float textWidth = 0.0F;
      if (!label.isEmpty()) {
         textWidth = renderer.measureText(FontRegistry.INTER_SEMIBOLD, label, 18.0F).width;
      }

      float padding = 24.0F;
      float desiredWidth = textWidth + padding;
      float contentAnchor = layout.valueContent().height() > 0.0F ? layout.valueContent().x() : layout.bindBlock().x();
      float maxWidth = Math.max(padding, layout.field().right() - contentAnchor);
      float resolvedWidth = Math.min(Math.max(desiredWidth, padding), maxWidth);
      float right = layout.field().right();
      float x = right - resolvedWidth;
      return new BindPopupRenderer.Rect(x, layout.field().y(), resolvedWidth, layout.field().height());
   }

   public static void render(
      Renderer2D renderer, FontObject defaultFont, BindPopupModel model, BindPopupRenderer.Layout layout, BindPopupRenderer.RenderState state
   ) {
      Objects.requireNonNull(renderer, "renderer");
      Objects.requireNonNull(defaultFont, "defaultFont");
      Objects.requireNonNull(model, "model");
      Objects.requireNonNull(layout, "layout");
      Objects.requireNonNull(state, "state");
      float alpha = clamp01(state.alpha());
      if (!(alpha <= 0.001F)) {
         renderer.pushAlpha(alpha);

         try {
            double backgroundAlpha = 0.75;
            renderer.rect(
               layout.bounds().x(), layout.bounds().y(), layout.bounds().width(), layout.bounds().height(), 12.0F, Color.getRGB(1447446, backgroundAlpha)
            );
            renderer.rectOutline(
               layout.bounds().x(), layout.bounds().y(), layout.bounds().width(), layout.bounds().height(), 12.0F, Color.getRGB(3355443, 1.0), 0.5F
            );
            float textX = layout.bounds().x() + 18.0F;
            float bindHover = Math.max(state.bindHoverProgress(), state.bindHovered() ? 1.0F : 0.0F);
            int bindTextColor;
            if (state.listening()) {
               bindTextColor = Color.getRGB(16777215, 0.98);
            } else if (bindHover > 0.001F) {
               int normalColor = Color.getRGB(8947848, 0.92);
               int hoverColor = Color.getRGB(16777215, 0.85);
               bindTextColor = Color.lerp(normalColor, hoverColor, bindHover);
            } else {
               bindTextColor = Color.getRGB(8947848, 0.92);
            }

            renderer.text(FontRegistry.INTER_SEMIBOLD, textX, layout.bindLabelBaseline(), 17.0F, "Bind Key", bindTextColor, "l");
            int fieldOutlineColor;
            if (state.listening()) {
               fieldOutlineColor = Color.getRGB(6974057, 1.0);
            } else if (bindHover > 0.001F) {
               int normalOutlineColor = Color.getRGB(5197646, 1.0);
               int hoverOutlineColor = Color.getRGB(6974057, 1.0);
               fieldOutlineColor = Color.lerp(normalOutlineColor, hoverOutlineColor, bindHover);
            } else {
               fieldOutlineColor = Color.getRGB(5197646, 1.0);
            }

            BindPopupRenderer.Rect fieldRect = state.fieldRect();
            renderer.rectOutline(fieldRect.x(), fieldRect.y(), fieldRect.width(), fieldRect.height(), 8.0F, fieldOutlineColor, 1.0F);
            float keyBaseline = fieldRect.centerY() + 5.0F + 1.0F;
            int keyTextColor;
            if (state.listening()) {
               keyTextColor = Color.getRGB(16777215, 0.98);
            } else if (bindHover > 0.001F) {
               int normalColor = Color.getRGB(8947848, 0.92);
               int hoverColor = Color.getRGB(16777215, 0.85);
               keyTextColor = Color.lerp(normalColor, hoverColor, bindHover);
            } else {
               keyTextColor = Color.getRGB(8947848, 0.92);
            }

            renderer.text(FontRegistry.INTER_SEMIBOLD, fieldRect.centerX(), keyBaseline, 18.0F, state.keyLabel(), keyTextColor, "c");
            if (!state.statusMessage().isEmpty()) {
               renderer.text(
                  FontRegistry.INTER_SEMIBOLD, textX, fieldRect.bottom() + 8.0F + 18.0F, 15.0F, state.statusMessage(), Color.getRGB(7105644, 0.9), "l"
               );
            }

            drawSeparator(renderer, layout.bindBlock().bottom(), layout.bounds().x(), layout.bounds().width(), alpha);
            float toggleHover = Math.max(state.toggleHoverProgress(), state.toggleHovered() ? 1.0F : 0.0F);
            float holdHover = Math.max(state.holdHoverProgress(), state.holdHovered() ? 1.0F : 0.0F);
            renderModeButton(renderer, layout.toggleButton(), "Toggle", state.mode() == BindingMode.TOGGLE, toggleHover, alpha, state.toggleSelectionProgress());
            renderModeButton(renderer, layout.holdButton(), "Hold", state.mode() == BindingMode.HOLD, holdHover, alpha, state.holdSelectionProgress());
            if (layout.valueBlock().height() > 0.0F) {
               renderValueSection(renderer, layout, state);
               drawSeparator(renderer, layout.modesBlock().bottom(), layout.bounds().x(), layout.bounds().width(), alpha);
            }
         } finally {
            renderer.popAlpha();
         }
      }
   }

   private static void renderValueSection(Renderer2D renderer, BindPopupRenderer.Layout layout, BindPopupRenderer.RenderState state) {
      if (!(state.valueBlockHeight() <= 0.001F)) {
         BindPopupRenderer.Rect block = layout.valueBlock();
         if (!(block.height() <= 0.001F)) {
            renderer.rect(block.x(), block.y(), block.width(), block.height(), 10.0F, Color.getRGB(1447446, 0.18));
            float headerHeight = Math.max(0.0F, layout.valueHeaderHeight());
            if (headerHeight > 0.001F) {
               renderer.rect(block.x(), block.y(), block.width(), headerHeight, 10.0F, 10.0F, 0.0F, 0.0F, Color.getRGB(1447446, 0.24));
            }
         }
      }
   }

   private static void renderModeButton(
      Renderer2D renderer, BindPopupRenderer.Rect bounds, String label, boolean selected, float hoverProgress, float alpha, float selectionProgress
   ) {
      double effectiveAlpha = clamp01(alpha);
      int outlineColor = Color.getRGB(5197646, 0.9);
      float resolvedHover = clamp01(hoverProgress);
      float resolvedSelection = clamp01(selectionProgress);
      double fillAlpha = 0.12 + 0.06 * resolvedHover;
      renderer.rect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 6.0F, Color.getRGB(1447446, fillAlpha));
      renderer.rectOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 6.0F, outlineColor, 1.0F);
      if (resolvedSelection > 0.001F) {
         renderer.rect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 6.0F, Color.getRGB(Colors.getClientPrimary(), resolvedSelection));
         renderer.rectOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 6.0F, Color.getRGB(Colors.getClientPrimary(), resolvedSelection), 1.0F);
      }

      int inactiveText = Color.getRGB(8947848, 0.85 * effectiveAlpha);
      int activeText = Color.getRGB(16777215, effectiveAlpha);
      int hoverText = Color.lerp(inactiveText, activeText, 0.35F * resolvedHover);
      int textColor = Color.lerp(hoverText, activeText, resolvedSelection);
      float baseline = bounds.centerY() + 5.0F;
      renderer.text(FontRegistry.INTER_SEMIBOLD, bounds.centerX(), baseline, 17.0F, label, textColor, "c");
   }

   private static void drawSeparator(Renderer2D renderer, float y, float popupX, float popupWidth, float alpha) {
      float menuScale = resolveMenuScale();
      float separatorY = Math.round(y * menuScale) / menuScale;
      renderer.rect(popupX + 18.0F, separatorY, popupWidth - 36.0F, 1.0F / menuScale, Color.getRGB(16777215, 0.05 * alpha));
   }

   private static float clamp01(float value) {
      if (value <= 0.0F) {
         return 0.0F;
      } else {
         return value >= 1.0F ? 1.0F : value;
      }
   }

   private static float resolveMenuScale() {
      MenuSettingsModule module = MenuSettingsModule.getInstanceIfAvailable();
      return module == null ? 1.0F : (float)module.getMenuScaleValue();
   }

   @Environment(EnvType.CLIENT)
   public record Layout(
      BindPopupRenderer.Rect bounds,
      BindPopupRenderer.Rect header,
      BindPopupRenderer.Rect bindBlock,
      BindPopupRenderer.Rect modesBlock,
      BindPopupRenderer.Rect valueBlock,
      BindPopupRenderer.Rect valueContent,
      BindPopupRenderer.Rect field,
      BindPopupRenderer.Rect toggleButton,
      BindPopupRenderer.Rect holdButton,
      float titleBaseline,
      float subtitleBaseline,
      float bindLabelBaseline,
      float modeLabelBaseline,
      float valueLabelBaseline,
      float valueHeaderHeight,
      float valueContentHeight
   ) {
   }

   @Environment(EnvType.CLIENT)
   public static final class Rect {
      private final float x;
      private final float y;
      private final float width;
      private final float height;

      public Rect(float x, float y, float width, float height) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      public float width() {
         return this.width;
      }

      public float height() {
         return this.height;
      }

      public float centerX() {
         return this.x + this.width * 0.5F;
      }

      public float centerY() {
         return this.y + this.height * 0.5F;
      }

      public float bottom() {
         return this.y + this.height;
      }

      public float right() {
         return this.x + this.width;
      }

      public boolean contains(double px, double py) {
         return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
      }
   }

   @Environment(EnvType.CLIENT)
   public record RenderState(
      float alpha,
      float blurFactor,
      boolean listening,
      boolean bindHovered,
      boolean toggleHovered,
      boolean holdHovered,
      float bindHoverProgress,
      float toggleHoverProgress,
      float holdHoverProgress,
      float toggleSelectionProgress,
      float holdSelectionProgress,
      BindingMode mode,
      String keyLabel,
      String statusMessage,
      float valueBlockHeight,
      float valueLabelBaseline,
      BindPopupRenderer.Rect fieldRect
   ) {
      public RenderState(
         float alpha,
         float blurFactor,
         boolean listening,
         boolean bindHovered,
         boolean toggleHovered,
         boolean holdHovered,
         float bindHoverProgress,
         float toggleHoverProgress,
         float holdHoverProgress,
         float toggleSelectionProgress,
         float holdSelectionProgress,
         BindingMode mode,
         String keyLabel,
         String statusMessage,
         float valueBlockHeight,
         float valueLabelBaseline,
         BindPopupRenderer.Rect fieldRect
      ) {
         Objects.requireNonNull(mode, "mode");
         keyLabel = keyLabel == null ? "" : keyLabel;
         statusMessage = statusMessage == null ? "" : statusMessage;
         fieldRect = Objects.requireNonNull(fieldRect, "fieldRect");
         bindHoverProgress = BindPopupRenderer.clamp01(bindHoverProgress);
         toggleHoverProgress = BindPopupRenderer.clamp01(toggleHoverProgress);
         holdHoverProgress = BindPopupRenderer.clamp01(holdHoverProgress);
         toggleSelectionProgress = BindPopupRenderer.clamp01(toggleSelectionProgress);
         holdSelectionProgress = BindPopupRenderer.clamp01(holdSelectionProgress);
         valueBlockHeight = Math.max(0.0F, valueBlockHeight);
         if (valueBlockHeight <= 0.0F) {
            valueLabelBaseline = 0.0F;
         }

         this.alpha = alpha;
         this.blurFactor = blurFactor;
         this.listening = listening;
         this.bindHovered = bindHovered;
         this.toggleHovered = toggleHovered;
         this.holdHovered = holdHovered;
         this.bindHoverProgress = bindHoverProgress;
         this.toggleHoverProgress = toggleHoverProgress;
         this.holdHoverProgress = holdHoverProgress;
         this.toggleSelectionProgress = toggleSelectionProgress;
         this.holdSelectionProgress = holdSelectionProgress;
         this.mode = mode;
         this.keyLabel = keyLabel;
         this.statusMessage = statusMessage;
         this.valueBlockHeight = valueBlockHeight;
         this.valueLabelBaseline = valueLabelBaseline;
         this.fieldRect = fieldRect;
      }
   }
}