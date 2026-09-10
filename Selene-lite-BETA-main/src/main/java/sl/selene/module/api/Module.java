package sl.selene.module.api;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.Selene;
import sl.selene.event.EventManager;
import sl.selene.module.api.setting.Config;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BindSettings;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.ListSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.MultiBooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.module.api.setting.impl.StringSetting;
import sl.selene.module.impl.visuals.Hud;
import sl.selene.util.render.animation.util.Animation;
import sl.selene.util.render.math.animation.Translate;
import sl.selene.util.render.math.animation.anim.util.Animation2;
import sl.selene.util.render.math.animation.anim.util.Easings;
import sl.selene.util.render.math.animation.impl.EaseInOutQuad;
import sl.selene.util.render.utils.SoundUtil;

@Environment(EnvType.CLIENT)
public class Module extends Config {

   public IModule module = this.getClass().getAnnotation(IModule.class);
   public static MinecraftClient mc = MinecraftClient.getInstance();
   public String name;
   public int bind;
   public boolean enable;
   public boolean open = false;
   public Category category;
   public String displayName;
   public String description;
   public boolean binding;

   public long lastToggleTime;

   public boolean holdOnly;
   public boolean isRender = true;
   public Translate a = new Translate(0.0F, 0.0F);
   public Animation animation = new Animation();
   public sl.selene.util.render.math.animation.Animation animation1 = new EaseInOutQuad(300, 1.0);
   public sl.selene.util.render.math.animation.Animation animation2 = new EaseInOutQuad(300, 1.0);
   public final Animation2 mAnim = new Animation2();
   public sl.selene.util.render.math.animation.Animation animation3 = new EaseInOutQuad(300, 1.0);
   public sl.selene.util.render.math.animation.Animation animation4 = new EaseInOutQuad(300, 1.0);
   private static boolean configLoadInProgress;

   public static boolean isConfigLoadInProgress() {
      return configLoadInProgress;
   }

   public static void beginConfigLoad() {
      configLoadInProgress = true;
   }

   public static void endConfigLoad() {
      configLoadInProgress = false;
   }

   public Module() {
      this.name = this.module.name();
      this.category = this.module.category();
      if (this.module.bind() == 0) {
         this.bind = -1;
      } else {
         this.bind = this.module.bind();
      }

      this.enable = false;
      this.description = this.module.description();
      this.displayName = this.name;
   }

   public void onEnable() {
      System.out.println("[Module] Enabling: " + this.name);

      try {
         EventManager.register(this);
      } catch (Exception var2) {
         System.err.println("[Module] Failed to enable " + this.name + ": " + var2.getMessage());
         var2.printStackTrace();
         this.enable = false;
         return;
      }

      if (mc.player != null) {
         Selene.get.manager.get(Hud.class).showNotification(this.name + " enabled", Hud.NotificationType.SUCCESS, 2400L);
         SoundUtil.playSound_wav("on", 0.35F);
      }

      this.mAnim.run(1.0, 0.24F, Easings.QUART_OUT);
   }

   public void onDisable() {
      EventManager.unregister(this);
      if (mc.player != null) {
         Selene.get.manager.get(Hud.class).showNotification(this.name + " disabled", Hud.NotificationType.WARNING, 2400L);
         SoundUtil.playSound_wav("off", 0.35F);
      }

      this.mAnim.run(0.0, 0.24F, Easings.QUART_OUT);
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getArrayListSuffix() {
      return null;
   }

   protected static String fmt(float value) {
      String s = Float.toString(value);
      if (s.endsWith(".0")) {
         s = s.substring(0, s.length() - 2);
      }
      return s;
   }

   public void toggle() {
      this.enable = !this.enable;
      this.lastToggleTime = System.nanoTime();
      if (this.enable) {
         this.onEnable();
      } else {
         this.onDisable();
      }

      if (!configLoadInProgress && Selene.get.configManager != null) {
         Selene.get.configManager.autoSave();
      }
   }

   public JsonObject save() {
      JsonObject object = new JsonObject();
      if (this.enable) {
         object.addProperty("enable", this.enable);
      }

      if (this.bind > 0 || this.bind <= -100) {
         object.addProperty("keyIndex", this.bind);
      }

      JsonObject propertiesObject = new JsonObject();

      for (Setting set : this.getSettings()) {
         if (set instanceof BooleanSetting) {
            propertiesObject.addProperty(set.name, ((BooleanSetting)set).get());
         } else if (set instanceof ModeSetting) {
            propertiesObject.addProperty(set.name, ((ModeSetting)set).currentMode);
         } else if (set instanceof SliderSetting) {
            propertiesObject.addProperty(set.name, ((SliderSetting)set).current);
         } else if (set instanceof BindSettings) {
            propertiesObject.addProperty(set.name, ((BindSettings)set).key);
         } else if (set instanceof StringSetting) {
            propertiesObject.addProperty(set.name, ((StringSetting)set).input);
         } else if (set instanceof HueSetting hueSetting) {
            JsonObject colorObject = new JsonObject();
            colorObject.addProperty("hue", hueSetting.current);
            colorObject.addProperty("saturation", hueSetting.saturation);
            colorObject.addProperty("brightness", hueSetting.brightness);
            propertiesObject.add(set.name, colorObject);
         } else if (set instanceof MultiBooleanSetting) {
            JsonObject multiBoolObject = new JsonObject();

            for (BooleanSetting boolSetting : ((MultiBooleanSetting)set).settings) {
               multiBoolObject.addProperty(boolSetting.name, boolSetting.get());
            }

            propertiesObject.add(set.name, multiBoolObject);
         }
      }

      object.add("Settings", propertiesObject);
      return object;
   }

   public void load(JsonObject object) {
      if (object != null) {
         if (object.has("enable")) {
            this.setState(object.get("enable").getAsBoolean());
         }

         if (object.has("keyIndex")) {
            this.bind = object.get("keyIndex").getAsInt();
         }

         for (Setting set : this.getSettings()) {
            JsonObject propertiesObject = object.getAsJsonObject("Settings");
            if (set != null && propertiesObject != null && propertiesObject.has(set.name)) {
               if (set instanceof BooleanSetting) {
                  ((BooleanSetting)set).set(propertiesObject.get(set.name).getAsBoolean());
               } else if (set instanceof ModeSetting) {
                  ((ModeSetting)set).currentMode = propertiesObject.get(set.name).getAsString();
               } else if (set instanceof SliderSetting) {
                  ((SliderSetting)set).current = propertiesObject.get(set.name).getAsFloat();
               } else if (set instanceof BindSettings) {
                  ((BindSettings)set).key = propertiesObject.get(set.name).getAsInt();
               } else if (set instanceof StringSetting) {
                  ((StringSetting)set).input = propertiesObject.get(set.name).getAsString();
               } else if (set instanceof HueSetting hueSetting) {
                  if (propertiesObject.get(set.name).isJsonObject()) {
                     JsonObject colorObject = propertiesObject.getAsJsonObject(set.name);
                     if (colorObject.has("hue")) {
                        hueSetting.current = colorObject.get("hue").getAsFloat();
                     }
                     if (colorObject.has("saturation")) {
                        hueSetting.saturation = colorObject.get("saturation").getAsFloat();
                     }
                     if (colorObject.has("brightness")) {
                        hueSetting.brightness = colorObject.get("brightness").getAsFloat();
                     }
                  } else {
                     hueSetting.current = propertiesObject.get(set.name).getAsFloat();
                  }
               } else if (set instanceof MultiBooleanSetting) {
                  if (propertiesObject.get(set.name).isJsonObject()) {
                     JsonObject multiBoolObject = propertiesObject.getAsJsonObject(set.name);

                     for (BooleanSetting boolSetting : ((MultiBooleanSetting)set).settings) {
                        if (multiBoolObject.has(boolSetting.name)) {
                           boolSetting.set(multiBoolObject.get(boolSetting.name).getAsBoolean());
                        }
                     }
                  }
               } else if (set instanceof ListSetting) {
                  String[] split = propertiesObject.get(set.name).getAsString().split(",");
                  ((ListSetting)set).selected = new ArrayList<>();

                  for (String s : split) {
                     if (((ListSetting)set).list.contains(s)) {
                        ((ListSetting)set).selected.add(s);
                     }
                  }
               }
            }
         }
      }
   }

   public int getBind() {
      return this.bind;
   }

   public void setState(boolean enable) {
      if (this.enable == enable) {
         return;
      }

      this.enable = enable;
      if (configLoadInProgress) {
         if (enable) {
            try {
               EventManager.register(this);
               this.onConfigLoadEnable();
            } catch (Exception var2) {
               this.enable = false;
            }
         } else {
            EventManager.unregister(this);
            this.onConfigLoadDisable();
         }
         return;
      }

      if (enable) {
         this.onEnable();
      } else {
         this.onDisable();
      }
   }

   public void setEnable(boolean enable) {
      this.enable = !enable;
      if (enable) {
         this.onEnable();
      } else {
         this.onDisable();
      }
   }

   protected void onConfigLoadEnable() {
   }

   protected void onConfigLoadDisable() {
   }
}