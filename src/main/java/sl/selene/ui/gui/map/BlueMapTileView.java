package sl.selene.ui.gui.map;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.util.render.texture.TextureLoader;

@Environment(EnvType.CLIENT)
public final class BlueMapTileView {
   public static final String BASE_URL = "http://194.164.96.153:8192";
   public static final String MAP_ID = "monas2";
   private static final int DEFAULT_BLOCK_X = 1732;
   private static final int DEFAULT_BLOCK_Z = 1812;
   private static final int FALLBACK_TILE_X = 6;
   private static final int FALLBACK_TILE_Z = 7;
   private static final int GRID_RADIUS = 1;

   private static final int[] LOD_CANDIDATES = new int[] { 1, 2, 3 };
   private static final int BLOCKS_PER_TILE_BASE = 32;
   private static final int MAX_CENTER_SEARCH_RADIUS = 14;
   private static final long REFRESH_INTERVAL_MS = 30000L;

   private final List<BlueMapTileView.TileSlot> tiles = new ArrayList<>();
   private volatile boolean active;
   private volatile boolean loading;
   private volatile boolean loadedAny;
   private volatile int resolvedLod = -1;
   private volatile int centerTileX;
   private volatile int centerTileZ;
   private volatile long lastRefreshMs;

   public void open() {
      this.active = true;
      this.loadedAny = false;
      this.resolvedLod = -1;
      this.lastRefreshMs = 0L;
      this.releaseTextures();
      this.tiles.clear();
      this.scheduleRefresh(true);
   }

   public void close() {
      this.active = false;
      this.loading = false;
      this.releaseTextures();
      this.tiles.clear();
      this.loadedAny = false;
      this.resolvedLod = -1;
   }

   public void tick() {
      if (!this.active || this.loading) {
         return;
      }

      long now = System.currentTimeMillis();
      if (this.lastRefreshMs > 0L && now - this.lastRefreshMs >= REFRESH_INTERVAL_MS) {
         this.scheduleRefresh(false);
      }
   }

   public boolean isLoading() {
      return this.loading;
   }

   public boolean hasTiles() {
      return this.loadedAny;
   }

   public int getResolvedLod() {
      return this.resolvedLod;
   }

   public List<TileSlot> getTiles() {
      return this.tiles;
   }

   public int getCenterTileX() {
      return this.centerTileX;
   }

   public int getCenterTileZ() {
      return this.centerTileZ;
   }

   private void scheduleRefresh(boolean force) {
      if (!this.active || this.loading) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null) {
         return;
      }

      int blockX = DEFAULT_BLOCK_X;
      int blockZ = DEFAULT_BLOCK_Z;
      if (client.player != null) {
         blockX = (int) Math.floor(client.player.getX());
         blockZ = (int) Math.floor(client.player.getZ());
      }

      this.loading = true;
      int finalBlockX = blockX;
      int finalBlockZ = blockZ;
      Thread thread = new Thread(() -> this.loadTilesAround(finalBlockX, finalBlockZ, force), "BlueMapTileView");
      thread.setDaemon(true);
      thread.start();
   }

   private void loadTilesAround(int blockX, int blockZ, boolean force) {
      try {
         for (int lod : LOD_CANDIDATES) {
            int blocksPerTile = BLOCKS_PER_TILE_BASE << lod;
            int preferredTileX = Math.floorDiv(blockX, blocksPerTile);
            int preferredTileZ = Math.floorDiv(blockZ, blocksPerTile);
            int[] center = this.resolveTileCenter(lod, preferredTileX, preferredTileZ);
            if (center == null) {
               continue;
            }

            List<PendingTile> pending = this.fetchGridBytes(lod, center[0], center[1]);
            if (!pending.isEmpty()) {
               MinecraftClient client = MinecraftClient.getInstance();
               if (client != null) {
                  int centerX = center[0];
                  int centerZ = center[1];
                  client.execute(() -> this.uploadAndApply(lod, centerX, centerZ, pending, force));
               }
               return;
            }
         }

         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null) {
            client.execute(() -> this.finishFailed(force));
         }
      } catch (Exception error) {
         System.out.println("[BlueMapTileView] Load failed: " + error.getMessage());
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null) {
            client.execute(() -> this.finishFailed(force));
         }
      }
   }

   private int[] resolveTileCenter(int lod, int preferredTileX, int preferredTileZ) {
      if (this.downloadTileBytes(lod, preferredTileX, preferredTileZ) != null) {
         return new int[] { preferredTileX, preferredTileZ };
      }

      for (int radius = 1; radius <= MAX_CENTER_SEARCH_RADIUS; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                  continue;
               }

               int tileX = preferredTileX + dx;
               int tileZ = preferredTileZ + dz;
               if (this.downloadTileBytes(lod, tileX, tileZ) != null) {
                  return new int[] { tileX, tileZ };
               }
            }
         }
      }

      if (this.downloadTileBytes(lod, FALLBACK_TILE_X, FALLBACK_TILE_Z) != null) {
         return new int[] { FALLBACK_TILE_X, FALLBACK_TILE_Z };
      }

      return null;
   }

   private List<PendingTile> fetchGridBytes(int lod, int centerX, int centerZ) {
      List<PendingTile> result = new ArrayList<>();

      for (int dx = -GRID_RADIUS; dx <= GRID_RADIUS; dx++) {
         for (int dz = -GRID_RADIUS; dz <= GRID_RADIUS; dz++) {
            int tileX = centerX + dx;
            int tileZ = centerZ + dz;
            byte[] data = this.downloadTileBytes(lod, tileX, tileZ);
            if (data != null) {
               result.add(new PendingTile(tileX, tileZ, data));
            }
         }
      }

      return result;
   }

   private byte[] downloadTileBytes(int lod, int tileX, int tileZ) {
      String[] urls = new String[] {
            BASE_URL + "/maps/" + MAP_ID + "/tiles/" + lod + "/x" + tileX + "/z" + tileZ + ".png",
            BASE_URL + "/maps/" + MAP_ID + "/tiles/" + lod + "/" + tileX + "_" + tileZ + ".png"
      };

      for (String url : urls) {
         byte[] data = this.httpGet(url);
         if (isPng(data)) {
            return data;
         }
      }

      return null;
   }

   private static boolean isPng(byte[] data) {
      return data != null
            && data.length >= 8
            && data[0] == (byte) 0x89
            && data[1] == 0x50
            && data[2] == 0x4E
            && data[3] == 0x47;
   }

   private byte[] httpGet(String url) {
      HttpURLConnection connection = null;
      try {
         connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
         connection.setRequestMethod("GET");
         connection.setConnectTimeout(4000);
         connection.setReadTimeout(8000);
         connection.setInstanceFollowRedirects(true);
         connection.setRequestProperty("Accept", "image/png,*/*");
         connection.setRequestProperty("User-Agent", "ZeroClient/1.0");
         int code = connection.getResponseCode();
         if (code == 204 || code < 200 || code >= 300) {
            return null;
         }

         try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream(32768)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
               output.write(buffer, 0, read);
            }

            byte[] bytes = output.toByteArray();
            return bytes.length > 0 ? bytes : null;
         }
      } catch (Exception ignored) {
         return null;
      } finally {
         if (connection != null) {
            connection.disconnect();
         }
      }
   }

   private void uploadAndApply(int lod, int centerX, int centerZ, List<PendingTile> pending, boolean force) {
      this.loading = false;
      this.lastRefreshMs = System.currentTimeMillis();
      if (!this.active) {
         return;
      }

      if (TextureLoader.getBackend() == null) {
         System.out.println("[BlueMapTileView] TextureLoader backend is null");
         this.finishFailed(force);
         return;
      }

      List<TileSlot> uploaded = new ArrayList<>();
      for (PendingTile tile : pending) {
         int textureId = TextureLoader.loadFromPngBytes(tile.pngData);
         if (textureId > 0) {
            uploaded.add(new TileSlot(tile.tileX, tile.tileZ, textureId));
         }
      }

      if (uploaded.isEmpty()) {
         System.out.println("[BlueMapTileView] PNG tiles downloaded but textures failed to upload");
         this.finishFailed(force);
         return;
      }

      if (!force && this.loadedAny && this.resolvedLod == lod && this.centerTileX == centerX && this.centerTileZ == centerZ) {
         for (TileSlot tile : uploaded) {
            TextureLoader.releaseTexture(tile.textureId);
         }
         return;
      }

      this.releaseTextures();
      this.tiles.clear();
      this.tiles.addAll(uploaded);
      this.resolvedLod = lod;
      this.centerTileX = centerX;
      this.centerTileZ = centerZ;
      this.loadedAny = true;
   }

   private void finishFailed(boolean force) {
      this.loading = false;
      this.lastRefreshMs = System.currentTimeMillis();
      if (!this.active) {
         return;
      }

      if (!force && this.loadedAny) {
         return;
      }

      this.loadedAny = false;
      this.resolvedLod = -1;
   }

   private void releaseTextures() {
      for (TileSlot tile : this.tiles) {
         TextureLoader.releaseTexture(tile.textureId);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class PendingTile {
      private final int tileX;
      private final int tileZ;
      private final byte[] pngData;

      private PendingTile(int tileX, int tileZ, byte[] pngData) {
         this.tileX = tileX;
         this.tileZ = tileZ;
         this.pngData = pngData;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class TileSlot {
      public final int tileX;
      public final int tileZ;
      public final int textureId;

      public TileSlot(int tileX, int tileZ, int textureId) {
         this.tileX = tileX;
         this.tileZ = tileZ;
         this.textureId = textureId;
      }
   }
}