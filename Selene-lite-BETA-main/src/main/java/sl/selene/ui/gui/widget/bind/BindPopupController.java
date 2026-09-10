package sl.selene.ui.gui.widget.bind;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.Selene;
import sl.selene.event.input.MouseUpdateEvent;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.ListSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.module.bind.BindingManager;
import sl.selene.module.bind.BindingMode;
import sl.selene.client.SeleneKeyBindings;
import sl.selene.ui.gui.widget.bind.editor.BindPopupComboEditor;
import sl.selene.ui.gui.widget.bind.editor.BindPopupSelectableEditor;
import sl.selene.ui.gui.widget.bind.editor.BindPopupSettingEditor;
import sl.selene.ui.gui.widget.bind.editor.BindPopupSliderEditor;
import sl.selene.ui.gui.widget.bind.editor.BindPopupSwitchEditor;
import sl.selene.ui.gui.widget.popup.PopupPositioner;
import sl.selene.ui.gui.widget.settings.HSBColor;
import sl.selene.util.render.animation.AnimationSystem;
import sl.selene.util.render.animation.Easings;
import sl.selene.util.render.animation.SpringConfig;
import sl.selene.util.render.animation.SpringFloatAnimator;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontObject;

@Environment(EnvType.CLIENT)
public final class BindPopupController {
   private static final SpringConfig VISIBILITY_SPRING = SpringConfig.of(2.2F, 0.72F);
   private static final SpringConfig HOVER_SPRING = SpringConfig.of(1.9F, 0.68F);
   private final SpringFloatAnimator visibilityAnimator;
   private final SpringFloatAnimator bindHoverAnimator;
   private final SpringFloatAnimator toggleHoverAnimator;
   private final SpringFloatAnimator holdHoverAnimator;
   private final SpringFloatAnimator toggleSelectionAnimator;
   private final SpringFloatAnimator holdSelectionAnimator;
   private final SpringFloatAnimator blurAnimator;
   private final PopupPositioner popupPositioner;
   private BindPopupModel model;
   private BindPopupRenderer.Layout layout;
   private BindPopupRenderer.Rect fieldRect = new BindPopupRenderer.Rect(0.0F, 0.0F, 0.0F, 0.0F);
   private BindPopupSettingEditor valueEditor;
   private float popupX;
   private float popupY;
   private float anchorX = Float.NaN;
   private float anchorY = Float.NaN;
   private float layoutScale = 1.0F;
   private boolean listening;
   private boolean shouldReleaseModel;
   private boolean conflictActive;
   private long conflictTimestamp;
   private double cursorX = -1.0;
   private double cursorY = -1.0;

   private static SpringFloatAnimator createHoverAnimator() {
      SpringFloatAnimator animator = new SpringFloatAnimator(AnimationSystem.getInstance(), HOVER_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      animator.setOutputTransform(Easings.SMOOTH_STEP);
      return animator;
   }

   private BindPopupController(SpringFloatAnimator visibilityAnimator) {
      this.visibilityAnimator = Objects.requireNonNull(visibilityAnimator, "visibilityAnimator");
      this.bindHoverAnimator = createHoverAnimator();
      this.toggleHoverAnimator = createHoverAnimator();
      this.holdHoverAnimator = createHoverAnimator();
      this.toggleSelectionAnimator = createHoverAnimator();
      this.holdSelectionAnimator = createHoverAnimator();
      this.blurAnimator = createHoverAnimator();
      this.popupPositioner = new PopupPositioner(16.0F, 8.0F, 8.0F);
   }

   private void initializeValueEditor() {
      this.valueEditor = null;
      if (this.model != null && this.model.isSettingTarget()) {
         Setting setting = this.model.setting();
         if (setting instanceof BooleanSetting booleanSetting) {
            this.valueEditor = new BindPopupSwitchEditor(this.model, booleanSetting);
         } else if (setting instanceof SliderSetting sliderSetting) {
            this.valueEditor = new BindPopupSliderEditor(this.model, sliderSetting);
         } else if (setting instanceof ModeSetting modeSetting) {
            this.valueEditor = new BindPopupComboEditor(this.model, modeSetting);
         } else if (setting instanceof ListSetting listSetting) {
            this.valueEditor = new BindPopupSelectableEditor(this.model, listSetting);
         }
      }
   }

   private float resolveValueBlockHeight() {
      return this.valueEditor == null ? 0.0F : Math.max(0.0F, this.valueEditor.getHeight());
   }

   private void layoutValueEditor() {
      if (this.valueEditor != null && this.layout != null) {
         BindPopupRenderer.Rect area = this.layout.valueContent();
         if (!(area.height() <= 0.0F)) {
            this.valueEditor.layout(area);
         }
      }
   }

   private boolean dispatchValueEditorClick(double mouseX, double mouseY, int button) {
      if (this.valueEditor == null) {
         return false;
      } else if (this.valueEditor.isBlocking()) {
         return this.valueEditor.handleMouseClick(mouseX, mouseY, button) ? true : true;
      } else {
         return this.layout != null && this.layout.valueContent().contains(mouseX, mouseY) ? this.valueEditor.handleMouseClick(mouseX, mouseY, button) : false;
      }
   }

   private boolean dispatchValueEditorScroll(double mouseX, double mouseY, double horizontal, double vertical) {
      if (this.valueEditor == null) {
         return false;
      } else if (this.valueEditor.isBlocking()) {
         return this.valueEditor.handleMouseScroll(mouseX, mouseY, horizontal, vertical) ? true : true;
      } else {
         return this.layout != null && this.layout.valueContent().contains(mouseX, mouseY)
            ? this.valueEditor.handleMouseScroll(mouseX, mouseY, horizontal, vertical)
            : false;
      }
   }

   public static BindPopupController getInstance() {
      return BindPopupController.Holder.INSTANCE;
   }

   public synchronized void openForModule(Module module, double anchorX, double anchorY, int viewportWidth, int viewportHeight) {
      Objects.requireNonNull(module, "module");
      BindPopupModel newModel = BindPopupModel.forModule(module);
      this.open(newModel, anchorX, anchorY, viewportWidth, viewportHeight);
   }

   public synchronized void openForSetting(Module module, Setting setting, double anchorX, double anchorY, Object initialValue) {
      MinecraftClient client = MinecraftClient.getInstance();
      int viewportWidth = 1;
      int viewportHeight = 1;
      if (client != null && client.getWindow() != null) {
         viewportWidth = Math.max(1, client.getWindow().getFramebufferWidth());
         viewportHeight = Math.max(1, client.getWindow().getFramebufferHeight());
      }

      this.openForSetting(module, setting, anchorX, anchorY, viewportWidth, viewportHeight, initialValue);
   }

   public synchronized void openForSetting(
      Module module, Setting setting, double anchorX, double anchorY, int viewportWidth, int viewportHeight, Object initialValue
   ) {
      Objects.requireNonNull(module, "module");
      Objects.requireNonNull(setting, "setting");
      Object fallbackValue = initialValue != null ? initialValue : getSettingValue(setting);
      Object defaultValue = getSettingDefaultValue(setting);
      int keyCode = -1;
      int sanitizedWidth = Math.max(1, viewportWidth);
      int sanitizedHeight = Math.max(1, viewportHeight);
      BindPopupModel newModel = BindPopupModel.forSetting(module, setting, fallbackValue, defaultValue, keyCode, BindingMode.TOGGLE);
      this.open(newModel, anchorX, anchorY, sanitizedWidth, sanitizedHeight);
   }

   private void open(BindPopupModel newModel, double anchorX, double anchorY, int viewportWidth, int viewportHeight) {
      this.model = Objects.requireNonNull(newModel, "newModel");
      this.listening = false;
      this.conflictActive = false;
      this.conflictTimestamp = 0L;
      this.shouldReleaseModel = false;
      BindingManager.getInstance().setAwaitingCapture(false);
      this.initializeValueEditor();
      BindPopupRenderer.Layout metrics = BindPopupRenderer.computeLayout(this.model, 0.0F, 0.0F, this.resolveValueBlockHeight());
      float preferredWidth = metrics.bounds().width();
      float preferredHeight = metrics.bounds().height();
      this.anchorX = sanitizeAnchorCoordinate(anchorX);
      this.anchorY = sanitizeAnchorCoordinate(anchorY);
      this.layoutScale = this.resolveLayoutScale();
      this.resolvePopupPosition(preferredWidth, preferredHeight, viewportWidth, viewportHeight);
      this.layout = BindPopupRenderer.computeLayout(this.model, this.popupX, this.popupY, this.resolveValueBlockHeight());
      this.fieldRect = this.layout.field();
      this.layoutValueEditor();
      this.resetHoverAnimators();
      this.updateModeAnimators();
      this.blurAnimator.setTarget(1.0F);
      this.visibilityAnimator.setTarget(1.0F);
   }

   private static Object getSettingValue(Setting setting) {
      if (setting instanceof BooleanSetting) {
         return ((BooleanSetting)setting).get();
      } else if (setting instanceof ModeSetting) {
         return ((ModeSetting)setting).currentMode;
      } else if (setting instanceof SliderSetting) {
         return (double)((SliderSetting)setting).current;
      } else if (setting instanceof ListSetting) {
         return new LinkedHashSet<>(((ListSetting)setting).selected);
      } else {
         return setting instanceof HueSetting
            ? HSBColor.of(((HueSetting)setting).current, ((HueSetting)setting).saturation, ((HueSetting)setting).brightness, 1.0F)
            : null;
      }
   }

   private static Object getSettingDefaultValue(Setting setting) {
      if (setting instanceof BooleanSetting) {
         return Boolean.FALSE;
      } else if (setting instanceof ModeSetting) {
         return ((ModeSetting)setting).currentMode != null ? ((ModeSetting)setting).currentMode : "";
      } else if (setting instanceof SliderSetting) {
         return (double)((SliderSetting)setting).current;
      } else if (setting instanceof ListSetting) {
         return new LinkedHashSet<>(((ListSetting)setting).selected);
      } else {
         return setting instanceof HueSetting
            ? HSBColor.of(((HueSetting)setting).current, ((HueSetting)setting).saturation, ((HueSetting)setting).brightness, 1.0F)
            : null;
      }
   }


   private static boolean sanitizeSwitchValue(Setting setting, Object value) {
      if (value instanceof Boolean bool) {
         return bool;
      } else if (value instanceof Number number) {
         return number.doubleValue() != 0.0;
      } else {
         return setting instanceof BooleanSetting ? ((BooleanSetting)setting).get() : false;
      }
   }

   private static JsonElement serializeColorValue(Setting setting, Object value) {
      if (setting instanceof HueSetting hueSetting) {
         HSBColor colorValue;
         if (value instanceof HSBColor direct) {
            colorValue = direct;
         } else if (value instanceof Number number) {
            colorValue = HSBColor.fromRgba(number.intValue());
         } else if (value instanceof String hex) {
            try {
               String stripped = hex.startsWith("#") ? hex.substring(1) : hex;
               int parsed = (int)Long.parseUnsignedLong(stripped, 16);
               int argb = stripped.length() > 6 ? parsed : 0xFF000000 | parsed;
               colorValue = HSBColor.fromRgba(argb);
            } catch (NumberFormatException var10) {
               colorValue = HSBColor.of(hueSetting.current, hueSetting.saturation, hueSetting.brightness, 1.0F);
            }
         } else {
            colorValue = HSBColor.of(hueSetting.current, hueSetting.saturation, hueSetting.brightness, 1.0F);
         }

         return new JsonPrimitive(colorValue.toRgba());
      } else {
         throw new IllegalStateException("Expected HueSetting for colour type");
      }
   }

   private static double sanitizeSliderValue(SliderSetting setting, Object value) {
      double resolved;
      if (value instanceof Number number) {
         resolved = number.doubleValue();
      } else {
         resolved = setting.current;
      }

      if (!Double.isFinite(resolved)) {
         resolved = setting.current;
      }

      double min = setting.minimum;
      double max = setting.maximum;
      double step = setting.increment;
      if (!Double.isFinite(step) || step <= 0.0) {
         step = 1.0;
      }

      double clamped = Math.min(Math.max(resolved, min), max);
      double steps = Math.round((clamped - min) / step);
      double snapped = min + steps * step;
      if (snapped < min) {
         snapped = min;
      } else if (snapped > max) {
         snapped = max;
      }

      return snapped;
   }

   private static String sanitizeComboValue(ModeSetting setting, Object value) {
      String resolved = value != null ? value.toString() : null;
      if (resolved == null || resolved.isBlank() || setting.modes != null && !setting.modes.contains(resolved)) {
         resolved = setting.currentMode != null ? setting.currentMode : "";
      }

      return resolved;
   }


   private static JsonElement serializeSelectableValue(ListSetting setting, Object value) {
      Collection<?> source;
      if (value instanceof Collection<?> collection) {
         source = collection;
      } else {
         source = setting.selected != null ? setting.selected : List.of();
      }

      LinkedHashSet<String> selections = new LinkedHashSet<>();
      if (source != null) {
         for (Object element : source) {
            if (element != null) {
               String option = element.toString();
               if (setting.list != null && setting.list.contains(option)) {
                  selections.add(option);
               }
            }
         }
      }

      if (selections.isEmpty() && setting.selected != null) {
         selections.addAll(setting.selected);
      }

      JsonArray array = new JsonArray();

      for (String option : selections) {
         array.add(option);
      }

      return array;
   }

   private void resolvePopupPosition(float popupWidth, float popupHeight, int viewportWidth, int viewportHeight) {
      PopupPositioner.PopupPlacement placement = this.popupPositioner
         .resolve(this.anchorX, this.anchorY, popupWidth, popupHeight, viewportWidth, viewportHeight, this.layoutScale);
      this.popupX = placement.x();
      this.popupY = placement.y();
   }

   public synchronized boolean handleMouseClick(double mouseX, double mouseY, int button) {
      if (!this.isInteractable()) {
         return false;
      } else if (this.layout == null) {
         return false;
      } else if (this.dispatchValueEditorClick(mouseX, mouseY, button)) {
         return true;
      } else {
         boolean inside = this.layout.bounds().contains(mouseX, mouseY);
         if (!inside) {
            this.close();
            return true;
         } else if (button == 0) {
            if (this.fieldRect.contains(mouseX, mouseY)) {
               if (this.listening) {
                  this.stopListening();
               } else {
                  this.startListening();
               }

               return true;
            } else if (this.layout.toggleButton().contains(mouseX, mouseY)) {
               this.setMode(BindingMode.TOGGLE);
               return true;
            } else if (this.layout.holdButton().contains(mouseX, mouseY)) {
               this.setMode(BindingMode.HOLD);
               return true;
            } else {
               this.close();
               return true;
            }
         } else if (button == 1) {
            this.close();
            return true;
         } else {
            return inside;
         }
      }
   }

   public synchronized boolean handleMouseScroll(double mouseX, double mouseY, double horizontal, double vertical) {
      if (!this.isInteractable()) {
         return false;
      } else {
         return this.dispatchValueEditorScroll(mouseX, mouseY, horizontal, vertical) ? true : true;
      }
   }

   public synchronized boolean handleMouseUpdateEvent(MouseUpdateEvent event) {
      Objects.requireNonNull(event, "event");
      return this.isBlockingInteractions();
   }

   public synchronized boolean handleKeyEvent(int key, int scancode, int action, int modifiers) {
      if (!this.isInteractable()) {
         return false;
      } else if (!this.listening) {
         int menuActivationKey = resolveMenuActivationKey();
         return menuActivationKey != -1 && key == menuActivationKey ? false : this.model != null;
      } else if (action != 1) {
         return true;
      } else if (key == 261 || key == 259 || key == 256) {
         this.model.markCleared();
         this.conflictActive = false;
         this.conflictTimestamp = 0L;
         this.stopListening();
         this.apply();
         return true;
      } else if (key == -1) {
         return true;
      } else if (this.isKeyConflict(key)) {
         this.conflictActive = true;
         this.conflictTimestamp = System.nanoTime();
         return true;
      } else {
         this.model.setKeyCode(key);
         this.conflictActive = false;
         this.conflictTimestamp = 0L;
         this.stopListening();
         this.apply();
         return true;
      }
   }

   private static int resolveMenuActivationKey() {
      int key = SeleneKeyBindings.getBoundKeyCode();
      return key > 0 ? key : SeleneKeyBindings.DEFAULT_MENU_KEY;
   }

   public synchronized void render(Renderer2D renderer, FontObject defaultFont, int viewportWidth, int viewportHeight, float blurFactor) {
      Objects.requireNonNull(renderer, "renderer");
      Objects.requireNonNull(defaultFont, "defaultFont");
      if (this.model == null) {
         if (this.shouldReleaseModel && this.visibilityAnimator.getValue() <= 0.001F) {
            this.clearModel();
         }
      } else {
         this.ensureLayoutWithinViewport(viewportWidth, viewportHeight);
         float alpha = clamp01(this.visibilityAnimator.getValue());
         if (alpha <= 0.001F && this.visibilityAnimator.getTarget() <= 0.0F) {
            if (this.shouldReleaseModel) {
               this.clearModel();
            }
         } else {
            float desiredValueHeight = this.resolveValueBlockHeight();
            if (this.layout == null || Math.abs(this.layout.valueBlock().height() - desiredValueHeight) > 0.001F) {
               this.layout = BindPopupRenderer.computeLayout(this.model, this.popupX, this.popupY, desiredValueHeight);
               this.layoutValueEditor();
            }

            if (this.valueEditor != null) {
               this.valueEditor.updateState();
            }

            String keyLabel;
            if (this.listening) {
               keyLabel = "Press a key";
            } else {
               int keyCode = this.model.getKeyCode();
               if (keyCode == -1) {
                  keyLabel = "None";
               } else {
                  keyLabel = formatKeyName(keyCode);
               }
            }

            this.fieldRect = BindPopupRenderer.resolveFieldRect(this.layout, renderer, keyLabel);
            boolean bindHovered = this.fieldRect.contains(this.cursorX, this.cursorY);
            boolean toggleHovered = this.layout.toggleButton().contains(this.cursorX, this.cursorY);
            boolean holdHovered = this.layout.holdButton().contains(this.cursorX, this.cursorY);
            this.updateHoverTargets(bindHovered, toggleHovered, holdHovered);
            float bindHoverProgress = this.bindHoverAnimator.getValue();
            float toggleHoverProgress = this.toggleHoverAnimator.getValue();
            float holdHoverProgress = this.holdHoverAnimator.getValue();
            boolean conflictVisible = this.conflictActive && System.nanoTime() - this.conflictTimestamp <= 1200000000L;
            String statusMessage = "";
            if (conflictVisible) {
               statusMessage = "";
            }

            float toggleSelectionProgress = this.toggleSelectionAnimator.getValue();
            float holdSelectionProgress = this.holdSelectionAnimator.getValue();
            float animatedBlurFactor = this.blurAnimator.getValue() * blurFactor;
            BindPopupRenderer.RenderState state = new BindPopupRenderer.RenderState(
               alpha,
               animatedBlurFactor,
               this.listening,
               bindHovered,
               toggleHovered,
               holdHovered,
               bindHoverProgress,
               toggleHoverProgress,
               holdHoverProgress,
               toggleSelectionProgress,
               holdSelectionProgress,
               this.model.getMode(),
               keyLabel,
               statusMessage,
               this.layout.valueBlock().height(),
               this.layout.valueLabelBaseline(),
               this.fieldRect
            );
            BindPopupRenderer.render(renderer, defaultFont, this.model, this.layout, state);
            if (this.valueEditor != null) {
               this.valueEditor.render(renderer, alpha, 1.0F);
               this.valueEditor.renderOverlay(renderer, alpha, 1.0F);
            }

            if (!conflictVisible) {
               this.conflictActive = false;
            }
         }
      }
   }

   public synchronized void updateCursor(double cursorX, double cursorY) {
      this.cursorX = cursorX;
      this.cursorY = cursorY;
      if (this.valueEditor != null) {
         this.valueEditor.updateHover(cursorX, cursorY);
      }
   }

   public synchronized void close() {
      if (this.model != null || !(this.visibilityAnimator.getValue() <= 0.001F)) {
         if (this.model != null) {
            this.apply();
         }

         this.stopListening();
         this.updateHoverTargets(false, false, false);
         this.blurAnimator.setTarget(0.0F);
         this.visibilityAnimator.setTarget(0.0F);
         this.shouldReleaseModel = true;
      }
   }

   public synchronized void closeImmediately() {
      if (this.model != null || !(this.visibilityAnimator.getValue() <= 0.001F)) {
         if (this.model != null) {
            this.apply();
         }

         this.stopListening();
         this.updateHoverTargets(false, false, false);
         this.blurAnimator.snapTo(0.0F);
         this.visibilityAnimator.snapTo(0.0F);
         this.clearModel();
      }
   }

   public synchronized boolean isBlockingInteractions() {
      return this.model != null ? true : this.visibilityAnimator.getValue() > 0.001F;
   }

   public synchronized boolean isOpen() {
      return this.model != null;
   }

   public synchronized BindPopupRenderer.Layout getCurrentLayout() {
      return this.layout;
   }

   public synchronized BindPopupRenderer.RenderState getCurrentRenderState(float blurFactor) {
      if (this.model != null && this.layout != null) {
         float alpha = clamp01(this.visibilityAnimator.getValue());
         boolean bindHovered = this.fieldRect.contains(this.cursorX, this.cursorY);
         boolean toggleHovered = this.layout.toggleButton().contains(this.cursorX, this.cursorY);
         boolean holdHovered = this.layout.holdButton().contains(this.cursorX, this.cursorY);
         float bindHoverProgress = this.bindHoverAnimator.getValue();
         float toggleHoverProgress = this.toggleHoverAnimator.getValue();
         float holdHoverProgress = this.holdHoverAnimator.getValue();
         String keyLabel;
         if (this.listening) {
            keyLabel = "Press a key";
         } else {
            int keyCode = this.model.getKeyCode();
            if (keyCode == -1) {
               keyLabel = "None";
            } else {
               keyLabel = formatKeyName(keyCode);
            }
         }

         boolean conflictVisible = this.conflictActive && System.nanoTime() - this.conflictTimestamp <= 1200000000L;
         String statusMessage = "";
         if (conflictVisible) {
            statusMessage = "";
         }

         float toggleSelectionProgress = this.toggleSelectionAnimator.getValue();
         float holdSelectionProgress = this.holdSelectionAnimator.getValue();
         float animatedBlurFactor = this.blurAnimator.getValue() * blurFactor;
         return new BindPopupRenderer.RenderState(
            alpha,
            animatedBlurFactor,
            this.listening,
            bindHovered,
            toggleHovered,
            holdHovered,
            bindHoverProgress,
            toggleHoverProgress,
            holdHoverProgress,
            toggleSelectionProgress,
            holdSelectionProgress,
            this.model.getMode(),
            keyLabel,
            statusMessage,
            this.layout.valueBlock().height(),
            this.layout.valueLabelBaseline(),
            this.fieldRect
         );
      } else {
         return null;
      }
   }

   public synchronized void apply() {
      if (this.model != null) {
         if (this.model.isDirty()) {
            BindingManager manager = BindingManager.getInstance();
            if (this.model.isModuleTarget()) {
               Module module = this.model.module();
               if (module != null) {
                  manager.updateModuleBinding(module, this.model.getKeyCode(), this.model.getMode());
               }
            } else if (this.model.isSettingTarget()) {
               Module module = this.model.module();
               Setting setting = this.model.setting();
               if (module != null && setting != null) {
                  if (this.model.isCleared()) {
                     manager.removeSettingBinding(module.name, setting.name);
                  } else {
                     Object targetValue = this.model.getTargetValue();
                     if (targetValue != null) {
                        setSettingValue(setting, targetValue);
                        manager.putSettingBinding(module, setting, this.model.getMode(), this.model.getKeyCode(), targetValue);
                     }
                  }
               }
            }

            this.model.confirmChanges();
         }
      }
   }

   private boolean isInteractable() {
      return this.model != null ? true : this.visibilityAnimator.getValue() > 0.001F && this.layout != null;
   }

   private void ensureLayoutWithinViewport(int viewportWidth, int viewportHeight) {
      if (this.model != null && this.layout != null) {
         float previousX = this.popupX;
         float previousY = this.popupY;
         this.layoutScale = this.resolveLayoutScale();
         this.resolvePopupPosition(this.layout.bounds().width(), this.layout.bounds().height(), viewportWidth, viewportHeight);
         if (this.popupX != previousX || this.popupY != previousY) {
            this.layout = BindPopupRenderer.computeLayout(this.model, this.popupX, this.popupY, this.resolveValueBlockHeight());
            this.fieldRect = this.layout.field();
            this.layoutValueEditor();
         }
      }
   }

   private void resetHoverAnimators() {
      this.bindHoverAnimator.setTarget(0.0F);
      this.toggleHoverAnimator.setTarget(0.0F);
      this.holdHoverAnimator.setTarget(0.0F);
      this.blurAnimator.setTarget(0.0F);
      this.bindHoverAnimator.snapTo(0.0F);
      this.toggleHoverAnimator.snapTo(0.0F);
      this.holdHoverAnimator.snapTo(0.0F);
      this.blurAnimator.snapTo(0.0F);
   }

   private void updateHoverTargets(boolean bindHovered, boolean toggleHovered, boolean holdHovered) {
      this.bindHoverAnimator.setTarget(bindHovered ? 1.0F : 0.0F);
      this.toggleHoverAnimator.setTarget(toggleHovered ? 1.0F : 0.0F);
      this.holdHoverAnimator.setTarget(holdHovered ? 1.0F : 0.0F);
   }

   private void startListening() {
      this.listening = true;
      this.conflictActive = false;
      this.conflictTimestamp = 0L;
      BindingManager.getInstance().setAwaitingCapture(true);
   }

   private void stopListening() {
      if (this.listening) {
         this.listening = false;
         BindingManager.getInstance().setAwaitingCapture(false);
      }
   }

   private void setMode(BindingMode mode) {
      if (this.model != null && mode != null) {
         this.model.setMode(mode);
         this.updateModeAnimators();
         this.apply();
      }
   }

   private void updateModeAnimators() {
      if (this.model != null) {
         this.toggleSelectionAnimator.setTarget(this.model.getMode() == BindingMode.TOGGLE ? 1.0F : 0.0F);
         this.holdSelectionAnimator.setTarget(this.model.getMode() == BindingMode.HOLD ? 1.0F : 0.0F);
      }
   }

   private boolean isKeyConflict(int keyCode) {
      return false;
   }

   private void clearModel() {
      this.model = null;
      this.layout = null;
      this.fieldRect = new BindPopupRenderer.Rect(0.0F, 0.0F, 0.0F, 0.0F);
      this.valueEditor = null;
      this.shouldReleaseModel = false;
      this.conflictActive = false;
      this.conflictTimestamp = 0L;
      this.anchorX = Float.NaN;
      this.anchorY = Float.NaN;
      this.layoutScale = 1.0F;
      this.resetHoverAnimators();
   }

   private float resolveLayoutScale() {
      float scale = 1.0F;
      if (!Float.isFinite(scale)) {
         return 1.0F;
      } else {
         return scale <= 0.001F ? 1.0F : scale;
      }
   }

   private static float sanitizeAnchorCoordinate(double coordinate) {
      if (!Double.isFinite(coordinate)) {
         return Float.NaN;
      } else if (coordinate > Float.MAX_VALUE) {
         return Float.MAX_VALUE;
      } else {
         return coordinate < -Float.MAX_VALUE ? -Float.MAX_VALUE : (float)coordinate;
      }
   }


   private static float clamp01(float value) {
      if (value <= 0.0F) {
         return 0.0F;
      } else {
         return value >= 1.0F ? 1.0F : value;
      }
   }

   private static String formatKeyName(int keyCode) {
      if (keyCode == -1) {
         return "None";
      }
      return sl.selene.util.keyboard.Keyboard.keyName(keyCode);
   }

   private static void setSettingValue(Setting setting, Object value) {
      boolean changed = false;
      if (setting instanceof BooleanSetting && value instanceof Boolean) {
         ((BooleanSetting)setting).set((Boolean)value);
         changed = true;
      } else if (setting instanceof ModeSetting && value instanceof String) {
         ((ModeSetting)setting).currentMode = (String)value;
         if (((ModeSetting)setting).modes != null && ((ModeSetting)setting).modes.contains((String)value)) {
            ((ModeSetting)setting).index = ((ModeSetting)setting).modes.indexOf((String)value);
         }
         changed = true;
      } else if (setting instanceof SliderSetting && value instanceof Number) {
         double val = ((Number)value).doubleValue();
         ((SliderSetting)setting).current = (float)Math.max((double)((SliderSetting)setting).minimum, Math.min((double)((SliderSetting)setting).maximum, val));
         changed = true;
      } else if (setting instanceof ListSetting && value instanceof Collection) {
         ((ListSetting)setting).selected = new ArrayList<>((Collection<? extends String>)value);
         changed = true;
      } else if (setting instanceof HueSetting && value instanceof HSBColor hsb) {
         ((HueSetting)setting).current = hsb.hue();
         ((HueSetting)setting).saturation = hsb.saturation();
         ((HueSetting)setting).brightness = hsb.brightness();
         changed = true;
      }

      if (changed && Selene.get != null && Selene.get.configManager != null) {
         Selene.get.configManager.autoSave();
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Holder {
      private static final BindPopupController INSTANCE = new BindPopupController(createVisibilityAnimator());

      private static SpringFloatAnimator createVisibilityAnimator() {
         SpringFloatAnimator animator = new SpringFloatAnimator(
            AnimationSystem.getInstance(), BindPopupController.VISIBILITY_SPRING, 0.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F
         );
         animator.setOutputTransform(Easings.SMOOTH_STEP);
         return animator;
      }
   }
}