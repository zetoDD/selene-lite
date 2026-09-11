package sl.selene.ui.gui.widget.settings;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.ui.Colors;
import sl.selene.ui.gui.component.render.GlassStyle;
import sl.selene.util.render.animation.AnimationSystem;
import sl.selene.util.render.animation.Easings;
import sl.selene.util.render.animation.SpringConfig;
import sl.selene.util.render.animation.SpringFloatAnimator;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.utils.Color;

@Environment(EnvType.CLIENT)
public final class SliderWidget implements SettingWidget {
   private static final SpringConfig INTERACTION_SPRING = SpringConfig.of(2.1F, 0.55F);
   private static final SpringConfig HOVER_SPRING = SpringConfig.of(1.4F, 0.7F);
   private static final SpringConfig PROGRESS_SPRING = SpringConfig.of(8.0F, 0.8F);
   private static final SpringConfig VISIBILITY_SPRING = SpringConfig.of(1.8F, 0.65F);
   private static final int TRACK_COLOR = 0xFFFFFFFF;
   private static final int KNOB_COLOR = 0xFFFFFFFF;
   private final Module module;
   private final SliderSetting setting;
   private final PopupOpenContext popupContext;
   private final SettingValueAccessor<Double> valueAccessor;
   private final String labelOverride;
   private final SpringFloatAnimator knobScaleAnimator;
   private final SpringFloatAnimator highlightAnimator;
   private final SpringFloatAnimator progressAnimator;
   private final SpringFloatAnimator visibilityAnimator;
   private SliderWidget.Rect layoutBounds = SliderWidget.Rect.EMPTY;
   private SliderWidget.Rect interactionBounds = SliderWidget.Rect.EMPTY;
   private SliderWidget.Rect trackBounds = SliderWidget.Rect.EMPTY;
   private boolean dragging = false;
   private double currentValue;
   private int valuePrecision;
   private boolean hovered = false;

   public SliderWidget(Module module, PopupOpenContext popupContext, SliderSetting setting, SettingValueAccessor<Double> valueAccessor) {
      this(module, popupContext, setting, valueAccessor, null);
   }

   public SliderWidget(Module module, PopupOpenContext popupContext, SliderSetting setting, SettingValueAccessor<Double> valueAccessor, String labelOverride) {
      this.module = Objects.requireNonNull(module, "module");
      this.popupContext = Objects.requireNonNull(popupContext, "popupContext");
      this.setting = Objects.requireNonNull(setting, "setting");
      this.valueAccessor = Objects.requireNonNull(valueAccessor, "valueAccessor");
      this.labelOverride = normalizeLabel(labelOverride);
      this.knobScaleAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), INTERACTION_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.knobScaleAnimator.setOutputTransform(Easings.SMOOTH_STEP);
      this.highlightAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), HOVER_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.highlightAnimator.setOutputTransform(Easings.SMOOTH_STEP);
      Double valueObj = valueAccessor.get();
      double resolvedValue = valueObj != null ? valueObj : setting.current;
      this.currentValue = resolvedValue;
      this.valuePrecision = computePrecision(setting.increment);
      float initialProgress = this.computeProgress(this.currentValue);
      this.progressAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), PROGRESS_SPRING, initialProgress, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.visibilityAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), VISIBILITY_SPRING, 1.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.visibilityAnimator.setOutputTransform(Easings.SMOOTH_STEP);
   }

   @Override
   public void updateState() {
      Double valueObj = this.valueAccessor.get();
      double resolvedValue = valueObj != null ? valueObj : this.setting.current;
      this.currentValue = resolvedValue;
      this.knobScaleAnimator.setTarget(this.dragging ? 1.0F : 0.0F);
      this.progressAnimator.setTarget(this.computeProgress());
      this.updateHighlightTarget();
   }

   @Override
   public void updateVisibility(boolean visible) {
      float target = visible ? 1.0F : 0.0F;
      this.visibilityAnimator.setTarget(target);
      if (!visible) {
         this.dragging = false;
         this.hovered = false;
         this.knobScaleAnimator.setTarget(0.0F);
         this.updateHighlightTarget();
      }
   }

   @Override
   public void layout(float panelX, float top, float panelWidth) {
      float currentHeight = Math.max(0.0F, this.getCurrentHeight());
      this.layoutBounds = new SliderWidget.Rect(panelX, top, panelWidth, 80.0F);
      this.interactionBounds = new SliderWidget.Rect(panelX, top, panelWidth, currentHeight);
      float trackX = panelX + 18.0F;
      float trackY = top + 17.0F + 18.0F + 15.0F;
      this.trackBounds = new SliderWidget.Rect(trackX, trackY, 298.0F, 6.0F);
   }

   @Override
   public float getHeight() {
      return 80.0F;
   }

   @Override
   public float getCurrentHeight() {
      return 80.0F * clamp01(this.visibilityAnimator.getValue());
   }

   @Override
   public void render(Renderer2D renderer, float alphaMultiplier, float expansionProgress, float scrollOffset) {
      float visibility = clamp01(this.visibilityAnimator.getValue());
      if (!(visibility <= 0.001F)) {
         float effectiveAlpha = alphaMultiplier * clamp01(expansionProgress) * visibility;
         if (!(effectiveAlpha <= 1.0E-4F)) {
            renderer.pushScale(1.0F, visibility, this.layoutBounds.x, this.layoutBounds.y);

            try {
               this.updateDraggingState();
               float textProgress = clamp01(this.highlightAnimator.getValue());
               int lerpedTextColor = Color.lerp(-7829368, -1, textProgress);
               int textColor = applyAlpha(lerpedTextColor, effectiveAlpha);
               float labelX = this.layoutBounds.x + 18.0F;
               float labelBaseline = this.layoutBounds.y + 17.0F + 18.0F;
               renderer.text(FontRegistry.INTER_SEMIBOLD, labelX, labelBaseline, 18.0F, this.resolveLabel(), textColor, "l");
               float valueBaseline = this.layoutBounds.y + 20.0F + 18.0F;
               float valueX = this.layoutBounds.x + this.layoutBounds.width - 18.0F;
               renderer.text(FontRegistry.INTER_SEMIBOLD, valueX, valueBaseline, 18.0F, this.formatCurrentValue(), textColor, "r");
               float progress = clamp01(this.progressAnimator.getValue());
               int trackColor = applyAlpha(TRACK_COLOR, effectiveAlpha * 0.06F);
               int fillColor = applyAlpha(Colors.getClientPrimary(), effectiveAlpha);
               int knobColor = applyAlpha(KNOB_COLOR, effectiveAlpha);
               GlassStyle.outline(renderer, this.trackBounds.x, this.trackBounds.y, this.trackBounds.width, this.trackBounds.height, this.trackBounds.height * 0.5F, effectiveAlpha);
               GlassStyle.sliderTrack(renderer, this.trackBounds.x, this.trackBounds.y, this.trackBounds.width, this.trackBounds.height, trackColor, fillColor, progress);
               float knobScale = 1.0F + clamp01(this.knobScaleAnimator.getValue()) * 0.25F;
               float knobHeight = this.trackBounds.height * knobScale;
               float knobY = this.trackBounds.y + (this.trackBounds.height - knobHeight) * 0.5F;
               GlassStyle.sliderKnob(renderer, GlassStyle.sliderKnobX(this.trackBounds.x, this.trackBounds.width, progress), knobY, knobHeight, knobColor);
            } finally {
               renderer.popScale();
            }
         }
      }
   }

   @Override
   public boolean handleMouseClick(double mouseX, double mouseY, int button) {
      if (!this.isInteractable() || !this.interactionBounds.contains(mouseX, mouseY)) {
         return false;
      } else if (button == 2) {
         Double valueObj = this.valueAccessor.get();
         Double initialValue = valueObj != null ? valueObj : this.setting.current;
         this.popupContext.openForSetting(this.module, this.setting, mouseX, mouseY, initialValue);
         return true;
      } else if (button != 0) {
         return false;
      } else {
         this.dragging = true;
         this.knobScaleAnimator.setTarget(1.0F);
         this.updateHighlightTarget();
         return true;
      }
   }

   @Override
   public Setting getSetting() {
      return this.setting;
   }

   @Override
   public boolean supportsBinding() {
      return true;
   }

   @Override
   public boolean handleMouseScroll(double mouseX, double mouseY, double horizontal, double vertical) {
      if (this.isInteractable() && this.interactionBounds.contains(mouseX, mouseY)) {
         if (Math.abs(vertical) <= 1.0E-4) {
            return false;
         } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
               long windowHandle = client.getWindow().getHandle();
               if (GLFW.glfwGetKey(windowHandle, 341) != 1) {
                  return false;
               } else if (!this.applyScrollDelta(vertical)) {
                  return false;
               } else {
                  this.knobScaleAnimator.setTarget(1.0F);
                  this.updateHighlightTarget();
                  return true;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   @Override
   public void updateHoverState(double mouseX, double mouseY) {
      if (!this.isInteractable()) {
         this.hovered = false;
         this.updateHighlightTarget();
      } else {
         this.hovered = this.interactionBounds.contains(mouseX, mouseY);
         this.updateHighlightTarget();
      }
   }

   private void updateDraggingState() {
      if (this.dragging) {
         if (!this.isInteractable()) {
            this.dragging = false;
            this.knobScaleAnimator.setTarget(0.0F);
            this.updateHighlightTarget();
         } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
               long handle = client.getWindow().getHandle();
               if (GLFW.glfwGetMouseButton(handle, 0) != 1) {
                  this.dragging = false;
                  this.knobScaleAnimator.setTarget(0.0F);
                  this.updateHighlightTarget();
               } else {
                  double[] cursorX = new double[1];
                  double[] cursorY = new double[1];
                  GLFW.glfwGetCursorPos(handle, cursorX, cursorY);
                  this.updateValueFromCursor(adjustCursorX(cursorX[0], client));
               }
            } else {
               this.dragging = false;
               this.knobScaleAnimator.setTarget(0.0F);
               this.updateHighlightTarget();
            }
         }
      }
   }

   private void updateValueFromCursor(double cursorX) {
      if (!(this.trackBounds.width <= 0.0F) && this.isInteractable()) {
         double clamped = Math.min(Math.max(cursorX, (double)this.trackBounds.x), (double)(this.trackBounds.x + this.trackBounds.width));
         double t = (clamped - this.trackBounds.x) / this.trackBounds.width;
         double min = this.setting.minimum;
         double max = this.setting.maximum;
         if (!(max <= min)) {
            double newValue = min + (max - min) * t;
            if (!(Math.abs(newValue - this.currentValue) <= 1.0E-4F)) {
               this.valueAccessor.set(newValue);
               Double resolvedValue = this.valueAccessor.get();
               this.currentValue = resolvedValue != null ? resolvedValue : this.setting.current;
               this.progressAnimator.setTarget(this.computeProgress());
            }
         }
      }
   }

   private boolean applyScrollDelta(double verticalDelta) {
      double step = this.setting.increment;
      if (step <= 0.0) {
         return false;
      } else {
         double min = this.setting.minimum;
         double max = this.setting.maximum;
         double direction = Math.signum(verticalDelta);
         if (direction == 0.0) {
            return false;
         } else {
            double magnitude = Math.ceil(Math.abs(verticalDelta));
            if (magnitude <= 0.0) {
               magnitude = 1.0;
            }

            double candidate = this.currentValue + step * magnitude * direction;
            double clamped = Math.min(Math.max(candidate, min), max);
            if (Math.abs(clamped - this.currentValue) <= 1.0E-4F) {
               return false;
            } else {
               this.valueAccessor.set(clamped);
               Double valueObj = this.valueAccessor.get();
               double resolvedValue = valueObj instanceof Number ? valueObj.doubleValue() : this.setting.current;
               this.currentValue = resolvedValue;
               this.updateHighlightTarget();
               this.progressAnimator.setTarget(this.computeProgress());
               return true;
            }
         }
      }
   }

   private float computeProgress() {
      return this.computeProgress(this.currentValue);
   }

   private float computeProgress(double value) {
      double min = this.setting.minimum;
      double max = this.setting.maximum;
      if (max <= min) {
         return 0.0F;
      } else {
         double clamped = Math.min(Math.max(value, min), max);
         return (float)((clamped - min) / (max - min));
      }
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

   private String formatCurrentValue() {
      return this.valuePrecision <= 0
         ? String.format(Locale.US, "%.0f", this.currentValue)
         : String.format(Locale.US, "%1$." + this.valuePrecision + "f", this.currentValue);
   }

   private static int computePrecision(double step) {
      BigDecimal decimal = BigDecimal.valueOf(step);
      int scale = decimal.scale();
      if (scale <= 0) {
         return 0;
      } else {
         BigDecimal normalized = decimal.stripTrailingZeros();
         return Math.max(0, normalized.scale());
      }
   }

   private void updateHighlightTarget() {
      float target;
      if (this.dragging) {
         target = 1.0F;
      } else if (this.hovered && this.isInteractable()) {
         target = 0.5F;
      } else {
         target = 0.0F;
      }

      this.highlightAnimator.setTarget(target);
   }

   private boolean isInteractable() {
      return clamp01(this.visibilityAnimator.getValue()) > 0.001F;
   }

   private static float clamp01(float value) {
      if (value <= 0.0F) {
         return 0.0F;
      } else {
         return value >= 1.0F ? 1.0F : value;
      }
   }

   private static int applyAlpha(int rgba, float alpha) {
      int originalAlpha = rgba >>> 24 & 0xFF;
      int modulatedAlpha = Math.round(originalAlpha * alpha);
      int rgb = rgba & 16777215;
      return modulatedAlpha << 24 | rgb;
   }

   private static double applyInverseScale(double mouseX, int viewportWidth, float scale) {
      if (viewportWidth <= 0) {
         return mouseX;
      } else if (Float.isFinite(scale) && !(Math.abs(scale - 1.0F) <= 0.001F)) {
         double centerX = viewportWidth * 0.5;
         return centerX + (mouseX - centerX) / scale;
      } else {
         return mouseX;
      }
   }

   private static double adjustCursorX(double rawCursorX, MinecraftClient client) {
      if (client != null && client.getWindow() != null) {
         int viewportWidth = client.getWindow().getFramebufferWidth();
         if (viewportWidth <= 0) {
            return rawCursorX;
         } else {
            float layoutScale = 1.0F;
            return Float.isFinite(layoutScale) && !(Math.abs(layoutScale) <= 1.0E-4F) ? applyInverseScale(rawCursorX, viewportWidth, layoutScale) : rawCursorX;
         }
      } else {
         return rawCursorX;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Rect(float x, float y, float width, float height) {
      private static final SliderWidget.Rect EMPTY = new SliderWidget.Rect(0.0F, 0.0F, 0.0F, 0.0F);

      private boolean contains(double px, double py) {
         return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
      }
   }
}