package sl.selene.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.Selene;
import sl.selene.module.api.Category;
import sl.selene.ui.gui.GuiScreen;

@Environment(EnvType.CLIENT)
public class GuiManager {
   public static MinecraftClient mc = MinecraftClient.getInstance();
   private File file;
   private Category currentCategory = Category.Visuals;

   public void init() {
      this.file = new File(new File(Selene.get.root, "configs"), "gui.cfg");

      try {
         if (!this.file.getParentFile().exists()) {
            this.file.getParentFile().mkdirs();
         }

         if (!this.file.exists()) {
            this.file.createNewFile();
            this.saveSettings();
         } else {
            this.readSettings();
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   public void setGuiCategory(Category category) {
      this.currentCategory = category;
      this.saveSettings();
   }

   public Category getCurrentCategory() {
      return this.currentCategory;
   }

   public void saveSettings() {
      try (FileWriter writer = new FileWriter(this.file)) {
         Properties props = new Properties();
         props.setProperty("category", this.currentCategory.name());
         props.setProperty("frostedGlass", Boolean.toString(GuiScreen.frostedGlass));
         props.setProperty("plainArrayList", Boolean.toString(GuiScreen.plainArrayList));
         props.store(writer, "GUI Settings");
      } catch (IOException var6) {
         var6.printStackTrace();
      }
   }

   private void readSettings() {
      try (FileReader reader = new FileReader(this.file)) {
         Properties props = new Properties();
         props.load(reader);
         String savedCategory = props.getProperty("category", Category.Visuals.name());
         if ("FunTime".equals(savedCategory)) {
            savedCategory = Category.Misc.name();
         }
         if ("Prime".equals(savedCategory)) {
            savedCategory = Category.Visuals.name();
         }
         this.currentCategory = Category.valueOf(savedCategory);
         GuiScreen.frostedGlass = Boolean.parseBoolean(props.getProperty("frostedGlass", "false"));
         GuiScreen.plainArrayList = Boolean.parseBoolean(props.getProperty("plainArrayList", "true"));
      } catch (IllegalArgumentException | IOException var6) {
         var6.printStackTrace();
      }
   }
}