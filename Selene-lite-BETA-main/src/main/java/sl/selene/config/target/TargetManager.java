package sl.selene.config.target;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.Selene;

@Environment(EnvType.CLIENT)
public class TargetManager {
   public static MinecraftClient mc = MinecraftClient.getInstance();
   public static final List<Target> targets = new ArrayList<>();
   public static final File file = new File(new File(Selene.get.root, "configs"), "target.cfg");

   public static void init() {
      try {
         if (!file.exists()) {
            file.createNewFile();
         } else {
            readTargets();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public void add(String name) {
      if (name == null || name.isBlank()) {
         return;
      }

      String trimmed = name.trim();
      if (this.isTarget(trimmed)) {
         return;
      }

      targets.add(new Target(trimmed));
      this.updateFile();
   }

   public boolean isTarget(String name) {
      if (name == null || name.isBlank()) {
         return false;
      }

      return targets.stream().anyMatch(target -> target.getName().equalsIgnoreCase(name.trim()));
   }

   public void remove(String name) {
      targets.removeIf(target -> target.getName().equalsIgnoreCase(name));
      this.updateFile();
   }

   public void clearTargets() {
      targets.clear();
      this.updateFile();
   }

   public static List<Target> getTargets() {
      return targets;
   }

   public void updateFile() {
      try {
         StringBuilder builder = new StringBuilder();
         targets.forEach(target -> builder.append(target.getName()).append("\n"));
         Files.write(file.toPath(), builder.toString().getBytes());
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static void readTargets() {
      try {
         BufferedReader reader = new BufferedReader(
               new InputStreamReader(new DataInputStream(new FileInputStream(file.getAbsolutePath()))));

         String line;
         while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
               targets.add(new Target(line.trim()));
            }
         }

         reader.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}