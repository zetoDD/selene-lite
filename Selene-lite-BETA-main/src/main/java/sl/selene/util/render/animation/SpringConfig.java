package sl.selene.util.render.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SpringConfig {
   private final float frequencyHz;
   private final float dampingRatio;

   private SpringConfig(float frequencyHz, float dampingRatio) {
      if (frequencyHz <= 0.0F) {
         throw new IllegalArgumentException("frequencyHz must be > 0");
      } else if (dampingRatio <= 0.0F) {
         throw new IllegalArgumentException("dampingRatio must be > 0");
      } else {
         this.frequencyHz = frequencyHz;
         this.dampingRatio = dampingRatio;
      }
   }

   public static SpringConfig of(float frequencyHz, float dampingRatio) {
      return new SpringConfig(frequencyHz, dampingRatio);
   }

   public float getFrequencyHz() {
      return this.frequencyHz;
   }

   public float getDampingRatio() {
      return this.dampingRatio;
   }
}