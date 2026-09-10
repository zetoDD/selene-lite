package sl.selene.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.render.GuiRenderConfigPopup;
import sl.selene.util.render.math.ScaleHelper;
import sl.selene.util.render.math.ScaledResolution;

@Environment(EnvType.CLIENT)
public class GuiMouseDragged extends GuiScreen {
   public static boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      int mouseX = (int)ScaleHelper.calcFromScaled((float)pMouseX, (float)pMouseY)[0];
      int mouseY = (int)ScaleHelper.calcFromScaled((float)pMouseX, (float)pMouseY)[1];
      int contentMouseX = mouseX;
      int contentMouseY = mouseY;

      if (GuiScreen.configPopupDragging && pButton == 0) {
         return GuiRenderConfigPopup.drag(contentMouseX, contentMouseY);
      }

      if (GuiScreen.windowDragging && pButton == 0) {
         ScaledResolution sr = new ScaledResolution(GuiScreen.mc);
         float scaledWidth = sr.getWidth();
         float scaledHeight = sr.getHeight();
         float baseX = (scaledWidth - GuiScreen.width) / 2.0F;
         float baseY = (scaledHeight - GuiScreen.height) / 2.0F;
         GuiScreen.windowOffsetX = contentMouseX - GuiScreen.windowDragOffsetX - baseX;
         GuiScreen.windowOffsetY = contentMouseY - GuiScreen.windowDragOffsetY - baseY;
         return true;
      }

      if (GuiScreen.activeColorPicker != null && GuiScreen.activeColorPicker instanceof HueSetting) {
         HueSetting hueSetting = GuiScreen.activeColorPicker;
         float pickerX = GuiScreen.colorPickerX;
         float pickerY = GuiScreen.colorPickerY;
         if (pickerX != 0.0F || pickerY != 0.0F) {
            float paletteWidth = 63.92F;
            float paletteHeight = 47.02F;
            float paletteX = pickerX + 5.0F;
            float paletteY = pickerY + 5.0F;
            if (GuiScreen.pickingSaturationBrightness) {
               float x = Math.max(0.0F, Math.min(contentMouseX - paletteX, paletteWidth));
               float y = Math.max(0.0F, Math.min(contentMouseY - paletteY, paletteHeight));
               hueSetting.saturation = x / paletteWidth;
               hueSetting.brightness = 1.0F - y / paletteHeight;
               if (Selene.get.configManager != null) {
                  Selene.get.configManager.autoSave();
               }

               return true;
            }

            if (GuiScreen.pickingHue) {
               float hueSliderWidth = 64.0F;
               float hueSliderHeight = 2.59F;
               float hueSliderX = pickerX + 5.0F;
               float hueSliderY = paletteY + paletteHeight + 5.0F;
               float huePos = Math.max(0.0F, Math.min(contentMouseX - hueSliderX, hueSliderWidth));
               hueSetting.current = huePos / hueSliderWidth * 106.0F;
               if (Selene.get.configManager != null) {
                  Selene.get.configManager.autoSave();
               }

               return true;
            }
         }
      }

      if (GuiScreen.getScrollUtil().handleScrollbarDrag(contentMouseY)) {
         return true;
      }

      if (GuiScreen.activeSliderSetting != null && GuiScreen.sliderWidth > 0.0F) {
         SliderSetting sliderSetting = GuiScreen.activeSliderSetting;
         float progress = (contentMouseX - GuiScreen.sliderX) / GuiScreen.sliderWidth;
         progress = Math.max(0.0F, Math.min(1.0F, progress));
         sliderSetting.current = sliderSetting.minimum + (sliderSetting.maximum - sliderSetting.minimum) * progress;
         if (Selene.get.configManager != null) {
            Selene.get.configManager.autoSave();
         }

         float left = GuiScreen.sliderX;
         float right = GuiScreen.sliderX + GuiScreen.sliderWidth;
         if (contentMouseX < left) {
            float raw = left - contentMouseX;
            GuiScreen.setSliderOverflow(-1, GuiScreen.decaySliderOverflow(raw, GuiScreen.MAX_SLIDER_OVERFLOW));
         } else if (contentMouseX > right) {
            float raw = contentMouseX - right;
            GuiScreen.setSliderOverflow(1, GuiScreen.decaySliderOverflow(raw, GuiScreen.MAX_SLIDER_OVERFLOW));
         } else {
            GuiScreen.setSliderOverflow(0, 0.0F);
         }

         return true;
      } else {
         return false;
      }
   }
}