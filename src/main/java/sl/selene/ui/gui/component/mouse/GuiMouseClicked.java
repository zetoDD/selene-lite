package sl.selene.ui.gui.component.mouse;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.mouse.category.GuiMouseClickedCategory;
import sl.selene.ui.gui.component.mouse.colorpicker.GuiMouseClickedColorPicker;
import sl.selene.ui.gui.component.mouse.module.GuiMouseClickedModule;
import sl.selene.ui.gui.map.GuiServerMapPanel;
import sl.selene.ui.gui.component.render.GuiRenderConfigPanel;
import sl.selene.ui.gui.component.render.GuiRenderMain;
import sl.selene.ui.gui.component.render.GuiRenderSettings;
import sl.selene.ui.gui.component.render.GuiRenderUpPanel;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.ScaleHelper;
import sl.selene.util.render.math.ScaledResolution;

@Environment(EnvType.CLIENT)
public class GuiMouseClicked extends GuiScreen {
   public static boolean mouseClicked(Renderer2D renderer2D, double pMouseX, double pMouseY, int pButton) {
      int mouseX = (int)ScaleHelper.calcFromScaled((float)pMouseX, (float)pMouseY)[0];
      int mouseY = (int)ScaleHelper.calcFromScaled((float)pMouseX, (float)pMouseY)[1];

      int contentMouseX = mouseX;
      int contentMouseY = mouseY;
      ScaledResolution sr = new ScaledResolution(GuiScreen.mc);
      float maxWindowX = Math.max(0.0F, sr.getWidth() - GuiScreen.width);
      float maxWindowY = Math.max(0.0F, sr.getHeight() - GuiScreen.height);
      GuiScreen.x = Math.max(0.0F, Math.min(maxWindowX, GuiScreen.x));
      GuiScreen.y = Math.max(0.0F, Math.min(maxWindowY, GuiScreen.y));
      if (!GuiScreen.exit) {
         float islandY = GuiRenderUpPanel.islandY();
         float islandH = GuiRenderUpPanel.islandHeight();

         if (!GuiScreen.serverMapOpen && pButton == 0) {
            for (int i = 0; i < GuiScreen.TAB_COUNT; i++) {
               if (GuiRenderMain.isHovered(contentMouseX, contentMouseY, GuiRenderUpPanel.tabX(i),
                     GuiRenderUpPanel.tabY(), GuiRenderUpPanel.TAB_SIZE, GuiRenderUpPanel.TAB_SIZE)) {
                  GuiScreen.selectTab(i);
                  return true;
               }
            }

            if (GuiRenderUpPanel.isOverTabBar(contentMouseX, contentMouseY)) {
               return true;
            }
         }

         if (GuiScreen.selectedTab == GuiScreen.TAB_MODULES && pButton == 0
               && GuiRenderMain.isHovered(contentMouseX, contentMouseY, GuiRenderUpPanel.searchIslandX(), islandY,
                     GuiRenderUpPanel.searchIslandWidth(), islandH)) {
            GuiScreen.activeSearch = true;
            return true;
         }

         if (GuiServerMapPanel.handleMapButtonClick(contentMouseX, contentMouseY, pButton)) {
            return true;
         }

         if (GuiScreen.serverMapOpen) {
            GuiServerMapPanel.handlePanelClick(contentMouseX, contentMouseY, pButton);
            return true;
         }

         if (pButton == 0 && GuiRenderMain.isHovered(contentMouseX, contentMouseY, GuiRenderUpPanel.closeIslandX(),
               islandY, GuiRenderUpPanel.closeIslandWidth(), islandH)) {
            GuiScreen.alphaPC.run(0.0, 0.4F, sl.selene.util.render.math.animation.anim.util.Easings.CIRC_OUT);
            GuiScreen.exit = true;
            return true;
         }

         if (GuiScreen.selectedTab == GuiScreen.TAB_SETTINGS) {
            GuiRenderSettings.mouseClicked(renderer2D, contentMouseX, contentMouseY, pButton);
            return true;
         }

         if (GuiScreen.selectedTab == GuiScreen.TAB_CONFIG) {
            if (GuiRenderConfigPanel.mouseClicked(contentMouseX, contentMouseY, pButton)) {
               return true;
            }
         } else {
            GuiMouseClickedCategory.mouseClickedCategory(contentMouseX, contentMouseY);

            if (GuiMouseClickedColorPicker.mouseClickedColorPicker(contentMouseX, contentMouseY, pButton)) {
               return true;
            }

            if (GuiScreen.getScrollUtil().handleScrollbarClick(contentMouseX, contentMouseY, pButton)) {
               return true;
            }

            if (GuiMouseClickedModule.mouseClickedModule(renderer2D, contentMouseX, contentMouseY, pButton)) {
               return true;
            }
         }

      }

      if (GuiScreen.activeBindSetting != null && pButton >= 0 && pButton <= 7) {
         int mouseKey = -100 - pButton;
         GuiScreen.activeBindSetting.key = mouseKey;
         GuiScreen.activeBindSetting.active = false;
         GuiScreen.activeBindSetting = null;
         return true;
      }

      if (pButton == 0 && insideWindow(contentMouseX, contentMouseY)) {
         startWindowDrag(contentMouseX, contentMouseY);
         return true;
      }
      return false;
   }

   private static boolean insideWindow(int mouseX, int mouseY) {
      return mouseX >= GuiScreen.x && mouseX <= GuiScreen.x + GuiScreen.width
            && mouseY >= GuiScreen.y && mouseY <= GuiScreen.y + GuiScreen.height;
   }

   private static void startWindowDrag(int mouseX, int mouseY) {
      GuiScreen.windowDragging = true;
      GuiScreen.windowDragOffsetX = mouseX - GuiScreen.x;
      GuiScreen.windowDragOffsetY = mouseY - GuiScreen.y;
   }
}