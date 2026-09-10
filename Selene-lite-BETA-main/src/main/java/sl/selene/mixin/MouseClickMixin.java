package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.event.EventManager;
import sl.selene.event.input.CursorLockEvent;
import sl.selene.event.input.MouseButtonEvent;

@Environment(EnvType.CLIENT)
@Mixin({Mouse.class})
public class MouseClickMixin {
   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleMenuMouseClick(long window, MouseInput input, int action, CallbackInfo ci) {
      double[] xPos = new double[1];
      double[] yPos = new double[1];
      GLFW.glfwGetCursorPos(window, xPos, yPos);
      MouseButtonEvent event = new MouseButtonEvent(window, input.button(), action, input.modifiers(), xPos[0], yPos[0]);
      EventManager.call(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"lockCursor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void preventCursorLock(CallbackInfo ci) {
      MinecraftClient client = MinecraftClient.getInstance();
      long handle = 0L;
      if (client != null && client.getWindow() != null) {
         handle = client.getWindow().getHandle();
      }

      CursorLockEvent event = new CursorLockEvent(client, handle);
      EventManager.call(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}