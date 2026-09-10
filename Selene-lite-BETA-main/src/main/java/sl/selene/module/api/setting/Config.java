package sl.selene.module.api.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Config {
   private final ArrayList<Setting> settingList = new ArrayList<>();

   public final void addSettings(Setting... options) {
      this.settingList.addAll(Arrays.asList(options));
   }

   public final List<Setting> getSettingsForGUI() {
      ArrayList<Setting> visible = new ArrayList<>(this.settingList.size());
      for (Setting setting : this.settingList) {
         if (!setting.hidden.get()) {
            visible.add(setting);
         }
      }

      return visible;
   }

   public final List<Setting> getSettings() {
      return this.settingList;
   }
}