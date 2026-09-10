package sl.selene.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.ui.gui.GuiScreen;

@Environment(EnvType.CLIENT)
public class GuiClose extends GuiScreen {
   public static void closeCheck() {
      if (GuiScreen.exit && GuiScreen.alphaPC.isFinished()) {
         GuiScreen.exit = false;
      }
   }
}