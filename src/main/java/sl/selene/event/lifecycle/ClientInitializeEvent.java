package sl.selene.event.lifecycle;

import java.time.Instant;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public final class ClientInitializeEvent extends Event {
   private final MinecraftClient client;
   private final Instant timestamp;

   public ClientInitializeEvent(MinecraftClient client) {
      this(client, Instant.now());
   }

   public ClientInitializeEvent(MinecraftClient client, Instant timestamp) {
      this.client = client;
      this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
   }

   public MinecraftClient client() {
      return this.client;
   }

   public Instant timestamp() {
      return this.timestamp;
   }
}