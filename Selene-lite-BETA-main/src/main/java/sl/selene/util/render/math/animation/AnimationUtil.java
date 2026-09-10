package sl.selene.util.render.math.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public class AnimationUtil {
   public static double delta;

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

   public static double animation(double animation, double target, double speedTarget) {
      double dif = (target - animation) / Math.max(MinecraftClient.getInstance().getCurrentFps(), 5) * speedTarget;
      if (dif > 0.0) {
         dif = Math.max(speedTarget, dif);
         dif = Math.min(target - animation, dif);
      } else if (dif < 0.0) {
         dif = Math.min(-speedTarget, dif);
         dif = Math.max(target - animation, dif);
      }

      return animation + dif;
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
         if ((current = current - (float)dif) < target) {
            current = target;
         }
      } else if (diff < -speed) {
         if ((current = current + (float)dif) > target) {
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

   public static double Interpolate(double start, double end, double step) {
      return start + (end - start) * step;
   }

   public static float getAnimationState(float animation, float finalState, float speed) {
      float add = (float)(delta * (speed / 1000.0F));
      if (animation < finalState) {
         if (animation + add < finalState) {
            animation += add;
         } else {
            animation = finalState;
         }
      } else if (animation - add > finalState) {
         animation -= add;
      } else {
         animation = finalState;
      }

      return animation;
   }

   public static void scaleAnimation(float x, float y, float scale, Runnable data) {
      GL11.glPushMatrix();
      GL11.glTranslatef(x, y, 0.0F);
      GL11.glScalef(scale, scale, 1.0F);
      GL11.glTranslatef(-x, -y, 0.0F);
      data.run();
      GL11.glPopMatrix();
   }

   public static void translateAnimation(float x, float y, Runnable data) {
      GL11.glPushMatrix();
      GL11.glTranslatef(x, y, 0.0F);
      data.run();
      GL11.glPopMatrix();
   }
}