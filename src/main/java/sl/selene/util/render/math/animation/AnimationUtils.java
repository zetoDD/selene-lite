package sl.selene.util.render.math.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.render.math.MathHelper;

@Environment(EnvType.CLIENT)
public class AnimationUtils {
   long mc;
   public float anim;
   public float to;
   public float speed;

   public AnimationUtils(float anim, float to, float speed) {
      this.anim = anim;
      this.to = to;
      this.speed = speed;
      this.mc = System.currentTimeMillis();
   }

   public float getAnim() {
      if (Math.abs(this.anim - this.to) < 1.0E-4) {
         this.anim = this.to;
      }

      int count;
      if ((count = (int)(Math.min((float)(System.currentTimeMillis() - this.mc), 400.0F) / 5.0F)) > 0) {
         this.mc = System.currentTimeMillis();
      }

      for (int i = 0; i < count; i++) {
         this.anim = MathHelper.lerp(this.anim, this.to, this.speed);
      }

      return this.anim;
   }

   public float getAngleAnim() {
      if (Math.abs(this.anim - this.to) > 1.0E-4) {
         int count = (int)(Math.min((float)(System.currentTimeMillis() - this.mc), 400.0F) / 5.0F);
         if (count > 0) {
            this.mc = System.currentTimeMillis();
         }

         for (int i = 0; i < count; i++) {
            this.anim = (float)this.lerpAngle(this.anim, this.to, this.speed);
         }
      }

      return MathHelper.wrapAngleTo180_float(this.anim);
   }

   public void setAnim(float anim) {
      this.anim = anim;
      this.mc = System.currentTimeMillis();
   }

   double lerpAngle(float start, float end, float amount) {
      float minAngle = (end - start + 180.0F) % 360.0F - 180.0F;
      return minAngle * amount + start;
   }
}