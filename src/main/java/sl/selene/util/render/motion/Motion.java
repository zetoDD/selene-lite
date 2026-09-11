package sl.selene.util.render.motion;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.render.math.animation.AnimationMath;

@Environment(EnvType.CLIENT)
public final class Motion {

   public static final float HOVER_SPEED = 14.0F;
   public static final float CHIP_SPEED = 10.0F;
   public static final float TAB_DOCK_SPEED = 15.0F;
   public static final float BAR_SPEED = 15.0F;

   public static final float PRESS_STIFFNESS = 520.0F;
   public static final float PRESS_DAMPING = 21.0F;
   public static final float ELASTIC_STIFFNESS = 240.0F;
   public static final float ELASTIC_DAMPING = 24.0F;

   public static final float DUR_FAST = 0.15F;
   public static final float DUR_NORMAL = 0.24F;
   public static final float DUR_CONTENT = 0.3F;

   public static final long CURSOR_BLINK_HALF_PERIOD_MS = 500L;

   private static final float MIN_DELTA_SECONDS = 5.0E-4F;
   private static final float MAX_DELTA_SECONDS = 0.1F;
   private static final float SPRING_STEP_SECONDS = 1.0F / 240.0F;
   private static final int MAX_SPRING_STEPS = 32;
   private static final long FRAME_FRESHNESS_NANOS = 250_000_000L;

   private static volatile float frameDeltaSeconds = -1.0F;
   private static volatile long frameStampNanos = 0L;

   private Motion() {
   }

   public static void beginFrame(float dynamicDeltaTicks) {
      float seconds = dynamicDeltaTicks * 0.05F;
      if (!(seconds >= MIN_DELTA_SECONDS)) {
         seconds = MIN_DELTA_SECONDS;
      } else if (seconds > MAX_DELTA_SECONDS) {
         seconds = MAX_DELTA_SECONDS;
      }

      frameDeltaSeconds = seconds;
      frameStampNanos = System.nanoTime();
   }

   public static float deltaSeconds() {
      float stored = frameDeltaSeconds;
      if (stored > 0.0F && System.nanoTime() - frameStampNanos < FRAME_FRESHNESS_NANOS) {
         return stored;
      }

      float estimated = (float)AnimationMath.deltaTime();
      if (!(estimated >= MIN_DELTA_SECONDS)) {
         estimated = MIN_DELTA_SECONDS;
      }

      return Math.min(estimated, MAX_DELTA_SECONDS);
   }

   public static float smooth(float current, float target, float speed) {
      float blend = 1.0F - (float)Math.exp(-Math.max(0.0F, speed) * deltaSeconds());
      return current + (target - current) * blend;
   }

   public static void spring(Motion.Spring spring, float target, float stiffness, float damping) {
      float delta = deltaSeconds();
      int steps = Math.max(1, Math.min(MAX_SPRING_STEPS, (int)Math.ceil(delta / SPRING_STEP_SECONDS)));
      float step = delta / (float)steps;
      for (int i = 0; i < steps; i++) {
         float displacement = spring.value - target;
         float acceleration = -stiffness * displacement - damping * spring.velocity;
         spring.velocity += acceleration * step;
         spring.value += spring.velocity * step;
      }
   }

   public static float blink(long halfPeriodMs) {
      long period = Math.max(1L, halfPeriodMs) * 2L;
      double phase = (double)(System.currentTimeMillis() % period) / (double)period * 2.0 * Math.PI;
      return 0.5F - (float)Math.cos(phase) * 0.5F;
   }

   @Environment(EnvType.CLIENT)
   public static final class Spring {
      public float value;
      public float velocity;

      public Spring(float initialValue) {
         this.value = initialValue;
      }
   }
}

