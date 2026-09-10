package sl.selene.module.impl.visuals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.world.WorldRenderer;

@IModule(name = "AmethystESP", description = "Highlights every amethyst block, bud and cluster", category = Category.Donut, bind = -1)
@Environment(EnvType.CLIENT)
public class AmethystESP extends Module {

   public static SliderSetting range = new SliderSetting("Range", 64.0F, 16.0F, 128.0F, 8.0F, false);
   public static SliderSetting fillAlpha = new SliderSetting("Fill Alpha", 60.0F, 0.0F, 255.0F, 5.0F, false);
   public static HueSetting blockColor = new HueSetting("Amethyst Color", 73.0F, 0.85F, 1.0F);
   public static HueSetting clusterColor = new HueSetting("Cluster Color", 82.0F, 0.9F, 1.0F);

   private static final int SCAN_INTERVAL_TICKS = 5;
   private static final int FULL_RESCAN_CYCLES = 40;
   private static final int CHUNKS_PER_TICK = 2;
   private static final double BOX_INSET = 0.02;
   private static final int RESCAN_COOLDOWN_TICKS = 40;
   private static final Predicate<BlockState> AMETHYST_PREDICATE = state -> {
      Block block = state.getBlock();
      return block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST
            || block == Blocks.AMETHYST_CLUSTER || block == Blocks.LARGE_AMETHYST_BUD
            || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.SMALL_AMETHYST_BUD;
   };

   private final Map<BlockPos, Integer> positions = new HashMap<>();
   private final Map<Long, List<BlockPos>> chunkEntries = new HashMap<>();
   private final Map<Integer, List<MergedBox>> renderBoxes = new HashMap<>();
   private final Queue<Long> scanQueue = new ArrayDeque<>();
   private final Set<Long> queuedChunks = new HashSet<>();
   private final Set<Long> scannedChunks = new HashSet<>();
   private final Map<Long, Integer> lastScanTick = new HashMap<>();
   private ClientWorld world;
   private int tickCounter;
   private int rescanCounter;
   private int tick;
   private boolean boxesDirty = true;

   public AmethystESP() {
      this.addSettings(new Setting[] { range, fillAlpha, blockColor, clusterColor });
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.clearAll();
      this.world = mc.world;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.clearAll();
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.world == null || mc.player == null) {
         if (!this.positions.isEmpty()) {
            this.clearAll();
         }
         return;
      }

      this.tick++;
      if (mc.world != this.world) {
         this.world = (ClientWorld) mc.world;
         this.clearAll();
      }

      if (++this.tickCounter >= SCAN_INTERVAL_TICKS) {
         this.tickCounter = 0;
         if (++this.rescanCounter >= FULL_RESCAN_CYCLES) {
            this.rescanCounter = 0;
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

         Iterator<Map.Entry<Long, List<BlockPos>>> iterator = this.chunkEntries.entrySet().iterator();
         while (iterator.hasNext()) {
            Map.Entry<Long, List<BlockPos>> entry = iterator.next();
            if (!active.contains(entry.getKey())) {
               for (BlockPos pos : entry.getValue()) {
                  this.positions.remove(pos);
               }
               iterator.remove();
               this.boxesDirty = true;
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
      long key = chunk.getPos().toLong();
      Map<BlockPos, Integer> found = new HashMap<>();
      ChunkSection[] sections = chunk.getSectionArray();
      int baseY = chunk.getBottomY();
      int startX = chunk.getPos().getStartX();
      int startZ = chunk.getPos().getStartZ();

      for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
         ChunkSection section = sections[sectionIndex];
         if (section == null || section.isEmpty() || !section.hasAny(AMETHYST_PREDICATE)) {
            continue;
         }
         int sectionBaseY = baseY + sectionIndex * 16;
         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               for (int y = 0; y < 16; y++) {
                  Block block = section.getBlockState(x, y, z).getBlock();
                  int color = this.colorFor(block);
                  if (color != 0) {
                     found.put(new BlockPos(startX + x, sectionBaseY + y, startZ + z), color);
                  }
               }
            }
         }
      }

      List<BlockPos> old = this.chunkEntries.put(key, new ArrayList<>(found.keySet()));
      if (old != null) {
         for (BlockPos pos : old) {
            this.positions.remove(pos);
         }
      }
      this.positions.putAll(found);
      this.boxesDirty = true;
   }

   private int colorFor(Block block) {
      if (block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST) {
         return this.blockColor.getRGB();
      }
      if (block == Blocks.AMETHYST_CLUSTER || block == Blocks.LARGE_AMETHYST_BUD
            || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.SMALL_AMETHYST_BUD) {
         return this.clusterColor.getRGB();
      }
      return 0;
   }

   private void rebuildBoxes() {
      this.renderBoxes.clear();
      if (this.positions.isEmpty()) {
         this.boxesDirty = false;
         return;
      }

      List<BlockPos> sorted = new ArrayList<>(this.positions.keySet());
      sorted.sort(Comparator.comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getZ)
            .thenComparingInt(BlockPos::getY));
      Map<Integer, Set<Long>> cellsByColor = new HashMap<>();
      for (BlockPos pos : sorted) {
         cellsByColor.computeIfAbsent(this.positions.get(pos), k -> new HashSet<>()).add(pos.asLong());
      }

      for (Map.Entry<Integer, Set<Long>> entry : cellsByColor.entrySet()) {
         int color = entry.getKey();
         Set<Long> cells = entry.getValue();
         Set<Long> claimed = new HashSet<>();
         List<MergedBox> boxes = new ArrayList<>();
         for (BlockPos pos : sorted) {
            if (this.positions.get(pos) != color || claimed.contains(pos.asLong())) {
               continue;
            }
            int minX = pos.getX(), maxX = minX;
            int minY = pos.getY(), maxY = minY;
            int minZ = pos.getZ(), maxZ = minZ;
            boolean expanded;
            do {
               expanded = false;
               if (this.allFree(cells, claimed, maxX + 1, maxX + 1, minY, maxY, minZ, maxZ)) {
                  maxX++;
                  expanded = true;
               }
               if (this.allFree(cells, claimed, minX - 1, minX - 1, minY, maxY, minZ, maxZ)) {
                  minX--;
                  expanded = true;
               }
               if (this.allFree(cells, claimed, minX, maxX, maxY + 1, maxY + 1, minZ, maxZ)) {
                  maxY++;
                  expanded = true;
               }
               if (this.allFree(cells, claimed, minX, maxX, minY - 1, minY - 1, minZ, maxZ)) {
                  minY--;
                  expanded = true;
               }
               if (this.allFree(cells, claimed, minX, maxX, minY, maxY, maxZ + 1, maxZ + 1)) {
                  maxZ++;
                  expanded = true;
               }
               if (this.allFree(cells, claimed, minX, maxX, minY, maxY, minZ - 1, minZ - 1)) {
                  minZ--;
                  expanded = true;
               }
            } while (expanded);

            for (int x = minX; x <= maxX; x++) {
               for (int y = minY; y <= maxY; y++) {
                  for (int z = minZ; z <= maxZ; z++) {
                     claimed.add(BlockPos.asLong(x, y, z));
                  }
               }
            }
            boxes.add(new MergedBox(minX, minY, minZ, maxX, maxY, maxZ));
         }
         this.renderBoxes.put(color, boxes);
      }
      this.boxesDirty = false;
   }

   private boolean allFree(Set<Long> cells, Set<Long> claimed, int x0, int x1, int y0, int y1, int z0, int z1) {
      for (int x = x0; x <= x1; x++) {
         for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
               long key = BlockPos.asLong(x, y, z);
               if (!cells.contains(key) || claimed.contains(key)) {
                  return false;
               }
            }
         }
      }
      return true;
   }

   @EventInit
   public void onRender(WorldRenderEvent event) {
      if (mc.world == null || mc.player == null || this.positions.isEmpty()) {
         return;
      }
      if (this.boxesDirty) {
         this.rebuildBoxes();
      }

      WorldRenderer renderer = event.worldRenderer();
      int fill = this.clamp255(this.fillAlpha.get());
      if (fill <= 0) {
         return;
      }
      double reach = this.range.get();
      double reachSq = reach * reach;
      double px = mc.player.getX();
      double pz = mc.player.getZ();

      for (Map.Entry<Integer, List<MergedBox>> entry : this.renderBoxes.entrySet()) {
         int color = ColorUtil.replAlpha(entry.getKey(), fill);
         for (MergedBox box : entry.getValue()) {
            double cx = Math.max(box.minX, Math.min(px, box.maxX + 1));
            double cz = Math.max(box.minZ, Math.min(pz, box.maxZ + 1));
            double dx = cx - px;
            double dz = cz - pz;
            if (dx * dx + dz * dz > reachSq) {
               continue;
            }
            renderer.drawCube(new Vec3d(box.minX + BOX_INSET, box.minY + BOX_INSET, box.minZ + BOX_INSET),
                  new Vec3d(box.maxX + 1 - BOX_INSET, box.maxY + 1 - BOX_INSET, box.maxZ + 1 - BOX_INSET),
                  color, false);
         }
      }
   }

   private int clamp255(float value) {
      return Math.max(0, Math.min(255, Math.round(value)));
   }

   private void clearAll() {
      this.positions.clear();
      this.chunkEntries.clear();
      this.renderBoxes.clear();
      this.scanQueue.clear();
      this.queuedChunks.clear();
      this.scannedChunks.clear();
      this.lastScanTick.clear();
      this.boxesDirty = true;
   }

   private static final class MergedBox {
      private final int minX;
      private final int minY;
      private final int minZ;
      private final int maxX;
      private final int maxY;
      private final int maxZ;

      private MergedBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
         this.minX = minX;
         this.minY = minY;
         this.minZ = minZ;
         this.maxX = maxX;
         this.maxY = maxY;
         this.maxZ = maxZ;
      }
   }
}