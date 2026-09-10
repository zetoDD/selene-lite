package sl.selene.config;

import java.awt.Desktop;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import sl.selene.Selene;
import sl.selene.cfg.Config;
import sl.selene.cfg.ConfigManager;
import sl.selene.commands.Command;
import sl.selene.commands.CommandContext;
import sl.selene.commands.CommandException;
import sl.selene.module.impl.visuals.Hud;
import sl.selene.ui.Colors;

@Environment(EnvType.CLIENT)
public final class ConfigCommand implements Command {
   private static final ConfigCommand INSTANCE = new ConfigCommand();
   private static final List<String> COMMAND_ALIASES = List.of(".cfg", ".c", ".config", ".cgf", ".fig");
   private static final Map<String, ConfigCommand.CommandMetadata> COMMAND_DEFINITIONS;
   private static final List<String> SUB_COMMANDS;

   private ConfigCommand() {
   }

   public static ConfigCommand getInstance() {
      return INSTANCE;
   }

   public List<String> getCommandAliases() {
      return COMMAND_ALIASES;
   }

   public List<String> getSubCommands() {
      return SUB_COMMANDS;
   }

   public Map<String, ConfigCommand.CommandMetadata> getCommandMetadata() {
      return COMMAND_DEFINITIONS;
   }

   @Override
   public String name() {
      return "config";
   }

   @Override
   public List<String> aliases() {
      return COMMAND_ALIASES;
   }

   @Override
   public String usage() {
      return COMMAND_ALIASES.get(0) + " <save/load/list/delete/dir>";
   }

   @Override
   public String description() {
      return "Manage client configuration profiles";
   }

   @Override
   public void execute(CommandContext context, String arguments) throws CommandException {
      if (Selene.get.configManager == null) {
         throw new CommandException("Configuration system is not ready yet");
      } else if (arguments != null && !arguments.isBlank()) {
         String[] parts = arguments.split("\\s+", 2);
         String subCommand = parts[0].toLowerCase(Locale.ROOT);
         String remainder = parts.length > 1 ? parts[1].trim() : "";
         switch (subCommand) {
            case "save":
               this.handleSave(context, remainder);
               break;
            case "load":
               this.handleLoad(context, remainder);
               break;
            case "list":
               this.handleList(context);
               break;
            case "delete":
               this.handleDelete(context, remainder);
               break;
            case "dir":
               this.handleOpenDir(context);
               break;
            default:
               throw new CommandException("Unknown command. Use save/load/list/delete/dir");
         }
      } else {
         context.sendInfo(
            "Usage: "
               + COMMAND_DEFINITIONS.values().stream().map(metadata -> COMMAND_ALIASES.get(0) + " " + metadata.usage()).collect(Collectors.joining(", "))
         );
      }
   }

   private void handleSave(CommandContext context, String name) throws CommandException {
      if (name != null && !name.isBlank()) {
         ConfigManager configManager = Selene.get.configManager;
         if (configManager.saveConfig(name)) {
            Selene.get.manager.get(Hud.class).showNotification("Saved config " + name, Hud.NotificationType.CONFIG, 6000L);
            context.sendSuccess("Config '" + name + "' saved");
         } else {
            throw new CommandException("Failed to save config '" + name + "'");
         }
      } else {
         throw new CommandException("Specify config name");
      }
   }

   private void handleLoad(CommandContext context, String name) throws CommandException {
      if (name != null && !name.isBlank()) {
         ConfigManager configManager = Selene.get.configManager;
         if (configManager.loadConfig(name)) {
            Selene.get.manager.get(Hud.class).showNotification("Loaded config " + name, Hud.NotificationType.CONFIG, 6000L);
            context.sendSuccess("Config '" + name + "' loaded");
         } else {
            throw new CommandException("Config '" + name + "' not found or failed to load");
         }
      } else {
         throw new CommandException("Specify config name to load");
      }
   }

   private void handleList(CommandContext context) {
      ConfigManager configManager = Selene.get.configManager;
      List<Config> configs = configManager.getContents();
      if (configs.isEmpty()) {
         context.sendInfo("No available configs");
      } else {
         MutableText builder = Text.literal("Available configs: ");

         for (int i = 0; i < configs.size(); i++) {
            String name = configs.get(i).getName();
            MutableText nameText = Text.literal(name);
            nameText = nameText.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(Colors.getClientPrimary())));
            builder = builder.append(nameText);
            if (i < configs.size() - 1) {
               builder = builder.append(Text.literal(" | "));
            }
         }

         context.sendInfo(builder);
      }
   }

   private void handleDelete(CommandContext context, String name) throws CommandException {
      if (name != null && !name.isBlank()) {
         ConfigManager configManager = Selene.get.configManager;
         if (configManager.deleteConfig(name)) {
            Selene.get.manager.get(Hud.class).showNotification("Deleted config " + name, Hud.NotificationType.CONFIG, 6000L);
            context.sendSuccess("Config '" + name + "' deleted");
         } else {
            throw new CommandException("Config '" + name + "' not found or failed to delete");
         }
      } else {
         throw new CommandException("Specify config name to delete");
      }
   }

   private void handleOpenDir(CommandContext context) {
      String path = ConfigManager.configDirectory.getAbsolutePath();
      boolean opened = false;

      try {
         if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
               desktop.open(ConfigManager.configDirectory);
               opened = true;
            }
         }
      } catch (Exception ignored) {
      }

      if (opened) {
         context.sendSuccess("Opened config folder: " + path);
      } else {
         context.sendInfo("Config folder: " + path);
      }
   }

   static {
      Map<String, ConfigCommand.CommandMetadata> commands = new LinkedHashMap<>();
      commands.put("save", new ConfigCommand.CommandMetadata("save <name>", ConfigCommand.ArgumentType.NEW_CONFIG_NAME, "Save current config"));
      commands.put(
         "load", new ConfigCommand.CommandMetadata("load <name>", ConfigCommand.ArgumentType.EXISTING_CONFIG_NAME, "Load a config and apply its settings")
      );
      commands.put("list", new ConfigCommand.CommandMetadata("list", ConfigCommand.ArgumentType.NONE, "Show all available configs"));
      commands.put("delete", new ConfigCommand.CommandMetadata("delete <name>", ConfigCommand.ArgumentType.EXISTING_CONFIG_NAME, "Delete a saved config"));
      commands.put("dir", new ConfigCommand.CommandMetadata("dir", ConfigCommand.ArgumentType.NONE, "Open config folder"));
      COMMAND_DEFINITIONS = Collections.unmodifiableMap(commands);
      SUB_COMMANDS = List.copyOf(COMMAND_DEFINITIONS.keySet());
   }

   @Environment(EnvType.CLIENT)
   public static enum ArgumentType {
      NONE,
      NEW_CONFIG_NAME,
      EXISTING_CONFIG_NAME;
   }

   @Environment(EnvType.CLIENT)
   public record CommandMetadata(String usage, ConfigCommand.ArgumentType argumentType, String description) {
   }
}