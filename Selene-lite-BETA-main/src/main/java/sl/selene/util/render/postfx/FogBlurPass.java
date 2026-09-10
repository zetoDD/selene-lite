package sl.selene.util.render.postfx;

import java.nio.FloatBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import sl.selene.util.render.backends.gl.GlState;
import sl.selene.util.render.backends.gl.ShaderProgram;

@Environment(EnvType.CLIENT)
public final class FogBlurPass {
   private final DownsampleBlur blur = new DownsampleBlur(32856, 5121);
   private final RenderTarget compositeTarget = new RenderTarget();
   private final RenderTarget scaledColorTarget = new RenderTarget();
   private final FullScreenQuad quad = new FullScreenQuad();
   private final ShaderProgram program = ShaderProgram.fromResources(
      "assets/selene/shaders/blur/blur_fullscreen.vert", "assets/selene/shaders/postfx/fog_blur.frag"
   );
   private final ShaderProgram copyProgram = ShaderProgram.fromResources(
      "assets/selene/shaders/blur/blur_fullscreen.vert", "assets/selene/shaders/postfx/fog_copy.frag"
   );
   private final int uSourceLoc = this.program.getUniformLocation("uSource");
   private final int uBlurredLoc = this.program.getUniformLocation("uBlurred");
   private final int uDepthLoc = this.program.getUniformLocation("uDepth");
   private final int uNearFarLoc = this.program.getUniformLocation("uNearFar");
   private final int uInverseProjectionLoc = this.program.getUniformLocation("uInverseProjection");
   private final int uInverseProjectionValidLoc = this.program.getUniformLocation("uInverseProjectionValid");
   private final int uThresholdLoc = this.program.getUniformLocation("uThreshold");
   private final int uTintColorLoc = this.program.getUniformLocation("uTintColor");
   private final int uTintStrengthLoc = this.program.getUniformLocation("uTintStrength");
   private final int uBlurredTexelSizeLoc = this.program.getUniformLocation("uBlurredTexelSize");
   private final int uBlurResolutionScaleLoc = this.program.getUniformLocation("uBlurResolutionScale");
   private final int uCopySourceLoc = this.copyProgram.getUniformLocation("uSource");
   private boolean destroyed;

   public FogBlurPass.Result render(
      FogBlurPass.TextureBinding colorBinding,
      FogBlurPass.TextureBinding depthBinding,
      int width,
      int height,
      float blurRadius,
      Matrix4f inverseProjection,
      boolean inverseProjectionValid,
      float nearPlane,
      float farPlane,
      float minThreshold,
      float maxThreshold,
      float tintR,
      float tintG,
      float tintB,
      float tintStrength
   ) {
      if (colorBinding == null || colorBinding.textureId() <= 0 || width <= 0 || height <= 0) {
         return new FogBlurPass.Result(0, 0, 0, 0);
      } else if (this.destroyed) {
         return new FogBlurPass.Result(colorBinding.textureId(), 0, width, height);
      } else if (!colorBinding.isTexture2D()) {
         return new FogBlurPass.Result(0, 0, 0, 0);
      } else if (depthBinding != null && depthBinding.isTexture2D()) {
         int colorTexture = colorBinding.textureId();
         int depthTexture = depthBinding.textureId();
         float sanitizedNear = Math.max(1.0E-4F, nearPlane);
         float sanitizedFar = Math.max(sanitizedNear + 0.001F, farPlane);
         float thresholdMin = Math.min(minThreshold, maxThreshold);
         float thresholdMax = Math.max(minThreshold, maxThreshold);
         float sanitizedTintStrength = Math.max(0.0F, Math.min(1.0F, tintStrength));
         float sanitizedTintR = Math.max(0.0F, Math.min(1.0F, tintR));
         float sanitizedTintG = Math.max(0.0F, Math.min(1.0F, tintG));
         float sanitizedTintB = Math.max(0.0F, Math.min(1.0F, tintB));
         boolean sanitizedInverseProjectionValid = inverseProjectionValid && inverseProjection != null;
         Matrix4f projectionMatrix = sanitizedInverseProjectionValid ? new Matrix4f(inverseProjection) : new Matrix4f();
         float sanitizedBlurRadius = Math.max(0.0F, blurRadius);
         float resolutionScale = chooseResolutionScale(sanitizedBlurRadius);
         int downscaledWidth = Math.max(1, Math.round(width * resolutionScale));
         int downscaledHeight = Math.max(1, Math.round(height * resolutionScale));
         this.quad.ensure();
         GlState.Snapshot snapshot = GlState.push();

         FogBlurPass.Result unit0;
         try {
            GL11.glDisable(3089);
            GL11.glDisable(2884);
            GL11.glDisable(3042);
            GL11.glDisable(2929);
            GlState.disableFramebufferSrgb();
            int blurSourceTexture = colorTexture;
            int blurSourceWidth = width;
            int blurSourceHeight = height;
            boolean downscalePass = resolutionScale < 1.0F && (downscaledWidth != width || downscaledHeight != height);
            float blurResolutionScale = downscalePass ? resolutionScale : 1.0F;
            if (downscalePass) {
               this.scaledColorTarget.ensure(downscaledWidth, downscaledHeight);
               GL30.glBindFramebuffer(36160, this.scaledColorTarget.fbo);
               GL11.glViewport(0, 0, downscaledWidth, downscaledHeight);
               GL11.glDrawBuffer(36064);
               this.copyProgram.use();
               if (this.uCopySourceLoc >= 0) {
                  GL20.glUniform1i(this.uCopySourceLoc, 0);
               }

               try (TextureUnitGuard unit0x = TextureUnitGuard.capture(0, 3553, colorBinding.target())) {
                  GL13.glActiveTexture(33984);
                  GL11.glBindTexture(3553, colorTexture);
                  this.quad.bindAndDraw();
               }

               GL20.glUseProgram(0);
               GL30.glBindFramebuffer(36160, 0);
               GL13.glActiveTexture(33984);
               GL11.glBindTexture(3553, 0);
               blurSourceTexture = this.scaledColorTarget.colorTex;
               blurSourceWidth = downscaledWidth;
               blurSourceHeight = downscaledHeight;
            }

            float scaledBlurRadius = sanitizedBlurRadius * blurResolutionScale;
            int blurredTexture = this.blur.blurFromColorTexture(blurSourceTexture, blurSourceWidth, blurSourceHeight, scaledBlurRadius, false);
            if (blurredTexture != 0) {
               this.compositeTarget.ensure(width, height);
               GL11.glDisable(3089);
               GL11.glDisable(2884);
               GL11.glDisable(3042);
               GL11.glDisable(2929);
               GlState.disableFramebufferSrgb();
               GL30.glBindFramebuffer(36160, this.compositeTarget.fbo);
               GL11.glViewport(0, 0, width, height);
               GL11.glDrawBuffer(36064);
               this.program.use();
               if (this.uSourceLoc >= 0) {
                  GL20.glUniform1i(this.uSourceLoc, 0);
               }

               if (this.uBlurredLoc >= 0) {
                  GL20.glUniform1i(this.uBlurredLoc, 1);
               }

               if (this.uDepthLoc >= 0) {
                  GL20.glUniform1i(this.uDepthLoc, 2);
               }

               if (this.uNearFarLoc >= 0) {
                  GL20.glUniform2f(this.uNearFarLoc, sanitizedNear, sanitizedFar);
               }

               if (this.uInverseProjectionValidLoc >= 0) {
                  GL20.glUniform1i(this.uInverseProjectionValidLoc, sanitizedInverseProjectionValid ? 1 : 0);
               }

               if (this.uInverseProjectionLoc >= 0 && sanitizedInverseProjectionValid) {
                  MemoryStack stack = MemoryStack.stackPush();

                  try {
                     FloatBuffer buffer = stack.mallocFloat(16);
                     projectionMatrix.get(buffer);
                     GL20.glUniformMatrix4fv(this.uInverseProjectionLoc, false, buffer);
                  } catch (Throwable var63) {
                     if (stack != null) {
                        try {
                           stack.close();
                        } catch (Throwable var60) {
                           var63.addSuppressed(var60);
                        }
                     }

                     throw var63;
                  }

                  if (stack != null) {
                     stack.close();
                  }
               }

               if (this.uThresholdLoc >= 0) {
                  GL20.glUniform2f(this.uThresholdLoc, thresholdMin, thresholdMax);
               }

               if (this.uTintColorLoc >= 0) {
                  GL20.glUniform3f(this.uTintColorLoc, sanitizedTintR, sanitizedTintG, sanitizedTintB);
               }

               if (this.uTintStrengthLoc >= 0) {
                  GL20.glUniform1f(this.uTintStrengthLoc, sanitizedTintStrength);
               }

               if (this.uBlurredTexelSizeLoc >= 0) {
                  GL20.glUniform2f(this.uBlurredTexelSizeLoc, 1.0F / Math.max(1, blurSourceWidth), 1.0F / Math.max(1, blurSourceHeight));
               }

               if (this.uBlurResolutionScaleLoc >= 0) {
                  GL20.glUniform1f(this.uBlurResolutionScaleLoc, blurResolutionScale);
               }

               try (
                  TextureUnitGuard unit0x = TextureUnitGuard.capture(0, 3553, colorBinding.target());
                  TextureUnitGuard unit1 = TextureUnitGuard.capture(1, 3553);
                  TextureUnitGuard unit2 = TextureUnitGuard.capture(2, 3553, depthBinding.target());
               ) {
                  GL13.glActiveTexture(33984);
                  GL11.glBindTexture(colorBinding.target(), colorTexture);
                  GL13.glActiveTexture(33985);
                  GL11.glBindTexture(3553, blurredTexture);
                  GL13.glActiveTexture(33986);
                  GL11.glBindTexture(depthBinding.target(), depthTexture);
                  this.quad.bindAndDraw();
               }

               return new FogBlurPass.Result(this.compositeTarget.colorTex, this.compositeTarget.fbo, width, height);
            }

            unit0 = new FogBlurPass.Result(colorTexture, 0, width, height);
         } finally {
            GL20.glUseProgram(0);
            GL30.glBindFramebuffer(36160, 0);
            GL13.glActiveTexture(33984);
            GL11.glBindTexture(3553, 0);
            GlState.pop(snapshot);
         }

         return unit0;
      } else {
         return new FogBlurPass.Result(colorBinding.textureId(), 0, width, height);
      }
   }

   public void destroy() {
      if (!this.destroyed) {
         this.destroyed = true;
         this.blur.destroy();
         this.compositeTarget.destroy();
         this.scaledColorTarget.destroy();
         this.quad.destroy();
         this.program.delete();
         this.copyProgram.delete();
      }
   }

   private static float chooseResolutionScale(float blurRadius) {
      float sanitized = Math.max(0.0F, blurRadius);
      if (sanitized >= 12.0F) {
         return 1.0F;
      } else {
         return sanitized >= 4.0F ? 0.5F : 0.25F;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Result(int colorTexture, int framebuffer, int width, int height) {
   }

   @Environment(EnvType.CLIENT)
   public record TextureBinding(int textureId, int target) {
      public boolean isValid() {
         return this.textureId > 0 && this.target != 0;
      }

      public boolean isTexture2D() {
         return this.textureId > 0 && this.target == 3553;
      }
   }
}