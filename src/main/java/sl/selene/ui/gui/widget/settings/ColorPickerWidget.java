package sl.selene.ui.gui.widget.settings;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.ui.gui.widget.popup.PopupPositioner;
import sl.selene.util.render.animation.AnimationSystem;
import sl.selene.util.render.animation.Easings;
import sl.selene.util.render.animation.SpringConfig;
import sl.selene.util.render.animation.SpringFloatAnimator;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.utils.Color;

@Environment(EnvType.CLIENT)
public final class ColorPickerWidget implements SettingWidget {
   private static final PopupPositioner POPUP_POSITIONER = new PopupPositioner(16.0F, 8.0F, 8.0F);
   private static final SpringConfig HOVER_SPRING = SpringConfig.of(1.4F, 0.7F);
   private static final SpringConfig POPUP_SPRING = SpringConfig.of(2.1F, 0.68F);
   private static final SpringConfig VISIBILITY_SPRING = SpringConfig.of(1.8F, 0.65F);
   private static final float[] HUE_STOPS = new float[]{0.0F, 60.0F, 120.0F, 180.0F, 240.0F, 300.0F, 360.0F};
   private final Module module;
   private final HueSetting setting;
   private final PopupOpenContext popupContext;
   private final SettingValueAccessor<HSBColor> valueAccessor;
   private final String labelOverride;
   private final SpringFloatAnimator hoverAnimator;
   private final SpringFloatAnimator popupAnimator;
   private final SpringFloatAnimator visibilityAnimator;
   private ColorPickerWidget.Rect layoutBounds = ColorPickerWidget.Rect.EMPTY;
   private ColorPickerWidget.Rect interactionBounds = ColorPickerWidget.Rect.EMPTY;
   private ColorPickerWidget.Rect swatchBounds = ColorPickerWidget.Rect.EMPTY;
   private ColorPickerWidget.Rect popupBounds = ColorPickerWidget.Rect.EMPTY;
   private ColorPickerWidget.Rect saturationBounds = ColorPickerWidget.Rect.EMPTY;
   private ColorPickerWidget.Rect hueSliderBounds = ColorPickerWidget.Rect.EMPTY;
   private ColorPickerWidget.Rect alphaSliderBounds = ColorPickerWidget.Rect.EMPTY;
   private boolean hovered = false;
   private boolean popupOpen = false;
   private boolean sbDragging = false;
   private boolean hueDragging = false;
   private boolean alphaDragging = false;
   private HSBColor currentColor;
   private float hue;
   private float saturation;
   private float brightness;
   private float alpha;
   private double lastPointerX;
   private double lastPointerY;
   private double popupAnchorX = Double.NaN;
   private double popupAnchorY = Double.NaN;
   private boolean pendingAnchorReset = false;
   private float textX;
   private float textBaseline;

   public ColorPickerWidget(Module module, PopupOpenContext popupContext, HueSetting setting, SettingValueAccessor<HSBColor> valueAccessor) {
      this(module, popupContext, setting, valueAccessor, null);
   }

   public ColorPickerWidget(
      Module module, PopupOpenContext popupContext, HueSetting setting, SettingValueAccessor<HSBColor> valueAccessor, String labelOverride
   ) {
      this.module = Objects.requireNonNull(module, "module");
      this.popupContext = Objects.requireNonNull(popupContext, "popupContext");
      this.setting = Objects.requireNonNull(setting, "setting");
      this.valueAccessor = Objects.requireNonNull(valueAccessor, "valueAccessor");
      this.labelOverride = normalizeLabel(labelOverride);
      this.hue = setting.current;
      this.saturation = setting.saturation;
      this.brightness = setting.brightness;
      this.alpha = 1.0F;
      this.currentColor = HSBColor.of(this.hue, this.saturation, this.brightness, this.alpha);
      this.hoverAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), HOVER_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.hoverAnimator.setOutputTransform(Easings.SMOOTH_STEP);
      this.popupAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), POPUP_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.popupAnimator.setOutputTransform(Easings.SMOOTH_STEP);
      this.visibilityAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), VISIBILITY_SPRING, 1.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.visibilityAnimator.setOutputTransform(Easings.SMOOTH_STEP);
   }

   @Override
   public void updateState() {
      this.hue = this.setting.current;
      this.saturation = this.setting.saturation;
      this.brightness = this.setting.brightness;
      this.alpha = 1.0F;
      this.currentColor = HSBColor.of(this.hue, this.saturation, this.brightness, this.alpha);
      this.updateHoverTarget();
   }

   @Override
   public void layout(float panelX, float top, float panelWidth) {
      this.layoutBounds = new ColorPickerWidget.Rect(panelX, top, panelWidth, 62.0F);
      float visibility = clamp01(this.visibilityAnimator.getValue());
      float currentHeight = 62.0F * visibility;
      this.interactionBounds = new ColorPickerWidget.Rect(panelX, top, panelWidth, currentHeight);
      float swatchX = panelX + panelWidth - 18.0F - 22.0F;
      float swatchY = top + 20.0F;
      this.swatchBounds = new ColorPickerWidget.Rect(swatchX, swatchY, 22.0F, 22.0F);
      this.textX = panelX + 18.0F;
      this.textBaseline = top + 31.0F + 5.0F;
      this.updatePopupLayout();
   }

   @Override
   public float getHeight() {
      return 62.0F;
   }

   @Override
   public float getCurrentHeight() {
      return 62.0F * clamp01(this.visibilityAnimator.getValue());
   }

   @Override
   public void updateVisibility(boolean visible) {
      float target = visible ? 1.0F : 0.0F;
      this.visibilityAnimator.setTarget(target);
      if (!visible) {
         this.hovered = false;
         this.updateHoverTarget();
         this.closePopup();
      }
   }

   @Override
   public void render(Renderer2D renderer, float alphaMultiplier, float expansionProgress) {
      float visibility = clamp01(this.visibilityAnimator.getValue());
      if (!(visibility <= 0.001F)) {
         float effectiveAlpha = alphaMultiplier * clamp01(expansionProgress) * visibility;
         if (!(effectiveAlpha <= 0.001F)) {
            float highlight = clamp01(this.hoverAnimator.getValue());
            int baseColor = applyAlpha(-7829368, effectiveAlpha);
            int highlightColor = applyAlpha(-1, effectiveAlpha);
            int textColor = Color.lerp(baseColor, highlightColor, highlight);
            renderer.pushScale(1.0F, visibility, this.layoutBounds.x, this.layoutBounds.y);

            try {
               renderer.text(FontRegistry.INTER_SEMIBOLD, this.textX, this.textBaseline, 18.0F, this.resolveLabel(), textColor, "l");
               if (this.alpha < 0.999F) {
                  renderer.pushRoundedClipRect(
                     this.swatchBounds.x, this.swatchBounds.y, this.swatchBounds.width, this.swatchBounds.height, 8.0F, 8.0F, 8.0F, 8.0F
                  );

                  try {
                     this.renderCheckerboard(renderer, this.swatchBounds, effectiveAlpha);
                  } finally {
                     renderer.popClipRect();
                  }
               }

               int swatchColor = modulateColor(this.currentColor.toRgba(), effectiveAlpha);
               renderer.rect(this.swatchBounds.x, this.swatchBounds.y, this.swatchBounds.width, this.swatchBounds.height, 8.0F, swatchColor);
            } finally {
               renderer.popScale();
            }
         }
      }
   }

   @Override
   public void renderOverlay(Renderer2D renderer, float alphaMultiplier, float expansionProgress) {
      float openProgress = clamp01(this.popupAnimator.getValue());
      if (!(openProgress <= 0.01F)) {
         ColorPickerPopupManager.getInstance().queue(this, alphaMultiplier, expansionProgress);
      }
   }

   void renderPopupContents(Renderer2D renderer, float alphaMultiplier, float expansionProgress) {
      float openProgress = clamp01(this.popupAnimator.getValue());
      if (!this.isInteractable()) {
         if (this.popupOpen) {
            this.closePopup();
         }

         if (openProgress <= 0.01F) {
            if (this.pendingAnchorReset) {
               this.resetPopupAnchor();
            }

            return;
         }
      } else if (!this.popupOpen && openProgress <= 0.01F) {
         if (this.pendingAnchorReset) {
            this.resetPopupAnchor();
         }

         return;
      }

      this.updatePopupLayout();
      float effectiveAlpha = alphaMultiplier * clamp01(expansionProgress) * openProgress;
      if (!(effectiveAlpha <= 0.001F)) {
         this.updateDraggingState();
         renderer.rect(
            this.popupBounds.x, this.popupBounds.y, this.popupBounds.width, this.popupBounds.height, 12.0F, Color.getRGB(1447446, 0.76 * effectiveAlpha)
         );
         renderer.rectOutline(
            this.popupBounds.x, this.popupBounds.y, this.popupBounds.width, this.popupBounds.height, 12.0F, applyAlpha(-11448499, effectiveAlpha), 1.0F
         );
         this.renderSaturationBrightnessField(renderer, effectiveAlpha);
         this.renderHueSlider(renderer, effectiveAlpha);
         this.renderAlphaSlider(renderer, effectiveAlpha);
         this.renderCrosshair(renderer, effectiveAlpha);
         this.renderSliderCursors(renderer, effectiveAlpha);
      }
   }

   @Override
   public boolean isBlockingInteractions() {
      return !this.isInteractable() ? false : this.popupOpen || this.popupAnimator.getValue() > 0.01F;
   }

   @Override
   public boolean handleOverlayClick(double mouseX, double mouseY, int button) {
      if (!this.isBlockingInteractions()) {
         return false;
      } else if (button == 1) {
         this.closePopup();
         return true;
      } else if (button != 0) {
         return true;
      } else if (!this.popupBounds.contains(mouseX, mouseY)) {
         this.closePopup();
         return true;
      } else if (this.saturationBounds.contains(mouseX, mouseY)) {
         this.sbDragging = true;
         this.updateFromSaturationBrightness(mouseX, mouseY);
         return true;
      } else if (this.hueSliderBounds.contains(mouseX, mouseY)) {
         this.hueDragging = true;
         this.updateFromHue(mouseY);
         return true;
      } else if (this.alphaSliderBounds.contains(mouseX, mouseY)) {
         this.alphaDragging = true;
         this.updateFromAlpha(mouseY);
         return true;
      } else {
         return true;
      }
   }

   @Override
   public boolean handleMouseClick(double mouseX, double mouseY, int button) {
      if (!this.isInteractable() || !this.interactionBounds.contains(mouseX, mouseY)) {
         return false;
      } else if (button == 2) {
         HSBColor initial = this.currentColor;
         this.popupContext.openForSetting(this.module, this.setting, mouseX, mouseY, initial);
         return true;
      } else if (button != 0) {
         return false;
      } else {
         if (this.popupOpen) {
            this.closePopup();
         } else {
            this.setPopupAnchor(mouseX, mouseY);
            this.openPopup();
         }

         return true;
      }
   }

   @Override
   public void updateHoverState(double mouseX, double mouseY) {
      if (!this.isInteractable()) {
         this.hovered = false;
         this.updateHoverTarget();
      } else {
         this.lastPointerX = mouseX;
         this.lastPointerY = mouseY;
         this.hovered = this.interactionBounds.contains(mouseX, mouseY);
         this.updateHoverTarget();
      }
   }

   @Override
   public void collapse() {
      this.closePopup();
   }

   @Override
   public boolean handleOverlayScroll(double mouseX, double mouseY, double horizontal, double vertical) {
      return this.isBlockingInteractions();
   }

   @Override
   public Setting getSetting() {
      return this.setting;
   }

   private boolean isInteractable() {
      return clamp01(this.visibilityAnimator.getValue()) > 0.001F;
   }

   public float getPopupAnimationProgress() {
      return clamp01(this.popupAnimator.getValue());
   }

   private void openPopup() {
      this.popupOpen = true;
      this.popupAnimator.setTarget(1.0F);
      this.sbDragging = false;
      this.hueDragging = false;
      this.alphaDragging = false;
      this.ensurePopupAnchor();
      this.updatePopupLayout();
      this.pendingAnchorReset = false;
   }

   private void closePopup() {
      this.popupOpen = false;
      this.popupAnimator.setTarget(0.0F);
      this.sbDragging = false;
      this.hueDragging = false;
      this.alphaDragging = false;
      this.pendingAnchorReset = true;
   }

   private void updateHoverTarget() {
      float target = this.hovered ? 1.0F : 0.0F;
      this.hoverAnimator.setTarget(target);
   }

   private void updatePopupLayout() {
      PopupPositioner.PopupPlacement placement = this.resolvePopupPlacement();
      float popupX = placement.x();
      float popupY = placement.y();
      this.popupBounds = new ColorPickerWidget.Rect(popupX, popupY, 275.0F, 222.0F);
      float sbX = popupX + 10.0F;
      float sbY = popupY + 10.0F;
      this.saturationBounds = new ColorPickerWidget.Rect(sbX, sbY, 200.0F, 200.0F);
      float hueX = sbX + 200.0F + 7.0F;
      this.hueSliderBounds = new ColorPickerWidget.Rect(hueX, sbY, 18.0F, 200.0F);
      float alphaX = hueX + 18.0F + 7.0F;
      this.alphaSliderBounds = new ColorPickerWidget.Rect(alphaX, sbY, 18.0F, 200.0F);
   }

   private PopupPositioner.PopupPlacement resolvePopupPlacement() {
      MinecraftClient client = MinecraftClient.getInstance();
      int viewportWidth = 1;
      int viewportHeight = 1;
      if (client != null && client.getWindow() != null) {
         viewportWidth = Math.max(1, client.getWindow().getFramebufferWidth());
         viewportHeight = Math.max(1, client.getWindow().getFramebufferHeight());
      }

      double anchorX = Double.isFinite(this.popupAnchorX) ? this.popupAnchorX : this.swatchBounds.right();
      double anchorY = Double.isFinite(this.popupAnchorY) ? this.popupAnchorY : this.swatchBounds.centerY();
      float layoutScale = 1.0F;
      if (!Float.isFinite(layoutScale) || layoutScale <= 0.0F) {
         layoutScale = 1.0F;
      }

      return POPUP_POSITIONER.resolve(anchorX, anchorY, 275.0F, 222.0F, viewportWidth, viewportHeight, layoutScale);
   }

   private void setPopupAnchor(double anchorX, double anchorY) {
      this.popupAnchorX = Double.isFinite(anchorX) ? anchorX : Double.NaN;
      this.popupAnchorY = Double.isFinite(anchorY) ? anchorY : Double.NaN;
   }

   private void resetPopupAnchor() {
      this.popupAnchorX = Double.NaN;
      this.popupAnchorY = Double.NaN;
      this.pendingAnchorReset = false;
   }

   private void ensurePopupAnchor() {
      if (!Double.isFinite(this.popupAnchorX) || !Double.isFinite(this.popupAnchorY)) {
         float fallbackX = this.swatchBounds.right();
         float fallbackY = this.swatchBounds.centerY();
         this.setPopupAnchor(fallbackX, fallbackY);
      }
   }

   private void updateDraggingState() {
      boolean leftDown = this.isMouseButtonDown();
      if (!leftDown) {
         this.sbDragging = false;
         this.hueDragging = false;
         this.alphaDragging = false;
      } else {
         if (this.sbDragging) {
            this.updateFromSaturationBrightness(this.lastPointerX, this.lastPointerY);
         }

         if (this.hueDragging) {
            this.updateFromHue(this.lastPointerY);
         }

         if (this.alphaDragging) {
            this.updateFromAlpha(this.lastPointerY);
         }
      }
   }

   private void updateFromSaturationBrightness(double mouseX, double mouseY) {
      float relativeX = clamp01((float)((mouseX - this.saturationBounds.x) / this.saturationBounds.width));
      float relativeY = clamp01((float)((mouseY - this.saturationBounds.y) / this.saturationBounds.height));
      float newBrightness = 1.0F - relativeY;
      this.applyNewColor(this.currentColor.withSaturation(relativeX).withBrightness(newBrightness));
   }

   private void updateFromHue(double mouseY) {
      float relative = clamp01((float)((mouseY - this.hueSliderBounds.y) / this.hueSliderBounds.height));
      float newHue = clampHue(Math.min(359.999F, relative * 360.0F));
      this.applyNewColor(this.currentColor.withHue(newHue));
   }

   private void updateFromAlpha(double mouseY) {
      float relative = clamp01((float)((mouseY - this.alphaSliderBounds.y) / this.alphaSliderBounds.height));
      float newAlpha = clamp01(1.0F - relative);
      this.applyNewColor(this.currentColor.withAlpha(newAlpha));
   }

   private void applyNewColor(HSBColor candidate) {
      HSBColor normalised = candidate.normalised();
      if (!normalised.equals(this.currentColor)) {
         this.currentColor = normalised;
         this.hue = normalised.hue();
         this.saturation = normalised.saturation();
         this.brightness = normalised.brightness();
         this.alpha = normalised.alpha();
         this.setting.current = this.hue;
         this.setting.saturation = this.saturation;
         this.setting.brightness = this.brightness;
         this.valueAccessor.set(normalised);
      }
   }

   private void renderSaturationBrightnessField(Renderer2D renderer, float effectiveAlpha) {
      int topLeft = applyAlpha(-1, effectiveAlpha);
      int topRight = modulateColor(HSBColor.of(this.hue, 1.0F, 1.0F, 1.0F).toRgba(), effectiveAlpha);
      int bottomLeft = applyAlpha(-16777216, effectiveAlpha);
      int bottomRight = applyAlpha(-16777216, effectiveAlpha);
      renderer.gradient(
         this.saturationBounds.x,
         this.saturationBounds.y,
         this.saturationBounds.width,
         this.saturationBounds.height,
         6.0F,
         topLeft,
         topRight,
         bottomRight,
         bottomLeft
      );
   }

   private void renderHueSlider(Renderer2D renderer, float effectiveAlpha) {
      float segmentHeight = this.hueSliderBounds.height / (HUE_STOPS.length - 1);
      float currentY = this.hueSliderBounds.y;

      for (int i = 0; i < HUE_STOPS.length - 1; i++) {
         float startHue = HUE_STOPS[i];
         float endHue = HUE_STOPS[i + 1];
         int topColor = modulateColor(HSBColor.of(startHue, 1.0F, 1.0F, 1.0F).toRgba(), effectiveAlpha);
         int bottomColor = modulateColor(HSBColor.of(endHue, 1.0F, 1.0F, 1.0F).toRgba(), effectiveAlpha);
         float height = i == HUE_STOPS.length - 2 ? this.hueSliderBounds.bottom() - currentY : segmentHeight;
         float topLeftRadius = i == 0 ? 4.0F : 0.0F;
         float topRightRadius = i == 0 ? 4.0F : 0.0F;
         float bottomRightRadius = i == HUE_STOPS.length - 2 ? 4.0F : 0.0F;
         renderer.gradient(
            this.hueSliderBounds.x,
            currentY,
            this.hueSliderBounds.width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomRightRadius,
            topColor,
            topColor,
            bottomColor,
            bottomColor
         );
         currentY += height;
      }
   }

   private void renderAlphaSlider(Renderer2D renderer, float effectiveAlpha) {
      renderer.pushRoundedClipRect(
         this.alphaSliderBounds.x, this.alphaSliderBounds.y, this.alphaSliderBounds.width, this.alphaSliderBounds.height, 4.0F, 4.0F, 4.0F, 4.0F
      );

      try {
         this.renderCheckerboard(renderer, this.alphaSliderBounds, effectiveAlpha);
      } finally {
         renderer.popClipRect();
      }

      HSBColor opaque = HSBColor.of(this.hue, this.saturation, this.brightness, 1.0F);
      HSBColor transparent = HSBColor.of(this.hue, this.saturation, this.brightness, 0.0F);
      renderer.gradient(
         this.alphaSliderBounds.x,
         this.alphaSliderBounds.y,
         this.alphaSliderBounds.width,
         this.alphaSliderBounds.height,
         4.0F,
         modulateColor(opaque.toRgba(), effectiveAlpha),
         modulateColor(opaque.toRgba(), effectiveAlpha),
         modulateColor(transparent.toRgba(), effectiveAlpha),
         modulateColor(transparent.toRgba(), effectiveAlpha)
      );
   }

   private void renderCrosshair(Renderer2D renderer, float effectiveAlpha) {
      float cursorX = this.saturationBounds.x + this.saturation * this.saturationBounds.width;
      float cursorY = this.saturationBounds.y + (1.0F - this.brightness) * this.saturationBounds.height;
      int fill = applyAlpha(-1, effectiveAlpha);
      int outline = applyAlpha(-16777216, effectiveAlpha);
      float innerRadius = Math.max(0.0F, 4.5F);
      renderer.shadow(cursorX, cursorY, 1.0F, 1.0F, 5.0F, 5.0F, 0.0F, 1828716544);
      renderer.circle(cursorX, cursorY, innerRadius, 0.0F, 1.0F, fill);
   }

   private void renderSliderCursors(Renderer2D renderer, float effectiveAlpha) {
      float hueRelative = clamp01(this.hue / 359.999F);
      float hueCenterY = this.hueSliderBounds.y + hueRelative * this.hueSliderBounds.height;
      this.renderSliderCursor(renderer, this.hueSliderBounds.centerX(), hueCenterY, effectiveAlpha);
      float alphaRelative = 1.0F - this.alpha;
      float alphaCenterY = this.alphaSliderBounds.y + alphaRelative * this.alphaSliderBounds.height;
      this.renderSliderCursor(renderer, this.alphaSliderBounds.centerX(), alphaCenterY, effectiveAlpha);
   }

   private void renderSliderCursor(Renderer2D renderer, float centerX, float centerY, float effectiveAlpha) {
      float cursorX = centerX - 11.0F;
      float cursorY = centerY - 2.5F;
      renderer.rect(cursorX, cursorY, 22.0F, 5.0F, 8.0F, applyAlpha(-1, effectiveAlpha));
   }

   private void renderCheckerboard(Renderer2D renderer, ColorPickerWidget.Rect bounds, float effectiveAlpha) {
      float squareSize = 6.0F;
      boolean toggleRow = false;

      for (float y = bounds.y; y < bounds.bottom(); y += squareSize) {
         boolean toggle = toggleRow;
         float rowHeight = Math.min(squareSize, bounds.bottom() - y);

         for (float x = bounds.x; x < bounds.right(); x += squareSize) {
            float colWidth = Math.min(squareSize, bounds.right() - x);
            int color = toggle ? applyAlpha(-12829636, effectiveAlpha) : applyAlpha(-14013910, effectiveAlpha);
            renderer.rect(x, y, colWidth, rowHeight, color);
            toggle = !toggle;
         }

         toggleRow = !toggleRow;
      }
   }

   private boolean isMouseButtonDown() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client != null && client.getWindow() != null ? GLFW.glfwGetMouseButton(client.getWindow().getHandle(), 0) == 1 : false;
   }

   private String resolveLabel() {
      return this.labelOverride != null ? this.labelOverride : this.setting.name;
   }

   private static String normalizeLabel(String label) {
      if (label == null) {
         return null;
      } else {
         String trimmed = label.trim();
         return trimmed.isEmpty() ? null : trimmed;
      }
   }

   private static float clampHue(float hue) {
      if (!Float.isFinite(hue)) {
         return 0.0F;
      } else {
         float wrapped = hue % 360.0F;
         if (wrapped < 0.0F) {
            wrapped += 360.0F;
         }

         return wrapped;
      }
   }

   private static float clamp01(float value) {
      return !(value <= 0.0F) && !Float.isNaN(value) ? Math.min(value, 1.0F) : 0.0F;
   }

   private static int applyAlpha(int rgba, float alpha) {
      int baseAlpha = rgba >>> 24 & 0xFF;
      int finalAlpha = Math.round(baseAlpha * clamp01(alpha));
      return finalAlpha << 24 | rgba & 16777215;
   }

   private static int modulateColor(int rgba, float alphaMultiplier) {
      int a = rgba >>> 24 & 0xFF;
      int modulated = Math.round(a * clamp01(alphaMultiplier));
      return modulated << 24 | rgba & 16777215;
   }

   @Environment(EnvType.CLIENT)
   private record Rect(float x, float y, float width, float height) {
      private static final ColorPickerWidget.Rect EMPTY = new ColorPickerWidget.Rect(0.0F, 0.0F, 0.0F, 0.0F);

      private Rect(float x, float y, float width, float height) {
         if (!Float.isFinite(x)) {
            x = 0.0F;
         }

         if (!Float.isFinite(y)) {
            y = 0.0F;
         }

         if (!Float.isFinite(width) || width < 0.0F) {
            width = 0.0F;
         }

         if (!Float.isFinite(height) || height < 0.0F) {
            height = 0.0F;
         }

         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      private boolean contains(double px, double py) {
         return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
      }

      private float centerX() {
         return this.x + this.width * 0.5F;
      }

      private float centerY() {
         return this.y + this.height * 0.5F;
      }

      private float right() {
         return this.x + this.width;
      }

      private float bottom() {
         return this.y + this.height;
      }
   }
}