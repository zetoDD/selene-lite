package sl.selene.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventPacket;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;

@IModule(
   name = "JumpReset",
   description = "Automatically jumps when hit to reduce knockback",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class JumpReset extends Module {

   public static SliderSetting chance = new SliderSetting("Chance", 100.0F, 0.0F, 100.0F, 1.0F, true);

   public static BooleanSetting onlyGround = new BooleanSetting("Only On Ground", true);

   public static BooleanSetting sprintOnly = new BooleanSetting("Sprint Only", false);

   private static final int HIT_TICK = 9;

   private static final long FALL_DAMAGE_SUPPRESS_MS = 1000L;
   private static final int ARM_GRACE_TICKS = 6;

   private int armedTicks;

   private long fallDamageUntil;

   public JumpReset() {
      this.addSettings(new Setting[] { chance, onlyGround, sprintOnly });
   }

   @Override
   public String getArrayListSuffix() {
      return fmt(chance.get());
   }

   @Override
   public void onDisable() {
      armedTicks = 0;
      super.onDisable();
   }

   @EventInit
   public void onPacket(EventPacket event) {
      if (event.getType() != EventPacket.Type.RECEIVE) {
         return;
      }
      if (mc.player == null) {
         return;
      }
      if (!(event.getPacket() instanceof EntityVelocityUpdateS2CPacket velocity)) {
         return;
      }
      if (velocity.getEntityId() != mc.player.getId()) {
         return;
      }
      Vec3d v = velocity.getVelocity();
      if (v.x == 0.0 && v.z == 0.0 && v.y < 0.0) {
         fallDamageUntil = System.currentTimeMillis() + FALL_DAMAGE_SUPPRESS_MS;
         armedTicks = 0;
         return;
      }
      if (v.x != 0.0 || v.z != 0.0 || v.y > 0.0) {
         if (Math.random() * 100.0 < chance.get()) {
            armedTicks = ARM_GRACE_TICKS;
         }
      }
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null || mc.world == null) {
         armedTicks = 0;
         return;
      }
      if (mc.currentScreen != null) {
         armedTicks = 0;
         return;
      }
      if (System.currentTimeMillis() < fallDamageUntil) {
         armedTicks = 0;
         return;
      }
      if (armedTicks <= 0) {

         if (mc.player.hurtTime == HIT_TICK && this.canJump()) {
            this.jump();
         }
         return;
      }

      armedTicks--;
      if (onlyGround.get() && !mc.player.isOnGround()) {
         return;
      }
      if (sprintOnly.get() && !mc.player.isSprinting()) {
         return;
      }
      this.jump();
   }

   private boolean canJump() {
      if (onlyGround.get() && !mc.player.isOnGround()) {
         return false;
      }
      if (sprintOnly.get() && !mc.player.isSprinting()) {
         return false;
      }
      if (Math.random() * 100.0 >= chance.get()) {
         return false;
      }
      return true;
   }

   private void jump() {
      if (mc.player == null) {
         return;
      }

      mc.player.jump();
   }
}