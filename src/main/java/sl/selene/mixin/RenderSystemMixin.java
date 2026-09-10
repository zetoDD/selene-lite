package sl.selene.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.Selene;

@Environment(EnvType.CLIENT)
@Mixin({RenderSystem.class})
public class RenderSystemMixin {
   @Inject(
      method = {"flipFrame"},
      at = {@At("HEAD")}
   )
   private static void flipFrame(Window window, TracyFrameCapturer capturer, CallbackInfo ci) {
      Selene.onRender();
   }
}