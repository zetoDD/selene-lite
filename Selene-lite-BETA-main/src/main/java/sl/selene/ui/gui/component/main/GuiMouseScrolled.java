package sl.selene.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.render.GuiRenderMain;
import sl.selene.util.render.math.ScaleHelper;

@Environment(EnvType.CLIENT)
public class GuiMouseScrolled extends GuiScreen {
   public static boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      float[] mouseCoords = ScaleHelper.calcFromScaled((float)pMouseX, (float)pMouseY);
      float mouseX = mouseCoords[0];
      float mouseY = mouseCoords[1];
      float x1 = GuiScreen.x;
      float y1 = GuiScreen.y;
      float rectWidth = GuiScreen.width;
      float rectHeight = GuiScreen.height;
      if (!GuiScreen.exit && GuiRenderMain.isHovered(mouseX, mouseY, x1, y1, rectWidth, rectHeight)) {
         GuiScreen.getScrollUtil().setEnabled(true);

         double delta = Math.abs(pScrollY) > Math.abs(pScrollX) ? pScrollY : pScrollX;
         if (Math.abs(delta) < 1.0E-4) {
            return false;
         } else {
            GuiScreen.getScrollUtil().handleScroll(delta);
            return true;
         }
      } else {
         return false;
      }
   }
}