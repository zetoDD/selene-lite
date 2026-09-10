package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sl.selene.Selene;
import sl.selene.module.impl.visuals.FullBright;

@Environment(EnvType.CLIENT)
@Mixin({LightmapTextureManager.class})
public class LightmapTextureManagerMixin {
   private static final float FULL_BRIGHT_GAMMA = 200.0F;

   @Redirect(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Double;floatValue()F",
         ordinal = 1
      )
   )
   private float selene$boostGamma(Double instance) {
      if (Selene.get != null && Selene.get.manager != null) {
         FullBright module = Selene.get.manager.get(FullBright.class);
         if (module != null && module.enable) {
            return Math.max(0.0F, FullBright.brightness.get()) * FULL_BRIGHT_GAMMA;
         }
      }
      return instance.floatValue();
   }
}