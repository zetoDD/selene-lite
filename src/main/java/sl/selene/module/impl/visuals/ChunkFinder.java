package sl.selene.module.impl.visuals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
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
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.world.WorldRenderUtil;
import sl.selene.util.render.world.WorldRenderer;

@IModule(name = "ChunkFinder", description = "Highlights chunks with amethyst, mined amethyst or hidden tunnels", category = Category.Donut, bind = -1)
@Environment(EnvType.CLIENT)
public class ChunkFinder extends Module {

   public static SliderSetting range = new SliderSetting("Range", 64.0F, 16.0F, 128.0F, 8.0F, false);
   public static BooleanSetting showAmethyst = new BooleanSetting("Amethyst", true);
   public static BooleanSetting showMined = new BooleanSetting("Mined Amethyst", true);
   public static BooleanSetting showTunnels = new BooleanSetting("Tunnels", true);
   public static BooleanSetting showKelp = new BooleanSetting("Kelp", true);
   public static ModeSetting boxMode = new ModeSetting("Box Mode", "Both", "Fill", "Outline", "Both");
   public static SliderSetting fillAlpha = new SliderSetting("Fill Alpha", 45.0F, 0.0F, 255.0F, 5.0F, false);
   public static SliderSetting outlineAlpha = new SliderSetting("Outline Alpha", 180.0F, 0.0F, 255.0F, 5.0F, false);
   public static SliderSetting minTunnelLen = new SliderSetting("Min Tunnel Length", 6.0F, 3.0F, 20.0F, 1.0F, false);
   public static SliderSetting minKelpHeight = new SliderSetting("Min Kelp Height", 8.0F, 2.0F, 32.0F, 1.0F, false);
   public static SliderSetting minedThreshold = new SliderSetting("Mined Threshold", 3.0F, 0.0F, 8.0F, 1.0F, false);
   public static SliderSetting rescanInterval = new SliderSetting("Rescan Interval", 300.0F, 0.0F, 600.0F, 15.0F, false);
   public static HueSetting amethystColor = new HueSetting("Amethyst Color", 73.0F, 0.85F, 1.0F);
   public static HueSetting minedColor = new HueSetting("Mined Color", 6.0F, 0.9F, 1.0F);
   public static HueSetting tunnelColor = new HueSetting("Tunnel Color", 35.0F, 0.9F, 1.0F);
   public static HueSetting kelpColor = new HueSetting("Kelp Color", 45.0F, 0.9F, 1.0F);

   private static final int CHUNKS_PER_TICK = 2;
   private static final float OUTLINE_HALF_THICKNESS = 0.04F;
   private static final double BOX_INSET = 0.02;
   private static final int TUNNEL_SAMPLES = 8;
   private static final int TUNNEL_MIN_MISMATCH = 3;
   private static final int RESCAN_COOLDOWN_TICKS = 40;
   private static final int MIN_ROCK_RUN = 10;
   private static final int MAX_ISOLATED_CELLS = 8;
   private static final int MAX_VISIBLE_CHUNKS = 2;
   private static final int MAX_ISOLATED_GRAVEL = 4;

   private static final Set<Block> NEVER_RUN = Set.of(
         Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN,
         Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.TUFF,
         Blocks.REINFORCED_DEEPSLATE, Blocks.POINTED_DRIPSTONE,
         Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.AMETHYST_CLUSTER,
         Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD,
         Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
         Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG,
         Blocks.CRIMSON_STEM, Blocks.WARPED_STEM,
         Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG,
         Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG,
         Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_CHERRY_LOG,
         Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM,
         Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES,
         Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.CHERRY_LEAVES,
         Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
         Blocks.MUSHROOM_STEM, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK,
         Blocks.BAMBOO, Blocks.BAMBOO_SAPLING, Blocks.CACTUS, Blocks.SUGAR_CANE,
         Blocks.KELP, Blocks.KELP_PLANT, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS,
         Blocks.VINE, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT, Blocks.WEEPING_VINES,
         Blocks.WEEPING_VINES_PLANT, Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT,
         Blocks.CHORUS_PLANT, Blocks.CHORUS_FLOWER, Blocks.POWDER_SNOW,
         Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.GOLD_ORE,
         Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
         Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_COPPER_ORE,
         Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
         Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
         Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE,
         Blocks.RAW_IRON_BLOCK, Blocks.RAW_COPPER_BLOCK, Blocks.RAW_GOLD_BLOCK);

   private static final Set<Block> ROCK = Set.of(
         Blocks.STONE, Blocks.DEEPSLATE, Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK,
         Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.SANDSTONE, Blocks.RED_SANDSTONE,
         Blocks.END_STONE, Blocks.NETHERRACK, Blocks.BLACKSTONE);

   private static final Set<Block> NATURAL_NON_ROCK = Set.of(
         Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.GRASS_BLOCK,
         Blocks.PODZOL, Blocks.MYCELIUM, Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL,
         Blocks.CLAY, Blocks.MOSS_BLOCK, Blocks.SCULK, Blocks.SCULK_CATALYST,
         Blocks.SCULK_SHRIEKER, Blocks.SCULK_SENSOR, Blocks.SCULK_VEIN,
         Blocks.MUD, Blocks.PACKED_MUD, Blocks.SNOW_BLOCK, Blocks.ICE,
         Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.MAGMA_BLOCK, Blocks.GLOWSTONE,
         Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.SPONGE, Blocks.WET_SPONGE,
         Blocks.RAW_IRON_BLOCK, Blocks.RAW_COPPER_BLOCK, Blocks.RAW_GOLD_BLOCK,
         Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA,
         Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA,
         Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
         Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA,
         Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA,
         Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA,
         Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS,
         Blocks.CHISELED_STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS,
         Blocks.INFESTED_MOSSY_STONE_BRICKS, Blocks.INFESTED_CRACKED_STONE_BRICKS,
         Blocks.INFESTED_CHISELED_STONE_BRICKS, Blocks.INFESTED_STONE, Blocks.INFESTED_COBBLESTONE,
         Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES,
         Blocks.CRACKED_DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE,
         Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN,
         Blocks.CRIMSON_NYLIUM, Blocks.WARPED_NYLIUM, Blocks.NETHER_WART_BLOCK,
         Blocks.WARPED_WART_BLOCK, Blocks.SHROOMLIGHT);

   private static final Set<Block> ROCK_RING_NEUTRAL = Set.of(
         Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.GOLD_ORE,
         Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
         Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_COPPER_ORE,
         Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
         Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
         Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE,
         Blocks.RAW_IRON_BLOCK, Blocks.RAW_COPPER_BLOCK, Blocks.RAW_GOLD_BLOCK,
         Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.TUFF,
         Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK, Blocks.POINTED_DRIPSTONE,
         Blocks.SCULK, Blocks.SCULK_VEIN, Blocks.SCULK_CATALYST, Blocks.SCULK_SHRIEKER, Blocks.SCULK_SENSOR,
         Blocks.MOSS_BLOCK, Blocks.CLAY, Blocks.MUD, Blocks.PACKED_MUD,
         Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.GRASS_BLOCK,
         Blocks.PODZOL, Blocks.MYCELIUM, Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL,
         Blocks.SANDSTONE, Blocks.RED_SANDSTONE,
         Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW,
         Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.AMETHYST_CLUSTER,
         Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD,
         Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.REINFORCED_DEEPSLATE,
         Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.MAGMA_BLOCK, Blocks.GLOWSTONE,
         Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.SOUL_SAND, Blocks.SOUL_SOIL,
         Blocks.END_STONE);

   private final Map<Long, List<Finding>> chunkFindings = new HashMap<>();
   private final Queue<Long> scanQueue = new ArrayDeque<>();
   private final Set<Long> queuedChunks = new HashSet<>();
   private final Set<Long> scannedChunks = new HashSet<>();
   private final Map<Long, Integer> lastScanTick = new HashMap<>();
   private ClientWorld world;
   private int tickCounter;
   private int tick;
   private String lastSettings = "";

   public ChunkFinder() {
      this.addSettings(new Setting[] { range, showAmethyst, showMined, showTunnels, showKelp, boxMode, fillAlpha,
            outlineAlpha, minTunnelLen, minKelpHeight, minedThreshold, rescanInterval, amethystColor, minedColor,
            tunnelColor, kelpColor });
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

      String settings = this.settingsSnapshot();
      if (!settings.equals(this.lastSettings)) {
         this.lastSettings = settings;
         this.scannedChunks.clear();
      }

      float rescanSeconds = this.rescanInterval.get();
      if (rescanSeconds > 0.0F && ++this.tickCounter >= Math.max(20, Math.round(rescanSeconds * 20.0F))) {
         this.tickCounter = 0;
         this.scannedChunks.clear();
      }

      int radius = Math.max(1, Math.min(8, Math.round(this.range.get()) / 16));
      int playerChunkX = mc.player.getChunkPos().x;
      int playerChunkZ = mc.player.getChunkPos().z;
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

      this.chunkFindings.keySet().removeIf(key -> !active.contains(key) && !this.scanQueue.contains(key));
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

   private void scanChunk(WorldChunk chunk) {
      List<Finding> found = new ArrayList<>();
      if (this.showAmethyst.get() || this.showMined.get()) {
         this.scanAmethyst(chunk, found);
      }
      if (this.showTunnels.get()) {
         this.scanTunnels(chunk, found);
      }
      if (this.showKelp.get()) {
         this.scanKelp(chunk, found);
      }

      long key = chunk.getPos().toLong();
      this.chunkFindings.remove(key);
      if (!found.isEmpty()) {
         this.chunkFindings.put(key, found);
      }
   }

   private void scanAmethyst(WorldChunk chunk, List<Finding> found) {
      ChunkSection[] sections = chunk.getSectionArray();
      int baseY = chunk.getBottomY();
      int startX = chunk.getPos().getStartX();
      int startZ = chunk.getPos().getStartZ();
      int amethystBlocks = 0;
      int budding = 0;
      int clusters = 0;
      int minY = Integer.MAX_VALUE;
      int maxY = Integer.MIN_VALUE;
      int minX = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxZ = Integer.MIN_VALUE;

      for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
         ChunkSection section = sections[sectionIndex];
         if (section == null || section.isEmpty()) {
            continue;
         }
         int sectionBaseY = baseY + sectionIndex * 16;
         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               for (int y = 0; y < 16; y++) {
                  Block block = section.getBlockState(x, y, z).getBlock();
                  if (block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST
                        || block == Blocks.AMETHYST_CLUSTER || block == Blocks.LARGE_AMETHYST_BUD
                        || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.SMALL_AMETHYST_BUD) {
                     if (block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST) {
                        amethystBlocks++;
                        if (block == Blocks.BUDDING_AMETHYST) {
                           budding++;
                        }
                     } else {
                        clusters++;
                     }
                     int worldX = startX + x;
                     int worldY = sectionBaseY + y;
                     int worldZ = startZ + z;
                     if (worldX < minX) {
                        minX = worldX;
                     }
                     if (worldX > maxX) {
                        maxX = worldX;
                     }
                     if (worldY < minY) {
                        minY = worldY;
                     }
                     if (worldY > maxY) {
                        maxY = worldY;
                     }
                     if (worldZ < minZ) {
                        minZ = worldZ;
                     }
                     if (worldZ > maxZ) {
                        maxZ = worldZ;
                     }
                  }
               }
            }
         }
      }

      if (amethystBlocks + clusters < 1) {
         return;
      }
      if (clusters >= Math.round(this.minedThreshold.get())
            || this.neighbourHoldsCrystals(chunk, minY, maxY)) {
         found.add(new Finding(Kind.AMETHYST, minY, maxY, minX, maxX, minZ, maxZ));
      } else {
         found.add(new Finding(Kind.AMETHYST_MINED, minY, maxY, minX, maxX, minZ, maxZ));
      }
   }

   private boolean neighbourHoldsCrystals(WorldChunk chunk, int minY, int maxY) {
      int threshold = Math.round(this.minedThreshold.get());
      if (threshold <= 0) {
         return true;
      }
      ChunkPos pos = chunk.getPos();
      int sectionMin = Math.max(0, ((minY - 16) - chunk.getBottomY()) >> 4);
      int sectionMax = Math.min(chunk.getSectionArray().length - 1, ((maxY + 16) - chunk.getBottomY()) >> 4);
      for (Direction direction : Direction.Type.HORIZONTAL) {
         WorldChunk neighbour = mc.world.getChunkManager().getWorldChunk(
               pos.x + direction.getOffsetX(), pos.z + direction.getOffsetZ(), false);
         if (neighbour == null) {
            continue;
         }
         ChunkSection[] sections = neighbour.getSectionArray();
         for (int sectionIndex = sectionMin; sectionIndex <= sectionMax; sectionIndex++) {
            ChunkSection section = sections[sectionIndex];
            if (section == null || section.isEmpty()) {
               continue;
            }
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = 0; y < 16; y++) {
                     Block block = section.getBlockState(x, y, z).getBlock();
                     if (block == Blocks.AMETHYST_CLUSTER || block == Blocks.LARGE_AMETHYST_BUD
                           || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.SMALL_AMETHYST_BUD) {
                        if (--threshold <= 0) {
                           return true;
                        }
                     }
                  }
               }
            }
         }
      }
      return false;
   }

   private void scanKelp(WorldChunk chunk, List<Finding> found) {
      int chunkX = chunk.getPos().getStartX();
      int chunkZ = chunk.getPos().getStartZ();
      int minHeight = Math.round(this.minKelpHeight.get());
      int worldBottom = mc.world.getBottomY();
      int worldTop = worldBottom + mc.world.getHeight();
      for (int lx = 0; lx < 16; lx++) {
         for (int lz = 0; lz < 16; lz++) {
            int x = chunkX + lx;
            int z = chunkZ + lz;
            for (int y = worldTop - 1; y >= worldBottom; y--) {
               Block block = this.blockAt(chunk, x, y, z);
               if (block != Blocks.KELP && block != Blocks.KELP_PLANT) {
                  continue;
               }
               int bottom = y;
               while (bottom > worldBottom) {
                  Block below = this.blockAt(chunk, x, bottom - 1, z);
                  if (below != Blocks.KELP && below != Blocks.KELP_PLANT) {
                     break;
                  }
                  bottom--;
               }
               boolean fullyGrown = this.blockAt(chunk, x, y + 1, z) == Blocks.AIR;
               if (y - bottom + 1 >= minHeight && fullyGrown) {
                  found.add(new Finding(Kind.KELP, y, y, chunkX, chunkX + 15, chunkZ, chunkZ + 15));
               }
               y = bottom;
            }
         }
      }
   }

   private void scanTunnels(WorldChunk chunk, List<Finding> found) {
      int chunkX = chunk.getPos().getStartX();
      int chunkZ = chunk.getPos().getStartZ();
      int minRun = Math.round(this.minTunnelLen.get());
      int worldBottom = mc.world.getBottomY();
      int worldTop = worldBottom + mc.world.getHeight();
      List<int[]> runs = new ArrayList<>();
      for (int lx = 0; lx < 16; lx++) {
         for (int lz = 0; lz < 16; lz++) {
            int x = chunkX + lx;
            int z = chunkZ + lz;
            int runStart = -1;
            Block runBlock = null;
            for (int y = worldTop - 1; y >= worldBottom; y--) {
               Block block = this.blockAt(chunk, x, y, z);
               if (block == runBlock) {
                  continue;
               }
               if (runBlock != null && runStart - y >= minRun && this.isTunnelRun(x, y + 1, runStart, z, runBlock)) {
                  this.mergeRun(runs, y + 1, runStart);
               }
               runBlock = block;
               runStart = y;
            }
            if (runBlock != null && runStart - worldBottom + 1 >= minRun
                  && this.isTunnelRun(x, worldBottom, runStart, z, runBlock)) {
               this.mergeRun(runs, worldBottom, runStart);
            }
         }
      }

      for (int[] run : runs) {
         found.add(new Finding(Kind.TUNNEL, run[0], run[1], chunkX, chunkX + 15, chunkZ, chunkZ + 15));
      }
   }

   private void mergeRun(List<int[]> runs, int minY, int maxY) {
      for (int[] run : runs) {
         if (Math.max(run[0], minY) <= Math.min(run[1], maxY) + 3) {
            if (minY < run[0]) {
               run[0] = minY;
            }
            if (maxY > run[1]) {
               run[1] = maxY;
            }
            return;
         }
      }
      runs.add(new int[] { minY, maxY });
   }

   private boolean isTunnelRun(int x, int minY, int maxY, int z, Block block) {
      BlockState sample = block.getDefaultState();
      if (sample.isAir() || sample.isLiquid() || NEVER_RUN.contains(block)) {
         return false;
      }
      int runLen = Math.max(1, maxY - minY + 1);

      if (!ROCK.contains(block) && !NATURAL_NON_ROCK.contains(block)) {
         return true;
      }
      if (ROCK.contains(block) && runLen < Math.max(Math.round(this.minTunnelLen.get()), MIN_ROCK_RUN)) {
         return false;
      }

      int step = Math.max(1, (runLen + TUNNEL_SAMPLES - 1) / TUNNEL_SAMPLES);
      int mismatch = 0;
      for (int i = 0; i < TUNNEL_SAMPLES; i++) {
         int y = minY + Math.min(runLen - 1, i * step);
         if (this.standsOut(x, y, z, block)) {
            mismatch++;
         }
      }
      return mismatch >= TUNNEL_MIN_MISMATCH;
   }

   private boolean standsOut(int x, int y, int z, Block block) {
      if (ROCK.contains(block)) {
         return this.rockStandsOut(x, y, z, block);
      }
      return this.isolatedColumn(x, y, z, block);
   }

   private boolean rockStandsOut(int x, int y, int z, Block block) {
      int same = 0;
      int other = 0;
      int solid = 0;
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
               continue;
            }
            BlockState state = mc.world.getBlockState(new BlockPos(x + dx, y, z + dz));
            if (state.isAir() || state.isLiquid()) {
               continue;
            }
            solid++;
            Block neighbour = state.getBlock();
            if (neighbour == block) {
               same++;
               continue;
            }
            if (ROCK_RING_NEUTRAL.contains(neighbour)) {
               continue;
            }
            if (block == Blocks.STONE && neighbour == Blocks.DEEPSLATE) {
               other++;
            } else if (block == Blocks.DEEPSLATE && neighbour == Blocks.STONE) {
               other++;
            } else if (block != Blocks.STONE && block != Blocks.DEEPSLATE
                  && (neighbour == Blocks.STONE || neighbour == Blocks.DEEPSLATE)) {
               other++;
            } else {
               other++;
            }
         }
      }
      return solid >= 8 && other > same;
   }

   private boolean isolatedColumn(int x, int y, int z, Block block) {
      int solid = 0;
      int same = 0;
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
               continue;
            }
            BlockState state = mc.world.getBlockState(new BlockPos(x + dx, y, z + dz));
            if (state.isAir() || state.isLiquid()) {
               continue;
            }
            solid++;
            if (state.getBlock() == block) {
               same++;
            }
         }
      }
      for (int dy = -1; dy <= 1; dy += 2) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               BlockState state = mc.world.getBlockState(new BlockPos(x + dx, y + dy, z + dz));
               if (state.getBlock() == block) {
                  same++;
               }
            }
         }
      }
      int limit = block == Blocks.GRAVEL ? MAX_ISOLATED_GRAVEL : MAX_ISOLATED_CELLS;
      return solid >= 8 && same <= limit;
   }

   private Block blockAt(WorldChunk chunk, int x, int y, int z) {
      int sectionIndex = (y - chunk.getBottomY()) >> 4;
      ChunkSection[] sections = chunk.getSectionArray();
      if (sectionIndex < 0 || sectionIndex >= sections.length) {
         return Blocks.AIR;
      }
      ChunkSection section = sections[sectionIndex];
      if (section == null || section.isEmpty()) {
         return Blocks.AIR;
      }
      return section.getBlockState(x & 15, y & 15, z & 15).getBlock();
   }

   @EventInit
   public void onRender(WorldRenderEvent event) {
      if (mc.world == null || mc.player == null || this.chunkFindings.isEmpty()) {
         return;
      }

      WorldRenderer renderer = event.worldRenderer();
      int fill = this.clamp255(this.fillAlpha.get());
      int outline = this.clamp255(this.outlineAlpha.get());
      boolean doFill = fill > 0 && (this.boxMode.is("Fill") || this.boxMode.is("Both"));
      boolean doOutline = outline > 0 && (this.boxMode.is("Outline") || this.boxMode.is("Both"));
      if (!doFill && !doOutline) {
         return;
      }

      double playerX = mc.player.getX();
      double playerZ = mc.player.getZ();
      Map<Long, RenderChunk> chunks = new HashMap<>();
      for (Map.Entry<Long, List<Finding>> entry : this.chunkFindings.entrySet()) {
         long key = entry.getKey();
         int chunkStartX = ChunkPos.getPackedX(key) * 16;
         int chunkStartZ = ChunkPos.getPackedZ(key) * 16;
         List<Finding> visible = new ArrayList<>();
         int bestPriority = Integer.MAX_VALUE;
         for (Finding finding : entry.getValue()) {
            if (this.isVisible(finding.kind)) {
               visible.add(finding);
               bestPriority = Math.min(bestPriority, this.priority(finding.kind));
            }
         }
         if (visible.isEmpty()) {
            continue;
         }
         double dx = chunkStartX + 8.0 - playerX;
         double dz = chunkStartZ + 8.0 - playerZ;
         chunks.put(key, new RenderChunk(chunkStartX, chunkStartZ, bestPriority, dx * dx + dz * dz, visible));
      }

      List<RenderChunk> sorted = new ArrayList<>(chunks.values());
      Comparator<RenderChunk> byPriorityThenDistance = (a, b) -> {
         int cmp = Integer.compare(a.priority, b.priority);
         return cmp != 0 ? cmp : Double.compare(a.distSq, b.distSq);
      };
      sorted.sort(byPriorityThenDistance);
      int shown = 0;
      for (RenderChunk chunk : sorted) {
         if (shown >= MAX_VISIBLE_CHUNKS) {
            break;
         }
         shown++;
         for (Finding finding : chunk.findings) {
            int color = this.colorFor(finding.kind);
            double layerY = finding.minY + (finding.maxY - finding.minY) / 2.0;
            double minX = chunk.chunkStartX + BOX_INSET;
            double minZ = chunk.chunkStartZ + BOX_INSET;
            double maxX = chunk.chunkStartX + 16 - BOX_INSET;
            double maxZ = chunk.chunkStartZ + 16 - BOX_INSET;
            if (doFill) {
               renderer.drawCube(new Vec3d(minX, layerY, minZ),
                     new Vec3d(maxX, layerY + 1, maxZ),
                     ColorUtil.replAlpha(color, fill), false);
            }
            if (doOutline) {
               WorldRenderUtil.drawSolidBoxOutline(renderer, minX, layerY, minZ,
                     maxX, layerY + 1, maxZ,
                     ColorUtil.replAlpha(color, outline), OUTLINE_HALF_THICKNESS, false);
            }
         }
      }
   }

   private int priority(Kind kind) {
      return switch (kind) {
         case TUNNEL -> 0;
         case KELP -> 1;
         case AMETHYST_MINED -> 2;
         case AMETHYST -> 3;
      };
   }

   private boolean isVisible(Kind kind) {
      return switch (kind) {
         case AMETHYST -> this.showAmethyst.get();
         case AMETHYST_MINED -> this.showMined.get();
         case TUNNEL -> this.showTunnels.get();
         case KELP -> this.showKelp.get();
      };
   }

   private int colorFor(Kind kind) {
      return switch (kind) {
         case AMETHYST -> this.amethystColor.getRGB();
         case AMETHYST_MINED -> this.minedColor.getRGB();
         case TUNNEL -> this.tunnelColor.getRGB();
         case KELP -> this.kelpColor.getRGB();
      };
   }

   private String settingsSnapshot() {
      return this.range.get() + "|" + this.showAmethyst.get() + "|" + this.showMined.get() + "|"
            + this.showTunnels.get() + "|" + this.showKelp.get() + "|" + this.minTunnelLen.get() + "|"
            + this.minKelpHeight.get() + "|" + this.minedThreshold.get() + "|" + this.rescanInterval.get();
   }

   private int clamp255(float value) {
      return Math.max(0, Math.min(255, Math.round(value)));
   }

   private void clear() {
      this.chunkFindings.clear();
      this.scanQueue.clear();
      this.queuedChunks.clear();
      this.scannedChunks.clear();
      this.lastScanTick.clear();
   }

   private enum Kind { AMETHYST, AMETHYST_MINED, TUNNEL, KELP }

   private static final class RenderChunk {
      private final int chunkStartX;
      private final int chunkStartZ;
      private final int priority;
      private final double distSq;
      private final List<Finding> findings;

      private RenderChunk(int chunkStartX, int chunkStartZ, int priority, double distSq, List<Finding> findings) {
         this.chunkStartX = chunkStartX;
         this.chunkStartZ = chunkStartZ;
         this.priority = priority;
         this.distSq = distSq;
         this.findings = findings;
      }
   }

   private static final class Finding {
      private final Kind kind;
      private final int minY;
      private final int maxY;
      private final int minX;
      private final int maxX;
      private final int minZ;
      private final int maxZ;

      private Finding(Kind kind, int minY, int maxY, int minX, int maxX, int minZ, int maxZ) {
         this.kind = kind;
         this.minY = minY;
         this.maxY = maxY;
         this.minX = minX;
         this.maxX = maxX;
         this.minZ = minZ;
         this.maxZ = maxZ;
      }
   }
}