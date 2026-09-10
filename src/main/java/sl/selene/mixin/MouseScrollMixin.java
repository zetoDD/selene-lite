package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.event.EventManager;
import sl.selene.event.input.MouseScrollEvent;

@Environment(EnvType.CLIENT)
@Mixin({Mouse.class})
public class MouseScrollMixin {
   @Inject(
      method = {"onMouseScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleMenuMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      double[] cursorX = new double[1];
      double[] cursorY = new double[1];
      GLFW.glfwGetCursorPos(window, cursorX, cursorY);
      MouseScrollEvent event = new MouseScrollEvent(window, horizontal, vertical, cursorX[0], cursorY[0]);
      EventManager.call(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}