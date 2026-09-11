package sl.selene.ui.gui.widget.settings;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.ui.gui.component.render.GlassStyle;
import sl.selene.util.render.animation.AnimationSystem;
import sl.selene.util.render.animation.Easings;
import sl.selene.util.render.animation.SpringConfig;
import sl.selene.util.render.animation.SpringFloatAnimator;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.utils.Color;

@Environment(EnvType.CLIENT)
public final class BooleanWidget implements SettingWidget {
   private static final float ROW_HEIGHT = 62.0F;
   private static final float TOGGLE_WIDTH = 26.0F;
   private static final float TOGGLE_HEIGHT = 13.0F;
   private static final SpringConfig TOGGLE_SPRING = SpringConfig.of(2.1F, 0.55F);
   private static final SpringConfig HOVER_SPRING = SpringConfig.of(1.4F, 0.7F);
   private final Module module;
   private final BooleanSetting setting;
   private final PopupOpenContext popupContext;
   private final SettingValueAccessor<Boolean> valueAccessor;
   private final String labelOverride;
   private final SpringFloatAnimator toggleAnimator;
   private final SpringFloatAnimator hoverAnimator;
   private BooleanWidget.Rect rowBounds = BooleanWidget.Rect.EMPTY;
   private BooleanWidget.Rect toggleBounds = BooleanWidget.Rect.EMPTY;
   private float textX = 0.0F;
   private float textBaseline = 0.0F;
   private boolean hovered = false;

   public BooleanWidget(Module module, PopupOpenContext popupContext, BooleanSetting setting, SettingValueAccessor<?> valueAccessor) {
      this(module, popupContext, setting, valueAccessor, null);
   }

   public BooleanWidget(Module module, PopupOpenContext popupContext, BooleanSetting setting, SettingValueAccessor<?> valueAccessor, String labelOverride) {
      this.module = Objects.requireNonNull(module, "module");
      this.popupContext = Objects.requireNonNull(popupContext, "popupContext");
      this.setting = Objects.requireNonNull(setting, "setting");
      this.valueAccessor = Objects.requireNonNull((SettingValueAccessor<Boolean>)valueAccessor, "valueAccessor");
      this.labelOverride = normalizeLabel(labelOverride);
      Object initialValueObj = valueAccessor.get();
      boolean initialState = initialValueObj instanceof Boolean ? (Boolean)initialValueObj : false;
      float initialValue = initialState ? 1.0F : 0.0F;
      this.toggleAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), TOGGLE_SPRING, initialValue, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.toggleAnimator.setOutputTransform(Easings.EASE_OUT_CUBIC);
      this.hoverAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), HOVER_SPRING, initialValue, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.hoverAnimator.setOutputTransform(Easings.SMOOTH_STEP);
   }

   @Override
   public void updateState() {
      Object value = this.valueAccessor.get();
      boolean boolValue = value instanceof Boolean ? (Boolean)value : false;
      this.toggleAnimator.setTarget(boolValue ? 1.0F : 0.0F);
      this.updateHighlightTarget();
   }

   @Override
   public void layout(float panelX, float top, float panelWidth) {
      this.rowBounds = new BooleanWidget.Rect(panelX, top, panelWidth, ROW_HEIGHT);
      float toggleX = panelX + panelWidth - 18.0F - TOGGLE_WIDTH;
      float toggleY = top + (ROW_HEIGHT - TOGGLE_HEIGHT) * 0.5F;
      this.toggleBounds = new BooleanWidget.Rect(toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT);
      this.textX = panelX + 18.0F;
      this.textBaseline = top + 31.0F + 5.0F;
   }

   @Override
   public float getHeight() {
      return 62.0F;
   }

   @Override
   public void render(Renderer2D renderer, float alphaMultiplier, float expansionProgress, float scrollOffset) {
      float effectiveAlpha = alphaMultiplier * (float)clamp01(expansionProgress);
      if (!(effectiveAlpha <= 0.0F)) {
         float toggleProgress = this.toggleAnimator.getValue();
         GlassStyle.toggle(renderer, this.toggleBounds.x, this.toggleBounds.y, this.toggleBounds.width, this.toggleBounds.height, effectiveAlpha, toggleProgress);

         double textAlpha = clamp01(effectiveAlpha);
         float highlightProgress = this.hoverAnimator.getValue();
         int baseTextColor = Color.getRGB(8947848, textAlpha);
         int highlightedTextColor = Color.getRGB(16777215, textAlpha);
         int textColor = Color.lerp(baseTextColor, highlightedTextColor, highlightProgress);
         renderer.text(FontRegistry.INTER_SEMIBOLD, this.textX, this.textBaseline, 18.0F, this.resolveLabel(), textColor, "l");
      }
   }

   @Override
   public boolean handleMouseClick(double mouseX, double mouseY, int button) {
      if (!this.rowBounds.contains(mouseX, mouseY)) {
         return false;
      } else if (button == 2) {
         Object initialValueObj = this.valueAccessor.get();
         Boolean initialValue = initialValueObj instanceof Boolean ? (Boolean)initialValueObj : false;
         this.popupContext.openForSetting(this.module, this.setting, mouseX, mouseY, initialValue);
         return true;
      } else if (button != 0) {
         return false;
      } else {
         Object currentValueObj = this.valueAccessor.get();
         boolean currentValue = currentValueObj instanceof Boolean ? (Boolean)currentValueObj : false;
         boolean newValue = !currentValue;
         this.valueAccessor.set(newValue);
         this.toggleAnimator.setTarget(newValue ? 1.0F : 0.0F);
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
   public void updateHoverState(double mouseX, double mouseY) {
      this.hovered = this.rowBounds.contains(mouseX, mouseY);
      this.updateHighlightTarget();
   }

   private void updateHighlightTarget() {
      Object valueObj = this.valueAccessor.get();
      boolean value = valueObj instanceof Boolean ? (Boolean)valueObj : false;
      float target;
      if (value) {
         target = 1.0F;
      } else if (this.hovered) {
         target = 0.5F;
      } else {
         target = 0.0F;
      }

      this.hoverAnimator.setTarget(target);
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

   private static double clamp01(double value) {
      if (value <= 0.0) {
         return 0.0;
      } else {
         return value >= 1.0 ? 1.0 : value;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Rect(float x, float y, float width, float height) {
      private static final BooleanWidget.Rect EMPTY = new BooleanWidget.Rect(0.0F, 0.0F, 0.0F, 0.0F);

      private boolean contains(double px, double py) {
         return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
      }

      private float centerX() {
         return this.x + this.width * 0.5F;
      }

      private float centerY() {
         return this.y + this.height * 0.5F;
      }
   }
}