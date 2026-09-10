package sl.selene.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import sl.selene.Selene;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventUpdate;
import sl.selene.event.render.WorldRenderEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.world.WorldRenderer;

@IModule(
   name = "Animations",
   description = "Tab and chunk loading animations",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Animations extends Module {
   public static final BooleanSetting tabAnimation = new BooleanSetting("Tab Animation", true);
   public static final SliderSetting tabSpeed = new SliderSetting("Tab Speed", 0.18F, 0.05F, 0.5F, 0.01F, false)
      .hidden(() -> !tabAnimation.get());
   public static final BooleanSetting chunkLoadAnimation = new BooleanSetting("Chunk Animation", true);
   public static final SliderSetting chunkDuration = new SliderSetting("Duration (s)", 1.2F, 0.4F, 3.0F, 0.1F, false)
      .hidden(() -> !chunkLoadAnimation.get());
   public static final SliderSetting chunkRadius = new SliderSetting("Effect Radius", 14.0F, 6.0F, 32.0F, 1.0F, false)
      .hidden(() -> !chunkLoadAnimation.get());

   private static float tabProgress = 1.0F;
   private ChunkPos lastChunkPos;
   private final List<ChunkPulse> chunkPulses = new ArrayList<>();

   public Animations() {
      this.addSettings(new Setting[] { tabAnimation, tabSpeed, chunkLoadAnimation, chunkDuration, chunkRadius });
   }

   @Override
   public void onDisable() {
      tabProgress = 1.0F;
      chunkPulses.clear();
      lastChunkPos = null;
   }

   @EventInit
   public void onUpdate(EventUpdate event) {
      if (!this.enable || mc.player == null) {
         return;
      }

      if (tabAnimation.get() && mc.options.playerListKey.wasPressed()) {
         resetTabProgress();
      }

      if (tabAnimation.get() && tabProgress < 1.0F) {
         tabProgress = Math.min(1.0F, tabProgress + tabSpeed.get());
      }

      if (chunkLoadAnimation.get()) {
         ChunkPos current = mc.player.getChunkPos();
         if (lastChunkPos != null && !lastChunkPos.equals(current)) {
            chunkPulses.add(new ChunkPulse(current, System.currentTimeMillis()));
         }

         lastChunkPos = current;
         long durationMs = (long) (chunkDuration.get() * 1000.0F);
         chunkPulses.removeIf(pulse -> System.currentTimeMillis() - pulse.startMs > durationMs);
      }
   }

   @EventInit
   public void onWorldRender(WorldRenderEvent event) {
      if (!this.enable || !chunkLoadAnimation.get() || mc.player == null || chunkPulses.isEmpty()) {
         return;
      }

      WorldRenderer worldRenderer = event.worldRenderer();
      long durationMs = (long) (chunkDuration.get() * 1000.0F);
      float radius = chunkRadius.get();
      Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
      int colorBase = ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 120);

      for (ChunkPulse pulse : chunkPulses) {
         float progress = (System.currentTimeMillis() - pulse.startMs) / (float) durationMs;
         if (progress >= 1.0F) {
            continue;
         }

         float alpha = 1.0F - progress;
         double centerX = pulse.pos.getCenterX();
         double centerZ = pulse.pos.getCenterZ();
         double y = mc.player.getY() + 0.05;
         int color = ColorUtil.replAlpha(colorBase, (int) (alpha * 180.0F));
         double half = radius * (0.35 + progress * 0.65);
         double minX = centerX - half - camera.x;
         double maxX = centerX + half - camera.x;
         double minZ = centerZ - half - camera.z;
         double maxZ = centerZ + half - camera.z;
         double drawY = y - camera.y;
         worldRenderer.drawLine(new Vec3d(minX, drawY, minZ), new Vec3d(maxX, drawY, minZ), 1.5, color, false);
         worldRenderer.drawLine(new Vec3d(maxX, drawY, minZ), new Vec3d(maxX, drawY, maxZ), 1.5, color, false);
         worldRenderer.drawLine(new Vec3d(maxX, drawY, maxZ), new Vec3d(minX, drawY, maxZ), 1.5, color, false);
         worldRenderer.drawLine(new Vec3d(minX, drawY, maxZ), new Vec3d(minX, drawY, minZ), 1.5, color, false);
      }
   }

   public static boolean isTabAnimationEnabled() {
      return isEnabled() && tabAnimation.get();
   }

   public static float getTabProgress() {
      return tabProgress;
   }

   public static void resetTabProgress() {
      tabProgress = 0.0F;
   }

   private static boolean isEnabled() {
      if (Selene.get == null || Selene.get.manager == null) {
         return false;
      }

      Animations module = Selene.get.manager.get(Animations.class);
      return module != null && module.enable;
   }

   private record ChunkPulse(ChunkPos pos, long startMs) {
   }
}