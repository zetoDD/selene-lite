package sl.selene.module.api.setting.impl;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;
import sl.selene.util.render.animation.util.Animation;

@Environment(EnvType.CLIENT)
public class BooleanSetting extends Setting {
   private boolean state;
   public String description;
   public Animation anim = new Animation();

   public BooleanSetting(String name, boolean state) {
      this.name = name;
      this.state = state;
      this.description = this.description;
   }

   public boolean get() {
      return this.state;
   }

   public void set(boolean state) {
      this.state = state;
   }

   public BooleanSetting hidden(Supplier<Boolean> hidden) {
      this.hidden = hidden;
      return this;
   }
}