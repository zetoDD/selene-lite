package sl.selene.ui.gui.widget.bind.editor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.impl.ListSetting;
import sl.selene.ui.gui.widget.bind.BindPopupModel;
import sl.selene.ui.gui.widget.settings.SelectableWidget;
import sl.selene.ui.gui.widget.settings.SettingValueAccessor;

@Environment(EnvType.CLIENT)
public final class BindPopupSelectableEditor extends AbstractBindPopupSettingEditor<Set<String>> {
   public BindPopupSelectableEditor(BindPopupModel model, ListSetting setting) {
      super(Objects.requireNonNull(model, "model"), createWidget(model, Objects.requireNonNull(setting, "setting")));
   }

   private static SelectableWidget createWidget(BindPopupModel model, ListSetting setting) {
      final LinkedHashSet<String> defaultValue = new LinkedHashSet<>(setting.selected != null ? setting.selected : Collections.emptyList());
      SettingValueAccessor<Set<String>> accessor = accessorFor(model, defaultValue, new AbstractBindPopupSettingEditor.ValueAdapter<Set<String>>() {
         public Set<String> read(BindPopupModel target) {
            Object value = target.getEditableTargetValue();
            return this.copySelections(value, defaultValue);
         }

         public void write(BindPopupModel target, Set<String> value) {
            target.setEditableTargetValue(this.copySelections(value, defaultValue));
         }

         private LinkedHashSet<String> copySelections(Object source, Set<String> fallback) {
            LinkedHashSet<String> selections = new LinkedHashSet<>();
            Collection<?> collection = null;
            boolean hasSource = false;
            if (source instanceof Collection<?> c) {
               collection = c;
               hasSource = true;
            } else if (source instanceof Set<?> s) {
               collection = s;
               hasSource = true;
            }

            if (collection != null) {
               for (Object element : collection) {
                  if (element != null) {
                     selections.add(element.toString());
                  }
               }
            } else if (source instanceof Object[] array) {
               hasSource = true;

               for (Object elementx : array) {
                  if (elementx != null) {
                     selections.add(elementx.toString());
                  }
               }
            }

            if (!hasSource && fallback != null) {
               selections.addAll(fallback);
            }

            return selections;
         }
      });
      return new SelectableWidget(requireModule(model), noPopupContext(), setting, accessor, "New Value");
   }
}