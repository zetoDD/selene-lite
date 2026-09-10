package sl.selene.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer.MappedView;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sl.selene.module.impl.utils.Optimizer;

@Environment(EnvType.CLIENT)
@Mixin({FogRenderer.class})
public abstract class FogRendererMixin {
   @Shadow
   @Final
   private MappableRingBuffer fogBuffer;

   @Shadow
   protected abstract void applyFog(ByteBuffer var1, int var2, Vector4f var3, float var4, float var5, float var6, float var7, float var8, float var9);

   @Shadow
   protected abstract Vector4f getFogColor(Camera var1, float var2, ClientWorld var3, int var4, float var5);

   @Inject(
      method = {"applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void night$applyFog(
      Camera camera, int viewDistance, RenderTickCounter tickCounter, float skyDarkness, ClientWorld world, CallbackInfoReturnable<Vector4f> cir
   ) {
      if (Optimizer.shouldDisableFog()) {
         cir.cancel();
         float f = tickCounter.getTickProgress(false);
         Vector4f vector4f = this.getFogColor(camera, f, world, viewDistance, skyDarkness);
         float far = Math.max(1024.0F, viewDistance * 64.0F);
         MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.fogBuffer.getBlocking(), false, true);

         try {
            this.applyFog(mappedView.data(), 0, vector4f, far, far, far, far, far, far);
         } catch (Throwable var24) {
            if (mappedView != null) {
               try {
                  mappedView.close();
               } catch (Throwable var23) {
                  var24.addSuppressed(var23);
               }
            }

            throw var24;
         }

         if (mappedView != null) {
            mappedView.close();
         }

         cir.setReturnValue(vector4f);
      }
   }
}