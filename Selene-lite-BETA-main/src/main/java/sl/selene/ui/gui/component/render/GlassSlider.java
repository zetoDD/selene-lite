package sl.selene.ui.gui.component.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class GlassSlider {

   private static final float TRACK_HEIGHT = 7.0F;

   public static final float HIT_PADDING = 9.0F;

   private static final float KNOB_BASE_RADIUS = 6.1F;

   private GlassSlider() {
   }

   @Environment(EnvType.CLIENT)
   public static final class State {
      private float hover;
      private float press;

      public float hover() {
         return this.hover;
      }
   }

   public static float trackHeight(State state) {
      return TRACK_HEIGHT + 1.6F * (state == null ? 0.0F : state.hover);
   }

   public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, State state) {
      float height = trackHeight(state);
      return GuiRenderMain.isHovered(mouseX, mouseY, x, y - HIT_PADDING, width, height + HIT_PADDING * 2.0F);
   }

   public static float progressFromMouse(float mouseX, float x, float width, State state) {
      float knobRadius = knobRadius(state);
      float travel = Math.max(1.0F, width - knobRadius * 2.0F);
      float progress = (mouseX - (x + knobRadius)) / travel;
      return clamp01(progress);
   }

   public static void render(
         Renderer2D renderer,
         State state,
         float x,
         float y,
         float width,
         float progress,
         boolean hovered,
         boolean active,
         float alpha) {
      if (state == null || width <= 0.0F || alpha <= 0.001F) {
         return;
      }

      state.hover = GuiScreen.easeHover(state.hover, hovered || active);
      state.press = GuiScreen.easeHover(state.press, active);

      float safeAlpha = clamp01(alpha);
      float t = clamp01(progress);
      float trackH = trackHeight(state);
      float radius = trackH * 0.5F;
      float centerY = y + radius;
      float knobRadius = knobRadius(state);
      float travel = Math.max(0.0F, width - knobRadius * 2.0F);
      float knobX = x + knobRadius + travel * t;
      float fillWidth = Math.max(0.0F, knobX - x);
      float lift = 0.35F + 0.65F * state.hover;

      renderer.shadow(x, y + 1.0F, width, trackH, radius, 4.5F, 0.55F,
            Renderer2D.ColorUtil.rgba(0, 0, 0, Math.round(78.0F * safeAlpha)));

      GlassStyle.fill(renderer, x, y, width, trackH, radius, safeAlpha * 0.92F);
      renderer.rect(x, y, width, trackH, radius,
            Renderer2D.ColorUtil.rgba(9, 12, 19, Math.round(96.0F * safeAlpha)));

      if (width > radius * 2.4F) {
         renderer.rect(x + radius * 0.7F, y + 0.7F, width - radius * 1.4F, 0.9F, 0.45F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(30.0F * safeAlpha)));
      }

      drawTicks(renderer, state, x, y, width, trackH, safeAlpha);

      if (fillWidth > 1.0F) {
         renderer.shadow(x, y, fillWidth, trackH, radius, 7.0F, 0.4F,
               Renderer2D.ColorUtil.rgba(188, 212, 255, Math.round(64.0F * safeAlpha * lift)));

         renderer.horizontalGradient(x, y, fillWidth, trackH, radius,
               Renderer2D.ColorUtil.rgba(146, 172, 214, Math.round(205.0F * safeAlpha)),
               Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(248.0F * safeAlpha)));

         if (fillWidth > radius * 2.2F) {
            renderer.rect(x + radius * 0.8F, y + 0.9F, fillWidth - radius * 1.6F, trackH * 0.28F,
                  trackH * 0.14F, Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(58.0F * safeAlpha)));
         }
      }

      drawKnob(renderer, state, knobX, centerY, knobRadius, safeAlpha);
   }

   private static void drawTicks(
         Renderer2D renderer, State state, float x, float y, float width, float trackH, float alpha) {
      float visibility = state.hover;
      if (visibility <= 0.02F) {
         return;
      }

      int tickAlpha = Math.round(46.0F * visibility * alpha);
      if (tickAlpha <= 0) {
         return;
      }

      int color = Renderer2D.ColorUtil.rgba(255, 255, 255, tickAlpha);
      float size = 1.4F;
      float tickY = y + trackH * 0.5F - size * 0.5F;
      for (int i = 1; i < 4; i++) {
         float tickX = x + width * (i / 4.0F) - size * 0.5F;
         renderer.rect(tickX, tickY, size, size, size * 0.5F, color);
      }
   }

   private static void drawKnob(
         Renderer2D renderer, State state, float cx, float cy, float knobRadius, float alpha) {
      if (state.press > 0.02F) {
         renderer.circle(cx, cy, knobRadius + 4.2F * state.press, 0.0F, 1.0F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(34.0F * state.press * alpha)));
      }

      renderer.shadow(cx - knobRadius, cy - knobRadius + 1.3F, knobRadius * 2.0F, knobRadius * 2.0F, knobRadius,
            6.0F, 0.5F, Renderer2D.ColorUtil.rgba(0, 0, 0, Math.round(128.0F * alpha)));

      renderer.circle(cx, cy, knobRadius, 0.0F, 1.0F,
            Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(104.0F * alpha)));
      renderer.circle(cx, cy, knobRadius - 0.9F, 0.0F, 1.0F,
            Renderer2D.ColorUtil.rgba(252, 253, 255, Math.round(255.0F * alpha)));
      renderer.circle(cx, cy + knobRadius * 0.30F, knobRadius * 0.62F, 0.0F, 1.0F,
            Renderer2D.ColorUtil.rgba(206, 214, 231, Math.round(150.0F * alpha)));

      renderer.circle(cx - knobRadius * 0.26F, cy - knobRadius * 0.34F, knobRadius * 0.40F, 0.0F, 1.0F,
            Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(220.0F * alpha)));
   }

   private static float knobRadius(State state) {
      float hover = state == null ? 0.0F : state.hover;
      float press = state == null ? 0.0F : state.press;
      return KNOB_BASE_RADIUS + 1.15F * hover + 0.7F * press;
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      }

      return value > 1.0F ? 1.0F : value;
   }
}