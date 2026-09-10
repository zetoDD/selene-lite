package sl.selene.cfg;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface ConfigUpdater {
   JsonObject save();

   void load(JsonObject var1);
}