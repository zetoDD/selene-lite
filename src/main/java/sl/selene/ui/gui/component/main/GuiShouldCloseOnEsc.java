package sl.selene.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.render.math.animation.anim.util.Easings;

@Environment(EnvType.CLIENT)
public class GuiShouldCloseOnEsc extends GuiScreen {
   public static boolean shouldCloseOnEsc() {
      if (!GuiScreen.exit && GuiScreen.alphaPC.getValue() > 0.0) {
         GuiScreen.alphaPC.run(0.0, 0.4F, Easings.CIRC_OUT);
         GuiScreen.exit = true;
      }

      return false;
   }
}