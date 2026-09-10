package sl.selene.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventScreen;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.player.CrosshairTargetUtil;
import sl.selene.util.render.core.Renderer2D;

@IModule(
   name = "Crosshair",
   description = "Customizable crosshair",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Crosshair extends Module {
   public static final ModeSetting style = new ModeSetting("Style", "Cross", "Cross", "Dot", "Circle");
   public static final SliderSetting length = new SliderSetting("Length", 6.0F, 2.0F, 20.0F, 0.5F, false);
   public static final SliderSetting thickness = new SliderSetting("Thickness", 1.5F, 0.5F, 5.0F, 0.1F, false);
   public static final SliderSetting gap = new SliderSetting("Gap", 3.0F, 0.0F, 12.0F, 0.5F, false);
   public static final SliderSetting dotSize = new SliderSetting("Dot Size", 2.0F, 1.0F, 6.0F, 0.5F, false)
      .hidden(() -> !style.is("Dot"));
   public static final SliderSetting circleRadius = new SliderSetting("Circle Radius", 5.0F, 2.0F, 16.0F, 0.5F, false)
      .hidden(() -> !style.is("Circle"));
   public static final HueSetting color = new HueSetting("Color", 0.0F);
   public static final BooleanSetting entityHighlight = new BooleanSetting("Red on Entities", true);
   public static final HueSetting entityColor = new HueSetting("Entity Color", 0.0F, 1.0F, 1.0F)
      .hidden(() -> !entityHighlight.get());

   public Crosshair() {
      this.addSettings(
         new Setting[] { style, length, thickness, gap, dotSize, circleRadius, color, entityHighlight, entityColor }
      );
   }

   @EventInit
   public void onRender(EventScreen event) {
      if (!this.enable || mc.player == null || event == null) {
         return;
      }

      Renderer2D r2 = event.renderer();
      if (r2 == null) {
         return;
      }

      float centerX = event.viewportWidth() * 0.5F;
      float centerY = event.viewportHeight() * 0.5F;

      int drawColor = ColorUtil.replAlpha(color.getRGB(), 255);
      if (entityHighlight.get()) {
         LivingEntity target = CrosshairTargetUtil.getLivingCrosshairTarget();
         if (target != null) {
            drawColor = ColorUtil.replAlpha(entityColor.getRGB(), 255);
         }
      }

      float len = length.get();
      float thick = thickness.get();
      float g = gap.get();

      switch (style.get()) {
         case "Dot" -> r2.rect(centerX - dotSize.get(), centerY - dotSize.get(), dotSize.get() * 2.0F, dotSize.get() * 2.0F, drawColor);
         case "Circle" -> r2.circle(centerX, centerY, circleRadius.get(), 0.0F, 1.0F, drawColor);
         default -> {
            r2.rect(centerX - thick * 0.5F, centerY - g - len, thick, len, drawColor);
            r2.rect(centerX - thick * 0.5F, centerY + g, thick, len, drawColor);
            r2.rect(centerX - g - len, centerY - thick * 0.5F, len, thick, drawColor);
            r2.rect(centerX + g, centerY - thick * 0.5F, len, thick, drawColor);
         }
      }
   }
}