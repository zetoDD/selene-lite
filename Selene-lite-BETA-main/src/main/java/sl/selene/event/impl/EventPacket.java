package sl.selene.event.impl;

import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.Packet;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class EventPacket extends Event {
   private Packet<?> packet;
   private EventPacket.Type type;

   public boolean isSend() {
      return this.type.equals(EventPacket.Type.SEND);
   }

   @Generated
   public Packet<?> getPacket() {
      return this.packet;
   }

   @Generated
   public EventPacket.Type getType() {
      return this.type;
   }

   @Generated
   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   @Generated
   public void setType(EventPacket.Type type) {
      this.type = type;
   }

   @Generated
   public EventPacket(Packet<?> packet, EventPacket.Type type) {
      this.packet = packet;
      this.type = type;
   }

   @Environment(EnvType.CLIENT)
   public static enum Type {
      SEND,
      RECEIVE;
   }
}