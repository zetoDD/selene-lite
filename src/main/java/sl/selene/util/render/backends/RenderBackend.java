package sl.selene.util.render.backends;

import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface RenderBackend {
   record FrameCapture(int colorTexture, int depthTexture, int width, int height) {
   }

   void beginFrame(int width, int height);

   void endFrame();

   void flush();

   void setScissorEnabled(boolean enabled);

   void setScissorRect(int x, int y, int w, int h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft);

   void setTransform(float[] m3);

   void setBlurCaptureScale(float scaleX, float scaleY);

   void enqueueRect(
         float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft, int color, float[] transform);

   void enqueueRectOutline(
         float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft, int color, float thickness, float[] transform);

   void enqueueGradient(
         float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft, int colorTL, int colorTR, int colorBR, int colorBL, float[] transform);

   void enqueueCircle(float cx, float cy, float radius, float startDeg, float pct, int color, float[] transform);

   void drawDropShadowRect(
         float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft, float blurStrength, float spread, int rgbaPremul, float[] transform);

   void drawTexturedQuad(int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1,
         int rgbaPremul, float[] transform);

   void drawTexturedQuadRounded(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float rounding,
         int rgbaPremul, float[] transform);

   void drawRgbaTexturedQuad(int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1,
         int rgbaPremul, float[] transform);

   void drawRgbaTexturedQuad(
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
         boolean preservePremultipliedColor);

   void drawRgbaTexturedQuadRounded(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float rounding,
         int rgbaPremul, float[] transform);

   void drawRgbaTexturedQuadRounded(
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
         boolean preservePremultipliedColor);

   void drawRgbaOpaqueTexturedQuadRounded(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float rounding,
         int rgbaPremul, float[] transform);

   void drawRgbaOpaqueTexturedQuadRounded(
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
         boolean screenSpaceUv);

   void drawRgbaOpaqueTexturedQuad(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int rgbaPremul,
         float[] transform);

   void enqueueMsdfGlyph(
         int texture, float pxRange, float x, float y, float width, float height, float u0, float v0, float u1,
         float v1, int rgbaColor, float[] transform);

   void enqueueMsdfGlyphOutline(
         int texture, float pxRange, float outlineWidth, float x, float y, float width, float height, float u0, float v0,
         float u1, float v1, int rgbaColor, float[] transform);

   void drawInstances(ByteBuffer data, int instanceCount);

   int createMsdfTexture(int width, int height, ByteBuffer data);

   int createAlphaTexture(int width, int height);

   void uploadAlphaSubImage(int tex, int x, int y, int w, int h, ByteBuffer data);

   void uploadAlphaSubImageWithStride(int tex, int x, int y, int w, int h, ByteBuffer data, int sourceRowLength);

   int captureRegionToTexture(int x, int y, int w, int h);

   int captureRegionToTexture(int x, int y, int w, int h, boolean fullscreen);

   void prepareScreenBlur(int screenW, int screenH, float radiusPx);

   boolean prepareRegionBlur(int x, int y, int width, int height, float radiusPx);

   void drawPreparedBlurRounded(float x, float y, float w, float h, float rounding, float alpha, float[] transform);

   void drawPreparedRegionBlurRounded(
         float x, float y, float w, float h, float rounding, float alpha, float[] transform, int regionX, int regionY,
         int regionW, int regionH);

   FrameCapture captureFullFrame();

   void drawFullscreenTexture(int texture, int width, int height);

   boolean drawGlass(
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
         float[] transform);

   boolean drawGlassOutline(
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
         float[] transform);

   void destroyTexture(int textureId);

   void destroy();
}