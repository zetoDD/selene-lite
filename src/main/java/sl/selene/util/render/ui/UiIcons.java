package sl.selene.util.render.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.Category;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class UiIcons {
   private UiIcons() {
   }

   public static void category(Renderer2D renderer, Category category, float centerX, float centerY, float size, float alpha) {
      if (category == null) {
         return;
      }

      icon(renderer, category.getIcon(), centerX, centerY, size, alpha);
   }

   public static void named(Renderer2D renderer, String name, float centerX, float centerY, float size, float alpha) {
      if (name == null) {
         return;
      }

      String icon = switch (name) {
         case "user" -> "user";
         case "latency", "ping" -> "wifi";
         case "fps" -> "gauge";
         case "clock" -> "clock";
         case "server" -> "server";
         case "info" -> "info";
         case "coordinates" -> "crosshair";
         case "speed" -> "gauge";
         case "binds" -> "keyboard";
         case "target", "armor" -> name.equals("armor") ? "shield" : "target";
         case "success", "on" -> "check";
         case "warning", "warn" -> "warning";
         case "error", "off" -> "error";
         case "cfg" -> "settings";
         default -> "info";
      };
      icon(renderer, icon, centerX, centerY, size, alpha);
   }

   public static void draw(Renderer2D renderer, String name, float centerX, float centerY, float size, float alpha) {
      icon(renderer, name, centerX, centerY, size, alpha);
   }

   public static void drawTinted(Renderer2D renderer, String name, float centerX, float centerY, float size, float alpha,
         int tintRgba) {
      SeleneIcons.drawTinted(renderer, name, centerX, centerY, size, alpha, tintRgba);
   }

   public static void search(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "search", centerX, centerY, size, alpha);
   }

   public static void settings(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "settings", centerX, centerY, size, alpha);
   }

   public static void close(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "close", centerX, centerY, size, alpha);
   }

   public static void chevron(Renderer2D renderer, float centerX, float centerY, float size, boolean up, float alpha) {
      icon(renderer, up ? "chevron-up" : "chevron-down", centerX, centerY, size, alpha);
   }

   public static void map(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "map", centerX, centerY, size, alpha);
   }

   public static void info(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "info", centerX, centerY, size, alpha);
   }

   public static void coordinates(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "crosshair", centerX, centerY, size, alpha);
   }

   public static void speed(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "gauge", centerX, centerY, size, alpha);
   }

   public static void sliders(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "sliders", centerX, centerY, size, alpha);
   }
   public static void wrench(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "wrench", centerX, centerY, size, alpha);
   }

   public static void binds(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "keyboard", centerX, centerY, size, alpha);
   }

   public static void armor(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "shield", centerX, centerY, size, alpha);
   }

   public static void check(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "check", centerX, centerY, size, alpha);
   }

   public static void crosshair(Renderer2D renderer, float centerX, float centerY, float size, float alpha) {
      icon(renderer, "target", centerX, centerY, size, alpha);
   }

   public static void horizontalShineLine(Renderer2D renderer, float x, float y, float width, float thickness, float alpha) {
      if (width <= 0.0F || thickness <= 0.0F) {
         return;
      }

      float safeAlpha = clamp01(alpha);
      int base = white(safeAlpha * 0.40F);
      renderer.rect(x, y, width, thickness, 0.0F, base);
   }

   public static void verticalShineLine(Renderer2D renderer, float x, float y, float height, float thickness, float alpha) {
      if (height <= 0.0F || thickness <= 0.0F) {
         return;
      }

      float safeAlpha = clamp01(alpha);
      int base = white(safeAlpha * 0.34F);
      renderer.rect(x, y, thickness, height, 0.0F, base);
   }

   public static int whiteColor(float alpha) {
      return white(alpha);
   }

   private static void icon(Renderer2D renderer, String name, float centerX, float centerY, float size, float alpha) {
      SeleneIcons.draw(renderer, name, centerX, centerY, size, alpha);
   }

   private static int white(float alpha) {
      return Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(clamp01(alpha) * 255.0F));
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      }
      return value > 1.0F ? 1.0F : value;
   }
}