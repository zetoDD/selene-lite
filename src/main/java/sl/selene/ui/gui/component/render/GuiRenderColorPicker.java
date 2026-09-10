package sl.selene.ui.gui.component.render;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public class GuiRenderColorPicker extends GuiScreen {
   public static void renderColorPickerWindow(
      Renderer2D renderer2D, HueSetting hueSetting, int mouseX, int mouseY, int outlineColor, int bgColor, int textColor, float alpha
   ) {
      if (hueSetting != null) {
         if (GuiScreen.colorPickerX != 0.0F || GuiScreen.colorPickerY != 0.0F) {
            float pickerWidth = 73.64F;
            float pickerHeight = 69.68F;
            renderColorPicker(
               renderer2D,
               hueSetting,
               GuiScreen.colorPickerX,
               GuiScreen.colorPickerY,
               pickerWidth,
               pickerHeight,
               mouseX,
               mouseY,
               outlineColor,
               bgColor,
               textColor,
               alpha
            );
         }
      }
   }

   private static void renderColorPicker(
      Renderer2D renderer2D,
      HueSetting setting,
      float x,
      float y,
      float width,
      float height,
      int mouseX,
      int mouseY,
      int outlineColor,
      int bgColor,
      int textColor,
      float alpha
   ) {
      renderer2D.rectOutline(x + (30.0F - 30.0F * GuiScreen.animation15.getOutput()), y, width, height - 5.0F, 5.5F, outlineColor, 0.1F);
      renderer2D.rect(x + (30.0F - 30.0F * GuiScreen.animation15.getOutput()), y, width, height - 5.0F, 5.5F, bgColor);
      float paletteWidth = 63.92F;
      float paletteHeight = 47.02F;
      float paletteX = x + 5.0F + (30.0F - 30.0F * GuiScreen.animation15.getOutput());
      float paletteY = y + 5.0F;
      float hue = setting.current / 106.0F;
      renderSaturationBrightnessPalette(renderer2D, paletteX, paletteY, paletteWidth, paletteHeight, hue);
      float cursorX = paletteX + setting.saturation * paletteWidth;
      float cursorY = paletteY + (1.0F - setting.brightness) * paletteHeight;
      renderer2D.rect(cursorX - 2.5F, cursorY - 2.5F, 5.0F, 5.0F, 3.0F, new Color(255, 255, 255, 255).getRGB());
      float hueSliderWidth = 64.0F;
      float hueSliderHeight = 2.59F;
      float hueSliderX = x + 5.0F + (30.0F - 30.0F * GuiScreen.animation15.getOutput());
      float hueSliderY = paletteY + paletteHeight + 5.0F;
      renderer2D.rectOutline(hueSliderX, hueSliderY, hueSliderWidth, hueSliderHeight, 2.0F, outlineColor, 0.5F);
      renderHueSlider(renderer2D, hueSliderX, hueSliderY, hueSliderWidth, hueSliderHeight);
      float hueSliderPos = hueSliderX + setting.current / 106.0F * hueSliderWidth;
      renderer2D.rect(hueSliderPos - 3.0F, hueSliderY, 4.7F, 4.7F, 2.0F, new Color(255, 255, 255, 255).getRGB());
   }

   private static void renderSaturationBrightnessPalette(Renderer2D renderer2D, float x, float y, float width, float height, float hue) {
      Color baseColor = Color.getHSBColor(hue, 1.0F, 1.0F);
      renderer2D.horizontalGradient(x, y, width, height, new Color(255, 255, 255).getRGB(), baseColor.getRGB());
      renderer2D.verticalGradient(x, y, width, height, new Color(0, 0, 0, 0).getRGB(), new Color(0, 0, 0, 255).getRGB());
   }

   private static void renderHueSlider(Renderer2D renderer2D, float x, float y, float width, float height) {
      int segments = 6;
      float segmentWidth = width / segments;
      Color[] hueColors = new Color[]{
         Color.getHSBColor(0.0F, 1.0F, 1.0F),
         Color.getHSBColor(0.16666667F, 1.0F, 1.0F),
         Color.getHSBColor(0.33333334F, 1.0F, 1.0F),
         Color.getHSBColor(0.5F, 1.0F, 1.0F),
         Color.getHSBColor(0.6666667F, 1.0F, 1.0F),
         Color.getHSBColor(0.8333333F, 1.0F, 1.0F),
         Color.getHSBColor(1.0F, 1.0F, 1.0F)
      };

      for (int i = 0; i < segments; i++) {
         float segmentX = x + i * segmentWidth;
         renderer2D.horizontalGradient(
            segmentX,
            y + 1.0F,
            segmentWidth,
            height,
            ColorUtil.multAlpha(hueColors[i].getRGB(), GuiScreen.animation15.getOutput()),
            ColorUtil.multAlpha(hueColors[i + 1].getRGB(), GuiScreen.animation15.getOutput())
         );
      }
   }
}