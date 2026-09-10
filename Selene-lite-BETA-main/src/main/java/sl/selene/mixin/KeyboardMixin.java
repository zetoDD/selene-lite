package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.Selene;
import sl.selene.event.EventManager;
import sl.selene.event.input.KeyInputEvent;

@Environment(EnvType.CLIENT)
@Mixin({Keyboard.class})
public class KeyboardMixin {
   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleMenuKeyEvent(long window, int action, KeyInput input, CallbackInfo ci) {
      if (Selene.isModInitialized() && Selene.get != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.getWindow() != null && client.currentScreen == null) {
            KeyInputEvent event = new KeyInputEvent(window, input.key(), input.scancode(), action, input.modifiers());
            EventManager.call(event);
            if (event.isCancelled()) {
               ci.cancel();
            }
         }
      }
   }
}