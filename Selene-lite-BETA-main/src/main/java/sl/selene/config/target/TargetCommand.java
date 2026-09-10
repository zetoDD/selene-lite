package sl.selene.config.target;

import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import sl.selene.Selene;
import sl.selene.commands.Command;
import sl.selene.commands.CommandContext;
import sl.selene.commands.CommandException;

@Environment(EnvType.CLIENT)
public final class TargetCommand implements Command {
   private static final TargetCommand INSTANCE = new TargetCommand();
   private static final List<String> COMMAND_ALIASES = List.of(".target", ".tgt");
   private static final String USAGE = ".target add <name> | .target remove <name> | .target list";

   private TargetCommand() {
   }

   public static TargetCommand getInstance() {
      return INSTANCE;
   }

   public List<String> getCommandAliases() {
      return COMMAND_ALIASES;
   }

   @Override
   public String name() {
      return "target";
   }

   @Override
   public List<String> aliases() {
      return COMMAND_ALIASES;
   }

   @Override
   public String usage() {
      return USAGE;
   }

   @Override
   public String description() {
      return "Manage your target list";
   }

   @Override
   public void execute(CommandContext context, String arguments) throws CommandException {
      if (Selene.get == null || Selene.get.targetManager == null) {
         throw new CommandException("Target system is not loaded yet");
      }

      if (arguments == null || arguments.isBlank()) {
         context.sendInfo(USAGE);
         return;
      }

      String[] parts = arguments.split("\\s+", 2);
      String subCommand = parts[0].toLowerCase(Locale.ROOT);
      String remainder = parts.length > 1 ? parts[1].trim() : "";

      switch (subCommand) {
         case "add":
         case "+":
            handleAdd(context, remainder);
            break;
         case "remove":
         case "rem":
         case "del":
         case "delete":
         case "-":
            handleRemove(context, remainder);
            break;
         case "list":
         case "ls":
            handleList(context);
            break;
         default:
            throw new CommandException("Unknown subcommand. Use: add, remove, list");
      }
   }

   private void handleAdd(CommandContext context, String name) throws CommandException {
      if (name == null || name.isBlank()) {
         throw new CommandException("Provide a name: .target add <name>");
      }

      TargetManager manager = Selene.get.targetManager;
      if (manager.isTarget(name)) {
         context.sendInfo("'" + name + "' is already in the target list");
         return;
      }

      manager.add(name);
      context.sendSuccess("Added target: " + name);
   }

   private void handleRemove(CommandContext context, String name) throws CommandException {
      if (name == null || name.isBlank()) {
         throw new CommandException("Provide a name: .target remove <name>");
      }

      TargetManager manager = Selene.get.targetManager;
      if (!manager.isTarget(name)) {
         throw new CommandException("'" + name + "' is not in the target list");
      }

      manager.remove(name);
      context.sendSuccess("Removed from targets: " + name);
   }

   private void handleList(CommandContext context) {
      List<Target> targetList = TargetManager.getTargets();
      if (targetList.isEmpty()) {
         context.sendInfo("Target list is empty");
         return;
      }

      MutableText builder = Text.literal("Targets: ");
      for (int i = 0; i < targetList.size(); i++) {
         String targetName = targetList.get(i).getName();
         MutableText nameText = Text.literal(targetName);
         nameText.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF5555)));
         builder.append(nameText);
         if (i < targetList.size() - 1) {
            builder.append(Text.literal(" | "));
         }
      }

      context.sendInfo(builder);
   }
}