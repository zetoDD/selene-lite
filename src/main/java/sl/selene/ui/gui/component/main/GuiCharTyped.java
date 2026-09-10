package sl.selene.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;
import sl.selene.ui.gui.GuiScreen;

@Environment(EnvType.CLIENT)
public class GuiCharTyped extends GuiScreen {
   public static boolean charTyped(char codePoint, int modifiers) {
      if (GuiScreen.configInputActive) {
         if (codePoint >= ' ' && codePoint != 127) {
            char c = codePoint;
            boolean valid = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == ' ' || c == '-' || c == '_';
            if (valid && GuiScreen.configInputText.length() < 24) {
               GuiScreen.configInputText = GuiScreen.configInputText + c;
            }

            return true;
         }
      }

      if (GuiScreen.friendInputActive) {
         if (codePoint == '\b') {
            if (!GuiScreen.friendInputText.isEmpty()) {
               GuiScreen.friendInputText = GuiScreen.friendInputText.substring(0, GuiScreen.friendInputText.length() - 1);
            }

            return true;
         }

         if (codePoint >= ' ' && codePoint != 127) {
            if (GuiScreen.friendInputText.length() < 16) {
               GuiScreen.friendInputText = GuiScreen.friendInputText + codePoint;
            }

            return true;
         }
      }

      if (GuiScreen.activeStringSetting != null) {
         if (codePoint == '\b') {
            if (!GuiScreen.activeStringSetting.input.isEmpty()) {
               GuiScreen.activeStringSetting.input = GuiScreen.activeStringSetting.input.substring(0, GuiScreen.activeStringSetting.input.length() - 1);
               if (Selene.get.configManager != null) {
                  Selene.get.configManager.autoSave();
               }
            }

            return true;
         }

         if (codePoint >= ' ' && codePoint != 127) {
            if (GuiScreen.activeStringSetting.input.length() < 16) {
               GuiScreen.activeStringSetting.input = GuiScreen.activeStringSetting.input + codePoint;
               if (Selene.get.configManager != null) {
                  Selene.get.configManager.autoSave();
               }
            }

            return true;
         }
      }

      if (GuiScreen.activeSearch) {
         if (codePoint == '\b') {
            if (GuiScreen.searchSelectAll) {
               GuiScreen.searchText = "";
               GuiScreen.searchSelectAll = false;
            }

            return true;
         }

         if (codePoint >= ' '
            && codePoint != 127
            && (codePoint >= 'a' && codePoint <= 'z' || codePoint >= 'A' && codePoint <= 'Z' || codePoint >= '0' && codePoint <= '9' || codePoint == ' ')) {
            if (GuiScreen.searchSelectAll) {
               GuiScreen.searchText = "";
               GuiScreen.searchSelectAll = false;
            }

            if (GuiScreen.searchText.length() < 50) {
               GuiScreen.searchText = GuiScreen.searchText + codePoint;
            }

            return true;
         }
      }

      return false;
   }
}