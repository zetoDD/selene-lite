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
         float[] columnSettingsHeights = { 0.0F, 0.0F };

         for (Module module : filteredModules) {
            float settingsAnim = GuiScreen.getModuleSettingsAnimation(module).get();
            float settingsAlphaAnim = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
            boolean settingsShowing = GuiScreen.openSettingsModules.contains(module) || settingsAnim > 0.0F || settingsAlphaAnim > 0.0F;
            float settingsHeight = GuiRenderSetting.getAnimatedSettingsHeight(renderer2D, module.getSettingsForGUI(), settingsAnim, settingsShowing);

            int column = index % 2 == 0 ? 1 : 0;
            float columnX = column == 1 ? MODULE_COLUMN_2_X : MODULE_COLUMN_1_X;
            float moduleX = GuiScreen.x + columnX;
            float moduleY = GuiScreen.y + MODULE_CARD_TOP + downY + columnSettingsHeights[column]
                  - (column == 1 ? MODULE_COLUMN_STAGGER : 0.0F);

            if (handleModuleClickAt(renderer2D, module, moduleX, moduleY, mouseX, mouseY, pButton)) {
               return true;
            }

            if (settingsShowing) {
               columnSettingsHeights[column] += settingsHeight;
            }

            if (column == 0) {
               downY += MODULE_ROW_PITCH;
            }

            index++;
         }

         return false;
      }
   }

   private static boolean handleModuleClickAt(Renderer2D renderer2D, Module module, float moduleX, float moduleY, int mouseX, int mouseY, int pButton) {
      if (GuiScreen.openSettingsModules.contains(module) && pButton == 0) {
         float settingY = moduleY + MODULE_CARD_HEIGHT + MODULE_SETTING_GAP;
         float settingX = moduleX + MODULE_SETTING_INSET_X;
         float settingWidth = SETTING_WIDTH;
         float totalSettingsHeight = 0.0F;

         for (Setting setting : module.getSettingsForGUI()) {
            float actualSettingY = settingY + totalSettingsHeight;
            if (GuiMouseClickedSetting.handleSettingClick(renderer2D, setting, settingX, actualSettingY, settingWidth, mouseX, mouseY, pButton)) {
               return true;
            }

            totalSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting) + 1.0F;
         }
      }

      if (handleBindPillClick(renderer2D, module, moduleX + MODULE_NAME_OFFSET_X, moduleY + MODULE_NAME_OFFSET_Y, mouseX, mouseY, pButton)) {
         return true;
      }

      if (GuiRenderMain.isHovered(mouseX, mouseY, moduleX, moduleY, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT) && pButton == 0) {
         if (module.holdOnly) {
            GuiScreen.wobbleBindPill(module);
         } else {
            module.toggle();
         }
      }

      if (GuiRenderMain.isHovered(mouseX, mouseY, moduleX, moduleY, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT)
         && pButton == 1
         && !module.getSettingsForGUI().isEmpty()) {
         if (GuiScreen.openSettingsModules.contains(module)) {
            GuiScreen.closeModuleSettings(module);
         } else {
            GuiScreen.openModuleSettings(module);
         }
      }

      if (GuiRenderMain.isHovered(mouseX, mouseY, moduleX, moduleY, MODULE_CARD_WIDTH, MODULE_CARD_HEIGHT) && pButton == 2) {
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
         float[] columnSettingsHeights = { 0.0F, 0.0F };

         for (Module module : GuiScreen.modules) {
            float settingsAnim = GuiScreen.getModuleSettingsAnimation(module).get();
            float settingsAlphaAnim = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
            boolean settingsShowing = GuiScreen.openSettingsModules.contains(module) || settingsAnim > 0.0F || settingsAlphaAnim > 0.0F;
            float settingsHeight = GuiRenderSetting.getAnimatedSettingsHeight(renderer2D, module.getSettingsForGUI(), settingsAnim, settingsShowing);

            int column = index % 2 == 0 ? 1 : 0;
            float columnX = column == 1 ? MODULE_COLUMN_2_X : MODULE_COLUMN_1_X;
            float moduleY = GuiScreen.y + MODULE_CARD_TOP + downY + columnSettingsHeights[column]
                  - (column == 1 ? MODULE_COLUMN_STAGGER : 0.0F);

            if (settingsShowing) {
               float settingY = moduleY + MODULE_CARD_HEIGHT + MODULE_SETTING_GAP;
               float settingX = GuiScreen.x + columnX + MODULE_SETTING_INSET_X;
               float totalSettingsHeight = 0.0F;

               for (Setting setting : module.getSettingsForGUI()) {
                  if (setting == hueSetting) {
                     float pickerX = settingX + SETTING_WIDTH - 15.0F;
                     float pickerY = settingY + totalSettingsHeight * settingsAlphaAnim - 5.0F;
                     return new float[]{pickerX, pickerY};
                  }

                  totalSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting) + 1.0F;
               }

               columnSettingsHeights[column] += settingsHeight;
            }

            if (column == 0) {
               downY += MODULE_ROW_PITCH;
            }

            index++;
         }

         return null;
      }
   }
}

