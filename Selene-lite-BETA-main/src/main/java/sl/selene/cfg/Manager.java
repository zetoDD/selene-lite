package sl.selene.cfg;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class Manager<T> {
   private List<T> contents = new ArrayList<>();

   public List<T> getContents() {
      return this.contents;
   }

   public void setContents(ArrayList<T> contents) {
      this.contents = contents;
   }
}