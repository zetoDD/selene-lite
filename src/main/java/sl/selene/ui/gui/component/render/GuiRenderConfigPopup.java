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
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.ScaledResolution;
import sl.selene.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public final class GuiRenderConfigPopup extends GuiScreen {

   private static final float POPUP_W = 260.0F;
   private static final float POPUP_H = 280.0F;

   private static final float PAD = 12.0F;
   private static final float FIELD_Y = 46.0F;
   private static final float FIELD_H = 18.0F;
   private static final float BUTTONS_Y = 72.0F;
   private static final float BUTTON_H = 18.0F;
   private static final float LIST_TOP = 108.0F;
   private static final float ROW_STEP = 20.0F;
   private static final float ROW_H = 16.0F;
   private static final float DRAG_BAR_H = 34.0F;
   private static final float ROW_CLIP = 34.0F;
   private static final long STATUS_MS = 2600L;
   private static final long DELETE_CONFIRM_MS = 2500L;
   private static final int DANGER_TEXT = Renderer2D.ColorUtil.rgba(255, 110, 100, 255);
   private static final int DANGER_FILL = Renderer2D.ColorUtil.rgba(255, 90, 80, 30);

   private GuiRenderConfigPopup() {
   }

   public static void setStatus(String text) {
      GuiScreen.configStatusText = text;
      GuiScreen.configStatusUntil = System.currentTimeMillis() + STATUS_MS;
   }

   private static float popupX() {
      float screenWidth = mc != null ? new ScaledResolution(mc).getWidth() : GuiScreen.width;
      float screenLeft = 8.0F;
      float screenRight = Math.max(screenLeft, screenWidth - POPUP_W - 8.0F);
      if (GuiScreen.configPopupX != 0.0F) {
         return Math.max(screenLeft, Math.min(GuiScreen.configPopupX, screenRight));
      }

      float preferredRight = GuiScreen.x + GuiScreen.width + 10.0F;
      float preferredLeft = GuiScreen.x - POPUP_W - 10.0F;
      if (preferredRight <= screenRight) {
         return preferredRight;
      }
      if (preferredLeft >= screenLeft) {
         return preferredLeft;
      }
      return screenRight;
   }

   private static float popupY() {
      float screenHeight = mc != null ? new ScaledResolution(mc).getHeight() : GuiScreen.height;
      float natural = GuiScreen.configPopupY != 0.0F
         ? GuiScreen.configPopupY
         : GuiScreen.y + GuiScreen.CONTENT_TOP;
      float topLimit = 8.0F;
      float bottomLimit = screenHeight - POPUP_H - 8.0F;
      return Math.max(topLimit, Math.min(natural, Math.max(topLimit, bottomLimit)));
   }

   private static List<Config> sortedConfigs() {
      List<Config> configs = new ArrayList<>(ConfigManager.getLoadedConfigs());
      configs.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
      return configs;
   }

   public static void renderPopup(Renderer2D renderer, MatrixStack pose, int mouseX, int mouseY, float alpha) {
      if (alpha <= 0.001F) {
         return;
      }
      if (GuiScreen.configDeleteArmed && System.currentTimeMillis() >= GuiScreen.configDeleteArmedUntil) {
         GuiScreen.configDeleteArmed = false;
      }
      float px = popupX();
      float py = popupY();

      GlassStyle.card(renderer, px, py, POPUP_W, POPUP_H, alpha);
      renderer.rect(px + 1.0F, py + 1.0F, POPUP_W - 2.0F, DRAG_BAR_H, 6.0F,
            Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(7.0F * alpha)));

      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * alpha));
      int muted = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(255.0F * alpha));
      int fieldColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * alpha));

      float titleSize = 16.0F;
      float titleWidth = renderer.measureText(FontRegistry.INTER_SEMIBOLD, "Config", titleSize).width;
      renderer.text(FontRegistry.INTER_SEMIBOLD, px + (POPUP_W - titleWidth) * 0.5F, py + 22.0F,
            titleSize, "Config", textColor);

      float fieldX = px + PAD;
      float fieldY = py + FIELD_Y;
      float fieldW = POPUP_W - PAD * 2.0F;
      GlassStyle.panel(renderer, fieldX, fieldY, fieldW, FIELD_H, 4.0F, 4.0F, 4.0F, 4.0F, alpha * 0.82F);
      boolean fieldHovered = GuiRenderMain.isHovered(mouseX, mouseY, fieldX, fieldY, fieldW, FIELD_H);
      if (fieldHovered) {
         renderer.rect(fieldX, fieldY, fieldW, FIELD_H, 4.0F, Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(10.0F * alpha)));
      }
      String display = GuiScreen.configInputActive || !GuiScreen.configInputText.isEmpty()
         ? GuiScreen.configInputText
         : "Config name";
      int inputColor = GuiScreen.configInputActive || !GuiScreen.configInputText.isEmpty() ? textColor : muted;
      renderer.text(FontRegistry.INTER_MEDIUM, fieldX + 8.0F, fieldY + centeredBaseline(FIELD_H, 13.0F), 13.0F, display, inputColor);
      if (GuiScreen.configInputActive) {
         long now = System.currentTimeMillis();
         if (now / 500L % 2L == 0L) {
            float textW = renderer.measureText(FontRegistry.INTER_MEDIUM, GuiScreen.configInputText, 13.0F).width;
            renderer.rect(fieldX + 8.0F + textW, fieldY + centeredBaseline(FIELD_H, 13.0F), 1.0F, 10.0F, 0.5F, fieldColor);
         }
      }

      float buttonGap = 6.0F;
      float buttonW = (POPUP_W - PAD * 2.0F - buttonGap * 3.0F) / 4.0F;
      renderConfigButton(renderer, "Save", px + PAD, py + BUTTONS_Y, buttonW, BUTTON_H, mouseX, mouseY, alpha, textColor, false);
      renderConfigButton(renderer, "Load", px + PAD + buttonW + buttonGap, py + BUTTONS_Y, buttonW, BUTTON_H, mouseX, mouseY, alpha, textColor, false);
      boolean deleteArmed = GuiScreen.configDeleteArmed
            && System.currentTimeMillis() < GuiScreen.configDeleteArmedUntil;
      renderConfigButton(renderer, deleteArmed ? "Confirm?" : "Delete", px + PAD + (buttonW + buttonGap) * 2.0F,
            py + BUTTONS_Y, buttonW, BUTTON_H, mouseX, mouseY, alpha, textColor, deleteArmed);
      renderConfigButton(renderer, "Folder", px + PAD + (buttonW + buttonGap) * 3.0F,
            py + BUTTONS_Y, buttonW, BUTTON_H, mouseX, mouseY, alpha, textColor, false);

      renderer.text(FontRegistry.INTER_MEDIUM, px + PAD, py + LIST_TOP - 4.0F, 12.0F, "Saved configs", muted);
      float listY = py + LIST_TOP + 6.0F;
      int index = 0;
      for (Config config : sortedConfigs()) {
         float rowY = listY + index * ROW_STEP;
         if (rowY + ROW_H > py + POPUP_H - ROW_CLIP) {
            break;
         }
         boolean hovered = GuiRenderMain.isHovered(mouseX, mouseY, px + PAD, rowY, POPUP_W - PAD * 2.0F, ROW_H);
         boolean isCurrent = config.getName().equalsIgnoreCase(ConfigManager.getCurrentConfigName());
         boolean isAutosave = config.isAutosave();
         String timeText = formatRelative(config.getLastModified());
         float tsW = timeText.isEmpty()
               ? 0.0F
               : renderer.measureText(FontRegistry.INTER_MEDIUM, timeText, 10.0F).width;
         float autoW = isAutosave
               ? renderer.measureText(FontRegistry.INTER_MEDIUM, "auto", 10.0F).width + 3.0F
               : 0.0F;
         float nameAvailable = POPUP_W - PAD * 2.0F - 8.0F - tsW - autoW;
         String configName = truncate(config.getName(), nameAvailable, renderer);

         GlassStyle.fill(renderer, px + PAD, rowY, POPUP_W - PAD * 2.0F, ROW_H, 4.0F, alpha * 0.55F);
         renderer.rect(px + PAD, rowY, POPUP_W - PAD * 2.0F, ROW_H, 4.0F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(8.0F * alpha)));
         if (isCurrent) {
            renderer.rect(px + PAD, rowY, POPUP_W - PAD * 2.0F, ROW_H, 4.0F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(16.0F * alpha)));
         }
         if (hovered) {
            renderer.rect(px + PAD, rowY, POPUP_W - PAD * 2.0F, ROW_H, 4.0F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(14.0F * alpha)));
         }
         renderer.text(FontRegistry.INTER_MEDIUM, px + PAD + 4.0F,
               rowY + centeredBaseline(ROW_H, 12.0F), 12.0F, configName,
               isCurrent ? fieldColor : textColor);
         if (isAutosave) {
            renderer.text(FontRegistry.INTER_MEDIUM,
                  px + PAD + 4.0F + renderer.measureText(FontRegistry.INTER_MEDIUM, configName, 12.0F).width + 2.0F,
                  rowY + centeredBaseline(ROW_H, 10.0F), 10.0F, "auto", muted);
         }
         if (!timeText.isEmpty()) {
            renderer.text(FontRegistry.INTER_MEDIUM,
                  px + PAD + POPUP_W - PAD * 2.0F - 4.0F - tsW,
                  rowY + centeredBaseline(ROW_H, 10.0F), 10.0F, timeText, muted);
         }
         index++;
      }

      if (System.currentTimeMillis() < GuiScreen.configStatusUntil && !GuiScreen.configStatusText.isEmpty()) {
         float statusW = renderer.measureText(FontRegistry.INTER_MEDIUM, GuiScreen.configStatusText, 11.0F).width;
         renderer.text(FontRegistry.INTER_MEDIUM, px + (POPUP_W - statusW) * 0.5F, py + POPUP_H - 16.0F,
               11.0F, GuiScreen.configStatusText, textColor);
      }
   }

   private static void renderConfigButton(Renderer2D renderer, String label, float bx, float by, float bw, float bh,
         int mouseX, int mouseY, float alpha, int textColor, boolean danger) {
      boolean hovered = GuiRenderMain.isHovered(mouseX, mouseY, bx, by, bw, bh);
      GlassStyle.panel(renderer, bx, by, bw, bh, 4.0F, 4.0F, 4.0F, 4.0F, alpha * 0.72F);
      if (danger) {
         renderer.rect(bx, by, bw, bh, 4.0F, DANGER_FILL);
      } else if (!hovered) {
         renderer.rect(bx, by, bw, bh, 4.0F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(5.0F * alpha)));
      }
      renderer.text(FontRegistry.INTER_MEDIUM, bx + bw * 0.5F, by + centeredBaseline(bh, 11.0F),
            11.0F, label, danger ? DANGER_TEXT : textColor, "c");
   }

   public static boolean beginDrag(int mouseX, int mouseY) {
      if (!isMouseOver(mouseX, mouseY)) {
         return false;
      }
      float px = popupX();
      float py = popupY();
      if (GuiRenderMain.isHovered(mouseX, mouseY, px, py, POPUP_W, DRAG_BAR_H)) {
         GuiScreen.configPopupDragging = true;
         GuiScreen.configPopupOffsetX = mouseX - px;
         GuiScreen.configPopupOffsetY = mouseY - py;
         GuiScreen.configInputActive = false;
         return true;
      }
      return false;
   }

   public static boolean drag(int mouseX, int mouseY) {
      if (!GuiScreen.configPopupDragging) {
         return false;
      }
      float screenWidth = mc != null ? new ScaledResolution(mc).getWidth() : GuiScreen.width;
      float screenHeight = mc != null ? new ScaledResolution(mc).getHeight() : GuiScreen.height;
      float minX = 8.0F;
      float maxX = Math.max(minX, screenWidth - POPUP_W - 8.0F);
      float minY = 8.0F;
      float maxY = Math.max(minY, screenHeight - POPUP_H - 8.0F);
      GuiScreen.configPopupX = Math.max(minX, Math.min(mouseX - GuiScreen.configPopupOffsetX, maxX));
      GuiScreen.configPopupY = Math.max(minY, Math.min(mouseY - GuiScreen.configPopupOffsetY, maxY));
      return true;
   }

   public static boolean mouseClicked(int mouseX, int mouseY, int button) {
      if (!isMouseOver(mouseX, mouseY)) {
         return false;
      }
      if (button != 0) {
         return true;
      }
      float px = popupX();
      float py = popupY();

      float fieldX = px + PAD;
      float fieldY = py + FIELD_Y;
      if (GuiRenderMain.isHovered(mouseX, mouseY, fieldX, fieldY, POPUP_W - PAD * 2.0F, FIELD_H)) {
         GuiScreen.configInputActive = true;
         GuiScreen.configDeleteArmed = false;
         return true;
      }
      if (GuiScreen.configInputActive) {
         GuiScreen.configInputActive = false;
      }

      String name = GuiScreen.configInputText;
      boolean valid = ConfigManager.isValidConfigName(name);
      boolean exists = valid && ConfigManager.getLoadedConfigs().stream()
            .anyMatch(c -> c.getName().equalsIgnoreCase(name));
      boolean hasManager = Selene.get != null && Selene.get.configManager != null;
      float buttonGap = 6.0F;
      float buttonW = (POPUP_W - PAD * 2.0F - buttonGap * 3.0F) / 4.0F;
      float buttonsY = py + BUTTONS_Y;
      if (GuiRenderMain.isHovered(mouseX, mouseY, px + PAD, buttonsY, buttonW, BUTTON_H)) {
         if (valid && hasManager) {
            Selene.get.configManager.saveConfig(name);
            setStatus("Saved '" + name + "'");
            GuiScreen.configInputText = "";
         } else {
            setStatus("Invalid name");
         }
         GuiScreen.configDeleteArmed = false;
         return true;
      }
      if (GuiRenderMain.isHovered(mouseX, mouseY, px + PAD + buttonW + buttonGap, buttonsY, buttonW, BUTTON_H)) {
         if (valid && hasManager) {
            if (Selene.get.configManager.loadConfig(name)) {
               setStatus("Loaded '" + name + "'");
            } else {
               setStatus("Config '" + name + "' not found");
            }
         } else {
            setStatus("Invalid name");
         }
         GuiScreen.configDeleteArmed = false;
         return true;
      }
      if (GuiRenderMain.isHovered(mouseX, mouseY, px + PAD + (buttonW + buttonGap) * 2.0F, buttonsY, buttonW, BUTTON_H)) {
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
         return true;
      }

      if (GuiRenderMain.isHovered(mouseX, mouseY, px + PAD + (buttonW + buttonGap) * 3.0F, buttonsY, buttonW, BUTTON_H)) {
         GuiScreen.configDeleteArmed = false;
         openConfigFolder();
         return true;
      }

      float listY = py + LIST_TOP + 6.0F;
      int index = 0;
      for (Config config : sortedConfigs()) {
         float rowY = listY + index * ROW_STEP;
         if (rowY + ROW_H > py + POPUP_H - ROW_CLIP) {
            break;
         }
         if (GuiRenderMain.isHovered(mouseX, mouseY, px + PAD, rowY, POPUP_W - PAD * 2.0F, ROW_H)) {
            GuiScreen.configInputText = config.getName();
            GuiScreen.configInputActive = true;
            GuiScreen.configDeleteArmed = false;
            if (hasManager && Selene.get.configManager.loadConfig(config.getName())) {
               setStatus("Loaded '" + config.getName() + "'");
            }
            return true;
         }
         index++;
      }

      GuiScreen.configDeleteArmed = false;
      return true;
   }

   private static float centeredBaseline(float height, float textSize) {
      return Math.max(1.0F, (height - textSize) * 0.5F + textSize * 0.82F);
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

   private static String truncate(String text, float maxWidth, Renderer2D renderer) {
      if (maxWidth <= 0.0F) {
         return "";
      }
      String result = text;
      while (result.length() > 1
            && renderer.measureText(FontRegistry.INTER_MEDIUM, result, 12.0F).width > maxWidth) {
         result = result.substring(0, result.length() - 1);
      }
      return result;
   }

   private static String formatRelative(long lastModified) {
      if (lastModified <= 0L) {
         return "";
      }
      long diffMs = System.currentTimeMillis() - lastModified;
      long sec = diffMs / 1000L;
      if (sec < 60L) {
         return "now";
      }
      long min = sec / 60L;
      if (min < 60L) {
         return min + "m";
      }
      long hr = min / 60L;
      if (hr < 24L) {
         return hr + "h";
      }
      return (hr / 24L) + "d";
   }

   public static boolean isMouseOver(int mouseX, int mouseY) {
      return GuiRenderMain.isHovered(mouseX, mouseY, popupX(), popupY(), POPUP_W, POPUP_H);
   }
}