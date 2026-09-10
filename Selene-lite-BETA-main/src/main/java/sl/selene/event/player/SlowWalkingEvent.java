package sl.selene.event.player;

import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class SlowWalkingEvent extends Event {
   private float moveForward;
   private float moveStrafe;

   public SlowWalkingEvent(float moveForward, float moveStrafe) {
      this.moveForward = moveForward;
      this.moveStrafe = moveStrafe;
   }

   public void hook() {
   }

   @Generated
   public float getMoveForward() {
      return this.moveForward;
   }

   @Generated
   public float getMoveStrafe() {
      return this.moveStrafe;
   }

   @Generated
   public void setMoveForward(float moveForward) {
      this.moveForward = moveForward;
   }

   @Generated
   public void setMoveStrafe(float moveStrafe) {
      this.moveStrafe = moveStrafe;
   }
}