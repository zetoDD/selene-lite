package sl.selene.ui.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.Selene;
import sl.selene.module.api.Category;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.impl.BindSettings;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.module.api.setting.impl.StringSetting;
import sl.selene.util.render.math.ScrollUtil;
import sl.selene.util.render.math.animation.Animation;
import sl.selene.util.render.math.animation.AnimationMath;
import sl.selene.util.render.math.animation.Direction;
import sl.selene.util.render.math.animation.anim.util.Animation2;
import sl.selene.util.render.math.animation.anim.util.Easings;
import sl.selene.util.render.math.animation.anim2.Easing;
import sl.selene.util.render.math.animation.impl.EaseInOutQuad;

@Environment(EnvType.CLIENT)
public class GuiScreen {

   public static MinecraftClient mc = MinecraftClient.getInstance();
   public static Animation mainAnimation = new EaseInOutQuad(200, 1.0);
   public static Animation categoryAnimation = new EaseInOutQuad(500, 1.0);
   public static Animation moduleAnimation = new EaseInOutQuad(500, 1.0);
   public static Animation animation15 = new EaseInOutQuad(1000, 1.0);
   public static sl.selene.util.render.math.animation.anim2.Animation alpha = new sl.selene.util.render.math.animation.anim2.Animation(
      Easing.EASE_OUT_SINE, 1500L
   );
   public static sl.selene.util.render.math.animation.anim.Animation animation = new sl.selene.util.render.math.animation.anim.Animation();
   public static Animation2 alphaPC = new Animation2();
   public static Animation2 settingPC = new Animation2();
   public static Animation2 alphaPC2 = new Animation2();
   public static Animation2 alphaPC3 = new Animation2();
   public static Animation2 openAnimation = new Animation2();
   public static HueSetting activeColorPicker = null;
   public static float colorPickerX = 0.0F;
   public static float colorPickerY = 0.0F;
   public static boolean pickingSaturationBrightness = false;
   public static boolean pickingHue = false;
   public static boolean pickingAlpha = false;
   public static BindSettings activeBindSetting = null;
   public static StringSetting activeStringSetting = null;
   public static SliderSetting activeSliderSetting = null;
   public static Module activeModuleBind = null;
   public static float sliderX = 0.0F;
   public static float sliderY = 0.0F;
   public static float sliderWidth = 0.0F;

   public static final float MAX_SLIDER_OVERFLOW = 50.0F;

   public static int sliderOverflowSide = 0;

   public static float sliderOverflow = 0.0F;
   private static float sliderOverflowTarget = 0.0F;
   private static float sliderOverflowVel = 0.0F;
   private static final float SLIDER_SPRING_STIFFNESS = 240.0F;
   private static final float SLIDER_SPRING_DAMPING = 24.0F;

   public static float decaySliderOverflow(float value, float max) {
      if (max == 0.0F) {
         return 0.0F;
      }
      float entry = value / max;
      float sigmoid = (float)(2.0 * (1.0 / (1.0 + Math.exp(-entry)) - 0.5));
      return sigmoid * max;
   }

   public static void setSliderOverflow(int side, float amount) {
      sliderOverflowSide = side;
      sliderOverflowTarget = amount;
   }

   public static void updateSliderElastic() {
      float dt = 0.0166667F;
      float accel = SLIDER_SPRING_STIFFNESS * (sliderOverflowTarget - sliderOverflow) - SLIDER_SPRING_DAMPING * sliderOverflowVel;
      sliderOverflowVel += accel * dt;
      sliderOverflow += sliderOverflowVel * dt;
      if (sliderOverflowTarget == 0.0F && Math.abs(sliderOverflow) < 0.05F && Math.abs(sliderOverflowVel) < 0.05F) {
         sliderOverflow = 0.0F;
         sliderOverflowVel = 0.0F;
         sliderOverflowSide = 0;
      }
   }
   public static String searchText = "";
   public static boolean activeSearch = false;
   public static boolean searchSelectAll = false;
   public static long lastBackspaceTime = 0L;
   public static boolean backspaceHeld = false;
   public static long firstBackspacePressTime = 0L;

   public static boolean frostedGlass = false;

   public static boolean plainArrayList = true;

   public static boolean settingsPageOpen = false;
   public static int selectedSettingsCategory = 0;
   public static final String[] SETTINGS_CATEGORIES = { "General", "Appearance", "HUD", "Performance" };
   public static boolean serverMapOpen = false;
   public static boolean friendsTabOpen = false;
   public static boolean friendInputActive = false;
   public static String friendInputText = "";
   public static boolean configInputActive = false;
   public static String configInputText = "";
   public static boolean configPopupOpen = false;
   public static boolean configPopupDragging = false;
   public static float configPopupOffsetX;
   public static float configPopupOffsetY;
   public static float configPopupX;
   public static float configPopupY;
   public static String configStatusText = "";
   public static long configStatusUntil = 0L;
   public static boolean configDeleteArmed = false;
   public static long configDeleteArmedUntil = 0L;
   public static boolean exit = false;

   public static final float HEADER_HEIGHT = 36.0F;
   public static final float HEADER_CLIP_HEIGHT = 42.0F;
   public static final float SIDEBAR_WIDTH = 105.0F;

   public static final float GAP = 12.0F;

   public static final float SIDEBAR_BOTTOM_INSET = 0.0F;
   public static final float CONTENT_TOP = HEADER_HEIGHT + GAP;
   public static float renderScale = 1.0F;
   public static float x;
   public static float y;
   public static float width;
   public static float height;

   public static boolean windowDragging = false;

   public static float windowDragOffsetX;
   public static float windowDragOffsetY;

   public static float windowOffsetX;
   public static float windowOffsetY;

   public static void resetWindowPosition() {
      windowOffsetX = 0.0F;
      windowOffsetY = 0.0F;
      windowDragging = false;
      configPopupDragging = false;
      configPopupOffsetX = 0.0F;
      configPopupOffsetY = 0.0F;
      configPopupX = 0.0F;
      configPopupY = 0.0F;
         }
   public static int currentMouseX = 0;
   public static int currentMouseY = 0;
   public static Category[] categories;
   public static Category selectedCategories;
   public static List<Module> modules;
   private static ScrollUtil scrollUtil;
   public static Set<Module> openSettingsModules = new HashSet<>();
   public static Map<Module, Animation2> moduleSettingsAnimations = new HashMap<>();
   public static Map<Module, Animation2> moduleSettingsAlphaAnimations = new HashMap<>();
   public static Map<Module, Animation2> moduleBindAnimations = new HashMap<>();
   public static Map<Module, Long> moduleBindWobbles = new HashMap<>();
   public static Map<SliderSetting, Animation2> sliderAnimations = new HashMap<>();

   public static Map<SliderSetting, Float> sliderHoverAnimations = new HashMap<>();

   public static Map<Module, Float> cardHoverAnimations = new HashMap<>();
   public static Map<Category, Float> categoryHoverAnimations = new HashMap<>();
   public static float[] settingsNavHover = new float[SETTINGS_CATEGORIES.length];
   public static float hoverSettingsIsland;
   public static float hoverConfigIsland;
   public static float hoverSearchIsland;
   public static float hoverCloseIsland;
   public static Animation2 toggleFrostedAnim = new Animation2();
   public static Animation2 toggleHudLayoutAnim = new Animation2();
   public static Animation2 toggleHudTextAnim = new Animation2();

   public static void wobbleBindPill(Module module) {
      moduleBindWobbles.put(module, System.currentTimeMillis());
   }

   public static float easeHover(float current, boolean hovered) {
      return AnimationMath.fast(current, hovered ? 1.0F : 0.0F, 14.0F);
   }

   public static float getBindWobble(Module module) {
      Long start = moduleBindWobbles.get(module);
      if (start == null) {
         return 0.0F;
      }
      long elapsed = System.currentTimeMillis() - start;
      if (elapsed > 900L) {
         moduleBindWobbles.remove(module);
         return 0.0F;
      }
      float decay = 1.0F - elapsed / 900.0F;
      return (float) Math.sin(elapsed * 0.057) * 2.5F * decay;
   }

   public static float getBindWobbleBoost(Module module) {
      Long start = moduleBindWobbles.get(module);
      if (start == null) {
         return 0.0F;
      }
      long elapsed = System.currentTimeMillis() - start;
      if (elapsed > 900L) {
         moduleBindWobbles.remove(module);
         return 0.0F;
      }
      return 1.0F - elapsed / 900.0F;
   }
   private static final float SETTINGS_OPEN_DURATION = 0.24F;
   private static final float SETTINGS_CLOSE_DURATION = 0.22F;

   public static void openModuleSettings(Module module) {
      openSettingsModules.add(module);
      getModuleSettingsAnimation(module).run(1.0, SETTINGS_OPEN_DURATION, Easings.SINE_OUT);
      getModuleSettingsAlphaAnimation(module).run(1.0, SETTINGS_OPEN_DURATION, Easings.SINE_OUT);
   }

   public static void closeModuleSettings(Module module) {
      openSettingsModules.remove(module);
      getModuleSettingsAnimation(module).run(0.0, SETTINGS_CLOSE_DURATION, Easings.SINE_IN);
      getModuleSettingsAlphaAnimation(module).run(0.0, SETTINGS_CLOSE_DURATION, Easings.SINE_IN);
      if (activeColorPicker != null && module.getSettingsForGUI().contains(activeColorPicker)) {
         animation15.setDirection(Direction.BACKWARDS);
         activeColorPicker = null;
         colorPickerX = 0.0F;
         colorPickerY = 0.0F;
      }
   }

   public static Category[] resolveCategories() {
      return new Category[] { Category.Combat, Category.Movement, Category.Visuals, Category.Utils, Category.Misc, Category.Donut };
   }

   public static void refreshCategoriesAndModules() {
      Category[] resolved = resolveCategories();
      if (categories == null || !Arrays.equals(categories, resolved)) {
         categories = resolved;
      }

      if (selectedCategories == null) {
         selectedCategories = Category.Visuals;
      } else {
         boolean categoryAvailable = false;
         for (Category category : categories) {
            if (category == selectedCategories) {
               categoryAvailable = true;
               break;
            }
         }
         if (!categoryAvailable) {
            selectedCategories = Category.Visuals;
         }
      }

      if (Selene.get != null && Selene.get.manager != null) {
         modules = Selene.get.manager.getType(selectedCategories);
      }
   }

   public static ScrollUtil getScrollUtil() {
      if (scrollUtil == null) {
         scrollUtil = new ScrollUtil();
      }

      return scrollUtil;
   }

   public static Animation2 getModuleSettingsAnimation(Module module) {
      return moduleSettingsAnimations.computeIfAbsent(module, k -> new Animation2());
   }

   public static Animation2 getModuleSettingsAlphaAnimation(Module module) {
      return moduleSettingsAlphaAnimations.computeIfAbsent(module, k -> new Animation2());
   }

   public static Animation2 getModuleBindAnimation(Module module) {
      Animation2 anim = moduleBindAnimations.computeIfAbsent(module, k -> new Animation2());
      if (module.bind != -1 && anim.getDuration() == 0.0 && anim.getValue() == 0.0) {
         anim.setValue(1.0);
      }

      return anim;
   }

   public static Animation2 getSliderAnimation(SliderSetting slider) {
      return sliderAnimations.computeIfAbsent(slider, k -> {
         Animation2 newAnim = new Animation2();
         float targetProgress = (slider.current - slider.minimum) / (slider.maximum - slider.minimum);
         newAnim.setValue(targetProgress);
         return newAnim;
      });
   }

   public static boolean isVanillaStyle() {
      return false;
   }

}