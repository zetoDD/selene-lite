package sl.selene.event.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Box;
import net.minecraft.client.util.math.Vector2f;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class EventMotion extends Event {
   private float yaw;
   private float pitch;
   private double posX;
   private double posY;
   private double posZ;
   private boolean onGround;
   private Box aabbFrom;
   Runnable postMotion;

   public EventMotion(float yaw, float pitch, double posX, double posY, double posZ, boolean onGround, Box aabbFrom, Runnable postMotion) {
      this.yaw = yaw;
      this.pitch = pitch;
      this.posX = posX;
      this.posY = posY;
      this.posZ = posZ;
      this.onGround = onGround;
      this.aabbFrom = aabbFrom;
      this.postMotion = postMotion;
   }

   public void setRotate(Vector2f vector2f) {
      this.setYaw(vector2f.x());
      this.setPitch(vector2f.y());
   }

   public Box getAabbFrom() {
      return this.aabbFrom;
   }

   public void setAabbFrom(Box aabbFrom) {
      this.aabbFrom = aabbFrom;
   }

   public Runnable getPostMotion() {
      return this.postMotion;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public double getPosX() {
      return this.posX;
   }

   public double getPosY() {
      return this.posY;
   }

   public double getPosZ() {
      return this.posZ;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public void setPostMotion(Runnable postMotion) {
      this.postMotion = postMotion;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void setPosX(double posX) {
      this.posX = posX;
   }

   public void setPosY(double posY) {
      this.posY = posY;
   }

   public void setPosZ(double posZ) {
      this.posZ = posZ;
   }

   public void setOnGround(boolean onGround) {
      this.onGround = onGround;
   }
}