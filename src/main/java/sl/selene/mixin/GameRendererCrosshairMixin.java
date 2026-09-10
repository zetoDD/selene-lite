package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.hit.HitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sl.selene.util.player.CrosshairPin;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererCrosshairMixin {

   @Redirect(
      method = "updateCrosshairTarget",
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;",
         opcode = Opcodes.PUTFIELD
      )
   )
   private void selene$keepPinnedCrosshair(MinecraftClient instance, HitResult recomputed) {
      HitResult pinned = CrosshairPin.get();
      instance.crosshairTarget = pinned != null ? pinned : recomputed;
   }
}