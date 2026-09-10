package sl.selene.util.render.ui;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.texture.TextureLoader;

@Environment(EnvType.CLIENT)
public final class SeleneIcons {
   private static final String ATLAS_PATH = "assets/selene/icons/atlas.png";
   private static final String SEMANTIC_ATLAS_PATH = "assets/selene/icons/semantic-atlas.png";
   private static final int CELL = 96;
   private static final int SEMANTIC_CELL = 96;
   private static final int SEMANTIC_COLS = 4;
   private static final int SEMANTIC_ATLAS_W = 384;
   private static final int SEMANTIC_ATLAS_H = 384;
   private static final int COLS = 12;
   private static final int ATLAS_W = 1152;
   private static final int ATLAS_H = 768;

   private static final String[] NAMES = {

      "sword", "axe", "bow", "arrow", "crossbow", "trident", "mace", "dagger",
      "hammer", "pickaxe", "shovel", "hoe", "spear",

      "shield", "chestplate", "helmet", "boots", "leggings",

      "potion", "apple", "heart", "diamond", "emerald", "ingot", "compass", "clock",
      "book", "feather", "fishing-rod", "carrot", "bone", "egg", "pearl", "eye-of-ender",
      "totem", "key", "lock", "magnet", "star", "gem", "trophy", "medal", "bell", "flag",
      "snowflake", "flame", "lightning", "music", "gamepad", "film", "camera", "chat",
      "mail", "cart", "dollar", "coin", "bag", "backpack",

      "zombie", "skeleton", "creeper", "enderman", "bee", "paw", "slime", "skull", "spider",

      "search", "settings", "close", "chevron-up", "chevron-down", "map", "info", "user",
      "wifi", "gauge", "keyboard", "server", "check", "warning", "error", "sliders",
      "layout", "palette", "eye", "sparkles", "wrench", "activity", "crosshair", "target",
      "crown"
   };

   private static final Map<String, Integer> NAME_INDEX = new HashMap<>();
   private static final Map<String, Integer> SEMANTIC_INDEX = new HashMap<>();
   private static final Map<String, Integer> CATEGORY_TEXTURES = new HashMap<>();
   private static final String[] CATEGORY_NAMES = {
      "combat", "movement", "visuals", "player", "utils", "misc"
   };

   private static final Map<String, Integer> CUSTOM_TEXTURES = new HashMap<>();
   private static final String[] CUSTOM_NAMES = { "search", "close", "settings" };
   private static volatile int atlasTexture = -1;
   private static volatile int semanticAtlasTexture = -1;

   static {
      for (int i = 0; i < NAMES.length; i++) {
         NAME_INDEX.put(NAMES[i], i);
      }

      String[] semanticNames = {
         "combat", "movement", "visuals", "player", "utils", "misc", "prime", "settings",
         "search", "close", "info", "gauge", "keyboard", "target", "shield", "wrench"
      };
      for (int i = 0; i < semanticNames.length; i++) {
         SEMANTIC_INDEX.put(semanticNames[i], i);
      }
   }

   private SeleneIcons() {
   }

   public static void draw(Renderer2D renderer, String name, float centerX, float centerY, float size, float alpha) {
      if (renderer == null || name == null || size <= 0.0F || alpha <= 0.001F) {
         return;
      }

      if ("donut".equals(name)) {
         drawDonut(renderer, centerX, centerY, size, alpha);
         return;
      }

      int categoryTexture = categoryTexture(name);
      if (categoryTexture > 0) {
         drawSingle(renderer, categoryTexture, centerX, centerY, size, alpha);
         return;
      }

      int customTexture = customIconTexture(name);
      if (customTexture > 0) {
         drawSingle(renderer, customTexture, centerX, centerY, size, alpha);
         return;
      }

      Integer semanticIndex = SEMANTIC_INDEX.get(name);
      if (semanticIndex != null) {
         drawSemantic(renderer, semanticIndex, centerX, centerY, size, alpha);
         return;
      }

      if ("bucket".equals(name)) {
         drawBucket(renderer, centerX, centerY, size, alpha);
         return;
      }

      int texture = atlasTexture();
      if (texture <= 0) {
         return;
      }

      Integer index = NAME_INDEX.get(name);
      if (index == null) {
         index = NAME_INDEX.get("info");
      }
      if (index == null) {
         return;
      }

      int col = index % COLS;
      int row = index / COLS;
      float u0 = col * CELL / (float) ATLAS_W;
      float u1 = (col + 1) * CELL / (float) ATLAS_W;

      float v0 = row * CELL / (float) ATLAS_H;
      float v1 = (row + 1) * CELL / (float) ATLAS_H;

      renderer.pushAlpha(clamp01(alpha));
      renderer.drawRgbaTextureWithUV(
         texture,
         centerX - size * 0.5F,
         centerY - size * 0.5F,
         size,
         size,
         u0,
         v0,
         u1,
         v1
      );
      renderer.popAlpha();
   }

   public static void clearCache() {
      atlasTexture = -1;
      semanticAtlasTexture = -1;
      CATEGORY_TEXTURES.clear();
      CUSTOM_TEXTURES.clear();
   }

   private static int categoryTexture(String name) {
      if (name == null || name.isEmpty() || !isCategoryName(name)) {
         return 0;
      }

      String key = name;
      Integer cached = CATEGORY_TEXTURES.get(key);
      if (cached != null) {
         return cached;
      }

      int texture = TextureLoader.load("assets/selene/icons/categories/" + name + ".png");
      CATEGORY_TEXTURES.put(key, texture);
      return texture;
   }

   private static boolean isCategoryName(String name) {
      for (String candidate : CATEGORY_NAMES) {
         if (candidate.equals(name)) {
            return true;
         }
      }
      return false;
   }

   private static int customIconTexture(String name) {
      if (name == null || name.isEmpty()) {
         return 0;
      }

      boolean known = false;
      for (String candidate : CUSTOM_NAMES) {
         if (candidate.equals(name)) {
            known = true;
            break;
         }
      }
      if (!known) {
         return 0;
      }

      Integer cached = CUSTOM_TEXTURES.get(name);
      if (cached != null) {
         return cached;
      }
      int texture = TextureLoader.load("assets/selene/icons/" + name + ".png");
      CUSTOM_TEXTURES.put(name, texture);
      return texture;
   }

   private static void drawSingle(Renderer2D renderer, int texture, float centerX, float centerY, float size, float alpha) {
      renderer.pushAlpha(clamp01(alpha));

      renderer.drawRgbaTexture(texture, centerX - size * 0.5F, centerY - size * 0.5F, size, size, -1, false);
      renderer.popAlpha();
   }

   private static void drawSemantic(Renderer2D renderer, int index, float centerX, float centerY, float size, float alpha) {
      int texture = semanticAtlasTexture();
      if (texture <= 0) {
         return;
      }

      int col = index % SEMANTIC_COLS;
      int row = index / SEMANTIC_COLS;
      float u0 = col * SEMANTIC_CELL / (float) SEMANTIC_ATLAS_W;
      float u1 = (col + 1) * SEMANTIC_CELL / (float) SEMANTIC_ATLAS_W;
      float v0 = row * SEMANTIC_CELL / (float) SEMANTIC_ATLAS_H;
      float v1 = (row + 1) * SEMANTIC_CELL / (float) SEMANTIC_ATLAS_H;
      renderer.pushAlpha(clamp01(alpha));
      renderer.drawRgbaTextureWithUV(
         texture,
         centerX - size * 0.5F,
         centerY - size * 0.5F,
         size,
         size,
         u0,
         v0,
         u1,
         v1
      );
      renderer.popAlpha();
   }

   private static void drawDonut(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      float safeAlpha = clamp01(alpha);
      int ring = Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(safeAlpha * 255.0F));
      int hole = Renderer2D.ColorUtil.rgba(10, 14, 22, Math.round(safeAlpha * 230.0F));
      float outerRadius = size * 0.42F;
      float holeRadius = size * 0.17F;

      renderer.circle(centerX, centerY, outerRadius, 0.0F, 1.0F, ring);
      renderer.circle(centerX, centerY, holeRadius, 0.0F, 1.0F, hole);
   }

   private static void drawBucket(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      int stroke = Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(clamp01(alpha) * 255.0F));
      int fill = Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(clamp01(alpha) * 42.0F));
      float left = centerX - size * 0.28F;
      float top = centerY - size * 0.29F;
      float width = size * 0.56F;
      float rimHeight = Math.max(1.0F, size * 0.09F);
      float bodyTop = top + size * 0.13F;
      float bodyHeight = size * 0.46F;

      renderer.rect(left, top, width, rimHeight, rimHeight * 0.35F, stroke);
      renderer.rect(left + size * 0.05F, bodyTop, width - size * 0.10F, bodyHeight, size * 0.05F, fill);
      renderer.rectOutline(left + size * 0.05F, bodyTop, width - size * 0.10F, bodyHeight, size * 0.05F, stroke, Math.max(1.0F, size * 0.055F));

      renderer.pushTranslation(centerX, centerY + size * 0.03F);
      renderer.pushRotation(14.0F);
      renderer.rect(-size * 0.31F, -size * 0.17F, Math.max(1.0F, size * 0.045F), size * 0.47F, size * 0.02F, stroke);
      renderer.popRotation();
      renderer.pushRotation(-14.0F);
      renderer.rect(size * 0.265F, -size * 0.17F, Math.max(1.0F, size * 0.045F), size * 0.47F, size * 0.02F, stroke);
      renderer.popRotation();
      renderer.popTransform();
   }

   private static int atlasTexture() {
      int cached = atlasTexture;
      if (cached > 0) {
         return cached;
      }
      int loaded = TextureLoader.load(ATLAS_PATH);
      atlasTexture = loaded;
      return loaded;
   }

   private static int semanticAtlasTexture() {
      int cached = semanticAtlasTexture;
      if (cached > 0) {
         return cached;
      }
      int loaded = TextureLoader.load(SEMANTIC_ATLAS_PATH);
      semanticAtlasTexture = loaded;
      return loaded;
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      }
      return value > 1.0F ? 1.0F : value;
   }
}