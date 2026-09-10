package sl.selene.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.EventInit;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.SliderSetting;

@IModule(
   name = "FullBright",
   description = "Raises the gamma so the world is always bright",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class FullBright extends Module {

   public static SliderSetting brightness = new SliderSetting("Brightness", 1.0F, 0.0F, 1.0F, 0.01F, false);

   private double previousGamma = -1.0D;

   public FullBright() {
      this.addSettings(new Setting[] { brightness });
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (mc.options != null && this.previousGamma < 0.0D) {
         this.previousGamma = mc.options.getGamma().getValue();
      }
      if (mc.worldRenderer != null) {
         mc.worldRenderer.reload();
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (mc.options != null) {
         if (this.previousGamma >= 0.0D) {
            mc.options.getGamma().setValue(this.previousGamma);
         }
         this.previousGamma = -1.0D;
      }
      if (mc.worldRenderer != null) {
         mc.worldRenderer.reload();
      }
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null || mc.options == null) {
         return;
      }
      mc.options.getGamma().setValue((double) brightness.get());
   }
}