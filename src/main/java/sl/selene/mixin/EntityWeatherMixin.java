package sl.selene.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sl.selene.util.world.ClientWeatherHelper;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityWeatherMixin {
   @ModifyReturnValue(method = {"isBeingRainedOn"}, at = {@At("RETURN")})
   private boolean selene$restoreGameplayRain(boolean original) {
      if (original || !ClientWeatherHelper.shouldHideWeatherVisuals()) {
         return original;
      }

      Entity self = (Entity)(Object)this;
      World world = self.getEntityWorld();
      BlockPos blockPos = self.getBlockPos();
      if (ClientWeatherHelper.hasGameplayRain(world, blockPos)) {
         return true;
      }

      BlockPos headPos = BlockPos.ofFloored(blockPos.getX(), self.getBoundingBox().maxY, blockPos.getZ());
      return ClientWeatherHelper.hasGameplayRain(world, headPos);
   }
}