package sl.selene.ui.gui.component.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class GlassStyle {

   public static final int FRESNEL = Renderer2D.ColorUtil.rgba(255, 255, 255, 104);
   public static final float FRESNEL_POWER = 42.0F;
   public static final float BASE_ALPHA = 0.16F;
   public static final float FRESNEL_MIX = 0.55F;
   private static final int SURFACE_R = 10;
   private static final int SURFACE_G = 14;
   private static final int SURFACE_B = 22;
   private static final float SURFACE_ALPHA = 18.0F;
   public static final float DISTORT = 9.0F;

   public static final float BACKDROP_RADIUS = 10.0F;

   public static final int OUTLINE_TINT = Renderer2D.ColorUtil.rgba(255, 255, 255, 64);
   public static final int OUTLINE_FRESNEL = Renderer2D.ColorUtil.rgba(168, 168, 174, 86);
   public static final float OUTLINE_POWER = 35.0F;
   public static final float OUTLINE_BASE = 0.18F;
   public static final float OUTLINE_MIX = 0.50F;
   public static final float OUTLINE_DISTORT = 0.0015F;
   public static final float OUTLINE_THICKNESS = 1.0F;

   private static final long SHINE_PERIOD_MS = 2400L;

   private GlassStyle() {
   }

   public static float shinePhase() {
      return (System.currentTimeMillis() % SHINE_PERIOD_MS) / (float) SHINE_PERIOD_MS;
   }

   public static void fill(Renderer2D r, float x, float y, float w, float h, float radius, float alpha) {
      fill(r, x, y, w, h, radius, radius, radius, radius, alpha);
   }

   public static void fill(
         Renderer2D r, float x, float y, float w, float h,
         float rTL, float rTR, float rBR, float rBL, float alpha) {
      float safeAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
      r.glass(x, y, w, h, rTL, rTR, rBR, rBL, FRESNEL, FRESNEL_POWER, BASE_ALPHA, true, FRESNEL_MIX, DISTORT, alpha);

      r.rect(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, rTL, rTR, rBR, rBL,
            Renderer2D.ColorUtil.rgba(SURFACE_R, SURFACE_G, SURFACE_B, Math.round(SURFACE_ALPHA * safeAlpha)));
   }

   public static void outline(Renderer2D r, float x, float y, float w, float h, float radius, float alpha) {
      outline(r, x, y, w, h, radius, radius, radius, radius, alpha);
   }

   public static void outline(
         Renderer2D r, float x, float y, float w, float h,
         float rTL, float rTR, float rBR, float rBL, float alpha) {
      r.glassOutline(x, y, w, h, rTL, rTR, rBR, rBL, OUTLINE_THICKNESS, OUTLINE_TINT, OUTLINE_FRESNEL,
            OUTLINE_POWER, OUTLINE_BASE, false, OUTLINE_MIX, OUTLINE_DISTORT, shinePhase(), alpha);
   }

   public static void panel(
         Renderer2D r, float x, float y, float w, float h,
         float rTL, float rTR, float rBR, float rBL, float alpha) {
      fill(r, x, y, w, h, rTL, rTR, rBR, rBL, alpha);
      outline(r, x, y, w, h, rTL, rTR, rBR, rBL, alpha);
   }

   public static void card(Renderer2D r, float x, float y, float w, float h, float alpha) {
      float radius = 6.5F;
      fill(r, x, y, w, h, radius, alpha);
      outline(r, x, y, w, h, radius, alpha);
   }

   public static void toggle(Renderer2D r, float x, float y, float w, float h, float alpha, float progress) {
      float safeAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
      float t = Math.max(0.0F, Math.min(1.0F, progress));
      float radius = h * 0.5F;
      int offTrack = Renderer2D.ColorUtil.rgba(42, 45, 52, Math.round(220.0F * safeAlpha));
      int onTrack = Renderer2D.ColorUtil.rgba(190, 192, 198, Math.round(245.0F * safeAlpha));
      int track = ColorUtil.overCol(offTrack, onTrack, t);
      r.rect(x, y, w, h, radius, track);
      r.rectOutline(x, y, w, h, radius,
            Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round((72.0F + 42.0F * t) * safeAlpha)), 1.0F);

      float knobRadius = Math.max(1.0F, h * 0.5F - 2.0F);
      float knobX = x + radius + (w - h) * t;
      int offKnob = Renderer2D.ColorUtil.rgba(218, 220, 226, Math.round(255.0F * safeAlpha));
      int onKnob = Renderer2D.ColorUtil.rgba(22, 24, 29, Math.round(255.0F * safeAlpha));
      r.circle(knobX, y + radius, knobRadius, 0.0F, 1.0F, ColorUtil.overCol(offKnob, onKnob, t));
   }

   public static void sliderTrack(Renderer2D r, float x, float y, float w, float h, int trackColor, int fillColor, float progress) {
      float t = Math.max(0.0F, Math.min(1.0F, progress));
      r.rect(x, y, w, h, h * 0.5F, trackColor);
      float fillWidth = w * t;
      if (fillWidth > 0.0F) {
         float fillHeight = h - 1.0F;
         r.rect(x + 1.0F, y + 0.5F, Math.max(0.0F, fillWidth - 2.0F), fillHeight, fillHeight * 0.5F, fillColor);
      }
   }

   public static float sliderKnobX(float x, float w, float progress) {
      float t = Math.max(0.0F, Math.min(1.0F, progress));
      float fillWidth = w * t;
      return x + fillWidth - 5.0F + (fillWidth <= 0.0F ? 5.0F : 2.0F);
   }

   public static void sliderKnob(Renderer2D r, float knobX, float trackY, float trackH, int knobColor) {
      float knobHeight = trackH - 0.12F;
      r.rect(knobX, trackY + 0.2F, 5.0F, knobHeight, knobHeight * 0.5F, knobColor);
   }
}