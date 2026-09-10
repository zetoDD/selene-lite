package sl.selene.module.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.render.animation.util.Animation;

@Environment(EnvType.CLIENT)
public enum Category {
   Combat("Combat", "combat"),
   Movement("Movement", "movement"),
   Visuals("Visuals", "visuals"),
   Player("Player", "player"),
   Utils("Utils", "utils"),
   Misc("Misc", "misc"),
   Donut("Donut", "donut");

   private final String name;
   private final String icon;
   public Animation anim33 = new Animation();
   public Animation anim44 = new Animation();

   private Category(String name, String icon) {
      this.name = name;
      this.icon = icon;
   }

   public String getIcon() {
      return this.icon;
   }

   public String getName() {
      return this.name;
   }
}