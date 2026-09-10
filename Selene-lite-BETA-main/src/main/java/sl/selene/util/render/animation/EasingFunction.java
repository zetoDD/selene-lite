package sl.selene.util.render.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface EasingFunction {
   float ease(float var1);

   static EasingFunction identity() {
      return t -> t;
   }

   default EasingFunction compose(EasingFunction after) {
      return after == null ? this : t -> after.ease(this.ease(t));
   }
}