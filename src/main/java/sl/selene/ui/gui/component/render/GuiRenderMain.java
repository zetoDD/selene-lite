package sl.selene.ui.gui.component.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.setting.GuiRenderSetting;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.keyboard.Keyboard;
import sl.selene.util.player.MovementManager;
import sl.selene.util.render.animation.util.Easings;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.animation.Direction;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.utils.KeyUtil;

@Environment(EnvType.CLIENT)
public class GuiRenderMain extends GuiScreen {

   private record Palette(int outline, int main, int main6, int main40, int text) {
   }

   public static void renderMain(Renderer2D renderer2D, MatrixStack pose, int mouseX, int mouseY, float mainAlpha) {
      if (mainAlpha <= 0.001F) {
         return;
      }

      boolean searchActive = GuiScreen.activeSearch;
      MovementManager movementManager = MovementManager.getInstance();
      if (searchActive) {
         movementManager.lockMovement("Search");
      } else {
         movementManager.unlockMovement("Search");
      }

      if (searchActive) {
         boolean backspaceDown = KeyUtil.isKeyDown(259);
         long currentTime = System.currentTimeMillis();
         if (backspaceDown) {
            if (!GuiScreen.backspaceHeld) {
               GuiScreen.backspaceHeld = true;
               GuiScreen.firstBackspacePressTime = currentTime;
               GuiScreen.lastBackspaceTime = currentTime;
               if (GuiScreen.searchSelectAll) {
                  GuiScreen.searchText = "";
                  GuiScreen.searchSelectAll = false;
               } else if (!GuiScreen.searchText.isEmpty()) {
                  GuiScreen.searchText = GuiScreen.searchText.substring(0, GuiScreen.searchText.length() - 1);
               }
            } else if (currentTime - GuiScreen.firstBackspacePressTime > 500L && currentTime - GuiScreen.lastBackspaceTime > 30L) {
               if (GuiScreen.searchSelectAll) {
                  GuiScreen.searchText = "";
                  GuiScreen.searchSelectAll = false;
               } else if (!GuiScreen.searchText.isEmpty()) {
                  GuiScreen.searchText = GuiScreen.searchText.substring(0, GuiScreen.searchText.length() - 1);
               }

               GuiScreen.lastBackspaceTime = currentTime;
            }
         } else {
            GuiScreen.backspaceHeld = false;
            GuiScreen.firstBackspacePressTime = 0L;
         }
      } else {
         GuiScreen.backspaceHeld = false;
         GuiScreen.firstBackspacePressTime = 0L;
      }

      Palette palette = new Palette(
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha)),
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha)),
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(15.3F * mainAlpha)),
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha)),
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha)));
      int backGroundOneColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(178.5F * mainAlpha));
      float x1 = GuiScreen.x + GuiScreen.SIDEBAR_WIDTH;
      float y1 = GuiScreen.y + GuiScreen.CONTENT_TOP;
      float rectWidth = GuiScreen.width - GuiScreen.SIDEBAR_WIDTH;
      float rectHeight = GuiScreen.height - GuiScreen.CONTENT_TOP;
      float clipX = x1 + 5.0F;
      float clipY = y1 + 5.0F;
      float clipWidth = rectWidth - 10.0F;
      float clipHeight = rectHeight - 10.0F;
      renderer2D.pushRoundedClipRect(clipX, clipY, clipWidth, clipHeight, 0.0F, 0.0F, 0.0F, 0.0F);

      List<Module> filteredModules = GuiScreen.modules;
      if (searchActive && !GuiScreen.searchText.isEmpty()) {
         String searchLower = GuiScreen.searchText.toLowerCase().trim();
         ArrayList<Module> result = new ArrayList<>();
         for (Module module : GuiScreen.modules) {
            if (module.name.toLowerCase().contains(searchLower)) {
               result.add(module);
            }
         }

         filteredModules = result;
      }

      Map<Module, List<Setting>> settingsCache = new HashMap<>(Math.max(16, filteredModules.size()));
      Map<Module, Float> settingsHeightCache = new HashMap<>(Math.max(16, filteredModules.size()));
      for (Module module : filteredModules) {
         settingsCache.put(module, module.getSettingsForGUI());
      }

      float calcDownY = 0.0F;
      float calcDownYSetting1 = 0.0F;
      float calcDownYSetting2 = 0.0F;
      float maxHeightColumn1 = 0.0F;
      float maxHeightColumn2 = 0.0F;
      int calcIndex = 1;

      for (Module module : filteredModules) {
         module.animation.update();
         module.animation.run(module.enable ? 1.0 : 0.0, 0.15F, Easings.SINE_OUT);
         module.animation1.setDirection(module.enable ? Direction.FORWARDS : Direction.BACKWARDS);
         float settingsAnim = GuiScreen.getModuleSettingsAnimation(module).get();
         float settingsAlphaAnim = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
         boolean expanded = GuiScreen.openSettingsModules.contains(module) || settingsAnim > 0.0F || settingsAlphaAnim > 0.0F;
         float settingsHeight = GuiRenderSetting.getAnimatedSettingsHeight(renderer2D, settingsCache.get(module), settingsAnim, expanded);
         settingsHeightCache.put(module, settingsHeight);
         float cardHeight = MODULE_CARD_HEIGHT + (expanded ? settingsHeight : 0.0F);

         if (calcIndex % 2 == 0) {
            float currentDownY = calcDownY + calcDownYSetting2 - MODULE_COLUMN_STAGGER;
            maxHeightColumn2 = Math.max(maxHeightColumn2, currentDownY + cardHeight);
            if (expanded) {
               calcDownYSetting2 += settingsHeight;
            }
         } else {
            float currentDownY = calcDownY + calcDownYSetting1;
            maxHeightColumn1 = Math.max(maxHeightColumn1, currentDownY + cardHeight);
            if (expanded) {
               calcDownYSetting1 += settingsHeight;
            }

            calcDownY += MODULE_ROW_PITCH;
         }

         calcIndex++;
      }

      float totalHeight = Math.max(maxHeightColumn1, maxHeightColumn2);
      float contentHeight = totalHeight + 8.0F;
      boolean check = isHovered(mouseX, mouseY, GuiScreen.x, GuiScreen.y, GuiScreen.width, GuiScreen.height);
      GuiScreen.getScrollUtil().setSpeed(6.0F);
      GuiScreen.getScrollUtil().setEnabled(check);
      GuiScreen.getScrollUtil().update();
      GuiScreen.getScrollUtil().setMax(contentHeight, rectHeight - 10.0F);
      int index = 1;
      float downY = GuiScreen.getScrollUtil().getScroll();
      float[] columnSettingsHeights = { 0.0F, 0.0F };

      for (Module module : filteredModules) {
         int column = index % 2 == 0 ? 1 : 0;
         float columnX = column == 1 ? MODULE_COLUMN_2_X : MODULE_COLUMN_1_X;
         float stagger = column == 1 ? MODULE_COLUMN_STAGGER : 0.0F;
         float cardX = GuiScreen.x + columnX;
         float cardY = GuiScreen.y + MODULE_CARD_TOP + downY + columnSettingsHeights[column] - stagger;
         float settingsAnim = GuiScreen.getModuleSettingsAnimation(module).get();
         float settingsAlphaAnim = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
         boolean settingsVisible = settingsAnim > 0.0F || settingsAlphaAnim > 0.0F;
         float settingsHeight = settingsHeightCache.getOrDefault(module, SETTINGS_COLLAPSED_HEIGHT);
         float enableProgress = module.animation.get();

         renderModuleCard(renderer2D, module, settingsCache.get(module), cardX, cardY, mouseX, mouseY, mainAlpha,
               palette, enableProgress, settingsAnim, settingsAlphaAnim, settingsHeight, settingsVisible);

         if (settingsVisible) {
            columnSettingsHeights[column] += settingsHeight;
         }

         if (column == 0) {
            downY += MODULE_ROW_PITCH;
         }

         index++;
      }

      renderer2D.popClipRect();
      GuiScreen.getScrollUtil().render(
         renderer2D,
         GuiScreen.x + GuiScreen.width - 4.0F,
         GuiScreen.y + GuiScreen.CONTENT_TOP + 5.0F,
         4.0F,
         GuiScreen.height - GuiScreen.CONTENT_TOP - 13.5F,
         mainAlpha
      );
      if (GuiScreen.activeColorPicker != null && GuiScreen.activeColorPicker instanceof HueSetting) {
         GuiRenderColorPicker.renderColorPickerWindow(
            renderer2D,
            GuiScreen.activeColorPicker,
            mouseX,
            mouseY,
            ColorUtil.multAlpha(palette.outline(), GuiScreen.animation15.getOutput()),
            ColorUtil.multAlpha(backGroundOneColor, GuiScreen.animation15.getOutput()),
            ColorUtil.multAlpha(palette.main40(), GuiScreen.animation15.getOutput()),
            mainAlpha * GuiScreen.animation15.getOutput()
         );
      }
   }

   private static void renderModuleCard(
         Renderer2D renderer2D,
         Module module,
         List<Setting> moduleSettings,
         float cardX,
         float cardY,
         int mouseX,
         int mouseY,
         float mainAlpha,
         Palette palette,
         float enableProgress,
         float settingsAnim,
         float settingsAlphaAnim,
         float settingsHeight,
         boolean settingsVisible
   ) {
      float drawnHeight = settingsVisible ? MODULE_CARD_HEIGHT + settingsHeight : MODULE_CARD_HEIGHT;

      renderer2D.shadow(cardX, cardY + 1.5F, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT, 6.5F, 5.0F, 0.4F,
            Renderer2D.ColorUtil.rgba(0, 0, 0, (int)(32.0F * mainAlpha)));
      GlassStyle.card(renderer2D, cardX, cardY, MODULE_CARD_WIDTH, drawnHeight, mainAlpha);
      if (settingsVisible && settingsAlphaAnim > 0.01F) {
         renderer2D.rect(cardX, cardY + MODULE_CARD_HEIGHT, MODULE_CARD_WIDTH, 1.0F,
               ColorUtil.multAlpha(palette.outline(), settingsAlphaAnim));
      }

      if (enableProgress > 0.01F) {
         renderer2D.rect(cardX, cardY, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT, 6.5F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(7.0F * enableProgress * mainAlpha)));
      }

      float cardHover = GuiScreen.easeHover(
            GuiScreen.cardHoverAnimations.getOrDefault(module, 0.0F),
            isHovered(mouseX, mouseY, cardX, cardY, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT)
      );
      GuiScreen.cardHoverAnimations.put(module, cardHover);
      if (cardHover > 0.01F) {
         renderer2D.rect(cardX, cardY, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT, 6.5F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(10.0F * cardHover * mainAlpha)));
      }

      float nameX = cardX + MODULE_NAME_OFFSET_X;
      float nameY = cardY + MODULE_NAME_OFFSET_Y;
      renderer2D.text(FontRegistry.INTER_SEMIBOLD, nameX, nameY + MODULE_NAME_TEXT_OFFSET_Y, 14.0F, module.name, palette.text());
      renderBindPill(renderer2D, module, nameX, nameY, mainAlpha, palette);

      GlassStyle.toggle(renderer2D, cardX + MODULE_SWITCH_OFFSET_X, cardY + MODULE_SWITCH_OFFSET_Y, 26.0F, 13.0F, mainAlpha, enableProgress);

      if (settingsVisible) {
         float settingX = cardX + MODULE_SETTING_INSET_X;
         float settingY = cardY + MODULE_CARD_HEIGHT + MODULE_SETTING_GAP;
         float totalSettingsHeight = 0.0F;

         for (Setting setting : moduleSettings) {
            totalSettingsHeight += GuiRenderSetting.renderSetting(
                  renderer2D,
                  setting,
                  settingX,
                  settingY + totalSettingsHeight,
                  SETTING_WIDTH,
                  mouseX,
                  mouseY,
                  ColorUtil.multAlpha(palette.outline(), settingsAlphaAnim),
                  ColorUtil.multAlpha(palette.main(), settingsAlphaAnim),
                  ColorUtil.multAlpha(palette.main6(), settingsAlphaAnim),
                  ColorUtil.multAlpha(palette.main40(), settingsAlphaAnim),
                  ColorUtil.multAlpha(palette.text(), settingsAlphaAnim),
                  mainAlpha * settingsAlphaAnim
               )
               * settingsAlphaAnim;
         }
      }
   }

   private static void renderBindPill(Renderer2D renderer2D, Module module, float nameX, float nameY, float mainAlpha, Palette palette) {
      float bindAnim = GuiScreen.getModuleBindAnimation(module).get();
      float bindWobble = GuiScreen.getBindWobble(module);
      float bindWobbleBoost = GuiScreen.getBindWobbleBoost(module);
      float pillAlpha = Math.max(Math.max(bindAnim, module.binding ? 1.0F : 0.35F), bindWobbleBoost);
      float keyTextAlpha = module.binding ? 1.0F : Math.max(Math.max(bindAnim, 1.0F), bindWobbleBoost);
      float bindHeight = 10.0F;
      String keyText = module.binding ? "..." : (module.bind != -1 ? Keyboard.keyName(module.bind) : "None");
      float keyTextWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, keyText, 12.0F).width;
      float buttonWidth = Math.max(6.0F, keyTextWidth + 6.0F);
      float moduleNameWidth = renderer2D.measureText(FontRegistry.INTER_SEMIBOLD, module.name, 14.0F).width;
      float bindX = nameX + moduleNameWidth + 6.0F + bindWobble;
      float bindY = nameY - 0.35F;
      renderer2D.rectOutline(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(palette.outline(), pillAlpha), 0.1F);
      renderer2D.rect(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(palette.main6(), pillAlpha));
      renderer2D.text(
         FontRegistry.INTER_MEDIUM,
         bindX + buttonWidth / 2.0F - keyTextWidth / 2.0F - 0.2F,
         bindY + 2.0F + 5.25F,
         12.0F,
         keyText,
         ColorUtil.multAlpha(palette.main(), keyTextAlpha)
      );
      if (bindWobbleBoost > 0.01F) {
         renderer2D.rectOutline(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(palette.text(), bindWobbleBoost), 0.18F);
         renderer2D.rect(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(palette.text(), bindWobbleBoost * 0.22F));
      }
   }

   public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
   }
}

