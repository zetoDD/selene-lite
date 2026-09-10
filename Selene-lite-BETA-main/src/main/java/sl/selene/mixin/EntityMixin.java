package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sl.selene.module.impl.donut.Freecam;
import sl.selene.util.player.RotationUtil;

@Environment(EnvType.CLIENT)
@Mixin({ Entity.class })
public abstract class EntityMixin {

   @Inject(method = "getRotationVec", at = @At("HEAD"), cancellable = true)
   private void selene$silentRotationVec(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
      if ((Object) this == MinecraftClient.getInstance().player && RotationUtil.hasSilentRotation()) {
         cir.setReturnValue(RotationUtil.getRotationVector(RotationUtil.getServerPitch(), RotationUtil.getServerYaw()));
      }
   }

   @Inject(method = "getHeadRotationVector", at = @At("HEAD"), cancellable = true)
   private void selene$silentHeadRotationVec(CallbackInfoReturnable<Vec3d> cir) {
      if ((Object) this == MinecraftClient.getInstance().player && RotationUtil.hasSilentRotation()) {
         cir.setReturnValue(RotationUtil.getRotationVector(RotationUtil.getServerPitch(), RotationUtil.getServerYaw()));
      }
   }

   @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
   private void selene$freecamSneaking(CallbackInfoReturnable<Boolean> cir) {
      if ((Object) this == MinecraftClient.getInstance().player && Freecam.isActive()) {
         cir.setReturnValue(Freecam.instance.shouldRenderSneaking());
      }
   }

   @Inject(method = "shouldRender(D)Z", at = @At("HEAD"), cancellable = true)
   private void selene$freecamShouldRender(double distance, CallbackInfoReturnable<Boolean> cir) {
      if (Freecam.isActive() && Freecam.instance.shouldRevealEntities() && distance < Freecam.getMaxRenderDistance()) {
         cir.setReturnValue(true);
      }
   }
}