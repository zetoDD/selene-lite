package sl.selene.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import sl.selene.Selene;
import sl.selene.client.SeleneKeyBindings;
import sl.selene.cfg.ConfigManager;
import sl.selene.module.impl.client.MenuSettingsModule;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.render.GuiRenderConfigPanel;
import sl.selene.util.render.math.animation.anim.util.Easings;

@Environment(EnvType.CLIENT)
public class GuiKeyPressed extends GuiScreen {
   public static boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (GuiScreen.configInputActive) {
         if (keyCode == 256) {
            GuiScreen.configInputActive = false;
            return true;
         }

         if (keyCode == 257 || keyCode == 335) {
            String name = GuiScreen.configInputText;
            if (ConfigManager.isValidConfigName(name) && Selene.get != null && Selene.get.configManager != null) {
               Selene.get.configManager.saveConfig(name);
               GuiRenderConfigPanel.setStatus("Saved '" + name + "'");
               GuiScreen.configInputText = "";
            } else {
               GuiRenderConfigPanel.setStatus("Invalid name");
            }

            GuiScreen.configInputActive = false;
            GuiScreen.configDeleteArmed = false;
            return true;
         }

         if (keyCode == 259) {
            if (!GuiScreen.configInputText.isEmpty()) {
               GuiScreen.configInputText = GuiScreen.configInputText.substring(0, GuiScreen.configInputText.length() - 1);
            }

            return true;
         }
      }

      if (GuiScreen.friendInputActive) {
         if (keyCode == 256) {

            GuiScreen.friendInputActive = false;
            return true;
         }

         if (keyCode == 257 || keyCode == 335) {

            String name = GuiScreen.friendInputText.trim();
            if (!name.isEmpty() && Selene.get != null && Selene.get.friendManager != null) {
               if (Selene.get.friendManager.isFriend(name)) {
                  Selene.get.friendManager.remove(name);
               } else {
                  Selene.get.friendManager.add(name);
               }

               if (Selene.get.configManager != null) {
                  Selene.get.configManager.autoSave();
               }
            }

            GuiScreen.friendInputText = "";
            GuiScreen.friendInputActive = false;
            return true;
         }

         if (keyCode == 259) {

            if (!GuiScreen.friendInputText.isEmpty()) {
               GuiScreen.friendInputText = GuiScreen.friendInputText.substring(0, GuiScreen.friendInputText.length() - 1);
            }

            return true;
         }
      }

      if (GuiScreen.activeModuleBind != null) {
         if (keyCode == 256) {
            GuiScreen.activeModuleBind.binding = false;
            GuiScreen.activeModuleBind = null;
         } else if (keyCode == 261) {
            GuiScreen.activeModuleBind.bind = -1;
            GuiScreen.activeModuleBind.binding = false;
            GuiScreen.getModuleBindAnimation(GuiScreen.activeModuleBind).run(0.0, 0.2F, Easings.SINE_OUT);
            if (GuiScreen.activeModuleBind instanceof MenuSettingsModule) {
               SeleneKeyBindings.setBoundKeyCode(SeleneKeyBindings.DEFAULT_MENU_KEY);
               GuiScreen.activeModuleBind.bind = SeleneKeyBindings.DEFAULT_MENU_KEY;
            }

            GuiScreen.activeModuleBind = null;
            if (Selene.get.configManager != null) {
               Selene.get.configManager.autoSave();
            }
         } else {
            GuiScreen.activeModuleBind.bind = keyCode;
            GuiScreen.activeModuleBind.binding = false;
            GuiScreen.getModuleBindAnimation(GuiScreen.activeModuleBind).run(1.0, 0.2F, Easings.SINE_OUT);
            if (GuiScreen.activeModuleBind instanceof MenuSettingsModule) {
               SeleneKeyBindings.syncFromMenuModule();
            }

            GuiScreen.activeModuleBind = null;
            if (Selene.get.configManager != null) {
               Selene.get.configManager.autoSave();
            }
         }

         return true;
      } else if (GuiScreen.activeBindSetting != null) {
         if (keyCode == 256) {
            GuiScreen.activeBindSetting.active = false;
            GuiScreen.activeBindSetting = null;
         } else if (keyCode == 261) {
            GuiScreen.activeBindSetting.key = -1;
            GuiScreen.activeBindSetting.active = false;
            GuiScreen.activeBindSetting = null;
            if (Selene.get.configManager != null) {
               Selene.get.configManager.autoSave();
            }
         } else {
            GuiScreen.activeBindSetting.key = keyCode;
            GuiScreen.activeBindSetting.active = false;
            GuiScreen.activeBindSetting = null;
            if (Selene.get.configManager != null) {
               Selene.get.configManager.autoSave();
            }
         }

         return true;
      } else {
         if (GuiScreen.activeStringSetting != null) {
            if (keyCode == 256) {
               GuiScreen.activeStringSetting.active = false;
               GuiScreen.activeStringSetting = null;
               if (Selene.get.configManager != null) {
                  Selene.get.configManager.autoSave();
               }

               return true;
            }

            if (keyCode == 259) {
               if (!GuiScreen.activeStringSetting.input.isEmpty()) {
                  GuiScreen.activeStringSetting.input = GuiScreen.activeStringSetting.input.substring(0, GuiScreen.activeStringSetting.input.length() - 1);
                  if (Selene.get.configManager != null) {
                     Selene.get.configManager.autoSave();
                  }
               }

               return true;
            }
         }

         if (GuiScreen.activeSearch) {
            if (keyCode == 256) {
               GuiScreen.activeSearch = false;
               GuiScreen.searchText = "";
               GuiScreen.searchSelectAll = false;
               return true;
            }

            if (isCtrlDown(modifiers)) {
               if (keyCode == GLFW.GLFW_KEY_A) {
                  GuiScreen.searchSelectAll = !GuiScreen.searchText.isEmpty();
                  return true;
               }

               if (keyCode == GLFW.GLFW_KEY_C && mc.keyboard != null) {
                  mc.keyboard.setClipboard(GuiScreen.searchText);
                  return true;
               }

               if (keyCode == GLFW.GLFW_KEY_V && mc.keyboard != null) {
                  String clipboard = mc.keyboard.getClipboard();
                  if (clipboard != null && !clipboard.isEmpty()) {
                     if (GuiScreen.searchSelectAll) {
                        GuiScreen.searchText = "";
                        GuiScreen.searchSelectAll = false;
                     }

                     GuiScreen.searchText = appendSearchText(GuiScreen.searchText, clipboard);
                  }

                  return true;
               }
            }

            if (keyCode == 259) {
               if (GuiScreen.searchSelectAll) {
                  GuiScreen.searchText = "";
                  GuiScreen.searchSelectAll = false;
               }

               return true;
            }
         }

         return false;
      }
   }

   private static boolean isCtrlDown(int modifiers) {
      return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
   }

   private static String appendSearchText(String current, String addition) {
      StringBuilder builder = new StringBuilder(current);

      for (int i = 0; i < addition.length() && builder.length() < 50; i++) {
         char ch = addition.charAt(i);
         if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9' || ch == ' ') {
            builder.append(ch);
         }
      }

      return builder.toString();
   }
}