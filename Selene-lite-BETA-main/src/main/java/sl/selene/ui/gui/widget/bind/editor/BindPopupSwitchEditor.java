package sl.selene.ui.gui.widget.bind.editor;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.ui.gui.widget.bind.BindPopupModel;
import sl.selene.ui.gui.widget.settings.BooleanWidget;
import sl.selene.ui.gui.widget.settings.SettingValueAccessor;

@Environment(EnvType.CLIENT)
public final class BindPopupSwitchEditor extends AbstractBindPopupSettingEditor<Boolean> {
   public BindPopupSwitchEditor(BindPopupModel model, BooleanSetting setting) {
      super(Objects.requireNonNull(model, "model"), createWidget(model, Objects.requireNonNull(setting, "setting")));
   }

   private static BooleanWidget createWidget(BindPopupModel model, BooleanSetting setting) {
      Boolean defaultValue = Boolean.FALSE;
      SettingValueAccessor<Boolean> accessor = accessorFor(model, defaultValue, new AbstractBindPopupSettingEditor.ValueAdapter<Boolean>() {
         public Boolean read(BindPopupModel target) {
            Object value = target.getEditableTargetValue();
            return Boolean.TRUE.equals(value);
         }

         public void write(BindPopupModel target, Boolean value) {
            target.setEditableTargetValue(Boolean.TRUE.equals(value));
         }
      });
      return new BooleanWidget(requireModule(model), noPopupContext(), setting, accessor, "New Value");
   }
}