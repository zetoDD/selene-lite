package sl.selene.ui.gui.widget.bind.editor;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.ui.gui.widget.bind.BindPopupModel;
import sl.selene.ui.gui.widget.settings.ComboWidget;
import sl.selene.ui.gui.widget.settings.SettingValueAccessor;

@Environment(EnvType.CLIENT)
public final class BindPopupComboEditor extends AbstractBindPopupSettingEditor<String> {
   public BindPopupComboEditor(BindPopupModel model, ModeSetting setting) {
      super(Objects.requireNonNull(model, "model"), createWidget(model, Objects.requireNonNull(setting, "setting")));
   }

   private static ComboWidget createWidget(BindPopupModel model, ModeSetting setting) {
      final String defaultValue = setting.currentMode != null ? setting.currentMode : "";
      SettingValueAccessor<String> accessor = accessorFor(model, defaultValue, new AbstractBindPopupSettingEditor.ValueAdapter<String>() {
         public String read(BindPopupModel target) {
            Object value = target.getEditableTargetValue();
            return value != null ? value.toString() : defaultValue;
         }

         public void write(BindPopupModel target, String value) {
            String sanitized = value != null ? value : defaultValue;
            target.setEditableTargetValue(sanitized);
         }
      });
      return new ComboWidget(requireModule(model), noPopupContext(), setting, accessor, "New Value");
   }
}