package sl.selene.event.player;

import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class ScreenCloseEvent extends Event {
   private Screen screen;
   private int windowId;

   @Generated
   public Screen getScreen() {
      return this.screen;
   }

   @Generated
   public int getWindowId() {
      return this.windowId;
   }

   @Generated
   public ScreenCloseEvent(Screen screen, int windowId) {
      this.screen = screen;
      this.windowId = windowId;
   }
}