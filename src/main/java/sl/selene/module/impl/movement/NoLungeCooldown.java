package sl.selene.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import sl.selene.event.EventInit;
import sl.selene.event.input.MouseButtonEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.module.impl.combat.SpearUtil;

@IModule(
   name = "NoLungeCooldown",
   description = "Chains multiple stab packets for a stronger spear lunge, no swing cooldown",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class NoLungeCooldown extends Module {

   public static SliderSetting multiplier = new SliderSetting("Multiplier", 2.0F, 1.0F, 10.0F, 1.0F, false);

   public NoLungeCooldown() {
      this.addSettings(new Setting[] { multiplier });
   }

   @Override
   public String getArrayListSuffix() {
      return fmt(multiplier.get());
   }

   @EventInit
   public void onMouse(MouseButtonEvent event) {
      if (!event.isPress() || event.button() != 0) {
         return;
      }
      if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.isPaused()) {
         return;
      }
      if (!SpearUtil.isHoldingSpear()) {
         return;
      }
      event.cancel();
      mc.player.swingHand(Hand.MAIN_HAND);
      SpearUtil.sendStab((int) multiplier.get());
   }
}