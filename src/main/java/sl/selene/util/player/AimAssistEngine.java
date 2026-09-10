package sl.selene.util.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sl.selene.module.impl.combat.CombatUtil;
import sl.selene.util.other.IMinecraft;

@Environment(EnvType.CLIENT)
public final class AimAssistEngine implements IMinecraft {

   public static final float DEADZONE = 0.6F;

   private long lastFrameNanos;
   private double jitterPhase = Math.random() * Math.PI * 2.0;
   private double wobblePhase = Math.random() * Math.PI * 2.0;
   private double jitterFreq = 9.0 + Math.random() * 5.0;
   private double wobbleFreq = 1.6 + Math.random() * 1.4;
   private boolean reactionDone;
   private long nextAssistAt;
   private long pauseUntil;

   public void reset() {
      lastFrameNanos = 0L;
      jitterPhase = Math.random() * Math.PI * 2.0;
      wobblePhase = Math.random() * Math.PI * 2.0;
      jitterFreq = 9.0 + Math.random() * 5.0;
      wobbleFreq = 1.6 + Math.random() * 1.4;
      reactionDone = false;
      nextAssistAt = 0L;
      pauseUntil = 0L;
   }

   public void resetReaction() {
      reactionDone = false;
      nextAssistAt = 0L;
      pauseUntil = 0L;
   }

   public void aim(Vec3d aim, float turnSpeed, float speed, float smoothness, float jitter, float wobble,
                   String aimMode, boolean humanize) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      long now = System.nanoTime();
      float delta = (now - lastFrameNanos) / 1_000_000_000.0F;
      lastFrameNanos = now;
      if (delta <= 0.0F || delta > 0.1F) {
         delta = 0.016F;
      }

      float wobbleAmount = wobble;
      if (wobbleAmount > 0.0F) {
         this.wobblePhase += this.wobbleFreq * delta;
         double wx = Math.cos(this.wobblePhase) * wobbleAmount;
         double wz = Math.sin(this.wobblePhase * 1.3) * wobbleAmount;
         aim = aim.add(wx, Math.sin(this.wobblePhase * 0.7) * wobbleAmount * 0.5, wz);
      }

      float[] rotation = CombatUtil.rotationTo(mc, aim);
      float targetYaw = rotation[0];
      float targetPitch = MathHelper.clamp(rotation[1], -90.0F, 90.0F);

      float currentYaw = mc.player.getYaw();
      float currentPitch = mc.player.getPitch();
      float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
      float pitchDiff = targetPitch - currentPitch;

      if (Math.abs(yawDiff) < DEADZONE && Math.abs(pitchDiff) < DEADZONE) {
         return;
      }

      if (humanize) {
         if (!this.reactionDone) {
            this.reactionDone = true;
            this.nextAssistAt = now + (long) (40L + Math.random() * 120L) * 1_000_000L;
         }
         if (now < this.nextAssistAt) {
            return;
         }
         if (now < this.pauseUntil) {
            return;
         }
         if (Math.random() < 0.035) {
            this.pauseUntil = now + (long) (30L + Math.random() * 90L) * 1_000_000L;
            return;
         }
      }

      float maxStep = Math.max(0.05F, turnSpeed * delta);

      float yawStep;
      float pitchStep;
      switch (aimMode == null ? "Wind" : aimMode) {
         case "Instant" -> {
            yawStep = yawDiff;
            pitchStep = pitchDiff;
         }
         case "Smooth" -> {
            float response = smoothness;
            float factor = 1.0F - (float) Math.exp(-response * delta);
            float blend = Math.max(0.05F, Math.min(1.0F, factor));
            yawStep = yawDiff * blend;
            pitchStep = pitchDiff * blend;
         }
         default -> {
            float error = Math.max(0.001F, (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff));
            float wind = speed;
            float windStep = error * wind * delta;
            float capped = Math.min(windStep, maxStep);
            float scale = capped / error;
            yawStep = yawDiff * scale;
            pitchStep = pitchDiff * scale;
         }
      }

      if (humanize) {
         float humanVar = 0.8F + (float) Math.random() * 0.45F;
         yawStep *= humanVar;
         pitchStep *= humanVar;
      }

      yawStep = MathHelper.clamp(yawStep, -maxStep, maxStep);
      pitchStep = MathHelper.clamp(pitchStep, -maxStep, maxStep);

      float jitterAmount = jitter;
      if (jitterAmount > 0.0F && !"Instant".equals(aimMode)) {
         this.jitterPhase += this.jitterFreq * delta;
         float jy = (float) Math.sin(this.jitterPhase) * jitterAmount * delta * 4.0F;
         float jp = (float) Math.sin(this.jitterPhase * 1.7 + 1.3) * jitterAmount * delta * 4.0F;
         yawStep = MathHelper.clamp(yawStep + jy, -maxStep, maxStep);
         pitchStep = MathHelper.clamp(pitchStep + jp, -maxStep, maxStep);
      }

      double sens = (Double) mc.options.getMouseSensitivity().getValue();
      double d = sens * 0.6D + 0.2D;
      double cube = d * d * d;
      double perCount = cube * 0.15D * (mc.options.getPerspective().isFirstPerson() ? 1.0D : 8.0D);
      long yawCounts = perCount > 1.0E-9D ? Math.round((double) yawStep / perCount) : 0L;
      long pitchCounts = perCount > 1.0E-9D ? Math.round((double) pitchStep / perCount) : 0L;
      if (yawCounts == 0L && pitchCounts == 0L) {
         return;
      }

      double factor = cube * (mc.options.getPerspective().isFirstPerson() ? 1.0D : 8.0D);
      float yawDelta = (float) ((double) yawCounts * factor * 0.15D);
      float pitchDelta = (float) ((double) pitchCounts * factor * 0.15D);

      mc.player.setYaw(currentYaw + yawDelta);
      mc.player.setPitch(MathHelper.clamp(currentPitch + pitchDelta, -90.0F, 90.0F));
   }
}