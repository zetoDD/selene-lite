package sl.selene.util.render.text;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.render.backends.RenderBackend;

@Environment(EnvType.CLIENT)
public final class TextRenderer {
   private static final float[] IDENTITY_TRANSFORM = new float[]{1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F};
   private final RenderBackend backend;
   private final MsdfFont font;

   public TextRenderer(RenderBackend backend, MsdfFont font) {
      this.backend = Objects.requireNonNull(backend, "backend");
      this.font = Objects.requireNonNull(font, "font");
   }

   public void drawText(float x, float y, float size, String text, int rgbaPremul) {
      this.drawText(x, y, size, text, rgbaPremul, "l", IDENTITY_TRANSFORM);
   }

   public void drawText(float x, float y, float size, String text, int rgbaPremul, float[] transform) {
      this.drawText(x, y, size, text, rgbaPremul, "l", transform);
   }

   public void drawText(float x, float y, float size, String text, int rgbaPremul, String alignKey) {
      this.drawText(x, y, size, text, rgbaPremul, alignKey, IDENTITY_TRANSFORM);
   }

   public void drawText(float x, float y, float size, String text, int rgbaPremul, String alignKey, float[] transform) {
      this.drawTextInternal(x, y, size, text, rgbaPremul, 0.0F, alignKey, transform);
   }

   public void drawTextOutline(float x, float y, float size, String text, int rgbaPremul, float outlineWidth) {
      this.drawTextOutline(x, y, size, text, rgbaPremul, outlineWidth, "l", IDENTITY_TRANSFORM);
   }

   public void drawTextOutline(
         float x,
         float y,
         float size,
         String text,
         int rgbaPremul,
         float outlineWidth,
         String alignKey,
         float[] transform) {
      this.drawTextInternal(x, y, size, text, rgbaPremul, outlineWidth, alignKey, transform);
   }

   private void drawTextInternal(
         float x,
         float y,
         float size,
         String text,
         int rgbaPremul,
         float outlineWidth,
         String alignKey,
         float[] transform) {
      if (!(size <= 0.0F)) {
         String content = text == null ? "" : text;
         if (!content.isEmpty()) {
            float[] matrix = transform != null && transform.length >= 6 ? transform : IDENTITY_TRANSFORM;
            float scale = size / Math.max(1.0E-6F, this.font.emSize());
            float lineHeight = this.font.lineHeight() * scale;
            float baselineY = y;
            String align = alignKey == null ? "l" : alignKey.toLowerCase();
            int color = rgbaPremul;
            int texture = this.font.textureId();
            float pxRange = this.font.distanceRange();
            float safeOutlineWidth = Math.max(0.0F, outlineWidth);
            String[] lines = content.split("\\n", -1);

            for (String line : lines) {
               float width = this.measureLineWidth(line, scale);
               float startX = x;
               if ("c".equals(align)) {
                  startX = x - width * 0.5F;
               } else if ("r".equals(align)) {
                  startX = x - width;
               }

               this.drawTextLine(startX, baselineY, scale, line, color, matrix, texture, pxRange, safeOutlineWidth);
               baselineY += lineHeight;
            }
         }
      }
   }

   private void drawTextLine(
         float x,
         float baseline,
         float scale,
         String line,
         int color,
         float[] matrix,
         int texture,
         float pxRange,
         float outlineWidth) {
      if (!line.isEmpty()) {
         float penX = x;
         float baselineY = baseline;
         int prevCodepoint = -1;
         int i = 0;

         while (i < line.length()) {
            char ch = line.charAt(i);
            if (ch == '\\' && i + 9 < line.length() && line.charAt(i + 1) == 'c') {
               i += 10;
            } else {
               int cp = line.codePointAt(i);
               int cpLen = Character.charCount(cp);
               i += cpLen;
               MsdfFont.Glyph glyph = this.font.glyph(cp);
               int glyphCode = cp;
               if (glyph == null) {
                  glyph = this.font.glyph(63);
                  glyphCode = 63;
                  if (glyph == null) {
                     continue;
                  }
               }

               if (prevCodepoint != -1) {
                  penX += this.font.kerning(prevCodepoint, glyphCode) * scale;
               }

               if (glyph.renderable) {
                  float x0 = penX + glyph.planeLeft * scale;
                  float y0 = baselineY - glyph.planeTop * scale;
                  float x1 = penX + glyph.planeRight * scale;
                  float y1 = baselineY - glyph.planeBottom * scale;
                  float width = x1 - x0;
                  float height = y1 - y0;
                  if (width > 0.0F && height > 0.0F) {
                     if (outlineWidth > 0.0F) {
                        this.backend.enqueueMsdfGlyphOutline(
                              texture,
                              pxRange,
                              outlineWidth,
                              x0,
                              y0,
                              width,
                              height,
                              glyph.u0,
                              glyph.v1,
                              glyph.u1,
                              glyph.v0,
                              color,
                              matrix);
                     } else {
                        this.backend.enqueueMsdfGlyph(
                              texture,
                              pxRange,
                              x0,
                              y0,
                              width,
                              height,
                              glyph.u0,
                              glyph.v1,
                              glyph.u1,
                              glyph.v0,
                              color,
                              matrix);
                     }
                  }
               }

               penX += glyph.advance * scale;
               prevCodepoint = glyphCode;
            }
         }
      }
   }

   public TextRenderer.TextMetrics measureText(String text, float size) {
      if (size <= 0.0F) {
         return new TextRenderer.TextMetrics(0.0F, 0.0F);
      } else {
         String content = text == null ? "" : text;
         if (content.isEmpty()) {
            return new TextRenderer.TextMetrics(0.0F, 0.0F);
         } else {
            float scale = size / Math.max(1.0E-6F, this.font.emSize());
            float lineHeight = this.font.lineHeight() * scale;
            String[] lines = content.split("\\n", -1);
            float maxWidth = 0.0F;

            for (String line : lines) {
               maxWidth = Math.max(maxWidth, this.measureLineWidth(line, scale));
            }

            float height = Math.max(lineHeight * lines.length, lineHeight);
            return new TextRenderer.TextMetrics(maxWidth, height);
         }
      }
   }

   private float measureLineWidth(String line, float scale) {
      if (line.isEmpty()) {
         return 0.0F;
      } else {
         float penX = 0.0F;
         int prevCodepoint = -1;
         int i = 0;

         while (i < line.length()) {
            char ch = line.charAt(i);
            if (ch == '\\' && i + 9 < line.length() && line.charAt(i + 1) == 'c') {
               i += 10;
            } else {
               int cp = line.codePointAt(i);
               int cpLen = Character.charCount(cp);
               i += cpLen;
               MsdfFont.Glyph glyph = this.font.glyph(cp);
               int glyphCode = cp;
               if (glyph == null) {
                  glyph = this.font.glyph(63);
                  glyphCode = 63;
                  if (glyph == null) {
                     continue;
                  }
               }

               if (prevCodepoint != -1) {
                  penX += this.font.kerning(prevCodepoint, glyphCode) * scale;
               }

               penX += glyph.advance * scale;
               prevCodepoint = glyphCode;
            }
         }

         return penX;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class TextMetrics {
      public final float width;
      public final float height;

      public TextMetrics(float width, float height) {
         this.width = width;
         this.height = height;
      }
   }
}