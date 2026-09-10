package sl.selene.ui.gui.component.mouse.module;

import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.client.SeleneKeyBindings;
import sl.selene.module.api.Module;
import sl.selene.module.impl.client.MenuSettingsModule;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.mouse.setting.GuiMouseClickedSetting;
import sl.selene.ui.gui.component.render.GuiRenderMain;
import sl.selene.ui.gui.component.setting.GuiRenderSetting;
import sl.selene.util.keyboard.Keyboard;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.animation.anim.util.Easings;
import sl.selene.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class GuiMouseClickedModule extends GuiScreen {
   public static boolean mouseClickedModule(Renderer2D renderer2D, int mouseX, int mouseY, int pButton) {
      float x1 = GuiScreen.x + GuiScreen.SIDEBAR_WIDTH;
      float y1 = GuiScreen.y + GuiScreen.CONTENT_TOP;
      float rectWidth = GuiScreen.width - GuiScreen.SIDEBAR_WIDTH;
      float rectHeight = GuiScreen.height - GuiScreen.CONTENT_TOP;
      float clipX = x1 + 5.0F;
      float clipY = y1 + 5.0F;
      float clipWidth = rectWidth - 10.0F;
      float clipHeight = rectHeight - 10.0F;
      if (!GuiRenderMain.isHovered(mouseX, mouseY, clipX, clipY, clipWidth, clipHeight)) {
         return false;
      } else {
         List<Module> filteredModules = GuiScreen.modules;
         if (GuiScreen.activeSearch && !GuiScreen.searchText.isEmpty()) {
            String searchLower = GuiScreen.searchText.toLowerCase().trim();
            filteredModules = GuiScreen.modules.stream().filter(modulex -> modulex.name.toLowerCase().contains(searchLower)).collect(Collectors.toList());
         }

         int index = 1;
         float downY = GuiScreen.getScrollUtil().getScroll();
         float downYSetting1 = 0.0F;
         float downYSetting2 = 0.0F;

         for (Module module : filteredModules) {
            float settingsAnim = GuiScreen.getModuleSettingsAnimation(module).get();
            float settingsAlphaAnim = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
            boolean settingsShowing = GuiScreen.openSettingsModules.contains(module) || settingsAnim > 0.0F || settingsAlphaAnim > 0.0F;
            float settingsHeight = 12.0F;
            if (settingsShowing) {
               float fullSettingsHeight = 12.0F;
               for (Setting setting : module.getSettingsForGUI()) {
                  fullSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting) + 1.0F;
               }

               fullSettingsHeight = Math.max(fullSettingsHeight, 20.0F);
               settingsHeight = 12.0F + (fullSettingsHeight - 12.0F) * settingsAnim;
            }

            if (index % 2 == 0) {
               float currentDownY = downY + downYSetting2 - 30.0F;
               float moduleX = GuiScreen.x + 266.35F;
               float moduleY = GuiScreen.y + 55.365F + currentDownY;
               float moduleWidth = 150.0F;
               float moduleHeight = 21.325F;
               if (GuiScreen.openSettingsModules.contains(module) && pButton == 0) {
                  float settingY = GuiScreen.y + 76.69F + currentDownY + 4.0F;
                  float settingX = GuiScreen.x + 275.35F;
                  float settingWidth = 132.47F;
                  float totalSettingsHeight = 0.0F;

                  for (Setting setting : module.getSettingsForGUI()) {
                     float actualSettingY = settingY + totalSettingsHeight;
                     if (GuiMouseClickedSetting.handleSettingClick(renderer2D, setting, settingX, actualSettingY, settingWidth, mouseX, mouseY, pButton)) {
                        return true;
                     }

                     totalSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting) + 1.0F;
                  }
               }

               if (settingsShowing) {
                  downYSetting2 += settingsHeight;
               }

               if (handleBindPillClick(renderer2D, module, GuiScreen.x + 280.35F, GuiScreen.y + 61.555F + currentDownY, mouseX, mouseY, pButton)) {
                  return true;
               }

               if (GuiRenderMain.isHovered(mouseX, mouseY, moduleX, moduleY, moduleWidth, moduleHeight) && pButton == 0) {
                  if (module.holdOnly) {
                     GuiScreen.wobbleBindPill(module);
                  } else {
                     module.toggle();
                  }
               }

               if (GuiRenderMain.isHovered(mouseX, mouseY, moduleX, moduleY, moduleWidth, moduleHeight)
                  && pButton == 1
                  && !module.getSettingsForGUI().isEmpty()) {
                  if (GuiScreen.openSettingsModules.contains(module)) {
                     GuiScreen.closeModuleSettings(module);
                  } else {
                     GuiScreen.openModuleSettings(module);
                  }
               }

               if (GuiRenderMain.isHovered(mouseX, mouseY, moduleX, moduleY, moduleWidth, moduleHeight) && pButton == 2) {
                  if (module.binding) {
                     module.binding = false;
                     GuiScreen.activeModuleBind = null;
                     GuiScreen.getModuleBindAnimation(module).run(0.0, 0.2F, Easings.SINE_OUT);
                  } else {
                     if (GuiScreen.activeModuleBind != null) {
                        GuiScreen.activeModuleBind.binding = false;
                        GuiScreen.getModuleBindAnimation(GuiScreen.activeModuleBind).run(0.0, 0.2F, Easings.SINE_OUT);
                     }

                     GuiScreen.activeModuleBind = module;
                     module.binding = true;
                     GuiScreen.getModuleBindAnimation(module).run(1.0, 0.2F, Easings.SINE_OUT);
                  }

                  return true;
               }

            } else {
               float currentDownYx = downY + downYSetting1;
               float moduleXx = GuiScreen.x + 111.885F;
               float moduleYx = GuiScreen.y + 55.365F + currentDownYx;
               float moduleWidthx = 150.0F;
               float moduleHeightx = 21.325F;
               if (GuiScreen.openSettingsModules.contains(module) && pButton == 0) {
                  float settingY = GuiScreen.y + 76.69F + currentDownYx + 4.0F;
                  float settingX = GuiScreen.x + 111.885F + 9.0F;
                  float settingWidth = 132.47F;
                  float totalSettingsHeight = 0.0F;

                  for (Setting setting : module.getSettingsForGUI()) {
                     float actualSettingY = settingY + totalSettingsHeight;
                     if (GuiMouseClickedSetting.handleSettingClick(renderer2D, setting, settingX, actualSettingY, settingWidth, mouseX, mouseY, pButton)) {
                        return true;
                     }

                     totalSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting) + 1.0F;
                  }
               }

               if (settingsShowing) {
                  downYSetting1 += settingsHeight;
               }

               if (handleBindPillClick(renderer2D, module, GuiScreen.x + 125.885F, GuiScreen.y + 61.555F + currentDownYx, mouseX, mouseY, pButton)) {
                  return true;
               }

               if (GuiRenderMain.isHovered(mouseX, mouseY, moduleXx, moduleYx, moduleWidthx, moduleHeightx) && pButton == 0) {
                  if (module.holdOnly) {
                     GuiScreen.wobbleBindPill(module);
                  } else {
                     module.toggle();
                  }
               }

               if (GuiRenderMain.isHovered(mouseX, mouseY, moduleXx, moduleYx, moduleWidthx, moduleHeightx)
                  && pButton == 1
                  && !module.getSettingsForGUI().isEmpty()) {
                  if (GuiScreen.openSettingsModules.contains(module)) {
                     GuiScreen.closeModuleSettings(module);
                  } else {
                     GuiScreen.openModuleSettings(module);
                  }
               }

               if (GuiRenderMain.isHovered(mouseX, mouseY, moduleXx, moduleYx, moduleWidthx, moduleHeightx) && pButton == 2) {
                  if (module.binding) {
                     module.binding = false;
                     GuiScreen.activeModuleBind = null;
                     GuiScreen.getModuleBindAnimation(module).run(0.0, 1.0, Easings.SINE_OUT);
                  } else {
                     if (GuiScreen.activeModuleBind != null) {
                        GuiScreen.activeModuleBind.binding = false;
                        GuiScreen.getModuleBindAnimation(GuiScreen.activeModuleBind).run(0.0, 1.0, Easings.SINE_OUT);
                     }

                     GuiScreen.activeModuleBind = module;
                     module.binding = true;
                     GuiScreen.getModuleBindAnimation(module).run(1.0, 1.0, Easings.SINE_OUT);
                  }

                  return true;
               }

               downY += 30.325F;
            }

            index++;
         }

         return false;
      }
   }

   private static boolean handleBindPillClick(Renderer2D renderer2D, Module module, float moduleNameX, float moduleNameY, int mouseX, int mouseY, int pButton) {
      float moduleNameWidth = renderer2D.measureText(FontRegistry.INTER_SEMIBOLD, module.name, 14.0F).width;
      float bindX = moduleNameX + moduleNameWidth + 6.0F;
      float bindY = moduleNameY - 1.0F;
      String keyText = module.binding ? "..." : (module.bind != -1 ? Keyboard.keyName(module.bind) : "None");
      float keyTextWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, keyText, 12.0F).width;
      float buttonWidth = Math.max(6.0F, keyTextWidth + 6.0F);
      if (!GuiRenderMain.isHovered(mouseX, mouseY, bindX, bindY, buttonWidth, 14.0F)) {
         return false;
      }

      if (module.binding && pButton >= 0 && pButton <= 7) {
         module.bind = -100 - pButton;
         module.binding = false;
         if (module instanceof MenuSettingsModule) {
            SeleneKeyBindings.syncFromMenuModule();
         }

         GuiScreen.activeModuleBind = null;
         GuiScreen.getModuleBindAnimation(module).run(1.0, 0.2F, Easings.SINE_OUT);
         return true;
      }

      if (pButton == 0 || pButton == 2) {
         if (module.binding) {
            module.binding = false;
            GuiScreen.activeModuleBind = null;
            GuiScreen.getModuleBindAnimation(module).run(0.0, 0.2F, Easings.SINE_OUT);
         } else {
            if (GuiScreen.activeModuleBind != null) {
               GuiScreen.activeModuleBind.binding = false;
               GuiScreen.getModuleBindAnimation(GuiScreen.activeModuleBind).run(0.0, 0.2F, Easings.SINE_OUT);
            }

            GuiScreen.activeModuleBind = module;
            module.binding = true;
            GuiScreen.getModuleBindAnimation(module).run(1.0, 0.2F, Easings.SINE_OUT);
         }

         return true;
      }

      return false;
   }

   public static float[] findColorPickerPosition(Renderer2D renderer2D, HueSetting hueSetting) {
      if (hueSetting == null) {
         return null;
      } else {
         int index = 1;
         float downY = GuiScreen.getScrollUtil().getScroll();
         float downYSetting1 = 0.0F;
         float downYSetting2 = 0.0F;

         for (Module module : GuiScreen.modules) {
            float settingsAnim = GuiScreen.getModuleSettingsAnimation(module).get();
            float settingsAlphaAnim = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
            boolean settingsShowing = GuiScreen.openSettingsModules.contains(module) || settingsAnim > 0.0F || settingsAlphaAnim > 0.0F;
            float settingsHeight = 12.0F;
            if (settingsShowing) {
               float fullSettingsHeight = 12.0F;
               for (Setting setting : module.getSettingsForGUI()) {
                  fullSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting) + 1.0F;
               }

               fullSettingsHeight = Math.max(fullSettingsHeight, 20.0F);
               settingsHeight = 12.0F + (fullSettingsHeight - 12.0F) * settingsAnim;
            }

            if (index % 2 == 0) {
               float currentDownY = downY + downYSetting2 - 30.0F;
               if (settingsShowing) {
                  float settingY = GuiScreen.y + 76.69F + currentDownY + 4.0F;
                  float settingX = GuiScreen.x + 275.35F;
                  float settingWidth = 132.47F;
                  float totalSettingsHeight = 0.0F;

                  for (Setting setting : module.getSettingsForGUI()) {
                     if (setting == hueSetting) {
                        float pickerX = settingX + settingWidth - 15.0F;
                        float pickerY = settingY + totalSettingsHeight * settingsAlphaAnim - 5.0F;
                        return new float[]{pickerX, pickerY};
                     }

                     totalSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting) + 1.0F;
                  }

                  downYSetting2 += settingsHeight;
               }
            } else {
               float currentDownY = downY + downYSetting1;
               if (settingsShowing) {
                  float settingY = GuiScreen.y + 76.69F + currentDownY + 4.0F;
                  float settingX = GuiScreen.x + 111.885F + 9.0F;
                  float settingWidth = 132.47F;
                  float totalSettingsHeight = 0.0F;

                  for (Setting setting : module.getSettingsForGUI()) {
                     if (setting == hueSetting) {
                        float pickerX = settingX + settingWidth - 15.0F;
                        float pickerY = settingY + totalSettingsHeight * settingsAlphaAnim - 5.0F;
                        return new float[]{pickerX, pickerY};
                     }

                     totalSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting) + 1.0F;
                  }

                  downYSetting1 += settingsHeight;
               }

               downY += 30.325F;
            }

            index++;
         }

         return null;
      }
   }
}