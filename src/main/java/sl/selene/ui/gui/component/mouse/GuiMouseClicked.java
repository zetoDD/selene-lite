package sl.selene.ui.gui.component.mouse;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.mouse.category.GuiMouseClickedCategory;
import sl.selene.ui.gui.component.mouse.colorpicker.GuiMouseClickedColorPicker;
import sl.selene.ui.gui.component.mouse.module.GuiMouseClickedModule;
import sl.selene.ui.gui.map.GuiServerMapPanel;
import sl.selene.ui.gui.component.render.GuiRenderConfigPopup;
import sl.selene.ui.gui.component.render.GuiRenderMain;
import sl.selene.ui.gui.component.render.GuiRenderSettings;
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
         float groupStart = (GuiScreen.SIDEBAR_WIDTH + GuiScreen.width) * 0.5F - 100.0F;
         float islandY = GuiScreen.y + 21.0F;
         float islandH = 20.0F;
         float pillW = 62.0F;
         float pillH = 19.0F;
         float pillX = GuiScreen.x + groupStart + 100.0F - pillW * 0.5F;
         float pillY = GuiScreen.y + 0.5F;

         if (pButton == 0 && GuiRenderMain.isHovered(contentMouseX, contentMouseY, pillX, pillY, pillW, pillH)) {
            GuiScreen.configPopupOpen = !GuiScreen.configPopupOpen;
            GuiScreen.activeSearch = false;
            GuiScreen.searchText = "";
            if (GuiScreen.configPopupOpen) {
               GuiScreen.configInputActive = true;
            } else {
               GuiScreen.configInputActive = false;
               GuiScreen.configDeleteArmed = false;
            }
            return true;
         }

         if (GuiScreen.configPopupOpen && pButton == 0
               && GuiRenderConfigPopup.beginDrag(contentMouseX, contentMouseY)) {
            return true;
         }

         if (GuiScreen.configPopupOpen) {
            if (GuiRenderConfigPopup.isMouseOver(contentMouseX, contentMouseY)) {
               if (GuiRenderConfigPopup.mouseClicked(contentMouseX, contentMouseY, pButton)) {
                  return true;
               }
            } else {
               GuiScreen.configPopupOpen = false;
               GuiScreen.configInputActive = false;
               GuiScreen.configDeleteArmed = false;
            }
         }

         float searchX = GuiScreen.x + groupStart + 30.0F;
         if (!GuiScreen.settingsPageOpen && pButton == 0 && GuiRenderMain.isHovered(contentMouseX, contentMouseY, searchX, islandY, 140.0F, islandH)) {
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

         float settingsButtonX = GuiScreen.x + groupStart;
         if (pButton == 0 && GuiRenderMain.isHovered(contentMouseX, contentMouseY, settingsButtonX, islandY, 24.0F, islandH)) {
            GuiScreen.settingsPageOpen = !GuiScreen.settingsPageOpen;
            GuiScreen.activeSearch = false;
            GuiScreen.searchText = "";
            if (!GuiScreen.settingsPageOpen) {
               GuiScreen.configInputActive = false;
            }
            return true;
         }

         float closeButtonX = GuiScreen.x + groupStart + 176.0F;
         if (pButton == 0 && GuiRenderMain.isHovered(contentMouseX, contentMouseY, closeButtonX, islandY, 24.0F, islandH)) {
            GuiScreen.alphaPC.run(0.0, 0.4F, sl.selene.util.render.math.animation.anim.util.Easings.CIRC_OUT);
            GuiScreen.exit = true;
            return true;
         }

         if (GuiScreen.settingsPageOpen) {
            boolean consumed = GuiRenderSettings.mouseClicked(renderer2D, contentMouseX, contentMouseY, pButton);
            if (!consumed && pButton == 0 && insideWindow(contentMouseX, contentMouseY)) {
               startWindowDrag(contentMouseX, contentMouseY);
            }
            return true;
         }

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