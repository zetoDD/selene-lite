package sl.selene.module.impl.visuals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SmokerBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.world.WorldRenderer;

@IModule(name = "StorageESP", description = "Shows chests, spawners and storage through walls", category = Category.Donut, bind = -1)
@Environment(EnvType.CLIENT)
public class StorageESP extends Module {

   public static SliderSetting range = new SliderSetting("Range", 64.0F, 16.0F, 128.0F, 8.0F, false);
   public static BooleanSetting showChest = new BooleanSetting("Chest", true);
   public static BooleanSetting showEnderChest = new BooleanSetting("Ender Chest", true);
   public static BooleanSetting showSpawner = new BooleanSetting("Spawner", true);
   public static BooleanSetting showShulker = new BooleanSetting("Shulker Box", true);
   public static BooleanSetting showFurnace = new BooleanSetting("Furnace", true);
   public static BooleanSetting showBarrel = new BooleanSetting("Barrel", true);
   public static BooleanSetting showEnchanting = new BooleanSetting("Enchanting Table", true);
   public static BooleanSetting showHopper = new BooleanSetting("Hopper", true);
   public static BooleanSetting showDispenser = new BooleanSetting("Dispenser", false);
   public static SliderSetting fillAlpha = new SliderSetting("Fill Alpha", 45.0F, 0.0F, 255.0F, 5.0F, false);

   private static final int SCAN_INTERVAL_TICKS = 5;
   private static final int FULL_RESCAN_CYCLES = 40;
   private static final int CHUNKS_PER_TICK = 2;
   private static final double CHEST_INSET = 1.0 / 16.0;

   private final Map<BlockPos, Integer> positions = new HashMap<>();
   private final Map<Long, List<BlockPos>> chunkEntries = new HashMap<>();
   private final Queue<Long> scanQueue = new ArrayDeque<>();
   private final Set<Long> queuedChunks = new HashSet<>();
   private final Set<Long> scannedChunks = new HashSet<>();
   private ClientWorld world;
   private int tickCounter;
   private int rescanCounter;

   public StorageESP() {
      this.addSettings(new Setting[] { range, showChest, showEnderChest, showSpawner, showShulker, showFurnace,
            showBarrel, showEnchanting, showHopper, showDispenser, fillAlpha });
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
      BlockPos pos = null;
      if (packet instanceof BlockEntityUpdateS2CPacket blockEntityUpdate) {
         pos = blockEntityUpdate.getPos();
      } else if (packet instanceof BlockUpdateS2CPacket blockUpdate) {
         pos = blockUpdate.getPos();
      }
      if (pos != null) {
         this.markDirty(new ChunkPos(pos).toLong());
      }
   }

   private void scanChunk(WorldChunk chunk) {
      long key = chunk.getPos().toLong();

      Map<BlockPos, Integer> found = new HashMap<>();
      for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
         BlockEntity blockEntity = entry.getValue();
         if (blockEntity != null && this.accept(blockEntity)) {
            found.put(entry.getKey().toImmutable(), this.colorFor(blockEntity));
         }
      }

      List<BlockPos> old = this.chunkEntries.put(key, new ArrayList<>(found.keySet()));
      if (old != null) {
         for (BlockPos pos : old) {
            this.positions.remove(pos);
         }
      }
      this.positions.putAll(found);
   }

   private void markDirty(long key) {
      if (this.scannedChunks.remove(key) && this.queuedChunks.add(key)) {
         this.scanQueue.add(key);
      }
   }

   private boolean accept(BlockEntity blockEntity) {
      if (blockEntity instanceof ChestBlockEntity) {
         return this.showChest.get();
      }
      if (blockEntity instanceof EnderChestBlockEntity) {
         return this.showEnderChest.get();
      }
      if (blockEntity instanceof MobSpawnerBlockEntity) {
         return this.showSpawner.get();
      }
      if (blockEntity instanceof ShulkerBoxBlockEntity) {
         return this.showShulker.get();
      }
      if (blockEntity instanceof FurnaceBlockEntity || blockEntity instanceof BlastFurnaceBlockEntity
            || blockEntity instanceof SmokerBlockEntity) {
         return this.showFurnace.get();
      }
      if (blockEntity instanceof BarrelBlockEntity) {
         return this.showBarrel.get();
      }
      if (blockEntity instanceof EnchantingTableBlockEntity) {
         return this.showEnchanting.get();
      }
      if (blockEntity instanceof HopperBlockEntity) {
         return this.showHopper.get();
      }
      if (blockEntity instanceof DispenserBlockEntity || blockEntity instanceof DropperBlockEntity) {
         return this.showDispenser.get();
      }
      return false;
   }

   private int colorFor(BlockEntity blockEntity) {
      if (blockEntity instanceof TrappedChestBlockEntity) {
         return 0xFFFF0000;
      }
      if (blockEntity instanceof ChestBlockEntity) {
         return 0xFFFFA000;
      }
      if (blockEntity instanceof EnderChestBlockEntity) {
         return 0xFF7800FF;
      }
      if (blockEntity instanceof ShulkerBoxBlockEntity) {
         return 0xFFFFA000;
      }
      if (blockEntity instanceof BarrelBlockEntity) {
         return 0xFFFFA000;
      }
      if (blockEntity instanceof FurnaceBlockEntity || blockEntity instanceof BlastFurnaceBlockEntity
            || blockEntity instanceof SmokerBlockEntity || blockEntity instanceof HopperBlockEntity
            || blockEntity instanceof DispenserBlockEntity || blockEntity instanceof DropperBlockEntity
            || blockEntity instanceof MobSpawnerBlockEntity || blockEntity instanceof EnchantingTableBlockEntity) {
         return 0xFF8C8C8C;
      }
      return 0xFF8C8C8C;
   }

   @EventInit
   public void onRender(WorldRenderEvent event) {
      if (mc.world == null || mc.player == null || this.positions.isEmpty()) {
         return;
      }

      WorldRenderer renderer = event.worldRenderer();
      int fill = this.clamp255(this.fillAlpha.get());
      double reach = this.range.get();
      double reachSq = reach * reach;
      Set<BlockPos> drawn = new HashSet<>();

      for (Map.Entry<BlockPos, Integer> entry : this.positions.entrySet()) {
         BlockPos pos = entry.getKey();
         if (drawn.contains(pos)) {
            continue;
         }
         double distX = pos.getX() + 0.5 - mc.player.getX();
         double distZ = pos.getZ() + 0.5 - mc.player.getZ();
         if (distX * distX + distZ * distZ > reachSq) {
            continue;
         }

         int color = entry.getValue();
         double minX = pos.getX();
         double minY = pos.getY();
         double minZ = pos.getZ();
         double maxX = minX + 1.0;
         double maxY = minY + 1.0;
         double maxZ = minZ + 1.0;

         BlockEntity blockEntity = mc.world.getBlockEntity(pos);
         if (blockEntity instanceof ChestBlockEntity chest) {
            BlockState state = chest.getCachedState();
            ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
            if (chestType != ChestType.SINGLE) {
               for (Direction direction : Direction.Type.HORIZONTAL) {
                  BlockPos other = pos.offset(direction);
                  if (!this.positions.containsKey(other)) {
                     continue;
                  }
                  BlockEntity otherEntity = mc.world.getBlockEntity(other);
                  if (otherEntity instanceof ChestBlockEntity otherChest
                        && otherChest.getClass() == chest.getClass()
                        && otherChest.getCachedState().get(ChestBlock.CHEST_TYPE) == chestType.getOpposite()) {
                     drawn.add(other);
                     minX = Math.min(minX, other.getX());
                     minZ = Math.min(minZ, other.getZ());
                     maxX = Math.max(maxX, other.getX() + 1.0);
                     maxZ = Math.max(maxZ, other.getZ() + 1.0);
                     break;
                  }
               }
            }
         }

         if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity) {
            double inset = CHEST_INSET;
            minX += inset;
            minZ += inset;
            maxX -= inset;
            maxY -= inset * 2.0;
            maxZ -= inset;
         }

         if (fill > 0) {
            renderer.drawCube(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ),
                  ColorUtil.replAlpha(color, fill), false);
         }
      }
   }

   private int clamp255(float value) {
      return Math.max(0, Math.min(255, Math.round(value)));
   }

   private void clearAll() {
      this.positions.clear();
      this.chunkEntries.clear();
      this.scanQueue.clear();
      this.queuedChunks.clear();
      this.scannedChunks.clear();
   }
}