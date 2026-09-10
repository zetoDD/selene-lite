package sl.selene.util.render.core;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.MinecraftClient;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector2d;
import org.lwjgl.opengl.GL11;
import sl.selene.util.render.backends.RenderBackend;
import sl.selene.util.render.backends.gl.GlBackend;
import sl.selene.util.render.backends.gl.GlassMoonRenderer;
import sl.selene.util.render.math.MathHelper;
import sl.selene.util.render.math.animation.anim2.Interpolator;
import sl.selene.util.render.text.FontObject;
import sl.selene.util.render.text.TextRenderer;

@Environment(EnvType.CLIENT)
public final class Renderer2D {

   private final RenderBackend backend;
   private final ArrayDeque<Renderer2D.ClipState> clipStack = new ArrayDeque<>();
   private final ArrayDeque<Float> alphaStack = new ArrayDeque<>();
   private final TransformStack transformStack = new TransformStack();
   private Map<String, TextRenderer> idToTextRenderer = new HashMap<>();
   private final ShapeBatcher batcher;
   private boolean frameBegun = false;
   private int frameWidth = 0;
   private int frameHeight = 0;
   private boolean blurPrepared = false;
   private float blurPreparedStrength = 0.0F;
   private int blurPreparedWidth = 0;
   private int blurPreparedHeight = 0;
   private boolean regionBlurPrepared = false;
   private float regionBlurPreparedStrength = 0.0F;
   private int regionBlurCaptureX = 0;
   private int regionBlurCaptureY = 0;
   private int regionBlurCaptureWidth = 0;
   private int regionBlurCaptureHeight = 0;
   public static MinecraftClient mc = MinecraftClient.getInstance();

   public Renderer2D(RenderBackend backend) {
      if (backend == null) {
         throw new IllegalArgumentException("RenderBackend cannot be null");
      } else {
         this.backend = backend;
         this.batcher = new ShapeBatcher(backend);
         this.resetAlphaStack();
      }
   }

   public void begin(int width, int height) {
      if (width > 0 && height > 0) {
         if (this.frameBegun) {
            this.forceEnd();
         }

         try {
            this.frameBegun = true;
            this.frameWidth = width;
            this.frameHeight = height;
            this.blurPrepared = false;
            this.blurPreparedStrength = 0.0F;
            this.blurPreparedWidth = 0;
            this.blurPreparedHeight = 0;
            this.regionBlurPrepared = false;
            this.regionBlurPreparedStrength = 0.0F;
            this.regionBlurCaptureX = 0;
            this.regionBlurCaptureY = 0;
            this.regionBlurCaptureWidth = 0;
            this.regionBlurCaptureHeight = 0;
            if (this.backend != null) {
               RenderFrameMetrics.getInstance().beginFrame(width, height);
               this.backend.beginFrame(width, height);
               this.backend.setScissorEnabled(false);
            }

            this.clipStack.clear();
            this.transformStack.clear();
            this.resetAlphaStack();
         } catch (Exception var4) {
            this.frameBegun = false;
            System.err.println("Error in Renderer2D.begin(): " + var4.getMessage());
            var4.printStackTrace();
            throw new RuntimeException("Failed to begin render frame", var4);
         }
      } else {
         throw new IllegalArgumentException("Width and height must be positive, got: " + width + "x" + height);
      }
   }

   private void forceEnd() {
      try {
         if (this.batcher != null) {
            this.batcher.flush();
         }

         if (this.backend != null) {
            this.backend.endFrame();
         }

         RenderFrameMetrics.getInstance().endFrame();
      } catch (Exception var5) {
      } finally {
         this.frameBegun = false;
         this.frameWidth = 0;
         this.frameHeight = 0;
         this.blurPrepared = false;
         this.blurPreparedStrength = 0.0F;
         this.blurPreparedWidth = 0;
         this.blurPreparedHeight = 0;
         this.regionBlurPrepared = false;
         this.regionBlurPreparedStrength = 0.0F;
         this.regionBlurCaptureX = 0;
         this.regionBlurCaptureY = 0;
         this.regionBlurCaptureWidth = 0;
         this.regionBlurCaptureHeight = 0;
         this.clipStack.clear();
         this.transformStack.clear();
         this.resetAlphaStack();
      }
   }

   private void ensureFrame() {
      if (!this.frameBegun) {
         throw new IllegalStateException("begin() must be called before issuing draw commands");
      } else if (this.backend == null) {
         throw new IllegalStateException("Renderer2D backend is null - initialization failed");
      } else if (this.batcher == null) {
         throw new IllegalStateException("Renderer2D batcher is null - initialization failed");
      }
   }

   public void rect(float x, float y, float w, float h, int rgbaPremul) {
      this.ensureFrame();
      this.batcher.enqueueRect(x, y, w, h, 0.0F, 0.0F, 0.0F, 0.0F, this.modulateColor(rgbaPremul),
            this.transformStack.current());
   }

   public void rect(float x, float y, float w, float h, float rounding, int rgbaPremul) {
      this.rect(x, y, w, h, rounding, rounding, rounding, rounding, rgbaPremul);
   }

   public void rect(float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft, int rgbaPremul) {
      this.ensureFrame();
      float[] radii = scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
      normalizeCornerRadii(w, h, radii);
      this.batcher.enqueueRect(x, y, w, h, radii[0], radii[1], radii[2], radii[3], this.modulateColor(rgbaPremul),
            this.transformStack.current());
   }

   public void drawRgbaTexture(int texture, float x, float y, float w, float h) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, -1, true, false);
   }

   public void drawRgbaTexture(int texture, float x, float y, float w, float h, int tintRgba) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, tintRgba, true, false);
   }

   public void drawRgbaTexture(int texture, float x, float y, float w, float h, int tintRgba, boolean flipVertically) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, tintRgba, flipVertically, false);
   }

   public void drawPremultipliedRgbaTexture(int texture, float x, float y, float w, float h) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, -1, true, true);
   }

   public void drawPremultipliedRgbaTexture(int texture, float x, float y, float w, float h, int tintRgba,
         boolean flipVertically) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, tintRgba, flipVertically, true);
   }

   public void drawRgbaTextureWithUV(int texture, float x, float y, float w, float h, float u0, float v0, float u1,
         float v1) {
      this.ensureFrame();
      if (texture > 0) {
         this.backend.drawRgbaTexturedQuad(texture, x, y, w, h, u0, v0, u1, v1, this.modulateColor(-1),
               this.transformStack.current(), false);
      }
   }

   public void drawRgbaTextureWithUVRounded(int texture, float x, float y, float w, float h, float u0, float v0,
         float u1, float v1, float rounding) {
      this.ensureFrame();
      if (texture > 0) {
         this.backend.drawRgbaTexturedQuadRounded(texture, x, y, w, h, u0, v0, u1, v1, rounding, this.modulateColor(-1),
               this.transformStack.current(), false);
      }
   }

   private void drawRgbaTextureInternal(
         int texture, float x, float y, float w, float h, int tintRgba, boolean flipVertically,
         boolean preservePremultipliedColor) {
      this.ensureFrame();
      if (texture > 0) {
         float v0 = flipVertically ? 1.0F : 0.0F;
         float v1 = flipVertically ? 0.0F : 1.0F;
         this.backend
               .drawRgbaTexturedQuad(
                     texture, x, y, w, h, 0.0F, v0, 1.0F, v1, this.modulateColor(tintRgba),
                     this.transformStack.current(), preservePremultipliedColor);
      }
   }

   public void end() {
      if (this.frameBegun) {
         try {
            if (this.batcher != null) {
               this.batcher.flush();
            }

            if (this.backend != null) {
               this.backend.endFrame();
            }

            RenderFrameMetrics.getInstance().endFrame();
         } catch (Exception var5) {
            System.err.println("Error in Renderer2D.end(): " + var5.getMessage());
            var5.printStackTrace();
         } finally {
            this.frameBegun = false;
            this.frameWidth = 0;
            this.frameHeight = 0;
            this.blurPrepared = false;
            this.blurPreparedStrength = 0.0F;
            this.blurPreparedWidth = 0;
            this.blurPreparedHeight = 0;
            this.regionBlurPrepared = false;
            this.regionBlurPreparedStrength = 0.0F;
            this.regionBlurCaptureX = 0;
            this.regionBlurCaptureY = 0;
            this.regionBlurCaptureWidth = 0;
            this.regionBlurCaptureHeight = 0;
            this.clipStack.clear();
            this.transformStack.clear();
            this.resetAlphaStack();
         }
      }
   }

   public void flush() {
      this.ensureFrame();
      this.batcher.flush();
   }

   public void pushClipRect(int x, int y, int w, int h) {
      this.pushRoundedClipRect(x, y, w, h, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public void pushRoundedClipRect(float x, float y, float w, float h, float roundTopLeft, float roundTopRight,
         float roundBottomRight, float roundBottomLeft) {
      this.ensureFrame();
      Renderer2D.ClipState incoming = Renderer2D.ClipState.fromRect(
            x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft, this.transformStack.current());
      Renderer2D.ClipState applied;
      if (this.clipStack.isEmpty()) {
         applied = incoming;
      } else {
         Renderer2D.ClipState current = this.clipStack.peek();
         applied = Renderer2D.ClipState.intersect(current, incoming);
      }

      this.clipStack.push(applied);
      this.applyClipState(applied);
   }

   public void popClipRect() {
      this.ensureFrame();
      if (!this.clipStack.isEmpty()) {
         this.clipStack.pop();
         if (this.clipStack.isEmpty()) {
            this.backend.setScissorEnabled(false);
         } else {
            this.applyClipState(this.clipStack.peek());
         }
      }
   }

   private void applyClipState(Renderer2D.ClipState state) {
      if (state == null) {
         this.backend.setScissorEnabled(false);
      } else {
         this.backend.setScissorEnabled(true);
         this.backend
               .setScissorRect(
                     state.x(), state.y(), state.w(), state.h(), state.roundTopLeft(), state.roundTopRight(),
                     state.roundBottomRight(), state.roundBottomLeft());
      }
   }

   public void rectOutline(float x, float y, float w, float h, int rgbaPremul, float thickness) {
      this.ensureFrame();
      x--;
      y--;
      w += 2.0F;
      h += 2.0F;
      this.batcher
            .enqueueRectOutline(x, y, w, h, 0.0F, 0.0F, 0.0F, 0.0F, this.modulateColor(rgbaPremul),
                  Math.max(1.0F, thickness), this.transformStack.current());
   }

   public void rectOutline(float x, float y, float w, float h, float rounding, int rgbaPremul, float thickness) {
      this.rectOutline(x, y, w, h, rounding, rounding, rounding, rounding, rgbaPremul, thickness);
   }

   public void rectOutline(
         float x,
         float y,
         float w,
         float h,
         float roundTopLeft,
         float roundTopRight,
         float roundBottomRight,
         float roundBottomLeft,
         int rgbaPremul,
         float thickness) {
      this.ensureFrame();
      float[] radii = scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
      x--;
      y--;
      w += 2.0F;
      h += 2.0F;
      normalizeCornerRadii(w, h, radii);
      this.batcher
            .enqueueRectOutline(
                  x, y, w, h, radii[0], radii[1], radii[2], radii[3], this.modulateColor(rgbaPremul),
                  Math.max(1.0F, thickness), this.transformStack.current());
   }

   public void gradient(float x, float y, float w, float h, int c00, int c10, int c11, int c01) {
      this.ensureFrame();
      this.batcher
            .enqueueGradient(
                  x,
                  y,
                  w,
                  h,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  this.modulateColor(c00),
                  this.modulateColor(c10),
                  this.modulateColor(c11),
                  this.modulateColor(c01),
                  this.transformStack.current());
   }

   public void gradient(float x, float y, float w, float h, float rounding, int c00, int c10, int c11, int c01) {
      this.gradient(x, y, w, h, rounding, rounding, rounding, rounding, c00, c10, c11, c01);
   }

   public void gradient(
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
         int c01) {
      this.ensureFrame();
      float[] radii = scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
      normalizeCornerRadii(w, h, radii);
      this.batcher
            .enqueueGradient(
                  x,
                  y,
                  w,
                  h,
                  radii[0],
                  radii[1],
                  radii[2],
                  radii[3],
                  this.modulateColor(c00),
                  this.modulateColor(c10),
                  this.modulateColor(c11),
                  this.modulateColor(c01),
                  this.transformStack.current());
   }

   public void horizontalGradient(float x, float y, float w, float h, int leftColor, int rightColor) {
      this.gradient(x, y, w, h, leftColor, rightColor, rightColor, leftColor);
   }

   public void horizontalGradient(float x, float y, float w, float h, float rounding, int leftColor, int rightColor) {
      this.gradient(x, y, w, h, rounding, leftColor, rightColor, rightColor, leftColor);
   }

   public void horizontalGradient(
         float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft, int leftColor, int rightColor) {
      this.gradient(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft, leftColor, rightColor,
            rightColor, leftColor);
   }

   public void verticalGradient(float x, float y, float w, float h, int topColor, int bottomColor) {
      this.gradient(x, y, w, h, topColor, topColor, bottomColor, bottomColor);
   }

   public void verticalGradient(float x, float y, float w, float h, float rounding, int topColor, int bottomColor) {
      this.gradient(x, y, w, h, rounding, topColor, topColor, bottomColor, bottomColor);
   }

   public void verticalGradient(
         float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft, int topColor, int bottomColor) {
      this.gradient(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft, topColor, topColor,
            bottomColor, bottomColor);
   }

   public void circle(float cx, float cy, float radius, float startDeg, float pct, int rgbaPremul) {
      this.ensureFrame();
      this.batcher.enqueueCircle(cx, cy, radius, startDeg, pct, this.modulateColor(rgbaPremul),
            this.transformStack.current());
   }

   public void shadow(float x, float y, float w, float h, float rounding, float blurStrength, float spread,
         int rgbaPremul) {
      this.shadow(x, y, w, h, rounding, rounding, rounding, rounding, blurStrength, spread, rgbaPremul);
   }

   public void shadow(
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
         int rgbaPremul) {
      this.ensureFrame();
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         float safeBlur = Math.max(0.0F, blurStrength);
         float safeSpread = Math.max(0.0F, spread);
         if (!(safeBlur <= 0.0F) || !(safeSpread <= 0.0F)) {
            float[] radii = scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
            normalizeCornerRadii(w, h, radii);
            this.backend
                  .drawDropShadowRect(
                        x, y, w, h, radii[0], radii[1], radii[2], radii[3], safeBlur, safeSpread,
                        this.modulateColor(rgbaPremul), this.transformStack.current());
         }
      }
   }

   public void blur(float x, float y, float w, float h, float rounding) {
      this.blur(x, y, w, h, rounding, 1.0F);
   }

   public void blur(float x, float y, float w, float h, float rounding, float alpha) {
      this.ensureFrame();
      if (this.blurPrepared) {
         float opacity = clamp01(alpha) * this.currentAlphaMultiplier();
         if (!(opacity <= 1.0E-4F)) {
            this.backend.drawPreparedBlurRounded(x, y, w, h, Math.max(0.0F, rounding), opacity,
                  this.transformStack.current());
         }
      }
   }

   public void blurRegion(float x, float y, float w, float h, float rounding) {
      this.blurRegion(x, y, w, h, rounding, 1.0F);
   }

   public void blurRegion(float x, float y, float w, float h, float rounding, float alpha) {
      this.ensureFrame();
      if (this.regionBlurPrepared) {
         float opacity = clamp01(alpha) * this.currentAlphaMultiplier();
         if (!(opacity <= 1.0E-4F)) {
            this.backend
                  .drawPreparedRegionBlurRounded(
                        x,
                        y,
                        w,
                        h,
                        Math.max(0.0F, rounding),
                        opacity,
                        this.transformStack.current(),
                        this.regionBlurCaptureX,
                        this.regionBlurCaptureY,
                        this.regionBlurCaptureWidth,
                        this.regionBlurCaptureHeight);
         }
      }
   }

   public void glass(
         float x,
         float y,
         float w,
         float h,
         float radius,
         int fresnelArgb,
         float fresnelPower,
         float baseAlpha,
         boolean fresnelInvert,
         float fresnelMix,
         float distortStrength,
         float globalAlpha) {
      this.glass(x, y, w, h, radius, radius, radius, radius, fresnelArgb, fresnelPower, baseAlpha, fresnelInvert,
            fresnelMix, distortStrength, globalAlpha);
   }

   public void glass(
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
         float globalAlpha) {
      this.ensureFrame();
      float alpha = clamp01(globalAlpha);
      if (alpha <= 1.0E-4F) {
         return;
      }

      this.batcher.flush();
      boolean drew = this.backend.drawGlass(
            x,
            y,
            w,
            h,
            roundTopLeft,
            roundTopRight,
            roundBottomRight,
            roundBottomLeft,
            fresnelArgb,
            fresnelPower,
            baseAlpha,
            fresnelInvert,
            fresnelMix,
            distortStrength,
            alpha * this.currentAlphaMultiplier(),
            this.transformStack.current());
      if (!drew) {

         int fallbackAlpha = (int) (Math.min(0.16F, alpha * 0.16F) * 255.0F);
         this.rect(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft,
               ColorUtil.rgba(10, 14, 22, fallbackAlpha));
      }
   }

   public void liquidGlass(float x, float y, float w, float h, float radius, float distortStrength, float globalAlpha) {
      this.glass(x, y, w, h, radius, Renderer2D.ColorUtil.rgba(255, 255, 255, 150), 42.0F, 0.16F, true, 0.22F,
            distortStrength, globalAlpha);
   }

   public void glassOutline(
         float x,
         float y,
         float w,
         float h,
         float radius,
         float thickness,
         int tintArgb,
         int fresnelArgb,
         float fresnelPower,
         float baseAlpha,
         boolean fresnelInvert,
         float fresnelMix,
         float distortStrength,
         float shinePhase,
         float globalAlpha) {
      this.glassOutline(x, y, w, h, radius, radius, radius, radius, thickness, tintArgb, fresnelArgb, fresnelPower,
            baseAlpha, fresnelInvert, fresnelMix, distortStrength, shinePhase, globalAlpha);
   }

   public void glassOutline(
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
         float globalAlpha) {
      this.ensureFrame();
      float alpha = clamp01(globalAlpha);
      if (alpha <= 1.0E-4F) {
         return;
      }

      this.batcher.flush();
      boolean drew = this.backend.drawGlassOutline(
            x,
            y,
            w,
            h,
            roundTopLeft,
            roundTopRight,
            roundBottomRight,
            roundBottomLeft,
            thickness,
            tintArgb,
            fresnelArgb,
            fresnelPower,
            baseAlpha,
            fresnelInvert,
            fresnelMix,
            distortStrength,
            shinePhase,
            alpha * this.currentAlphaMultiplier(),
            this.transformStack.current());
      if (!drew) {

         int fallbackAlpha = (int)(Math.min(0.58F, alpha * 0.58F) * 255.0F);
         this.rectOutline(
            x,
            y,
            w,
            h,
            roundTopLeft,
            roundTopRight,
            roundBottomRight,
            roundBottomLeft,
            ColorUtil.rgba(255, 255, 255, fallbackAlpha),
            Math.max(1.0F, thickness)
         );
      }
   }

   public boolean logoGlass(int textureId, float x, float y, float w, float h, float globalAlpha) {
      float alpha = clamp01(globalAlpha) * this.currentAlphaMultiplier();
      if (textureId <= 0 || !(w > 0.0F) || !(h > 0.0F) || alpha <= 1.0E-4F) {
         return false;
      }
      this.ensureFrame();
      this.batcher.flush();
      GlassMoonRenderer.BlurInfo blur = null;
      if (this.backend instanceof GlBackend gl) {
         blur = new GlassMoonRenderer.BlurInfo(
               gl.getPreparedBlurTexture(),
               gl.getPreparedBlurWidth(),
               gl.getPreparedBlurHeight(),
               gl.getPreparedBlurScaleX(),
               gl.getPreparedBlurScaleY());
      }
      return GlassMoonRenderer.draw(textureId, blur, x, y, w, h, alpha, this.transformStack.current());
   }

   public void prepareBlur(float strength) {
      this.ensureFrame();
      int width = this.frameWidth;
      int height = this.frameHeight;
      if (width > 0 && height > 0) {
         float radius = Math.max(0.5F, strength);
         boolean alreadyPrepared = this.blurPrepared
               && this.blurPreparedWidth == width
               && this.blurPreparedHeight == height
               && Math.abs(this.blurPreparedStrength - radius) <= 0.05F;
         if (!alreadyPrepared) {
            this.backend.prepareScreenBlur(width, height, radius);
            this.blurPrepared = true;
            this.blurPreparedStrength = radius;
            this.blurPreparedWidth = width;
            this.blurPreparedHeight = height;
         }
      } else {
         this.blurPrepared = false;
         this.blurPreparedWidth = 0;
         this.blurPreparedHeight = 0;
      }
   }

   public static void setupRender(boolean bloom) {
      if (bloom) {
         GlStateManager._enableBlend();
         GL11.glBlendFunc(770, 771);
         GlStateManager._disableCull();
         GlStateManager._blendFuncSeparate(770, 771, 1, 0);
         GlStateManager._colorMask(true, true, true, true);
      } else {
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._enableBlend();
      }
   }

   public static void endRender(boolean bloom) {
      if (bloom) {
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._blendFuncSeparate(770, 771, 1, 0);
         GlStateManager._enableCull();
         GlStateManager._disableBlend();
      } else {
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._enableBlend();
      }
   }

   public static void endBuilding(BufferBuilder bb) {
   }

   public void prepareBlurRegion(float x, float y, float w, float h, float strength) {
      this.ensureFrame();
      if (this.frameWidth <= 0 || this.frameHeight <= 0) {
         this.regionBlurPrepared = false;
         this.regionBlurCaptureWidth = 0;
         this.regionBlurCaptureHeight = 0;
      } else if (!(w <= 0.0F) && !(h <= 0.0F)) {
         float[] matrix = this.transformStack.current();
         Renderer2D.Bounds bounds = computeTransformedBounds(matrix, x, y, w, h);
         int captureLeft = clampToViewportFloor(bounds.minX, this.frameWidth);
         int captureTop = clampToViewportFloor(bounds.minY, this.frameHeight);
         int captureRight = clampToViewportCeil(bounds.maxX, this.frameWidth);
         int captureBottom = clampToViewportCeil(bounds.maxY, this.frameHeight);
         int captureWidth = Math.max(0, captureRight - captureLeft);
         int captureHeight = Math.max(0, captureBottom - captureTop);
         if (captureWidth > 0 && captureHeight > 0) {
            float radius = Math.max(0.5F, strength);
            boolean alreadyPrepared = this.regionBlurPrepared
                  && this.regionBlurCaptureX == captureLeft
                  && this.regionBlurCaptureY == captureTop
                  && this.regionBlurCaptureWidth == captureWidth
                  && this.regionBlurCaptureHeight == captureHeight
                  && Math.abs(this.regionBlurPreparedStrength - radius) <= 0.05F;
            if (!alreadyPrepared) {
               boolean success = this.backend.prepareRegionBlur(captureLeft, captureTop, captureWidth, captureHeight,
                     radius);
               this.regionBlurPrepared = success;
               if (success) {
                  this.regionBlurPreparedStrength = radius;
                  this.regionBlurCaptureX = captureLeft;
                  this.regionBlurCaptureY = captureTop;
                  this.regionBlurCaptureWidth = captureWidth;
                  this.regionBlurCaptureHeight = captureHeight;
               } else {
                  this.regionBlurPreparedStrength = 0.0F;
                  this.regionBlurCaptureWidth = 0;
                  this.regionBlurCaptureHeight = 0;
               }
            }
         } else {
            this.regionBlurPrepared = false;
            this.regionBlurCaptureWidth = 0;
            this.regionBlurCaptureHeight = 0;
         }
      } else {
         this.regionBlurPrepared = false;
         this.regionBlurCaptureWidth = 0;
         this.regionBlurCaptureHeight = 0;
      }
   }

   private static int clampToViewportFloor(float value, int viewportMax) {
      int floored = (int) Math.floor(value);
      if (floored < 0) {
         return 0;
      } else {
         return floored > viewportMax ? viewportMax : floored;
      }
   }

   private static int clampToViewportCeil(float value, int viewportMax) {
      int ceiled = (int) Math.ceil(value);
      if (ceiled < 0) {
         return 0;
      } else {
         return ceiled > viewportMax ? viewportMax : ceiled;
      }
   }

   private static Renderer2D.Bounds computeTransformedBounds(float[] matrix, float x, float y, float w, float h) {
      float x1 = x + w;
      float y1 = y + h;
      float wx0y0x = transformX(matrix, x, y);
      float wx0y0y = transformY(matrix, x, y);
      float wx1y0x = transformX(matrix, x1, y);
      float wx1y0y = transformY(matrix, x1, y);
      float wx1y1x = transformX(matrix, x1, y1);
      float wx1y1y = transformY(matrix, x1, y1);
      float wx0y1x = transformX(matrix, x, y1);
      float wx0y1y = transformY(matrix, x, y1);
      float minX = Math.min(Math.min(wx0y0x, wx1y0x), Math.min(wx1y1x, wx0y1x));
      float maxX = Math.max(Math.max(wx0y0x, wx1y0x), Math.max(wx1y1x, wx0y1x));
      float minY = Math.min(Math.min(wx0y0y, wx1y0y), Math.min(wx1y1y, wx0y1y));
      float maxY = Math.max(Math.max(wx0y0y, wx1y0y), Math.max(wx1y1y, wx0y1y));
      return new Renderer2D.Bounds(minX, minY, maxX, maxY);
   }

   private static float transformX(float[] matrix, float px, float py) {
      return matrix != null && matrix.length >= 6 ? matrix[0] * px + matrix[1] * py + matrix[2] : px;
   }

   private static float transformY(float[] matrix, float px, float py) {
      return matrix != null && matrix.length >= 6 ? matrix[3] * px + matrix[4] * py + matrix[5] : py;
   }

   public void setTransform(float[] m3) {
      this.ensureFrame();
      this.transformStack.clear();
      this.transformStack.replaceTop(m3);
   }

   public void pushRotation(float degrees) {
      this.ensureFrame();
      this.transformStack.pushRotation(degrees);
   }

   public void popRotation() {
      this.ensureFrame();
      this.transformStack.pop();
   }

   public void pushTranslation(float tx, float ty) {
      this.ensureFrame();
      this.transformStack.pushTranslation(tx, ty);
   }

   public void popTransform() {
      this.ensureFrame();
      this.transformStack.pop();
   }

   public void pushScale(float scale) {
      this.pushScale(scale, scale);
   }

   public void pushScale(float sx, float sy) {
      this.ensureFrame();
      this.transformStack.pushScale(sx, sy, 0.0F, 0.0F);
   }

   public void pushScaleCentered(float scale) {
      this.pushScaleCentered(scale, scale);
   }

   public void pushScaleCentered(float sx, float sy) {
      this.ensureFrame();
      if (this.frameWidth > 0 && this.frameHeight > 0) {
         this.transformStack.pushScale(sx, sy, this.frameWidth * 0.5F, this.frameHeight * 0.5F);
      } else {
         throw new IllegalStateException(
               "Cannot compute frame center before begin(width, height) is called with positive dimensions");
      }
   }

   public void pushScale(float scale, float originX, float originY) {
      this.pushScale(scale, scale, originX, originY);
   }

   public void pushScale(float sx, float sy, float originX, float originY) {
      this.ensureFrame();
      this.transformStack.pushScale(sx, sy, originX, originY);
   }

   public void popScale() {
      this.ensureFrame();
      this.transformStack.pop();
   }

   public void pushAlpha(float alpha) {
      this.ensureFrame();
      float parent = this.currentAlphaMultiplier();
      float clamped = clamp01(alpha);
      this.alphaStack.push(parent * clamped);
   }

   public void popAlpha() {
      this.ensureFrame();
      if (this.alphaStack.size() > 1) {
         this.alphaStack.pop();
      }
   }

   public void registerTextRenderer(String fontId, TextRenderer tr) {
      if (tr != null) {
         this.idToTextRenderer.put(fontId, tr);
      }
   }

   public void registerTextRenderer(FontObject fo, TextRenderer tr) {
      if (tr != null) {
         this.idToTextRenderer.put(fo.id, tr);
      }
   }

   public TransformStack getTransformStack() {
      return this.transformStack;
   }

   public void text(FontObject fo, float x, float y, float size, String s, int rgbaPremul) {
      this.ensureFrame();
      if (fo == null) {
         throw new IllegalArgumentException("FontObject must not be null");
      } else if (!(size <= 0.0F)) {
         TextRenderer tr = this.idToTextRenderer.get(fo.id);
         if (tr != null) {
            tr.drawText(x, y, size / 2.0F, s, this.modulateColor(rgbaPremul), this.transformStack.current());
         }
      }
   }

   public void text(FontObject fo, float x, float y, float size, String s, int rgbaPremul, String alignKey) {
      this.ensureFrame();
      if (fo == null) {
         throw new IllegalArgumentException("FontObject must not be null");
      } else if (!(size <= 0.0F)) {
         TextRenderer tr = this.idToTextRenderer.get(fo.id);
         if (tr != null) {
            tr.drawText(x, y, size / 2.0F, s, this.modulateColor(rgbaPremul), alignKey, this.transformStack.current());
         }
      }
   }

   public void textOutline(FontObject fo, float x, float y, float size, String s, int rgbaPremul, float outlineWidth) {
      this.textOutline(fo, x, y, size, s, rgbaPremul, outlineWidth, "l");
   }

   public void textOutline(
         FontObject fo,
         float x,
         float y,
         float size,
         String s,
         int rgbaPremul,
         float outlineWidth,
         String alignKey) {
      this.ensureFrame();
      if (fo == null) {
         throw new IllegalArgumentException("FontObject must not be null");
      } else if (!(size <= 0.0F)) {
         TextRenderer tr = this.idToTextRenderer.get(fo.id);
         if (tr != null) {
            tr.drawTextOutline(
                  x,
                  y,
                  size / 2.0F,
                  s,
                  this.modulateColor(rgbaPremul),
                  outlineWidth,
                  alignKey,
                  this.transformStack.current());
         }
      }
   }

   public TextRenderer.TextMetrics measureText(FontObject fo, String text, float size) {
      if (fo == null) {
         throw new IllegalArgumentException("FontObject must not be null");
      } else if (size <= 0.0F) {
         return new TextRenderer.TextMetrics(0.0F, 0.0F);
      } else {
         TextRenderer tr = this.idToTextRenderer.get(fo.id);
         if (tr == null) {
            return new TextRenderer.TextMetrics(0.0F, 0.0F);
         } else {
            String content = text == null ? "" : text;
            return tr.measureText(content, size / 2.0F);
         }
      }
   }

   private void resetAlphaStack() {
      this.alphaStack.clear();
      this.alphaStack.push(1.0F);
   }

   private float currentAlphaMultiplier() {
      return this.alphaStack.isEmpty() ? 1.0F : this.alphaStack.peek();
   }

   private int modulateColor(int rgbaPremul) {
      float factor = this.currentAlphaMultiplier();
      if (factor >= 0.999F) {
         return rgbaPremul;
      } else {
         int a = rgbaPremul >>> 24 & 0xFF;
         int r = rgbaPremul >>> 16 & 0xFF;
         int g = rgbaPremul >>> 8 & 0xFF;
         int b = rgbaPremul & 0xFF;
         int na = scaleChannel(a, factor);
         int nr = scaleChannel(r, factor);
         int ng = scaleChannel(g, factor);
         int nb = scaleChannel(b, factor);
         return na << 24 | nr << 16 | ng << 8 | nb;
      }
   }

   private static int scaleChannel(int value, float factor) {
      float scaled = value * factor;
      if (scaled <= 0.0F) {
         return 0;
      } else {
         return scaled >= 255.0F ? 255 : Math.round(scaled);
      }
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      } else {
         return value > 1.0F ? 1.0F : value;
      }
   }

   private static float[] scratchRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
      return new float[] { topLeft, topRight, bottomRight, bottomLeft };
   }

   private static void normalizeCornerRadii(float w, float h, float[] radii) {
      if (radii != null && radii.length >= 4) {
         float absW = Math.abs(w);
         float absH = Math.abs(h);

         for (int i = 0; i < 4; i++) {
            float value = radii[i];
            if (!Float.isFinite(value)) {
               value = 0.0F;
            }

            radii[i] = Math.max(0.0F, value);
         }

         if (!(absW <= 0.0F) && !(absH <= 0.0F)) {
            float maxR = Math.min(absW, absH) * 0.5F;

            for (int i = 0; i < 4; i++) {
               radii[i] = Math.min(radii[i], maxR);
            }
         } else {
            Arrays.fill(radii, 0.0F);
         }
      } else {
         throw new IllegalArgumentException("radii");
      }
   }

   private static boolean nearlyEqual(float a, float b) {
      return Math.abs(a - b) <= 1.0E-4F;
   }

   private static boolean nearlyZero(float value) {
      return Math.abs(value) <= 1.0E-4F;
   }

   private static boolean isIdentityTransform(float[] matrix) {
      return matrix != null && matrix.length >= 9
            ? nearlyEqual(matrix[0], 1.0F)
                  && nearlyZero(matrix[1])
                  && nearlyZero(matrix[2])
                  && nearlyZero(matrix[3])
                  && nearlyEqual(matrix[4], 1.0F)
                  && nearlyZero(matrix[5])
                  && nearlyZero(matrix[6])
                  && nearlyZero(matrix[7])
                  && nearlyEqual(matrix[8], 1.0F)
            : true;
   }

   private static boolean isAxisAlignedTransform(float[] matrix) {
      return matrix != null && matrix.length >= 9
            ? nearlyZero(matrix[1]) && nearlyZero(matrix[3]) && nearlyZero(matrix[6]) && nearlyZero(matrix[7])
                  && nearlyEqual(matrix[8], 1.0F)
            : true;
   }

   private static float transformPointX(float[] matrix, float x, float y) {
      return matrix != null && matrix.length >= 9 ? matrix[0] * x + matrix[1] * y + matrix[2] : x;
   }

   private static float transformPointY(float[] matrix, float x, float y) {
      return matrix != null && matrix.length >= 9 ? matrix[3] * x + matrix[4] * y + matrix[5] : y;
   }

   private static float computeRadiusScale(float[] matrix) {
      if (matrix != null && matrix.length >= 9) {
         float scaleX = Math.abs(matrix[0]);
         float scaleY = Math.abs(matrix[4]);
         float minScale = Math.min(scaleX, scaleY);
         return minScale <= 1.0E-4F ? 0.0F : minScale;
      } else {
         return 1.0F;
      }
   }

   public static void setupOrientationMatrix(MatrixStack matrix, float x, float y, float z) {
      setupOrientationMatrix(matrix, (double) x, (double) y, (double) z);
   }

   public static void setupOrientationMatrix(MatrixStack matrix, double x, double y, double z) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      Vec3d renderPos = camera.getCameraPos();
      matrix.translate(x - renderPos.x, y - renderPos.y, z - renderPos.z);
   }

   public static Vector2d project2D(double x, double y, double z) {
      return sl.selene.util.render.world.WorldProjection.project(x, y, z);
   }

   @Environment(EnvType.CLIENT)
   private record Bounds(float minX, float minY, float maxX, float maxY) {
   }

   @Environment(EnvType.CLIENT)
   private record ClipState(int x, int y, int w, int h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft) {
      private static Renderer2D.ClipState fromRect(
            float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
            float roundBottomLeft) {
         return fromRect(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft, null);
      }

      private static Renderer2D.ClipState fromRect(
            float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
            float roundBottomLeft, float[] transform) {
         if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(w) && Float.isFinite(h)) {
            boolean hasTransform = transform != null && transform.length >= 9
                  && !Renderer2D.isIdentityTransform(transform);
            float[] radii = Renderer2D.scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
            Renderer2D.normalizeCornerRadii(Math.abs(w), Math.abs(h), radii);
            if (!hasTransform) {
               float left = (float) Math.floor(Math.min(x, x + w));
               float top = (float) Math.floor(Math.min(y, y + h));
               float right = (float) Math.ceil(Math.max(x, x + w));
               float bottom = (float) Math.ceil(Math.max(y, y + h));
               int ix = (int) left;
               int iy = (int) top;
               int iw = Math.max(0, (int) (right - left));
               int ih = Math.max(0, (int) (bottom - top));
               return iw > 0 && ih > 0
                     ? new Renderer2D.ClipState(ix, iy, iw, ih, radii[0], radii[1], radii[2], radii[3])
                     : new Renderer2D.ClipState(ix, iy, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
            } else {
               float x2 = x + w;
               float y2 = y + h;
               float[] xs = new float[] { x, x2, x, x2 };
               float[] ys = new float[] { y, y, y2, y2 };
               float minX = Float.POSITIVE_INFINITY;
               float minY = Float.POSITIVE_INFINITY;
               float maxX = Float.NEGATIVE_INFINITY;
               float maxY = Float.NEGATIVE_INFINITY;

               for (int i = 0; i < 4; i++) {
                  float tx = Renderer2D.transformPointX(transform, xs[i], ys[i]);
                  float ty = Renderer2D.transformPointY(transform, xs[i], ys[i]);
                  if (!Float.isFinite(tx) || !Float.isFinite(ty)) {
                     return new Renderer2D.ClipState(0, 0, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
                  }

                  if (tx < minX) {
                     minX = tx;
                  }

                  if (tx > maxX) {
                     maxX = tx;
                  }

                  if (ty < minY) {
                     minY = ty;
                  }

                  if (ty > maxY) {
                     maxY = ty;
                  }
               }

               float left = (float) Math.floor(Math.min(minX, maxX));
               float top = (float) Math.floor(Math.min(minY, maxY));
               float right = (float) Math.ceil(Math.max(minX, maxX));
               float bottom = (float) Math.ceil(Math.max(minY, maxY));
               int ix = (int) left;
               int iy = (int) top;
               int iw = Math.max(0, (int) (right - left));
               int ih = Math.max(0, (int) (bottom - top));
               if (iw > 0 && ih > 0) {
                  if (Renderer2D.isAxisAlignedTransform(transform)) {
                     float radiusScale = Renderer2D.computeRadiusScale(transform);
                     if (radiusScale > 0.0F) {
                        for (int i = 0; i < radii.length; i++) {
                           radii[i] *= radiusScale;
                        }
                     } else {
                        Arrays.fill(radii, 0.0F);
                     }
                  } else {
                     Arrays.fill(radii, 0.0F);
                  }

                  Renderer2D.normalizeCornerRadii(Math.abs(right - left), Math.abs(bottom - top), radii);
                  return new Renderer2D.ClipState(ix, iy, iw, ih, radii[0], radii[1], radii[2], radii[3]);
               } else {
                  return new Renderer2D.ClipState(ix, iy, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
               }
            }
         } else {
            return new Renderer2D.ClipState(0, 0, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
         }
      }

      private static Renderer2D.ClipState intersect(Renderer2D.ClipState a, Renderer2D.ClipState b) {
         if (a == null) {
            return b;
         } else if (b == null) {
            return a;
         } else {
            int nx = Math.max(a.x, b.x);
            int ny = Math.max(a.y, b.y);
            int nr = Math.min(a.x + a.w, b.x + b.w);
            int nb = Math.min(a.y + a.h, b.y + b.h);
            int nw = Math.max(0, nr - nx);
            int nh = Math.max(0, nb - ny);
            if (nw <= 0 || nh <= 0) {
               return new Renderer2D.ClipState(nx, ny, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
            } else if (matchesRect(nx, ny, nw, nh, b)) {
               return new Renderer2D.ClipState(nx, ny, nw, nh, b.roundTopLeft, b.roundTopRight, b.roundBottomRight,
                     b.roundBottomLeft);
            } else {
               return matchesRect(nx, ny, nw, nh, a)
                     ? new Renderer2D.ClipState(nx, ny, nw, nh, a.roundTopLeft, a.roundTopRight, a.roundBottomRight,
                           a.roundBottomLeft)
                     : new Renderer2D.ClipState(nx, ny, nw, nh, 0.0F, 0.0F, 0.0F, 0.0F);
            }
         }
      }

      private static boolean matchesRect(int x, int y, int w, int h, Renderer2D.ClipState other) {
         return other != null && other.x == x && other.y == y && other.w == w && other.h == h;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class ColorUtil {

      public static final int ACCENT = rgba(255, 255, 255, 255);
      public static final int BLACK = rgba(0, 0, 0, 255);

      public static final int RIM = rgba(255, 255, 255, 255);
      public static final int GLASS_BASE = rgba(16, 20, 30, 255);
      public static final int GLASS_BASE2 = rgba(34, 40, 56, 255);
      public static final int TEXT = rgba(255, 255, 255, 255);

      public static final int TEXT2 = rgba(255, 255, 255, 255);

      public static float getRed(int color) {
         return (color >> 16 & 0xFF) / 255.0F;
      }

      public static float getGreen(int color) {
         return (color >> 8 & 0xFF) / 255.0F;
      }

      public static float getBlue(int color) {
         return (color & 0xFF) / 255.0F;
      }

      public static float getAlpha(int color) {
         return (color >> 24 & 0xFF) / 255.0F;
      }

      public static Color injectAlpha(Color color, int alpha) {
         return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
      }

      public static Color TwoColoreffect(Color color, Color color2, double n) {
         float clamp = MathHelper.clamp((float) Math.sin((Math.PI * 6) * (n / 4.0 % 1.0)) / 2.0F + 0.5F, 0.0F, 1.0F);
         return new Color(
               MathHelper.lerp(color.getRed() / 255.0F, color2.getRed() / 255.0F, clamp),
               MathHelper.lerp(color.getGreen() / 255.0F, color2.getGreen() / 255.0F, clamp),
               MathHelper.lerp(color.getBlue() / 255.0F, color2.getBlue() / 255.0F, clamp),
               MathHelper.lerp(color.getAlpha() / 255.0F, color2.getAlpha() / 255.0F, clamp));
      }

      public static Color setAlpha(Color c, int alpha) {
         return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
      }

      public static int setAlpha(int color, int alpha) {
         return color & 16777215 | alpha << 24;
      }

      public static int getClientColor() {
         return ACCENT;
      }

      public static int[] getClientColor(int speed, int alpha) {
         return new int[] { ACCENT, ACCENT, ACCENT, ACCENT };
      }

      public static int getBackGroundColor(int speed, int index) {
         return GLASS_BASE;
      }

      public static int getBackGroundTwoColor(int speed, int index) {
         return GLASS_BASE2;
      }

      public static int getOutLineColor(int speed, int index) {
         return RIM;
      }

      public static int getMainColor(int speed, int index) {
         return ACCENT;
      }

      public static int getTextColor(int speed, int index) {
         return TEXT;
      }

      public static int getTextTwoColor(int speed, int index) {
         return TEXT2;
      }

      public Color interpolate(Color color1, Color color2, double amount) {
         amount = 1.0 - amount;
         amount = (float) MathHelper.clamp(amount, 0.0, 1.0);
         return new Color(
               Interpolator.lerp(color1.getRed(), color2.getRed(), amount),
               Interpolator.lerp(color1.getGreen(), color2.getGreen(), amount),
               Interpolator.lerp(color1.getBlue(), color2.getBlue(), amount),
               Interpolator.lerp(color1.getAlpha(), color2.getAlpha(), amount));
      }

      public static Color interpolateTwoColors(int speed, int index, Color start, Color end, boolean trueColor) {
         int angle = 0;
         if (speed == 0) {
            angle = index % 360;
         } else {
            angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
         }

         angle = (angle >= 180 ? 360 - angle : angle) * 2;
         return trueColor ? interpolateColorHue(start, end, angle / 360.0F)
               : interpolateColorC(start, end, angle / 360.0F);
      }

      public static Color interpolateColorHue(Color color1, Color color2, float amount) {
         amount = Math.min(1.0F, Math.max(0.0F, amount));
         float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
         float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);
         Color resultColor = Color.getHSBColor(
               MathHelper.lerp(color1HSB[0], color2HSB[0], amount),
               MathHelper.lerp(color1HSB[1], color2HSB[1], amount),
               MathHelper.lerp(color1HSB[2], color2HSB[2], amount));
         return new Color(
               resultColor.getRed(),
               resultColor.getGreen(),
               resultColor.getBlue(),
               (int) MathHelper.lerp((float) color1.getAlpha(), (float) color2.getAlpha(), amount));
      }

      public static Color interpolateColorC(Color color1, Color color2, float amount) {
         return new Color(
               MathHelper.lerp((float) color1.getRed(), (float) color2.getRed(), amount),
               MathHelper.lerp((float) color1.getGreen(), (float) color2.getGreen(), amount),
               MathHelper.lerp((float) color1.getBlue(), (float) color2.getBlue(), amount),
               MathHelper.lerp((float) color1.getAlpha(), (float) color2.getAlpha(), amount));
      }

      public static int gradient2(int color1, int color2, int speed, int index) {
         Color col1 = new Color(color1);
         Color col2 = new Color(color2);
         double angle = (System.currentTimeMillis() / speed + index) % 360L;
         double var13;
         float ratio = (float) ((var13 = angle % 360.0) / 360.0);
         int red = (int) (col1.getRed() * (1.0F - ratio) + col2.getRed() * ratio);
         int green = (int) (col1.getGreen() * (1.0F - ratio) + col2.getGreen() * ratio);
         int blue = (int) (col1.getBlue() * (1.0F - ratio) + col2.getBlue() * ratio);
         Color interpolatedColor = new Color(red, green, blue);
         return interpolatedColor.getRGB();
      }

      public static int interpolate(int color1, int color2, double amount) {
         amount = (float) MathHelper.clamp(amount, 0.0, 1.0);
         return getColor(
               Interpolator.lerp(red(color1), red(color2), amount),
               Interpolator.lerp(green(color1), green(color2), amount),
               Interpolator.lerp(blue(color1), blue(color2), amount),
               Interpolator.lerp(alpha(color1), alpha(color2), amount));
      }

      public static int[] getRainbowColor(int speed) {
         int[] color1 = new int[4];
         if (speed == 0) {
            speed = 1;
         }

         color1[0] = rainbow(speed, 1, 1.0F, 1.0F, 1.0F);
         color1[1] = rainbow(speed, 90, 1.0F, 1.0F, 1.0F);
         color1[2] = rainbow(speed, 180, 1.0F, 1.0F, 1.0F);
         color1[3] = rainbow(speed, 270, 1.0F, 1.0F, 1.0F);
         return color1;
      }

      public static int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
         int angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
         float hue = angle / 360.0F;
         int color = Color.HSBtoRGB(hue, saturation, brightness);
         return getColor(red(color), green(color), blue(color), Math.max(0, Math.min(255, (int) (opacity * 255.0F))));
      }

      public static int gradient(int speed, int index, int... colors) {
         int angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
         angle = (angle > 180 ? 360 - angle : angle) + 180;
         int colorIndex = (int) (angle / 360.0F * colors.length);
         if (colorIndex == colors.length) {
            colorIndex--;
         }

         int color1 = colors[colorIndex];
         int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
         return interpolateColor(color1, color2, angle / 360.0F * colors.length - colorIndex);
      }

      public static int interpolateColor(int color1, int color2, double offset) {
         float[] rgba1 = getRGBAf(color1);
         float[] rgba2 = getRGBAf(color2);
         double r = rgba1[0] + (rgba2[0] - rgba1[0]) * offset;
         double g = rgba1[1] + (rgba2[1] - rgba1[1]) * offset;
         double b = rgba1[2] + (rgba2[2] - rgba1[2]) * offset;
         double a = rgba1[3] + (rgba2[3] - rgba1[3]) * offset;
         return rgba((int) (r * 255.0), (int) (g * 255.0), (int) (b * 255.0), (int) (a * 255.0));
      }

      public static float[] getRGBAf(int c) {
         return new float[] { red(c) / 255.0F, green(c) / 255.0F, blue(c) / 255.0F, alpha(c) / 255.0F };
      }

      public static int skyRainbow(int speed, int index) {
         double angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
         double var4;
         return Color
               .getHSBColor((var4 = angle % 360.0) / 360.0 < 0.5 ? -((float) (var4 / 360.0)) : (float) (var4 / 360.0),
                     0.5F, 1.0F)
               .hashCode();
      }

      public static int[] getAstolfoColor(int speed) {
         int[] color1 = new int[4];
         if (speed == 0) {
            int var2 = 1;
         }

         color1[0] = skyRainbow(25, 1);
         color1[1] = skyRainbow(25, 90);
         color1[2] = skyRainbow(25, 180);
         color1[3] = skyRainbow(25, 270);
         return color1;
      }

      public static int applyOpacity(int n, float f) {
         return rgba2(getRedInt(n), getGreenInt(n), getBlueInt(n), (int) (getAlphaInt(n) * f / 255.0F));
      }

      public static int rgba2(int n, int n2, int n3, int n4) {
         return n4 << 24 | n << 16 | n2 << 8 | n3;
      }

      public static int getRedInt(int n) {
         return n >> 16 & 0xFF;
      }

      public static int getGreenInt(int n) {
         return n >> 8 & 0xFF;
      }

      public static int getBlueInt(int n) {
         return n & 0xFF;
      }

      public static int getAlphaInt(int n) {
         return n >> 24 & 0xFF;
      }

      public static float[] getColorComps(Color color) {
         return new float[] { color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F,
               color.getAlpha() / 255.0F };
      }

      public static int getClientColorOne(int speed, int index) {
         return ACCENT;
      }

      public static int swapAlpha(int color, float alpha) {
         int f = color >> 16 & 0xFF;
         int f1 = color >> 8 & 0xFF;
         int f2 = color & 0xFF;
         return getColor(f, f1, f2, (int) alpha);
      }

      public static Color getColor(int color) {
         int r = color >> 16 & 0xFF;
         int g = color >> 8 & 0xFF;
         int b = color & 0xFF;
         int a = color >> 24 & 0xFF;
         return new Color(r, g, b, a);
      }

      public static int replAlpha(int c, int a) {
         return getColor(red(c), green(c), blue(c), a);
      }

      public static int multDark(int c, float brpc) {
         return getColor(red(c) * brpc, green(c) * brpc, blue(c) * brpc, (float) alpha(c));
      }

      public static int red(int c) {
         return c >> 16 & 0xFF;
      }

      public static int green(int c) {
         return c >> 8 & 0xFF;
      }

      public static int blue(int c) {
         return c & 0xFF;
      }

      public static int alpha(int c) {
         return c >> 24 & 0xFF;
      }

      public static int getColor(float r, float g, float b, float a) {
         return new Color((int) r, (int) g, (int) b, (int) a).getRGB();
      }

      public static int getColor(int red, int green, int blue) {
         return getColor(red, green, blue, 255);
      }

      public static int getColor(int red, int green, int blue, int alpha) {
         int color = 0;
         color |= alpha << 24;
         color |= red << 16;
         color |= green << 8;
         return color | blue;
      }

      public static int getRedFromColor(int color) {
         return color >> 16 & 0xFF;
      }

      public static int getGreenFromColor(int color) {
         return color >> 8 & 0xFF;
      }

      public static int getBlueFromColor(int color) {
         return color & 0xFF;
      }

      public static int getAlphaFromColor(int color) {
         return color >> 24 & 0xFF;
      }

      public static float[] rgb(int color) {
         return new float[] { (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
               (color >> 24 & 0xFF) / 255.0F };
      }

      public static int rgba(int r, int g, int b, int a) {
         return a << 24 | r << 16 | g << 8 | b;
      }

      public static int colorToHex(Color color) {
         int a = color.getAlpha();
         int r = color.getRed();
         int g = color.getGreen();
         int b = color.getBlue();
         return a << 24 | r << 16 | g << 8 | b;
      }

      public static float[] rgba(int color) {
         return new float[] { (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
               (color >> 24 & 0xFF) / 255.0F };
      }
   }
}