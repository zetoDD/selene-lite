package sl.selene.util.render.capture;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.entity.Entity;
import net.minecraft.world.LightType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class EntityFramebufferCaptureManager {
   private static final Logger LOGGER = LogManager.getLogger("EntityFramebufferCapture");
   private static final EntityFramebufferCaptureManager INSTANCE = new EntityFramebufferCaptureManager();
   private volatile SimpleFramebuffer captureFramebuffer;
   private volatile boolean enabled;
   private volatile boolean frameAvailable;
   private volatile boolean executingCapturePass;
   private volatile boolean captureFrameActive;
   private volatile int framebufferWidth = -1;
   private volatile int framebufferHeight = -1;
   private EntityFramebufferCaptureManager.FrameResources frameResources;

   private EntityFramebufferCaptureManager() {
   }

   public static EntityFramebufferCaptureManager getInstance() {
      return INSTANCE;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (!enabled) {
            this.frameAvailable = false;
            this.executingCapturePass = false;
            this.captureFrameActive = false;
            this.framebufferWidth = -1;
            this.framebufferHeight = -1;
            this.resetFrameResources();
            this.deleteFramebuffer();
         }
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean hasCapturedFrame() {
      return this.enabled && this.frameAvailable && extractColorTextureId(this.captureFramebuffer) > 0;
   }

   public boolean isExecutingCapturePass() {
      return this.executingCapturePass;
   }

   public void beginFrame(WorldRenderer worldRenderer, RenderTickCounter tickCounter, Camera camera) {
      if (!this.enabled) {
         this.resetFrameState();
      } else {
         Objects.requireNonNull(worldRenderer, "worldRenderer");
         Objects.requireNonNull(tickCounter, "tickCounter");
         MinecraftClient client = MinecraftClient.getInstance();
         if (client == null || client.world == null || client.gameRenderer == null) {
            this.resetFrameState();
         } else if (!client.gameRenderer.isRenderingPanorama() && camera != null) {
            Framebuffer mainFramebuffer = client.getFramebuffer();
            if (mainFramebuffer == null) {
               this.resetFrameState();
            } else {
               Window window = client.getWindow();
               int width = window != null ? window.getFramebufferWidth() : mainFramebuffer.textureWidth;
               int height = window != null ? window.getFramebufferHeight() : mainFramebuffer.textureHeight;
               if (width <= 0 || height <= 0) {
                  this.resetFrameState();
               } else if (!this.ensureCaptureFramebuffer(width, height)) {
                  this.resetFrameState();
               } else {
                  SimpleFramebuffer framebuffer = this.captureFramebuffer;
                  if (framebuffer == null) {
                     this.resetFrameState();
                  } else {
                     GpuTextureView colorView = framebuffer.getColorAttachmentView();
                     if (colorView != null && !colorView.isClosed()) {
                        GpuTextureView depthView = framebuffer.getDepthAttachmentView();
                        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                        GpuTexture colorTexture = colorView.texture();
                        if (depthView != null && !depthView.isClosed()) {
                           encoder.clearColorAndDepthTextures(colorTexture, 0, depthView.texture(), 1.0);
                        } else {
                           encoder.clearColorTexture(colorTexture, 0);
                        }

                        this.resetFrameResources();

                        try {
                           BufferAllocator allocator = new BufferAllocator(1048576);
                           this.frameResources = new EntityFramebufferCaptureManager.FrameResources(allocator);
                        } catch (RuntimeException var15) {
                           LOGGER.warn("Failed to allocate capture resources", var15);
                           this.resetFrameState();
                           return;
                        }

                        this.frameAvailable = false;
                        this.captureFrameActive = true;
                     } else {
                        this.resetFrameState();
                     }
                  }
               }
            }
         } else {
            this.resetFrameState();
         }
      }
   }

   public void endFrame() {
      EntityFramebufferCaptureManager.FrameResources resources = this.frameResources;

      try {
         if (resources != null) {
            try {
               resources.flush();
            } finally {
               resources.close();
            }
         }
      } catch (RuntimeException var11) {
         LOGGER.warn("Failed to finalize capture frame", var11);
         this.frameAvailable = false;
      } finally {
         this.frameResources = null;
         this.captureFrameActive = false;
      }

      this.frameAvailable = this.frameAvailable && extractColorTextureId(this.captureFramebuffer) > 0;
   }

   public void captureEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices) {
      if (this.enabled && this.captureFrameActive && !this.executingCapturePass) {
         Objects.requireNonNull(entity, "entity");
         Objects.requireNonNull(matrices, "matrices");
         EntityFramebufferCaptureManager.FrameResources resources = this.frameResources;
         SimpleFramebuffer framebuffer = this.captureFramebuffer;
         if (resources != null && framebuffer != null) {
            GpuTextureView colorView = framebuffer.getColorAttachmentView();
            if (colorView != null && !colorView.isClosed()) {
               GpuTextureView depthView = framebuffer.getDepthAttachmentView();
               MinecraftClient client = MinecraftClient.getInstance();
               if (client != null && client.world != null) {
                  Object dispatcher = client.getEntityRenderDispatcher();
                  if (dispatcher != null) {
                     MatrixStack captureMatrices = resources.borrowMatrixStack(matrices);
                     if (captureMatrices != null) {
                        Vec3d interpolated = entity.getLerpedPos(tickDelta);
                        double renderX = interpolated.x - cameraX;
                        double renderY = interpolated.y - cameraY;
                        double renderZ = interpolated.z - cameraZ;
                        BlockPos samplePos = BlockPos.ofFloored(interpolated);
                        int blockLight = client.world.getLightLevel(LightType.BLOCK, samplePos);
                        int skyLight = client.world.getLightLevel(LightType.SKY, samplePos);
                        int light = LightmapTextureManager.pack(skyLight, blockLight);
                        GpuTextureView previousColorOverride = RenderSystem.outputColorTextureOverride;
                        GpuTextureView previousDepthOverride = RenderSystem.outputDepthTextureOverride;
                        RenderSystem.outputColorTextureOverride = colorView;
                        RenderSystem.outputDepthTextureOverride = depthView;
                        Object dispatcherAccessor = dispatcher;
                        boolean restoreRenderShadows = false;
                        boolean previousRenderShadows = false;

                        this.executingCapturePass = true;

                        try {
                           try {
                              resources.onEntityRendered();
                              this.frameAvailable = true;
                           } finally {
                              resources.flush();
                           }
                        } catch (RuntimeException var42) {
                           LOGGER.warn("Failed to visuals entity {} into capture framebuffer", entity.getName().getString(), var42);
                           this.frameAvailable = false;
                        } finally {
                           this.executingCapturePass = false;

                           RenderSystem.outputColorTextureOverride = previousColorOverride;
                           RenderSystem.outputDepthTextureOverride = previousDepthOverride;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean isCaptureFrameActive() {
      return this.enabled && this.captureFrameActive && this.captureFramebuffer != null;
   }

   public void renderFramebuffer(Renderer2D renderer, int viewportWidth, int viewportHeight) {
      if (renderer != null && viewportWidth > 0 && viewportHeight > 0) {
         if (this.hasCapturedFrame()) {
            int textureId = extractColorTextureId(this.captureFramebuffer);
            if (textureId > 0) {
               renderer.drawPremultipliedRgbaTexture(textureId, 0.0F, 0.0F, viewportWidth, viewportHeight);
            }
         }
      }
   }

   private boolean ensureCaptureFramebuffer(int width, int height) {
      SimpleFramebuffer framebuffer = this.captureFramebuffer;
      if (framebuffer == null) {
         try {
            framebuffer = new SimpleFramebuffer("night_entity_capture", width, height, true);
            this.captureFramebuffer = framebuffer;
            this.framebufferWidth = width;
            this.framebufferHeight = height;
         } catch (RuntimeException var6) {
            LOGGER.warn("Failed to create capture framebuffer {}x{}", width, height, var6);
            this.captureFramebuffer = null;
            this.framebufferWidth = -1;
            this.framebufferHeight = -1;
            return false;
         }
      }

      if (this.framebufferWidth != width || this.framebufferHeight != height) {
         try {
            framebuffer.resize(width, height);
            this.framebufferWidth = width;
            this.framebufferHeight = height;
         } catch (RuntimeException var5) {
            LOGGER.warn("Failed to resize capture framebuffer to {}x{}", width, height, var5);
            this.deleteFramebuffer();
            this.framebufferWidth = -1;
            this.framebufferHeight = -1;
            return false;
         }
      }

      return hasColorAttachment(framebuffer);
   }

   private void deleteFramebuffer() {
      Framebuffer framebuffer = this.captureFramebuffer;
      if (framebuffer != null) {
         try {
            framebuffer.delete();
         } catch (RuntimeException var3) {
            LOGGER.warn("Failed to delete capture framebuffer", var3);
         }

         this.captureFramebuffer = null;
      }
   }

   private void resetFrameState() {
      this.frameAvailable = false;
      this.captureFrameActive = false;
      this.resetFrameResources();
   }

   private void resetFrameResources() {
      EntityFramebufferCaptureManager.FrameResources resources = this.frameResources;
      if (resources != null) {
         try {
            try {
               resources.flush();
            } catch (RuntimeException var3) {
               LOGGER.warn("Failed to flush capture resources during reset", var3);
            }

            resources.close();
         } catch (RuntimeException var4) {
            LOGGER.warn("Failed to release capture resources", var4);
         }

         this.frameResources = null;
      }
   }

   private static boolean hasColorAttachment(Framebuffer framebuffer) {
      if (framebuffer == null) {
         return false;
      } else {
         return framebuffer.getColorAttachment() instanceof GlTexture glTexture ? glTexture.getGlId() > 0 : false;
      }
   }

   private static int extractColorTextureId(Framebuffer framebuffer) {
      if (framebuffer == null) {
         return 0;
      } else {
         return framebuffer.getColorAttachment() instanceof GlTexture glTexture ? glTexture.getGlId() : 0;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class FrameResources implements AutoCloseable {
      private final BufferAllocator allocator;
      private final Immediate consumers;
      private boolean flushed;

      private FrameResources(BufferAllocator allocator) {
         this.allocator = allocator;
         this.consumers = VertexConsumerProvider.immediate(allocator);
      }

      private Immediate consumers() {
         return this.consumers;
      }

      private MatrixStack borrowMatrixStack(MatrixStack source) {
         if (source == null) {
            return null;
         } else {
            MatrixStack copy = new MatrixStack();
            Entry sourceEntry = source.peek();
            Entry copyEntry = copy.peek();
            copyEntry.getPositionMatrix().set(sourceEntry.getPositionMatrix());
            copyEntry.getNormalMatrix().set(sourceEntry.getNormalMatrix());
            return copy;
         }
      }

      private void onEntityRendered() {
         this.flushed = false;
      }

      private void flush() {
         if (!this.flushed) {
            this.consumers.draw();
            this.flushed = true;
         }
      }

      @Override
      public void close() {
         this.allocator.close();
      }
   }
}