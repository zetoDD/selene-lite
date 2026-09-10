package sl.selene.module.api.setting.impl;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;

@Environment(EnvType.CLIENT)
public class SliderSetting extends Setting {
   public float current;
   public float minimum;
   public float maximum;
   public float increment;
   public float sliderWidth;
   public boolean sliding;
   public boolean percent;
   public String description;

   public SliderSetting(String name, float current, float minimum, float maximum, float increment, boolean percent) {
      this.name = name;
      this.minimum = minimum;
      this.current = current;
      this.maximum = maximum;
      this.increment = increment;
      this.description = this.description;
      this.percent = percent;
   }

   public float get() {
      return this.current;
   }

   public SliderSetting hidden(Supplier<Boolean> hidden) {
      this.hidden = hidden;
      return this;
   }
}