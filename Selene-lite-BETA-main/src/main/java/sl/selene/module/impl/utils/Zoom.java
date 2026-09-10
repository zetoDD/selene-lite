package sl.selene.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BindSettings;
import sl.selene.module.api.setting.impl.SliderSetting;

@IModule(
   name = "Zoom",
   description = "Hold a key to zoom in (like Zoomify)",
   category = Category.Utils,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Zoom extends Module {
   public static final BindSettings zoomKey = new BindSettings("Zoom Key", GLFW.GLFW_KEY_C);
   public static final SliderSetting zoomFov = new SliderSetting("Zoom FOV", 30.0F, 10.0F, 110.0F, 1.0F, false);
   public static final SliderSetting zoomSpeed = new SliderSetting("Speed", 18.0F, 4.0F, 40.0F, 1.0F, false);

   private float blend;

   public Zoom() {
      this.addSettings(new Setting[] { zoomKey, zoomFov, zoomSpeed });
   }

   @Override
   public String getArrayListSuffix() {
      return fmt(zoomFov.get());
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.blend = 0.0F;
   }

   public float modifyFov(float vanillaFov, float tickDelta) {
      if (!this.enable || mc == null || mc.getWindow() == null) {
         return vanillaFov;
      }
      int key = zoomKey.get();
      if (key == 0) {
         return vanillaFov;
      }
      boolean hold = zoomKey.isKeyDown(key);
      float target = hold ? 1.0F : 0.0F;
      float dt = Math.max(0.0F, tickDelta);
      double speed = zoomSpeed.get();
      float step = 1.0F - (float) Math.exp(-dt * speed * 0.35);
      step = MathHelper.clamp(step, 0.0F, 1.0F);
      this.blend = MathHelper.lerp(step, this.blend, target);
      if (this.blend < 0.002F) {
         return vanillaFov;
      }
      return MathHelper.lerp(this.blend, vanillaFov, zoomFov.get());
   }
}