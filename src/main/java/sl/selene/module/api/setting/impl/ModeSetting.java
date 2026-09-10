package sl.selene.module.api.setting.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;
import sl.selene.util.render.math.animation.Animation;
import sl.selene.util.render.math.animation.impl.EaseInOutQuad;

@Environment(EnvType.CLIENT)
public class ModeSetting extends Setting {
   public final List<String> modes;
   public String currentMode;
   public String description;
   public Animation animation = new EaseInOutQuad(300, 1.0);
   public int index;
   public boolean opened;

   public ModeSetting(String name, String currentMode, String... options) {
      this.name = name;
      this.modes = Arrays.asList(options);
      this.index = this.modes.indexOf(currentMode);
      this.currentMode = this.modes.get(this.index);
   }

   public String get() {
      return this.currentMode;
   }

   public boolean is(String mode) {
      return this.currentMode.equalsIgnoreCase(mode);
   }

   public ModeSetting hidden(Supplier<Boolean> hidden) {
      this.hidden = hidden;
      return this;
   }
}