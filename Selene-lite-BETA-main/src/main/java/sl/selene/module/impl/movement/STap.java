package sl.selene.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.EventInit;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.event.player.AttackEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.module.impl.combat.CombatUtil;

@IModule(
   name = "STap",
   description = "Taps the backward key after attacking to reset sprint",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class STap extends Module {

   public static SliderSetting chance = new SliderSetting("Chance", 100.0F, 0.0F, 100.0F, 1.0F, true);
   public static SliderSetting tapTicks = new SliderSetting("Tap Ticks", 2.0F, 1.0F, 6.0F, 1.0F, false);
   public static SliderSetting delayTicks = new SliderSetting("Delay Ticks", 1.0F, 0.0F, 4.0F, 1.0F, false);
   public static BooleanSetting onlyOnGround = new BooleanSetting("Only On Ground", false);
   public static BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", true);

   private int pendingDelay;
   private int tapRemaining;
   private boolean pressed;

   public STap() {
      this.addSettings(new Setting[] { chance, tapTicks, delayTicks, onlyOnGround, onlyWeapon });
   }

   @Override
   public String getArrayListSuffix() {
      return fmt(tapTicks.get());
   }

   @EventInit
   public void onAttack(AttackEvent event) {
      if (mc.player == null || event.isCancelled()) {
         return;
      }
      if (onlyOnGround.get() && !mc.player.isOnGround()) {
         return;
      }
      if (onlyWeapon.get() && !CombatUtil.isWeaponType(mc.player.getMainHandStack(), "Melee")) {
         return;
      }

      if (!mc.options.forwardKey.isPressed()) {
         return;
      }
      if (Math.random() * 100.0 >= chance.get()) {
         return;
      }
      if (pendingDelay <= 0 && tapRemaining <= 0) {
         pendingDelay = Math.max(1, Math.round(delayTicks.get()));
      }
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null) {
         return;
      }

      if (pendingDelay > 0) {
         pendingDelay--;
         if (pendingDelay > 0) {
            return;
         }
         if (!mc.options.backKey.isPressed()) {
            mc.options.backKey.setPressed(true);
            pressed = true;
         }
         tapRemaining = Math.max(1, Math.round(tapTicks.get()));
         return;
      }

      if (tapRemaining > 0) {
         tapRemaining--;
         if (tapRemaining <= 0 && pressed) {
            mc.options.backKey.setPressed(false);
            pressed = false;
         }
      }
   }

   @Override
   public void onDisable() {
      pendingDelay = 0;
      tapRemaining = 0;
      if (pressed) {
         mc.options.backKey.setPressed(false);
      }
      pressed = false;
      super.onDisable();
   }
}