package sl.selene.event.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class EventLook extends Event {
   public double yaw;
   public double pitch;

   public EventLook(double yaw, double pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public double getYaw() {
      return this.yaw;
   }

   public void setYaw(double yaw) {
      this.yaw = yaw;
   }

   public double getPitch() {
      return this.pitch;
   }

   public void setPitch(double pitch) {
      this.pitch = pitch;
   }
}