package sl.selene.ui.gui.widget.settings;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.ListSetting;
import sl.selene.ui.Colors;
import sl.selene.util.render.animation.AnimationSystem;
import sl.selene.util.render.animation.Easings;
import sl.selene.util.render.animation.SpringConfig;
import sl.selene.util.render.animation.SpringFloatAnimator;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.UiIcons;
import sl.selene.util.render.utils.Color;

@Environment(EnvType.CLIENT)
public final class SelectableWidget implements SettingWidget {
   private static final SpringConfig HOVER_SPRING = SpringConfig.of(1.4F, 0.7F);
   private static final SpringConfig OPEN_SPRING = SpringConfig.of(2.1F, 0.55F);
   private static final SpringConfig SELECTION_SPRING = SpringConfig.of(2.2F, 0.6F);
   private final Module module;
   private final ListSetting setting;
   private final PopupOpenContext popupContext;
   private final SettingValueAccessor<Set<String>> valueAccessor;
   private final String labelOverride;
   private final List<String> options;
   private final SpringFloatAnimator highlightAnimator;
   private final SpringFloatAnimator openAnimator;
   private final List<SpringFloatAnimator> optionHoverAnimators;
   private final List<SpringFloatAnimator> optionSelectionAnimators;
   private final List<SpringFloatAnimator> optionPrefixAnimators;
   private final List<Integer> visibleSelectionOrder;
   private SelectableWidget.Rect widgetBounds = SelectableWidget.Rect.EMPTY;
   private SelectableWidget.Rect fieldBounds = SelectableWidget.Rect.EMPTY;
   private SelectableWidget.Rect arrowBounds = SelectableWidget.Rect.EMPTY;
   private SelectableWidget.Rect popupBounds = SelectableWidget.Rect.EMPTY;
   private final List<SelectableWidget.Rect> optionBounds = new ArrayList<>();
   private float labelX = 0.0F;
   private float labelBaseline = 0.0F;
   private boolean hovered = false;
   private boolean open = false;
   private int hoveredOption = -1;
   private boolean hasSelectedValues = false;
   private final LinkedHashSet<String> currentSelections = new LinkedHashSet<>();

   public SelectableWidget(Module module, PopupOpenContext popupContext, ListSetting setting, SettingValueAccessor<Set<String>> valueAccessor) {
      this(module, popupContext, setting, valueAccessor, null);
   }

   public SelectableWidget(
      Module module, PopupOpenContext popupContext, ListSetting setting, SettingValueAccessor<Set<String>> valueAccessor, String labelOverride
   ) {
      this.module = Objects.requireNonNull(module, "module");
      this.popupContext = Objects.requireNonNull(popupContext, "popupContext");
      this.setting = Objects.requireNonNull(setting, "setting");
      this.valueAccessor = Objects.requireNonNull(valueAccessor, "valueAccessor");
      this.labelOverride = normalizeLabel(labelOverride);
      this.options = new ArrayList<>(setting.list != null ? setting.list : List.of());
      this.highlightAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), HOVER_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.highlightAnimator.setOutputTransform(Easings.SMOOTH_STEP);
      this.openAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), OPEN_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      this.openAnimator.setOutputTransform(Easings.SMOOTH_STEP);
      this.optionHoverAnimators = new ArrayList<>();
      this.optionSelectionAnimators = new ArrayList<>();
      this.optionPrefixAnimators = new ArrayList<>();
      this.visibleSelectionOrder = new ArrayList<>();
      this.initializeOptionAnimators();
   }

   @Override
   public void updateState() {
      List<String> currentOptions = this.setting.list != null ? this.setting.list : List.of();
      if (currentOptions.size() != this.options.size() || !this.options.equals(currentOptions)) {
         this.options.clear();
         this.options.addAll(currentOptions);
         this.initializeOptionAnimators();
         this.recomputePopupBounds();
      }

      this.highlightAnimator.setTarget(this.open ? 1.0F : (this.hovered ? 0.5F : 0.0F));
      this.openAnimator.setTarget(this.open ? 1.0F : 0.0F);
      this.currentSelections.clear();
      Set<String> accessorValue = this.valueAccessor.get();
      if (accessorValue != null) {
         this.currentSelections.addAll(accessorValue);
      }

      this.hasSelectedValues = !this.currentSelections.isEmpty();

      for (int i = 0; i < this.optionHoverAnimators.size(); i++) {
         float hoverTarget = this.open && i == this.hoveredOption ? 1.0F : 0.0F;
         this.optionHoverAnimators.get(i).setTarget(hoverTarget);
      }

      for (int i = 0; i < this.optionSelectionAnimators.size(); i++) {
         String option = this.options.get(i);
         float selectionTarget = this.currentSelections.contains(option) ? 1.0F : 0.0F;
         this.optionSelectionAnimators.get(i).setTarget(selectionTarget);
      }

      boolean hasActiveBefore = false;

      for (int i = 0; i < this.optionPrefixAnimators.size(); i++) {
         SpringFloatAnimator selectionAnimator = this.optionSelectionAnimators.get(i);
         SpringFloatAnimator prefixAnimator = this.optionPrefixAnimators.get(i);
         float currentSelection = clamp01(selectionAnimator.getValue());
         float targetSelection = selectionAnimator.getTarget();
         boolean currentlyContributes = currentSelection > 1.0E-4F;
         boolean willContribute = targetSelection > 1.0E-4F;
         boolean isAnimating = Math.abs(currentSelection - targetSelection) > 1.0E-4F;
         boolean shouldShowPrefix = hasActiveBefore && (currentlyContributes || isAnimating && willContribute);
         prefixAnimator.setTarget(shouldShowPrefix ? 1.0F : 0.0F);
         boolean contributes = currentlyContributes || willContribute;
         hasActiveBefore = hasActiveBefore || contributes;
      }

      if (!this.open) {
         this.hoveredOption = -1;
      }
   }

   @Override
   public void layout(float panelX, float top, float panelWidth) {
      this.widgetBounds = new SelectableWidget.Rect(panelX, top, panelWidth, 100.0F);
      this.labelX = panelX + 18.0F;
      this.labelBaseline = top + 17.0F + 18.0F;
      float fieldX = panelX + 18.0F;
      float fieldY = this.labelBaseline + 8.0F;
      this.fieldBounds = new SelectableWidget.Rect(fieldX, fieldY, 298.0F, 38.0F);
      this.arrowBounds = new SelectableWidget.Rect(fieldX + 298.0F - 40.0F, fieldY, 40.0F, 38.0F);
      this.recomputePopupBounds();
   }

   @Override
   public float getHeight() {
      return 100.0F;
   }

   @Override
   public void render(Renderer2D renderer, float alphaMultiplier, float expansionProgress, float scrollOffset) {
      float expansion = clamp01(expansionProgress);
      float effectiveAlpha = alphaMultiplier * expansion;
      if (!(effectiveAlpha <= 1.0E-4F)) {
         float highlightProgress = clamp01(this.highlightAnimator.getValue());
         int labelBaseColor = applyAlpha(-7829368, effectiveAlpha);
         int labelHighlightColor = applyAlpha(-1, effectiveAlpha);
         int labelColor = Color.lerp(labelBaseColor, labelHighlightColor, highlightProgress);
         renderer.text(FontRegistry.INTER_SEMIBOLD, this.labelX, this.labelBaseline - 4.0F, 18.0F, this.resolveLabel(), labelColor, "l");
         int fieldColor = applyAlpha(-14408668, effectiveAlpha);
         renderer.rect(this.fieldBounds.x, this.fieldBounds.y, this.fieldBounds.width, this.fieldBounds.height, 6.0F, fieldColor);
         int arrowColor = applyAlpha(Colors.getClientPrimary(), effectiveAlpha);
         renderer.rect(this.arrowBounds.x, this.arrowBounds.y, this.arrowBounds.width, this.arrowBounds.height, 0.0F, 6.0F, 6.0F, 0.0F, arrowColor);
         float valueBaseline = this.fieldBounds.centerY() + 6.0F;
         float valueTextX = this.fieldBounds.x + 16.0F;
         int valueBaseColor = applyAlpha(-7829368, effectiveAlpha);
         int valueHighlightColor = applyAlpha(-1, effectiveAlpha);
         int valueColor = Color.lerp(valueBaseColor, valueHighlightColor, highlightProgress);
         float clipWidth = this.arrowBounds.x - valueTextX - 10.0F;
         float clipHeight = this.fieldBounds.height - 8.0F;
         if (clipWidth > 0.0F && clipHeight > 0.0F) {
            int clipX = Math.round(valueTextX);
            int clipY = Math.round(this.fieldBounds.y + 4.0F);
            int clipW = Math.max(0, Math.round(clipWidth));
            int clipH = Math.max(0, Math.round(clipHeight));
            renderer.pushClipRect(clipX, clipY, clipW, clipH);

            try {
               this.visibleSelectionOrder.clear();

               for (int i = 0; i < this.options.size() && i < this.optionSelectionAnimators.size(); i++) {
                  SpringFloatAnimator selectionAnimator = this.optionSelectionAnimators.get(i);
                  float selectionProgress = clamp01(selectionAnimator.getValue());
                  float targetSelection = selectionAnimator.getTarget();
                  boolean isVisible = selectionProgress > 1.0E-4F;
                  boolean isAnimatingOut = selectionProgress > 1.0E-4F && targetSelection <= 1.0E-4F;
                  if (isVisible || isAnimatingOut) {
                     this.visibleSelectionOrder.add(i);
                  }
               }

               if (!this.hasSelectedValues && this.visibleSelectionOrder.isEmpty()) {
                  int placeholderColor = applyAlpha(-11184811, effectiveAlpha);
                  renderer.text(FontRegistry.INTER_SEMIBOLD, valueTextX, valueBaseline, 16.0F, "Select values", placeholderColor, "l");
               } else {
                  float cursorX = valueTextX;
                  float separatorFullWidth = renderer.measureText(FontRegistry.INTER_SEMIBOLD, ", ", 16.0F).width;

                  for (int listIndex = 0; listIndex < this.visibleSelectionOrder.size(); listIndex++) {
                     int optionIndex = this.visibleSelectionOrder.get(listIndex);
                     SpringFloatAnimator selectionAnimator = this.optionSelectionAnimators.get(optionIndex);
                     float selectionProgress = clamp01(selectionAnimator.getValue());
                     if (!(selectionProgress <= 1.0E-4F)) {
                        float prefixProgress = this.optionPrefixAnimators.size() > optionIndex
                           ? clamp01(this.optionPrefixAnimators.get(optionIndex).getValue())
                           : 0.0F;
                        String selection = this.options.get(optionIndex);
                        float appearOffset = (1.0F - selectionProgress) * 12.0F;
                        int selectionColor = applyAlpha(valueColor, selectionProgress);
                        float effectivePrefixProgress = prefixProgress * selectionProgress;
                        if (effectivePrefixProgress > 1.0E-4F) {
                           int prefixColor = applyAlpha(valueColor, effectivePrefixProgress);
                           renderer.text(FontRegistry.INTER_SEMIBOLD, cursorX + appearOffset, valueBaseline, 16.0F, ", ", prefixColor, "l");
                           cursorX += Math.max(0.0F, separatorFullWidth * effectivePrefixProgress);
                        }

                        float optionWidth = renderer.measureText(FontRegistry.INTER_SEMIBOLD, selection, 16.0F).width;
                        renderer.text(FontRegistry.INTER_SEMIBOLD, cursorX + appearOffset, valueBaseline, 16.0F, selection, selectionColor, "l");
                        cursorX += Math.max(0.0F, optionWidth * selectionProgress);
                     }
                  }
               }
            } finally {
               renderer.popClipRect();
            }
         }

         UiIcons.chevron(
            renderer,
            this.arrowBounds.centerX(),
            this.arrowBounds.centerY(),
            14.0F,
            this.openAnimator.getValue() > 0.5F,
            effectiveAlpha
         );
      }
   }

   @Override
   public void renderOverlay(Renderer2D renderer, float alphaMultiplier, float expansionProgress) {
      float openProgress = clamp01(this.openAnimator.getValue());
      if (!(openProgress <= 0.001F)) {
         if (!this.options.isEmpty() && !(this.popupBounds.width <= 0.0F) && !(this.popupBounds.height <= 0.0F)) {
            float expansion = clamp01(expansionProgress);
            float effectiveAlpha = alphaMultiplier * expansion * openProgress;
            if (!(effectiveAlpha <= 1.0E-4F)) {
               int popupColor = -14408668;
               renderer.pushScale(1.0F, openProgress, this.popupBounds.x, this.popupBounds.y);

               try {
                  renderer.rect(this.popupBounds.x, this.popupBounds.y, this.popupBounds.width, this.popupBounds.height, 6.0F, 6.0F, 6.0F, 6.0F, popupColor);

                  for (int i = 0; i < this.optionBounds.size(); i++) {
                     SelectableWidget.Rect bounds = this.optionBounds.get(i);
                     String option = this.options.get(i);
                     boolean isSelected = this.currentSelections.contains(option);
                     float hoverProgress = i < this.optionHoverAnimators.size() ? clamp01(this.optionHoverAnimators.get(i).getValue()) : 0.0F;
                     if (hoverProgress > 0.001F) {
                        int hoverColor = applyAlpha(-13750738, hoverProgress * effectiveAlpha);
                        float topLeft = i == 0 ? 6.0F : 0.0F;
                        float topRight = i == 0 ? 6.0F : 0.0F;
                        float bottomRight = i == this.optionBounds.size() - 1 ? 6.0F : 0.0F;
                        float bottomLeft = i == this.optionBounds.size() - 1 ? 6.0F : 0.0F;
                        renderer.rect(bounds.x, bounds.y, bounds.width, bounds.height, topLeft, topRight, bottomRight, bottomLeft, hoverColor);
                     }

                     float textX = bounds.x + 16.0F;
                     float textBaseline = bounds.centerY() + 6.0F;
                     int textBaseColor = applyAlpha(-7829368, effectiveAlpha);
                     int textHighlightColor = applyAlpha(-1, effectiveAlpha);
                     float textHighlightFactor = isSelected ? 1.0F : hoverProgress * 0.7F;
                     int textColor = Color.lerp(textBaseColor, textHighlightColor, textHighlightFactor);
                     renderer.text(FontRegistry.INTER_SEMIBOLD, textX, textBaseline, 16.0F, option, textColor, "l");
                     float selectionProgress = i < this.optionSelectionAnimators.size()
                        ? clamp01(this.optionSelectionAnimators.get(i).getValue())
                        : (isSelected ? 1.0F : 0.0F);
                     if (selectionProgress > 1.0E-4F) {
                        UiIcons.check(renderer, bounds.x + bounds.width - 15.0F, bounds.centerY(), 14.0F, effectiveAlpha * selectionProgress);
                     }
                  }
               } finally {
                  renderer.popScale();
               }
            }
         }
      }
   }

   @Override
   public boolean isBlockingInteractions() {
      return (this.open || this.openAnimator.getValue() > 0.001F) && this.popupBounds.width > 0.0F && this.popupBounds.height > 0.0F;
   }

   @Override
   public boolean handleOverlayClick(double mouseX, double mouseY, int button) {
      if (!this.isBlockingInteractions()) {
         return false;
      } else if (!this.open) {
         return false;
      } else if (button != 0) {
         this.closePopup();
         return true;
      } else if (this.popupBounds.contains(mouseX, mouseY)) {
         int index = this.indexAt(mouseY);
         if (index >= 0 && index < this.options.size()) {
            this.toggleOption(index);
         }

         return true;
      } else if (!this.fieldBounds.contains(mouseX, mouseY) && !this.arrowBounds.contains(mouseX, mouseY)) {
         this.closePopup();
         return true;
      } else {
         this.closePopup();
         return true;
      }
   }

   @Override
   public boolean handleMouseClick(double mouseX, double mouseY, int button) {
      boolean insidePrimary = this.fieldBounds.contains(mouseX, mouseY) || this.arrowBounds.contains(mouseX, mouseY);
      if (button == 2) {
         if (!insidePrimary) {
            return false;
         } else {
            this.closePopup();
            Set<String> initialValue = new LinkedHashSet<>(this.currentSelections);
            this.popupContext.openForSetting(this.module, this.setting, mouseX, mouseY, initialValue);
            return true;
         }
      } else if (button != 0) {
         return false;
      } else if (this.open) {
         return this.handleOverlayClick(mouseX, mouseY, button);
      } else if (insidePrimary) {
         this.openPopup();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean handleOverlayScroll(double mouseX, double mouseY, double horizontal, double vertical) {
      return this.isBlockingInteractions();
   }

   @Override
   public void updateHoverState(double mouseX, double mouseY) {
      boolean baseHover = this.fieldBounds.contains(mouseX, mouseY) || this.arrowBounds.contains(mouseX, mouseY);
      boolean popupHover = this.open && this.popupBounds.contains(mouseX, mouseY);
      this.hovered = this.open ? false : baseHover;
      if (this.open) {
         if (popupHover) {
            this.hoveredOption = this.indexAt(mouseY);
         } else {
            this.hoveredOption = -1;
         }
      }
   }

   @Override
   public void collapse() {
      this.closePopupImmediate();
   }

   @Override
   public Setting getSetting() {
      return this.setting;
   }

   @Override
   public boolean supportsBinding() {
      return true;
   }

   private void openPopup() {
      this.open = true;
      this.openAnimator.setTarget(1.0F);
      this.highlightAnimator.setTarget(1.0F);
   }

   private void closePopup() {
      this.open = false;
      this.openAnimator.setTarget(0.0F);
      this.hoveredOption = -1;
   }

   private void closePopupImmediate() {
      this.open = false;
      this.openAnimator.snapTo(0.0F);
      this.hoveredOption = -1;
   }

   private void toggleOption(int index) {
      if (index >= 0 && index < this.options.size()) {
         String option = this.options.get(index);
         LinkedHashSet<String> updated = new LinkedHashSet<>(this.currentSelections);
         if (updated.contains(option)) {
            updated.remove(option);
         } else {
            updated.add(option);
         }

         this.valueAccessor.set(updated);
         this.currentSelections.clear();
         this.currentSelections.addAll(updated);
      }
   }

   private void recomputePopupBounds() {
      this.optionBounds.clear();
      if (this.options.isEmpty()) {
         this.popupBounds = SelectableWidget.Rect.EMPTY;
      } else {
         float popupX = this.fieldBounds.x;
         float popupY = this.fieldBounds.y + this.fieldBounds.height + 6.0F;
         float popupWidth = this.fieldBounds.width;
         float popupHeight = 38.0F * this.options.size();
         this.popupBounds = new SelectableWidget.Rect(popupX, popupY, popupWidth, popupHeight);
         float cursor = popupY;

         for (int i = 0; i < this.options.size(); i++) {
            this.optionBounds.add(new SelectableWidget.Rect(popupX, cursor, popupWidth, 38.0F));
            cursor += 38.0F;
         }
      }
   }

   private int indexAt(double mouseY) {
      if (!(mouseY < this.popupBounds.y) && !(mouseY > this.popupBounds.y + this.popupBounds.height)) {
         double relative = mouseY - this.popupBounds.y;
         if (relative < 0.0) {
            return -1;
         } else {
            int index = (int)(relative / 38.0);
            return index >= 0 && index < this.options.size() ? index : -1;
         }
      } else {
         return -1;
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

   private void initializeOptionAnimators() {
      this.optionHoverAnimators.clear();
      this.optionSelectionAnimators.clear();
      this.optionPrefixAnimators.clear();
      this.visibleSelectionOrder.clear();

      for (int i = 0; i < this.options.size(); i++) {
         SpringFloatAnimator hoverAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), HOVER_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
         hoverAnimator.setOutputTransform(Easings.SMOOTH_STEP);
         this.optionHoverAnimators.add(hoverAnimator);
         SpringFloatAnimator selectionAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), SELECTION_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
         selectionAnimator.setOutputTransform(Easings.SMOOTH_STEP);
         this.optionSelectionAnimators.add(selectionAnimator);
         SpringFloatAnimator prefixAnimator = new SpringFloatAnimator(AnimationSystem.getInstance(), SELECTION_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
         prefixAnimator.setOutputTransform(Easings.SMOOTH_STEP);
         this.optionPrefixAnimators.add(prefixAnimator);
      }
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

   @Environment(EnvType.CLIENT)
   private record Rect(float x, float y, float width, float height) {
      private static final SelectableWidget.Rect EMPTY = new SelectableWidget.Rect(0.0F, 0.0F, 0.0F, 0.0F);

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