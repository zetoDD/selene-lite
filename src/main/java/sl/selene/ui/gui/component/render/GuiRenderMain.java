package sl.selene.ui.gui.component.render;

import java.awt.Color;
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

      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int backGroundThreeColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(10.2F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int mainColor6 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(15.3F * mainAlpha));
      int mainColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int backGroundOneColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(178.5F * mainAlpha));
      Color mainColorGlow35 = Renderer2D.ColorUtil.getColor(Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(56.0F * mainAlpha)));
      boolean vanillaStyle = GuiScreen.isVanillaStyle();
      if (vanillaStyle) {
         outlineColor = Renderer2D.ColorUtil.replAlpha(new Color(86, 86, 86).getRGB(), (int)(190.0F * mainAlpha));
         backGroundThreeColor = Renderer2D.ColorUtil.replAlpha(new Color(57, 57, 57).getRGB(), (int)(220.0F * mainAlpha));
         mainColor = Renderer2D.ColorUtil.replAlpha(new Color(170, 170, 170).getRGB(), (int)(255.0F * mainAlpha));
         mainColor6 = Renderer2D.ColorUtil.replAlpha(new Color(95, 95, 95).getRGB(), (int)(230.0F * mainAlpha));
         mainColor40 = Renderer2D.ColorUtil.replAlpha(new Color(138, 138, 138).getRGB(), (int)(255.0F * mainAlpha));
         textColor = Renderer2D.ColorUtil.replAlpha(new Color(230, 230, 230).getRGB(), (int)(255.0F * mainAlpha));
         backGroundOneColor = Renderer2D.ColorUtil.replAlpha(new Color(44, 44, 44).getRGB(), (int)(225.0F * mainAlpha));
         mainColorGlow35 = new Color(0, 0, 0, 0);
      }
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
         float animPC = module.animation.get();
         float settingsHeight = 12.0F;
         float fullSettingsHeight = 12.0F;
         float settingsAnim = GuiScreen.getModuleSettingsAnimation(module).get();
         float settingsAlphaAnim = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
         if (GuiScreen.openSettingsModules.contains(module) || settingsAnim > 0.0F || settingsAlphaAnim > 0.0F) {
            List<Setting> moduleSettings = settingsCache.get(module);
            for (Setting setting : moduleSettings) {
               fullSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting) + 1.0F;
            }

            fullSettingsHeight = Math.max(fullSettingsHeight, 20.0F);
            settingsHeight = 12.0F + (fullSettingsHeight - 12.0F) * settingsAnim;
         }
         settingsHeightCache.put(module, settingsHeight);

         if (calcIndex % 2 == 0) {
            float currentDownY = calcDownY + calcDownYSetting2 - 30.0F;
            float moduleHeight = 21.325F;
            moduleHeight += settingsHeight;
            float totalY = currentDownY + moduleHeight;
            maxHeightColumn2 = Math.max(maxHeightColumn2, totalY);
            calcDownYSetting2 += settingsHeight;
         } else {
            float currentDownY = calcDownY + calcDownYSetting1;
            float moduleHeight = 21.325F;
            moduleHeight += settingsHeight;
            float totalY = currentDownY + moduleHeight;
            maxHeightColumn1 = Math.max(maxHeightColumn1, totalY);
            calcDownYSetting1 += settingsHeight;
            calcDownY += 30.325F;
         }

         calcIndex++;
      }

      float totalHeight = Math.max(maxHeightColumn1, maxHeightColumn2);
      float contentHeight = totalHeight + 8.0F;
      float x2 = GuiScreen.x + GuiScreen.SIDEBAR_WIDTH;
      float y2 = GuiScreen.y + GuiScreen.CONTENT_TOP;
      boolean check = isHovered(mouseX, mouseY, GuiScreen.x, GuiScreen.y, GuiScreen.width, GuiScreen.height);
      GuiScreen.getScrollUtil().setSpeed(6.0F);
      GuiScreen.getScrollUtil().setEnabled(check);
      GuiScreen.getScrollUtil().update();
      GuiScreen.getScrollUtil().setMax(contentHeight, rectHeight - 10.0F);
      float yShar = -0.35F;
      float yZnar = -0.7F;
      int index = 1;
      float downY = GuiScreen.getScrollUtil().getScroll();
      float downYSetting1 = 0.0F;
      float downYSetting2 = 0.0F;

      for (Module module : filteredModules) {
         if (index % 2 == 0) {
            float animPCx = module.animation.get();
            float currentDownY = downY + downYSetting2 - 30.0F;
            float settingsAnimx = GuiScreen.getModuleSettingsAnimation(module).get();
            float settingsAlphaAnimx = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
            float settingsHeightx = settingsHeightCache.getOrDefault(module, 12.0F);
            List<Setting> moduleSettings = settingsCache.get(module);

            renderer2D.shadow(GuiScreen.x + 266.35F, GuiScreen.y + 55.365F + currentDownY + 1.5F, 150.0F, 21.325F, 6.5F, 5.0F, 0.4F,
                  Renderer2D.ColorUtil.rgba(0, 0, 0, (int)(32.0F * mainAlpha)));
            if (!(settingsAnimx > 0.0F) && !(settingsAlphaAnimx > 0.0F)) {
               GlassStyle.card(renderer2D, GuiScreen.x + 266.35F, GuiScreen.y + 55.365F + currentDownY, 150.0F, 21.325F, mainAlpha);
            } else {
               GlassStyle.card(renderer2D, GuiScreen.x + 266.35F, GuiScreen.y + 55.365F + currentDownY, 150.0F, 21.325F + settingsHeightx, mainAlpha);
               if (settingsAlphaAnimx > 0.01F) {
                  renderer2D.rect(
                     GuiScreen.x + 266.515F, GuiScreen.y + 76.69F + currentDownY, 150.0F, 1.0F, ColorUtil.multAlpha(outlineColor, settingsAlphaAnimx)
                  );
               }
            }
            if (animPCx > 0.01F) {
               renderer2D.rect(GuiScreen.x + 266.35F, GuiScreen.y + 55.365F + currentDownY, 150.0F, 21.325F, 6.5F,
                     Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(7.0F * animPCx * mainAlpha)));
            }
            float cardHoverX = GuiScreen.easeHover(
                  GuiScreen.cardHoverAnimations.getOrDefault(module, 0.0F),
                  GuiRenderMain.isHovered(mouseX, mouseY, GuiScreen.x + 266.35F, GuiScreen.y + 55.365F + currentDownY, 150.0F, 21.325F)
            );
            GuiScreen.cardHoverAnimations.put(module, cardHoverX);
            if (cardHoverX > 0.01F) {
               renderer2D.rect(GuiScreen.x + 266.35F, GuiScreen.y + 55.365F + currentDownY, 150.0F, 21.325F, 6.5F,
                     Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(10.0F * cardHoverX * mainAlpha)));
            }
            float moduleNameX = GuiScreen.x + 280.35F;
            float moduleNameY = GuiScreen.y + 61.555F + currentDownY;
            renderer2D.text(FontRegistry.INTER_SEMIBOLD, moduleNameX, moduleNameY + 6.6F, 14.0F, module.name, textColor);

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
            float bindX = moduleNameX + moduleNameWidth + 6.0F + bindWobble;
            float bindY = moduleNameY - 0.35F;
            renderer2D.rectOutline(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(outlineColor, pillAlpha), 0.1F);
            renderer2D.rect(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(mainColor6, pillAlpha));
            renderer2D.text(
               FontRegistry.INTER_MEDIUM,
               bindX + buttonWidth / 2.0F - keyTextWidth / 2.0F - 0.2F,
               bindY + 2.0F + 5.25F,
               12.0F,
               keyText,
               ColorUtil.multAlpha(mainColor, keyTextAlpha)
            );
            if (bindWobbleBoost > 0.01F) {
               renderer2D.rectOutline(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(textColor, bindWobbleBoost), 0.18F);
               renderer2D.rect(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(textColor, bindWobbleBoost * 0.22F));
            }

            float switchW = 26.0F;
            float switchH = 13.0F;
            float switchX = GuiScreen.x + 382.35F;
            float switchY = GuiScreen.y + 59.525F + currentDownY;
            GlassStyle.toggle(renderer2D, switchX, switchY, switchW, switchH, mainAlpha, animPCx);
            if (settingsAnimx > 0.0F || settingsAlphaAnimx > 0.0F) {
               float settingY = GuiScreen.y + 76.69F + currentDownY + 4.0F;
               float settingX = GuiScreen.x + 275.35F;
               float settingWidth = 132.47F;
               float totalSettingsHeight = 0.0F;

               for (Setting setting : moduleSettings) {
                  totalSettingsHeight += GuiRenderSetting.renderSetting(
                        renderer2D,
                        setting,
                        settingX,
                        settingY + totalSettingsHeight,
                        settingWidth,
                        mouseX,
                        mouseY,
                        ColorUtil.multAlpha(outlineColor, settingsAlphaAnimx),
                        ColorUtil.multAlpha(mainColor, settingsAlphaAnimx),
                        ColorUtil.multAlpha(mainColor6, settingsAlphaAnimx),
                        ColorUtil.multAlpha(mainColor40, settingsAlphaAnimx),
                        ColorUtil.multAlpha(textColor, settingsAlphaAnimx),
                        mainAlpha * settingsAlphaAnimx
                     )
                     * settingsAlphaAnimx;
               }

               downYSetting2 += settingsHeightx;
            }
         } else {
            float animPCxx = module.animation.get();
            float currentDownYx = downY + downYSetting1;
            float settingsAnimxx = GuiScreen.getModuleSettingsAnimation(module).get();
            float settingsAlphaAnimxx = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
            float settingsHeightxx = settingsHeightCache.getOrDefault(module, 12.0F);
            List<Setting> moduleSettings = settingsCache.get(module);

            renderer2D.shadow(GuiScreen.x + 111.885F, GuiScreen.y + 55.365F + currentDownYx + 1.5F, 150.0F, 21.325F, 6.5F, 5.0F, 0.4F,
                  Renderer2D.ColorUtil.rgba(0, 0, 0, (int)(32.0F * mainAlpha)));
            if (!(settingsAnimxx > 0.0F) && !(settingsAlphaAnimxx > 0.0F)) {
               GlassStyle.card(renderer2D, GuiScreen.x + 111.885F, GuiScreen.y + 55.365F + currentDownYx, 150.0F, 21.325F, mainAlpha);
            } else {
               GlassStyle.card(renderer2D, GuiScreen.x + 111.885F, GuiScreen.y + 55.365F + currentDownYx, 150.0F, 21.325F + settingsHeightxx, mainAlpha);
               if (settingsAlphaAnimxx > 0.01F) {
                  renderer2D.rect(
                     GuiScreen.x + 111.885F, GuiScreen.y + 76.69F + currentDownYx, 150.0F, 1.0F, ColorUtil.multAlpha(outlineColor, settingsAlphaAnimxx)
                  );
               }
            }
            if (animPCxx > 0.01F) {
               renderer2D.rect(GuiScreen.x + 111.885F, GuiScreen.y + 55.365F + currentDownYx, 150.0F, 21.325F, 6.5F,
                     Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(7.0F * animPCxx * mainAlpha)));
            }
            float cardHoverXx = GuiScreen.easeHover(
                  GuiScreen.cardHoverAnimations.getOrDefault(module, 0.0F),
                  GuiRenderMain.isHovered(mouseX, mouseY, GuiScreen.x + 111.885F, GuiScreen.y + 55.365F + currentDownYx, 150.0F, 21.325F)
            );
            GuiScreen.cardHoverAnimations.put(module, cardHoverXx);
            if (cardHoverXx > 0.01F) {
               renderer2D.rect(GuiScreen.x + 111.885F, GuiScreen.y + 55.365F + currentDownYx, 150.0F, 21.325F, 6.5F,
                     Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(10.0F * cardHoverXx * mainAlpha)));
            }
            float moduleNameXx = GuiScreen.x + 125.885F;
            float moduleNameYx = GuiScreen.y + 61.555F + currentDownYx;
            renderer2D.text(
               FontRegistry.INTER_SEMIBOLD, moduleNameXx, moduleNameYx + 6.6F, 14.0F, module.name, textColor
            );

            float bindAnimx = GuiScreen.getModuleBindAnimation(module).get();
            float bindWobble = GuiScreen.getBindWobble(module);
            float bindWobbleBoost = GuiScreen.getBindWobbleBoost(module);
            float pillAlphax = Math.max(Math.max(bindAnimx, module.binding ? 1.0F : 0.35F), bindWobbleBoost);
            float keyTextAlphax = module.binding ? 1.0F : Math.max(Math.max(bindAnimx, 1.0F), bindWobbleBoost);
            float bindHeight = 10.0F;
            String keyText = module.binding ? "..." : (module.bind != -1 ? Keyboard.keyName(module.bind) : "None");
            float keyTextWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, keyText, 12.0F).width;
            float buttonWidth = Math.max(6.0F, keyTextWidth + 6.0F);
            float moduleNameWidth = renderer2D.measureText(FontRegistry.INTER_SEMIBOLD, module.name, 14.0F).width;
            float bindX = moduleNameXx + moduleNameWidth + 6.0F + bindWobble;
            float bindY = moduleNameYx - 0.35F;
            renderer2D.rectOutline(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(outlineColor, pillAlphax), 0.1F);
            renderer2D.rect(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(mainColor6, pillAlphax));
            renderer2D.text(
               FontRegistry.INTER_MEDIUM,
               bindX + buttonWidth / 2.0F - keyTextWidth / 2.0F - 0.2F,
               bindY + 2.0F + 5.25F,
               12.0F,
               keyText,
               ColorUtil.multAlpha(mainColor, keyTextAlphax)
            );
            if (bindWobbleBoost > 0.01F) {
               renderer2D.rectOutline(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(textColor, bindWobbleBoost), 0.18F);
               renderer2D.rect(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(textColor, bindWobbleBoost * 0.22F));
            }

            float switchW = 26.0F;
            float switchH = 13.0F;
            float switchX = GuiScreen.x + 227.885F;
            float switchY = GuiScreen.y + 59.525F + currentDownYx;
            GlassStyle.toggle(renderer2D, switchX, switchY, switchW, switchH, mainAlpha, animPCxx);
            if (settingsAnimxx > 0.0F || settingsAlphaAnimxx > 0.0F) {
               float settingY = GuiScreen.y + 76.69F + currentDownYx + 4.0F;
               float settingX = GuiScreen.x + 111.885F + 9.0F;
               float settingWidth = 132.47F;
               float totalSettingsHeight = 0.0F;

               for (Setting setting : moduleSettings) {
                  totalSettingsHeight += GuiRenderSetting.renderSetting(
                        renderer2D,
                        setting,
                        settingX,
                        settingY + totalSettingsHeight,
                        settingWidth,
                        mouseX,
                        mouseY,
                        ColorUtil.multAlpha(outlineColor, settingsAlphaAnimxx),
                        ColorUtil.multAlpha(mainColor, settingsAlphaAnimxx),
                        ColorUtil.multAlpha(mainColor6, settingsAlphaAnimxx),
                        ColorUtil.multAlpha(mainColor40, settingsAlphaAnimxx),
                        ColorUtil.multAlpha(textColor, settingsAlphaAnimxx),
                        mainAlpha * settingsAlphaAnimxx
                     )
                     * settingsAlphaAnimxx;
               }

               downYSetting1 += settingsHeightxx;
            }

            downY += 30.325F;
         }

         index++;
      }

      renderer2D.popClipRect();
      GuiScreen.getScrollUtil().render(
         renderer2D,
         GuiScreen.x + GuiScreen.width - 4.0F,
         GuiScreen.y + GuiScreen.CONTENT_TOP + 5.0F,
         2.0F,
         GuiScreen.height - GuiScreen.CONTENT_TOP - 13.5F,
         mainAlpha
      );
      if (GuiScreen.activeColorPicker != null && GuiScreen.activeColorPicker instanceof HueSetting) {
         GuiRenderColorPicker.renderColorPickerWindow(
            renderer2D,
            GuiScreen.activeColorPicker,
            mouseX,
            mouseY,
            ColorUtil.multAlpha(outlineColor, GuiScreen.animation15.getOutput()),
            ColorUtil.multAlpha(backGroundOneColor, GuiScreen.animation15.getOutput()),
            ColorUtil.multAlpha(mainColor40, GuiScreen.animation15.getOutput()),
            mainAlpha * GuiScreen.animation15.getOutput()
         );
      }
   }

   public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
   }
}