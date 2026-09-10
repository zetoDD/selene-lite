package sl.selene.ui.gui.component.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.Selene;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public final class GuiRenderSettings extends GuiScreen {
   private static final float NAV_TOP = 82.0F;
   private static final float NAV_STEP = 26.0F;
   private static final float NAV_ROW_HEIGHT = 23.0F;
   private static final float CARD_TOP = 52.0F;
   private static final float CARD_HEIGHT = 184.0F;
   private static final float CARD_PADDING = 15.0F;

   private static final float TOGGLE_Y = 97.0F;
   private static final float TOGGLE_H = 22.0F;
   private static final float TOGGLE_W = 34.0F;
   private static final float HUD_TEXT_Y = 57.0F;

   private static final float BLUR_ROW_Y = 57.0F;

   private GuiRenderSettings() {
   }

   public static void render(Renderer2D renderer, MatrixStack pose, int mouseX, int mouseY, float alpha) {
      if (alpha <= 0.001F) {
         return;
      }

      float x = cardX();
      float y = GuiScreen.y + CARD_TOP;
      float width = cardWidth();
      GlassStyle.card(renderer, x, y, width, CARD_HEIGHT, alpha);

      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * alpha));
      int mutedColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(255.0F * alpha));
      renderer.text(FontRegistry.INTER_SEMIBOLD, x + CARD_PADDING, y + 22.0F, 19.0F, GuiScreen.SETTINGS_CATEGORIES[GuiScreen.selectedSettingsCategory], textColor);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING, y + 38.0F, 12.0F,
         categoryDescription(GuiScreen.selectedSettingsCategory), mutedColor);

      switch (GuiScreen.selectedSettingsCategory) {
         case 0 -> renderGeneral(renderer, x, y, width, mouseX, mouseY, alpha);
         case 1 -> renderAppearance(renderer, x, y, width, alpha);
         case 2 -> renderHud(renderer, x, y, width, alpha);
         case 3 -> renderPerformance(renderer, x, y, width, alpha);
         default -> renderGeneral(renderer, x, y, width, mouseX, mouseY, alpha);
      }
   }

   public static void renderSidebar(Renderer2D renderer, float alpha) {
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * alpha));

      float rowW = GuiScreen.SIDEBAR_WIDTH - GuiScreen.GAP - 14.0F;
      for (int i = 0; i < GuiScreen.SETTINGS_CATEGORIES.length; i++) {
         float rowX = GuiScreen.x + 7.0F;
         float rowY = GuiScreen.y + NAV_TOP + i * NAV_STEP;
         boolean selected = i == GuiScreen.selectedSettingsCategory;
         float navHover = GuiScreen.easeHover(
               GuiScreen.settingsNavHover[i],
               GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, rowX, rowY, rowW, NAV_ROW_HEIGHT) && !selected
         );
         GuiScreen.settingsNavHover[i] = navHover;
         if (selected) {
            renderer.rect(rowX, rowY + 1.0F, rowW, NAV_ROW_HEIGHT - 2.0F, 6.0F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(13.0F * alpha)));
         }
         if (navHover > 0.01F) {
            renderer.rect(rowX, rowY + 1.0F, rowW, NAV_ROW_HEIGHT - 2.0F, 6.0F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(10.0F * navHover * alpha)));
         }
         drawCategoryIcon(renderer, i, rowX + 11.0F, rowY + NAV_ROW_HEIGHT * 0.5F, 7.5F, alpha * (selected ? 1.0F : 0.6F));
         int navTextColor = Renderer2D.ColorUtil.replAlpha(textColor, (int)(255.0F * alpha));
         renderer.text(FontRegistry.INTER_MEDIUM, rowX + 24.0F, rowY + 15.5F, 10.0F, GuiScreen.SETTINGS_CATEGORIES[i], navTextColor);
      }

      float backY = GuiScreen.y + 225.0F;
      boolean backHovered = isSettingsFooterHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY);
      renderer.rect(GuiScreen.x + 5.0F, GuiScreen.y + 214.0F, rowW + 4.0F, 24.0F, 6.5F,
            Renderer2D.ColorUtil.rgba(255, 255, 255, (int)((backHovered ? 12.0F : 5.0F) * alpha)));
      UiIcons.close(renderer, GuiScreen.x + 18.0F, backY, 9.0F, alpha);
      renderer.text(FontRegistry.INTER_MEDIUM, GuiScreen.x + 32.0F, backY + 4.0F, 13.0F, "Back", textColor);
   }

   public static boolean isSettingsFooterHovered(float mouseX, float mouseY) {
      return GuiRenderMain.isHovered(mouseX, mouseY, GuiScreen.x + 5.0F, GuiScreen.y + 210.0F, GuiScreen.SIDEBAR_WIDTH - GuiScreen.GAP - 10.0F, 36.0F);
   }

   public static boolean mouseClicked(Renderer2D renderer, int mouseX, int mouseY, int button) {
      if (button != 0) {
         return false;
      }

      if (isSettingsFooterHovered(mouseX, mouseY)) {
         GuiScreen.settingsPageOpen = false;
         GuiScreen.selectedSettingsCategory = 0;
         GuiScreen.configInputActive = false;
         return true;
      }

      if (GuiScreen.selectedSettingsCategory == 1) {
         float toggleX = cardX() + cardWidth() - CARD_PADDING - TOGGLE_W;
         float toggleY = GuiScreen.y + CARD_TOP + TOGGLE_Y;
         if (GuiRenderMain.isHovered(mouseX, mouseY, toggleX, toggleY, TOGGLE_W, TOGGLE_H)) {
            GuiScreen.frostedGlass = !GuiScreen.frostedGlass;
            saveGuiSettings();
            return true;
         }
      }

      if (GuiScreen.selectedSettingsCategory == 2) {
         float toggleX = cardX() + cardWidth() - CARD_PADDING - TOGGLE_W;
         float textToggleY = GuiScreen.y + CARD_TOP + HUD_TEXT_Y;
         if (GuiRenderMain.isHovered(mouseX, mouseY, toggleX, textToggleY, TOGGLE_W, TOGGLE_H)) {
            GuiScreen.plainArrayList = !GuiScreen.plainArrayList;
            saveGuiSettings();
            return true;
         }
      }

      for (int i = 0; i < GuiScreen.SETTINGS_CATEGORIES.length; i++) {
         float rowY = GuiScreen.y + NAV_TOP + i * NAV_STEP;
         if (GuiRenderMain.isHovered(mouseX, mouseY, GuiScreen.x + 7.0F, rowY, GuiScreen.SIDEBAR_WIDTH - GuiScreen.GAP - 14.0F, NAV_ROW_HEIGHT)) {
            GuiScreen.selectedSettingsCategory = i;
            GuiScreen.configInputActive = false;
            return true;
         }
      }

      return true;
   }

   private static void renderGeneral(Renderer2D renderer, float x, float y, float width, int mouseX, int mouseY, float alpha) {
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(235.0F * alpha));
      int muted = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(255.0F * alpha));

      UiIcons.info(renderer, x + CARD_PADDING + 8.0F, y + BLUR_ROW_Y, 10.5F, alpha);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, y + BLUR_ROW_Y + 4.0F, 13.0F, "Rendering", textColor);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, y + BLUR_ROW_Y + 17.0F, 11.0F,
         "Glass renders over a fixed backdrop blur with directional rim lighting.", muted);
   }

   private static void renderAppearance(Renderer2D renderer, float x, float y, float width, float alpha) {
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(235.0F * alpha));
      int muted = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(255.0F * alpha));
      float rowY = y + 57.0F;
      UiIcons.settings(renderer, x + CARD_PADDING + 8.0F, rowY, 10.5F, alpha);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, rowY + 4.0F, 13.0F, "Clear glass surface", textColor);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, rowY + 17.0F, 11.0F, "Translucent panels with a soft rim", muted);
      UiIcons.info(renderer, x + CARD_PADDING + 8.0F, rowY + 43.0F, 10.5F, alpha);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, rowY + 47.0F, 13.0F, "Icon atlas", textColor);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, rowY + 60.0F, 11.0F, "Bundled white glyphs", muted);

      UiIcons.sliders(renderer, x + CARD_PADDING + 8.0F, y + TOGGLE_Y + 3.0F, 10.5F, alpha);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, y + TOGGLE_Y + 1.0F, 13.0F, "Frosted Glass", textColor);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, y + TOGGLE_Y + 14.0F, 11.0F, "Bright frosted panels; off = smoked liquid glass", muted);
      drawToggle(renderer, x + width - CARD_PADDING - TOGGLE_W, y + TOGGLE_Y + 2.0F, alpha, GuiScreen.frostedGlass, GuiScreen.toggleFrostedAnim);
   }

   private static void renderHud(Renderer2D renderer, float x, float y, float width, float alpha) {
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(235.0F * alpha));
      int muted = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(255.0F * alpha));
      float textRowY = y + HUD_TEXT_Y;
      UiIcons.info(renderer, x + CARD_PADDING + 8.0F, textRowY + 3.0F, 10.5F, alpha);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, textRowY + 1.0F, 13.0F, "Plain Text Array List", textColor);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, textRowY + 14.0F, 11.0F, "Lightweight text-only module list", muted);
      drawToggle(renderer, x + width - CARD_PADDING - TOGGLE_W, textRowY + 2.0F, alpha, GuiScreen.plainArrayList, GuiScreen.toggleHudTextAnim);
   }

   private static void drawToggle(Renderer2D renderer, float x, float y, float alpha, boolean on, sl.selene.util.render.math.animation.anim.util.Animation2 anim) {
      anim.update();
      anim.run(on ? 1.0 : 0.0, 0.18F, sl.selene.util.render.math.animation.anim.util.Easings.SINE_OUT);
      GlassStyle.toggle(renderer, x, y, TOGGLE_W, 14.0F, alpha, anim.get());
   }

   private static void renderPerformance(Renderer2D renderer, float x, float y, float width, float alpha) {
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(235.0F * alpha));
      int muted = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(255.0F * alpha));
      UiIcons.speed(renderer, x + CARD_PADDING + 8.0F, y + 59.0F, 11.25F, alpha);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, y + 63.0F, 13.0F, "Render load", textColor);
      renderer.text(FontRegistry.INTER_MEDIUM, x + CARD_PADDING + 25.0F, y + 79.0F, 11.0F,
         "Use the Optimizer module to cut HUD render work.", muted);
   }
   private static void drawCategoryIcon(Renderer2D renderer, int index, float x, float y, float size, float alpha) {
      switch (index) {
         case 0 -> UiIcons.info(renderer, x, y, size, alpha);
         case 1 -> UiIcons.settings(renderer, x, y, size, alpha);
         case 2 -> UiIcons.settings(renderer, x, y, size, alpha);
         case 3 -> UiIcons.speed(renderer, x, y, size, alpha);
         default -> UiIcons.info(renderer, x, y, size, alpha);
      }
   }

   private static String categoryDescription(int index) {
      return switch (index) {
         case 0 -> "Core client and glass controls";
         case 1 -> "Visual language and surfaces";
         case 2 -> "HUD grouping and layout choices";
         case 3 -> "Rendering and frame-time guidance";
         default -> "Core client controls";
      };
   }

   private static float cardX() {
      return GuiScreen.x + GuiScreen.SIDEBAR_WIDTH + 10.0F;
   }

   private static float cardWidth() {
      return GuiScreen.width - GuiScreen.SIDEBAR_WIDTH - 20.0F;
   }

   private static void saveGuiSettings() {
      if (Selene.get != null && Selene.get.guiManager != null) {
         Selene.get.guiManager.saveSettings();
      }
   }
}