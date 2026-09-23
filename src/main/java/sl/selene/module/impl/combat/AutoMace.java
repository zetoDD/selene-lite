package sl.selene.module.impl.combat;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventChangeWorld;
import sl.selene.event.impl.EventScreen;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.event.player.AttackExecutedEvent;
import sl.selene.mixin.MinecraftClientAccessor;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.engine.HitEngine;
import sl.selene.util.engine.SlotEngine;
import sl.selene.util.player.AimAssistEngine;

@IModule(name = "AutoMace", description = "Aims and performs mace and stun-slam attacks", category = Category.Combat, bind = -1)
@Environment(EnvType.CLIENT)
public final class AutoMace extends Module {

   private static final double MIN_DROP = 1.5D;
   private static final double MAX_ATTACK_RANGE = 2.95D;
   private static final double AIM_EDGE = 0.5D;
   private static final float SMASH_CHARGE_TICKS = 5.0F;
   private static final int INPUT_RETRY_TICKS = 3;
   private static final int MAX_SEQUENCE_TICKS = 30;
   private static final long DIRECT_HIT_JITTER_MS = 2L;

   private final SliderSetting swingRange = new SliderSetting("Swing Range", 3.0F, 2.5F, 3.0F, 0.01F, false);
   private final SliderSetting yawFov = new SliderSetting("Yaw FOV", 360.0F, 0.0F, 360.0F, 1.0F, false);
   private final SliderSetting pitchFov = new SliderSetting("Pitch FOV", 180.0F, 0.0F, 180.0F, 1.0F, false);
   private final BooleanSetting targetPriority = new BooleanSetting("Target Priority", true);
   private final SliderSetting strength = new SliderSetting("Strength", 360.0F, 20.0F, 720.0F, 5.0F, false);
   private final SliderSetting preAimDistance = new SliderSetting("Pre-Aim Distance", 3.0F, 1.0F, 6.0F, 0.1F, false);
   private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);
   private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
   private final SliderSetting swapHitDelay = new SliderSetting("Swap Hit Delay (ms)", 36.0F, 34.0F, 44.0F, 1.0F,
         false);
   private final SliderSetting minFallDistance = new SliderSetting("Min Fall Distance", 1.5F, 0.0F, 10.0F, 0.1F, false);
   private final SliderSetting cooldown = new SliderSetting("Cooldown (ms)", 500.0F, 100.0F, 5000.0F, 50.0F, false);
   private final BooleanSetting stunSlam = new BooleanSetting("Stun Slam", true);
   private final SliderSetting stunDelay = new SliderSetting("Stun Delay (Ticks)", 2.0F, 1.0F, 8.0F, 1.0F, false)
         .hidden(() -> !stunSlam.get());
   private final BooleanSetting pauseWhileEating = new BooleanSetting("Pause While Eating", true);
   private final BooleanSetting pauseInGui = new BooleanSetting("Pause In GUI", true);

   private final SlotEngine slots = new SlotEngine();
   private final HitEngine hit = new HitEngine();
   private final AimAssistEngine aimer = new AimAssistEngine();

   private LivingEntity target;
   private State state = State.IDLE;
   private int stateStartedAge = -1;
   private int axeAttackAge = -1;
   private int preparedAxeSlot = -1;
   private int preparedMaceSlot = -1;
   private int retries;
   private int lastRetryAge = -1;
   private boolean cycleConsumed;
   private boolean stunAxeExecuted;
   private boolean stunMaceExecuted;
   private long clientTickId;
   private long axeExecutedTickId = -1L;
   private long stunStartedMs;
   private long directHitDueNs;
   private long directHitDeadlineNs;
   private long lastAttackMs;

   private enum State {
      IDLE,
      DIRECT_READY,
      STUN_AXE_READY,
      STUN_WAIT,
      STUN_MACE_READY,
      RESTORE,
      LOCKED
   }

   public AutoMace() {
      addSettings(new Setting[] {
            swingRange, yawFov, pitchFov, targetPriority,
            strength, preAimDistance,
            autoSwitch, swapBack, swapHitDelay, minFallDistance, cooldown,
            stunSlam, stunDelay,
            pauseWhileEating, pauseInGui
      });
   }

   @Override
   public void onEnable() {
      super.onEnable();
      reset();
   }

   @Override
   public void onDisable() {
      reset();
      super.onDisable();
   }

   @EventInit
   public void onWorldChange(EventChangeWorld event) {
      reset();
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         reset();
         return;
      }

      if (isPaused()) {
         finishCycle();
         return;
      }

      clientTickId++;

      if (shouldResetCycle()) {
         resetCycle();
      }

      if (state != State.IDLE) {
         tickSequence();
         return;
      }

      if (!hasSmashFallRequirement()) {
         target = null;
         aimer.reset();
         return;
      }

      target = findTarget();
      if (target == null || cycleConsumed) {
         aimer.reset();
         return;
      }

      if (!isAttackReady()
            || !withinReach(target) || raycastTarget(target) == null) {
         return;
      }

      int maceSlot = resolveMaceSlot();
      if (maceSlot < 0) {
         return;
      }

      if (stunSlam.get() && autoSwitch.get() && target instanceof PlayerEntity player && isTargetBlocking(player)) {
         int axeSlot = findAxe();
         if (axeSlot >= 0) {
            startStunSlam(axeSlot, maceSlot);
            return;
         }
      }

      startDirectHit(maceSlot);
   }

   @EventInit
   public void onRender(EventScreen event) {
      if (mc.player == null || mc.world == null || isPaused() || state == State.LOCKED) {
         aimer.reset();
         return;
      }

      LivingEntity activeTarget = target;
      if (activeTarget == null || !activeTarget.isAlive() || activeTarget.isRemoved()
            || mc.player.distanceTo(activeTarget) > engagementRange()) {
         aimer.reset();
         return;
      }

      if (state == State.IDLE && (!hasSmashFallRequirement() || !isTargetValid(activeTarget))) {
         aimer.reset();
         return;
      }

      float aimStrength = strength.get();
      float smoothing = state == State.IDLE ? 12.0F : 18.0F;
      aimer.aim(getAimPoint(activeTarget), aimStrength, aimStrength, smoothing, 0.0F, "Smooth");
      pollDirectHit();
      pollStunAxe();
      pollStunMace();
   }

   @EventInit
   public void onAttackExecuted(AttackExecutedEvent event) {
      if (event == null || event.getTarget() != target || mc.player == null) {
         return;
      }
      if (state == State.STUN_AXE_READY && mc.player.getMainHandStack().getItem() instanceof AxeItem) {
         stunAxeExecuted = true;
         axeExecutedTickId = clientTickId;
         axeAttackAge = mc.player.age;
      } else if (state == State.STUN_MACE_READY && mc.player.getMainHandStack().getItem() instanceof MaceItem) {
         stunMaceExecuted = true;
      }
   }

   private void startDirectHit(int maceSlot) {
      cycleConsumed = true;
      preparedMaceSlot = maceSlot;
      retries = 0;
      stateStartedAge = mc.player.age;
      state = State.DIRECT_READY;
      if (!selectSlot(maceSlot)) {
         finishCycle();
         return;
      }
      long configuredDelay = Math.round(swapHitDelay.get());
      long minimumDelay = Math.max(34L, configuredDelay - DIRECT_HIT_JITTER_MS);
      long maximumDelay = Math.min(44L, configuredDelay + DIRECT_HIT_JITTER_MS);
      long delay = ThreadLocalRandom.current().nextLong(minimumDelay, maximumDelay + 1L);
      directHitDueNs = System.nanoTime() + delay * 1_000_000L;
      directHitDeadlineNs = directHitDueNs + 250_000_000L;
   }

   private void startStunSlam(int axeSlot, int maceSlot) {
      cycleConsumed = true;
      preparedAxeSlot = axeSlot;
      preparedMaceSlot = maceSlot;
      axeAttackAge = -1;
      retries = 0;
      lastRetryAge = mc.player.age;
      stunAxeExecuted = false;
      stunMaceExecuted = false;
      axeExecutedTickId = -1L;
      stunStartedMs = 0L;
      stateStartedAge = mc.player.age;
      state = State.STUN_AXE_READY;
      if (!selectSlot(axeSlot)) {
         finishCycle();
      }
   }

   private void tickSequence() {
      if (state == State.LOCKED) {
         return;
      }

      if (target == null || !target.isAlive() || target.isRemoved()
            || mc.player.distanceTo(target) > engagementRange()
            || stateStartedAge >= 0 && mc.player.age - stateStartedAge > MAX_SEQUENCE_TICKS) {
         finishCycle();
         return;
      }

      switch (state) {
         case DIRECT_READY -> {
            if (System.nanoTime() > directHitDeadlineNs) {
               finishCycle();
            }
         }
         case STUN_AXE_READY -> tickStunAxeWait();
         case STUN_WAIT, STUN_MACE_READY -> tickStunMaceWait();
         case RESTORE -> tickRestore();
         default -> finishCycle();
      }
   }

   private void tickStunAxeWait() {
      if (!(target instanceof PlayerEntity player) || !isTargetBlocking(player) || !hasSmashFallRequirement()) {
         finishCycle();
         return;
      }
      if (mc.player.age != lastRetryAge) {
         lastRetryAge = mc.player.age;
         retries++;
      }
      if (retries > INPUT_RETRY_TICKS) {
         finishCycle();
      }
   }

   private void tickStunMaceWait() {
      if (!stunAxeExecuted || axeExecutedTickId < 0L
            || System.currentTimeMillis() - stunStartedMs > 1500L) {
         finishCycle();
         return;
      }
      if (mc.player.age != lastRetryAge) {
         lastRetryAge = mc.player.age;
         retries++;
      }
      if (retries > INPUT_RETRY_TICKS && System.currentTimeMillis() - stunStartedMs > 300L) {
         finishCycle();
      }
   }

   private void pollDirectHit() {
      if (state != State.DIRECT_READY || System.nanoTime() < directHitDueNs) {
         return;
      }
      if (!isSelectedMace(preparedMaceSlot)) {
         finishCycle();
         return;
      }
      if (!slots.ready(1)) {
         return;
      }
      if (attackTarget()) {
         lastAttackMs = System.currentTimeMillis();
         beginRestore();
      }
   }

   private void pollStunAxe() {
      if (state != State.STUN_AXE_READY || !(target instanceof PlayerEntity player)
            || !isTargetBlocking(player) || !hasSmashFallRequirement()) {
         return;
      }
      if (preparedAxeSlot < 0 || !selectSlot(preparedAxeSlot)
            || !(mc.player.getMainHandStack().getItem() instanceof AxeItem)) {
         finishCycle();
         return;
      }
      if (!slots.ready(1)) {
         return;
      }
      stunAxeExecuted = false;
      if (!attackPreparedTarget()) {
         return;
      }
      stunAxeExecuted = true;
      axeExecutedTickId = clientTickId;
      axeAttackAge = mc.player.age;
      stunStartedMs = System.currentTimeMillis();
      retries = 0;
      lastRetryAge = mc.player.age;
      state = State.STUN_WAIT;
   }

   private void pollStunMace() {
      if ((state != State.STUN_WAIT && state != State.STUN_MACE_READY)
            || !stunAxeExecuted || axeExecutedTickId < 0L
            || clientTickId <= axeExecutedTickId) {
         return;
      }
      long elapsedTicks = clientTickId - axeExecutedTickId;
      boolean urgent = isUrgent();
      if (elapsedTicks < Math.max(1L, legacyTickGap() - 1L) && !urgent) {
         return;
      }
      state = State.STUN_MACE_READY;
      if (preparedMaceSlot < 0 || !selectSlot(preparedMaceSlot)
            || !(mc.player.getMainHandStack().getItem() instanceof MaceItem)) {
         finishCycle();
         return;
      }
      if (!slots.ready(1) || elapsedTicks < legacyTickGap() && !urgent) {
         return;
      }
      stunMaceExecuted = false;
      if (!attackPreparedTarget()) {
         return;
      }
      stunMaceExecuted = true;
      lastAttackMs = System.currentTimeMillis();
      beginRestore();
   }

   private void beginRestore() {
      retries = 0;
      stateStartedAge = mc.player.age;
      state = State.RESTORE;
   }

   private void tickRestore() {
      if (mc.player.age <= stateStartedAge) {
         return;
      }
      restoreSlot();
      clearSequence();
      state = State.LOCKED;
   }

   private boolean attackTarget() {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || target == null) {
         return false;
      }

      EntityHitResult hitResult = getCurrentCrosshairTarget();
      if (hitResult == null || !hit.press(hitResult)) {
         return false;
      }

      try {
         MinecraftClientAccessor accessor = (MinecraftClientAccessor) mc;
         accessor.setAttackCooldown(0);
         return accessor.invokeDoAttack();
      } finally {
         clearQueuedAttack();
         hit.release();
      }
   }

   private EntityHitResult getCurrentCrosshairTarget() {
      if (mc.gameRenderer == null || target == null) {
         return null;
      }
      mc.gameRenderer.updateCrosshairTarget(1.0F);
      if (mc.crosshairTarget instanceof EntityHitResult hitResult
            && hitResult.getEntity() == target) {
         return hitResult;
      }
      EntityHitResult hitResult = raycastTarget(target);
      if (hitResult != null) {
         mc.crosshairTarget = hitResult;
         mc.targetedEntity = target;
      }
      return hitResult;
   }

   private boolean attackPreparedTarget() {
      return attackTarget();
   }

   private void clearQueuedAttack() {
      if (mc.options == null || mc.options.attackKey == null) {
         return;
      }
      while (mc.options.attackKey.wasPressed()) {
      }
      mc.options.attackKey.setPressed(false);
   }

   private boolean selectSlot(int slot) {
      return slot >= 0 && slot < 9 && slots.selectNow(slot);
   }

   private int legacyTickGap() {
      return 1 + Math.max(1, Math.round(stunDelay.get()));
   }

   private boolean isUrgent() {
      return computeTicksToGroundImpact() <= 1 || computeTicksToExitReach() <= 1;
   }

   private int computeTicksToGroundImpact() {
      if (mc.player == null || mc.world == null || mc.player.isOnGround()) {
         return 0;
      }

      double velocity = mc.player.getVelocity().y;
      if (velocity >= 0.0D) {
         return Integer.MAX_VALUE;
      }

      double clearance = groundClearance();
      if (clearance <= 0.15D) {
         return 0;
      }

      double travelled = 0.0D;
      for (int ticks = 1; ticks <= 20; ticks++) {
         travelled += Math.abs(velocity);
         if (travelled >= clearance) {
            return ticks;
         }
         velocity = (velocity - 0.08D) * 0.98D;
      }
      return 20;
   }

   private double groundClearance() {
      Box box = mc.player.getBoundingBox();
      Vec3d start = new Vec3d(mc.player.getX(), box.minY + 0.05D, mc.player.getZ());
      HitResult result = mc.world.raycast(new RaycastContext(
            start,
            start.add(0.0D, -48.0D, 0.0D),
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player));
      if (result.getType() != HitResult.Type.BLOCK) {
         return Double.MAX_VALUE;
      }
      return Math.max(0.0D, start.y - 0.05D - result.getPos().y);
   }

   private int computeTicksToExitReach() {
      if (target == null) {
         return 0;
      }

      double remaining = effectiveAttackRange() - mc.player.distanceTo(target);
      if (remaining <= 0.0D) {
         return 0;
      }

      Vec3d relativeVelocity = mc.player.getVelocity().subtract(target.getVelocity());
      Vec3d direction = target.getEyePos().subtract(mc.player.getEyePos());
      if (direction.dotProduct(relativeVelocity) >= 0.0D) {
         return Integer.MAX_VALUE;
      }

      double speed = relativeVelocity.length();
      return speed <= 0.02D ? Integer.MAX_VALUE : Math.max(1, (int) Math.ceil(remaining / speed));
   }

   private boolean isPaused() {
      return pauseInGui.get() && mc.currentScreen != null
            || pauseWhileEating.get() && CombatUtil.isEating(mc);
   }

   private boolean shouldResetCycle() {
      return mc.player.isOnGround() || mc.player.getVelocity().y >= 0.0D || mc.player.fallDistance < 0.1F;
   }

   private boolean canAimDuringFall() {
      return !mc.player.isOnGround()
            && !mc.player.isGliding()
            && !mc.player.isClimbing()
            && !mc.player.isTouchingWater()
            && !mc.player.isInLava()
            && mc.player.getVelocity().y < -0.01D;
   }

   private boolean hasSmashFallRequirement() {
      return canAimDuringFall()
            && mc.player.fallDistance >= Math.max(MIN_DROP, minFallDistance.get());
   }

   private boolean isAttackReady() {
      if (System.currentTimeMillis() - lastAttackMs < Math.round(cooldown.get())) {
         return false;
      }
      float perTick = Math.max(1.0F, mc.player.getAttackCooldownProgressPerTick());
      return mc.player.getAttackCooldownProgress(0.0F) * perTick >= SMASH_CHARGE_TICKS;
   }

   private double effectiveAttackRange() {
      return Math.min(Math.max(2.5D, swingRange.get()), MAX_ATTACK_RANGE);
   }

   private double engagementRange() {
      return effectiveAttackRange() + Math.max(1.0D, preAimDistance.get());
   }

   private boolean withinReach(LivingEntity entity) {
      Vec3d eye = mc.player.getEyePos();
      Box box = entity.getBoundingBox();
      double x = MathHelper.clamp(eye.x, box.minX, box.maxX);
      double y = MathHelper.clamp(eye.y, box.minY, box.maxY);
      double z = MathHelper.clamp(eye.z, box.minZ, box.maxZ);
      return eye.squaredDistanceTo(x, y, z) <= effectiveAttackRange() * effectiveAttackRange();
   }

   private LivingEntity findTarget() {
      LivingEntity best = null;
      double bestScore = Double.MAX_VALUE;
      for (PlayerEntity player : mc.world.getPlayers()) {
         if (!isTargetValid(player) || mc.player.distanceTo(player) > engagementRange()) {
            continue;
         }

         Vec3d point = getAimPoint(player);
         if (!inFov(point)) {
            continue;
         }

         float[] rotation = CombatUtil.rotationTo(mc, point);
         double distance = mc.player.squaredDistanceTo(point);
         double angle = Math.abs(MathHelper.wrapDegrees(rotation[0] - mc.player.getYaw()))
               + Math.abs(rotation[1] - mc.player.getPitch());
         double score = targetPriority.get() ? distance + angle * 0.05D : distance;
         if (score < bestScore) {
            bestScore = score;
            best = player;
         }
      }
      return best;
   }

   private boolean isTargetValid(LivingEntity entity) {
      return entity instanceof PlayerEntity player
            && player != mc.player
            && player.isAlive()
            && !player.isDead()
            && !player.isRemoved()
            && !player.isSpectator()
            && !player.isCreative()
            && !mc.player.isTeammate(player)
            && player.getY() - mc.player.getY() <= 3.5D
            && mc.player.canSee(player);
   }

   private boolean isTargetBlocking(PlayerEntity player) {
      if (!player.isBlocking()) {
         return false;
      }

      Vec3d toAttacker = mc.player.getEyePos().subtract(player.getEyePos());
      double horizontalLength = Math.sqrt(toAttacker.x * toAttacker.x + toAttacker.z * toAttacker.z);
      if (horizontalLength < 1.0E-6D) {
         return true;
      }

      double yaw = Math.toRadians(player.getHeadYaw());
      double facingX = -Math.sin(yaw);
      double facingZ = Math.cos(yaw);
      double dot = (facingX * toAttacker.x + facingZ * toAttacker.z) / horizontalLength;
      return dot > 0.0D;
   }

   private boolean inFov(Vec3d point) {
      float[] rotation = CombatUtil.rotationTo(mc, point);
      float yaw = Math.abs(MathHelper.wrapDegrees(rotation[0] - mc.player.getYaw()));
      float pitch = Math.abs(rotation[1] - mc.player.getPitch());
      return yaw <= yawFov.get() * 0.5F && pitch <= pitchFov.get() * 0.5F;
   }

   private Vec3d getAimPoint(LivingEntity entity) {
      Box box = entity.getBoundingBox();
      Vec3d eye = mc.player.getEyePos();
      Vec3d closest = new Vec3d(
            MathHelper.clamp(eye.x, box.minX, box.maxX),
            MathHelper.clamp(eye.y, box.minY, box.maxY),
            MathHelper.clamp(eye.z, box.minZ, box.maxZ));
      return box.getCenter().lerp(closest, AIM_EDGE);
   }

   private EntityHitResult raycastTarget(LivingEntity entity) {
      if (entity == null || !withinReach(entity)) {
         return null;
      }

      Vec3d eye = mc.player.getEyePos();
      Vec3d end = eye.add(mc.player.getRotationVec(1.0F).multiply(effectiveAttackRange() + 0.2D));
      double maxDistance = effectiveAttackRange() * effectiveAttackRange();
      BlockHitResult block = mc.world.raycast(new RaycastContext(
            eye,
            end,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player));
      if (block.getType() == HitResult.Type.BLOCK) {
         maxDistance = Math.min(maxDistance, eye.squaredDistanceTo(block.getPos()));
      }

      Optional<Vec3d> hitPosition = entity.getBoundingBox().expand(entity.getTargetingMargin()).raycast(eye, end);
      if (hitPosition.isEmpty() || eye.squaredDistanceTo(hitPosition.get()) > maxDistance) {
         return null;
      }
      return new EntityHitResult(entity, hitPosition.get());
   }

   private int resolveMaceSlot() {
      int selected = mc.player.getInventory().getSelectedSlot();
      if (!autoSwitch.get()) {
         return isMaceSlot(selected) ? selected : -1;
      }
      return findBestMace();
   }

   private int findAxe() {
      for (int slot = 0; slot < 9; slot++) {
         if (mc.player.getInventory().getStack(slot).getItem() instanceof AxeItem) {
            return slot;
         }
      }
      return -1;
   }

   private int findBestMace() {
      int bestSlot = -1;
      int bestWindBurst = -1;
      for (int slot = 0; slot < 9; slot++) {
         ItemStack candidate = mc.player.getInventory().getStack(slot);
         if (!(candidate.getItem() instanceof MaceItem)) {
            continue;
         }
         int windBurst = enchantmentLevel(candidate, "wind_burst");
         if (bestSlot < 0 || windBurst > bestWindBurst) {
            bestSlot = slot;
            bestWindBurst = windBurst;
         }
      }
      return bestSlot;
   }

   private int enchantmentLevel(ItemStack stack, String name) {
      ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
      if (enchantments == null || enchantments.isEmpty()) {
         return 0;
      }

      int level = 0;
      for (var enchantment : enchantments.getEnchantments()) {
         if (enchantment.getIdAsString().toLowerCase().contains(name)) {
            level = Math.max(level, enchantments.getLevel(enchantment));
         }
      }
      return level;
   }

   private boolean isSelectedMace(int slot) {
      return mc.player.getInventory().getSelectedSlot() == slot
            && mc.player.getMainHandStack().getItem() instanceof MaceItem;
   }

   private boolean isMaceSlot(int slot) {
      return slot >= 0 && slot < 9
            && mc.player.getInventory().getStack(slot).getItem() instanceof MaceItem;
   }

   private void finishCycle() {
      restoreSlot();
      clearSequence();
      state = State.LOCKED;
   }

   private void resetCycle() {
      restoreSlot();
      clearSequence();
      state = State.IDLE;
      cycleConsumed = false;
      target = null;
      aimer.reset();
   }

   private void clearSequence() {
      hit.release();
      stateStartedAge = -1;
      axeAttackAge = -1;
      preparedAxeSlot = -1;
      preparedMaceSlot = -1;
      retries = 0;
      lastRetryAge = -1;
      stunAxeExecuted = false;
      stunMaceExecuted = false;
      axeExecutedTickId = -1L;
      stunStartedMs = 0L;
      directHitDueNs = 0L;
      directHitDeadlineNs = 0L;
   }

   private void restoreSlot() {
      if (swapBack.get()) {
         slots.restore();
      } else {
         slots.reset();
      }
   }

   private void reset() {
      restoreSlot();
      hit.release();
      aimer.reset();
      target = null;
      state = State.IDLE;
      stateStartedAge = -1;
      axeAttackAge = -1;
      preparedAxeSlot = -1;
      preparedMaceSlot = -1;
      retries = 0;
      lastRetryAge = -1;
      cycleConsumed = false;
      stunAxeExecuted = false;
      stunMaceExecuted = false;
      clientTickId = 0L;
      axeExecutedTickId = -1L;
      stunStartedMs = 0L;
      directHitDueNs = 0L;
      directHitDeadlineNs = 0L;
      lastAttackMs = 0L;
   }
}
