package sl.selene.util.render.postfx;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import sl.selene.util.render.backends.gl.GlState;
import sl.selene.util.render.backends.gl.ShaderProgram;
import sl.selene.util.render.core.RenderFrameMetrics;

@Environment(EnvType.CLIENT)
public final class DownsampleBlur {
   private static final int GL_COLOR_ATTACHMENT0 = 36064;
   private static final IntBuffer DRAW_BUFFER_COLOR0 = BufferUtils.createIntBuffer(1);

   static {
      DRAW_BUFFER_COLOR0.put(GL_COLOR_ATTACHMENT0).flip();
   }

   private final ShaderProgram upsampleProgram;
   private final int intermediateInternalFormat;
   private final int intermediatePixelType;
   private final int upSamplerLoc;
   private final int upTexelSizeLoc;
   private final int upOffsetLoc;
   private int quadVao;
   private int quadVbo;
   private int lastPassCount = 0;
   private final DownsampleBlur.LevelTarget fullResolutionTarget = new DownsampleBlur.LevelTarget();
   private final DownsampleBlur.LevelTarget smallTempTarget = new DownsampleBlur.LevelTarget();

   public float minimumRadius() {
      return 0.5F;
   }

   public float smallKernelThreshold() {
      return 30.0F;
   }

   public DownsampleBlur() {

      this(32856, 5121);
   }

   public DownsampleBlur(int intermediateInternalFormat, int intermediatePixelType) {
      if (intermediateInternalFormat == 0) {
         throw new IllegalArgumentException("intermediateInternalFormat must be a valid OpenGL format constant");
      } else if (intermediatePixelType == 0) {
         throw new IllegalArgumentException("intermediatePixelType must be a valid OpenGL pixel type constant");
      } else {
         this.upsampleProgram = ShaderProgram.fromResources("assets/selene/shaders/blur/blur_fullscreen.vert", "assets/selene/shaders/blur/blur_upsample.frag");
         this.intermediateInternalFormat = intermediateInternalFormat;
         this.intermediatePixelType = intermediatePixelType;
         this.upSamplerLoc = this.upsampleProgram.getUniformLocation("uSource");
         this.upTexelSizeLoc = this.upsampleProgram.getUniformLocation("uTexelSize");
         this.upOffsetLoc = this.upsampleProgram.getUniformLocation("uOffset");

         this.quadVao = GL30.glGenVertexArrays();
         this.quadVbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.quadVao);
         GL15.glBindBuffer(34962, this.quadVbo);
         float[] vertices = new float[]{-1.0F, -1.0F, 0.0F, 0.0F, 1.0F, -1.0F, 1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};
         GL15.glBufferData(34962, vertices, 35044);
         int stride = 16;
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 2, 5126, false, stride, 0L);
         GL20.glEnableVertexAttribArray(1);
         GL20.glVertexAttribPointer(1, 2, 5126, false, stride, 8L);
         GL30.glBindVertexArray(0);
         GL15.glBindBuffer(34962, 0);
      }
   }

   public void destroy() {
      this.destroyLevel(this.fullResolutionTarget);
      this.destroyLevel(this.smallTempTarget);
      if (this.quadVao != 0) {
         GL30.glDeleteVertexArrays(this.quadVao);
         this.quadVao = 0;
      }

      if (this.quadVbo != 0) {
         GL15.glDeleteBuffers(this.quadVbo);
         this.quadVbo = 0;
      }

      this.upsampleProgram.delete();
   }

   public int blurFromColorTexture(int sourceTexture, int width, int height, float radiusPx) {
      return this.blurFromColorTexture(sourceTexture, width, height, radiusPx, true);
   }

   public int blurFromColorTexture(int sourceTexture, int width, int height, float radiusPx, boolean preserveState) {
      if (sourceTexture != 0 && width > 0 && height > 0) {
         float effectiveRadius = Math.max(radiusPx, 0.5F);

         int passCount = this.determinePassCount(effectiveRadius, width, height);
         if (passCount <= 0) {
            return sourceTexture;
         }

         float[] offsets = this.buildOffsets(passCount, effectiveRadius);
         this.ensureLevel(this.fullResolutionTarget, width, height);
         this.ensureLevel(this.smallTempTarget, width, height);
         this.lastPassCount = passCount;

         GlState.Snapshot snapshot = preserveState ? GlState.push() : null;

         int var12;
         try (TextureUnitGuard unit0 = TextureUnitGuard.capture(0, 3553)) {
            GL11.glDisable(3089);
            GL11.glDisable(2929);
            GL11.glDisable(2884);
            GL11.glDisable(3042);
            GlState.disableFramebufferSrgb();
            GL13.glActiveTexture(33984);
            GL30.glBindVertexArray(this.quadVao);
            this.runDownsampleBlur(sourceTexture, width, height, passCount, offsets);
            var12 = this.fullResolutionTarget.texture;
         } finally {
            GL30.glBindVertexArray(0);
            GL20.glUseProgram(0);
            GL30.glBindFramebuffer(36160, 0);
            GL13.glActiveTexture(33984);
            GL11.glBindTexture(3553, 0);
            if (preserveState && snapshot != null) {
               GlState.pop(snapshot);
            }
         }

         return var12;
      } else {
         return 0;
      }
   }

   private void runDownsampleBlur(int sourceTexture, int width, int height, int passCount, float[] offsets) {
      if (offsets == null || offsets.length != passCount) {
         throw new IllegalArgumentException("offsets length must match passCount");
      }

      float texelX = 1.0F / Math.max(1, width);
      float texelY = 1.0F / Math.max(1, height);
      this.upsampleProgram.use();
      if (this.upSamplerLoc >= 0) {
         GL20.glUniform1i(this.upSamplerLoc, 0);
      }
      if (this.upTexelSizeLoc >= 0) {
         GL20.glUniform2f(this.upTexelSizeLoc, texelX, texelY);
      }

      boolean startWithSmall = (passCount % 2 == 0);
      int currentTexture = sourceTexture;
      for (int i = 0; i < passCount; i++) {
         boolean last = (i == passCount - 1);
         boolean wantSmall = !last && (((i & 1) == 0) == startWithSmall);
         DownsampleBlur.LevelTarget target = wantSmall ? this.smallTempTarget : this.fullResolutionTarget;
         if (this.upOffsetLoc >= 0) {
            GL20.glUniform1f(this.upOffsetLoc, offsets[i]);
         }
         this.bindTarget(target);
         GL11.glBindTexture(3553, currentTexture);
         this.drawQuad();
         currentTexture = target.texture;
      }
   }

   private void drawQuad() {
      RenderFrameMetrics.getInstance().recordDrawCall(2);
      GL11.glDrawArrays(5, 0, 4);
   }

   private void bindTarget(DownsampleBlur.LevelTarget target) {
      GL30.glBindFramebuffer(36160, target.fbo);
      GL11.glViewport(0, 0, target.width, target.height);

      GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
   }

   private void ensureLevel(DownsampleBlur.LevelTarget target, int width, int height) {
      if (target.texture != 0 && (target.width != width || target.height != height)) {
         GL11.glDeleteTextures(target.texture);
         GL30.glDeleteFramebuffers(target.fbo);
         target.texture = 0;
         target.fbo = 0;
      }

      if (target.texture == 0) {
         target.texture = this.createRenderTexture(width, height);
         target.fbo = this.createFramebuffer(target.texture);
      }

      target.width = width;
      target.height = height;
   }

   private void destroyLevel(DownsampleBlur.LevelTarget target) {
      if (target != null) {
         if (target.texture != 0) {
            GL11.glDeleteTextures(target.texture);
            target.texture = 0;
         }

         if (target.fbo != 0) {
            GL30.glDeleteFramebuffers(target.fbo);
            target.fbo = 0;
         }

         target.width = 0;
         target.height = 0;
      }
   }

   private int createRenderTexture(int width, int height) {
      int tex = GL11.glGenTextures();
      GL11.glBindTexture(3553, tex);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexImage2D(3553, 0, this.intermediateInternalFormat, width, height, 0, 6408, this.intermediatePixelType, (ByteBuffer)null);
      GL11.glBindTexture(3553, 0);
      return tex;
   }

   private int createFramebuffer(int texture) {
      int fbo = GL30.glGenFramebuffers();
      GL30.glBindFramebuffer(36160, fbo);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, texture, 0);
      int status = GL30.glCheckFramebufferStatus(36160);
      GL30.glBindFramebuffer(36160, 0);
      if (status != 36053) {
         GL30.glDeleteFramebuffers(fbo);
         GL11.glDeleteTextures(texture);
         throw new IllegalStateException("Blur framebuffer incomplete: status=" + status);
      } else {
         return fbo;
      }
   }

   private int determinePassCount(float radiusPx, int width, int height) {
      int available = 0;
      int w = width;
      int h = height;

      while (available < 6 && (w > 1 || h > 1)) {
         w = Math.max(1, w / 2);
         h = Math.max(1, h / 2);
         available++;
         if (w == 1 && h == 1) {
            break;
         }
      }

      if (available == 0) {
         available = 1;
      }

      int desired = Math.max(1, (int)Math.ceil(Math.sqrt(radiusPx / 2.0F)));
      return Math.min(available, desired);
   }

   private float[] buildOffsets(int passCount, float radiusPx) {
      float[] offsets = new float[passCount];

      float offset = Math.max(0.5F, radiusPx / (float) passCount);
      for (int i = 0; i < passCount; i++) {
         offsets[i] = offset;
      }

      return offsets;
   }

   @Environment(EnvType.CLIENT)
   private static final class LevelTarget {
      int fbo;
      int texture;
      int width;
      int height;
   }
}