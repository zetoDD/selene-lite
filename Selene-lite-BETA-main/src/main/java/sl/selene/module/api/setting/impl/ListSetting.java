package sl.selene.module.api.setting.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;

@Environment(EnvType.CLIENT)
public class ListSetting extends Setting {
   public List<String> list;
   public boolean opened;
   public String description;
   public List<String> selected = new ArrayList<>();

   public ListSetting(String name, String... settings) {
      this.name = name;
      this.list = Arrays.asList(settings);
      this.description = this.description;
   }

   public ListSetting hidden(Supplier<Boolean> hidden) {
      this.hidden = hidden;
      return this;
   }

   public String getFormattedList() {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < this.list.size(); i++) {
         sb.append(this.list.get(i));
         if (i == 2 && this.list.size() > 3) {
            sb.append("...");
            break;
         }

         if (i < this.list.size() - 1) {
            sb.append(", ");
         }
      }

      return sb.toString();
   }

   public boolean isSelected(String element) {
      return this.selected.contains(element);
   }
}