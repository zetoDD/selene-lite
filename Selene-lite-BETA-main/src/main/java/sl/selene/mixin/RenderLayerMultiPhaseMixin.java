package sl.selene.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sl.selene.util.render.world.MoreMultiPhase;

@Environment(EnvType.CLIENT)
@Mixin(RenderLayer.class)
public abstract class RenderLayerMultiPhaseMixin implements MoreMultiPhase {
   @Unique
   private Consumer<RenderPass> renderPassSetup;

   @Override
   public Object withRenderPassSetup(Consumer<RenderPass> renderPassSetup) {
      this.renderPassSetup = renderPassSetup;
      return this;
   }

   @Inject(method = { "draw" }, at = {
         @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V") }, locals = LocalCapture.CAPTURE_FAILHARD)
   private void applyRenderPassSetup(
         BuiltBuffer buffer,
         CallbackInfo ci,
         org.joml.Matrix4fStack matrix4fStack,
         java.util.function.Consumer consumer,
         GpuBufferSlice gpuBufferSlice,
         Map<String, RenderSetup.Texture> map,
         BuiltBuffer capturedBuffer,
         GpuBuffer sourceBuffer,
         GpuBuffer indexBuffer,
         IndexType indexType,
         Framebuffer framebuffer,
         GpuTextureView colorTarget,
         GpuTextureView depthTarget,
         RenderPass renderPass) {
      if (this.renderPassSetup != null) {
         this.renderPassSetup.accept(renderPass);
      }
   }
}