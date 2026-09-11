package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sl.selene.module.impl.visuals.BossBarRemover;

@Environment(EnvType.CLIENT)
@Mixin(BossBarHud.class)
public class BossBarHudMixin {

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void selene$hideBossBars(DrawContext context, CallbackInfo ci) {
      if (BossBarRemover.isHiding()) {
         ci.cancel();
      }
   }

   @Inject(method = "shouldDarkenSky", at = @At("HEAD"), cancellable = true)
   private void selene$hideSkyDarkening(CallbackInfoReturnable<Boolean> cir) {
      if (BossBarRemover.isHidingEffects()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "shouldThickenFog", at = @At("HEAD"), cancellable = true)
   private void selene$hideFogThickening(CallbackInfoReturnable<Boolean> cir) {
      if (BossBarRemover.isHidingEffects()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "shouldPlayDragonMusic", at = @At("HEAD"), cancellable = true)
   private void selene$hideDragonMusic(CallbackInfoReturnable<Boolean> cir) {
      if (BossBarRemover.isHidingEffects()) {
         cir.setReturnValue(false);
      }
   }
}

