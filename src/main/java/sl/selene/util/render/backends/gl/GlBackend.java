package sl.selene.util.render.backends.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.other.PlatformUtil;
import sl.selene.util.render.core.RenderFrameMetrics;
import sl.selene.util.render.backends.RenderBackend;
import sl.selene.util.render.backends.ShapeInstanceBatch;
import sl.selene.util.render.postfx.DepthRenderTarget;
import sl.selene.util.render.postfx.DownsampleBlur;

@Environment(EnvType.CLIENT)
public final class GlBackend implements RenderBackend {

   private final boolean ssboSupported;
   private final boolean debugOutputSupported;
   private final ShaderProgram shapeProgram;
   private final int vaoDraw;
   private final int ssbo;
   private final int instanceVbo;
   private final ShapeInstanceBatch batch = new ShapeInstanceBatch();
   private GlState.Snapshot snapshot;
   private int viewportWidth;
   private int viewportHeight;
   private int uViewportLoc = -1;
   private boolean samplersInitialized = false;
   private int captureTex = 0;
   private int captureW = 0;
   private int captureH = 0;
   private int captureFbo = 0;
   private int downscaledCaptureTex = 0;
   private int downscaledCaptureW = 0;
   private int downscaledCaptureH = 0;
   private int downscaledCaptureFbo = 0;
   private float blurCaptureScaleX = 0.5F;
   private float blurCaptureScaleY = 0.5F;
   private int regionCaptureTex = 0;
   private int regionCaptureW = 0;
   private int regionCaptureH = 0;
   private int regionCaptureFbo = 0;
   private final DepthRenderTarget fullFrameTarget = new DepthRenderTarget();
   private int fullFrameReadFbo = 0;
   private static final java.nio.IntBuffer DRAW_BUFFER_COLOR0 = org.lwjgl.BufferUtils.createIntBuffer(1);

   static {
      DRAW_BUFFER_COLOR0.put(36064).flip();
   }
   private int fullscreenQuadVao = 0;
   private int fullscreenQuadVbo = 0;
   private ShaderProgram fullscreenProgram;
   private int fullscreenSamplerLoc = -1;
   private ShaderProgram glassProgram;
   private ShaderProgram glassOutlineProgram;
   private int glassQuadVao;
   private int glassQuadVbo;
   private int glassInstanceVbo;
   private int uGlassViewportLoc = -1;
   private int uGlassBlurScaleLoc = -1;
   private int uGlassBlurOffsetLoc = -1;
   private int uGlassSamplerLoc = -1;
   private int uGlassScissorLoc = -1;
   private int uGlassScissorEnabledLoc = -1;
   private int uGlassOutlineViewportLoc = -1;
   private int uGlassOutlineBlurScaleLoc = -1;
   private int uGlassOutlineBlurOffsetLoc = -1;
   private int uGlassOutlineSamplerLoc = -1;
   private int uGlassOutlineScissorLoc = -1;
   private int uGlassOutlineScissorEnabledLoc = -1;
   private ByteBuffer glassInstanceBuffer;
   private GLDebugMessageCallback debugCallback;
   private final DownsampleBlur screenBlur = new DownsampleBlur(32856, 5121);
   private final DownsampleBlur regionBlur = new DownsampleBlur(32856, 5121);
   private int preparedBlurTex = 0;
   private int preparedBlurW = 0;
   private int preparedBlurH = 0;
   private float preparedBlurScaleX = 1.0F;
   private float preparedBlurScaleY = 1.0F;
   private int whitePixelTex = 0;
   private int darkPixelTex = 0;
   private int transparentPixelTex = 0;
   private static final java.nio.ByteBuffer PIXEL_BUF = org.lwjgl.BufferUtils.createByteBuffer(4);
   private int preparedRegionBlurTex = 0;
   private int preparedRegionBlurX = 0;
   private int preparedRegionBlurY = 0;
   private int preparedRegionBlurW = 0;
   private int preparedRegionBlurH = 0;
   private boolean destroyed = false;

   public GlBackend() {
      GLCapabilities caps = GL.getCapabilities();
      boolean isMac = PlatformUtil.isMac();
      this.ssboSupported = !isMac && (caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object);
      this.debugOutputSupported = !isMac && (caps.OpenGL43 || caps.GL_KHR_debug);
      boolean instancedArraysSupported = caps.OpenGL33 || caps.GL_ARB_instanced_arrays;
      boolean drawInstancedSupported = caps.OpenGL31 || caps.GL_ARB_draw_instanced;
      if (this.ssboSupported || instancedArraysSupported && drawInstancedSupported) {
         String vertexShaderPath = this.ssboSupported ? "assets/selene/shaders/shape.vert"
               : "assets/selene/shaders/shape_compat.vert";
         this.shapeProgram = ShaderProgram.fromResources(vertexShaderPath, "assets/selene/shaders/shape.frag");
         this.vaoDraw = GL30.glGenVertexArrays();
         int vbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.vaoDraw);
         GL15.glBindBuffer(34962, vbo);
         float[] quad = new float[] { 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F };
         GL15.glBufferData(34962, quad, 35044);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 2, 5126, false, 0, 0L);
         int localInstanceVbo = 0;
         if (!this.ssboSupported) {
            localInstanceVbo = GL15.glGenBuffers();
            GL15.glBindBuffer(34962, localInstanceVbo);
            int stride = 144;
            long offset = 0L;
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 4, 5126, false, stride, offset);
            GL33.glVertexAttribDivisor(1, 1);
            offset += 16L;
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 4, 5126, false, stride, offset);
            GL33.glVertexAttribDivisor(2, 1);
            offset += 16L;
            GL20.glEnableVertexAttribArray(3);
            GL30.glVertexAttribIPointer(3, 4, 5124, stride, offset);
            GL33.glVertexAttribDivisor(3, 1);
            offset += 16L;
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 4, 5126, false, stride, offset);
            GL33.glVertexAttribDivisor(4, 1);
            offset += 16L;
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 4, 5126, false, stride, offset);
            GL33.glVertexAttribDivisor(5, 1);
            offset += 16L;
            GL20.glEnableVertexAttribArray(6);
            GL30.glVertexAttribIPointer(6, 4, 5124, stride, offset);
            GL33.glVertexAttribDivisor(6, 1);
            offset += 16L;
            GL20.glEnableVertexAttribArray(7);
            GL20.glVertexAttribPointer(7, 4, 5126, false, stride, offset);
            GL33.glVertexAttribDivisor(7, 1);
            offset += 16L;
            GL20.glEnableVertexAttribArray(8);
            GL20.glVertexAttribPointer(8, 4, 5126, false, stride, offset);
            GL33.glVertexAttribDivisor(8, 1);
            offset += 16L;
            GL20.glEnableVertexAttribArray(9);
            GL30.glVertexAttribIPointer(9, 1, 5124, stride, offset);
            GL33.glVertexAttribDivisor(9, 1);
            offset += 4L;
            GL20.glEnableVertexAttribArray(10);
            GL30.glVertexAttribIPointer(10, 1, 5124, stride, offset);
            GL33.glVertexAttribDivisor(10, 1);
            GL15.glBindBuffer(34962, 0);
         }

         GL15.glBindBuffer(34962, 0);
         GL30.glBindVertexArray(0);
         this.ssbo = this.ssboSupported ? GL15.glGenBuffers() : 0;
         this.instanceVbo = localInstanceVbo;
         this.batch.setFlushAction(this::flush);
         if (isMac) {

            this.blurCaptureScaleX = 0.4F;
            this.blurCaptureScaleY = 0.4F;
         }

      } else {
         throw new IllegalStateException(
               "OpenGL instanced rendering is required when shader storage buffers are unavailable");
      }
   }

   private void ensureFullscreenResources() {
      if (this.fullscreenQuadVao == 0) {
         this.fullscreenQuadVao = GL30.glGenVertexArrays();
         this.fullscreenQuadVbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.fullscreenQuadVao);
         GL15.glBindBuffer(34962, this.fullscreenQuadVbo);
         float[] quad = new float[] {
               -1.0F,
               -1.0F,
               0.0F,
               0.0F,
               1.0F,
               -1.0F,
               1.0F,
               0.0F,
               1.0F,
               1.0F,
               1.0F,
               1.0F,
               -1.0F,
               -1.0F,
               0.0F,
               0.0F,
               1.0F,
               1.0F,
               1.0F,
               1.0F,
               -1.0F,
               1.0F,
               0.0F,
               1.0F
         };
         GL15.glBufferData(34962, quad, 35044);
         int stride = 16;
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 2, 5126, false, stride, 0L);
         GL20.glEnableVertexAttribArray(1);
         GL20.glVertexAttribPointer(1, 2, 5126, false, stride, 8L);
         GL15.glBindBuffer(34962, 0);
         GL30.glBindVertexArray(0);
      }
   }

   private ShaderProgram ensureFullscreenProgram() {
      if (this.fullscreenProgram != null) {
         return this.fullscreenProgram;
      } else {
         String vertex = ResourceUtils.readText("assets/selene/shaders/blur/blur_fullscreen.vert");
         String fragment = "#version 330 core\nlayout(location = 0) out vec4 fragColor;\nin vec2 vUv;\nuniform sampler2D uSource;\nvoid main() {\n    fragColor = texture(uSource, vUv);\n}";
         this.fullscreenProgram = new ShaderProgram(vertex, fragment);
         this.fullscreenSamplerLoc = this.fullscreenProgram.getUniformLocation("uSource");
         return this.fullscreenProgram;
      }
   }

   public void beginFrame(int width, int height) {
      this.snapshot = GlState.push();
      this.viewportWidth = width;
      this.viewportHeight = height;
      GL11.glViewport(0, 0, width, height);
      this.batch.beginFrame(width, height);
      this.shapeProgram.use();
      if (this.uViewportLoc == -1) {
         this.uViewportLoc = this.shapeProgram.getUniformLocation("uViewport");
      }

      GL30.glBindVertexArray(this.vaoDraw);
      GL20.glUniform2f(this.uViewportLoc, width, height);
      this.preparedRegionBlurTex = 0;
      this.preparedRegionBlurW = 0;
      this.preparedRegionBlurH = 0;
      this.preparedRegionBlurX = 0;
      this.preparedRegionBlurY = 0;
      GL11.glDisable(2929);
      GL11.glDisable(2884);
      GL11.glDisable(3089);
      GL11.glEnable(3042);
      GL14.glBlendFuncSeparate(1, 771, 1, 771);
      GL11.glColorMask(true, true, true, true);
      if (!this.samplersInitialized) {
         for (int i = 0; i < 16; i++) {
            int loc = this.shapeProgram.getUniformLocation("uTextures[" + i + "]");
            if (loc != -1) {
               GL20.glUniform1i(loc, i);
            }
         }

         this.samplersInitialized = true;
      }
   }

   public void endFrame() {
      this.flush();
      GL30.glBindVertexArray(0);
      GL20.glUseProgram(0);
      if (this.snapshot != null) {
         GlState.pop(this.snapshot);
         this.snapshot = null;
      }

      this.batch.afterFlush();
   }


   public void flush() {
      if (this.batch.getInstanceCount() > 0) {
         ByteBuffer instanceBuffer = this.batch.prepareFlushBuffer();
         if (this.ssboSupported) {
            GL15.glBindBuffer(37074, this.ssbo);
            GL15.glBufferData(37074, instanceBuffer, 35040);
            GL43.glBindBufferBase(37074, 0, this.ssbo);
         } else {
            GL15.glBindBuffer(34962, this.instanceVbo);
            GL15.glBufferData(34962, instanceBuffer, 35040);
            GL15.glBindBuffer(34962, 0);
         }

         int prevActive = GL11.glGetInteger(34016);
         int usedSlots = this.batch.getSlotToTexture().size();
         int[] prevBindings = new int[16];

         int transparentTex = this.transparentPixelTexture();
         for (int slot = 0; slot < 16; slot++) {
            GL13.glActiveTexture(33984 + slot);
            prevBindings[slot] = GL11.glGetInteger(32873);
            int tex = slot < usedSlots ? this.batch.getSlotToTexture().get(slot) : transparentTex;
            GL11.glBindTexture(3553, tex);
         }

         int trianglesDrawn = Math.max(0, this.batch.getInstanceCount()) * 2;
         if (trianglesDrawn > 0) {
            RenderFrameMetrics.getInstance().recordDrawCall(trianglesDrawn);
         }

         GL30.glBindVertexArray(this.vaoDraw);

         if (this.ssboSupported) {
            GL11.glDrawArrays(4, 0, this.batch.getInstanceCount() * 6);
         } else {
            GL31.glDrawArraysInstanced(4, 0, 6, this.batch.getInstanceCount());
         }

         for (int slot = 0; slot < 16; slot++) {
            GL13.glActiveTexture(33984 + slot);
            GL11.glBindTexture(3553, prevBindings[slot]);
         }

         GL13.glActiveTexture(prevActive);
         this.batch.afterFlush();
      }
   }

   public void setExternalViewport(int width, int height) {
      this.viewportWidth = width;
      this.viewportHeight = height;
   }

   public void setScissorEnabled(boolean enabled) {
      this.batch.setScissorEnabled(enabled);
   }

   public void setScissorRect(int x, int y, int w, int h, float roundTopLeft, float roundTopRight,
         float roundBottomRight, float roundBottomLeft) {
      this.batch.setScissorRect(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
   }

   public void setTransform(float[] m3) {
   }

   public void setBlurCaptureScale(float scaleX, float scaleY) {
      if (!Float.isFinite(scaleX) || !Float.isFinite(scaleY)) {
         throw new IllegalArgumentException("Blur capture scale must be finite");
      } else if (!(scaleX <= 0.0F) && !(scaleY <= 0.0F)) {
         this.blurCaptureScaleX = scaleX;
         this.blurCaptureScaleY = scaleY;
      } else {
         throw new IllegalArgumentException("Blur capture scale must be positive");
      }
   }

   public void enqueueRect(
         float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft, int color, float[] transform) {
      this.batch.enqueueRect(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft, color,
            transform);
   }

   public void enqueueRectOutline(
         float x,
         float y,
         float w,
         float h,
         float roundTopLeft,
         float roundTopRight,
         float roundBottomRight,
         float roundBottomLeft,
         int color,
         float thickness,
         float[] transform) {
      this.batch.enqueueRectOutline(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft, color,
            thickness, transform);
   }

   public void enqueueGradient(
         float x,
         float y,
         float w,
         float h,
         float roundTopLeft,
         float roundTopRight,
         float roundBottomRight,
         float roundBottomLeft,
         int c00,
         int c10,
         int c11,
         int c01,
         float[] transform) {
      this.batch.enqueueGradient(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft, c00, c10,
            c11, c01, transform);
   }

   public void enqueueCircle(float cx, float cy, float radius, float startDeg, float pct, int color,
         float[] transform) {
      this.batch.enqueueCircle(cx, cy, radius, startDeg, pct, color, transform);
   }

   public void drawDropShadowRect(
         float x,
         float y,
         float w,
         float h,
         float roundTopLeft,
         float roundTopRight,
         float roundBottomRight,
         float roundBottomLeft,
         float blurStrength,
         float spread,
         int rgbaPremul,
         float[] transform) {
      this.batch.drawDropShadowRect(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft,
            blurStrength, spread, rgbaPremul, transform);
   }

   public void drawTexturedQuad(int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1,
         int rgbaPremul, float[] transform) {
      this.batch.drawTexturedQuad(texture, x, y, w, h, u0, v0, u1, v1, rgbaPremul, transform);
   }

   public void drawTexturedQuadRounded(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float rounding,
         int rgbaPremul, float[] transform) {
      this.batch.drawTexturedQuadRounded(texture, x, y, w, h, u0, v0, u1, v1, rounding, rgbaPremul, transform);
   }

   public void drawRgbaTexturedQuad(int texture, float x, float y, float w, float h, float u0, float v0, float u1,
         float v1, int rgbaPremul, float[] transform) {
      this.batch.drawRgbaTexturedQuad(texture, x, y, w, h, u0, v0, u1, v1, rgbaPremul, transform);
   }

   public void drawRgbaTexturedQuad(
         int texture,
         float x,
         float y,
         float w,
         float h,
         float u0,
         float v0,
         float u1,
         float v1,
         int rgbaPremul,
         float[] transform,
         boolean preservePremultipliedColor) {
      this.batch.drawRgbaTexturedQuad(texture, x, y, w, h, u0, v0, u1, v1, rgbaPremul, transform,
            preservePremultipliedColor);
   }

   public void drawRgbaTexturedQuadRounded(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float rounding,
         int rgbaPremul, float[] transform) {
      this.batch.drawRgbaTexturedQuadRounded(texture, x, y, w, h, u0, v0, u1, v1, rounding, rgbaPremul, transform);
   }

   public void drawRgbaTexturedQuadRounded(
         int texture,
         float x,
         float y,
         float w,
         float h,
         float u0,
         float v0,
         float u1,
         float v1,
         float rounding,
         int rgbaPremul,
         float[] transform,
         boolean preservePremultipliedColor) {
      this.batch.drawRgbaTexturedQuadRounded(texture, x, y, w, h, u0, v0, u1, v1, rounding, rgbaPremul, transform,
            preservePremultipliedColor);
   }

   public void drawRgbaOpaqueTexturedQuadRounded(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float rounding,
         int rgbaPremul, float[] transform) {
      this.batch.drawRgbaOpaqueTexturedQuadRounded(texture, x, y, w, h, u0, v0, u1, v1, rounding, rgbaPremul, transform);
   }

   public void drawRgbaOpaqueTexturedQuadRounded(
         int texture,
         float x,
         float y,
         float w,
         float h,
         float u0,
         float v0,
         float u1,
         float v1,
         float rounding,
         int rgbaPremul,
         float[] transform,
         boolean screenSpaceUv) {
      this.batch.drawRgbaOpaqueTexturedQuadRounded(texture, x, y, w, h, u0, v0, u1, v1, rounding, rgbaPremul, transform,
            screenSpaceUv);
   }

   public void drawRgbaOpaqueTexturedQuad(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int rgbaPremul,
         float[] transform) {
      this.batch.drawRgbaOpaqueTexturedQuad(texture, x, y, w, h, u0, v0, u1, v1, rgbaPremul, transform);
   }

   public void enqueueMsdfGlyph(
         int texture, float pxRange, float x, float y, float width, float height, float u0, float v0, float u1,
         float v1, int rgbaColor, float[] transform) {
      this.batch.enqueueMsdfGlyph(texture, pxRange, x, y, width, height, u0, v0, u1, v1, rgbaColor, transform);
   }

   @Override
   public void enqueueMsdfGlyphOutline(
         int texture, float pxRange, float outlineWidth, float x, float y, float width, float height, float u0, float v0,
         float u1, float v1, int rgbaColor, float[] transform) {
      this.batch.enqueueMsdfGlyphOutline(texture, pxRange, outlineWidth, x, y, width, height, u0, v0, u1, v1, rgbaColor,
            transform);
   }

   public void drawInstances(ByteBuffer data, int instanceCount) {
   }

   public int createMsdfTexture(int width, int height, ByteBuffer data) {
      if (width <= 0 || height <= 0) {
         throw new IllegalArgumentException("Invalid MSDF texture dimensions: " + width + "x" + height);
      } else if (data == null) {
         throw new IllegalArgumentException("data");
      } else {
         int tex = GL11.glGenTextures();
         GL11.glBindTexture(3553, tex);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL12.glTexParameteri(3553, 33084, 0);
         GL12.glTexParameteri(3553, 33085, 0);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glPixelStorei(3317, 1);
         GL12.glPixelStorei(3314, 0);
         data.rewind();
         GL11.glTexImage2D(3553, 0, 32856, width, height, 0, 6408, 5121, data);
         GL11.glBindTexture(3553, 0);
         return tex;
      }
   }

   public int createAlphaTexture(int width, int height) {
      int tex = GL11.glGenTextures();
      GL11.glBindTexture(3553, tex);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL12.glTexParameteri(3553, 33084, 0);
      GL12.glTexParameteri(3553, 33085, 0);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexParameteri(3553, 36418, 6403);
      GL11.glTexParameteri(3553, 36419, 6403);
      GL11.glTexParameteri(3553, 36420, 6403);
      GL11.glTexParameteri(3553, 36421, 6403);
      GL11.glPixelStorei(3317, 1);
      GL12.glPixelStorei(3314, 0);
      GL11.glTexImage2D(3553, 0, 33321, width, height, 0, 6403, 5121, (ByteBuffer) null);
      GL11.glBindTexture(3553, 0);
      return tex;
   }

   public void uploadAlphaSubImage(int tex, int x, int y, int w, int h, ByteBuffer data) {
      int prevAlign = GL11.glGetInteger(3317);
      int prevRowLen = GL11.glGetInteger(3314);
      data.order(ByteOrder.nativeOrder());
      GL11.glBindTexture(3553, tex);
      GL11.glPixelStorei(3317, 1);
      GL12.glPixelStorei(3314, 0);
      GL11.glTexSubImage2D(3553, 0, x, y, w, h, 6403, 5121, data);
      GL12.glPixelStorei(3314, prevRowLen);
      GL11.glPixelStorei(3317, prevAlign);
      GL11.glBindTexture(3553, 0);
   }

   public void uploadAlphaSubImageWithStride(int tex, int x, int y, int w, int h, ByteBuffer data,
         int sourceRowLength) {
      int prevAlign = GL11.glGetInteger(3317);
      int prevRowLen = GL11.glGetInteger(3314);
      data.order(ByteOrder.nativeOrder());
      GL11.glBindTexture(3553, tex);
      GL11.glPixelStorei(3317, 1);
      GL12.glPixelStorei(3314, sourceRowLength);
      GL11.glTexSubImage2D(3553, 0, x, y, w, h, 6403, 5121, data);
      GL12.glPixelStorei(3314, prevRowLen);
      GL11.glPixelStorei(3317, prevAlign);
      GL11.glBindTexture(3553, 0);
   }

   private void ensureCaptureTex(int w, int h, boolean fullscreen) {
      if (w > 0 && h > 0) {
         int currentTex = fullscreen ? this.captureTex : this.regionCaptureTex;
         int currentFbo = fullscreen ? this.captureFbo : this.regionCaptureFbo;
         int currentW = fullscreen ? this.captureW : this.regionCaptureW;
         int currentH = fullscreen ? this.captureH : this.regionCaptureH;
         if (currentTex == 0 || w != currentW || h != currentH) {
            if (currentTex != 0) {
               GL11.glDeleteTextures(currentTex);
            }

            if (currentFbo != 0) {
               GL30.glDeleteFramebuffers(currentFbo);
            }

            currentTex = GL11.glGenTextures();
            GL11.glBindTexture(3553, currentTex);
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            GL11.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer) null);
            GL11.glBindTexture(3553, 0);
            currentFbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(36160, currentFbo);
            GL30.glFramebufferTexture2D(36160, 36064, 3553, currentTex, 0);
            GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
            int status = GL30.glCheckFramebufferStatus(36160);
            GL30.glBindFramebuffer(36160, 0);
            if (status != 36053) {
               GL30.glDeleteFramebuffers(currentFbo);
               GL11.glDeleteTextures(currentTex);
               throw new IllegalStateException("Capture FBO incomplete: status=" + status);
            } else {
               if (fullscreen) {
                  this.captureTex = currentTex;
                  this.captureFbo = currentFbo;
                  this.captureW = w;
                  this.captureH = h;
               } else {
                  this.regionCaptureTex = currentTex;
                  this.regionCaptureFbo = currentFbo;
                  this.regionCaptureW = w;
                  this.regionCaptureH = h;
               }
            }
         }
      }
   }

   private void ensureDownscaledCapture(int screenW, int screenH) {
      this.ensureDownscaledCapture(screenW, screenH, this.blurCaptureScaleX, this.blurCaptureScaleY);
   }

   private void ensureDownscaledCapture(int screenW, int screenH, float scaleX, float scaleY) {
      if (screenW > 0 && screenH > 0) {
         if (!Float.isFinite(scaleX) || !Float.isFinite(scaleY)) {
            throw new IllegalArgumentException("Blur capture scale must be finite");
         } else if (!(scaleX <= 0.0F) && !(scaleY <= 0.0F)) {
            int clampedSrcW = Math.max(1, screenW);
            int clampedSrcH = Math.max(1, screenH);
            int targetW = Math.max(1, Math.round(clampedSrcW * scaleX));
            int targetH = Math.max(1, Math.round(clampedSrcH * scaleY));
            if (this.downscaledCaptureTex == 0 || targetW != this.downscaledCaptureW
                  || targetH != this.downscaledCaptureH) {
               if (this.downscaledCaptureTex != 0) {
                  GL11.glDeleteTextures(this.downscaledCaptureTex);
                  this.downscaledCaptureTex = 0;
               }

               if (this.downscaledCaptureFbo != 0) {
                  GL30.glDeleteFramebuffers(this.downscaledCaptureFbo);
                  this.downscaledCaptureFbo = 0;
               }

               this.downscaledCaptureTex = GL11.glGenTextures();
               GL11.glBindTexture(3553, this.downscaledCaptureTex);
               GL11.glTexParameteri(3553, 10241, 9729);
               GL11.glTexParameteri(3553, 10240, 9729);
               GL11.glTexParameteri(3553, 10242, 33071);
               GL11.glTexParameteri(3553, 10243, 33071);
               GL11.glTexImage2D(3553, 0, 32856, targetW, targetH, 0, 6408, 5121, (ByteBuffer) null);
               GL11.glBindTexture(3553, 0);
               this.downscaledCaptureFbo = GL30.glGenFramebuffers();
               GL30.glBindFramebuffer(36160, this.downscaledCaptureFbo);
               GL30.glFramebufferTexture2D(36160, 36064, 3553, this.downscaledCaptureTex, 0);
               GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
               int status = GL30.glCheckFramebufferStatus(36160);
               GL30.glBindFramebuffer(36160, 0);
               if (status != 36053) {
                  GL30.glDeleteFramebuffers(this.downscaledCaptureFbo);
                  GL11.glDeleteTextures(this.downscaledCaptureTex);
                  this.downscaledCaptureFbo = 0;
                  this.downscaledCaptureTex = 0;
                  throw new IllegalStateException("Downscaled capture FBO incomplete: status=" + status);
               } else {
                  this.downscaledCaptureW = targetW;
                  this.downscaledCaptureH = targetH;
               }
            }
         } else {
            throw new IllegalArgumentException("Blur capture scale must be positive");
         }
      }
   }

   private void releaseMainFramebufferReadAttachments() {
      if (this.fullFrameReadFbo == 0) {
         return;
      }

      GL30.glBindFramebuffer(36008, this.fullFrameReadFbo);
      GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
      GL30.glFramebufferTexture2D(36008, 36096, 3553, 0, 0);
   }

   public int captureRegionToTexture(int x, int y, int w, int h) {
      return this.captureRegionToTexture(x, y, w, h, true);
   }

   public int captureRegionToTexture(int x, int y, int w, int h, boolean fullscreen) {
      try {
         this.ensureCaptureTex(w, h, fullscreen);
      } catch (RuntimeException e) {
         return 0;
      }

      int targetFbo = fullscreen ? this.captureFbo : this.regionCaptureFbo;
      int targetTex = fullscreen ? this.captureTex : this.regionCaptureTex;
      if (targetFbo == 0 || targetTex == 0) {
         return 0;
      }
      int srcX = Math.max(0, Math.min(x, this.viewportWidth));
      int srcY = Math.max(0, this.viewportHeight - y - h);
      int srcW = Math.min(w, this.viewportWidth - srcX);
      int srcH = Math.min(h, this.viewportHeight - Math.max(0, this.viewportHeight - y - h));
      if (srcW <= 0 || srcH <= 0) {
         return 0;
      }
      GlState.Snapshot s = GlState.push();

      try {
         boolean wasScissorEnabled = GL11.glIsEnabled(3089);
         boolean wasSrgb = GlState.isFramebufferSrgbEnabled();
         if (wasScissorEnabled) {
            GL11.glDisable(3089);
         }

         if (wasSrgb) {
            GlState.setFramebufferSrgbEnabled(false);
         }

         MinecraftClient client = MinecraftClient.getInstance();
         Framebuffer framebuffer = client != null ? client.getFramebuffer() : null;
         boolean readFromMain = framebuffer != null && framebuffer.getColorAttachment() instanceof GlTexture;
         if (readFromMain) {
            GlTexture glColor = (GlTexture) framebuffer.getColorAttachment();
            int sourceColor = glColor.getGlId();
            if (this.fullFrameReadFbo == 0) {
               this.fullFrameReadFbo = GL30.glGenFramebuffers();
            }

            GL30.glBindFramebuffer(36008, this.fullFrameReadFbo);
            GL30.glFramebufferTexture2D(36008, 36064, 3553, sourceColor, 0);
            GL30.glFramebufferTexture2D(36008, 36096, 3553, 0, 0);
            GL30.glBindFramebuffer(36009, targetFbo);
            GL11.glReadBuffer(36064);
            GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GL11.glClear(16384);
            GL30.glBlitFramebuffer(srcX, srcY, srcX + srcW, srcY + srcH, 0, 0, w, h, 16384, 9729);
         } else {
            GL30.glBindFramebuffer(36008, 0);
            GL11.glReadBuffer(1029);
            GL30.glBindFramebuffer(36009, targetFbo);
            GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GL11.glClear(16384);
            GL30.glBlitFramebuffer(srcX, srcY, srcX + srcW, srcY + srcH, 0, 0, w, h, 16384, 9729);
         }

         if (readFromMain) {
            this.releaseMainFramebufferReadAttachments();
         }
         if (wasScissorEnabled) {
            GL11.glEnable(3089);
         }

         if (wasSrgb) {
            GlState.setFramebufferSrgbEnabled(true);
         }
      } finally {
         GlState.pop(s);
      }

      return targetTex;
   }

   public void prepareScreenBlur(int screenW, int screenH, float radiusPx) {
      if (screenW > 0 && screenH > 0) {
         float requestedScaleX = 1.0F;
         float requestedScaleY = 1.0F;
         float userScaleFloorX = this.blurCaptureScaleX;
         float userScaleFloorY = this.blurCaptureScaleY;
         float smallKernelThreshold = Float.MAX_VALUE;
         if (radiusPx > smallKernelThreshold) {
            float minimumRadius = this.screenBlur.minimumRadius();
            float radius = Math.max(radiusPx, minimumRadius);
            float adaptiveScale = smallKernelThreshold / radius;
            adaptiveScale = Math.max(adaptiveScale, 0.2F);
            requestedScaleX = Math.min(requestedScaleX, adaptiveScale);
            requestedScaleY = Math.min(requestedScaleY, adaptiveScale);
         }

         requestedScaleX = Math.max(requestedScaleX, userScaleFloorX);
         requestedScaleY = Math.max(requestedScaleY, userScaleFloorY);
         this.ensureDownscaledCapture(screenW, screenH, requestedScaleX, requestedScaleY);
         if (this.downscaledCaptureTex != 0 && this.downscaledCaptureFbo != 0) {
            float actualScaleX = (float) this.downscaledCaptureW / Math.max(1, screenW);
            float actualScaleY = (float) this.downscaledCaptureH / Math.max(1, screenH);
            GlState.Snapshot state = GlState.push();
            boolean readFromMain = false;

            try {
               boolean wasScissorEnabled = GL11.glIsEnabled(3089);
               boolean wasSrgb = GlState.isFramebufferSrgbEnabled();
               if (wasScissorEnabled) {
                  GL11.glDisable(3089);
               }

               if (wasSrgb) {
                  GlState.setFramebufferSrgbEnabled(false);
               }

               int currentReadFbo = GL11.glGetInteger(36010);
               MinecraftClient client = MinecraftClient.getInstance();
               Framebuffer framebuffer = client != null ? client.getFramebuffer() : null;
               boolean hasMainColor = framebuffer != null
                     && framebuffer.getColorAttachment() instanceof GlTexture
                     && ((GlTexture) framebuffer.getColorAttachment()).getGlId() > 0;
               String captureSource;
               if (hasMainColor) {
                  GlTexture glColor = (GlTexture) framebuffer.getColorAttachment();
                  int sourceColor = glColor.getGlId();
                  int srcW = Math.max(1, framebuffer.textureWidth);
                  int srcH = Math.max(1, framebuffer.textureHeight);
                  if (this.fullFrameReadFbo == 0) {
                     this.fullFrameReadFbo = GL30.glGenFramebuffers();
                  }

                  GL30.glBindFramebuffer(36008, this.fullFrameReadFbo);
                  GL30.glFramebufferTexture2D(36008, 36064, 3553, sourceColor, 0);
                  GL30.glFramebufferTexture2D(36008, 36096, 3553, 0, 0);
                  if (GL30.glCheckFramebufferStatus(36008) == 36053) {
                     GL30.glBindFramebuffer(36009, this.downscaledCaptureFbo);
                     GL11.glReadBuffer(36064);
                     GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
                     GL30.glBlitFramebuffer(0, 0, srcW, srcH, 0, 0, this.downscaledCaptureW, this.downscaledCaptureH,
                           16384, 9729);
                     readFromMain = true;
                     captureSource = "mainFbo";
                  } else if (currentReadFbo == 0) {
                     GL30.glBindFramebuffer(36008, 0);
                     GL11.glReadBuffer(1029);
                     GL30.glBindFramebuffer(36009, this.downscaledCaptureFbo);
                     GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
                     GL30.glBlitFramebuffer(0, 0, screenW, screenH, 0, 0, this.downscaledCaptureW, this.downscaledCaptureH,
                           16384, 9729);
                     captureSource = "backBuffer";
                  } else {
                     GL30.glBindFramebuffer(36008, currentReadFbo);
                     GL11.glReadBuffer(36064);
                     GL30.glBindFramebuffer(36009, this.downscaledCaptureFbo);
                     GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
                     GL30.glBlitFramebuffer(0, 0, screenW, screenH, 0, 0, this.downscaledCaptureW, this.downscaledCaptureH,
                           16384, 9729);
                     captureSource = "readFbo";
                  }
               } else if (currentReadFbo == 0) {
                  GL30.glBindFramebuffer(36008, 0);
                  GL11.glReadBuffer(1029);
                  GL30.glBindFramebuffer(36009, this.downscaledCaptureFbo);
                  GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
                  GL30.glBlitFramebuffer(0, 0, screenW, screenH, 0, 0, this.downscaledCaptureW, this.downscaledCaptureH,
                        16384, 9729);
                  captureSource = "backBuffer";
               } else {
                  GL30.glBindFramebuffer(36008, currentReadFbo);
                  GL11.glReadBuffer(36064);
                  GL30.glBindFramebuffer(36009, this.downscaledCaptureFbo);
                  GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
                  GL30.glBlitFramebuffer(0, 0, screenW, screenH, 0, 0, this.downscaledCaptureW, this.downscaledCaptureH,
                        16384, 9729);
                  captureSource = "readFbo";
               }

               if (readFromMain) {
                  this.releaseMainFramebufferReadAttachments();
               }

               if (wasScissorEnabled) {
                  GL11.glEnable(3089);
               }

               if (wasSrgb) {
                  GlState.setFramebufferSrgbEnabled(true);
               }
            } finally {
               GlState.pop(state);
            }

            float var28 = (float) Math.sqrt(Math.max(0.0F, actualScaleX) * Math.max(0.0F, actualScaleY));
            int blurred = this.downscaledCaptureTex;
            if (blurred == 0) {
               this.preparedBlurTex = 0;
               this.preparedBlurW = 0;
               this.preparedBlurH = 0;
               this.preparedBlurScaleX = 1.0F;
               this.preparedBlurScaleY = 1.0F;
            } else if (GuiScreen.frostedGlass) {
               this.preparedBlurTex = this.whitePixelTexture();
               this.preparedBlurW = 1;
               this.preparedBlurH = 1;
               this.preparedBlurScaleX = 1.0F;
               this.preparedBlurScaleY = 1.0F;
            } else if (!readFromMain && this.isCaptureBlack(blurred, this.downscaledCaptureW, this.downscaledCaptureH)) {
               this.preparedBlurTex = this.darkPixelTexture();
               this.preparedBlurW = 1;
               this.preparedBlurH = 1;
               this.preparedBlurScaleX = 1.0F;
               this.preparedBlurScaleY = 1.0F;
            } else {
               this.preparedBlurTex = blurred;
               this.preparedBlurW = this.downscaledCaptureW;
               this.preparedBlurH = this.downscaledCaptureH;
               this.preparedBlurScaleX = actualScaleX;
               this.preparedBlurScaleY = actualScaleY;
            }
         } else {
            this.preparedBlurTex = 0;
            this.preparedBlurW = 0;
            this.preparedBlurH = 0;
            this.preparedBlurScaleX = 1.0F;
            this.preparedBlurScaleY = 1.0F;
         }
      } else {
         this.preparedBlurTex = 0;
         this.preparedBlurW = 0;
         this.preparedBlurH = 0;
         this.preparedBlurScaleX = 1.0F;
         this.preparedBlurScaleY = 1.0F;
      }
   }

   private boolean isCaptureBlack(int texture, int width, int height) {
      if (texture <= 0 || width <= 0 || height <= 0) {
         return false;
      }
      GlState.Snapshot state = GlState.push();
      try {
         if (this.fullFrameReadFbo == 0) {
            this.fullFrameReadFbo = GL30.glGenFramebuffers();
         }

         GL30.glBindFramebuffer(36008, this.fullFrameReadFbo);

         GL30.glFramebufferTexture2D(36008, 36064, 3553, texture, 0);
         GL11.glReadBuffer(36064);
         boolean allBlack = true;
         for (int gy = 0; gy < 3; gy++) {
            for (int gx = 0; gx < 3; gx++) {
               int px = gx * Math.max(0, width - 1) / 2;
               int py = gy * Math.max(0, height - 1) / 2;
               PIXEL_BUF.clear();
               GL11.glReadPixels(px, py, 1, 1, 6403, 5121, PIXEL_BUF);
               float brightness = (float) (PIXEL_BUF.get(0) & 0xFF) / 255.0F;
               if (brightness >= 0.03F) {
                  allBlack = false;
                  break;
               }
            }
            if (!allBlack) {
               break;
            }
         }
         return allBlack;
      } finally {
         GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
         GlState.pop(state);
      }
   }

   private int whitePixelTexture() {
      if (this.whitePixelTex == 0) {
         this.whitePixelTex = this.solidPixelTexture(255, 255, 255);
      }
      return this.whitePixelTex;
   }

   private int darkPixelTexture() {
      if (this.darkPixelTex == 0) {
         this.darkPixelTex = this.solidPixelTexture(24, 30, 42);
      }
      return this.darkPixelTex;
   }

   private int transparentPixelTexture() {
      if (this.transparentPixelTex == 0) {
         int tex = GL11.glGenTextures();
         GL11.glBindTexture(3553, tex);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10241, 9729);
         ByteBuffer px = org.lwjgl.BufferUtils.createByteBuffer(4);
         px.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0).flip();
         GL11.glTexImage2D(3553, 0, 6408, 1, 1, 0, 6408, 5121, px);
         GL11.glBindTexture(3553, 0);
         this.transparentPixelTex = tex;
      }
      return this.transparentPixelTex;
   }

   private int solidPixelTexture(int r, int g, int b) {
      int tex = GL11.glGenTextures();
      GL11.glBindTexture(3553, tex);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10241, 9729);
      ByteBuffer px = org.lwjgl.BufferUtils.createByteBuffer(3);
      px.put((byte) r).put((byte) g).put((byte) b).flip();
      GL11.glTexImage2D(3553, 0, 6407, 1, 1, 0, 6407, 5121, px);

      GL11.glBindTexture(3553, 0);
      return tex;
   }

   public boolean prepareRegionBlur(int x, int y, int width, int height, float radiusPx) {
      if (width > 0 && height > 0) {
         int texture = this.captureRegionToTexture(x, y, width, height, false);
         int blurred = texture; 
         this.preparedRegionBlurTex = blurred;
         this.preparedRegionBlurW = width;
         this.preparedRegionBlurH = height;
         this.preparedRegionBlurX = x;
         this.preparedRegionBlurY = y;
         return blurred != 0;
      } else {
         this.preparedRegionBlurTex = 0;
         this.preparedRegionBlurW = 0;
         this.preparedRegionBlurH = 0;
         this.preparedRegionBlurX = 0;
         this.preparedRegionBlurY = 0;
         return false;
      }
   }

   public void drawPreparedBlurRounded(float x, float y, float w, float h, float rounding, float alpha,
         float[] transform) {
      if (this.preparedBlurTex != 0) {
         int colorPremul = (int) (Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24 | 16777215;
         float uScale = this.preparedBlurW > 0 ? this.preparedBlurScaleX / this.preparedBlurW : 0.0F;
         float vScale = this.preparedBlurH > 0 ? -this.preparedBlurScaleY / this.preparedBlurH : 0.0F;
         float uOffset = 0.0F;
         float vOffset = this.preparedBlurH > 0 ? 1.0F : 0.0F;
         this.drawRgbaOpaqueTexturedQuadRounded(this.preparedBlurTex, x, y, w, h, uScale, vScale, uOffset, vOffset,
               rounding, colorPremul, transform, true);
      }
   }

   public void drawPreparedRegionBlurRounded(
         float x, float y, float w, float h, float rounding, float alpha, float[] transform, int regionX, int regionY,
         int regionW, int regionH) {
      if (this.preparedRegionBlurTex != 0) {
         if (regionW > 0 && regionH > 0) {
            if (this.preparedRegionBlurW == regionW
                  && this.preparedRegionBlurH == regionH
                  && this.preparedRegionBlurX == regionX
                  && this.preparedRegionBlurY == regionY) {
               int colorPremul = (int) (Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24 | 16777215;
               float u0 = 0.0F;
               float v0 = 1.0F;
               float u1 = 1.0F;
               float v1 = 0.0F;
               this.drawRgbaOpaqueTexturedQuadRounded(this.preparedRegionBlurTex, x, y, w, h, u0, v0, u1, v1, rounding,
                     colorPremul, transform, false);
            }
         }
      }
   }

   public RenderBackend.FrameCapture captureFullFrame() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null) {
         return new RenderBackend.FrameCapture(0, 0, 0, 0);
      } else {
         Framebuffer framebuffer = client.getFramebuffer();
         if (framebuffer == null) {
            return new RenderBackend.FrameCapture(0, 0, 0, 0);
         } else if (!(framebuffer.getColorAttachment() instanceof GlTexture glColor)) {
            return new RenderBackend.FrameCapture(0, 0, 0, 0);
         } else {
            int sourceColor = glColor.getGlId();
            int sourceDepth = 0;
            if (framebuffer.getDepthAttachment() instanceof GlTexture glDepth) {
               sourceDepth = glDepth.getGlId();
            }

            int width = Math.max(1, framebuffer.textureWidth);
            int height = Math.max(1, framebuffer.textureHeight);
            this.fullFrameTarget.ensure(width, height);
            GlState.Snapshot state = GlState.push();

            try {
               GL11.glDisable(3089);
               GL11.glDisable(2884);
               GL11.glDisable(3042);
               GL11.glDisable(2929);
               GlState.disableFramebufferSrgb();
               if (this.fullFrameReadFbo == 0) {
                  this.fullFrameReadFbo = GL30.glGenFramebuffers();
               }

               GL30.glBindFramebuffer(36008, this.fullFrameReadFbo);
               GL30.glFramebufferTexture2D(36008, 36064, 3553, sourceColor, 0);
               if (sourceDepth > 0) {
                  GL30.glFramebufferTexture2D(36008, 36096, 3553, sourceDepth, 0);
               } else {
                  GL30.glFramebufferTexture2D(36008, 36096, 3553, 0, 0);
               }

               int status = GL30.glCheckFramebufferStatus(36008);
               if (status != 36053) {
                  throw new IllegalStateException("Source framebuffer incomplete: status=" + status);
               }

               GL30.glBindFramebuffer(36009, this.fullFrameTarget.fbo);
               GL11.glReadBuffer(36064);
               GL20.glDrawBuffers(DRAW_BUFFER_COLOR0);
               int mask = 16384;
               if (sourceDepth > 0) {
                  mask |= 256;
               }

               GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, mask, 9728);
            } finally {
               this.releaseMainFramebufferReadAttachments();
               GlState.pop(state);
            }

            return new RenderBackend.FrameCapture(this.fullFrameTarget.colorTex, this.fullFrameTarget.depthTex, width,
                  height);
         }
      }
   }

   public void drawFullscreenTexture(int texture, int width, int height) {
      if (texture > 0 && width > 0 && height > 0) {
         this.ensureFullscreenResources();
         ShaderProgram program = this.ensureFullscreenProgram();
         GlState.Snapshot state = GlState.push();

         try {
            GL30.glBindFramebuffer(36160, 0);
            GL11.glViewport(0, 0, width, height);
            GL11.glDisable(3089);
            GL11.glDisable(2884);
            GL11.glDisable(2929);
            GL11.glDisable(3042);
            GlState.disableFramebufferSrgb();
            program.use();
            if (this.fullscreenSamplerLoc >= 0) {
               GL20.glUniform1i(this.fullscreenSamplerLoc, 0);
            }

            GL13.glActiveTexture(33984);
            GL11.glBindTexture(3553, texture);
            GL30.glBindVertexArray(this.fullscreenQuadVao);
            RenderFrameMetrics.getInstance().recordDrawCall(2);
            GL11.glDrawArrays(4, 0, 6);
            GL30.glBindVertexArray(0);
         } finally {
            GL13.glActiveTexture(33984);
            GL11.glBindTexture(3553, 0);
            GL20.glUseProgram(0);
            GlState.pop(state);
         }
      }
   }

   private void ensureGlassResources() {
      if (this.glassProgram != null) {
         return;
      }

      this.glassProgram = ShaderProgram.fromResources("assets/selene/shaders/ui/glass.vert", "assets/selene/shaders/ui/glass.frag");
      this.uGlassViewportLoc = this.glassProgram.getUniformLocation("uViewport");
      this.uGlassBlurScaleLoc = this.glassProgram.getUniformLocation("uBlurScale");
      this.uGlassBlurOffsetLoc = this.glassProgram.getUniformLocation("uBlurOffset");
      this.uGlassSamplerLoc = this.glassProgram.getUniformLocation("uBlur");
      this.uGlassScissorLoc = this.glassProgram.getUniformLocation("uScissor");
      this.uGlassScissorEnabledLoc = this.glassProgram.getUniformLocation("uScissorEnabled");

      this.glassOutlineProgram = ShaderProgram.fromResources("assets/selene/shaders/ui/glass.vert", "assets/selene/shaders/ui/glass_outline.frag");
      this.uGlassOutlineViewportLoc = this.glassOutlineProgram.getUniformLocation("uViewport");
      this.uGlassOutlineBlurScaleLoc = this.glassOutlineProgram.getUniformLocation("uBlurScale");
      this.uGlassOutlineBlurOffsetLoc = this.glassOutlineProgram.getUniformLocation("uBlurOffset");
      this.uGlassOutlineSamplerLoc = this.glassOutlineProgram.getUniformLocation("uBlur");
      this.uGlassOutlineScissorLoc = this.glassOutlineProgram.getUniformLocation("uScissor");
      this.uGlassOutlineScissorEnabledLoc = this.glassOutlineProgram.getUniformLocation("uScissorEnabled");

      this.glassQuadVao = GL30.glGenVertexArrays();
      this.glassQuadVbo = GL15.glGenBuffers();
      this.glassInstanceVbo = GL15.glGenBuffers();
      GL30.glBindVertexArray(this.glassQuadVao);
      GL15.glBindBuffer(34962, this.glassQuadVbo);
      float[] quad = new float[] { 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F };
      GL15.glBufferData(34962, quad, 35044);
      GL20.glEnableVertexAttribArray(0);
      GL20.glVertexAttribPointer(0, 2, 5126, false, 0, 0L);
      GL15.glBindBuffer(34962, this.glassInstanceVbo);
      int stride = 24 * 4;
      long offset = 0L;
      for (int loc = 1; loc <= 6; loc++) {
         GL20.glEnableVertexAttribArray(loc);
         GL20.glVertexAttribPointer(loc, 4, 5126, false, stride, offset);
         GL33.glVertexAttribDivisor(loc, 1);
         offset += 16L;
      }

      GL15.glBindBuffer(34962, 0);
      GL30.glBindVertexArray(0);
      this.glassInstanceBuffer = ByteBuffer.allocateDirect(stride).order(ByteOrder.nativeOrder());
   }

   public boolean drawGlass(
         float x,
         float y,
         float w,
         float h,
         float roundTopLeft,
         float roundTopRight,
         float roundBottomRight,
         float roundBottomLeft,
         int fresnelArgb,
         float fresnelPower,
         float baseAlpha,
         boolean fresnelInvert,
         float fresnelMix,
         float distortStrength,
         float globalAlpha,
         float[] transform) {
      if (this.preparedBlurTex == 0 || !(w > 0.0F) || !(h > 0.0F) || globalAlpha <= 0.0F) {
         return false;
      }

      this.ensureGlassResources();
      float[] bounds = this.computeGlassBounds(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight,
            roundBottomLeft, transform);
      if (bounds == null) {
         return false;
      }

      float fr = (fresnelArgb >>> 16 & 0xFF) / 255.0F;
      float fg = (fresnelArgb >>> 8 & 0xFF) / 255.0F;
      float fb = (fresnelArgb & 0xFF) / 255.0F;
      float fa = (fresnelArgb >>> 24 & 0xFF) / 255.0F;

      ByteBuffer buf = this.glassInstanceBuffer;
      buf.clear();
      this.putGlassHeader(buf, bounds);
      buf.putFloat(clamp01(globalAlpha));
      buf.putFloat(Math.max(0.001F, fresnelPower));
      buf.putFloat(clamp01(baseAlpha));
      buf.putFloat(clamp01(fresnelMix));
      buf.putFloat(fr);
      buf.putFloat(fg);
      buf.putFloat(fb);
      buf.putFloat(fa);
      buf.putFloat(fresnelInvert ? 1.0F : 0.0F);
      buf.putFloat(distortStrength);
      buf.putFloat(0.0F);
      buf.putFloat(0.0F);
      buf.putFloat(0.0F);
      buf.putFloat(0.0F);
      buf.putFloat(0.0F);
      buf.putFloat(0.0F);
      buf.flip();

      return this.renderGlassQuad(
            this.glassProgram, this.uGlassViewportLoc, this.uGlassBlurScaleLoc, this.uGlassBlurOffsetLoc,
            this.uGlassSamplerLoc, this.uGlassScissorLoc, this.uGlassScissorEnabledLoc, buf);
   }

   public boolean drawGlassOutline(
         float x,
         float y,
         float w,
         float h,
         float roundTopLeft,
         float roundTopRight,
         float roundBottomRight,
         float roundBottomLeft,
         float thickness,
         int tintArgb,
         int fresnelArgb,
         float fresnelPower,
         float baseAlpha,
         boolean fresnelInvert,
         float fresnelMix,
         float distortStrength,
         float shinePhase,
         float globalAlpha,
         float[] transform) {
      if (this.preparedBlurTex == 0 || !(w > 0.0F) || !(h > 0.0F) || globalAlpha <= 0.0F) {
         return false;
      }

      this.ensureGlassResources();
      float[] bounds = this.computeGlassBounds(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight,
            roundBottomLeft, transform);
      if (bounds == null) {
         return false;
      }

      float tr = (tintArgb >>> 16 & 0xFF) / 255.0F;
      float tg = (tintArgb >>> 8 & 0xFF) / 255.0F;
      float tb = (tintArgb & 0xFF) / 255.0F;
      float ta = (tintArgb >>> 24 & 0xFF) / 255.0F;
      float fr = (fresnelArgb >>> 16 & 0xFF) / 255.0F;
      float fg = (fresnelArgb >>> 8 & 0xFF) / 255.0F;
      float fb = (fresnelArgb & 0xFF) / 255.0F;
      float fa = (fresnelArgb >>> 24 & 0xFF) / 255.0F;
      float thicknessPx = Math.max(0.0F, thickness * bounds[8]);

      ByteBuffer buf = this.glassInstanceBuffer;
      buf.clear();
      this.putGlassHeader(buf, bounds);
      buf.putFloat(clamp01(globalAlpha));
      buf.putFloat(Math.max(0.001F, fresnelPower));
      buf.putFloat(clamp01(baseAlpha));
      buf.putFloat(clamp01(fresnelMix));
      buf.putFloat(fr);
      buf.putFloat(fg);
      buf.putFloat(fb);
      buf.putFloat(fa);
      buf.putFloat(fresnelInvert ? 1.0F : 0.0F);
      buf.putFloat(distortStrength);
      buf.putFloat(thicknessPx);
      buf.putFloat(shinePhase);
      buf.putFloat(tr);
      buf.putFloat(tg);
      buf.putFloat(tb);
      buf.putFloat(ta);
      buf.flip();

      return this.renderGlassQuad(
            this.glassOutlineProgram, this.uGlassOutlineViewportLoc, this.uGlassOutlineBlurScaleLoc,
            this.uGlassOutlineBlurOffsetLoc, this.uGlassOutlineSamplerLoc, this.uGlassOutlineScissorLoc,
            this.uGlassOutlineScissorEnabledLoc, buf);
   }

   private void putGlassHeader(ByteBuffer buf, float[] bounds) {
      buf.putFloat(bounds[0]);
      buf.putFloat(bounds[1]);
      buf.putFloat(bounds[2]);
      buf.putFloat(bounds[3]);
      buf.putFloat(bounds[4]);
      buf.putFloat(bounds[5]);
      buf.putFloat(bounds[6]);
      buf.putFloat(bounds[7]);
   }

   private float[] computeGlassBounds(
         float x, float y, float w, float h,
         float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft,
         float[] transform) {
      float[] m = transform != null && transform.length >= 6
            ? transform
            : new float[] { 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F };
      float x0 = m[0] * x + m[1] * y + m[2];
      float y0 = m[3] * x + m[4] * y + m[5];
      float x1 = m[0] * (x + w) + m[1] * y + m[2];
      float y1 = m[3] * (x + w) + m[4] * y + m[5];
      float x2 = m[0] * (x + w) + m[1] * (y + h) + m[2];
      float y2 = m[3] * (x + w) + m[4] * (y + h) + m[5];
      float x3 = m[0] * x + m[1] * (y + h) + m[2];
      float y3 = m[3] * x + m[4] * (y + h) + m[5];
      float minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
      float maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
      float minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
      float maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));
      float tw = maxX - minX;
      float th = maxY - minY;
      if (!(tw > 0.0F) || !(th > 0.0F)) {
         return null;
      }

      float scale = Math.min((float) Math.hypot(m[0], m[3]), (float) Math.hypot(m[1], m[4]));
      float rTL = Math.max(0.0F, roundTopLeft * scale);
      float rTR = Math.max(0.0F, roundTopRight * scale);
      float rBR = Math.max(0.0F, roundBottomRight * scale);
      float rBL = Math.max(0.0F, roundBottomLeft * scale);
      return new float[] { minX, minY, tw, th, rTL, rTR, rBR, rBL, scale };
   }

   private boolean renderGlassQuad(
         ShaderProgram program,
         int viewportLoc,
         int blurScaleLoc,
         int blurOffsetLoc,
         int samplerLoc,
         int scissorLoc,
         int scissorEnabledLoc,
         ByteBuffer buf) {
      float uScaleX = this.preparedBlurW > 0 ? this.preparedBlurScaleX / this.preparedBlurW : 0.0F;
      float uScaleY = this.preparedBlurH > 0 ? -this.preparedBlurScaleY / this.preparedBlurH : 0.0F;
      GlState.Snapshot state = GlState.push();

      try {
         GL11.glDisable(2929);
         GL11.glDisable(2884);
         GL11.glDisable(3089);
         GL11.glEnable(3042);
         GL14.glBlendFuncSeparate(1, 771, 1, 771);
         GL11.glColorMask(true, true, true, true);
         program.use();
         if (viewportLoc >= 0) {
            GL20.glUniform2f(viewportLoc, this.viewportWidth, this.viewportHeight);
         }

         if (blurScaleLoc >= 0) {
            GL20.glUniform2f(blurScaleLoc, uScaleX, uScaleY);
         }

         if (blurOffsetLoc >= 0) {
            GL20.glUniform2f(blurOffsetLoc, 0.0F, 1.0F);
         }

         if (samplerLoc >= 0) {
            GL20.glUniform1i(samplerLoc, 0);
         }

         if (scissorEnabledLoc >= 0) {
            GL20.glUniform1f(scissorEnabledLoc, this.batch.isClipEnabled() ? 1.0F : 0.0F);
         }

         if (scissorLoc >= 0) {
            if (this.batch.isClipEnabled()) {
               GL20.glUniform4f(
                     scissorLoc,
                     this.batch.getClipX(),
                     this.batch.getClipY(),
                     (float) this.batch.getClipX() + this.batch.getClipW(),
                     (float) this.batch.getClipY() + this.batch.getClipH());
            } else {
               GL20.glUniform4f(scissorLoc, 0.0F, 0.0F, 0.0F, 0.0F);
            }
         }

         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, this.preparedBlurTex);
         GL30.glBindVertexArray(this.glassQuadVao);
         GL15.glBindBuffer(34962, this.glassInstanceVbo);
         GL15.glBufferData(34962, buf, 35048);
         RenderFrameMetrics.getInstance().recordDrawCall(2);
         GL31.glDrawArraysInstanced(4, 0, 6, 1);
      } finally {
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, 0);
         GlState.pop(state);
      }

      return true;
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      } else {
         return value > 1.0F ? 1.0F : value;
      }
   }

   public int getPreparedBlurTexture() {
      return this.preparedBlurTex;
   }

   public int getPreparedBlurWidth() {
      return this.preparedBlurW;
   }

   public int getPreparedBlurHeight() {
      return this.preparedBlurH;
   }

   public float getPreparedBlurScaleX() {
      return this.preparedBlurScaleX;
   }

   public float getPreparedBlurScaleY() {
      return this.preparedBlurScaleY;
   }

   public int getPreparedRegionBlurTexture() {
      return this.preparedRegionBlurTex;
   }

   public int getPreparedRegionBlurX() {
      return this.preparedRegionBlurX;
   }

   public int getPreparedRegionBlurY() {
      return this.preparedRegionBlurY;
   }

   public int getPreparedRegionBlurWidth() {
      return this.preparedRegionBlurW;
   }

   public int getPreparedRegionBlurHeight() {
      return this.preparedRegionBlurH;
   }

   @Override
   public void destroyTexture(int textureId) {
      if (textureId > 0) {
         GL11.glDeleteTextures(textureId);
      }
   }

   public void destroy() {
      if (!this.destroyed) {
         this.destroyed = true;
         this.screenBlur.destroy();
         this.regionBlur.destroy();
         this.fullFrameTarget.destroy();
         if (this.fullFrameReadFbo != 0) {
            GL30.glDeleteFramebuffers(this.fullFrameReadFbo);
            this.fullFrameReadFbo = 0;
         }

         if (this.fullscreenQuadVao != 0) {
            GL30.glDeleteVertexArrays(this.fullscreenQuadVao);
            this.fullscreenQuadVao = 0;
         }

         if (this.fullscreenQuadVbo != 0) {
            GL15.glDeleteBuffers(this.fullscreenQuadVbo);
            this.fullscreenQuadVbo = 0;
         }

         if (this.captureFbo != 0) {
            GL30.glDeleteFramebuffers(this.captureFbo);
            this.captureFbo = 0;
         }

         if (this.captureTex != 0) {
            GL11.glDeleteTextures(this.captureTex);
            this.captureTex = 0;
         }

         this.captureW = 0;
         this.captureH = 0;
         if (this.downscaledCaptureFbo != 0) {
            GL30.glDeleteFramebuffers(this.downscaledCaptureFbo);
            this.downscaledCaptureFbo = 0;
         }

         if (this.downscaledCaptureTex != 0) {
            GL11.glDeleteTextures(this.downscaledCaptureTex);
            this.downscaledCaptureTex = 0;
         }

         this.downscaledCaptureW = 0;
         this.downscaledCaptureH = 0;
         if (this.regionCaptureFbo != 0) {
            GL30.glDeleteFramebuffers(this.regionCaptureFbo);
            this.regionCaptureFbo = 0;
         }

         if (this.regionCaptureTex != 0) {
            GL11.glDeleteTextures(this.regionCaptureTex);
            this.regionCaptureTex = 0;
         }

         this.regionCaptureW = 0;
         this.regionCaptureH = 0;
         this.preparedBlurTex = 0;
         this.preparedBlurW = 0;
         this.preparedBlurH = 0;
         this.preparedBlurScaleX = 1.0F;
         this.preparedBlurScaleY = 1.0F;
         this.preparedRegionBlurTex = 0;
         this.preparedRegionBlurW = 0;
         this.preparedRegionBlurH = 0;
         this.preparedRegionBlurX = 0;
         this.preparedRegionBlurY = 0;
         GL30.glBindVertexArray(0);
         GL20.glUseProgram(0);
         if (this.vaoDraw != 0) {
            GL30.glDeleteVertexArrays(this.vaoDraw);
         }

         if (this.ssbo != 0) {
            GL15.glDeleteBuffers(this.ssbo);
         }

         if (this.instanceVbo != 0) {
            GL15.glDeleteBuffers(this.instanceVbo);
         }

         this.shapeProgram.delete();
         if (this.fullscreenProgram != null) {
            this.fullscreenProgram.delete();
            this.fullscreenProgram = null;
         }

         if (this.glassProgram != null) {
            this.glassProgram.delete();
            this.glassProgram = null;
         }

         if (this.glassQuadVao != 0) {
            GL30.glDeleteVertexArrays(this.glassQuadVao);
            this.glassQuadVao = 0;
         }

         if (this.glassQuadVbo != 0) {
            GL15.glDeleteBuffers(this.glassQuadVbo);
            this.glassQuadVbo = 0;
         }

         if (this.glassInstanceVbo != 0) {
            GL15.glDeleteBuffers(this.glassInstanceVbo);
            this.glassInstanceVbo = 0;
         }

         if (this.debugCallback != null) {
            this.debugCallback.free();
            this.debugCallback = null;
         }
      }
   }


   private static String severityToString(int severity) {
      return switch (severity) {
         case 33387 -> "NOTIFICATION";
         case 37190 -> "HIGH";
         case 37191 -> "MEDIUM";
         case 37192 -> "LOW";
         default -> Integer.toString(severity);
      };
   }

}