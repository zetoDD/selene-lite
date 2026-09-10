package sl.selene.util.render.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SpringFloatAnimator implements AnimationSystem.Animated {
   private final AnimationSystem animationSystem;
   private final SpringConfig config;
   private final float minValue;
   private final float maxValue;
   private final float positionTolerance;
   private final float velocityTolerance;
   private float value;
   private float target;
   private float velocity;
   private EasingFunction outputTransform = EasingFunction.identity();

   public SpringFloatAnimator(
      AnimationSystem animationSystem,
      SpringConfig config,
      float initialValue,
      float minValue,
      float maxValue,
      float positionTolerance,
      float velocityTolerance
   ) {
      if (animationSystem == null) {
         throw new IllegalArgumentException("animationSystem must not be null");
      } else if (config == null) {
         throw new IllegalArgumentException("config must not be null");
      } else if (minValue > maxValue) {
         throw new IllegalArgumentException("minValue must be <= maxValue");
      } else if (!(positionTolerance <= 0.0F) && !(velocityTolerance <= 0.0F)) {
         this.animationSystem = animationSystem;
         this.config = config;
         this.minValue = minValue;
         this.maxValue = maxValue;
         this.positionTolerance = positionTolerance;
         this.velocityTolerance = velocityTolerance;
         float clamped = this.clamp(initialValue);
         this.value = clamped;
         this.target = clamped;
         this.velocity = 0.0F;
      } else {
         throw new IllegalArgumentException("tolerances must be > 0");
      }
   }

   public void setOutputTransform(EasingFunction transform) {
      this.outputTransform = transform == null ? EasingFunction.identity() : transform;
   }

   public void snapTo(float newValue) {
      float clamped = this.clamp(newValue);
      this.value = clamped;
      this.target = clamped;
      this.velocity = 0.0F;
      this.animationSystem.unregister(this);
   }

   public void setTarget(float targetValue) {
      float clamped = this.clamp(targetValue);
      if (Math.abs(clamped - this.target) <= this.positionTolerance * 0.25F) {
         this.target = clamped;
         if (this.isSettled()) {
            this.snapTo(clamped);
         }
      } else {
         this.target = clamped;
         this.animationSystem.ensureRegistered(this);
      }
   }

   public float getValue() {
      float normalized = 0.0F;
      float range = this.maxValue - this.minValue;
      if (range > 0.0F) {
         normalized = (this.value - this.minValue) / range;
      }

      float eased = this.outputTransform.ease(clamp01(normalized));
      return this.minValue + eased * range;
   }

   public float getRawValue() {
      return this.value;
   }

   public float getTarget() {
      return this.target;
   }

   public boolean isSettled() {
      float displacement = Math.abs(this.target - this.value);
      return displacement <= this.positionTolerance && Math.abs(this.velocity) <= this.velocityTolerance;
   }

   @Override
   public boolean update(float deltaSeconds) {
      float dt = deltaSeconds;
      if (deltaSeconds < 1.0E-4F) {
         dt = 1.0E-4F;
      } else if (deltaSeconds > 0.016666668F) {
         dt = 0.016666668F;
      }

      float omega = (float)((Math.PI * 2) * this.config.getFrequencyHz());
      float damping = 2.0F * this.config.getDampingRatio() * omega;
      float stiffness = omega * omega;
      float previousDisplacement = this.value - this.target;
      float acceleration = -stiffness * previousDisplacement - damping * this.velocity;
      this.velocity += acceleration * dt;
      this.value = this.value + this.velocity * dt;
      if (this.value < this.minValue) {
         this.value = this.minValue;
         this.velocity = 0.0F;
         return false;
      } else if (this.value > this.maxValue) {
         this.value = this.maxValue;
         this.velocity = 0.0F;
         return false;
      } else {
         float currentDisplacement = this.value - this.target;
         if ((!(previousDisplacement > 0.0F) || !(currentDisplacement < 0.0F)) && (!(previousDisplacement < 0.0F) || !(currentDisplacement > 0.0F))) {
            if (this.isSettled()) {
               this.value = this.target;
               this.velocity = 0.0F;
               return false;
            } else {
               return true;
            }
         } else {
            this.value = this.target;
            this.velocity = 0.0F;
            return false;
         }
      }
   }

   private float clamp(float candidate) {
      if (candidate <= this.minValue) {
         return this.minValue;
      } else {
         return candidate >= this.maxValue ? this.maxValue : candidate;
      }
   }

   private static float clamp01(float value) {
      if (value <= 0.0F) {
         return 0.0F;
      } else {
         return value >= 1.0F ? 1.0F : value;
      }
   }
}