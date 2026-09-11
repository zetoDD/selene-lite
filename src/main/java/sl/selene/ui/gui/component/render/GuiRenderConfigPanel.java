package sl.selene.ui.gui.component.render;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.Selene;
import sl.selene.cfg.Config;
import sl.selene.cfg.ConfigManager;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.motion.Motion;
import sl.selene.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public final class GuiRenderConfigPanel extends GuiScreen {
   private static final float CARD_TOP = 52.0F;
   private static final float CARD_HEIGHT = 184.0F;
   private static final float CARD_PADDING = 15.0F;
   private static final float FIELD_TOP = 56.0F;
   private static final float FIELD_HEIGHT = 22.0F;
   private static final float BUTTON_TOP = 86.0F;
   private static final float BUTTON_HEIGHT = 22.0F;
   private static final float BUTTON_GAP = 6.0F;
   private static final int BUTTON_COUNT = 5;
   private static final float INFO_TOP = 122.0F;
   private static final float INFO_STEP = 16.0F;
   private static final float INFO_LABEL_WIDTH = 74.0F;
   private static final float NAV_TOP = 82.0F;
   private static final float NAV_STEP = 22.0F;
   private static final float NAV_ROW_HEIGHT = 20.0F;
   private static final long STATUS_MS = 2600L;
   private static final long DELETE_CONFIRM_MS = 2500L;
   private static final int DANGER_TEXT = Renderer2D.ColorUtil.rgba(255, 110, 100, 255);
   private static final int DANGER_FILL = Renderer2D.ColorUtil.rgba(255, 90, 80, 30);
   private static final String[] BUTTON_LABELS = { "Save", "Load", "Delete", "Folder", "Reload" };

   private GuiRenderConfigPanel() {
   }

   public static void setStatus(String text) {
      GuiScreen.configStatusText = text;
      GuiScreen.configStatusUntil = System.currentTimeMillis() + STATUS_MS;
   }

   private static float cardX() {
      return GuiScreen.x + GuiScreen.SIDEBAR_WIDTH + 10.0F;
   }

   private static float cardWidth() {
      return GuiScreen.width - GuiScreen.SIDEBAR_WIDTH - 20.0F;
   }

   private static float buttonWidth() {
      return (cardWidth() - CARD_PADDING * 2.0F - BUTTON_GAP * (BUTTON_COUNT - 1)) / BUTTON_COUNT;
   }

   private static float buttonX(int index) {
      return cardX() + CARD_PADDING + index * (buttonWidth() + BUTTON_GAP);
   }

   private static float buttonY() {
      return GuiScreen.y + CARD_TOP + BUTTON_TOP;
   }

   private static int visibleRows(int total) {
      return Math.min(total, GuiScreen.configNavHover.length);
   }

   private static List<Config> sortedConfigs() {
      List<Config> configs = new ArrayList<>(ConfigManager.getLoadedConfigs());
      configs.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
      return configs;
   }

   private static boolean isCurrent(Config config) {
      String current = ConfigManager.getCurrentConfigName();
      return current != null && current.equalsIgnoreCase(config.getName());
   }

   public static void render(Renderer2D renderer, MatrixStack pose, int mouseX, int mouseY, float alpha) {
      if (alpha <= 0.001F) {
         return;
      }

      if (GuiScreen.configDeleteArmed && System.currentTimeMillis() >= GuiScreen.configDeleteArmedUntil) {
         GuiScreen.configDeleteArmed = false;
      }

      float x = cardX();
      float y = GuiScreen.y + CARD_TOP;
      float width = cardWidth();
      GlassStyle.card(renderer, x, y, width, CARD_HEIGHT, alpha);

      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * alpha));
      int mutedColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(255.0F * alpha));
      int accentColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * alpha));
      renderer.text(FontRegistry.INTER_SEMIBOLD, x + CARD_PADDING, y + 22.0F, 19.0F, "Configs", textColor);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING, y + 38.0F, 12.0F,
            "Save, load and manage client configs", mutedColor);

      renderField(renderer, x, y, width, mouseX, mouseY, alpha, textColor, mutedColor, accentColor);
      renderButtons(renderer, mouseX, mouseY, alpha, textColor);

      List<Config> configs = sortedConfigs();
      String active = ConfigManager.getCurrentConfigName();
      renderInfoRow(renderer, x, y, 0, "Active", active == null ? "None" : active, mutedColor, textColor);
      renderInfoRow(renderer, x, y, 1, "Autosave", ConfigManager.AUTO_SAVE_CONFIG_NAME + ".json", mutedColor, textColor);
      renderInfoRow(renderer, x, y, 2, "Storage", truncateStart(ConfigManager.configDirectory.getAbsolutePath(),
            width - CARD_PADDING * 2.0F - INFO_LABEL_WIDTH, renderer, 11.0F), mutedColor, mutedColor);

      String listSummary = configs.isEmpty() ? "No saved configs" : configs.size() + " saved configs";
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING, y + CARD_HEIGHT - 12.0F, 11.0F, listSummary, mutedColor);
      if (System.currentTimeMillis() < GuiScreen.configStatusUntil && !GuiScreen.configStatusText.isEmpty()) {
         float statusWidth = renderer.measureText(FontRegistry.INTER_MEDIUM, GuiScreen.configStatusText, 11.0F).width;
         renderer.text(FontRegistry.INTER_MEDIUM, x + width - CARD_PADDING - statusWidth, y + CARD_HEIGHT - 12.0F,
               11.0F, GuiScreen.configStatusText, accentColor);
      }
   }

   private static void renderField(Renderer2D renderer, float x, float y, float width, int mouseX, int mouseY,
         float alpha, int textColor, int mutedColor, int accentColor) {
      float fieldX = x + CARD_PADDING;
      float fieldY = y + FIELD_TOP;
      float fieldWidth = width - CARD_PADDING * 2.0F;
      GlassStyle.panel(renderer, fieldX, fieldY, fieldWidth, FIELD_HEIGHT, 5.0F, 5.0F, 5.0F, 5.0F, alpha * 0.82F);
      boolean hovered = GuiRenderMain.isHovered(mouseX, mouseY, fieldX, fieldY, fieldWidth, FIELD_HEIGHT);
      if (hovered || GuiScreen.configInputActive) {
         renderer.rect(fieldX, fieldY, fieldWidth, FIELD_HEIGHT, 5.0F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, (int)((GuiScreen.configInputActive ? 12.0F : 8.0F) * alpha)));
      }
      boolean hasText = !GuiScreen.configInputText.isEmpty();
      String display = GuiScreen.configInputActive || hasText ? GuiScreen.configInputText : "Config name";
      int displayColor = GuiScreen.configInputActive || hasText ? textColor : mutedColor;
      float baseline = fieldY + centeredBaseline(FIELD_HEIGHT, 13.0F);
      renderer.text(FontRegistry.INTER_MEDIUM, fieldX + 9.0F, baseline, 13.0F, display, displayColor);
      if (GuiScreen.configInputActive) {
         float cursorAlpha = Motion.blink(Motion.CURSOR_BLINK_HALF_PERIOD_MS);
         if (cursorAlpha > 0.01F) {
            float textWidth = renderer.measureText(FontRegistry.INTER_MEDIUM, GuiScreen.configInputText, 13.0F).width;
            renderer.rect(fieldX + 9.0F + textWidth, baseline - 5.0F, 1.0F, 10.0F, 0.5F, ColorUtil.multAlpha(accentColor, cursorAlpha));
         }
      }
   }

   private static void renderButtons(Renderer2D renderer, int mouseX, int mouseY, float alpha, int textColor) {
      boolean armed = GuiScreen.configDeleteArmed && System.currentTimeMillis() < GuiScreen.configDeleteArmedUntil;
      for (int i = 0; i < BUTTON_COUNT; i++) {
         float bx = buttonX(i);
         float by = buttonY();
         float bw = buttonWidth();
         boolean danger = i == 2;
         boolean hovered = GuiRenderMain.isHovered(mouseX, mouseY, bx, by, bw, BUTTON_HEIGHT);
         GlassStyle.panel(renderer, bx, by, bw, BUTTON_HEIGHT, 5.0F, 5.0F, 5.0F, 5.0F, alpha * 0.72F);
         if (danger && armed) {
            renderer.rect(bx, by, bw, BUTTON_HEIGHT, 5.0F, DANGER_FILL);
         } else if (hovered) {
            renderer.rect(bx, by, bw, BUTTON_HEIGHT, 5.0F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(12.0F * alpha)));
         }
         String label = danger && armed ? "Sure?" : BUTTON_LABELS[i];
         renderer.text(FontRegistry.INTER_MEDIUM, bx + bw * 0.5F, by + centeredBaseline(BUTTON_HEIGHT, 11.0F),
               11.0F, label, danger && armed ? DANGER_TEXT : textColor, "c");
      }
   }

   private static void renderInfoRow(Renderer2D renderer, float x, float y, int index, String label, String value,
         int labelColor, int valueColor) {
      float rowY = y + INFO_TOP + index * INFO_STEP;
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING, rowY, 11.0F, label, labelColor);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + INFO_LABEL_WIDTH, rowY, 11.0F, value, valueColor);
   }

   public static void renderSidebar(Renderer2D renderer, float alpha) {
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * alpha));
      int mutedColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(255.0F * alpha));
      int accentColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * alpha));
      List<Config> configs = sortedConfigs();
      float rowW = GuiScreen.SIDEBAR_WIDTH - GuiScreen.GAP - 14.0F;

      if (configs.isEmpty()) {
         renderer.text(FontRegistry.INTER_MEDIUM, GuiScreen.x + 12.0F, GuiScreen.y + NAV_TOP + 13.0F, 12.0F,
               "No configs saved", mutedColor);
         return;
      }

      int visible = visibleRows(configs.size());
      for (int i = 0; i < visible; i++) {
         Config config = configs.get(i);
         float rowX = GuiScreen.x + 7.0F;
         float rowY = GuiScreen.y + NAV_TOP + i * NAV_STEP;
         boolean current = isCurrent(config);
         float hover = GuiScreen.easeHover(GuiScreen.configNavHover[i],
               !current && GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, rowX, rowY, rowW, NAV_ROW_HEIGHT));
         GuiScreen.configNavHover[i] = hover;
         if (current) {
            renderer.rect(rowX, rowY + 1.0F, rowW, NAV_ROW_HEIGHT - 2.0F, 6.0F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(13.0F * alpha)));
         }
         if (hover > 0.01F) {
            renderer.rect(rowX, rowY + 1.0F, rowW, NAV_ROW_HEIGHT - 2.0F, 6.0F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(10.0F * hover * alpha)));
         }
         boolean autosave = config.isAutosave();
         String autoTag = autosave ? "auto" : "";
         float autoWidth = autosave ? renderer.measureText(FontRegistry.INTER_MEDIUM, autoTag, 9.0F).width + 4.0F : 0.0F;
         float nameWidth = rowW - 14.0F - autoWidth;
         renderer.text(FontRegistry.INTER_MEDIUM, rowX + 8.0F, rowY + 13.5F, 12.0F,
               truncate(config.getName(), nameWidth, renderer, 12.0F), current ? accentColor : textColor);
         if (autosave) {
            float tagWidth = renderer.measureText(FontRegistry.INTER_MEDIUM, autoTag, 9.0F).width;
            renderer.text(FontRegistry.INTER_MEDIUM, rowX + rowW - 6.0F - tagWidth, rowY + 13.0F, 9.0F, autoTag, mutedColor);
         }
      }
   }

   public static boolean mouseClicked(int mouseX, int mouseY, int button) {
      if (button != 0) {
         return false;
      }

      List<Config> configs = sortedConfigs();
      float rowW = GuiScreen.SIDEBAR_WIDTH - GuiScreen.GAP - 14.0F;
      int visible = visibleRows(configs.size());
      for (int i = 0; i < visible; i++) {
         float rowX = GuiScreen.x + 7.0F;
         float rowY = GuiScreen.y + NAV_TOP + i * NAV_STEP;
         if (GuiRenderMain.isHovered(mouseX, mouseY, rowX, rowY, rowW, NAV_ROW_HEIGHT)) {
            String name = configs.get(i).getName();
            GuiScreen.configInputText = name;
            GuiScreen.configInputActive = true;
            GuiScreen.configDeleteArmed = false;
            if (loadConfig(name)) {
               setStatus("Loaded '" + name + "'");
            }
            return true;
         }
      }

      if (!GuiRenderMain.isHovered(mouseX, mouseY, cardX(), GuiScreen.y + CARD_TOP, cardWidth(), CARD_HEIGHT)) {
         return false;
      }

      float fieldX = cardX() + CARD_PADDING;
      float fieldY = GuiScreen.y + CARD_TOP + FIELD_TOP;
      if (GuiRenderMain.isHovered(mouseX, mouseY, fieldX, fieldY, cardWidth() - CARD_PADDING * 2.0F, FIELD_HEIGHT)) {
         GuiScreen.configInputActive = true;
         GuiScreen.configDeleteArmed = false;
         return true;
      }

      for (int i = 0; i < BUTTON_COUNT; i++) {
         if (GuiRenderMain.isHovered(mouseX, mouseY, buttonX(i), buttonY(), buttonWidth(), BUTTON_HEIGHT)) {
            handleButton(i);
            return true;
         }
      }

      GuiScreen.configInputActive = false;
      GuiScreen.configDeleteArmed = false;
      return true;
   }

   private static void handleButton(int index) {
      GuiScreen.configInputActive = false;
      String name = GuiScreen.configInputText;
      boolean valid = ConfigManager.isValidConfigName(name);
      boolean exists = valid && ConfigManager.getLoadedConfigs().stream()
            .anyMatch(config -> config.getName().equalsIgnoreCase(name));
      boolean hasManager = Selene.get != null && Selene.get.configManager != null;

      switch (index) {
         case 0 -> {
            if (valid && hasManager) {
               Selene.get.configManager.saveConfig(name);
               setStatus("Saved '" + name + "'");
               GuiScreen.configInputText = "";
            } else {
               setStatus("Invalid name");
            }
            GuiScreen.configDeleteArmed = false;
         }
         case 1 -> {
            if (!valid) {
               setStatus("Invalid name");
            } else if (loadConfig(name)) {
               setStatus("Loaded '" + name + "'");
            } else {
               setStatus("Config '" + name + "' not found");
            }
            GuiScreen.configDeleteArmed = false;
         }
         case 2 -> {
            if (!valid) {
               setStatus("Invalid name");
               GuiScreen.configDeleteArmed = false;
            } else if (!exists) {
               setStatus("Config '" + name + "' not found");
               GuiScreen.configDeleteArmed = false;
            } else if (GuiScreen.configDeleteArmed && System.currentTimeMillis() < GuiScreen.configDeleteArmedUntil) {
               if (hasManager && Selene.get.configManager.deleteConfig(name)) {
                  setStatus("Deleted '" + name + "'");
                  GuiScreen.configInputText = "";
               } else {
                  setStatus("Delete failed");
               }
               GuiScreen.configDeleteArmed = false;
            } else {
               GuiScreen.configDeleteArmed = true;
               GuiScreen.configDeleteArmedUntil = System.currentTimeMillis() + DELETE_CONFIRM_MS;
               setStatus("Click again to delete '" + name + "'");
            }
         }
         case 3 -> {
            GuiScreen.configDeleteArmed = false;
            openConfigFolder();
         }
         case 4 -> {
            GuiScreen.configDeleteArmed = false;
            if (hasManager) {
               Selene.get.configManager.load();
               setStatus("Refreshed config list");
            } else {
               setStatus("Config manager unavailable");
            }
         }
         default -> {
         }
      }
   }

   private static boolean loadConfig(String name) {
      return Selene.get != null && Selene.get.configManager != null && Selene.get.configManager.loadConfig(name);
   }

   private static void openConfigFolder() {
      File folder = ConfigManager.configDirectory;
      boolean opened = false;
      try {
         if (!folder.exists()) {
            folder.mkdirs();
         }
         if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
               desktop.open(folder);
               opened = true;
            }
         }
      } catch (Exception ignored) {
      }
      if (!opened) {
         String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
         try {
            String path = folder.getAbsolutePath();
            if (os.contains("win")) {
               new ProcessBuilder("explorer.exe", path).start();
            } else if (os.contains("mac")) {
               new ProcessBuilder("open", path).start();
            } else {
               new ProcessBuilder("xdg-open", path).start();
            }
            opened = true;
         } catch (Exception ignored) {
         }
      }
      if (opened) {
         setStatus("Opened config folder");
      } else {
         setStatus(folder.getAbsolutePath());
      }
   }

   private static String truncate(String text, float maxWidth, Renderer2D renderer, float size) {
      if (maxWidth <= 0.0F) {
         return "";
      }

      String result = text;
      while (result.length() > 1 && renderer.measureText(FontRegistry.INTER_MEDIUM, result, size).width > maxWidth) {
         result = result.substring(0, result.length() - 1);
      }
      return result;
   }

   private static String truncateStart(String text, float maxWidth, Renderer2D renderer, float size) {
      if (maxWidth <= 0.0F) {
         return "";
      }

      String result = text;
      while (result.length() > 1 && renderer.measureText(FontRegistry.INTER_MEDIUM, result, size).width > maxWidth) {
         result = result.substring(1);
      }
      return result;
   }

   private static float centeredBaseline(float height, float textSize) {
      return Math.max(1.0F, (height - textSize) * 0.5F + textSize * 0.82F);
   }
}

