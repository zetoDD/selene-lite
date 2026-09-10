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
   private double wobblePhase = Math.random() * Math.PI * 2.0;
   private double wobbleFreq = 1.6 + Math.random() * 1.4;

   public void reset() {
      lastFrameNanos = 0L;
      wobblePhase = Math.random() * Math.PI * 2.0;
      wobbleFreq = 1.6 + Math.random() * 1.4;
   }

   public void aim(Vec3d aim, float turnSpeed, float speed, float smoothness, float wobble,
                   String aimMode) {
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

      yawStep = MathHelper.clamp(yawStep, -maxStep, maxStep);
      pitchStep = MathHelper.clamp(pitchStep, -maxStep, maxStep);

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