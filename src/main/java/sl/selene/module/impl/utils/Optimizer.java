package sl.selene.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;

@IModule(
   name = "Optimizer",
   description = "Optimizes HUD/GUI and reduces render load",
   category = Category.Utils,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Optimizer extends Module {
   public static final BooleanSetting optimizeHudAnimation = new BooleanSetting("Speed Up HUD Animations", true);
   public static final SliderSetting hudAnimationSpeed = new SliderSetting("HUD Animation Speed", 0.2F, 0.08F, 0.35F, 0.01F, false);
   public static final BooleanSetting disableHudShadow = new BooleanSetting("Disable HUD Shadows", true);
   public static final BooleanSetting limitNotifications = new BooleanSetting("Notification Limit", true);
   public static final BooleanSetting disableWeather = new BooleanSetting("Disable Rain/Thunder", false);
   public static final BooleanSetting disableFog = new BooleanSetting("Disable Fog", false);
   public static final SliderSetting maxNotifications = new SliderSetting("Max Notifications", 3.0F, 1.0F, 8.0F, 1.0F, false);

   public Optimizer() {
      this.addSettings(
         new Setting[]{
            optimizeHudAnimation,
            hudAnimationSpeed,
            disableHudShadow,
            limitNotifications,
            maxNotifications,
            disableWeather,
            disableFog
         }
      );
   }

   public static boolean isEnabled() {
      if (Selene.get == null || Selene.get.manager == null) {
         return false;
      }

      Optimizer optimizer = Selene.get.manager.get(Optimizer.class);
      return optimizer != null && optimizer.enable;
   }

   public static int getMaxNotifications() {
      if (!isEnabled() || !limitNotifications.get()) {
         return Integer.MAX_VALUE;
      }

      return Math.max(1, Math.round(maxNotifications.current));
   }

   public static float getHudAnimationSpeed(float baseSpeed) {
      if (!isEnabled() || !optimizeHudAnimation.get()) {
         return baseSpeed;
      }

      return Math.max(baseSpeed, hudAnimationSpeed.current);
   }

   public static boolean shouldRenderHudShadow() {
      return !isEnabled() || !disableHudShadow.get();
   }

   public static boolean shouldDisableWeather() {
      return isEnabled() && disableWeather.get();
   }

   public static boolean shouldDisableFog() {
      return isEnabled() && disableFog.get();
   }
}