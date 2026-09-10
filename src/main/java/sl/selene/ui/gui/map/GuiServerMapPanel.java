package sl.selene.ui.gui.map;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.render.GlassStyle;
import sl.selene.ui.gui.component.render.GuiRenderMain;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public final class GuiServerMapPanel extends GuiScreen {
   public static final float MAP_BUTTON_X_OFFSET = 364.35F;
   public static final float MAP_BUTTON_SIZE = 21.325F;
   private static final String TARGET_SERVER_IP = "194.164.96.153";

   private static final BlueMapTileView TILE_VIEW = new BlueMapTileView();

   private GuiServerMapPanel() {
   }

   public static boolean isMapButtonVisible() {
      return isTargetServer(MinecraftClient.getInstance());
   }

   private static boolean isTargetServer(MinecraftClient mc) {
      if (mc == null || mc.isConnectedToLocalServer()) {
         return false;
      }

      String entryAddress = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : null;
      if (entryAddress != null && normalizeHost(entryAddress).equals(TARGET_SERVER_IP)) {
         return true;
      }

      if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
         SocketAddress address = mc.getNetworkHandler().getConnection().getAddress();
         if (address instanceof InetSocketAddress inetSocketAddress) {
            String hostString = inetSocketAddress.getHostString();
            if (TARGET_SERVER_IP.equals(normalizeHost(hostString))) {
               return true;
            }
            InetAddress inetAddress = inetSocketAddress.getAddress();
            if (inetAddress != null && TARGET_SERVER_IP.equals(inetAddress.getHostAddress())) {
               return true;
            }
         }
      }
      return false;
   }

   private static String normalizeHost(String raw) {
      if (raw == null) return null;
      String value = raw.trim();
      if (value.startsWith("/")) {
         value = value.substring(1);
      }
      int colonIndex = value.lastIndexOf(':');
      if (colonIndex > 0) {
         value = value.substring(0, colonIndex);
      }
      return value;
   }

   public static boolean isOpen() {
      return GuiScreen.serverMapOpen;
   }

   public static void setOpen(boolean open) {
      if (open == GuiScreen.serverMapOpen) {
         return;
      }

      GuiScreen.serverMapOpen = open;
      if (open) {
         GuiScreen.settingsPageOpen = false;
         GuiScreen.configInputActive = false;
         TILE_VIEW.open();
      } else {
         TILE_VIEW.close();
      }
   }

   public static void toggle() {
      setOpen(!GuiScreen.serverMapOpen);
   }

   public static void tick() {
      if (GuiScreen.serverMapOpen && !isMapButtonVisible()) {
         setOpen(false);
      }

      if (GuiScreen.serverMapOpen) {
         TILE_VIEW.tick();
      }
   }

   public static float mapButtonX() {
      return GuiScreen.x + GuiScreen.width - MAP_BUTTON_X_OFFSET - MAP_BUTTON_SIZE;
   }

   public static float mapButtonY() {
      return GuiScreen.y + 6.185F;
   }

   public static void renderMapButton(Renderer2D renderer2D, float mainAlpha) {
      if (!isMapButtonVisible() || mainAlpha <= 0.001F) {
         return;
      }

      int activeBg = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int) (50.0F * mainAlpha));
      float bx = mapButtonX();
      float by = mapButtonY();

      GlassStyle.panel(renderer2D, bx, by, MAP_BUTTON_SIZE, MAP_BUTTON_SIZE, 6.5F, 6.5F, 6.5F, 6.5F, mainAlpha);
      if (GuiScreen.serverMapOpen) {
         renderer2D.rect(bx, by, MAP_BUTTON_SIZE, MAP_BUTTON_SIZE, 6.5F, activeBg);
      }

      UiIcons.map(renderer2D, bx + MAP_BUTTON_SIZE * 0.5F, by + MAP_BUTTON_SIZE * 0.5F, 13.0F, mainAlpha);
   }

   public static void renderPanel(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      if (!GuiScreen.serverMapOpen || mainAlpha <= 0.001F) {
         return;
      }

      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int) (20.4F * mainAlpha));
      int backGroundThreeColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int) (10.2F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int) (255.0F * mainAlpha));
      int textMuted = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int) (255.0F * mainAlpha));
      float panelX = GuiScreen.x + 8.0F;
      float panelY = GuiScreen.y + 38.0F;
      float panelW = GuiScreen.width - 16.0F;
      float panelH = GuiScreen.height - 46.0F;
      renderer2D.rectOutline(panelX, panelY, panelW, panelH, 6.0F, outlineColor, 0.1F);
      renderer2D.rect(panelX, panelY, panelW, panelH, 6.0F, backGroundThreeColor);
      float mapX = panelX + 6.0F;
      float mapY = panelY + 24.0F;
      float mapW = panelW - 12.0F;
      float mapH = panelH - 28.0F;
      renderer2D.rect(mapX, mapY, mapW, mapH, 5.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int) (120.0F * mainAlpha)));
      renderer2D.text(FontRegistry.INTER_MEDIUM, panelX + 10.0F, panelY + 8.0F + 7.0F, 14.0F, "Server Map", textColor);
      if (TILE_VIEW.hasTiles()) {
         renderTiles(renderer2D, mapX, mapY, mapW, mapH, mainAlpha);
      } else if (TILE_VIEW.isLoading()) {
         renderer2D.text(FontRegistry.INTER_MEDIUM, mapX + 12.0F, mapY + mapH / 2.0F, 14.0F, "Loading map...", textMuted);
      } else {
         renderer2D.text(FontRegistry.INTER_MEDIUM, mapX + 12.0F, mapY + mapH / 2.0F - 10.0F, 13.0F, "Preview unavailable", textMuted);
         renderer2D.text(FontRegistry.INTER_MEDIUM, mapX + 12.0F, mapY + mapH / 2.0F + 6.0F, 12.0F, "The server has not rendered this area", textMuted);
      }
   }

   private static void renderTiles(Renderer2D renderer2D, float mapX, float mapY, float mapW, float mapH, float mainAlpha) {
      int lod = TILE_VIEW.getResolvedLod();
      if (lod < 0) {
         return;
      }

      int blocksPerTile = 32 << lod;
      int centerX = TILE_VIEW.getCenterTileX();
      int centerZ = TILE_VIEW.getCenterTileZ();
      float tileScreen = Math.min(mapW, mapH) / 3.0F;
      float originX = mapX + mapW / 2.0F - tileScreen / 2.0F;
      float originZ = mapY + mapH / 2.0F - tileScreen / 2.0F;

      for (BlueMapTileView.TileSlot tile : TILE_VIEW.getTiles()) {
         float offsetX = (tile.tileX - centerX) * tileScreen;
         float offsetZ = (tile.tileZ - centerZ) * tileScreen;
         renderer2D.drawRgbaTexture(
               tile.textureId,
               originX + offsetX,
               originZ + offsetZ,
               tileScreen,
               tileScreen,
               Renderer2D.ColorUtil.replAlpha(-1, (int) (255.0F * mainAlpha))
         );
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.player != null) {
         int blockX = (int) Math.floor(client.player.getX());
         int blockZ = (int) Math.floor(client.player.getZ());
         int playerTileX = Math.floorDiv(blockX, blocksPerTile);
         int playerTileZ = Math.floorDiv(blockZ, blocksPerTile);
         float localX = (blockX - playerTileX * blocksPerTile) / (float) blocksPerTile;
         float localZ = (blockZ - playerTileZ * blocksPerTile) / (float) blocksPerTile;
         float markerX = originX + (playerTileX - centerX) * tileScreen + localX * tileScreen - 2.0F;
         float markerZ = originZ + (playerTileZ - centerZ) * tileScreen + localZ * tileScreen - 2.0F;
         int markerColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int) (255.0F * mainAlpha));
         renderer2D.rect(markerX, markerZ, 4.0F, 4.0F, 2.0F, markerColor);
      }
   }

   public static boolean handleMapButtonClick(int mouseX, int mouseY, int button) {
      if (!isMapButtonVisible() || button != 0) {
         return false;
      }

      if (GuiRenderMain.isHovered(mouseX, mouseY, mapButtonX(), mapButtonY(), MAP_BUTTON_SIZE, MAP_BUTTON_SIZE)) {
         toggle();
         return true;
      }

      return false;
   }

   public static boolean handlePanelClick(int mouseX, int mouseY, int button) {
      if (!GuiScreen.serverMapOpen || button != 0) {
         return false;
      }

      float panelX = GuiScreen.x + 8.0F;
      float panelY = GuiScreen.y + 38.0F;
      float panelW = GuiScreen.width - 16.0F;
      float panelH = GuiScreen.height - 46.0F;
      return GuiRenderMain.isHovered(mouseX, mouseY, panelX, panelY, panelW, panelH);
   }
}