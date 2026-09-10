package sl.selene.event.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class EventInput extends Event {
   private float forward;
   private float strafe;
   private boolean jump;
   private boolean sneak;
   private double sneakSlowDownMultiplier;

   public EventInput(float forward, float strafe, boolean jump, boolean sneak, double sneakSlowDownMultiplier) {
      this.forward = forward;
      this.strafe = strafe;
      this.jump = jump;
      this.sneak = sneak;
      this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
   }

   public float getForward() {
      return this.forward;
   }

   public void setForward(float forward) {
      this.forward = forward;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   public boolean isJump() {
      return this.jump;
   }

   public void setJump(boolean jump) {
      this.jump = jump;
   }

   public boolean isSneak() {
      return this.sneak;
   }

   public void setSneak(boolean sneak) {
      this.sneak = sneak;
   }

   public double getSneakSlowDownMultiplier() {
      return this.sneakSlowDownMultiplier;
   }

   public void setSneakSlowDownMultiplier(double sneakSlowDownMultiplier) {
      this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
   }
}