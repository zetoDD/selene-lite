package sl.selene.ui.gui.component.mouse.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BindSettings;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.MultiBooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.module.api.setting.impl.StringSetting;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.mouse.module.GuiMouseClickedModule;
import sl.selene.ui.gui.component.render.GuiRenderMain;
import sl.selene.ui.gui.component.setting.GuiRenderSetting;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.animation.Direction;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.utils.KeyUtil;

@Environment(EnvType.CLIENT)
public class GuiMouseClickedSetting extends GuiScreen {
   public static boolean handleSettingClick(Renderer2D renderer2D, Setting setting, float x, float y, float width, int mouseX, int mouseY, int button) {
      if (setting instanceof BooleanSetting boolSetting) {
         float checkBoxSize = 8.0F;
         float checkBoxX = x + width - checkBoxSize - 3.0F;
         float checkBoxY = y + 2.0F;
         if (button == 0 && GuiRenderMain.isHovered(mouseX, mouseY, checkBoxX, checkBoxY, checkBoxSize, checkBoxSize)) {
            boolSetting.set(!boolSetting.get());
            if (Selene.get.configManager != null) {
               Selene.get.configManager.autoSave();
            }

            return true;
         }
      }

      if (setting instanceof BindSettings bindSetting) {
         float bindHeight = 10.075F;
         String keyText = bindSetting.active ? "..." : KeyUtil.getKey(bindSetting.key);
         float keyTextWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, keyText, 12.0F).width;
         float minButtonWidth = 16.055F;
         float buttonWidth = Math.max(minButtonWidth, keyTextWidth + 8.0F);
         float bindButtonX = x + width - buttonWidth - 2.0F;
         if (bindButtonX < x) {
            bindButtonX = x;
            buttonWidth = width - 2.0F;
         }

         float backgroundX = bindButtonX - 6.0F;
         float backgroundWidth = buttonWidth + 2.0F;
         if (backgroundX < x) {
            backgroundWidth = backgroundX + backgroundWidth - x;
            backgroundX = x;
         }

         if (GuiRenderMain.isHovered(mouseX, mouseY, backgroundX, y, backgroundWidth, bindHeight)) {
            if (button == 0) {
               if (GuiScreen.activeBindSetting != bindSetting) {
                  if (GuiScreen.activeBindSetting != null) {
                     GuiScreen.activeBindSetting.active = false;
                  }

                  GuiScreen.activeBindSetting = bindSetting;
                  bindSetting.active = true;
               }

               return true;
            }

            if (GuiScreen.activeBindSetting == bindSetting && button >= 0 && button <= 7) {
               int mouseKey = -100 - button;
               bindSetting.key = mouseKey;
               bindSetting.active = false;
               GuiScreen.activeBindSetting = null;
               if (Selene.get.configManager != null) {
                  Selene.get.configManager.autoSave();
               }

               return true;
            }
         }
      }

      if (setting instanceof HueSetting hueSetting) {
         float colorWidth = 40.0F;
         float colorX = x + width - colorWidth - 2.0F;
         float buttonX = colorX - 10.0F;
         float buttonWidthx = 46.48F;
         float buttonHeight = 10.075F;
         if (button == 0 && GuiRenderMain.isHovered(mouseX, mouseY, buttonX, y, buttonWidthx, buttonHeight)) {
            if (GuiScreen.activeColorPicker == hueSetting) {
               GuiScreen.animation15.setDirection(Direction.BACKWARDS);
               GuiScreen.activeColorPicker = null;
               GuiScreen.colorPickerX = 0.0F;
               GuiScreen.colorPickerY = 0.0F;
            } else {
               GuiScreen.activeColorPicker = hueSetting;
               GuiScreen.animation15.setDirection(Direction.FORWARDS);
               float[] pickerPos = GuiMouseClickedModule.findColorPickerPosition(renderer2D, hueSetting);
               if (pickerPos != null) {
                  GuiScreen.colorPickerX = pickerPos[0];
                  GuiScreen.colorPickerY = pickerPos[1];
               }
            }

            return true;
         }

         if (GuiScreen.activeColorPicker == hueSetting) {
            float pickerX = GuiScreen.colorPickerX;
            float pickerY = GuiScreen.colorPickerY;
            if (pickerX != 0.0F || pickerY != 0.0F) {
               float paletteWidth = 63.92F;
               float paletteHeight = 47.02F;
               float paletteX = pickerX + 5.0F;
               float paletteY = pickerY + 5.0F;
               if (GuiRenderMain.isHovered(mouseX, mouseY, paletteX, paletteY, paletteWidth, paletteHeight)) {
                  GuiScreen.pickingSaturationBrightness = true;
                  float xPos = Math.max(0.0F, Math.min(mouseX - paletteX, paletteWidth));
                  float yPos = Math.max(0.0F, Math.min(mouseY - paletteY, paletteHeight));
                  hueSetting.saturation = xPos / paletteWidth;
                  hueSetting.brightness = 1.0F - yPos / paletteHeight;
                  if (Selene.get.configManager != null) {
                     Selene.get.configManager.autoSave();
                  }

                  return true;
               }

               float hueSliderWidth = 64.0F;
               float hueSliderHeight = 2.59F;
               float hueSliderX = pickerX + 5.0F;
               float hueSliderY = paletteY + paletteHeight + 5.0F;
               if (GuiRenderMain.isHovered(mouseX, mouseY, hueSliderX, hueSliderY, hueSliderWidth, hueSliderHeight)) {
                  GuiScreen.pickingHue = true;
                  float huePos = Math.max(0.0F, Math.min(mouseX - hueSliderX, hueSliderWidth));
                  hueSetting.current = huePos / hueSliderWidth * 106.0F;
                  if (Selene.get.configManager != null) {
                     Selene.get.configManager.autoSave();
                  }

                  return true;
               }
            }
         }
      }

      if (setting instanceof SliderSetting sliderSetting) {
         float sliderHeight = 4.0F;
         float sliderY = y + 10.0F;
         float sliderWidth = width - 2.5F;
         float sliderActualY = sliderY + 2.0F;

         float sliderHitY = y;
         float sliderHitHeight = Math.max(19.0F, GuiRenderSetting.getSettingHeight(renderer2D, sliderSetting));
         if (button == 0 && GuiRenderMain.isHovered(mouseX, mouseY, x, sliderHitY, sliderWidth, sliderHitHeight)) {
            GuiScreen.activeSliderSetting = sliderSetting;
            GuiScreen.sliderX = x;
            GuiScreen.sliderY = sliderActualY;
            GuiScreen.sliderWidth = Math.max(1.0F, sliderWidth);
            float progress = (mouseX - x) / sliderWidth;
            progress = Math.max(0.0F, Math.min(1.0F, progress));
            sliderSetting.current = sliderSetting.minimum + (sliderSetting.maximum - sliderSetting.minimum) * progress;
            if (Selene.get.configManager != null) {
               Selene.get.configManager.autoSave();
            }

            return true;
         }
      }

      if (setting instanceof ModeSetting modeSetting) {
         float modeSpacing = 2.0F;
         float modeHeight = 10.075F;
         float padding = 3.0F;
         float verticalSpacing = -2.0F;
         float calcX = padding;
         float calcY = 0.0F;

         for (String mode : modeSetting.modes) {
            float modeWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, mode, 12.0F).width + padding * 2.0F;
            if (calcX + modeWidth > width && calcX > padding) {
               calcX = padding;
               calcY += modeHeight + verticalSpacing;
            }

            calcX += modeWidth + modeSpacing;
         }

         float modeContainerY = y + 10.0F;
         float modeContainerHeight = calcY + modeHeight;
         if (button == 0 && GuiRenderMain.isHovered(mouseX, mouseY, x, modeContainerY, width, modeContainerHeight)) {
            float currentX = padding;
            float currentY = 1.5F;

            for (String mode : modeSetting.modes) {
               float modeWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, mode, 12.0F).width + padding * 2.0F;
               if (currentX + modeWidth > width && currentX > padding) {
                  currentX = padding;
                  currentY += modeHeight + verticalSpacing;
               }

               if (GuiRenderMain.isHovered(mouseX, mouseY, x + currentX, modeContainerY + currentY, modeWidth, modeHeight)) {
                  modeSetting.currentMode = mode;
                  modeSetting.index = modeSetting.modes.indexOf(mode);
                  if (Selene.get.configManager != null) {
                     Selene.get.configManager.autoSave();
                  }

                  return true;
               }

               currentX += modeWidth + modeSpacing;
            }
         }
      }

      if (setting instanceof StringSetting stringSetting) {
         float textFieldHeight = 10.075F;
         float textFieldWidth = 63.56F;
         float textFieldX = x + 42.0F;
         if (button == 0 && GuiRenderMain.isHovered(mouseX, mouseY, textFieldX, y, textFieldWidth, textFieldHeight)) {
            if (GuiScreen.activeStringSetting != stringSetting) {
               if (GuiScreen.activeStringSetting != null) {
                  GuiScreen.activeStringSetting.active = false;
               }

               GuiScreen.activeStringSetting = stringSetting;
               stringSetting.active = true;
            }

            return true;
         }

         if (button == 0 && GuiScreen.activeStringSetting == stringSetting) {
            GuiScreen.activeStringSetting.active = false;
            GuiScreen.activeStringSetting = null;
            if (Selene.get.configManager != null) {
               Selene.get.configManager.autoSave();
            }
         }
      }

      if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
         float startY = y + 10.0F;
         float currentX = x;
         float currentY = startY;
         float spacing = 3.0F;
         float boolHeight = 10.0F;
         float padding = 4.0F;

         for (BooleanSetting boolSettingx : multiBooleanSetting.settings) {
            float nameWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, boolSettingx.name, 12.0F).width;
            float boolWidth = nameWidth + padding * 2.0F;
            if (currentX + boolWidth > x + width) {
               currentX = x;
               currentY += boolHeight + spacing;
            }

            if (button == 0 && GuiRenderMain.isHovered(mouseX, mouseY, currentX, currentY, boolWidth, boolHeight)) {
               boolSettingx.set(!boolSettingx.get());
               if (Selene.get.configManager != null) {
                  Selene.get.configManager.autoSave();
               }

               return true;
            }

            currentX += boolWidth + spacing;
         }
      }

      return false;
   }
}