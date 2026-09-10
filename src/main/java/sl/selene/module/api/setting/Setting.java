package sl.selene.module.api.setting;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Setting extends Config {
   public String name;
   public Supplier<Boolean> hidden = () -> false;
}