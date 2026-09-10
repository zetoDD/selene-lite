package sl.selene.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.EventInit;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;

@IModule(
   name = "ToggleSprint",
   description = "Automatically sprints while moving forward",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class ToggleSprint extends Module {

   public static BooleanSetting requireFood = new BooleanSetting("Require Food", true);

   public ToggleSprint() {
      this.addSettings(new Setting[] { requireFood });
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      boolean movingForward = mc.options.forwardKey.isPressed();

      boolean sneakingInput = mc.options.sneakKey.isPressed();
      boolean canSprint = movingForward
            && !sneakingInput
            && !mc.player.isUsingItem()
            && (!requireFood.get() || mc.player.getHungerManager().getFoodLevel() > 6)
            && !mc.player.isTouchingWater()
            && !mc.player.horizontalCollision;

      mc.player.setSprinting(canSprint);
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
      super.onDisable();
   }
}