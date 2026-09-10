package sl.selene.ui.gui.widget.bind.editor;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.ui.gui.widget.bind.BindPopupModel;
import sl.selene.ui.gui.widget.settings.SettingValueAccessor;
import sl.selene.ui.gui.widget.settings.SliderWidget;

@Environment(EnvType.CLIENT)
public final class BindPopupSliderEditor extends AbstractBindPopupSettingEditor<Double> {
   public BindPopupSliderEditor(BindPopupModel model, SliderSetting setting) {
      super(Objects.requireNonNull(model, "model"), createWidget(model, Objects.requireNonNull(setting, "setting")));
   }

   private static SliderWidget createWidget(BindPopupModel model, SliderSetting setting) {
      final double defaultValue = setting.current;
      SettingValueAccessor<Double> accessor = accessorFor(model, defaultValue, new AbstractBindPopupSettingEditor.ValueAdapter<Double>() {
         public Double read(BindPopupModel target) {
            return target.getEditableTargetValue() instanceof Number number ? number.doubleValue() : defaultValue;
         }

         public void write(BindPopupModel target, Double value) {
            double sanitized = value != null ? value : defaultValue;
            target.setEditableTargetValue(sanitized);
         }
      });
      return new SliderWidget(requireModule(model), noPopupContext(), setting, accessor, "New Value");
   }
}