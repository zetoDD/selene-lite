package sl.selene.module.impl.misc;

import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.lwjgl.glfw.GLFW;
import sl.selene.event.EventInit;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.other.TimerUtil;

@IModule(
   name = "Item Scroller",
   description = "Quickly move items in the inventory (Shift+LMB)",
   category = Category.Utils,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class ItemScroller extends Module {
   public static SliderSetting delay = new SliderSetting("Delay (ms)", 50.0F, 0.0F, 200.0F, 1.0F, false);
   public final TimerUtil time = new TimerUtil();
   private static ItemScroller instance;

   public ItemScroller() {
      this.addSettings(new Setting[]{delay});
      instance = this;
      this.time.reset();
   }

   public static ItemScroller getInstance() {
      return instance;
   }

   @EventInit
   public void update(ClientTickEvent event) {
      if (mc.player != null && mc.currentScreen != null) {
         if (mc.currentScreen instanceof HandledScreen<?> screen) {
            if (mc.getWindow() != null) {
               long windowHandle = mc.getWindow().getHandle();
               boolean isShiftPressed = GLFW.glfwGetKey(windowHandle, 340) == 1 || GLFW.glfwGetKey(windowHandle, 344) == 1;
               boolean isLeftMousePressed = GLFW.glfwGetMouseButton(windowHandle, 0) == 1;
               if (isShiftPressed && isLeftMousePressed) {
                  double[] mouseX = new double[1];
                  double[] mouseY = new double[1];
                  GLFW.glfwGetCursorPos(windowHandle, mouseX, mouseY);

                  try {
                     Method getSlotAtMethod = HandledScreen.class.getDeclaredMethod("getSlotAt", double.class, double.class);
                     getSlotAtMethod.setAccessible(true);
                     Slot slot = (Slot)getSlotAtMethod.invoke(screen, mouseX[0], mouseY[0]);
                     if (slot != null && slot.hasStack() && this.time.hasTimeElapsed((long)delay.get())) {
                        mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                        this.time.reset();
                     }
                  } catch (Exception var11) {
                  }
               }
            }
         }
      }
   }
}