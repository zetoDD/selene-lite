package sl.selene.commands;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface Command {
   String name();

   List<String> aliases();

   String usage();

   String description();

   void execute(CommandContext var1, String var2) throws CommandException;
}