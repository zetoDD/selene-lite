package sl.selene.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.PlayerInput;
import sl.selene.event.EventInit;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;

@IModule(
   name = "SnapTap",
   description = "Prioritizes the last pressed movement key like Wooting keyboards",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class SnapTap extends Module {

   public static BooleanSetting strafe = new BooleanSetting("Strafe", true);
   public static BooleanSetting forward = new BooleanSetting("Forward", true);

   public static long leftPressTime = 0L;
   public static long rightPressTime = 0L;
   public static long forwardPressTime = 0L;
   public static long backPressTime = 0L;

   private boolean wasLeft;
   private boolean wasRight;
   private boolean wasForward;
   private boolean wasBack;
   private long pressCounter;

   public SnapTap() {
      this.addSettings(new Setting[] { strafe, forward });
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null || mc.options == null) {
         return;
      }
      boolean left = mc.options.leftKey.isPressed();
      boolean right = mc.options.rightKey.isPressed();
      boolean fwd = mc.options.forwardKey.isPressed();
      boolean back = mc.options.backKey.isPressed();

      if (left && !this.wasLeft) {
         leftPressTime = ++this.pressCounter;
      }
      if (right && !this.wasRight) {
         rightPressTime = ++this.pressCounter;
      }
      if (fwd && !this.wasForward) {
         forwardPressTime = ++this.pressCounter;
      }
      if (back && !this.wasBack) {
         backPressTime = ++this.pressCounter;
      }
      this.wasLeft = left;
      this.wasRight = right;
      this.wasForward = fwd;
      this.wasBack = back;
   }

   @Override
   public void onDisable() {
      this.wasLeft = false;
      this.wasRight = false;
      this.wasForward = false;
      this.wasBack = false;
      super.onDisable();
   }

   public static boolean getLeft(PlayerInput input) {
      if (strafe.get() && input.left()) {
         return !(input.right() && rightPressTime > leftPressTime);
      }
      return input.left();
   }

   public static boolean getRight(PlayerInput input) {
      if (strafe.get() && input.right()) {
         return !(input.left() && leftPressTime > rightPressTime);
      }
      return input.right();
   }

   public static boolean getForward(PlayerInput input) {
      if (forward.get() && input.forward()) {
         return !(input.backward() && backPressTime > forwardPressTime);
      }
      return input.forward();
   }

   public static boolean getBack(PlayerInput input) {
      if (forward.get() && input.backward()) {
         return !(input.forward() && forwardPressTime > backPressTime);
      }
      return input.backward();
   }
}