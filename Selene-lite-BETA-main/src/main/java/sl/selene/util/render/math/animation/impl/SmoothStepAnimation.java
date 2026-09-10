package sl.selene.util.render.math.animation.impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.render.math.animation.Animation;
import sl.selene.util.render.math.animation.Direction;

@Environment(EnvType.CLIENT)
public class SmoothStepAnimation extends Animation {
   public SmoothStepAnimation(int ms, double endPoint) {
      super(ms, endPoint);
   }

   public SmoothStepAnimation(int ms, double endPoint, Direction direction) {
      super(ms, endPoint, direction);
   }

   @Override
   protected double getEquation(double x) {
      double x1 = x / this.duration;
      return -2.0 * Math.pow(x1, 3.0) + 3.0 * Math.pow(x1, 2.0);
   }
}