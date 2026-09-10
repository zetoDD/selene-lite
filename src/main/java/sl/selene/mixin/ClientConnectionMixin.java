package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.event.EventManager;
import sl.selene.event.impl.EventPacket;
import sl.selene.event.player.ScreenCloseEvent;

@Environment(EnvType.CLIENT)
@Mixin({ClientConnection.class})
public class ClientConnectionMixin {
   @Inject(
      method = {"handlePacket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static <T extends PacketListener> void handlePacketPre(Packet<T> packet, PacketListener listener, CallbackInfo info) {
      EventPacket packetEvent = new EventPacket(packet, EventPacket.Type.RECEIVE);
      EventManager.call(packetEvent);
      if (packetEvent.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sendPre(Packet<?> packet, CallbackInfo info) {
      EventPacket packetEvent = new EventPacket(packet, EventPacket.Type.SEND);
      EventManager.call(packetEvent);
      if (packetEvent.isCancelled()) {
         info.cancel();
      }

      if (packet instanceof CloseHandledScreenC2SPacket closePacket) {
         MinecraftClient mc = MinecraftClient.getInstance();
         ScreenCloseEvent screenCloseEvent = new ScreenCloseEvent(mc.currentScreen, closePacket.getSyncId());
         EventManager.call(screenCloseEvent);
         if (screenCloseEvent.isCancelled()) {
            info.cancel();
         }
      }
   }
}