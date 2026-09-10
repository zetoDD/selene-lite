package sl.selene.ui.gui.component.mouse.colorpicker;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.render.GuiRenderMain;

@Environment(EnvType.CLIENT)
public class GuiMouseClickedColorPicker extends GuiScreen {
   public static boolean mouseClickedColorPicker(int mouseX, int mouseY, int pButton) {
      boolean colorPickerClickHandled = false;
      if (GuiScreen.activeColorPicker != null
         && GuiScreen.activeColorPicker instanceof HueSetting
         && (GuiScreen.colorPickerX != 0.0F || GuiScreen.colorPickerY != 0.0F)) {
         HueSetting hueSetting = GuiScreen.activeColorPicker;
         float pickerX = GuiScreen.colorPickerX;
         float pickerY = GuiScreen.colorPickerY;
         float paletteWidth = 63.92F;
         float paletteHeight = 47.02F;
         float paletteX = pickerX + 5.0F;
         float paletteY = pickerY + 5.0F;
         if (pButton == 0 && GuiRenderMain.isHovered(mouseX, mouseY, paletteX, paletteY, paletteWidth, paletteHeight)) {
            GuiScreen.pickingSaturationBrightness = true;
            float x = Math.max(0.0F, Math.min(mouseX - paletteX, paletteWidth));
            float y = Math.max(0.0F, Math.min(mouseY - paletteY, paletteHeight));
            hueSetting.saturation = x / paletteWidth;
            hueSetting.brightness = 1.0F - y / paletteHeight;
            colorPickerClickHandled = true;
         }

         if (!colorPickerClickHandled) {
            float hueSliderWidth = 64.0F;
            float hueSliderHeight = 3.59F;
            float hueSliderX = pickerX + 5.0F;
            float hueSliderY = paletteY + paletteHeight + 5.0F;
            if (pButton == 0 && GuiRenderMain.isHovered(mouseX, mouseY, hueSliderX, hueSliderY, hueSliderWidth, hueSliderHeight)) {
               GuiScreen.pickingHue = true;
               float huePos = Math.max(0.0F, Math.min(mouseX - hueSliderX, hueSliderWidth));
               hueSetting.current = huePos / hueSliderWidth * 106.0F;
               colorPickerClickHandled = true;
            }
         }
      }

      if (colorPickerClickHandled) {
         return true;
      } else {
         if (GuiScreen.activeColorPicker != null && (GuiScreen.colorPickerX != 0.0F || GuiScreen.colorPickerY != 0.0F)) {
            float pickerWidth = 73.64F;
            float pickerHeight = 64.68F;
            if (GuiRenderMain.isHovered(mouseX, mouseY, GuiScreen.colorPickerX, GuiScreen.colorPickerY, pickerWidth, pickerHeight)) {
               return true;
            }
         }

         return false;
      }
   }
}