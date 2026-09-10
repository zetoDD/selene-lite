package sl.selene.event.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class EventRotation extends Event {
   public float yaw;
   public float pitch;
   public float partialTicks;

   public EventRotation(float yaw, float pitch, float partialTicks) {
      this.yaw = yaw;
      this.pitch = pitch;
      this.partialTicks = partialTicks;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }

   public void setPartialTicks(float partialTicks) {
      this.partialTicks = partialTicks;
   }
}