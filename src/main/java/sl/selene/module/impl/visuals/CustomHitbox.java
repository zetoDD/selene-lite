package sl.selene.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import sl.selene.Selene;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.MultiBooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;

@IModule(
   name = "Custom Hitbox",
   description = "Replaces F3+B hitboxes with customizable ones",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class CustomHitbox extends Module {
   public static final MultiBooleanSetting targets = new MultiBooleanSetting(
      "Show",
      new BooleanSetting("Players", true),
      new BooleanSetting("Mobs", false)
   );
   public static final HueSetting color = new HueSetting("Color", 120.0F);
   public static final SliderSetting lineWidth = new SliderSetting("Line Width", 1.5F, 0.5F, 4.0F, 0.1F, false);
   public static final BooleanSetting showEyeLine = new BooleanSetting("Eye Line", true);
   public static final BooleanSetting showLookVector = new BooleanSetting("Look Direction", true);
   public static final SliderSetting lookLength = new SliderSetting("Arrow Length", 1.2F, 0.3F, 3.0F, 0.1F, false)
      .hidden(() -> !showLookVector.get());

   public CustomHitbox() {
      this.addSettings(new Setting[] { targets, color, lineWidth, showEyeLine, showLookVector, lookLength });
   }

   public static boolean isActive() {
      CustomHitbox module = getModule();
      return module != null && module.enable;
   }

   public static CustomHitbox getModule() {
      if (Selene.get == null || Selene.get.manager == null) {
         return null;
      }

      return Selene.get.manager.get(CustomHitbox.class);
   }

   public static boolean shouldRenderEntity(Entity entity) {
      if (entity == null || !entity.isAlive()) {
         return false;
      }

      if (entity instanceof PlayerEntity) {
         return targets.get("Players");
      }

      return entity instanceof LivingEntity && targets.get("Mobs");
   }
}