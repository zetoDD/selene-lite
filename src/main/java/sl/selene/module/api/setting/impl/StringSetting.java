package sl.selene.module.api.setting.impl;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;

@Environment(EnvType.CLIENT)
public class StringSetting extends Setting {
   public String input;
   public String description;
   public boolean active;

   public StringSetting(String name, String input) {
      this.name = name;
      this.input = input;
   }

   public String get() {
      return this.input;
   }

   public void set(String input) {
      this.input = input;
   }

   public StringSetting hidden(Supplier<Boolean> hidden) {
      this.hidden = hidden;
      return this;
   }
}