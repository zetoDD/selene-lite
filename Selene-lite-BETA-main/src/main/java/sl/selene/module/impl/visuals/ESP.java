package sl.selene.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2d;
import sl.selene.Selene;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventScreen;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.HueSetting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.MultiBooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.world.WorldProjection;

@IModule(name = "ESP", description = "2d boxes with health labels around entities", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ESP extends Module {

   private static final int MAX_ENTITIES_PER_FRAME = 128;
   private static final Identifier HEART_TEXTURE = Identifier.ofVanilla("hud/heart/full");
   private static final float LABEL_GAP = 12.0F;
      private static final float LABEL_FONT_SIZE = 9.0F;
   private static final int HEART_SIZE = 9;
   private static final int ITEM_SIZE = 16;

   public static MultiBooleanSetting targets = new MultiBooleanSetting("Show",
         new BooleanSetting("Players", true),
         new BooleanSetting("Friends", true),
         new BooleanSetting("Crystals", true),
         new BooleanSetting("Monsters", false),
         new BooleanSetting("Creatures", false),
         new BooleanSetting("Ambients", false),
         new BooleanSetting("Others", false));
   public static BooleanSetting outline = new BooleanSetting("Outline", true);
   public static ModeSetting colorMode = new ModeSetting("Color Mode", "Custom", "Custom", "Sync");
   public static BooleanSetting healthText = new BooleanSetting("Health Text", true);
   public static SliderSetting thickness = new SliderSetting("Thickness", 1.0F, 0.5F, 3.0F, 0.5F, false);
   public static SliderSetting range = new SliderSetting("Range", 96.0F, 16.0F, 256.0F, 8.0F, false);
   public static SliderSetting rounding = new SliderSetting("Rounding", 6.0F, 0.0F, 12.0F, 1.0F, false);

   public static HueSetting playerColor = new HueSetting("Player Color", 10.1F, 1.0F, 1.0F);
   public static HueSetting friendColor = new HueSetting("Friend Color", 32.0F, 1.0F, 1.0F);
   public static HueSetting crystalColor = new HueSetting("Crystal Color", 57.7F, 1.0F, 1.0F);
   public static HueSetting monsterColor = new HueSetting("Monster Color", 0.0F, 1.0F, 1.0F);
   public static HueSetting creatureColor = new HueSetting("Creature Color", 58.9F, 0.04F, 0.65F);
   public static HueSetting ambientColor = new HueSetting("Ambient Color", 79.2F, 1.0F, 1.0F);
   public static HueSetting otherColor = new HueSetting("Other Color", 99.2F, 1.0F, 1.0F);

   private final List<Entity> candidates = new ArrayList<>();

   public ESP() {
      this.addSettings(new Setting[] { targets, outline, colorMode, healthText, thickness, range, rounding,
            playerColor, friendColor, crystalColor, monsterColor, creatureColor, ambientColor, otherColor });
   }

   @EventInit
   public void onRender(EventScreen event) {
      if (mc.world == null || mc.player == null) {
         return;
      }

      Renderer2D r2 = event.renderer();
      if (r2 == null) {
         return;
      }

      float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      float scaleFactor = mc.getWindow().getScaleFactor();

      this.collectCandidates(cameraPos, tickDelta);

      int limit = Math.min(this.candidates.size(), MAX_ENTITIES_PER_FRAME);
      for (int i = 0; i < limit; i++) {
         this.renderBox(r2, this.candidates.get(i), tickDelta, event.drawContext(), scaleFactor);
      }

      this.candidates.clear();
   }

   private void collectCandidates(Vec3d cameraPos, float tickDelta) {
      this.candidates.clear();
      double reach = this.range.get();
      Box searchBox = mc.player.getBoundingBox().expand(reach);
      double reachSq = reach * reach;

      for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox, this::shouldRender)) {
         if (cameraPos.squaredDistanceTo(lerpedPos(entity, tickDelta)) <= reachSq) {
            this.candidates.add(entity);
         }
      }

      this.candidates.sort((a, b) -> Double.compare(
            cameraPos.squaredDistanceTo(lerpedPos(a, tickDelta)),
            cameraPos.squaredDistanceTo(lerpedPos(b, tickDelta))));
   }

   private boolean shouldRender(Entity entity) {
      if (entity == null || entity == mc.player || !entity.isAlive()) {
         return false;
      }

      if (entity instanceof PlayerEntity player) {
         if (this.isFriend(player)) {
            return this.targets.get("Friends");
         }
         return this.targets.get("Players");
      }

      if (entity instanceof EndCrystalEntity) {
         return this.targets.get("Crystals");
      }

      if (entity instanceof HostileEntity) {
         return this.targets.get("Monsters");
      }

      if (entity instanceof AnimalEntity) {
         return this.targets.get("Creatures");
      }

      if (entity instanceof AmbientEntity) {
         return this.targets.get("Ambients");
      }

      return entity instanceof LivingEntity && this.targets.get("Others");
   }

   private boolean isFriend(PlayerEntity player) {
      return Selene.get != null
            && Selene.get.friendManager != null
            && Selene.get.friendManager.isFriend(player.getName().getString());
   }

   private static Vec3d lerpedPos(Entity entity, float tickDelta) {
      return new Vec3d(
            entity.lastRenderX + (entity.getX() - entity.lastRenderX) * tickDelta,
            entity.lastRenderY + (entity.getY() - entity.lastRenderY) * tickDelta,
            entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * tickDelta);
   }

   private void renderBox(Renderer2D r2, Entity entity, float tickDelta, DrawContext context, float scaleFactor) {
      Vec3d pos = lerpedPos(entity, tickDelta);
      Box boundingBox = entity.getBoundingBox();

      double minX = boundingBox.minX - entity.getX() + pos.x;
      double minY = boundingBox.minY - entity.getY() + pos.y;
      double minZ = boundingBox.minZ - entity.getZ() + pos.z;
      double maxX = boundingBox.maxX - entity.getX() + pos.x;
      double maxY = boundingBox.maxY - entity.getY() + pos.y;
      double maxZ = boundingBox.maxZ - entity.getZ() + pos.z;

      double[] xs = new double[8];
      double[] ys = new double[8];
      int count = 0;

      double[] cornersX = { minX, minX, minX, minX, maxX, maxX, maxX, maxX };
      double[] cornersY = { minY, minY, maxY, maxY, minY, minY, maxY, maxY };
      double[] cornersZ = { minZ, maxZ, minZ, maxZ, minZ, maxZ, minZ, maxZ };

      for (int i = 0; i < 8; i++) {
         Vector2d projected = WorldProjection.project(cornersX[i], cornersY[i], cornersZ[i]);
         if (projected != null) {
            xs[count] = projected.x;
            ys[count] = projected.y;
            count++;
         }
      }

      if (count == 0) {
         return;
      }

      float minScaledX = Float.MAX_VALUE;
      float minScaledY = Float.MAX_VALUE;
      float maxScaledX = -Float.MAX_VALUE;
      float maxScaledY = -Float.MAX_VALUE;

      for (int i = 0; i < count; i++) {
         minScaledX = (float) Math.min(minScaledX, xs[i]);
         minScaledY = (float) Math.min(minScaledY, ys[i]);
         maxScaledX = (float) Math.max(maxScaledX, xs[i]);
         maxScaledY = (float) Math.max(maxScaledY, ys[i]);
      }

      float boxMinX = WorldProjection.toFramebufferX(minScaledX);
      float boxMinY = WorldProjection.toFramebufferY(minScaledY);
      float boxMaxX = WorldProjection.toFramebufferX(maxScaledX);
      float boxMaxY = WorldProjection.toFramebufferY(maxScaledY);

      this.drawBox(r2, boxMinX, boxMinY, boxMaxX, boxMaxY, this.resolveColor(this.typeColor(entity)));

      if (this.healthText.get() && entity instanceof LivingEntity living) {
         this.drawHealthLabel(r2, context, boxMinX, boxMinY, boxMaxX, scaleFactor, living);
      }
   }

   private int typeColor(Entity entity) {
      if (entity instanceof PlayerEntity player) {
         if (this.isFriend(player)) {
            return this.friendColor.getRGB();
         }
         return this.playerColor.getRGB();
      }
      if (entity instanceof EndCrystalEntity) {
         return this.crystalColor.getRGB();
      }
      if (entity instanceof HostileEntity) {
         return this.monsterColor.getRGB();
      }
      if (entity instanceof AnimalEntity) {
         return this.creatureColor.getRGB();
      }
      if (entity instanceof AmbientEntity) {
         return this.ambientColor.getRGB();
      }
      return this.otherColor.getRGB();
   }

   private int resolveColor(int customColor) {
      return this.colorMode.is("Sync") ? Renderer2D.ColorUtil.getClientColor() : customColor;
   }

   private void drawBox(Renderer2D r2, float minX, float minY, float maxX, float maxY, int color) {
      float t = Math.max(1.0F, this.thickness.get());
      float w = maxX - minX;
      float h = maxY - minY;
      float radius = Math.min(this.rounding.get(), Math.min(w, h) * 0.4F);

      if (this.outline.get()) {
         r2.rectOutline(minX, minY, w, h, radius, 0xFF000000, t + 2.0F);
      }
      r2.rectOutline(minX, minY, w, h, radius, color, t);
   }

   private void drawHealthLabel(
         Renderer2D r2, DrawContext context, float boxMinX, float boxMinY, float boxMaxX,
         float scaleFactor, LivingEntity living) {
      float hp = living.getHealth();
      float maxHp = living.getMaxHealth();
      if (maxHp <= 0.0F) {
         return;
      }
      if (hp > maxHp) {
         hp = maxHp;
      }
      String value = Integer.toString(Math.round(hp / 5.0F) * 5);

      float centerFbX = (boxMinX + boxMaxX) * 0.5F;
      float labelTopFbY = boxMinY - LABEL_GAP * scaleFactor;
      float fontSize = LABEL_FONT_SIZE * scaleFactor;

      int centerSX = Math.round(centerFbX / scaleFactor);
      int topSY = Math.round(labelTopFbY / scaleFactor);

      ItemStack held = living.getMainHandStack();
      boolean hasItem = held != null && !held.isEmpty();

      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_TEXTURE, centerSX - 22, topSY, HEART_SIZE, HEART_SIZE);

      float textCenterSX;
      if (hasItem) {
         context.drawItem(held, centerSX + 7, topSY - 3);
         textCenterSX = centerSX - 3.0F;
      } else {
         textCenterSX = centerSX + 4.0F;
      }

      float textFbX = textCenterSX * scaleFactor;
      float baselineFbY = labelTopFbY + 7.5F * scaleFactor;
      r2.text(FontRegistry.INTER_MEDIUM, textFbX + 1.0F, baselineFbY + 1.0F, fontSize, value, 0xCC000000, "c");
      r2.text(FontRegistry.INTER_MEDIUM, textFbX, baselineFbY, fontSize, value, 0xFFFFFFFF, "c");
   }
}