package sl.selene.module.api.setting.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;

@Environment(EnvType.CLIENT)
public class MultiBooleanSetting extends Setting {
   public List<BooleanSetting> settings;

   public MultiBooleanSetting(String name, BooleanSetting... settings) {
      this.name = name;
      this.settings = Arrays.asList(settings);
   }

   public boolean get(String name) {
      for (BooleanSetting setting : this.settings) {
         if (setting.name.equals(name)) {
            return setting.get();
         }
      }

      return false;
   }

   public MultiBooleanSetting hidden(Supplier<Boolean> hidden) {
      this.hidden = hidden;
      return this;
   }
}