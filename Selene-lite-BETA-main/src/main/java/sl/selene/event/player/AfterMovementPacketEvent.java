package sl.selene.event.player;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public final class AfterMovementPacketEvent extends Event {
   private final MinecraftClient client;
   private final ClientPlayerEntity player;
   private final ClientWorld world;

   public AfterMovementPacketEvent(MinecraftClient client, ClientPlayerEntity player, ClientWorld world) {
      this.client = Objects.requireNonNull(client, "client");
      this.player = Objects.requireNonNull(player, "player");
      this.world = Objects.requireNonNull(world, "world");
   }

   public MinecraftClient client() {
      return this.client;
   }

   public ClientPlayerEntity player() {
      return this.player;
   }

   public ClientWorld world() {
      return this.world;
   }
}