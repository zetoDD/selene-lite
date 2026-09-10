package sl.selene.module.bind;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import sl.selene.Selene;
import sl.selene.client.SeleneKeyBindings;
import sl.selene.module.impl.client.MenuSettingsModule;
import sl.selene.event.EventInit;
import sl.selene.event.EventManager;
import sl.selene.event.input.KeyInputEvent;
import sl.selene.event.input.MouseButtonEvent;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.ui.gui.GuiScreen;

@Environment(EnvType.CLIENT)
public class BindingManager {
   private static final BindingManager INSTANCE = new BindingManager();
   private boolean initialized = false;
   private boolean awaitingCapture = false;

   public static BindingManager getInstance() {
      return INSTANCE;
   }

   public void initialize() {
      if (!this.initialized) {
         EventManager.register(this);
         this.initialized = true;
      }
   }

   @EventInit
   public void onKeyInput(KeyInputEvent event) {
      if (Module.isConfigLoadInProgress()) {
         return;
      }

      if (isIgnoredBindKey(event.key())) {
         return;
      }

      if (event.action() == GLFW.GLFW_RELEASE) {

         this.releaseBoundModules(event.key());
         return;
      }

      if (event.action() != GLFW.GLFW_PRESS) {
         return;
      }

      if (GuiScreen.activeBindSetting != null || GuiScreen.activeModuleBind != null || this.awaitingCapture) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.currentScreen != null) {
         return;
      }

      if (Selene.get.manager == null) {
         return;
      }

      this.pressBoundModules(event.key());
   }

   @EventInit
   public void onMouseButton(MouseButtonEvent event) {
      if (Module.isConfigLoadInProgress()) {
         return;
      }

      if (event.button() < 0 || event.button() > 7) {
         return;
      }

      int mouseBindCode = -100 - event.button();
      if (event.isRelease()) {
         this.releaseBoundModules(mouseBindCode);
         return;
      }

      if (!event.isPress()) {
         return;
      }

      if (GuiScreen.activeBindSetting != null || GuiScreen.activeModuleBind != null || this.awaitingCapture) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.currentScreen != null) {
         return;
      }

      if (Selene.get == null || Selene.get.manager == null) {
         return;
      }

      this.pressBoundModules(mouseBindCode);
   }

   private static boolean isIgnoredBindKey(int key) {
      if (key <= 0 || key == GLFW.GLFW_KEY_UNKNOWN) {
         return true;
      }

      return switch (key) {
         case GLFW.GLFW_KEY_PAUSE, GLFW.GLFW_KEY_PRINT_SCREEN, GLFW.GLFW_KEY_SCROLL_LOCK, GLFW.GLFW_KEY_NUM_LOCK,
               GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_MENU,
               314, 315, 316, 317, 318, 319, 320, 321, 322, 323 -> true;
         default -> false;
      };
   }

   public void clearAllBindings() {
   }

   public void clearModuleBinds(String name) {
   }

   public void setAwaitingCapture(boolean awaiting) {
      this.awaitingCapture = awaiting;
   }

   public boolean isAwaitingCapture() {
      return this.awaitingCapture;
   }

   public void updateModuleBinding(Module module, int keyCode, BindingMode mode) {
      if (module != null) {
         module.bind = keyCode;
         if (module instanceof MenuSettingsModule) {
            SeleneKeyBindings.syncFromMenuModule();
         }
      }
   }

   public void putSettingBinding(Module module, Setting setting, BindingMode mode, int keyCode, Object targetValue) {
   }

   public void removeSettingBinding(String moduleName, String settingName) {
   }

   public Object getSettingBinding(String moduleName, String settingName) {
      return null;
   }

   public String formatKeyName(int keyCode) {
      if (keyCode == -1) {
         return "None";
      }
      return sl.selene.util.keyboard.Keyboard.keyName(keyCode);
   }

   private void pressBoundModules(int bindCode) {
      Module[] modules = Selene.get.manager.getBind(bindCode);
      if (modules == null) {
         return;
      }
      for (Module module : modules) {
         if (module.holdOnly) {
            if (!module.enable) {
               module.setState(true);
            }
         } else {
            module.toggle();
         }
      }
   }

   private void releaseBoundModules(int bindCode) {
      if (Selene.get == null || Selene.get.manager == null) {
         return;
      }
      Module[] modules = Selene.get.manager.getBind(bindCode);
      if (modules == null) {
         return;
      }
      for (Module module : modules) {
         if (module.holdOnly && module.enable) {
            module.setState(false);
         }
      }
   }
}