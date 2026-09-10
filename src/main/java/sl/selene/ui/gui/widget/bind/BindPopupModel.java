package sl.selene.ui.gui.widget.bind;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.ListSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.module.api.setting.impl.StringSetting;
import sl.selene.module.bind.BindingMode;
import sl.selene.ui.gui.widget.settings.HSBColor;

@Environment(EnvType.CLIENT)
public final class BindPopupModel {
   private final BindPopupModel.TargetType targetType;
   private final Module module;
   private final Setting setting;
   private final String title;
   private final String subtitle;
   private final Object valueSnapshot;
   private final String infoMessage;
   private Object targetValue;
   private Object targetValueSnapshot;
   private int keyCode;
   private BindingMode mode;
   private int originalKeyCode;
   private BindingMode originalMode;
   private boolean cleared;
   private boolean originalCleared;

   private BindPopupModel(
      BindPopupModel.TargetType targetType,
      Module module,
      Setting setting,
      String title,
      String subtitle,
      Object valueSnapshot,
      String infoMessage,
      Object targetValue,
      Object targetValueSnapshot,
      int keyCode,
      BindingMode mode
   ) {
      this.targetType = Objects.requireNonNull(targetType, "targetType");
      this.module = module;
      this.setting = setting;
      this.title = title == null ? "" : title;
      this.subtitle = subtitle == null ? "" : subtitle;
      this.valueSnapshot = valueSnapshot;
      this.infoMessage = infoMessage == null ? "" : infoMessage;
      this.targetValue = targetValue;
      this.targetValueSnapshot = targetValueSnapshot;
      this.keyCode = keyCode;
      this.mode = Objects.requireNonNull(mode, "mode");
      this.originalKeyCode = keyCode;
      this.originalMode = mode;
      this.cleared = keyCode == -1;
      this.originalCleared = this.cleared;
   }

   public static BindPopupModel forModule(Module module) {
      Objects.requireNonNull(module, "module");
      int keyCode = module.bind > 0 || module.bind <= -100 ? module.bind : -1;
      return new BindPopupModel(
         BindPopupModel.TargetType.MODULE,
         module,
         null,
         module.name,
         module.description,
         module.enable,
         "Modules toggle state is controlled by the mode.",
         null,
         null,
         keyCode,
         BindingMode.TOGGLE
      );
   }

   public static BindPopupModel forSetting(Module module, Setting setting, Object currentValue, Object defaultValue, int keyCode, BindingMode mode) {
      Objects.requireNonNull(module, "module");
      Objects.requireNonNull(setting, "setting");
      return new BindPopupModel(
         BindPopupModel.TargetType.SETTING, module, setting, setting.name, module.name, currentValue, "", defaultValue, defaultValue, keyCode, mode
      );
   }

   public BindPopupModel.TargetType targetType() {
      return this.targetType;
   }

   public Module module() {
      return this.module;
   }

   public Setting setting() {
      return this.setting;
   }

   public String title() {
      return this.title;
   }

   public String subtitle() {
      return this.subtitle;
   }

   public Object valueSnapshot() {
      return this.valueSnapshot;
   }

   public String infoMessage() {
      return this.infoMessage;
   }

   public Object getTargetValue() {
      return !this.isSettingTarget() ? null : this.targetValue;
   }

   public void setTargetValue(Object targetValue) {
      this.ensureSettingTarget();
      this.targetValue = this.sanitizeTargetValue(targetValue);
      this.cleared = false;
   }

   public int getKeyCode() {
      return this.keyCode;
   }

   public void setKeyCode(int keyCode) {
      if (keyCode == -1 || keyCode >= 32 && keyCode <= 348) {
         this.keyCode = keyCode;
         if (keyCode != -1) {
            this.cleared = false;
         }
      } else {
         throw new IllegalArgumentException("keyCode must be GLFW.GLFW_KEY_UNKNOWN or a valid GLFW key constant");
      }
   }

   public BindingMode getMode() {
      return this.mode;
   }

   public void setMode(BindingMode mode) {
      this.mode = Objects.requireNonNull(mode, "mode");
   }

   public boolean isModuleTarget() {
      return this.targetType == BindPopupModel.TargetType.MODULE;
   }

   public boolean isSettingTarget() {
      return this.targetType == BindPopupModel.TargetType.SETTING;
   }

   public boolean hasExtendedValueBlock() {
      return this.targetType == BindPopupModel.TargetType.SETTING && this.setting != null
         ? this.setting instanceof SliderSetting || this.setting instanceof ModeSetting || this.setting instanceof ListSetting
         : false;
   }

   public boolean hasInfoMessage() {
      return !this.infoMessage.isBlank();
   }

   public boolean isDirty() {
      return this.keyCode != this.originalKeyCode || this.mode != this.originalMode || this.cleared != this.originalCleared || this.isTargetDirty();
   }

   public boolean isTargetDirty() {
      return this.isSettingTarget() && this.setting != null ? !valuesEqual(this.setting, this.targetValue, this.targetValueSnapshot) : false;
   }

   public void resetTargetValue() {
      this.ensureSettingTarget();
      this.cleared = this.originalCleared;
   }

   public void commit() {
      this.confirmChanges();
   }

   public void confirmChanges() {
      this.originalKeyCode = this.keyCode;
      this.originalMode = this.mode;
      this.originalCleared = this.cleared;
      if (this.isSettingTarget() && this.setting != null) {
         this.targetValueSnapshot = cloneValue(this.setting, this.targetValue);
      }
   }

   public void restoreOriginalState() {
      this.keyCode = this.originalKeyCode;
      this.mode = this.originalMode;
      this.cleared = this.originalCleared;
      if (this.isSettingTarget()) {
      }
   }

   public void markCleared() {
      this.keyCode = -1;
      this.cleared = true;
      if (this.isSettingTarget()) {
      }
   }

   public boolean isCleared() {
      return this.cleared;
   }

   public Object getEditableTargetValue() {
      this.ensureSettingTarget();
      return cloneValue(this.setting, this.targetValue);
   }

   public void setEditableTargetValue(Object value) {
      this.ensureSettingTarget();
      this.targetValue = this.sanitizeTargetValue(Objects.requireNonNull(value, "value"));
      this.cleared = false;
   }

   public String moduleName() {
      return this.module != null ? this.module.name : "";
   }

   public String settingName() {
      return this.setting != null ? this.setting.name : "";
   }

   private void ensureSettingTarget() {
      if (!this.isSettingTarget()) {
         throw new IllegalStateException("Operation only supported for setting targets");
      } else if (this.setting == null) {
         throw new IllegalStateException("Setting context is not available");
      }
   }

   private Object sanitizeTargetValue(Object value) {
      Objects.requireNonNull(value, "value");
      if (this.setting instanceof BooleanSetting) {
         if (value instanceof Boolean bool) {
            return bool;
         } else if (value instanceof Number number) {
            return number.doubleValue() != 0.0;
         } else {
            throw new IllegalArgumentException("Target value must be boolean-compatible");
         }
      } else if (this.setting instanceof SliderSetting) {
         return this.sanitizeSliderValue(value);
      } else if (this.setting instanceof ModeSetting) {
         return this.sanitizeComboValue(value);
      } else if (this.setting instanceof ListSetting) {
         return this.sanitizeSelectableValue(value);
      } else if (this.setting instanceof HueSetting) {
         return this.sanitizeColorValue(value);
      } else {
         return value instanceof String ? value : value.toString();
      }
   }

   private Object sanitizeSliderValue(Object value) {
      if (this.setting instanceof SliderSetting sliderSetting) {
         if (value instanceof Number number) {
            double raw = number.doubleValue();
            if (!Double.isNaN(raw) && !Double.isInfinite(raw)) {
               double clamped = Math.min(Math.max(raw, (double)sliderSetting.minimum), (double)sliderSetting.maximum);
               double steps = Math.round((clamped - sliderSetting.minimum) / sliderSetting.increment);
               double snapped = sliderSetting.minimum + steps * sliderSetting.increment;
               if (snapped < sliderSetting.minimum) {
                  snapped = sliderSetting.minimum;
               } else if (snapped > sliderSetting.maximum) {
                  snapped = sliderSetting.maximum;
               }

               return snapped;
            } else {
               throw new IllegalArgumentException("Target value must be a finite number");
            }
         } else {
            throw new IllegalArgumentException("Target value must be numeric");
         }
      } else {
         throw new IllegalStateException("Setting is not a SliderSetting");
      }
   }

   private Object sanitizeComboValue(Object value) {
      if (this.setting instanceof ModeSetting modeSetting) {
         String var4 = value.toString();
         if (modeSetting.modes != null && modeSetting.modes.contains(var4)) {
            return var4;
         } else {
            throw new IllegalArgumentException("Unsupported option '" + var4 + "'");
         }
      } else {
         throw new IllegalStateException("Setting is not a ModeSetting");
      }
   }

   private Object sanitizeSelectableValue(Object value) {
      if (!(this.setting instanceof ListSetting listSetting)) {
         throw new IllegalStateException("Setting is not a ListSetting");
      } else if (!(value instanceof Collection<?> collection)) {
         throw new IllegalArgumentException("Target value must be a collection");
      } else {
         LinkedHashSet selections = new LinkedHashSet();

         for (Object element : collection) {
            if (element != null) {
               String option = element.toString();
               if (listSetting.list == null || !listSetting.list.contains(option)) {
                  throw new IllegalArgumentException("Unsupported option '" + option + "'");
               }

               selections.add(option);
            }
         }

         return selections;
      }
   }

   private Object sanitizeColorValue(Object value) {
      if (this.setting instanceof HueSetting hueSetting) {
         if (value instanceof HSBColor hsbColor) {
            return hsbColor;
         } else if (value instanceof Number number) {
            return HSBColor.fromRgba(number.intValue());
         } else if (value instanceof String hex) {
            try {
               String stripped = hex.startsWith("#") ? hex.substring(1) : hex;
               int parsed = (int)Long.parseUnsignedLong(stripped, 16);
               int argb = stripped.length() > 6 ? parsed : 0xFF000000 | parsed;
               return HSBColor.fromRgba(argb);
            } catch (NumberFormatException var7) {
               throw new IllegalArgumentException("Invalid colour string: " + hex, var7);
            }
         } else {
            return HSBColor.of(hueSetting.current, hueSetting.saturation, hueSetting.brightness, 1.0F);
         }
      } else {
         throw new IllegalStateException("Setting is not a HueSetting");
      }
   }


   private static Object decodeSwitchValue(JsonElement element, Object defaultValue, Setting setting) {
      boolean value = defaultValue instanceof Boolean bool ? bool : Boolean.FALSE;
      if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
         value = element.getAsBoolean();
      }

      return value;
   }

   private static Object decodeSliderValue(JsonElement element, Setting setting, Object defaultValue) {
      if (setting instanceof SliderSetting sliderSetting) {
         double raw = defaultValue instanceof Number number ? number.doubleValue() : sliderSetting.current;
         if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            raw = element.getAsDouble();
         }

         double clamped = Math.min(Math.max(raw, (double)sliderSetting.minimum), (double)sliderSetting.maximum);
         double steps = Math.round((clamped - sliderSetting.minimum) / sliderSetting.increment);
         double snapped = sliderSetting.minimum + steps * sliderSetting.increment;
         if (snapped < sliderSetting.minimum) {
            snapped = sliderSetting.minimum;
         } else if (snapped > sliderSetting.maximum) {
            snapped = sliderSetting.maximum;
         }

         return snapped;
      } else {
         throw new IllegalStateException("Setting is not a SliderSetting");
      }
   }

   private static Object decodeComboValue(JsonElement element, Setting setting, Object defaultValue) {
      if (setting instanceof ModeSetting modeSetting) {
         String value = defaultValue instanceof String str ? str : (modeSetting.currentMode != null ? modeSetting.currentMode : "");
         if (element != null && element.isJsonPrimitive()) {
            value = element.getAsString();
         }

         if (modeSetting.modes == null || !modeSetting.modes.contains(value)) {
            value = modeSetting.currentMode != null ? modeSetting.currentMode : "";
         }

         return value;
      } else {
         throw new IllegalStateException("Setting is not a ModeSetting");
      }
   }


   private static Object decodeSelectableValue(JsonElement element, Setting setting, Object defaultValue) {
      if (!(setting instanceof ListSetting listSetting)) {
         throw new IllegalStateException("Setting is not a ListSetting");
      } else {
         LinkedHashSet selections = new LinkedHashSet();
         if (element != null && element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
               if (entry.isJsonPrimitive()) {
                  String option = entry.getAsString();
                  if (listSetting.list != null && listSetting.list.contains(option)) {
                     selections.add(option);
                  }
               }
            }
         }

         return selections;
      }
   }

   private static Object decodeColorValue(JsonElement element, Setting setting, Object defaultValue) {
      if (setting instanceof HueSetting hueSetting) {
         HSBColor fallback;
         if (defaultValue instanceof HSBColor hsbColor) {
            fallback = hsbColor;
         } else if (defaultValue instanceof Number number) {
            fallback = HSBColor.fromRgba(number.intValue());
         } else if (defaultValue instanceof String hex) {
            try {
               String stripped = hex.startsWith("#") ? hex.substring(1) : hex;
               int parsed = (int)Long.parseUnsignedLong(stripped, 16);
               int argb = stripped.length() > 6 ? parsed : 0xFF000000 | parsed;
               fallback = HSBColor.fromRgba(argb);
            } catch (NumberFormatException var11) {
               fallback = HSBColor.of(hueSetting.current, hueSetting.saturation, hueSetting.brightness, 1.0F);
            }
         } else {
            fallback = HSBColor.of(hueSetting.current, hueSetting.saturation, hueSetting.brightness, 1.0F);
         }

         return fallback;
      } else {
         throw new IllegalStateException("Setting is not a HueSetting");
      }
   }


   private static Object cloneValue(Setting setting, Object value) {
      if (setting == null || value == null) {
         return value;
      } else if (setting instanceof BooleanSetting) {
         return Boolean.TRUE.equals(value);
      } else if (setting instanceof SliderSetting) {
         return ((Number)value).doubleValue();
      } else if (setting instanceof ModeSetting || setting instanceof StringSetting) {
         return value.toString();
      } else if (setting instanceof ListSetting) {
         LinkedHashSet<String> copy = new LinkedHashSet<>();
         if (value instanceof Collection) {
            for (Object element : (Collection)value) {
               if (element != null) {
                  copy.add(element.toString());
               }
            }
         }

         return copy;
      } else {
         return setting instanceof HueSetting ? cloneColorValue(value) : value;
      }
   }

   private static boolean valuesEqual(Setting setting, Object a, Object b) {
      if (a == b) {
         return true;
      } else if (a == null || b == null) {
         return false;
      } else if (setting instanceof BooleanSetting || setting instanceof ModeSetting || setting instanceof StringSetting) {
         return Objects.equals(a, b);
      } else if (setting instanceof SliderSetting) {
         return Math.abs(((Number)a).doubleValue() - ((Number)b).doubleValue()) <= 1.0E-6;
      } else if (setting instanceof ListSetting) {
         if (!(a instanceof Collection<?> aCol && b instanceof Collection<?> bCol)) {
            return false;
         } else {
            return aCol.size() != bCol.size() ? false : new LinkedHashSet<>(convertToStrings(aCol)).equals(new LinkedHashSet<>(convertToStrings(bCol)));
         }
      } else if (setting instanceof HueSetting) {
         if (a instanceof HSBColor colorA && b instanceof HSBColor colorB) {
            return colorA.equals(colorB);
         } else if (a instanceof Number numberA && b instanceof Number numberB) {
            return numberA.intValue() == numberB.intValue();
         } else {
            return a instanceof String strA && b instanceof String strB ? strA.equalsIgnoreCase(strB) : false;
         }
      } else {
         return Objects.equals(a, b);
      }
   }

   private static List<String> convertToStrings(Collection<?> collection) {
      List<String> list = new ArrayList<>(collection.size());

      for (Object element : collection) {
         if (element != null) {
            list.add(element.toString());
         }
      }

      return list;
   }

   private static HSBColor cloneColorValue(Object value) {
      if (value instanceof HSBColor hsbColor) {
         return HSBColor.of(hsbColor.hue(), hsbColor.saturation(), hsbColor.brightness(), hsbColor.alpha());
      } else if (value instanceof Number number) {
         return HSBColor.fromRgba(number.intValue());
      } else if (value instanceof String hex) {
         try {
            String stripped = hex.startsWith("#") ? hex.substring(1) : hex;
            int parsed = (int)Long.parseUnsignedLong(stripped, 16);
            int argb = stripped.length() > 6 ? parsed : 0xFF000000 | parsed;
            return HSBColor.fromRgba(argb);
         } catch (NumberFormatException var5) {
            throw new IllegalArgumentException("Invalid colour string: " + hex, var5);
         }
      } else {
         throw new IllegalArgumentException("Unsupported colour value type: " + value.getClass().getName());
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum TargetType {
      MODULE,
      SETTING;
   }
}