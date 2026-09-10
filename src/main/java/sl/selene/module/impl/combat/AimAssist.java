package sl.selene.module.impl.combat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventScreen;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.player.AimAssistEngine;

@IModule(
   name = "AimAssist",
   description = "Smoothly pulls your crosshair toward nearby targets",
   category = Category.Combat,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class AimAssist extends Module {

   public static SliderSetting range = new SliderSetting("Range", 4.5F, 1.0F, 8.0F, 0.1F, false);
   public static SliderSetting fov = new SliderSetting("FOV", 100.0F, 10.0F, 130.0F, 1.0F, false);

   public static SliderSetting turnSpeed = new SliderSetting("Turn Speed (deg/s)", 32.0F, 5.0F, 90.0F, 1.0F, false);

   public static SliderSetting speed = new SliderSetting("Speed", 6.0F, 1.0F, 20.0F, 0.5F, false);

   public static SliderSetting smoothness = new SliderSetting("Smoothness", 6.0F, 1.0F, 16.0F, 0.5F, false);

   public static SliderSetting jitter = new SliderSetting("Jitter", 0.3F, 0.0F, 3.0F, 0.05F, false);

   public static SliderSetting wobble = new SliderSetting("Wobble", 0.1F, 0.0F, 0.5F, 0.01F, false);
   public static ModeSetting aimPoint = new ModeSetting("Aim Point", "Chest", "Chest", "Head", "Feet");
   public static ModeSetting aimMode = new ModeSetting("Aim Mode", "Wind", "Wind", "Smooth", "Instant");

   public static BooleanSetting perfectCentering = new BooleanSetting("Perfect Centering", true);

   public static BooleanSetting teams = new BooleanSetting("Teams", false);

   public static ModeSetting targetMode = new ModeSetting(
      "Target Mode", "Everything", "Players", "Hostiles", "Mobs", "Everything");

   public static ModeSetting weaponMode = new ModeSetting("Weapon", "Melee", "Any", "Sword", "Axe", "Melee");
   public static BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);

   public static BooleanSetting humanize = new BooleanSetting("Humanize", true);

   private final AimAssistEngine engine = new AimAssistEngine();

   private Entity currentTarget;

   public AimAssist() {
      this.addSettings(new Setting[] {
         range, fov, turnSpeed, speed, smoothness, jitter, wobble,
         aimPoint, aimMode, perfectCentering, teams, targetMode, weaponMode, throughWalls, humanize
      });
   }

   @Override
   public String getArrayListSuffix() {
      return fmt(turnSpeed.get());
   }

   @Override
   public void onEnable() {
      super.onEnable();
      currentTarget = null;
      engine.reset();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      currentTarget = null;
      engine.reset();
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null || mc.world == null) {
         return;
      }
      Entity newTarget = findBestTarget();
      if (newTarget != currentTarget) {
         currentTarget = newTarget;
         engine.resetReaction();
      }
   }

   @EventInit
   public void onRender(EventScreen event) {
      if (mc.player == null || mc.world == null || mc.currentScreen != null) {
         return;
      }
      if (!CombatUtil.isWeaponType(mc.player.getMainHandStack(), weaponMode.get())) {
         return;
      }
      if (currentTarget == null || !isValidTarget(currentTarget) || !this.withinRange(currentTarget)) {

         if (currentTarget != null && !this.withinRange(currentTarget)) {
            currentTarget = null;
         }
         return;
      }
      if (!throughWalls.get() && !mc.player.canSee(currentTarget)) {
         return;
      }

      Vec3d aim = CombatUtil.aimPoint((LivingEntity) currentTarget, aimPoint.get());
      if (perfectCentering.get()) {
         Vec3d eye = mc.player.getEyePos();
         double dx = aim.getX() - eye.getX();
         double dz = aim.getZ() - eye.getZ();
         double horiz = Math.sqrt(dx * dx + dz * dz);
         if (horiz > 1.0E-4) {
            double halfWidth = Math.max(0.1, currentTarget.getWidth() * 0.5);
            double pull = Math.max(0.0, halfWidth - 0.08);
            aim = aim.add(-dx / horiz * pull, 0.0, -dz / horiz * pull);
         }
      }
      engine.aim(aim, turnSpeed.get(), speed.get(), smoothness.get(), jitter.get(), wobble.get(),
            aimMode.get(), humanize.get());
   }

   private Entity findBestTarget() {
      Entity best = null;
      double bestScore = Double.MAX_VALUE;
      double rangeSq = (double) range.get() * range.get();
      float maxFov = fov.get();

      for (Entity entity : mc.world.getEntities()) {
         if (!isValidTarget(entity)) {
            continue;
         }
         Vec3d aim = CombatUtil.aimPoint((LivingEntity) entity, aimPoint.get());
         double distanceSq = mc.player.getEyePos().squaredDistanceTo(aim);
         if (distanceSq > rangeSq) {
            continue;
         }
         float[] rotation = CombatUtil.rotationTo(mc, aim);

         float yawDiff = MathHelper.wrapDegrees(rotation[0] - mc.player.getYaw());
         float pitchDiff = rotation[1] - mc.player.getPitch();
         if (maxFov < 180.0F
               && (Math.abs(yawDiff) > maxFov / 2.0F || Math.abs(pitchDiff) > maxFov / 2.0F)) {
            continue;
         }
         double offAngle = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
         double score = Math.sqrt(distanceSq) + offAngle * 2.0;
         if (score < bestScore) {
            bestScore = score;
            best = entity;
         }
      }
      return best;
   }

   private boolean withinRange(Entity entity) {
      if (entity == null || mc.player == null) {
         return false;
      }
      Vec3d aim = CombatUtil.aimPoint((LivingEntity) entity, aimPoint.get());
      double rangeSq = (double) range.get() * range.get();
      return mc.player.getEyePos().squaredDistanceTo(aim) <= rangeSq;
   }

   private boolean isValidTarget(Entity entity) {
      if (entity == null || entity == mc.player || entity == mc.getCameraEntity()) {
         return false;
      }
      if (!(entity instanceof LivingEntity living)) {
         return false;
      }
      if (!living.isAlive() || living.isDead()) {
         return false;
      }
      if (living instanceof ArmorStandEntity) {
         return false;
      }
      if (teams.get() && living.isTeammate(mc.player)) {
         return false;
      }
      return CombatUtil.matchesTargetMode(living, targetMode.get());
   }
}