package sl.selene.commands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class CommandException extends Exception {
   public CommandException(String message) {
      super(message);
   }
}