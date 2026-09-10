package sl.selene.event.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public final class CursorLockEvent extends Event {
   private final MinecraftClient client;
   private final long windowHandle;

   public CursorLockEvent(MinecraftClient client, long windowHandle) {
      this.client = client;
      this.windowHandle = windowHandle;
   }

   public MinecraftClient client() {
      return this.client;
   }

   public long windowHandle() {
      return this.windowHandle;
   }
}