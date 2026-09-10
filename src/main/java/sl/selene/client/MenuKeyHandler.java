package sl.selene.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.Selene;
import sl.selene.event.EventInit;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.ui.gui.GuiClient;

@Environment(EnvType.CLIENT)
public final class MenuKeyHandler {
   private int lastBoundKey = Integer.MIN_VALUE;

   public static void register() {
      sl.selene.event.EventManager.register(new MenuKeyHandler());
   }

   @EventInit
   public void onClientTick(ClientTickEvent event) {
      if (!Selene.isModInitialized() || Selene.get == null || SeleneKeyBindings.OPEN_MENU == null) {
         return;
      }

      MinecraftClient client = event.client();
      if (client == null || client.currentScreen != null) {
         return;
      }

      int boundKey = SeleneKeyBindings.getBoundKeyCode();
      if (boundKey != this.lastBoundKey) {
         this.lastBoundKey = boundKey;
         SeleneKeyBindings.syncToMenuModule();
      }

      while (SeleneKeyBindings.OPEN_MENU.wasPressed()) {
         GuiClient gui = Selene.get.getGuiClient();
         if (gui == null) {
            return;
         }

         client.setScreen(gui);
         if (client.mouse != null) {
            client.mouse.unlockCursor();
         }
      }
   }
}