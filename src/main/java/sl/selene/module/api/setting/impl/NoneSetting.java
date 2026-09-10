package sl.selene.module.api.setting.impl;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;

@Environment(EnvType.CLIENT)
public class NoneSetting extends Setting {
   public float up;

   public NoneSetting(float up) {
      this.up = up;
   }

   public NoneSetting() {
      this.up = 15.0F;
   }

   public NoneSetting hidden(Supplier<Boolean> hidden) {
      this.hidden = hidden;
      return this;
   }

   public float get() {
      return this.up;
   }
}