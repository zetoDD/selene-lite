package sl.selene.util.render.text;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class FontObject {
   public final String id;

   public FontObject(String id) {
      this.id = Objects.requireNonNull(id, "id");
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         FontObject that = (FontObject)o;
         return this.id.equals(that.id);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return "FontObject(" + this.id + ")";
   }
}