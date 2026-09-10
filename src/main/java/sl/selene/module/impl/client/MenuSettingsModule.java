package sl.selene.module.impl.client;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.ModeSetting;

@IModule(
   name = "Menu",
   description = "Controls how the client menu opens and closes.",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class MenuSettingsModule extends Module {
   public static MenuSettingsModule INSTANCE;
   public ModeSetting openingAnimations = new ModeSetting("Animation", "Sliding", "None", "Smoothy", "Sliding", "Cascade");
   public ModeSetting menuScale = new ModeSetting("Scale", "Auto", "80%", "Auto", "120%");

   public MenuSettingsModule() {
      this.addSettings(new Setting[]{this.openingAnimations, this.menuScale});
      INSTANCE = this;
   }

   public static MenuSettingsModule getInstanceIfAvailable() {
      return INSTANCE;
   }

   public boolean isOpeningAnimationEnabled(MenuSettingsModule.AnimationTrack track) {
      Objects.requireNonNull(track, "track");
      String mode = this.openingAnimations.get();

      return switch (mode) {
         case "None" -> false;
         case "Smoothy" -> track == MenuSettingsModule.AnimationTrack.SCALE || track == MenuSettingsModule.AnimationTrack.FADE;
         case "Sliding" -> track == MenuSettingsModule.AnimationTrack.SCALE
            || track == MenuSettingsModule.AnimationTrack.FADE
            || track == MenuSettingsModule.AnimationTrack.SLIDE;
         case "Cascade" -> track == MenuSettingsModule.AnimationTrack.FADE
            || track == MenuSettingsModule.AnimationTrack.SCALE
            || track == MenuSettingsModule.AnimationTrack.CASCADE;
         default -> true;
      };
   }

   public double getMenuScaleValue() {
      String value = this.menuScale.get();
      double autoScale = this.calculateAutoScale();
      if ("Auto".equals(value)) {
         return autoScale;
      } else {
         try {
            double percentage = Double.parseDouble(value.replace("%", "")) / 100.0;
            if ("120%".equals(value)) {
               percentage = 1.12;
            }

            return autoScale * percentage;
         } catch (NumberFormatException var6) {
            return autoScale;
         }
      }
   }

   private double calculateAutoScale() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.getWindow() != null) {
         int screenWidth = client.getWindow().getFramebufferWidth();
         int screenHeight = client.getWindow().getFramebufferHeight();
         double baseWidth = 2560.0;
         double baseHeight = 1440.0;
         double screenArea = screenWidth * screenHeight;
         double baseArea = baseWidth * baseHeight;
         double scale = Math.sqrt(screenArea / baseArea);
         scale = Math.max(0.5, Math.min(1.2, scale));
         return Math.round(scale * 20.0) / 20.0;
      } else {
         return 0.8;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum AnimationTrack {
      FADE,
      SCALE,
      SLIDE,
      CASCADE;
   }
}