package sl.selene.event.render;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.event.Event;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontObject;

@Environment(EnvType.CLIENT)
public final class RenderEvent extends Event {
   private final MinecraftClient client;
   private final Renderer2D renderer;
   private final FontObject defaultFont;
   private final int viewportWidth;
   private final int viewportHeight;

   public RenderEvent(MinecraftClient client, Renderer2D renderer, FontObject defaultFont, int viewportWidth, int viewportHeight) {
      this.client = Objects.requireNonNull(client, "client");
      this.renderer = Objects.requireNonNull(renderer, "renderer");
      this.defaultFont = Objects.requireNonNull(defaultFont, "defaultFont");
      this.viewportWidth = viewportWidth;
      this.viewportHeight = viewportHeight;
   }

   public MinecraftClient client() {
      return this.client;
   }

   public Renderer2D renderer() {
      return this.renderer;
   }

   public FontObject defaultFont() {
      return this.defaultFont;
   }

   public int viewportWidth() {
      return this.viewportWidth;
   }

   public int viewportHeight() {
      return this.viewportHeight;
   }
}