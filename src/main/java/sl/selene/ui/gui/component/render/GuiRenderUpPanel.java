package sl.selene.ui.gui.component.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.map.GuiServerMapPanel;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.motion.Motion;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public class GuiRenderUpPanel extends GuiScreen {
   private static final float ISLAND_Y = 21.0F;
   private static final float ISLAND_H = 20.0F;
   private static final float SEARCH_ISLAND_W = 140.0F;
   private static final float CLOSE_ISLAND_W = 24.0F;
   private static final float GROUP_GAP = 8.0F;
   private static final float TAB_BAR_PADDING = 3.0F;
   private static final float TAB_BAR_RADIUS = 7.0F;
   private static final float TAB_ICON_SIZE = 11.0F;
   private static final float SEARCH_ICON_SIZE = 9.0F;
   private static final float CLOSE_ICON_SIZE = 9.0F;
   private static final int TAB_SELECTED_INK = Renderer2D.ColorUtil.rgba(255, 255, 255, 255);
   private static final float TAB_ICON_ALPHA = 0.55F;
   private static final float TAB_ICON_GROW = 0.45F;
   private static final float TAB_PRESS_SQUEEZE = 0.24F;
   private static final float TAB_PRESS_DIP = 2.2F;
   private static final float TAB_DOCK_SPEED = 15.0F;
   private static final float[] TAB_DOCK_FALLOFF = { 1.0F, 0.22F, 0.06F };
   public static final float TAB_SIZE = 18.0F;
   public static final float TAB_GAP = 3.0F;

   public static float islandY() {
      return GuiScreen.y + ISLAND_Y;
   }

   public static float islandHeight() {
      return ISLAND_H;
   }

   private static float headerGroupWidth() {
      return SEARCH_ISLAND_W + GROUP_GAP + CLOSE_ISLAND_W;
   }

   public static float headerGroupStart() {
      return (GuiScreen.SIDEBAR_WIDTH + GuiScreen.width) * 0.5F - headerGroupWidth() * 0.5F;
   }

   public static float searchIslandX() {
      return GuiScreen.x + headerGroupStart();
   }

   public static float searchIslandWidth() {
      return SEARCH_ISLAND_W;
   }

   public static float closeIslandX() {
      return searchIslandX() + SEARCH_ISLAND_W + GROUP_GAP;
   }

   public static float closeIslandWidth() {
      return CLOSE_ISLAND_W;
   }

   public static float tabBarWidth() {
      return TAB_BAR_PADDING * 2.0F + GuiScreen.TAB_COUNT * TAB_SIZE + (GuiScreen.TAB_COUNT - 1) * TAB_GAP;
   }

   public static float tabBarHeight() {
      return GuiScreen.PINNED_BAR_HEIGHT;
   }

   public static float tabBarX() {
      return (GuiScreen.screenWidth - tabBarWidth()) * 0.5F;
   }

   public static float tabBarY() {
      return GuiScreen.pinnedBarTop();
   }

   public static float tabX(int index) {
      return tabBarX() + TAB_BAR_PADDING + index * (TAB_SIZE + TAB_GAP);
   }

   public static float tabY() {
      return tabBarY() + (tabBarHeight() - TAB_SIZE) * 0.5F;
   }

   public static boolean isOverTabBar(float mouseX, float mouseY) {
      return GuiRenderMain.isHovered(mouseX, mouseY, tabBarX(), tabBarY(), tabBarWidth(), tabBarHeight());
   }

   public static void renderUpPanel(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      if (mainAlpha <= 0.001F) {
         return;
      }

      float islandY = islandY();
      float islandH = islandHeight();
      float islandCenterY = islandY + islandH * 0.5F;
      float searchIslandX = searchIslandX();
      float closeIslandX = closeIslandX();
      GlassStyle.panel(renderer2D, searchIslandX, islandY, SEARCH_ISLAND_W, islandH, 6.5F, 6.5F, 6.5F, 6.5F, mainAlpha);
      GlassStyle.panel(renderer2D, closeIslandX, islandY, CLOSE_ISLAND_W, islandH, 6.5F, 6.5F, 6.5F, 6.5F, mainAlpha);
      float mx = GuiScreen.currentMouseX;
      float my = GuiScreen.currentMouseY;
      GuiScreen.hoverSearchIsland = easeHover(GuiScreen.hoverSearchIsland,
            GuiRenderMain.isHovered(mx, my, searchIslandX, islandY, SEARCH_ISLAND_W, islandH));
      GuiScreen.hoverCloseIsland = easeHover(GuiScreen.hoverCloseIsland,
            GuiRenderMain.isHovered(mx, my, closeIslandX, islandY, CLOSE_ISLAND_W, islandH));
      if (GuiScreen.hoverSearchIsland > 0.01F) {
         renderer2D.rect(searchIslandX, islandY, SEARCH_ISLAND_W, islandH, 6.5F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(10.0F * GuiScreen.hoverSearchIsland * mainAlpha)));
      }
      if (GuiScreen.hoverCloseIsland > 0.01F) {
         renderer2D.rect(closeIslandX, islandY, CLOSE_ISLAND_W, islandH, 6.5F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(16.0F * GuiScreen.hoverCloseIsland * mainAlpha)));
      }
      if (GuiScreen.activeSearch) {
         renderer2D.rectOutline(searchIslandX, islandY, SEARCH_ISLAND_W, islandH, 6.5F,
               Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(62.0F * mainAlpha)), 0.5F);
      }

      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int textTwoColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(80.0F * mainAlpha));
      UiIcons.close(renderer2D, closeIslandX + CLOSE_ISLAND_W * 0.5F, islandCenterY, CLOSE_ICON_SIZE, mainAlpha);
      String searchDisplayText;
      if (GuiScreen.activeSearch) {
         searchDisplayText = GuiScreen.searchText.isEmpty() ? "" : GuiScreen.searchText;
      } else {
         searchDisplayText = "Search";
      }

      UiIcons.search(renderer2D, searchIslandX + 13.0F, islandCenterY, SEARCH_ICON_SIZE, mainAlpha);
      float searchTextX = searchIslandX + 21.0F;
      float searchTextY = islandCenterY + 2.1F;
      int searchTextColor = GuiScreen.activeSearch && !GuiScreen.searchText.isEmpty() ? textColor : textTwoColor40;
      renderer2D.text(FontRegistry.INTER_MEDIUM, searchTextX, searchTextY, 14.0F, searchDisplayText, searchTextColor);
      if (GuiScreen.activeSearch) {
         float cursorAlpha = Motion.blink(Motion.CURSOR_BLINK_HALF_PERIOD_MS);
         if (cursorAlpha > 0.01F) {
            float textWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, searchDisplayText, 14.0F).width;
            float cursorX = searchTextX + textWidth;
            float cursorY = searchTextY - 0.5F;
            renderer2D.rect(cursorX, cursorY - 5.0F, 1.0F, 7.0F, 0.5F, ColorUtil.multAlpha(mainColor, cursorAlpha));
         }
      }

      GuiServerMapPanel.renderMapButton(renderer2D, mainAlpha);
   }

   public static void renderTabBar(Renderer2D renderer, float alpha) {
      if (alpha <= 0.001F) {
         return;
      }

      float barX = tabBarX();
      float barY = tabBarY();
      float barW = tabBarWidth();
      float barH = tabBarHeight();
      GlassStyle.panel(renderer, barX, barY, barW, barH, TAB_BAR_RADIUS, TAB_BAR_RADIUS, TAB_BAR_RADIUS, TAB_BAR_RADIUS, alpha * 0.9F);

      int hoveredTab = hoveredTab();
      for (int i = 0; i < GuiScreen.TAB_COUNT; i++) {
         float target = 0.0F;
         if (hoveredTab >= 0) {
            int distance = Math.abs(i - hoveredTab);
            target = distance < TAB_DOCK_FALLOFF.length ? TAB_DOCK_FALLOFF[distance] : 0.0F;
         }

         float grow = Motion.smooth(GuiScreen.tabHoverAnimations[i], target, TAB_DOCK_SPEED);
         GuiScreen.tabHoverAnimations[i] = grow;
         float press = GuiScreen.tabPressValues[i];
         float iconSize = TAB_ICON_SIZE * (1.0F + TAB_ICON_GROW * grow) * (1.0F - TAB_PRESS_SQUEEZE * press);
         float centerX = tabX(i) + TAB_SIZE * 0.5F;
         float centerY = tabY() + TAB_SIZE * 0.5F + TAB_PRESS_DIP * press;

         if (GuiScreen.selectedTab == i) {
            UiIcons.drawTinted(renderer, GuiScreen.TAB_ICONS[i], centerX, centerY, iconSize, alpha, TAB_SELECTED_INK);
         } else {
            UiIcons.draw(renderer, GuiScreen.TAB_ICONS[i], centerX, centerY, iconSize, alpha * TAB_ICON_ALPHA);
         }
      }
   }

   private static int hoveredTab() {
      float hitY = tabBarY() - 3.0F;
      float hitHeight = tabBarHeight() + 6.0F;
      for (int i = 0; i < GuiScreen.TAB_COUNT; i++) {
         float hitX = tabX(i) - TAB_GAP * 0.5F;
         float hitWidth = TAB_SIZE + TAB_GAP;
         if (GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, hitX, hitY,
               hitWidth, hitHeight)) {
            return i;
         }
      }

      return -1;
   }

   public static void drawClientRect(Renderer2D r2, float x, float y, float w, float h, float radius, float alpha, float thickness) {
      r2.rectOutline(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), alpha * 0.1F), thickness);
      r2.rect(x, y, w, h, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), alpha * 0.7F));
   }
}

