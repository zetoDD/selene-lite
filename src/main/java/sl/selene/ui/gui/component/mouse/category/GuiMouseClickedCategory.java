package sl.selene.ui.gui.component.mouse.category;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;
import sl.selene.module.api.Category;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.render.GuiRenderMain;
import sl.selene.util.render.math.animation.Direction;

@Environment(EnvType.CLIENT)
public class GuiMouseClickedCategory extends GuiScreen {
   public static void mouseClickedCategory(int mouseX, int mouseY) {
      float x1 = GuiScreen.x;
      float y1 = GuiScreen.y;
      float downY = 0.0F;

      for (Category category : GuiScreen.categories) {
         if (GuiRenderMain.isHovered(mouseX, mouseY, x1, y1 + 82.0F + downY - 2.0F, GuiScreen.SIDEBAR_WIDTH, 21.325F) && GuiScreen.selectedCategories != category) {
            GuiScreen.animation15.setDirection(Direction.BACKWARDS);
            GuiScreen.activeColorPicker = null;
            GuiScreen.selectedCategories = category;
            GuiScreen.modules = Selene.get.manager.getType(GuiScreen.selectedCategories);
            GuiScreen.categoryAnimation.reset();
            GuiScreen.moduleAnimation.reset();
            GuiScreen.getScrollUtil().reset();
            Selene.get.guiManager.setGuiCategory(category);
         }

         downY += 22.0F;
      }
   }
}