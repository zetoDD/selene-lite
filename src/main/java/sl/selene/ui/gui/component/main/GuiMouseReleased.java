package sl.selene.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;
import sl.selene.ui.gui.GuiScreen;

@Environment(EnvType.CLIENT)
public class GuiMouseReleased extends GuiScreen {
   public static void mouseReleased() {
      if ((GuiScreen.activeSliderSetting != null || GuiScreen.pickingSaturationBrightness || GuiScreen.pickingHue) && Selene.get.configManager != null) {
         Selene.get.configManager.autoSave();
      }

      GuiScreen.pickingSaturationBrightness = false;
      GuiScreen.pickingHue = false;
      GuiScreen.pickingAlpha = false;
      GuiScreen.windowDragging = false;
      GuiScreen.activeSliderSetting = null;
      GuiScreen.sliderX = 0.0F;
      GuiScreen.sliderY = 0.0F;
      GuiScreen.sliderWidth = 0.0F;

      GuiScreen.setSliderOverflow(0, 0.0F);
      GuiScreen.getScrollUtil().stopScrollbarDrag();
   }
}