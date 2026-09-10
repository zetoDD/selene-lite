package sl.selene.config.friend;

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
import sl.selene.ui.Colors;

@Environment(EnvType.CLIENT)
public final class FriendCommand implements Command {
   private static final FriendCommand INSTANCE = new FriendCommand();
   private static final List<String> COMMAND_ALIASES = List.of(".friend", ".fr", ".fried");
   private static final String USAGE = ".friend add <name> | .friend remove <name> | .friend list";

   private FriendCommand() {
   }

   public static FriendCommand getInstance() {
      return INSTANCE;
   }

   public List<String> getCommandAliases() {
      return COMMAND_ALIASES;
   }

   @Override
   public String name() {
      return "friend";
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
      return "Manage your friend list";
   }

   @Override
   public void execute(CommandContext context, String arguments) throws CommandException {
      if (Selene.get == null || Selene.get.friendManager == null) {
         throw new CommandException("Friend system is not loaded yet");
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
         throw new CommandException("Provide a name: .friend add <name>");
      }

      FriendManager manager = Selene.get.friendManager;
      if (manager.isFriend(name)) {
         context.sendInfo("'" + name + "' is already in the friend list");
         return;
      }

      manager.add(name);
      context.sendSuccess("Added friend: " + name);
   }

   private void handleRemove(CommandContext context, String name) throws CommandException {
      if (name == null || name.isBlank()) {
         throw new CommandException("Provide a name: .friend remove <name>");
      }

      FriendManager manager = Selene.get.friendManager;
      if (!manager.isFriend(name)) {
         throw new CommandException("'" + name + "' is not in the friend list");
      }

      manager.remove(name);
      context.sendSuccess("Removed from friends: " + name);
   }

   private void handleList(CommandContext context) {
      List<Friend> friends = FriendManager.getFriends();
      if (friends.isEmpty()) {
         context.sendInfo("Friend list is empty");
         return;
      }

      MutableText builder = Text.literal("Friends: ");
      for (int i = 0; i < friends.size(); i++) {
         String friendName = friends.get(i).getName();
         MutableText nameText = Text.literal(friendName);
         nameText.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(Colors.getClientPrimary())));
         builder.append(nameText);
         if (i < friends.size() - 1) {
            builder.append(Text.literal(" | "));
         }
      }

      context.sendInfo(builder);
   }
}