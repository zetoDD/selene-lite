package sl.selene.cfg;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.io.FilenameUtils;
import sl.selene.Selene;
import sl.selene.module.api.Module;

@Environment(EnvType.CLIENT)
public final class ConfigManager extends Manager<Config> {

   public static final String AUTO_SAVE_CONFIG_NAME = "latest";
   public static final File configDirectory = getConfigDirectory();
   public static final String[] AUTO_SAVE_ALIASES = new String[] { "latest", "last", "lastest" };
   private static final long AUTO_SAVE_DEBOUNCE_MS = 500L;
   private static final ArrayList<Config> loadedConfigs = new ArrayList<>();
   private static String currentConfigName = null;
   private static final ScheduledExecutorService AUTO_SAVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread thread = new Thread(r, "selene-config-autosave");
      thread.setDaemon(true);
      return thread;
   });
   private volatile ScheduledFuture<?> pendingAutoSave;

   private static File getConfigDirectory() {
      File root;
      if (Selene.get != null && Selene.get.root != null) {
         root = Selene.get.root;
      } else {
         File preRoot = new File(System.getProperty("user.home"), ".selene");
         root = new File(preRoot, "Selene");
      }

      return new File(new File(root, "configs"), "cfg");
   }
   public static boolean isValidConfigName(String name) {
      if (name == null || name.isEmpty() || name.length() > 32) {
         return false;
      }

      for (int i = 0; i < name.length(); i++) {
         char c = name.charAt(i);
         if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == ' ' || c == '-' || c == '_') {
            continue;
         }
         return false;
      }

      return true;
   }

   public ConfigManager() {
      loadedConfigs.clear();
      this.setContents(loadConfigs());
      configDirectory.mkdirs();
   }

   private static ArrayList<Config> loadConfigs() {
      loadedConfigs.clear();
      File[] files = configDirectory.listFiles();
      if (files != null) {
         for (File file : files) {
            if (FilenameUtils.getExtension(file.getName()).equals("json")) {
               String cfgName = FilenameUtils.removeExtension(file.getName());
               boolean exists = loadedConfigs.stream().anyMatch(c -> c.getName().equalsIgnoreCase(cfgName));
               if (!exists) {
                  loadedConfigs.add(new Config(cfgName));
               }
            }
         }
      }

      return loadedConfigs;
   }

   public static ArrayList<Config> getLoadedConfigs() {
      return loadedConfigs;
   }

   public static String getCurrentConfigName() {
      return currentConfigName;
   }

   public static void setCurrentConfigName(String name) {
      currentConfigName = name;
   }

   public void load() {
      if (!configDirectory.exists()) {
         configDirectory.mkdirs();
      }

      if (configDirectory != null) {
         loadedConfigs.clear();
         File[] files = configDirectory.listFiles(fx -> !fx.isDirectory() && FilenameUtils.getExtension(fx.getName()).equals("json"));

         if (files != null) {
            for (File f : files) {
               String cfgName = FilenameUtils.removeExtension(f.getName());
               boolean exists = loadedConfigs.stream().anyMatch(c -> c.getName().equalsIgnoreCase(cfgName));
               if (!exists) {
                  loadedConfigs.add(new Config(cfgName));
               }
            }
         }

         this.setContents(new ArrayList<>(loadedConfigs));
      }
   }

   public boolean loadConfig(String configName) {
      if (!isValidConfigName(configName)) {
         return false;
      } else {
         Config config = this.findConfig(configName);
         if (config == null) {
            return false;
         } else {
            try {
               Module.beginConfigLoad();
               try (FileReader reader = new FileReader(config.getFile())) {
                  JsonParser parser = new JsonParser();
                  JsonObject object = (JsonObject)parser.parse(reader);
                  config.load(object);
                  currentConfigName = configName;
                  return true;
               } finally {
                  Module.endConfigLoad();
               }
            } catch (IOException | RuntimeException var9) {
               currentConfigName = null;
               return false;
            }
         }
      }
   }

   public boolean saveConfig(String configName) {
      return this.saveConfig(configName, true);
   }

   private boolean saveConfig(String configName, boolean refreshIndex) {
      if (!isValidConfigName(configName)) {
         return false;
      } else {
         Config config;
         if ((config = this.findConfig(configName)) == null) {
            Config newConfig = config = new Config(configName);
            this.getContents().add(newConfig);
            loadedConfigs.add(newConfig);
         }

         String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(config.save());

         try {
            boolean var5;
            try (FileWriter writer = new FileWriter(config.getFile())) {
               writer.write(contentPrettyPrint);
               if (refreshIndex) {
                  this.load();
               }
               var5 = true;
            }

            return var5;
         } catch (IOException var9) {
            return false;
         }
      }
   }

   public Config findConfig(String configName) {
      if (!isValidConfigName(configName)) {
         return null;
      } else {
         for (Config config : this.getContents()) {
            if (config.getName().equalsIgnoreCase(configName)) {
               return config;
            }
         }

         if (new File(configDirectory, configName + ".json").exists()) {
            Config config = new Config(configName);
            this.getContents().add(config);
            if (loadedConfigs.stream().noneMatch(c -> c.getName().equalsIgnoreCase(configName))) {
               loadedConfigs.add(config);
            }
            return config;
         }

         return null;
      }
   }

   public boolean deleteConfig(String configName) {
      if (!isValidConfigName(configName)) {
         return false;
      } else {
         Config config;
         if ((config = this.findConfig(configName)) == null) {
            return false;
         } else {
            File f = config.getFile();
            this.getContents().remove(config);
            loadedConfigs.removeIf(c -> c.getName().equalsIgnoreCase(configName));
            boolean deleted = f.exists() && f.delete();
            this.load();
            return deleted;
         }
      }
   }

   public void autoSave() {
      if (Selene.get == null || Selene.get.configManager != this) {
         return;
      }

      ScheduledFuture<?> previous = this.pendingAutoSave;
      if (previous != null) {
         previous.cancel(false);
      }

      this.pendingAutoSave = AUTO_SAVE_EXECUTOR.schedule(this::performAutoSave, AUTO_SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
   }

   private void performAutoSave() {
      if (!this.saveConfig(AUTO_SAVE_CONFIG_NAME, false)) {
         return;
      }

      Config primary = this.findConfig(AUTO_SAVE_CONFIG_NAME);
      if (primary == null) {
         return;
      }

      File source = primary.getFile();
      for (String alias : AUTO_SAVE_ALIASES) {
         if (alias.equalsIgnoreCase(AUTO_SAVE_CONFIG_NAME)) {
            continue;
         }

         try {
            File target = new File(configDirectory, alias + ".json");
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (this.findConfig(alias) == null) {
               Config aliasConfig = new Config(alias);
               this.getContents().add(aliasConfig);
               loadedConfigs.add(aliasConfig);
            }
         } catch (IOException ignored) {
         }
      }
   }

   public static void shutdown() {
      AUTO_SAVE_EXECUTOR.shutdown();
      try {
         if (!AUTO_SAVE_EXECUTOR.awaitTermination(2L, TimeUnit.SECONDS)) {
            AUTO_SAVE_EXECUTOR.shutdownNow();
         }
      } catch (InterruptedException e) {
         AUTO_SAVE_EXECUTOR.shutdownNow();
         Thread.currentThread().interrupt();
      }
   }
}