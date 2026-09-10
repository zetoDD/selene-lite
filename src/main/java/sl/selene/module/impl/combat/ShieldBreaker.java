package sl.selene.module.impl.combat;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sl.selene.Selene;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventChangeWorld;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.player.CrosshairPin;
import sl.selene.util.player.RotationUtil;

@IModule(
   name = "Shield Breaker",
   description = "Swaps to an axe and hits blocking players to disable their shield",
   category = Category.Combat,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class ShieldBreaker extends Module {

   public static SliderSetting reach = new SliderSetting("Reach", 3.0F, 2.5F, 3.0F, 0.01F, false);
   public static SliderSetting breakDelay = new SliderSetting("Break Delay (ticks)", 1.0F, 0.0F, 5.0F, 1.0F, false);
   public static ModeSetting rotate = new ModeSetting("Rotate", "Off", "Off", "Silent");
   public static BooleanSetting switchToAxe = new BooleanSetting("Switch To Axe", true);
   public static BooleanSetting switchBack = new BooleanSetting("Switch Back", true);
   public static ModeSetting fireMode = new ModeSetting("Fire Mode", "Always", "Always", "Hold LMB");
   public static BooleanSetting followUpHit = new BooleanSetting("Follow Up Hit", true);
   public static SliderSetting followUpDelay = new SliderSetting("Follow Up Delay (ticks)", 4.0F, 0.0F, 10.0F, 1.0F, false)
         .hidden(() -> !followUpHit.get());
   public static SliderSetting followUpCharge = new SliderSetting("Follow Up Charge (%)", 100.0F, 50.0F, 100.0F, 1.0F, false)
         .hidden(() -> !followUpHit.get());
   public static BooleanSetting teams = new BooleanSetting("Teams", false);
   public static BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", true);
   public static BooleanSetting noInvisible = new BooleanSetting("No Invisible", true);

   private static final double CAST_PADDING = 0.2;
   private static final long CLICK_HOLD_MIN = 30L;
   private static final long CLICK_HOLD_SPREAD = 55L;
   private static final int FOLLOW_UP_TIMEOUT = 10;
   private static final int BREAK_RETRY_FLOOR = 5;
   private static final float ROTATE_TOLERANCE = 3.0F;
   private static final float ROTATE_SPEED = 140.0F;

   private boolean clickHeld;
   private long clickReleaseAt;
   private int boundAttackKeyCode = -1;
   private int lastAttackAge = Integer.MIN_VALUE;
   private int prevSlot = -1;
   private int swapAge = -1;
   private int lastBreakAge = -1;
   private int followUpTargetId = -1;
   private int shieldBreakAge = -1;

   public ShieldBreaker() {
      this.addSettings(new Setting[] {
         reach, breakDelay, switchToAxe, switchBack, rotate, fireMode,
         followUpHit, followUpDelay, followUpCharge, teams, ignoreFriends, noInvisible
      });
   }

   @Override
   public String getArrayListSuffix() {
      return fmt(reach.get());
   }

   @Override
   public void onDisable() {
      super.onDisable();
      releaseClick();
      finishFollowUp();
   }

   @EventInit
   public void onWorldChange(EventChangeWorld event) {
      releaseClick();
      finishFollowUp();
      this.lastBreakAge = -1;
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }
      if (mc.currentScreen != null || mc.player.isUsingItem()) {
         releaseClick();
         finishFollowUp();
         return;
      }

      if (clickHeld) {
         if (System.currentTimeMillis() >= clickReleaseAt) {
            releaseClick();
            restoreSlot();
            releaseRotation();
         }
         return;
      }

      if (this.followUpTargetId >= 0) {
         this.tickFollowUp();
         return;
      }

      if (!shouldFire()) {
         releaseRotation();
         return;
      }

      PlayerEntity target = findBlockingTarget(hitReach());
      if (target == null) {
         restoreSlot();
         releaseRotation();
         return;
      }

      if (!aimAt(target)) {
         return;
      }

      EntityHitResult hit = raycastPlayer(target, hitReach());
      if (hit == null) {
         restoreSlot();
         return;
      }

      if (!CombatUtil.isWeaponType(mc.player.getMainHandStack(), "Axe")) {
         if (!switchToAxe.get()) {
            releaseRotation();
            return;
         }
         int axeSlot = findAxeSlot();
         if (axeSlot < 0) {
            releaseRotation();
            return;
         }
         if (prevSlot < 0) {
            prevSlot = mc.player.getInventory().getSelectedSlot();
         }
         selectSlot(axeSlot);
         this.swapAge = mc.player.age;
      }

      if (this.swapAge >= 0 && mc.player.age - this.swapAge < Math.round(breakDelay.get())) {
         return;
      }
      if (this.lastBreakAge >= 0 && mc.player.age >= this.lastBreakAge
            && mc.player.age - this.lastBreakAge < BREAK_RETRY_FLOOR) {
         return;
      }
      if (mc.player.getAttackCooldownProgress(0.5F) <= 0.0F) {
         return;
      }
      if (mc.player.age == lastAttackAge) {
         return;
      }

      if (click(hit)) {
         this.swapAge = -1;
         this.lastBreakAge = mc.player.age;
         if (followUpHit.get()) {
            this.followUpTargetId = target.getId();
            this.shieldBreakAge = mc.player.age;
         }
      }
   }

   private void tickFollowUp() {
      if (!followUpHit.get() || mc.player == null) {
         finishFollowUp();
         return;
      }
      PlayerEntity player = findPlayerById(this.followUpTargetId);
      if (player == null || !player.isAlive() || player.isDead()) {
         finishFollowUp();
         return;
      }

      int delay = Math.round(followUpDelay.get());
      int ticksSinceBreak = mc.player.age - this.shieldBreakAge;
      if (player.isBlocking()) {
         if (ticksSinceBreak >= delay + FOLLOW_UP_TIMEOUT) {
            finishFollowUp();
         }
         return;
      }
      if (ticksSinceBreak < delay) {
         return;
      }

      float required = Math.max(0.5F, Math.min(1.0F, followUpCharge.get() / 100.0F));
      if (mc.player.getAttackCooldownProgress(0.5F) < required) {
         return;
      }
      if (mc.player.age == lastAttackAge) {
         return;
      }
      if (!aimAt(player)) {
         if (ticksSinceBreak >= delay + FOLLOW_UP_TIMEOUT) {
            finishFollowUp();
         }
         return;
      }

      EntityHitResult hit = raycastPlayer(player, hitReach());
      if (hit == null || !click(hit)) {
         finishFollowUp();
         return;
      }
      this.followUpTargetId = -1;
      this.shieldBreakAge = -1;
   }

   private void finishFollowUp() {
      this.followUpTargetId = -1;
      this.shieldBreakAge = -1;
      this.restoreSlot();
      releaseRotation();
   }

   private boolean aimAt(PlayerEntity target) {
      if (!rotate.is("Silent")) {
         return true;
      }
      Vec3d point = CombatUtil.aimPoint(target, "Chest");
      if (!RotationUtil.aim(this, RotationUtil.PRIORITY_HIGH, point, true,
            RotationUtil.AimType.REGULAR, ROTATE_SPEED)) {
         return false;
      }
      return RotationUtil.isAimed(point, ROTATE_TOLERANCE);
   }

   private void releaseRotation() {
      if (RotationUtil.isControlledBy(this)) {
         RotationUtil.cleanup(this);
      }
   }

   private static double hitReach() {
      return CombatUtil.clampReach(reach.get());
   }

   private PlayerEntity findBlockingTarget(double reach) {
      PlayerEntity best = null;
      double bestSq = Double.MAX_VALUE;
      Vec3d eye = mc.player.getEyePos();
      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof PlayerEntity player) || player == mc.player || player == mc.getCameraEntity()) {
            continue;
         }
         if (!player.isAlive() || player.isDead()) {
            continue;
         }
         if (!player.isBlocking()) {
            continue;
         }
         if (teams.get() && player.isTeammate(mc.player)) {
            continue;
         }
         if (noInvisible.get() && player.isInvisible()) {
            continue;
         }
         if (ignoreFriends.get() && isFriend(player)) {
            continue;
         }
         double range = reach + player.getWidth() * 0.5;
         double distSq = eye.squaredDistanceTo(player.getBoundingBox().getCenter());
         if (distSq > range * range) {
            continue;
         }
         if (distSq < bestSq) {
            bestSq = distSq;
            best = player;
         }
      }
      return best;
   }

   private PlayerEntity findPlayerById(int id) {
      if (mc.world == null) {
         return null;
      }
      for (Entity entity : mc.world.getEntities()) {
         if (entity.getId() == id && entity instanceof PlayerEntity player) {
            return player;
         }
      }
      return null;
   }

   private EntityHitResult raycastPlayer(PlayerEntity player, double reach) {
      Vec3d eye = mc.player.getEyePos();
      Vec3d look = eyeLook();
      double length = reach + CAST_PADDING;
      Vec3d end = eye.add(look.x * length, look.y * length, look.z * length);

      double maxDistSq = reach * reach;
      BlockHitResult blockHit = mc.world.raycast(
            new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
      if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
         double blockDistSq = eye.squaredDistanceTo(blockHit.getPos());
         if (blockDistSq < maxDistSq) {
            maxDistSq = blockDistSq;
         }
      }

      Box box = player.getBoundingBox();
      Optional<Vec3d> hit = box.raycast(eye, end);
      if (hit.isPresent()) {
         double distSq = eye.squaredDistanceTo(hit.get());
         if (distSq <= maxDistSq) {
            return new EntityHitResult(player, hit.get());
         }
      }
      return null;
   }

   private Vec3d eyeLook() {
      if (RotationUtil.hasSilentRotation()) {
         return RotationUtil.getRotationVector(RotationUtil.getServerPitch(), RotationUtil.getServerYaw());
      }
      return mc.player.getRotationVec(1.0F);
   }

   private boolean click(EntityHitResult hit) {
      if (mc.options == null) {
         return false;
      }
      if (fireMode.is("Hold LMB") && !mc.options.attackKey.isPressed()) {
         return false;
      }
      if (!CrosshairPin.take(this, hit)) {
         return false;
      }
      mc.crosshairTarget = hit;
      refreshBoundAttackKeyCode();
      if (boundAttackKeyCode > 0) {
         KeyBinding.setKeyPressed(InputUtil.fromTranslationKey(
               mc.options.attackKey.getBoundKeyTranslationKey()), true);
      } else {
         KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(
               mc.options.attackKey.getBoundKeyTranslationKey()));
      }
      clickHeld = true;
      clickReleaseAt = System.currentTimeMillis() + CLICK_HOLD_MIN + (long) (Math.random() * CLICK_HOLD_SPREAD);
      lastAttackAge = mc.player.age;
      return true;
   }

   private void refreshBoundAttackKeyCode() {
      if (mc.options == null) {
         boundAttackKeyCode = -1;
         return;
      }
      InputUtil.Key key = InputUtil.fromTranslationKey(mc.options.attackKey.getBoundKeyTranslationKey());
      if (key == null || key.getCode() == InputUtil.UNKNOWN_KEY.getCode()) {
         boundAttackKeyCode = -1;
      } else {
         boundAttackKeyCode = key.getCode();
      }
   }

   private void releaseClick() {
      if (!clickHeld) {
         return;
      }
      if (mc.options != null && boundAttackKeyCode > 0) {
         KeyBinding.setKeyPressed(InputUtil.fromTranslationKey(
               mc.options.attackKey.getBoundKeyTranslationKey()), false);
         mc.options.attackKey.setPressed(false);
      }
      clickHeld = false;
      CrosshairPin.clear(this);
      boundAttackKeyCode = -1;
   }

   private int findAxeSlot() {
      for (int slot = 0; slot < 9; slot++) {
         if (CombatUtil.isWeaponType(mc.player.getInventory().getStack(slot), "Axe")) {
            return slot;
         }
      }
      return -1;
   }

   private void selectSlot(int slot) {
      if (mc.player == null || mc.player.getInventory() == null) {
         return;
      }
      mc.player.getInventory().setSelectedSlot(slot);
   }

   private void restoreSlot() {
      this.swapAge = -1;
      if (prevSlot < 0 || mc.player == null || mc.player.getInventory() == null) {
         return;
      }
      if (!switchBack.get()) {
         prevSlot = -1;
         return;
      }
      if (mc.player.getInventory().getSelectedSlot() != prevSlot) {
         selectSlot(prevSlot);
      }
      prevSlot = -1;
   }

   private boolean isFriend(PlayerEntity player) {
      return Selene.get != null
            && Selene.get.friendManager != null
            && Selene.get.friendManager.isFriend(player.getName().getString());
   }

   private boolean shouldFire() {
      if (mc.options == null) {
         return false;
      }
      return switch (fireMode.get()) {
         case "Hold LMB" -> mc.options.attackKey.isPressed();
         default -> true;
      };
   }
}
