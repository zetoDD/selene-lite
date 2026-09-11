package sl.selene.ui.gui.component.setting;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BindSettings;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.MultiBooleanSetting;
import sl.selene.module.api.setting.impl.NoneSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.module.api.setting.impl.StringSetting;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.render.GlassStyle;
import sl.selene.ui.gui.component.render.GuiRenderMain;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.animation.util.Animation;
import sl.selene.util.render.animation.util.Easings;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.MathHelper;
import sl.selene.util.render.math.animation.anim.util.Animation2;
import sl.selene.util.render.motion.Motion;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.utils.KeyUtil;

@Environment(EnvType.CLIENT)
public class GuiRenderSetting {
   public static Animation multiAnim = new Animation();
   public static HashMap<ModeSetting, HashMap<String, Float>> animation = new HashMap<>();
   public static HashMap<String, Float> animation2 = new HashMap<>();
   public static HashMap<MultiBooleanSetting, HashMap<String, Float>> multiBooleanAnimation = new HashMap<>();

   private static final float MODE_CHIP_HEIGHT = 10.075F;
   private static final float MODE_CHIP_SPACING = 2.0F;
   private static final float MODE_CHIP_PADDING = 3.0F;
   private static final float MODE_CHIP_VERTICAL_SPACING = -2.0F;
   private static final float MULTI_BOOL_TOP = 10.0F;
   private static final float MULTI_BOOL_CHIP_HEIGHT = 10.0F;
   private static final float MULTI_BOOL_SPACING = 3.0F;
   private static final float MULTI_BOOL_PADDING = 4.0F;

   public static float getAnimatedSettingsHeight(Renderer2D renderer2D, List<Setting> moduleSettings, float settingsAnim, boolean expanded) {
      if (!expanded) {
         return GuiScreen.SETTINGS_COLLAPSED_HEIGHT;
      }

      float fullSettingsHeight = GuiScreen.SETTINGS_COLLAPSED_HEIGHT;
      for (Setting setting : moduleSettings) {
         fullSettingsHeight += getSettingHeight(renderer2D, setting) + 1.0F;
      }

      fullSettingsHeight = Math.max(fullSettingsHeight, 20.0F);
      return GuiScreen.SETTINGS_COLLAPSED_HEIGHT
         + (fullSettingsHeight - GuiScreen.SETTINGS_COLLAPSED_HEIGHT) * settingsAnim;
   }

   private static float measureModeChipRows(Renderer2D renderer2D, ModeSetting modeSetting) {
      float calcX = MODE_CHIP_PADDING;
      float calcY = 0.0F;
      for (String mode : modeSetting.modes) {
         float modeWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, mode, 12.0F).width + MODE_CHIP_PADDING * 2.0F;
         if (calcX + modeWidth > GuiScreen.SETTING_WIDTH && calcX > MODE_CHIP_PADDING) {
            calcX = MODE_CHIP_PADDING;
            calcY += MODE_CHIP_HEIGHT + MODE_CHIP_VERTICAL_SPACING;
         }

         calcX += modeWidth + MODE_CHIP_SPACING;
      }

      return calcY;
   }

   private static float measureMultiBooleanRows(Renderer2D renderer2D, MultiBooleanSetting multiBooleanSetting) {
      float currentX = 0.0F;
      float rowsBelow = 0.0F;
      for (BooleanSetting boolSetting : multiBooleanSetting.settings) {
         float nameWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, boolSetting.name, 12.0F).width;
         float boolWidth = nameWidth + MULTI_BOOL_PADDING * 2.0F;
         if (currentX + boolWidth > GuiScreen.SETTING_WIDTH) {
            currentX = 0.0F;
            rowsBelow += MULTI_BOOL_CHIP_HEIGHT + MULTI_BOOL_SPACING;
         }

         currentX += boolWidth + MULTI_BOOL_SPACING;
      }

      return rowsBelow;
   }

   public static float getSettingHeight(Renderer2D renderer2D, Setting setting) {
      if (setting instanceof NoneSetting) {
         return ((NoneSetting)setting).get();
      } else if (setting instanceof BooleanSetting) {
         return 10.0F;
      } else if (setting instanceof SliderSetting) {
         return 19.0F;
      } else if (setting instanceof ModeSetting modeSetting) {
         return measureModeChipRows(renderer2D, modeSetting) + MODE_CHIP_HEIGHT + 12.0F;
      } else if (setting instanceof BindSettings) {
         return 13.0F;
      } else if (setting instanceof StringSetting) {
         return 15.0F;
      } else if (setting instanceof HueSetting) {
         return 15.0F;
      } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
         return MULTI_BOOL_TOP + measureMultiBooleanRows(renderer2D, multiBooleanSetting) + MULTI_BOOL_CHIP_HEIGHT;
      } else {
         return 15.0F;
      }
   }

   public static float renderSetting(
      Renderer2D renderer2D,
      Setting setting,
      float x,
      float y,
      float width,
      int mouseX,
      int mouseY,
      int outlineColor,
      int mainColor,
      int mainColor6,
      int mainColor40,
      int textColor,
      float mainAlpha
   ) {
      float height = 0.0F;
      if (setting instanceof NoneSetting) {
         height = ((NoneSetting)setting).get();
      } else if (setting instanceof BooleanSetting boolSetting) {
         boolean value = boolSetting.get();
         float checkBoxSize = 8.0F;
         float checkBoxX = x + width - checkBoxSize - 3.0F;
         float checkBoxY = y + 2.0F;
         boolSetting.anim.update();
         boolSetting.anim.run(value ? 1.0 : 0.0, 0.15F, Easings.SINE_OUT);
         GlassStyle.outline(renderer2D, checkBoxX, checkBoxY, checkBoxSize, checkBoxSize, 3.0F, mainAlpha);
         renderer2D.rect(checkBoxX, checkBoxY, checkBoxSize, checkBoxSize, 3.0F, mainColor6);
         renderer2D.rect(checkBoxX + 2.3F, checkBoxY + 2.2F, 3.42F, 3.425F, 3.0F, ColorUtil.overCol(0, mainColor, boolSetting.anim.get()));
         renderer2D.text(FontRegistry.INTER_MEDIUM, x, y + 3.0F + 5.0F, 13.0F, setting.name, mainColor40);
         height = 10.0F;
      } else if (setting instanceof SliderSetting sliderSetting) {
         float sliderHeight = 4.0F;
         float sliderY = y + 10.0F;
         float sliderWidth = width - 2.5F;
         Animation2 sliderAnim = GuiScreen.getSliderAnimation(sliderSetting);
         float targetProgress = (sliderSetting.current - sliderSetting.minimum) / (sliderSetting.maximum - sliderSetting.minimum);
         double toValue = sliderAnim.getToValue();
         sliderAnim.update();
         sliderAnim.run(targetProgress, 0.24F, sl.selene.util.render.math.animation.anim.util.Easings.QUART_OUT);
         float progress = (float)sliderAnim.getValue();

         float trackLeft = x;
         float trackRight = x + sliderWidth;
         boolean hovered = GuiScreen.activeSliderSetting == sliderSetting
            || GuiRenderMain.isHovered(mouseX, mouseY, trackLeft, sliderY, sliderWidth, Math.max(12.0F, 19.0F));
         float hover = GuiScreen.sliderHoverAnimations.getOrDefault(sliderSetting, 0.0F);
         hover = Motion.smooth(hover, hovered ? 1.0F : 0.0F, Motion.HOVER_SPEED);
         GuiScreen.sliderHoverAnimations.put(sliderSetting, hover);

         float overflow = GuiScreen.sliderOverflow;
         boolean active = GuiScreen.activeSliderSetting == sliderSetting;
         float baseH = sliderHeight + 2.0F * hover;
         float overflowPct = active ? Math.min(1.0F, overflow / GuiScreen.MAX_SLIDER_OVERFLOW) : 0.0F;
         float scaleX = 1.0F + overflowPct * 0.9F;
         float scaleY = 1.0F - overflowPct * 0.2F;

         float originX = GuiScreen.sliderOverflowSide > 0 ? trackLeft : (GuiScreen.sliderOverflowSide < 0 ? trackRight : trackLeft + sliderWidth * 0.5F);
         float trackCenterY = sliderY + 2.0F + baseH * 0.5F;

         renderer2D.pushScale(scaleX, scaleY, originX, trackCenterY);
         try {
            GlassStyle.outline(renderer2D, trackLeft, sliderY + 2.0F, sliderWidth, baseH, baseH * 0.5F, mainAlpha);
            GlassStyle.sliderTrack(renderer2D, trackLeft, sliderY + 2.0F, sliderWidth, baseH, mainColor6, mainColor, progress);
            GlassStyle.sliderKnob(renderer2D, GlassStyle.sliderKnobX(trackLeft, sliderWidth, progress), sliderY + 2.0F, baseH, textColor);
         } finally {
            renderer2D.popTransform();
         }

         String valueText = sliderSetting.percent
            ? String.format("%.1f%%", sliderSetting.current)
            : String.format("%.1f / %.1f", sliderSetting.current, sliderSetting.maximum);
         renderer2D.text(FontRegistry.INTER_MEDIUM, x, y + 1.0F + 7.0F, 13.0F, setting.name, mainColor40);
         renderer2D.text(
            FontRegistry.INTER_MEDIUM,
            x + sliderWidth - renderer2D.measureText(FontRegistry.INTER_MEDIUM, valueText, 13.0F).width - 2.0F,
            y + 7.0F,
            13.0F,
            valueText,
            mainColor
         );
         height = 19.0F;
      } else if (setting instanceof ModeSetting modeSetting) {
         HashMap<String, Float> modeAnims = animation.computeIfAbsent(modeSetting, k -> new HashMap<>());
         for (String s : modeSetting.modes) {
            modeAnims.putIfAbsent(s, 0.0F);
         }

         renderer2D.text(FontRegistry.INTER_MEDIUM, x, y + 7.0F, 13.0F, setting.name, mainColor40);
         float modeContainerY = y + 10.0F;
         float modeContainerHeight = measureModeChipRows(renderer2D, modeSetting) + MODE_CHIP_HEIGHT;
         GlassStyle.outline(renderer2D, x, modeContainerY, width, modeContainerHeight, 3.0F, mainAlpha);
         renderer2D.rect(x, modeContainerY, width, modeContainerHeight, 3.0F, mainColor6);
         float currentX = MODE_CHIP_PADDING;
         float currentY = 1.5F;

         for (String mode : modeSetting.modes) {
            boolean isSelected = mode.equals(modeSetting.currentMode);
            float modeWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, mode, 12.0F).width + MODE_CHIP_PADDING * 2.0F;
            if (currentX + modeWidth > width && currentX > MODE_CHIP_PADDING) {
               currentX = MODE_CHIP_PADDING;
               currentY += MODE_CHIP_HEIGHT + MODE_CHIP_VERTICAL_SPACING;
            }

            float anim = modeAnims.getOrDefault(mode, 0.0F);
            float target = isSelected ? 1.0F : 0.0F;
            anim = Motion.smooth(anim, target, Motion.CHIP_SPEED);
            modeAnims.put(mode, anim);
            if (anim > 0.01F) {
               int chipBg = ColorUtil.overCol(mainColor6, mainColor, anim);
               chipBg = Renderer2D.ColorUtil.replAlpha(chipBg, (int)(12.0F + 18.0F * anim));
               renderer2D.rect(x + currentX, modeContainerY + currentY, modeWidth, MODE_CHIP_HEIGHT, 3.0F, chipBg);
            }
            int modeColor = ColorUtil.overCol(mainColor40, mainColor, anim);
            renderer2D.text(FontRegistry.INTER_MEDIUM, x + currentX, modeContainerY + currentY + 5.5F, 12.0F, mode, modeColor);
            currentX += modeWidth + MODE_CHIP_SPACING;
         }

         height = modeContainerHeight + 12.0F;
      } else if (setting instanceof BindSettings bindSetting) {
         float bindHeight = 10.075F;
         String displayName = setting.name != null && !setting.name.isEmpty() ? setting.name : "KEY";
         String keyText = bindSetting.active ? "..." : KeyUtil.getKey(bindSetting.key);
         float keyTextWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, keyText, 12.0F).width;
         float minButtonWidth = 16.055F;
         float buttonWidth = Math.max(minButtonWidth, keyTextWidth + 8.0F);
         float bindButtonX = x + width - buttonWidth - 2.0F;
         if (bindButtonX < x) {
            bindButtonX = x;
            buttonWidth = width - 2.0F;
         }

         renderer2D.text(FontRegistry.INTER_MEDIUM, x, y + 1.0F + 6.8F, 13.0F, displayName, mainColor40);
         float backgroundX = bindButtonX - 6.0F;
         float backgroundWidth = buttonWidth + 2.0F;
         if (backgroundX < x) {
            backgroundWidth = backgroundX + backgroundWidth - x;
            backgroundX = x;
         }

         GlassStyle.outline(renderer2D, backgroundX, y, backgroundWidth, bindHeight, 3.0F, mainAlpha);
         renderer2D.rect(backgroundX, y, backgroundWidth, bindHeight, 3.0F, mainColor6);
         renderer2D.text(
            FontRegistry.INTER_MEDIUM,
            backgroundX + backgroundWidth / 2.0F - keyTextWidth / 2.0F,
            y + 1.5F + 5.7F,
            12.0F,
            keyText,
            bindSetting.active ? mainColor : mainColor40
         );
         height = 13.0F;
      } else if (setting instanceof StringSetting stringSetting) {
         float textFieldHeight = 10.075F;
         float textFieldWidth = 63.56F;
         float textFieldX = x + 42.0F;
         float textX = textFieldX + 5.0F;
         float textY = y + 1.5F;
         renderer2D.text(FontRegistry.INTER_MEDIUM, x, y + 1.0F + 6.5F, 13.0F, setting.name, mainColor40);
         GlassStyle.outline(renderer2D, textFieldX, y, textFieldWidth, textFieldHeight, 3.0F, mainAlpha);
         renderer2D.rect(textFieldX, y, textFieldWidth, textFieldHeight, 3.0F, mainColor6);
         String inputText = stringSetting.input;
         boolean isEmpty = inputText.isEmpty();
         float cursorX = textX;
         if (isEmpty) {
            renderer2D.text(FontRegistry.INTER_MEDIUM, textX - 2.0F, textY - 0.5F + 6.1F, 12.0F, "Enter text", mainColor40);
         } else {
            float currentX = textX;
            float maxX = textFieldX + textFieldWidth - 5.0F;
            float gradientStartX = textX;
            float gradientEndX = textFieldX + textFieldWidth - 5.0F;

            for (int i = 0; i < inputText.length(); i++) {
               char c = inputText.charAt(i);
               String charStr = String.valueOf(c);
               float charWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, charStr, 12.0F).width;
               if (currentX + charWidth > maxX) {
                  cursorX = currentX;
                  break;
               }

               textColor = mainColor40;
               if (i >= 16) {
                  float fadeStartX = gradientStartX + renderer2D.measureText(FontRegistry.INTER_MEDIUM, inputText.substring(0, 16), 12.0F).width;
                  float gradientWidth = Math.min(30.0F, gradientEndX - fadeStartX);
                  if (gradientWidth > 0.0F) {
                     float fadeProgress = (currentX - fadeStartX) / gradientWidth;
                     fadeProgress = MathHelper.clamp(fadeProgress, 0.0F, 1.0F);
                     int alpha = mainColor40 >> 24 & 0xFF;
                     alpha = (int)(alpha * (1.0F - fadeProgress));
                     textColor = Renderer2D.ColorUtil.replAlpha(mainColor40, alpha);
                  } else {
                     textColor = Renderer2D.ColorUtil.replAlpha(mainColor40, 0);
                  }
               }

               renderer2D.text(FontRegistry.INTER_MEDIUM, currentX - 2.0F, textY - 0.5F + 6.1F, 12.0F, charStr, textColor);
               currentX += charWidth;
               cursorX = currentX;
            }
         }

         boolean isActive = GuiScreen.activeStringSetting == stringSetting && stringSetting.active;
         if (isActive) {
            float cursorAlpha = Motion.blink(Motion.CURSOR_BLINK_HALF_PERIOD_MS);
            if (cursorAlpha > 0.01F) {
               renderer2D.rect(cursorX - 3.0F, textY - 0.5F, 1.0F, 8.0F, 0.5F, ColorUtil.multAlpha(mainColor, cursorAlpha));
            }
         }

         height = 15.0F;
      } else if (setting instanceof HueSetting hueSetting) {
         float colorHeight = 12.0F;
         float colorWidth = 40.0F;
         float colorX = x + width - colorWidth - 2.0F;
         renderer2D.text(FontRegistry.INTER_MEDIUM, x, y + 1.0F + 7.0F, 13.0F, setting.name, mainColor40);
         Color hueColor = hueSetting.getColor();
         GlassStyle.outline(renderer2D, colorX - 10.0F, y, 46.48F, 10.075F, 3.0F, mainAlpha);
         renderer2D.rect(colorX - 10.0F, y, 46.48F, 10.075F, 3.0F, mainColor6);
         renderer2D.rect(
            colorX + 32.0F - 10.0F,
            y + 0.8F,
            13.285F,
            8.315F,
            0.0F,
            3.0F,
            3.0F,
            0.0F,
            Renderer2D.ColorUtil.replAlpha(hueColor.getRGB(), (int)(255.0F * mainAlpha))
         );
         String hexText = String.format("#%02X%02X%02X", hueColor.getRed(), hueColor.getGreen(), hueColor.getBlue());
         renderer2D.text(
            FontRegistry.INTER_MEDIUM,
            colorX + colorWidth / 2.0F - renderer2D.measureText(FontRegistry.INTER_MEDIUM, hexText, 12.0F).width / 2.0F - 14.0F,
            y + 1.5F + 5.7F,
            12.0F,
            hexText,
            mainColor40
         );
         height = 15.0F;
      } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
         renderer2D.text(FontRegistry.INTER_MEDIUM, x, y + 7.0F, 13.0F, setting.name, mainColor40);
         float currentX = x;
         float currentY = y + MULTI_BOOL_TOP;
         HashMap<String, Float> boolAnims = multiBooleanAnimation.computeIfAbsent(multiBooleanSetting, k -> new HashMap<>());

         for (BooleanSetting boolSetting : multiBooleanSetting.settings) {
            float nameWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, boolSetting.name, 12.0F).width;
            float boolWidth = nameWidth + MULTI_BOOL_PADDING * 2.0F;
            if (currentX + boolWidth > x + width) {
               currentX = x;
               currentY += MULTI_BOOL_CHIP_HEIGHT + MULTI_BOOL_SPACING;
            }

            GlassStyle.outline(renderer2D, currentX, currentY, boolWidth, MULTI_BOOL_CHIP_HEIGHT, 3.0F, mainAlpha);
            boolAnims.putIfAbsent(boolSetting.name, boolSetting.get() ? 1.0F : 0.0F);
            float anim = boolAnims.get(boolSetting.name);
            float target = boolSetting.get() ? 1.0F : 0.0F;
            anim = Motion.smooth(anim, target, Motion.CHIP_SPEED);
            boolAnims.put(boolSetting.name, anim);
            int chipBg = ColorUtil.overCol(mainColor6, mainColor, anim);
            chipBg = Renderer2D.ColorUtil.replAlpha(chipBg, (int)(12.0F + 18.0F * anim));
            renderer2D.rect(currentX, currentY, boolWidth, MULTI_BOOL_CHIP_HEIGHT, 3.0F, chipBg);
            textColor = ColorUtil.overCol(mainColor40, mainColor, anim);
            renderer2D.text(FontRegistry.INTER_MEDIUM, currentX + MULTI_BOOL_PADDING, currentY + 3.0F - 1.0F + 5.0F, 12.0F, boolSetting.name, textColor);
            currentX += boolWidth + MULTI_BOOL_SPACING;
         }

         height = MULTI_BOOL_TOP + measureMultiBooleanRows(renderer2D, multiBooleanSetting) + MULTI_BOOL_CHIP_HEIGHT;
      }

      return height + 1.0F;
   }
}