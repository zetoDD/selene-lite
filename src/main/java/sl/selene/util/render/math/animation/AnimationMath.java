package sl.selene.util.render.math.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class AnimationMath {
   public static double deltaTime() {
      return MinecraftClient.getInstance().getCurrentFps() > 0 ? 1.0 / MinecraftClient.getInstance().getCurrentFps() : 1.0;
   }

   public static float fast(float end, float start, float multiple) {
      return (1.0F - MathHelper.clamp((float)(deltaTime() * multiple), 0.0F, 1.0F)) * end
         + MathHelper.clamp((float)(deltaTime() * multiple), 0.0F, 1.0F) * start;
   }

   public static float animation(float animation, float target, float speedTarget) {
      float dif = (target - animation) / Math.max((float)MinecraftClient.getInstance().getCurrentFps(), 5.0F) * 15.0F;
      if (dif > 0.0F) {
         dif = Math.max(speedTarget, dif);
         dif = Math.min(target - animation, dif);
      } else if (dif < 0.0F) {
         dif = Math.min(-speedTarget, dif);
         dif = Math.max(target - animation, dif);
      }

      return animation + dif;
   }

   public static double Interpolate(double current, double old, double scale) {
      return old + (current - old) * scale;
   }

   public static float calculateCompensation(float target, float current, float delta, double speed) {
      float diff = current - target;
      if (delta < 1.0F) {
         delta = 1.0F;
      }

      if (delta > 1000.0F) {
         delta = 16.0F;
      }

      double dif = Math.max(speed * delta / 16.666666F, 0.5);
      if (diff > speed) {
         if ((current = (float)(current - dif)) < target) {
            current = target;
         }
      } else if (diff < -speed) {
         if ((current = (float)(current + dif)) > target) {
            current = target;
         }
      } else {
         current = target;
      }

      return current;
   }

   public static float Move(float from, float to, float minstep, float maxstep, float factor) {
      float f = (to - from) * MathHelper.clamp(factor, 0.0F, 1.0F);
      if (f < 0.0F) {
         f = MathHelper.clamp(f, -maxstep, -minstep);
      } else {
         f = MathHelper.clamp(f, minstep, maxstep);
      }

      return Math.abs(f) > Math.abs(to - from) ? to : from + f;
   }

   public static double animate(double target, double current, double speed) {
      boolean larger = target > current;
      if (speed < 0.0) {
         speed = 0.0;
      } else if (speed > 1.0) {
         speed = 1.0;
      }

      double dif = Math.max(target, current) - Math.min(target, current);
      double factor = dif * speed;
      if (factor < 0.1) {
         factor = 0.1;
      }

      if (larger) {
         current += factor;
      } else {
         current -= factor;
      }

      return current;
   }
}