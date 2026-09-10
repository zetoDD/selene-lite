package sl.selene.ui.gui.widget.settings;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.ListSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.SliderSetting;

@Environment(EnvType.CLIENT)
public interface SettingValueAccessor<T> {
   T get();

   void set(T var1);

   T getDefault();

   void reset();

   static void autoSaveConfig() {
      if (Selene.get != null && Selene.get.configManager != null) {
         Selene.get.configManager.autoSave();
      }
   }

   static SettingValueAccessor<?> forSetting(Setting setting) {
      Objects.requireNonNull(setting, "setting");
      if (setting instanceof BooleanSetting booleanSetting) {
         return new SettingValueAccessor<Boolean>() {
            public Boolean get() {
               return booleanSetting.get();
            }

            public void set(Boolean value) {
               if (value != null) {
                  booleanSetting.set(value);
                  SettingValueAccessor.autoSaveConfig();
               }
            }

            public Boolean getDefault() {
               return false;
            }

            @Override
            public void reset() {
               booleanSetting.set(false);
               SettingValueAccessor.autoSaveConfig();
            }
         };
      } else if (setting instanceof SliderSetting sliderSetting) {
         return new SettingValueAccessor<Double>() {
            public Double get() {
               return (double)sliderSetting.get();
            }

            public void set(Double value) {
               if (value != null) {
                  sliderSetting.current = value.floatValue();
                  SettingValueAccessor.autoSaveConfig();
               }
            }

            public Double getDefault() {
               return (double)sliderSetting.minimum;
            }

            @Override
            public void reset() {
               sliderSetting.current = sliderSetting.minimum;
               SettingValueAccessor.autoSaveConfig();
            }
         };
      } else if (setting instanceof ModeSetting modeSetting) {
         return new SettingValueAccessor<String>() {
            public String get() {
               return modeSetting.get();
            }

            public void set(String value) {
               if (modeSetting.modes.contains(value)) {
                  modeSetting.currentMode = value;
                  modeSetting.index = modeSetting.modes.indexOf(value);
                  SettingValueAccessor.autoSaveConfig();
               }
            }

            public String getDefault() {
               return modeSetting.modes.isEmpty() ? "" : modeSetting.modes.get(0);
            }

            @Override
            public void reset() {
               if (!modeSetting.modes.isEmpty()) {
                  modeSetting.currentMode = modeSetting.modes.get(0);
                  modeSetting.index = 0;
                  SettingValueAccessor.autoSaveConfig();
               }
            }
         };
      } else if (setting instanceof ListSetting listSetting) {
         return new SettingValueAccessor<Set<String>>() {
            public Set<String> get() {
               return new LinkedHashSet<>(listSetting.selected != null ? listSetting.selected : List.of());
            }

            public void set(Set<String> value) {
               if (value != null) {
                  listSetting.selected = new ArrayList<>(value);
               } else {
                  listSetting.selected = new ArrayList<>();
               }
               SettingValueAccessor.autoSaveConfig();
            }

            public Set<String> getDefault() {
               return new LinkedHashSet<>();
            }

            @Override
            public void reset() {
               listSetting.selected = new ArrayList<>();
               SettingValueAccessor.autoSaveConfig();
            }
         };
      } else {
         return setting instanceof HueSetting hueSetting ? new SettingValueAccessor<HSBColor>() {
            public HSBColor get() {
               float hue = hueSetting.current / hueSetting.maximum * 360.0F;
               return HSBColor.of(hue, hueSetting.saturation, hueSetting.brightness, 1.0F);
            }

            public void set(HSBColor value) {
               if (value != null) {
                  hueSetting.current = value.hue() / 360.0F * hueSetting.maximum;
                  hueSetting.saturation = value.saturation();
                  hueSetting.brightness = value.brightness();
                  SettingValueAccessor.autoSaveConfig();
               }
            }

            public HSBColor getDefault() {
               return HSBColor.of(0.0F, 1.0F, 1.0F, 1.0F);
            }

            @Override
            public void reset() {
               hueSetting.current = 0.0F;
               hueSetting.saturation = 1.0F;
               hueSetting.brightness = 1.0F;
               SettingValueAccessor.autoSaveConfig();
            }
         } : new SettingValueAccessor<Object>() {
            @Override
            public Object get() {
               return null;
            }

            @Override
            public void set(Object value) {
            }

            @Override
            public Object getDefault() {
               return null;
            }

            @Override
            public void reset() {
            }
         };
      }
   }
}