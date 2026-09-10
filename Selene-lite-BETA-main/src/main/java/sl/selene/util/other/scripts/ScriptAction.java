package sl.selene.util.other.scripts;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface ScriptAction {
   void perform();
}