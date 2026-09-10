package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.Selene;
import sl.selene.module.impl.visuals.Crosshair;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudCrosshairMixin {
   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void selene$cancelVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Selene.get != null && Selene.get.manager != null) {
         Crosshair module = Selene.get.manager.get(Crosshair.class);
         if (module != null && module.enable) {
            ci.cancel();
         }
      }
   }
}