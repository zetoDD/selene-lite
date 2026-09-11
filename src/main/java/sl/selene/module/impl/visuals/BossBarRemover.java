package sl.selene.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;

@IModule(
   name = "BossBarRemover",
   description = "Hides the boss bar and the screen effects that come with it",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class BossBarRemover extends Module {

   public static BooleanSetting bossEffects = new BooleanSetting("Boss Effects", true);

   private static boolean active;

   public BossBarRemover() {
      this.enable = true;
      active = true;
      this.addSettings(new Setting[] { bossEffects });
   }

   @Override
   public boolean isEnabledByDefault() {
      return true;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      active = true;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      active = false;
   }

   @Override
   protected void onConfigLoadEnable() {
      active = true;
   }

   @Override
   protected void onConfigLoadDisable() {
      active = false;
   }

   public static boolean isHiding() {
      return active;
   }

   public static boolean isHidingEffects() {
      return active && bossEffects.get();
   }
}

