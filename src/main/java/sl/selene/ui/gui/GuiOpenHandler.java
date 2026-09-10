package sl.selene.ui.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.EventInit;
import sl.selene.event.input.KeyInputEvent;

@Environment(EnvType.CLIENT)
public class GuiOpenHandler {
   @EventInit
   public void onKeyInput(KeyInputEvent event) {
   }
}