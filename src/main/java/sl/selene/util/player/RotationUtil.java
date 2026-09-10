package sl.selene.util.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sl.selene.event.EventInit;
import sl.selene.event.EventManager;
import sl.selene.event.impl.EventScreen;
import sl.selene.module.api.Module;
import sl.selene.util.other.IMinecraft;

@Environment(EnvType.CLIENT)
public final class RotationUtil implements IMinecraft {

   public enum AimType {
      REGULAR,
      BLATANT,
      STEADY
   }

   public static final int PRIORITY_HIGHEST = 5;
   public static final int PRIORITY_HIGH = 4;
   public static final int PRIORITY_NORMAL = 3;
   public static final int PRIORITY_LOW = 2;
   public static final int PRIORITY_LOWEST = 1;

   private static final float DEADZONE = 0.6F;
   private static final float WIND_GAIN = 6.0F;
   private static final float SILENT_SPEED_MULT = 2.5F;
   private static final float VISIBLE_SPEED_MULT = 1.0F;
   private static final float JITTER = 0.3F;
   private static final double MICRO_PAUSE_CHANCE = 0.035D;

   private static float serverYaw;
   private static float serverPitch;
   private static boolean hasSilentRotation;
   private static boolean isTracking;
   private static Module currentController;
   private static int currentPriority = Integer.MIN_VALUE;

   private static Vec3d activePoint;
   private static boolean activeSilent;
   private static AimType activeAimType = AimType.REGULAR;
   private static float activeSpeed;
   private static long lastRefreshNanos;
   private static final long STALE_AFTER_NANOS = 500_000_000L;

   private static long lastFrameNanos;
   private static boolean reactionDone;
   private static long nextAssistAt;
   private static long pauseUntil;
   private static double jitterPhase = Math.random() * Math.PI * 2.0;
   private static double jitterFreq = 9.0 + Math.random() * 5.0;

   private static boolean frameListenerRegistered;

   private RotationUtil() {
   }


   public static boolean tryTakeControl(Module requester, int priority) {
      if (currentController != null && currentController != requester && priority <= currentPriority) {
         return false;
      }
      currentController = requester;
      currentPriority = priority;
      return true;
   }

   public static void releaseControl(Module requester) {
      if (currentController == requester) {
         currentController = null;
         currentPriority = Integer.MIN_VALUE;
      }
   }

   public static boolean isControlledBy(Module module) {
      return currentController == module;
   }

   public static void stopTracking() {
      if (mc.player != null) {
         serverYaw = mc.player.getYaw();
         serverPitch = mc.player.getPitch();
      } else {
         serverYaw = 0.0F;
         serverPitch = 0.0F;
      }
      hasSilentRotation = false;
      isTracking = false;
      currentController = null;
      currentPriority = Integer.MIN_VALUE;
      activePoint = null;
      resetHumanizer();
   }

   public static void cleanup(Module requester) {
      if (!isControlledBy(requester)) {
         return;
      }
      stopTracking();
   }


   public static boolean aim(Module requester, int priority, Vec3d point, boolean silent, AimType aimType, float speed) {
      if (point == null || mc.player == null || mc.world == null) {
         return false;
      }
      boolean freshController = currentController != requester;
      if (!tryTakeControl(requester, priority)) {
         return false;
      }
      if (!isTracking) {
         initFromPlayer();
         isTracking = true;
      }
      if (freshController) {
         resetHumanizer();
      }

      activePoint = point;
      activeSilent = silent;
      activeAimType = aimType;
      activeSpeed = speed;
      lastRefreshNanos = System.nanoTime();
      ensureFrameListener();
      return true;
   }

   private static void ensureFrameListener() {
      if (frameListenerRegistered) {
         return;
      }
      frameListenerRegistered = true;
      EventManager.register(new Object() {
         @EventInit
         public void onFrame(EventScreen event) {
            stepFrame();
         }
      });
   }

   private static void stepFrame() {
      if (activePoint == null || currentController == null || mc.player == null || mc.world == null) {
         return;
      }
      if (System.nanoTime() - lastRefreshNanos > STALE_AFTER_NANOS) {
         activePoint = null;
         return;
      }
      if (mc.currentScreen != null) {
         return;
      }

      float delta = getDeltaSeconds();
      float pt = mc.getRenderTickCounter().getTickProgress(false);
      float[] targetRot = calculate(mc.player.getCameraPosVec(pt), activePoint);

      float baseYaw;
      float basePitch;
      if (activeSilent) {
         baseYaw = serverYaw;
         basePitch = serverPitch;
      } else {
         baseYaw = mc.player.getYaw();
         basePitch = mc.player.getPitch();
      }

      float yawDiff = MathHelper.wrapDegrees(targetRot[0] - MathHelper.wrapDegrees(baseYaw));
      float pitchDiff = MathHelper.clamp(targetRot[1], -90.0F, 90.0F) - basePitch;

      if (activeAimType == AimType.BLATANT) {
         applyStep(baseYaw, basePitch, yawDiff, pitchDiff, activeSilent);
         return;
      }

      if (Math.abs(yawDiff) < DEADZONE && Math.abs(pitchDiff) < DEADZONE) {
         return;
      }

      if (activeAimType == AimType.STEADY) {
         float maxRate = Math.max(1.0F, activeSpeed) * (activeSilent ? SILENT_SPEED_MULT : VISIBLE_SPEED_MULT);
         float maxStep = Math.max(0.05F, maxRate * delta);
         float speedScale = maxRate / 120.0F;
         float frac = Math.min(0.65F, WIND_GAIN * delta * speedScale);
         float yawStep = MathHelper.clamp(yawDiff * frac, -maxStep, maxStep);
         float pitchStep = MathHelper.clamp(pitchDiff * frac, -maxStep, maxStep);
         applyStep(baseYaw, basePitch, yawStep, pitchStep, activeSilent);
         return;
      }

      long now = System.nanoTime();
      if (!reactionDone) {
         reactionDone = true;
         nextAssistAt = now + (long) (40L + Math.random() * 120L) * 1_000_000L;
      }
      if (now < nextAssistAt || now < pauseUntil) {
         return;
      }
      if (Math.random() < MICRO_PAUSE_CHANCE) {
         pauseUntil = now + (long) (30L + Math.random() * 90L) * 1_000_000L;
         return;
      }

      float maxRate = Math.max(1.0F, activeSpeed) * (activeSilent ? SILENT_SPEED_MULT : VISIBLE_SPEED_MULT);
      float maxStep = Math.max(0.05F, maxRate * delta);
      float error = Math.max(0.001F, (float) Math.sqrt((double) yawDiff * yawDiff + (double) pitchDiff * pitchDiff));
      float windStep = Math.min(error * WIND_GAIN * delta, maxStep);
      float scale = windStep / error;
      float yawStep = yawDiff * scale;
      float pitchStep = pitchDiff * scale;

      float humanVar = 0.8F + (float) Math.random() * 0.45F;
      yawStep *= humanVar;
      pitchStep *= humanVar;

      yawStep = MathHelper.clamp(yawStep, -maxStep, maxStep);
      pitchStep = MathHelper.clamp(pitchStep, -maxStep, maxStep);

      jitterPhase += jitterFreq * (double) delta;
      float jy = (float) Math.sin(jitterPhase) * JITTER * delta * 4.0F;
      float jp = (float) Math.sin(jitterPhase * 1.7 + 1.3) * JITTER * delta * 4.0F;
      yawStep = MathHelper.clamp(yawStep + jy, -maxStep, maxStep);
      pitchStep = MathHelper.clamp(pitchStep + jp, -maxStep, maxStep);

      applyStep(baseYaw, basePitch, yawStep, pitchStep, activeSilent);
   }

   private static void applyStep(float baseYaw, float basePitch, float yawStep, float pitchStep, boolean silent) {
      double sens = (Double) mc.options.getMouseSensitivity().getValue();
      double f = sens * 0.6D + 0.2D;
      double cube = f * f * f;
      double perspective = mc.options.getPerspective().isFirstPerson() ? 1.0D : 8.0D;
      double perCount = cube * 0.15D * perspective;

      long yawCounts = perCount > 1.0E-9D ? Math.round((double) yawStep / perCount) : 0L;
      long pitchCounts = perCount > 1.0E-9D ? Math.round((double) pitchStep / perCount) : 0L;
      if (yawCounts == 0L && pitchCounts == 0L) {
         return;
      }

      float yawDelta = (float) ((double) yawCounts * perCount);
      float pitchDelta = (float) ((double) pitchCounts * perCount);

      if (silent) {
         serverYaw = baseYaw + yawDelta;
         serverPitch = MathHelper.clamp(basePitch + pitchDelta, -90.0F, 90.0F);
         hasSilentRotation = true;
      } else {
         mc.player.setYaw(baseYaw + yawDelta);
         mc.player.setPitch(MathHelper.clamp(basePitch + pitchDelta, -90.0F, 90.0F));
         hasSilentRotation = false;
      }
   }

   private static void initFromPlayer() {
      if (mc.player != null) {
         serverYaw = mc.player.getYaw();
         serverPitch = mc.player.getPitch();
      }
      hasSilentRotation = false;
   }

   private static void resetHumanizer() {
      lastFrameNanos = 0L;
      reactionDone = false;
      nextAssistAt = 0L;
      pauseUntil = 0L;
      jitterPhase = Math.random() * Math.PI * 2.0;
      jitterFreq = 9.0 + Math.random() * 5.0;
   }

   private static float getDeltaSeconds() {
      long now = System.nanoTime();
      float delta = lastFrameNanos == 0L ? 0.016F : (now - lastFrameNanos) / 1_000_000_000.0F;
      lastFrameNanos = now;
      if (delta <= 0.0F || delta > 0.1F) {
         delta = 0.016F;
      }
      return delta;
   }


   public static float[] calculate(Vec3d from, Vec3d to) {
      Vec3d diff = to.subtract(from);
      double distance = Math.hypot(diff.x, diff.z);
      float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
      float pitch = MathHelper.wrapDegrees((float) (-Math.toDegrees(Math.atan2(diff.y, distance))));
      return new float[] { yaw, pitch };
   }

   public static Vec3d getRotationVector(float pitch, float yaw) {
      float pitchRad = pitch * (float) (Math.PI / 180.0);
      float yawRad = -yaw * (float) (Math.PI / 180.0);
      float cosPitch = MathHelper.cos(pitchRad);
      float sinPitch = MathHelper.sin(pitchRad);
      float cosYaw = MathHelper.cos(yawRad);
      float sinYaw = MathHelper.sin(yawRad);
      return new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
   }

   public static boolean isAimed(Vec3d point, float toleranceDeg) {
      if (point == null || mc.player == null) {
         return false;
      }
      float[] target = calculate(mc.player.getEyePos(), point);
      float baseYaw = hasSilentRotation() ? serverYaw : mc.player.getYaw();
      float basePitch = hasSilentRotation() ? serverPitch : mc.player.getPitch();
      float yawDiff = Math.abs(MathHelper.wrapDegrees(target[0] - baseYaw));
      float pitchDiff = Math.abs(target[1] - basePitch);
      return yawDiff <= toleranceDeg && pitchDiff <= toleranceDeg;
   }


   public static BlockHitResult raycastFromRotation(float yaw, float pitch, float partialTick) {
      if (mc.player == null || mc.world == null) {
         return null;
      }
      Vec3d eye = mc.player.getCameraPosVec(partialTick);
      Vec3d look = getRotationVector(pitch, yaw);
      Vec3d end = eye.add(look.multiply(mc.player.getBlockInteractionRange()));
      return mc.world.raycast(
         new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player)
      );
   }

   public static boolean rayHitsEntity(net.minecraft.entity.Entity target, double reach, float expand) {
      if (target == null || mc.player == null || mc.world == null) {
         return false;
      }
      Vec3d eye = mc.player.getEyePos();
      float pitch = hasSilentRotation() ? serverPitch : mc.player.getPitch();
      float yaw = hasSilentRotation() ? serverYaw : mc.player.getYaw();
      Vec3d look = getRotationVector(pitch, yaw);
      Vec3d end = eye.add(look.multiply(reach));
      return target.getBoundingBox().expand(expand).raycast(eye, end).isPresent();
   }

   public static boolean rayIntersectsBlock(float yaw, float pitch, BlockPos pos, float partialTick) {
      if (mc.player == null || pos == null) {
         return false;
      }
      Vec3d eye = mc.player.getCameraPosVec(partialTick);
      Vec3d look = getRotationVector(pitch, yaw);
      Vec3d end = eye.add(look.multiply(mc.player.getBlockInteractionRange()));
      return new Box(pos).expand(1.0E-4).raycast(eye, end).isPresent();
   }


   public static float getServerYaw() {
      return serverYaw;
   }

   public static float getServerPitch() {
      return serverPitch;
   }

   public static boolean hasSilentRotation() {
      return hasSilentRotation;
   }

   public static boolean isTracking() {
      return isTracking;
   }
}