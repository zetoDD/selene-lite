package sl.selene.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sl.selene.module.impl.utils.Optimizer;

@Environment(EnvType.CLIENT)
@Mixin(World.class)
public abstract class WorldWeatherMixin {
   @ModifyReturnValue(method = "getRainGradient", at = {@At("RETURN")})
   private float night$alwaysClear_rainGradient(float original) {
      return Optimizer.shouldDisableWeather() ? 0.0F : original;
   }

   @ModifyReturnValue(method = "getThunderGradient", at = {@At("RETURN")})
   private float night$alwaysClear_thunderGradient(float original) {
      return Optimizer.shouldDisableWeather() ? 0.0F : original;
   }
}