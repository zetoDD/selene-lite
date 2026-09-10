package sl.selene.util.render.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import sl.selene.util.render.backends.RenderBackend;
import sl.selene.util.render.backends.gl.ResourceUtils;

@Environment(EnvType.CLIENT)
final class MsdfFont {
   private final Map<Integer, MsdfFont.Glyph> glyphs;
   private final Map<Long, Float> kerning;
   private final int textureId;
   private final int textureWidth;
   private final int textureHeight;
   private final float distanceRange;
   private final float emSize;
   private final float lineHeight;
   private final float ascender;
   private final float descender;

   private MsdfFont(
      int textureId,
      int textureWidth,
      int textureHeight,
      float distanceRange,
      float emSize,
      float lineHeight,
      float ascender,
      float descender,
      Map<Integer, MsdfFont.Glyph> glyphs,
      Map<Long, Float> kerning
   ) {
      this.textureId = textureId;
      this.textureWidth = textureWidth;
      this.textureHeight = textureHeight;
      this.distanceRange = distanceRange;
      this.emSize = emSize;
      this.lineHeight = lineHeight;
      this.ascender = ascender;
      this.descender = descender;
      this.glyphs = glyphs;
      this.kerning = kerning;
   }

   static MsdfFont load(RenderBackend backend, String jsonResourcePath, String textureResourcePath) {
      Objects.requireNonNull(backend, "backend");
      Objects.requireNonNull(jsonResourcePath, "jsonResourcePath");
      Objects.requireNonNull(textureResourcePath, "textureResourcePath");
      String json = ResourceUtils.readText(jsonResourcePath);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      JsonObject atlas = root.getAsJsonObject("atlas");
      if (atlas == null) {
         throw new IllegalStateException("Missing 'atlas' section in MSDF font: " + jsonResourcePath);
      } else {
         int atlasWidth = atlas.get("width").getAsInt();
         int atlasHeight = atlas.get("height").getAsInt();
         float distanceRange = atlas.has("distanceRange") ? atlas.get("distanceRange").getAsFloat() : 6.0F;
         JsonObject metrics = root.getAsJsonObject("metrics");
         if (metrics == null) {
            throw new IllegalStateException("Missing 'metrics' section in MSDF font: " + jsonResourcePath);
         } else {
            float emSize = metrics.has("emSize") ? metrics.get("emSize").getAsFloat() : 1.0F;
            float lineHeight = metrics.has("lineHeight") ? metrics.get("lineHeight").getAsFloat() : emSize;
            float ascender = metrics.has("ascender") ? metrics.get("ascender").getAsFloat() : lineHeight;
            float descenderValue = metrics.has("descender") ? metrics.get("descender").getAsFloat() : 0.0F;
            float descender = Math.abs(descenderValue);
            Map<Integer, MsdfFont.Glyph> glyphs = new HashMap<>();
            JsonArray glyphArray = root.getAsJsonArray("glyphs");
            if (glyphArray != null) {
               for (JsonElement element : glyphArray) {
                  JsonObject glyphObj = element.getAsJsonObject();
                  int codepoint = glyphObj.get("unicode").getAsInt();
                  float advance = glyphObj.has("advance") ? glyphObj.get("advance").getAsFloat() : 0.0F;
                  JsonObject planeBounds = glyphObj.has("planeBounds") ? glyphObj.getAsJsonObject("planeBounds") : null;
                  JsonObject atlasBounds = glyphObj.has("atlasBounds") ? glyphObj.getAsJsonObject("atlasBounds") : null;
                  MsdfFont.Glyph glyph;
                  if (planeBounds != null && atlasBounds != null) {
                     float planeLeft = planeBounds.get("left").getAsFloat();
                     float planeBottom = planeBounds.get("bottom").getAsFloat();
                     float planeRight = planeBounds.get("right").getAsFloat();
                     float planeTop = planeBounds.get("top").getAsFloat();
                     float atlasLeft = atlasBounds.get("left").getAsFloat();
                     float atlasBottom = atlasBounds.get("bottom").getAsFloat();
                     float atlasRight = atlasBounds.get("right").getAsFloat();
                     float atlasTop = atlasBounds.get("top").getAsFloat();
                     glyph = new MsdfFont.Glyph(
                        advance, planeLeft, planeBottom, planeRight, planeTop, atlasLeft, atlasBottom, atlasRight, atlasTop, atlasWidth, atlasHeight
                     );
                  } else {
                     glyph = new MsdfFont.Glyph(advance);
                  }

                  glyphs.put(codepoint, glyph);
               }
            }

            Map<Long, Float> kerning = new HashMap<>();
            JsonArray kerningArray = root.getAsJsonArray("kerning");
            if (kerningArray != null) {
               for (JsonElement element : kerningArray) {
                  JsonObject kObj = element.getAsJsonObject();
                  int left = kObj.get("unicode1").getAsInt();
                  int right = kObj.get("unicode2").getAsInt();
                  float advance = kObj.has("advance") ? kObj.get("advance").getAsFloat() : 0.0F;
                  kerning.put(pairKey(left, right), advance);
               }
            }

            ByteBuffer imageBuffer = ResourceUtils.readBinary(textureResourcePath);
            MemoryStack stack = MemoryStack.stackPush();

            int textureId;
            int textureWidth;
            int textureHeight;
            try {
               IntBuffer w = stack.mallocInt(1);
               IntBuffer h = stack.mallocInt(1);
               IntBuffer comp = stack.mallocInt(1);
               ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, 4);
               if (image == null) {
                  throw new IllegalStateException("Failed to load MSDF atlas '" + textureResourcePath + "': " + STBImage.stbi_failure_reason());
               }

               textureWidth = w.get(0);
               textureHeight = h.get(0);
               textureId = backend.createMsdfTexture(textureWidth, textureHeight, image);
               STBImage.stbi_image_free(image);
            } catch (Throwable var34) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var33) {
                     var34.addSuppressed(var33);
                  }
               }

               throw var34;
            }

            if (stack != null) {
               stack.close();
            }

            return new MsdfFont(textureId, textureWidth, textureHeight, distanceRange, emSize, lineHeight, ascender, descender, glyphs, kerning);
         }
      }
   }

   int textureId() {
      return this.textureId;
   }

   int textureWidth() {
      return this.textureWidth;
   }

   int textureHeight() {
      return this.textureHeight;
   }

   float distanceRange() {
      return this.distanceRange;
   }

   float emSize() {
      return this.emSize;
   }

   float lineHeight() {
      return this.lineHeight;
   }

   float ascender() {
      return this.ascender;
   }

   float descender() {
      return this.descender;
   }

   MsdfFont.Glyph glyph(int codepoint) {
      return this.glyphs.get(codepoint);
   }

   float kerning(int left, int right) {
      return this.kerning.getOrDefault(pairKey(left, right), 0.0F);
   }

   private static long pairKey(int left, int right) {
      return (long)left << 32 | right & 4294967295L;
   }

   @Environment(EnvType.CLIENT)
   static final class Glyph {
      final float advance;
      final boolean renderable;
      final float planeLeft;
      final float planeBottom;
      final float planeRight;
      final float planeTop;
      final float u0;
      final float v0;
      final float u1;
      final float v1;

      Glyph(float advance) {
         this.advance = advance;
         this.renderable = false;
         this.planeLeft = 0.0F;
         this.planeBottom = 0.0F;
         this.planeRight = 0.0F;
         this.planeTop = 0.0F;
         this.u0 = 0.0F;
         this.v0 = 0.0F;
         this.u1 = 0.0F;
         this.v1 = 0.0F;
      }

      Glyph(
         float advance,
         float planeLeft,
         float planeBottom,
         float planeRight,
         float planeTop,
         float atlasLeft,
         float atlasBottom,
         float atlasRight,
         float atlasTop,
         int atlasWidth,
         int atlasHeight
      ) {
         this.advance = advance;
         this.renderable = true;
         this.planeLeft = planeLeft;
         this.planeBottom = planeBottom;
         this.planeRight = planeRight;
         this.planeTop = planeTop;
         this.u0 = atlasLeft / atlasWidth;
         this.u1 = atlasRight / atlasWidth;
         float vBottom = atlasBottom / atlasHeight;
         float vTop = atlasTop / atlasHeight;
         this.v0 = 1.0F - vBottom;
         this.v1 = 1.0F - vTop;
      }
   }
}