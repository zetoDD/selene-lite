package sl.selene.module.impl.visuals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventPacket;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.event.render.WorldRenderEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.world.WorldRenderUtil;
import sl.selene.util.render.world.WorldRenderer;

@IModule(name = "HoleESP", description = "Shows deep fall pits through the ground", category = Category.Donut, bind = -1)
@Environment(EnvType.CLIENT)
public class HoleESP extends Module {

   public static SliderSetting range = new SliderSetting("Range", 64.0F, 16.0F, 128.0F, 8.0F, false);
   public static SliderSetting maxSize = new SliderSetting("Max Size", 9.0F, 4.0F, 25.0F, 1.0F, false);
   public static final int MAX_HOLE_WIDTH = 3;
   public static SliderSetting minDepth = new SliderSetting("Min Depth", 3.0F, 1.0F, 16.0F, 1.0F, false);
   public static SliderSetting fillAlpha = new SliderSetting("Fill Alpha", 90.0F, 0.0F, 255.0F, 5.0F, false);
   public static SliderSetting outlineAlpha = new SliderSetting("Outline Alpha", 180.0F, 0.0F, 255.0F, 5.0F, false);
   public static BooleanSetting gradient = new BooleanSetting("Gradient", true);
   public static HueSetting color = new HueSetting("Color", 28.0F, 1.0F, 1.0F);
   public static SliderSetting rescanInterval = new SliderSetting("Rescan Interval", 120.0F, 0.0F, 600.0F, 15.0F, false);

   private static final int CHUNKS_PER_TICK = 2;
   private static final float OUTLINE_HALF_THICKNESS = 0.04F;
   private static final float MIN_GRADIENT_ALPHA_FACTOR = 0.18F;
   private static final int MAX_GRADIENT_LAYERS = 16;
   private static final int RESCAN_COOLDOWN_TICKS = 40;
   private static final int SKY_EXPOSURE_WINDOW = 8;

   private final Map<Long, List<Hole>> chunkHoles = new HashMap<>();
   private final Queue<Long> scanQueue = new ArrayDeque<>();
   private final Set<Long> queuedChunks = new HashSet<>();
   private final Set<Long> scannedChunks = new HashSet<>();
   private final Map<Long, Integer> lastScanTick = new HashMap<>();
   private final Set<Long> visitedCells = new HashSet<>();
   private WorldChunk currentChunk;
   private ClientWorld world;
   private int tickCounter;
   private int tick;
   private String lastSettings = "";

   public HoleESP() {
      this.addSettings(new Setting[] { range, maxSize, minDepth, fillAlpha, outlineAlpha, gradient, color, rescanInterval });
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.clear();
      this.world = mc.world;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.clear();
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.world == null || mc.player == null) {
         return;
      }

      this.tick++;
      if (mc.world != this.world) {
         this.world = (ClientWorld) mc.world;
         this.clear();
      }

      String settings = this.range.get() + "|" + this.maxSize.get() + "|" + this.minDepth.get();
      if (!settings.equals(this.lastSettings)) {
         this.lastSettings = settings;
         this.scannedChunks.clear();
      }

      float rescanSeconds = this.rescanInterval.get();
      if (rescanSeconds > 0.0F && ++this.tickCounter >= Math.max(20, Math.round(rescanSeconds * 20.0F))) {
         this.tickCounter = 0;
         this.scannedChunks.clear();
      }

      int playerChunkX = mc.player.getChunkPos().x;
      int playerChunkZ = mc.player.getChunkPos().z;
      int radius = Math.max(1, Math.min(8, Math.round(this.range.get()) / 16));
      Set<Long> active = new HashSet<>();
      for (int cx = playerChunkX - radius; cx <= playerChunkX + radius; cx++) {
         for (int cz = playerChunkZ - radius; cz <= playerChunkZ + radius; cz++) {
            long key = ChunkPos.toLong(cx, cz);
            active.add(key);
            if (!this.scannedChunks.contains(key) && this.queuedChunks.add(key)) {
               this.scanQueue.add(key);
            }
         }
      }

      int processed = 0;
      while (!this.scanQueue.isEmpty() && processed < CHUNKS_PER_TICK) {
         Long key = this.scanQueue.poll();
         this.queuedChunks.remove(key);
         WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key), false);
         if (chunk != null) {
            this.scanChunk(chunk);
            this.scannedChunks.add(key);
            this.lastScanTick.put(key, this.tick);
            processed++;
         }
      }

      this.chunkHoles.keySet().removeIf(key -> !active.contains(key) && !this.scanQueue.contains(key));
   }

   private void scanChunk(WorldChunk chunk) {
      long key = chunk.getPos().toLong();
      this.chunkHoles.remove(key);

      this.currentChunk = chunk;
      this.visitedCells.clear();
      List<Hole> found = new ArrayList<>();
      try {
         ChunkSection[] sections = chunk.getSectionArray();
         int baseY = chunk.getBottomY();
         int startX = chunk.getPos().getStartX();
         int startZ = chunk.getPos().getStartZ();

         for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            ChunkSection section = sections[sectionIndex];
            if (section == null || section.isEmpty()) {
               continue;
            }

            int sectionBaseY = baseY + sectionIndex * 16;
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 15; y >= 0; y--) {
                     int worldY = sectionBaseY + y;
                     if (worldY >= mc.world.getBottomY() + mc.world.getHeight()) {
                        continue;
                     }
                     this.collectHoleAt(found, startX + x, worldY, startZ + z, key);
                  }
               }
            }
         }
      } finally {
         this.currentChunk = null;
      }

      if (!found.isEmpty()) {
         this.chunkHoles.put(key, found);
      }
   }

   @EventInit
   public void onPacket(EventPacket event) {
      if (event.isSend() || mc.world == null) {
         return;
      }
      Packet<?> packet = event.getPacket();
      if (packet instanceof BlockUpdateS2CPacket blockUpdate) {
         this.markDirty(new ChunkPos(blockUpdate.getPos()).toLong());
      } else if (packet instanceof ChunkDeltaUpdateS2CPacket delta) {
         delta.visitUpdates((pos, state) -> this.markDirty(new ChunkPos(pos).toLong()));
      } else if (packet instanceof BlockEntityUpdateS2CPacket blockEntityUpdate) {
         this.markDirty(new ChunkPos(blockEntityUpdate.getPos()).toLong());
      }
   }

   private void markDirty(long key) {
      Integer last = this.lastScanTick.get(key);
      if (last != null && this.tick - last < RESCAN_COOLDOWN_TICKS) {
         return;
      }
      if (this.scannedChunks.remove(key) && this.queuedChunks.add(key)) {
         this.scanQueue.add(key);
      }
   }

   private void collectHoleAt(List<Hole> found, int x, int y, int z, long chunkKey) {
      BlockPos pos = new BlockPos(x, y, z);
      if (!this.isPitLike(pos)) {
         return;
      }
      if (this.visitedCells.contains(pos.asLong())) {
         return;
      }
      if (this.wallsAround(pos) < 2) {
         return;
      }
      this.tryAddRegion(found, pos, chunkKey);
   }

   private void tryAddRegion(List<Hole> found, BlockPos start, long chunkKey) {
      int maxSize = Math.round(this.maxSize.get());
      int worldBottom = mc.world == null ? -64 : mc.world.getBottomY();
      int[] bounds = new int[] { Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE };

      List<BlockPos> region = this.floodRegion(List.of(start), maxSize);
      if (region == null || !this.isEnclosedRegion(region)) {
         return; 
      }
      addBounds(bounds, region);
      int openingCount = region.size();

      List<BlockPos> level = region;
      List<BlockPos> topLevel = level;
      int topY = start.getY();
      while (true) {
         List<BlockPos> seeds = new ArrayList<>();
         for (BlockPos cell : level) {
            if (this.isPitLike(cell.up())) {
               seeds.add(cell.up());
            }
         }
         if (seeds.isEmpty()) {
            break;
         }
         List<BlockPos> above = this.floodRegion(seeds, maxSize);
         if (above == null || !this.isEnclosedRegion(above) || above.size() > openingCount + 2) {
            break; 
         }
         addBounds(bounds, above);
         topY = above.get(0).getY();
         topLevel = above;
         level = above;
      }

      int floorY = topY;
      level = region;
      while (true) {
         addBounds(bounds, level);
         List<BlockPos> seeds = new ArrayList<>();
         for (BlockPos cell : level) {
            BlockPos below = cell.down();
            if (below.getY() >= worldBottom && this.isPitLike(below)) {
               seeds.add(below);
            }
         }
         if (seeds.isEmpty()) {
            floorY = level.get(0).getY(); 
            break;
         }
         List<BlockPos> next = this.floodRegion(seeds, maxSize);
         if (next == null || !this.isEnclosedRegion(next) || next.size() > openingCount + 2) {
            return; 
         }
         floorY = next.get(0).getY();
         level = next;
      }

      int depth = topY - floorY + 1;
      if (depth < Math.round(this.minDepth.get())) {
         return;
      }

      if (!this.isExposedToSky(topLevel)) {
         return;
      }

      this.addIfClear(found,
            new Box(bounds[0], floorY, bounds[2], bounds[1] + 1, topY + 1, bounds[3] + 1), chunkKey);
   }

   private boolean isExposedToSky(List<BlockPos> topLevel) {
      if (topLevel == null || topLevel.isEmpty()) {
         return false;
      }
      int maxY = mc.world == null ? 320 : mc.world.getBottomY() + mc.world.getHeight();
      for (BlockPos cell : topLevel) {
         int y = cell.getY() + 1;
         int limit = Math.min(cell.getY() + 1 + SKY_EXPOSURE_WINDOW, maxY);
         for (; y <= limit; y++) {
            if (!this.isPitLike(cell.withY(y))) {
               break; 
            }
         }
         if (y > limit) {
            return true; 
         }
      }
      return false;
   }

   private List<BlockPos> floodRegion(List<BlockPos> seeds, int maxSize) {
      List<BlockPos> region = new ArrayList<>();
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      for (BlockPos seed : seeds) {
         if (this.isPitLike(seed) && this.visitedCells.add(seed.asLong())) {
            queue.add(seed);
         }
      }
      int minX = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxZ = Integer.MIN_VALUE;
      while (!queue.isEmpty()) {
         BlockPos pos = queue.poll();
         region.add(pos);
         minX = Math.min(minX, pos.getX());
         maxX = Math.max(maxX, pos.getX());
         minZ = Math.min(minZ, pos.getZ());
         maxZ = Math.max(maxZ, pos.getZ());
         if (region.size() > maxSize || maxX - minX + 1 > MAX_HOLE_WIDTH || maxZ - minZ + 1 > MAX_HOLE_WIDTH) {
            return null;
         }
         for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos neighbor = pos.offset(direction);
            if (this.isPitLike(neighbor) && this.visitedCells.add(neighbor.asLong())) {
               queue.add(neighbor);
            }
         }
      }
      return region;
   }

   private boolean isEnclosedRegion(List<BlockPos> region) {
      Set<Long> members = new HashSet<>();
      for (BlockPos cell : region) {
         members.add(cell.asLong());
      }
      int boundary = 0;
      int walls = 0;
      for (BlockPos cell : region) {
         for (Direction direction : Direction.Type.HORIZONTAL) {
            if (members.contains(cell.offset(direction).asLong())) {
               continue;
            }
            boundary++;
            if (this.isSolidFloor(cell.offset(direction))) {
               walls++;
            }
         }
      }
      return boundary > 0 && walls * 4 >= boundary * 3;
   }

   private static void addBounds(int[] bounds, List<BlockPos> region) {
      for (BlockPos cell : region) {
         bounds[0] = Math.min(bounds[0], cell.getX());
         bounds[1] = Math.max(bounds[1], cell.getX());
         bounds[2] = Math.min(bounds[2], cell.getZ());
         bounds[3] = Math.max(bounds[3], cell.getZ());
      }
   }

   private int wallsAround(BlockPos pos) {
      int walls = 0;
      for (Direction direction : Direction.Type.HORIZONTAL) {
         if (this.isSolidFloor(pos.offset(direction))) {
            walls++;
         }
      }
      return walls;
   }

   private boolean isPitLike(BlockPos pos) {
      BlockState state = this.state(pos);
      if (state.isAir()) {
         return state.getFluidState().isEmpty();
      }
      Block block = state.getBlock();
      return block == Blocks.WATER || block == Blocks.LAVA;
   }

   private boolean isSolidFloor(BlockPos pos) {
      BlockState state = this.state(pos);
      if (state.isAir() || state.isLiquid()) {
         return false;
      }
      return !WEAK_BLOCKS.contains(state.getBlock());
   }

   private BlockState state(BlockPos pos) {
      WorldChunk chunk = this.currentChunk;
      if (chunk != null) {
         int chunkStartX = chunk.getPos().getStartX();
         int chunkStartZ = chunk.getPos().getStartZ();
         int x = pos.getX();
         int z = pos.getZ();
         if (x >= chunkStartX && x < chunkStartX + 16 && z >= chunkStartZ && z < chunkStartZ + 16) {
            int sectionIndex = (pos.getY() - chunk.getBottomY()) >> 4;
            ChunkSection[] sections = chunk.getSectionArray();
            if (sectionIndex >= 0 && sectionIndex < sections.length) {
               ChunkSection section = sections[sectionIndex];
               if (section != null && !section.isEmpty()) {
                  return section.getBlockState(x & 15, pos.getY() & 15, z & 15);
               }
            }
            return Blocks.AIR.getDefaultState();
         }
      }
      return mc.world == null ? Blocks.AIR.getDefaultState() : mc.world.getBlockState(pos);
   }

   private void addIfClear(List<Hole> found, Box box, long chunkKey) {
      int chunkX = ChunkPos.getPackedX(chunkKey);
      int chunkZ = ChunkPos.getPackedZ(chunkKey);
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            List<Hole> holes = this.chunkHoles.get(ChunkPos.toLong(chunkX + dx, chunkZ + dz));
            if (holes == null) {
               continue;
            }
            for (Hole hole : holes) {
               if (hole.box.intersects(box)) {
                  return;
               }
            }
         }
      }
      for (Hole hole : found) {
         if (hole.box.intersects(box)) {
            return;
         }
      }
      found.add(new Hole(box));
   }

   private void clear() {
      this.chunkHoles.clear();
      this.scanQueue.clear();
      this.queuedChunks.clear();
      this.scannedChunks.clear();
      this.lastScanTick.clear();
   }

   @EventInit
   public void onRender(WorldRenderEvent event) {
      if (mc.world == null || mc.player == null || this.chunkHoles.isEmpty()) {
         return;
      }

      WorldRenderer renderer = event.worldRenderer();
      int fill = this.clamp255(this.fillAlpha.get());
      int outline = this.clamp255(this.outlineAlpha.get());
      boolean doGradient = this.gradient.get();

      for (List<Hole> holes : this.chunkHoles.values()) {
         for (Hole hole : holes) {
            Box b = hole.box;
            if (doGradient) {
               this.renderGradient(renderer, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, fill);
            } else if (fill > 0) {
               renderer.drawCube(
                     new Vec3d(b.minX, b.minY, b.minZ), new Vec3d(b.maxX, b.maxY, b.maxZ),
                     ColorUtil.replAlpha(this.color.getRGB(), fill), false);
            }

            if (outline > 0) {
               WorldRenderUtil.drawSolidBoxOutline(
                     renderer, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ,
                     ColorUtil.replAlpha(this.color.getRGB(), outline), OUTLINE_HALF_THICKNESS, false);
            }
         }
      }
   }

   private void renderGradient(WorldRenderer renderer, double minX, double minY, double minZ,
         double maxX, double maxY, double maxZ, int alpha) {
      int layers = Math.max(1, Math.min(MAX_GRADIENT_LAYERS, (int) Math.ceil(maxY - minY)));
      int minAlpha = Math.max(6, Math.round(alpha * MIN_GRADIENT_ALPHA_FACTOR));
      int rgb = this.color.getRGB() & 0xFFFFFF;

      for (int i = 0; i < layers; i++) {
         double y0 = lerp(minY, maxY, (double) i / layers);
         double y1 = lerp(minY, maxY, (double) (i + 1) / layers);
         float fade = 1.0F - (float) i / Math.max(1, layers - 1);
         int a = Math.max(minAlpha, Math.round(alpha * fade));
         int argb = rgb | (a << 24);

         renderer.drawQuad(new Vec3d(minX, y0, minZ), new Vec3d(minX, y1, minZ), new Vec3d(maxX, y1, minZ), new Vec3d(maxX, y0, minZ), argb, false);
         renderer.drawQuad(new Vec3d(maxX, y0, maxZ), new Vec3d(maxX, y1, maxZ), new Vec3d(minX, y1, maxZ), new Vec3d(minX, y0, maxZ), argb, false);
         renderer.drawQuad(new Vec3d(minX, y0, minZ), new Vec3d(minX, y1, minZ), new Vec3d(minX, y1, maxZ), new Vec3d(minX, y0, maxZ), argb, false);
         renderer.drawQuad(new Vec3d(maxX, y0, minZ), new Vec3d(maxX, y1, minZ), new Vec3d(maxX, y1, maxZ), new Vec3d(maxX, y0, maxZ), argb, false);
      }

      int bottomColor = rgb | (Math.max(minAlpha, alpha) << 24);
      int topColor = rgb | (minAlpha << 24);
      renderer.drawQuad(new Vec3d(minX, maxY, minZ), new Vec3d(maxX, maxY, minZ), new Vec3d(maxX, maxY, maxZ), new Vec3d(minX, maxY, maxZ), topColor, false);
      renderer.drawQuad(new Vec3d(minX, minY, minZ), new Vec3d(minX, minY, maxZ), new Vec3d(maxX, minY, maxZ), new Vec3d(maxX, minY, minZ), bottomColor, false);
   }

   private static double lerp(double start, double end, double delta) {
      return start + (end - start) * delta;
   }

   private int clamp255(float value) {
      return Math.max(0, Math.min(255, Math.round(value)));
   }

   private static final Set<Block> WEAK_BLOCKS = Set.of(
         Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES,
         Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.CHERRY_LEAVES, Blocks.MANGROVE_LEAVES,
         Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
         Blocks.GLASS, Blocks.GLASS_PANE, Blocks.TINTED_GLASS, Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS,
         Blocks.MAGENTA_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS,
         Blocks.LIME_STAINED_GLASS, Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS,
         Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS,
         Blocks.BLUE_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS,
         Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS,
         Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS_PANE,
         Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS_PANE,
         Blocks.PINK_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
         Blocks.CYAN_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE,
         Blocks.BROWN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE,
         Blocks.BLACK_STAINED_GLASS_PANE,
         Blocks.VINE, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT, Blocks.WEEPING_VINES,
         Blocks.WEEPING_VINES_PLANT, Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT,
         Blocks.GLOW_LICHEN, Blocks.HANGING_ROOTS, Blocks.SPORE_BLOSSOM,
         Blocks.KELP, Blocks.KELP_PLANT, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS,
         Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
         Blocks.SUGAR_CANE, Blocks.DEAD_BUSH, Blocks.SWEET_BERRY_BUSH, Blocks.BAMBOO, Blocks.BAMBOO_SAPLING,
         Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL,
         Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, Blocks.BIRCH_FENCE, Blocks.JUNGLE_FENCE,
         Blocks.ACACIA_FENCE, Blocks.DARK_OAK_FENCE, Blocks.CHERRY_FENCE, Blocks.MANGROVE_FENCE,
         Blocks.BAMBOO_FENCE, Blocks.CRIMSON_FENCE, Blocks.WARPED_FENCE,
         Blocks.COBWEB);

   private static final class Hole {
      private final Box box;

      private Hole(Box box) {
         this.box = box;
      }
   }
}