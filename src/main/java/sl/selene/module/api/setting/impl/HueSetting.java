package sl.selene.module.api.setting.impl;

import java.awt.Color;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;
import sl.selene.util.render.math.animation.Animation;
import sl.selene.util.render.math.animation.impl.EaseInOutQuad;

@Environment(EnvType.CLIENT)
public class HueSetting extends Setting {
   public float current;
   public float minimum;
   public float maximum;
   public float increment;
   public float sliderWidth;
   public boolean sliding;
   public String description;
   public Animation animation = new EaseInOutQuad(300, 1.0);
   public float saturation = 1.0F;
   public float brightness = 1.0F;

   public HueSetting(String name, float current) {
      this.name = name;
      this.minimum = 0.0F;
      this.current = current;
      this.maximum = 106.0F;
      this.increment = 1.0F;
      this.saturation = 1.0F;
      this.brightness = 1.0F;
   }

   public HueSetting(String name, float current, float saturation, float brightness) {
      this.name = name;
      this.minimum = 0.0F;
      this.current = current;
      this.maximum = 106.0F;
      this.increment = 1.0F;
      this.saturation = saturation;
      this.brightness = brightness;
   }

   public HueSetting hidden(Supplier<Boolean> hidden) {
      this.hidden = hidden;
      return this;
   }

   public Color getColor() {
      float hue = this.current / this.maximum;
      return Color.getHSBColor(hue, this.saturation, this.brightness);
   }

   public void setColor(Color color) {
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
      this.current = hsb[0] * this.maximum;
      this.saturation = hsb[1];
      this.brightness = hsb[2];
   }

   public float getHue() {
      return this.current / this.maximum;
   }

   public int getRGB() {
      return this.getColor().getRGB();
   }

   public int getRGBA(int alpha) {
      Color color = this.getColor();
      return alpha << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
   }
}