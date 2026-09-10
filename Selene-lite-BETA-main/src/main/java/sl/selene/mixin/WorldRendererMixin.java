package sl.selene.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.event.EventManager;
import sl.selene.event.render.WorldRenderEvent;
import sl.selene.util.render.capture.EntityFramebufferCaptureManager;

@Environment(EnvType.CLIENT)
@Mixin({ WorldRenderer.class })
public class WorldRendererMixin {
   @Inject(method = { "render" }, at = { @At("HEAD") })
   private void beginEntityCapture(
         ObjectAllocator allocator,
         RenderTickCounter tickCounter,
         boolean renderBlockOutline,
         Camera camera,
         Matrix4f positionMatrix,
         Matrix4f basicProjectionMatrix,
         Matrix4f projectionMatrix,
         GpuBufferSlice fog,
         Vector4f fogColor,
         boolean shouldRenderSky,
         CallbackInfo ci) {
      EntityFramebufferCaptureManager captureManager = EntityFramebufferCaptureManager.getInstance();
      if (captureManager.isEnabled()) {
         captureManager.beginFrame((WorldRenderer) (Object) this, tickCounter, camera);
      }
   }

   @Inject(method = { "render" }, at = { @At("RETURN") })
   private void publishWorldRenderEvent(
         ObjectAllocator allocator,
         RenderTickCounter tickCounter,
         boolean renderBlockOutline,
         Camera camera,
         Matrix4f positionMatrix,
         Matrix4f basicProjectionMatrix,
         Matrix4f projectionMatrix,
         GpuBufferSlice fog,
         Vector4f fogColor,
         boolean shouldRenderSky,
         CallbackInfo ci) {
      EntityFramebufferCaptureManager captureManager = EntityFramebufferCaptureManager.getInstance();
      if (captureManager.isEnabled()) {
         captureManager.endFrame();
      }

      if (!EventManager.hasListeners(WorldRenderEvent.class)) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null || client.player == null) {
         return;
      }

      GameRenderer gameRenderer = client.gameRenderer;
      if (camera == null || gameRenderer == null) {
         return;
      }

      sl.selene.util.render.world.WorldRenderer worldRenderer = null;

      try {
         worldRenderer = sl.selene.util.render.world.WorldRenderer.begin(
               client, tickCounter, camera, positionMatrix, projectionMatrix);

         try {
            EventManager.call(
                  new WorldRenderEvent(client, gameRenderer, worldRenderer, worldRenderer.tickDelta()));
         } finally {
            if (worldRenderer != null) {
               try {
                  worldRenderer.flush();
               } finally {
                  worldRenderer.close();
               }
            }
         }
      } catch (RuntimeException e) {
         System.err.println("[Selene] World render event failed: " + e.getMessage());
         e.printStackTrace();
      }
   }
}