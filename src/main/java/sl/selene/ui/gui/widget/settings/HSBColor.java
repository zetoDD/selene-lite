package sl.selene.ui.gui.widget.settings;

import java.awt.Color;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class HSBColor {
   private final float hue;
   private final float saturation;
   private final float brightness;
   private final float alpha;

   private HSBColor(float hue, float saturation, float brightness, float alpha) {
      this.hue = clampHue(hue);
      this.saturation = clamp01(saturation);
      this.brightness = clamp01(brightness);
      this.alpha = clamp01(alpha);
   }

   public static HSBColor of(float hue, float saturation, float brightness, float alpha) {
      return new HSBColor(hue, saturation, brightness, alpha);
   }

   public static HSBColor of(float hue, float saturation, float brightness) {
      return new HSBColor(hue, saturation, brightness, 1.0F);
   }

   public static HSBColor fromRgba(int rgba) {
      int r = rgba >>> 16 & 0xFF;
      int g = rgba >>> 8 & 0xFF;
      int b = rgba & 0xFF;
      int a = rgba >>> 24 & 0xFF;
      float[] hsb = Color.RGBtoHSB(r, g, b, null);
      return new HSBColor(hsb[0] * 360.0F, hsb[1], hsb[2], a / 255.0F);
   }

   public float hue() {
      return this.hue;
   }

   public float saturation() {
      return this.saturation;
   }

   public float brightness() {
      return this.brightness;
   }

   public float alpha() {
      return this.alpha;
   }

   public HSBColor withHue(float newHue) {
      return new HSBColor(newHue, this.saturation, this.brightness, this.alpha);
   }

   public HSBColor withSaturation(float newSaturation) {
      return new HSBColor(this.hue, newSaturation, this.brightness, this.alpha);
   }

   public HSBColor withBrightness(float newBrightness) {
      return new HSBColor(this.hue, this.saturation, newBrightness, this.alpha);
   }

   public HSBColor withAlpha(float newAlpha) {
      return new HSBColor(this.hue, this.saturation, this.brightness, newAlpha);
   }

   public HSBColor normalised() {
      return this;
   }

   public int toRgba() {
      float h = this.hue / 360.0F;
      Color color = Color.getHSBColor(h, this.saturation, this.brightness);
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      int a = Math.round(this.alpha * 255.0F);
      return a << 24 | r << 16 | g << 8 | b;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         HSBColor hsbColor = (HSBColor)o;
         return Float.compare(hsbColor.hue, this.hue) == 0
            && Float.compare(hsbColor.saturation, this.saturation) == 0
            && Float.compare(hsbColor.brightness, this.brightness) == 0
            && Float.compare(hsbColor.alpha, this.alpha) == 0;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.hue, this.saturation, this.brightness, this.alpha);
   }

   private static float clampHue(float hue) {
      if (!Float.isFinite(hue)) {
         return 0.0F;
      } else {
         float wrapped = hue % 360.0F;
         if (wrapped < 0.0F) {
            wrapped += 360.0F;
         }

         return wrapped;
      }
   }

   private static float clamp01(float value) {
      return !(value <= 0.0F) && !Float.isNaN(value) ? Math.min(value, 1.0F) : 0.0F;
   }
}