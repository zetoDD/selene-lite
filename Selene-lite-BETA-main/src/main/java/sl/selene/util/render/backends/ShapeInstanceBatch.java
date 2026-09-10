package sl.selene.util.render.backends;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ShapeInstanceBatch {
   private static final int MAX_INSTANCES = 4096;
   private static final int INSTANCE_STRIDE = 144;
   private static final int INSTANCE_BUFFER_BYTES = MAX_INSTANCES * INSTANCE_STRIDE;
   private static final float[] IDENTITY_TRANSFORM = new float[] { 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F,
         1.0F };

   private final ByteBuffer instanceBuffer = ByteBuffer.allocateDirect(INSTANCE_BUFFER_BYTES).order(ByteOrder.nativeOrder());
   private int instanceCount;
   private int viewportWidth;
   private int viewportHeight;
   private boolean clipEnabled;
   private int clipX;
   private int clipY;
   private int clipW = Integer.MAX_VALUE;
   private int clipH = Integer.MAX_VALUE;
   private float clipRoundTL;
   private float clipRoundTR;
   private float clipRoundBR;
   private float clipRoundBL;
   private final Map<Integer, Integer> textureToSlot = new HashMap<>();
   private final List<Integer> slotToTexture = new ArrayList<>(16);

   private Runnable flushAction = () -> {
   };

   public void setFlushAction(Runnable flushAction) {
      this.flushAction = flushAction != null ? flushAction : () -> {
      };
   }

   public void beginFrame(int viewportWidth, int viewportHeight) {
      this.viewportWidth = viewportWidth;
      this.viewportHeight = viewportHeight;
      this.instanceCount = 0;
      this.instanceBuffer.clear();
      this.textureToSlot.clear();
      this.slotToTexture.clear();
   }

   public int getViewportWidth() {
      return this.viewportWidth;
   }

   public int getViewportHeight() {
      return this.viewportHeight;
   }

   public void setScissorEnabled(boolean enabled) {
      this.clipEnabled = enabled;
      if (!enabled) {
         this.clipRoundTL = 0.0F;
         this.clipRoundTR = 0.0F;
         this.clipRoundBR = 0.0F;
         this.clipRoundBL = 0.0F;
      }
   }

   public void setScissorRect(int x, int y, int w, int h, float roundTopLeft, float roundTopRight,
         float roundBottomRight, float roundBottomLeft) {
      this.clipX = x;
      this.clipY = y;
      this.clipW = w;
      this.clipH = h;
      this.clipRoundTL = roundTopLeft;
      this.clipRoundTR = roundTopRight;
      this.clipRoundBR = roundBottomRight;
      this.clipRoundBL = roundBottomLeft;
   }

   public int getInstanceCount() {
      return this.instanceCount;
   }

   public boolean isClipEnabled() {
      return this.clipEnabled;
   }

   public int getClipX() {
      return this.clipX;
   }

   public int getClipY() {
      return this.clipY;
   }

   public int getClipW() {
      return this.clipW;
   }

   public int getClipH() {
      return this.clipH;
   }

   public List<Integer> getSlotToTexture() {
      return this.slotToTexture;
   }

   public ByteBuffer prepareFlushBuffer() {
      this.instanceBuffer.limit(this.instanceCount * INSTANCE_STRIDE);
      this.instanceBuffer.position(0);
      return this.instanceBuffer;
   }

   public void afterFlush() {
      this.instanceCount = 0;
      this.instanceBuffer.clear();
      this.textureToSlot.clear();
      this.slotToTexture.clear();
   }

   private void ensureInstanceCapacity() {
      this.ensureInstanceCapacity(1);
   }

   private void ensureInstanceCapacity(int additionalInstances) {
      if (additionalInstances > 0) {
         if (additionalInstances > MAX_INSTANCES) {
            throw new IllegalArgumentException("additionalInstances must be between 1 and 4096");
         }
         if (this.instanceCount + additionalInstances > MAX_INSTANCES) {
            this.flushAction.run();
            this.instanceCount = 0;
            this.instanceBuffer.clear();
            this.textureToSlot.clear();
            this.slotToTexture.clear();
         }
      }
   }

   private static int packColorRgba(int color) {
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = color >>> 24 & 0xFF;
      return a << 24 | b << 16 | g << 8 | r;
   }

   private void writeInstanceEx(
         int type,
         float x,
         float y,
         float w,
         float h,
         int colorTL,
         int colorTR,
         int colorBR,
         int colorBL,
         float roundTL,
         float roundTR,
         float roundBR,
         float roundBL,
         float thickness,
         float[] transform,
         float u0,
         float v0,
         float u1,
         float v1,
         int texSlot,
         float startDeg,
         float arcPct,
         int extraFlags) {
      if (this.instanceCount >= MAX_INSTANCES) {
         throw new IllegalStateException("Instance capacity exceeded without prior ensureInstanceCapacity call");
      }
      int offset = this.instanceCount * INSTANCE_STRIDE;
      this.instanceBuffer.position(offset);
      putVertices(this.instanceBuffer, transform, x, y, w, h);
      int cx = this.clipEnabled ? this.clipX : 0;
      int cy = this.clipEnabled ? this.clipY : 0;
      int cw = this.clipEnabled ? this.clipW : this.viewportWidth;
      int ch = this.clipEnabled ? this.clipH : this.viewportHeight;
      float cRoundTL = this.clipEnabled ? this.clipRoundTL : 0.0F;
      float cRoundTR = this.clipEnabled ? this.clipRoundTR : 0.0F;
      float cRoundBR = this.clipEnabled ? this.clipRoundBR : 0.0F;
      float cRoundBL = this.clipEnabled ? this.clipRoundBL : 0.0F;
      this.instanceBuffer.putInt(cx);
      this.instanceBuffer.putInt(cy);
      this.instanceBuffer.putInt(cw);
      this.instanceBuffer.putInt(ch);
      this.instanceBuffer.putFloat(cRoundTL);
      this.instanceBuffer.putFloat(cRoundTR);
      this.instanceBuffer.putFloat(cRoundBR);
      this.instanceBuffer.putFloat(cRoundBL);
      this.instanceBuffer.putFloat(x);
      this.instanceBuffer.putFloat(y);
      this.instanceBuffer.putFloat(w);
      this.instanceBuffer.putFloat(h);
      this.instanceBuffer.putInt(packColorRgba(colorTL));
      this.instanceBuffer.putInt(packColorRgba(colorTR));
      this.instanceBuffer.putInt(packColorRgba(colorBR));
      this.instanceBuffer.putInt(packColorRgba(colorBL));
      float sanitizedTL = sanitizeRadius(roundTL);
      float sanitizedTR = sanitizeRadius(roundTR);
      float sanitizedBR = sanitizeRadius(roundBR);
      float sanitizedBL = sanitizeRadius(roundBL);
      this.instanceBuffer.putFloat(sanitizedTL);
      this.instanceBuffer.putFloat(sanitizedTR);
      this.instanceBuffer.putFloat(sanitizedBR);
      this.instanceBuffer.putFloat(sanitizedBL);
      this.instanceBuffer.putFloat(u0);
      this.instanceBuffer.putFloat(v0);
      this.instanceBuffer.putFloat(u1);
      this.instanceBuffer.putFloat(v1);
      int flags = type;
      if (type == 1) {
         int th = Math.max(0, Math.min(255, Math.round(thickness)));
         flags = type | th << 2;
      }

      if (type == 2) {
         float var44 = startDeg % 360.0F;
         if (var44 < 0.0F) {
            var44 += 360.0F;
         }

         int encodedStart = Math.max(0, Math.min(255, Math.round(var44 / 360.0F * 255.0F)));
         float clampedPct = Math.max(0.0F, Math.min(1.0F, arcPct));
         int encodedPct = Math.max(0, Math.min(255, Math.round(clampedPct * 255.0F)));
         flags |= encodedStart << 10;
         flags |= encodedPct << 18;
      }

      if (type == 3 && thickness > 0.0F) {
         flags |= 4;
      }

      flags |= extraFlags;
      this.instanceBuffer.putInt(flags);
      this.instanceBuffer.putInt(texSlot);
      this.instanceBuffer.putInt(0);
      this.instanceBuffer.putInt(0);
      this.instanceCount++;
   }

   private static void putVertices(ByteBuffer buffer, float[] matrix, float x, float y, float w, float h) {
      float[] mat = matrix != null && matrix.length >= 6 ? matrix : IDENTITY_TRANSFORM;
      float x1 = x + w;
      float y1 = y + h;
      putVertex(buffer, mat, x, y);
      putVertex(buffer, mat, x1, y);
      putVertex(buffer, mat, x1, y1);
      putVertex(buffer, mat, x, y1);
   }

   private static void putVertex(ByteBuffer buffer, float[] matrix, float px, float py) {
      float worldX = matrix[0] * px + matrix[1] * py + matrix[2];
      float worldY = matrix[3] * px + matrix[4] * py + matrix[5];
      buffer.putFloat(worldX);
      buffer.putFloat(worldY);
   }

   private static float sanitizeRadius(float radius) {
      if (!Float.isFinite(radius)) {
         return 0.0F;
      }
      return radius <= 0.0F ? 0.0F : radius;
   }

   private void writeInstance(
         int type,
         float x,
         float y,
         float w,
         float h,
         int color,
         float rounding,
         float thickness,
         float[] transform,
         float u0,
         float v0,
         float u1,
         float v1,
         int texSlot,
         float startDeg,
         float arcPct) {
      this.writeInstanceEx(
            type,
            x,
            y,
            w,
            h,
            color,
            color,
            color,
            color,
            rounding,
            rounding,
            rounding,
            rounding,
            thickness,
            transform,
            u0,
            v0,
            u1,
            v1,
            texSlot,
            startDeg,
            arcPct,
            0);
   }

   private int textureSlotFor(int texture) {
      Integer slot = this.textureToSlot.get(texture);
      if (slot != null) {
         return slot;
      }
      if (this.slotToTexture.size() >= 16) {
         this.flushAction.run();
         this.textureToSlot.clear();
         this.slotToTexture.clear();
      }

      int newSlot = this.slotToTexture.size();
      this.slotToTexture.add(texture);
      this.textureToSlot.put(texture, newSlot);
      return newSlot;
   }

   public void enqueueRect(
         float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight,
         float roundBottomLeft, int color, float[] transform) {
      this.ensureInstanceCapacity();
      this.writeInstanceEx(
            0,
            x,
            y,
            w,
            h,
            color,
            color,
            color,
            color,
            roundTopLeft,
            roundTopRight,
            roundBottomRight,
            roundBottomLeft,
            0.0F,
            transform,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            -1,
            0.0F,
            1.0F,
            0);
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
      this.ensureInstanceCapacity();
      this.writeInstanceEx(
            1,
            x,
            y,
            w,
            h,
            color,
            color,
            color,
            color,
            roundTopLeft,
            roundTopRight,
            roundBottomRight,
            roundBottomLeft,
            thickness,
            transform,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            -1,
            0.0F,
            1.0F,
            0);
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
      this.ensureInstanceCapacity();
      this.writeInstanceEx(
            0,
            x,
            y,
            w,
            h,
            c00,
            c10,
            c11,
            c01,
            roundTopLeft,
            roundTopRight,
            roundBottomRight,
            roundBottomLeft,
            0.0F,
            transform,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            -1,
            0.0F,
            1.0F,
            0);
   }

   public void enqueueCircle(float cx, float cy, float radius, float startDeg, float pct, int color,
         float[] transform) {
      float size = radius * 2.0F;
      this.ensureInstanceCapacity();
      this.writeInstance(2, cx - radius, cy - radius, size, size, color, 0.0F, 0.0F, transform, 0.0F, 0.0F, 1.0F, 1.0F,
            -1, startDeg, pct);
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
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         float safeBlur = blurStrength > 0.0F ? blurStrength : 0.0F;
         float safeSpread = spread > 0.0F ? spread : 0.0F;
         float padding = safeSpread + safeBlur * 3.0F;
         float expandedX = x - padding;
         float expandedY = y - padding;
         float expandedW = w + padding * 2.0F;
         float expandedH = h + padding * 2.0F;
         if (!(expandedW <= 0.0F) && !(expandedH <= 0.0F)) {
            this.ensureInstanceCapacity();
            this.writeInstanceEx(
                  0,
                  expandedX,
                  expandedY,
                  expandedW,
                  expandedH,
                  rgbaPremul,
                  rgbaPremul,
                  rgbaPremul,
                  rgbaPremul,
                  roundTopLeft,
                  roundTopRight,
                  roundBottomRight,
                  roundBottomLeft,
                  0.0F,
                  transform,
                  w,
                  h,
                  Math.max(safeBlur, 0.001F),
                  safeSpread,
                  0,
                  0.0F,
                  1.0F,
                  67108864);
         }
      }
   }

   public void drawTexturedQuad(int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1,
         int rgbaPremul, float[] transform) {
      this.ensureInstanceCapacity();
      int slot = this.textureSlotFor(texture);
      this.writeInstance(3, x, y, w, h, rgbaPremul, 0.0F, 0.0F, transform, u0, v0, u1, v1, slot, 0.0F, 1.0F);
   }

   public void drawTexturedQuadRounded(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float rounding,
         int rgbaPremul, float[] transform) {
      this.ensureInstanceCapacity();
      int slot = this.textureSlotFor(texture);
      this.writeInstance(3, x, y, w, h, rgbaPremul, rounding, 0.0F, transform, u0, v0, u1, v1, slot, 0.0F, 1.0F);
   }

   public void drawRgbaTexturedQuad(int texture, float x, float y, float w, float h, float u0, float v0, float u1,
         float v1, int rgbaPremul, float[] transform) {
      this.drawRgbaTexturedQuad(texture, x, y, w, h, u0, v0, u1, v1, rgbaPremul, transform, false);
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
      this.ensureInstanceCapacity();
      int slot = this.textureSlotFor(texture);
      int extraFlags = preservePremultipliedColor ? 64 : 0;
      this.writeInstanceEx(
            3, x, y, w, h, rgbaPremul, rgbaPremul, rgbaPremul, rgbaPremul, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, transform, u0,
            v0, u1, v1, slot, 0.0F, 1.0F, extraFlags);
   }

   public void drawRgbaTexturedQuadRounded(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float rounding,
         int rgbaPremul, float[] transform) {
      this.drawRgbaTexturedQuadRounded(texture, x, y, w, h, u0, v0, u1, v1, rounding, rgbaPremul, transform, false);
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
      this.ensureInstanceCapacity();
      int slot = this.textureSlotFor(texture);
      int extraFlags = preservePremultipliedColor ? 64 : 0;
      this.writeInstanceEx(
            3,
            x,
            y,
            w,
            h,
            rgbaPremul,
            rgbaPremul,
            rgbaPremul,
            rgbaPremul,
            rounding,
            rounding,
            rounding,
            rounding,
            1.0F,
            transform,
            u0,
            v0,
            u1,
            v1,
            slot,
            0.0F,
            1.0F,
            extraFlags);
   }

   public void drawRgbaOpaqueTexturedQuadRounded(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float rounding,
         int rgbaPremul, float[] transform) {
      this.ensureInstanceCapacity();
      this.drawRgbaOpaqueTexturedQuadRounded(texture, x, y, w, h, u0, v0, u1, v1, rounding, rgbaPremul, transform,
            false);
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
      this.ensureInstanceCapacity();
      int slot = this.textureSlotFor(texture);
      int extraFlags = 8;
      if (screenSpaceUv) {
         extraFlags |= 32;
      }

      this.writeInstanceEx(
            3,
            x,
            y,
            w,
            h,
            rgbaPremul,
            rgbaPremul,
            rgbaPremul,
            rgbaPremul,
            rounding,
            rounding,
            rounding,
            rounding,
            1.0F,
            transform,
            u0,
            v0,
            u1,
            v1,
            slot,
            0.0F,
            1.0F,
            extraFlags);
   }

   public void drawRgbaOpaqueTexturedQuad(
         int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int rgbaPremul,
         float[] transform) {
      this.ensureInstanceCapacity();
      int slot = this.textureSlotFor(texture);
      int extraFlags = 8;
      this.writeInstanceEx(
            3, x, y, w, h, rgbaPremul, rgbaPremul, rgbaPremul, rgbaPremul, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, transform, u0,
            v0, u1, v1, slot, 0.0F, 1.0F, extraFlags);
   }

   public void enqueueMsdfGlyph(
         int texture, float pxRange, float x, float y, float width, float height, float u0, float v0, float u1,
         float v1, int rgbaColor, float[] transform) {
      this.enqueueMsdfGlyphInternal(texture, pxRange, 0.0F, x, y, width, height, u0, v0, u1, v1, rgbaColor, transform,
            false);
   }

   public void enqueueMsdfGlyphOutline(
         int texture, float pxRange, float outlineWidth, float x, float y, float width, float height, float u0, float v0,
         float u1, float v1, int rgbaColor, float[] transform) {
      this.enqueueMsdfGlyphInternal(texture, pxRange, outlineWidth, x, y, width, height, u0, v0, u1, v1, rgbaColor,
            transform, true);
   }

   private void enqueueMsdfGlyphInternal(
         int texture, float pxRange, float outlineWidth, float x, float y, float width, float height, float u0, float v0,
         float u1, float v1, int rgbaColor, float[] transform, boolean outline) {
      if (texture != 0) {
         this.ensureInstanceCapacity();
         int slot = this.textureSlotFor(texture);
         float clampedRange = pxRange > 0.0F ? pxRange : 0.001F;
         float clampedOutline = Math.max(0.0F, outlineWidth);
         this.writeInstanceEx(
               3,
               x,
               y,
               width,
               height,
               rgbaColor,
               rgbaColor,
               rgbaColor,
               rgbaColor,
               clampedRange,
               outline ? clampedOutline : clampedRange,
               clampedRange,
               clampedRange,
               0.0F,
               transform,
               u0,
               v0,
               u1,
               v1,
               slot,
               0.0F,
               1.0F,
               outline ? 144 : 16);
      }
   }
}