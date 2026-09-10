package sl.selene.util.render.text;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.render.backends.RenderBackend;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class FontRegistry {
   private static final Map<String, MsdfFont> REGISTERED_FONTS = new HashMap<>();
   private static final Map<String, FontObject> FONT_OBJECTS = new HashMap<>();
   private static RenderBackend backend;
   private static boolean backendConfigured = false;
   private static boolean rendererFontsInitialized = false;
   public static FontObject INTER_MEDIUM;
   public static FontObject INTER_SEMIBOLD;
   public static FontObject TBANKSANS;

   private FontRegistry() {
   }

   public static synchronized void initialize(RenderBackend renderBackend, Renderer2D renderer) {
      configureBackend(renderBackend);
      Objects.requireNonNull(renderer, "renderer");
      if (!rendererFontsInitialized) {
         renderer.registerTextRenderer(INTER_MEDIUM, createTextRenderer(INTER_MEDIUM));
         renderer.registerTextRenderer(INTER_SEMIBOLD, createTextRenderer(INTER_SEMIBOLD));
         renderer.registerTextRenderer(TBANKSANS, createTextRenderer(TBANKSANS));
         rendererFontsInitialized = true;
      }
   }

   public static synchronized FontObject register(String id, String jsonResourcePath, String textureResourcePath) {
      ensureBackendConfigured();
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(jsonResourcePath, "jsonResourcePath");
      Objects.requireNonNull(textureResourcePath, "textureResourcePath");
      if (REGISTERED_FONTS.containsKey(id)) {
         throw new IllegalStateException("Font already registered: " + id);
      } else {
         MsdfFont font = MsdfFont.load(backend, jsonResourcePath, textureResourcePath);
         REGISTERED_FONTS.put(id, font);
         FontObject fontObject = new FontObject(id);
         FONT_OBJECTS.put(id, fontObject);
         return fontObject;
      }
   }

   public static synchronized TextRenderer createTextRenderer(FontObject fontObject) {
      ensureBackendConfigured();
      MsdfFont msdfFont = resolve(fontObject);
      return new TextRenderer(backend, msdfFont);
   }

   public static synchronized float centeredBaselineOffset(FontObject fontObject, int codepoint, float size) {
      ensureBackendConfigured();
      if (fontObject != null && !(size <= 0.0F)) {
         MsdfFont font = resolve(fontObject);
         MsdfFont.Glyph glyph = font.glyph(codepoint);
         if (glyph != null && glyph.renderable) {
            float emSize = Math.max(1.0E-6F, font.emSize());
            float scale = size / emSize;
            return (glyph.planeTop + glyph.planeBottom) * 0.5F * scale;
         } else {
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   public static synchronized FontObject get(String id) {
      ensureBackendConfigured();
      FontObject fontObject = FONT_OBJECTS.get(id);
      if (fontObject == null) {
         throw new IllegalArgumentException("Font not registered: " + id);
      } else {
         return fontObject;
      }
   }

   static synchronized MsdfFont resolve(FontObject fontObject) {
      ensureBackendConfigured();
      MsdfFont font = REGISTERED_FONTS.get(fontObject.id);
      if (font == null) {
         throw new IllegalStateException("Font not registered: " + fontObject.id);
      } else {
         return font;
      }
   }

   private static void configureBackend(RenderBackend renderBackend) {
      Objects.requireNonNull(renderBackend, "backend");
      if (backendConfigured) {
         if (backend != renderBackend) {
            throw new IllegalStateException("FontRegistry already initialized with a different backend instance");
         }
      } else {
         backend = renderBackend;
         backendConfigured = true;
         registerBuiltinFonts();
      }
   }

   private static void registerBuiltinFonts() {
      INTER_MEDIUM = register("inter_medium", "assets/selene/fonts/medium.json", "assets/selene/fonts/medium.png");
      INTER_SEMIBOLD = register("inter_semibold", "assets/selene/fonts/semibold.json", "assets/selene/fonts/semibold.png");
      TBANKSANS = register("tbanksans", "assets/selene/fonts/tbanksans.json", "assets/selene/fonts/tbanksans.png");
   }

   private static void ensureBackendConfigured() {
      if (!backendConfigured || backend == null) {
         throw new IllegalStateException("FontRegistry.initialize(backend, renderer) must be called before use");
      }
   }

   public static synchronized void shutdown() {
      REGISTERED_FONTS.clear();
      FONT_OBJECTS.clear();
      backend = null;
      backendConfigured = false;
      rendererFontsInitialized = false;
      INTER_MEDIUM = null;
      INTER_SEMIBOLD = null;
      TBANKSANS = null;
   }
}