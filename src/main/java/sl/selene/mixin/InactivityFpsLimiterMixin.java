package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(InactivityFpsLimiter.class)
public abstract class InactivityFpsLimiterMixin {
   private static final int OUT_OF_LEVEL_MENU_FPS = 60;

   @Shadow
   private int maxFps;

   @Inject(method = "update", at = @At("RETURN"), cancellable = true)
   private void replaceMenuFps(CallbackInfoReturnable<Integer> cir) {
      if (cir.getReturnValueI() == OUT_OF_LEVEL_MENU_FPS) {
         cir.setReturnValue(this.maxFps);
      }
   }
}