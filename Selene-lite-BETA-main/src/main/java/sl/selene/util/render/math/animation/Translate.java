package sl.selene.util.render.math.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Translate {
   private float x;
   private float y;

   public Translate(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public void interpolate(float targetX, float targetY, float smoothing) {
      this.x = this.animate(targetX, this.x, smoothing);
      this.y = this.animate(targetY, this.y, smoothing);
   }

   public void animate(float newX, float newY) {
      this.x = this.animate(this.x, newX, 1.0F);
      this.y = this.animate(this.y, newY, 1.0F);
   }

   public float animate(float target, float current, float speed) {
      if (speed < 0.0F) {
         speed = 0.0F;
      }

      if (speed > 1.0F) {
         speed = 1.0F;
      }

      float dif = target - current;
      float factor = Math.abs(dif) * speed;
      return factor < 0.1F ? target : current + (dif > 0.0F ? factor : -factor);
   }

   public float getX() {
      return this.x;
   }

   public void setX(float x) {
      this.x = x;
   }

   public float getY() {
      return this.y;
   }

   public void setY(float y) {
      this.y = y;
   }
}