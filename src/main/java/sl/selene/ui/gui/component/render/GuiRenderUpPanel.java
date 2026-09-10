package sl.selene.ui.gui.component.render;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.map.GuiServerMapPanel;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public class GuiRenderUpPanel extends GuiScreen {
   public static void renderUpPanel(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      if (mainAlpha <= 0.001F) {
         return;
      }

      boolean vanillaStyle = GuiScreen.isVanillaStyle();
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int textTwoColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(80.0F * mainAlpha));
      float islandY = GuiScreen.y + 21.0F;
      float islandH = 20.0F;
      float islandCenterY = islandY + islandH * 0.5F;
      float groupW = 24.0F + 6.0F + 140.0F + 6.0F + 24.0F;
      float groupCenter = (GuiScreen.SIDEBAR_WIDTH + GuiScreen.width) * 0.5F;
      float groupStart = groupCenter - groupW * 0.5F;
      float settingsIslandX = GuiScreen.x + groupStart;
      float searchIslandX = settingsIslandX + 30.0F;
      float closeIslandX = searchIslandX + 146.0F;
      float pillW = 62.0F;
      float pillH = 19.0F;
      float configPillX = searchIslandX + 70.0F - pillW * 0.5F;
      float configPillY = GuiScreen.y + 0.5F;
      float pillRadius = 9.0F;
      if (vanillaStyle) {
         renderer2D.rect(GuiScreen.x, GuiScreen.y, GuiScreen.width, GuiScreen.HEADER_HEIGHT, 1.25F, Renderer2D.ColorUtil.replAlpha(new Color(44, 44, 44).getRGB(), (int)(225.0F * mainAlpha)));
         renderer2D.rectOutline(GuiScreen.x, GuiScreen.y, GuiScreen.width, GuiScreen.HEADER_HEIGHT, 1.25F, Renderer2D.ColorUtil.replAlpha(new Color(90, 90, 90).getRGB(), (int)(210.0F * mainAlpha)), 1.0F);
      } else {
         GlassStyle.panel(renderer2D, settingsIslandX, islandY, 24.0F, islandH, 6.5F, 6.5F, 6.5F, 6.5F, mainAlpha);
         GlassStyle.panel(renderer2D, searchIslandX, islandY, 140.0F, islandH, 6.5F, 6.5F, 6.5F, 6.5F, mainAlpha);
         GlassStyle.panel(renderer2D, closeIslandX, islandY, 24.0F, islandH, 6.5F, 6.5F, 6.5F, 6.5F, mainAlpha);
         float mx = GuiScreen.currentMouseX;
         float my = GuiScreen.currentMouseY;
         GuiScreen.hoverSettingsIsland = easeHover(GuiScreen.hoverSettingsIsland,
               GuiRenderMain.isHovered(mx, my, settingsIslandX, islandY, 24.0F, islandH));
         GuiScreen.hoverSearchIsland = easeHover(GuiScreen.hoverSearchIsland,
               GuiRenderMain.isHovered(mx, my, searchIslandX, islandY, 140.0F, islandH));
         GuiScreen.hoverCloseIsland = easeHover(GuiScreen.hoverCloseIsland,
               GuiRenderMain.isHovered(mx, my, closeIslandX, islandY, 24.0F, islandH));
         if (GuiScreen.hoverSettingsIsland > 0.01F) {
            renderer2D.rect(settingsIslandX, islandY, 24.0F, islandH, 6.5F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(16.0F * GuiScreen.hoverSettingsIsland * mainAlpha)));
         }
         if (GuiScreen.hoverSearchIsland > 0.01F) {
            renderer2D.rect(searchIslandX, islandY, 140.0F, islandH, 6.5F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(10.0F * GuiScreen.hoverSearchIsland * mainAlpha)));
         }
         if (GuiScreen.hoverCloseIsland > 0.01F) {
            renderer2D.rect(closeIslandX, islandY, 24.0F, islandH, 6.5F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(16.0F * GuiScreen.hoverCloseIsland * mainAlpha)));
         }
         if (GuiScreen.activeSearch) {
            renderer2D.rectOutline(searchIslandX, islandY, 140.0F, islandH, 6.5F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(62.0F * mainAlpha)), 0.5F);
         }
      }

      GuiScreen.hoverConfigIsland = easeHover(GuiScreen.hoverConfigIsland,
            GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, configPillX, configPillY, pillW, pillH));
      GlassStyle.panel(renderer2D, configPillX, configPillY, pillW, pillH, pillRadius, pillRadius, pillRadius, pillRadius, mainAlpha);
      if (GuiScreen.hoverConfigIsland > 0.01F) {
         renderer2D.rect(configPillX, configPillY, pillW, pillH, pillRadius,
               Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(14.0F * GuiScreen.hoverConfigIsland * mainAlpha)));
      }
      float configTextSize = 15.0F;
      float configBaseline = configPillY + (pillH - configTextSize) * 0.5F + configTextSize * 0.82F;
      renderer2D.text(FontRegistry.INTER_MEDIUM, configPillX + pillW * 0.5F,
            configBaseline, configTextSize, "Config", textColor, "c");

      UiIcons.settings(renderer2D, settingsIslandX + 12.0F, islandCenterY, 9.0F, mainAlpha);
      UiIcons.close(renderer2D, closeIslandX + 12.0F, islandCenterY, 9.0F, mainAlpha);
      String searchDisplayText;
      if (GuiScreen.activeSearch) {
         searchDisplayText = GuiScreen.searchText.isEmpty() ? "" : GuiScreen.searchText;
      } else {
         searchDisplayText = "Search";
      }

      UiIcons.search(renderer2D, searchIslandX + 13.0F, islandCenterY, 9.0F, mainAlpha);
      float searchTextX = searchIslandX + 21.0F;
      float searchTextY = islandCenterY + 2.1F;
      int searchTextColor = GuiScreen.activeSearch && !GuiScreen.searchText.isEmpty() ? textColor : textTwoColor40;
      renderer2D.text(FontRegistry.INTER_MEDIUM, searchTextX, searchTextY, 14.0F, searchDisplayText, searchTextColor);
      if (GuiScreen.activeSearch) {
         long currentTime = System.currentTimeMillis();
         boolean showCursor = currentTime / 500L % 2L == 0L;
         if (showCursor) {
            float textWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, searchDisplayText, 14.0F).width;
            float cursorX = searchTextX + textWidth;
            float cursorY = searchTextY - 0.5F;
            renderer2D.rect(cursorX, cursorY - 5.0F, 1.0F, 7.0F, 0.5F, mainColor);
         }
      }

      GuiServerMapPanel.renderMapButton(renderer2D, mainAlpha);
   }

   public static void drawClientRect(Renderer2D r2, float x, float y, float w, float h, float radius, float alpha, float thickness) {
      r2.rectOutline(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), alpha * 0.1F), thickness);
      r2.rect(x, y, w, h, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), alpha * 0.7F));
   }
}