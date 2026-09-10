package sl.selene.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sl.selene.event.EventInit;
import sl.selene.event.render.WorldRenderEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.MultiBooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.world.WorldRenderer;

@IModule(name = "Projectile", description = "Shows projectile trajectories", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Projectile extends Module {
   private static final int BASE_COLOR = Renderer2D.ColorUtil.getMainColor(1, 1);
   private static final double MAX_TRACK_DISTANCE_SQ = 80.0 * 80.0;
   private static final int MAX_WORLD_PROJECTILES_PER_FRAME = 24;
   private static final double BOW_SPEED = 3.0;
   private static final double CROSSBOW_SPEED = 3.15;
   private static final double TRIDENT_SPEED = 2.5;
   private static final double THROWABLE_SPEED = 1.5;
   private static final double PROJECTILE_GRAVITY = 0.05;
   private static final double THROWABLE_GRAVITY = 0.03;
   private static final double AIR_DRAG = 0.99;
   private static final double WATER_DRAG = 0.8;

   public static MultiBooleanSetting projectileTypes = new MultiBooleanSetting(
         "Projectiles",
         new BooleanSetting("Arrows/Crossbow", true),
         new BooleanSetting("Trident", true),
         new BooleanSetting("Pearl", true),
         new BooleanSetting("Throwables", true),
         new BooleanSetting("Others", true));
   public static MultiBooleanSetting owners = new MultiBooleanSetting(
         "Thrower",
         new BooleanSetting("Mine", true),
         new BooleanSetting("Other Players", true),
         new BooleanSetting("Mobs", true));
   public static BooleanSetting showWorldProjectiles = new BooleanSetting("World Projectiles", true);
   public static BooleanSetting showAimPrediction = new BooleanSetting("Aim Prediction", true);
   public static BooleanSetting showThirdPerson = new BooleanSetting("Show in Third Person", true);
   public static BooleanSetting showImpactBlock = new BooleanSetting("Impact Highlight", true);
   public static BooleanSetting showTargetOutline = new BooleanSetting("Target Outline", true);
   public static BooleanSetting ignoreDepth = new BooleanSetting("Through Walls", false);
   public static SliderSetting lineWidth = new SliderSetting("Thickness", 0.25F, 0.1F, 1.2F, 0.05F, false);
   public static SliderSetting lineAlpha = new SliderSetting("Opacity", 145.0F, 20.0F, 255.0F, 1.0F, false);
   public static SliderSetting simulationSteps = new SliderSetting("Trajectory Length", 120.0F, 20.0F, 260.0F, 1.0F, false);
   public static SliderSetting simulationStep = new SliderSetting("Accuracy", 0.15F, 0.05F, 1.0F, 0.05F, false);

   public Projectile() {
      this.addSettings(new Setting[] {
            projectileTypes,
            owners,
            showWorldProjectiles,
            showAimPrediction,
            showThirdPerson,
            showImpactBlock,
            showTargetOutline,
            ignoreDepth,
            lineWidth,
            lineAlpha,
            simulationSteps,
            simulationStep
      });
   }

   @EventInit
   public void onWorldRender(WorldRenderEvent event) {
      if (mc.world == null || mc.player == null) {
         return;
      }

      WorldRenderer renderer = event.worldRenderer();
      int maxSteps = Math.max(1, (int) simulationSteps.get());
      double step = Math.max(0.05, simulationStep.get());
      double width = Math.max(0.1, lineWidth.get());
      int alpha = Math.max(0, Math.min(255, (int) lineAlpha.get()));
      int startColor = ColorUtil.replAlpha(BASE_COLOR, alpha);
      int endColor = ColorUtil.replAlpha(ColorUtil.multDark(BASE_COLOR, 0.45F), alpha);
      boolean depthTest = !ignoreDepth.get();

      if (showWorldProjectiles.get()) {
         int processed = 0;
         for (Entity entity : mc.world.getEntities()) {
            if (processed >= MAX_WORLD_PROJECTILES_PER_FRAME) {
               break;
            }
            if (!(entity instanceof ProjectileEntity projectile)
                  || mc.player.squaredDistanceTo(projectile) > MAX_TRACK_DISTANCE_SQ
                  || this.isLanded(projectile)
                  || !this.shouldTrack(projectile)) {
               continue;
            }

            Trajectory trajectory = this.simulateEntityTrajectory(projectile, maxSteps, step);
            this.renderTrajectory(renderer, trajectory, width, startColor, endColor, depthTest);
            processed++;
         }
      }

      if (showAimPrediction.get()) {
         if (!showThirdPerson.get() && !mc.options.getPerspective().isFirstPerson()) {
            return;
         }
         this.renderTrajectory(renderer, this.simulatePlayerPrediction(mc.player, maxSteps, step), width, startColor, endColor, depthTest);
      }
   }

   private void renderTrajectory(
         WorldRenderer renderer,
         Trajectory trajectory,
         double width,
         int startColor,
         int endColor,
         boolean depthTest
   ) {
      if (trajectory == null || trajectory.points().size() < 2) {
         return;
      }

      List<Vec3d> points = trajectory.points();
      int segments = points.size() - 1;
      for (int i = 0; i < segments; i++) {
         float t = segments <= 1 ? 1.0F : (float) i / (float) (segments - 1);
         renderer.drawLine(points.get(i), points.get(i + 1), width, ColorUtil.fadeBetween(startColor, endColor, t), depthTest);
      }

      if (showImpactBlock.get() && trajectory.impactBlock() != null) {
         this.renderImpactHighlight(renderer, trajectory.impactBlock(), width, depthTest);
      }

      if (showTargetOutline.get() && trajectory.hitEntity() != null) {
         this.renderEntityOutline(renderer, trajectory.hitEntity(), width, startColor, depthTest);
      }
   }

   private void renderImpactHighlight(WorldRenderer renderer, BlockPos pos, double width, boolean depthTest) {
      double pad = Math.max(0.004, width * 0.02);
      Vec3d min = new Vec3d(pos.getX() - pad, pos.getY() - pad, pos.getZ() - pad);
      Vec3d max = new Vec3d(pos.getX() + 1.0 + pad, pos.getY() + 1.0 + pad, pos.getZ() + 1.0 + pad);
      renderer.drawCube(min, max, ColorUtil.replAlpha(BASE_COLOR, 35), depthTest);
   }

   private void renderEntityOutline(WorldRenderer renderer, Entity entity, double width, int color, boolean depthTest) {
      if (entity == null) {
         return;
      }
      Box box = entity.getBoundingBox().expand(0.02);
      Vec3d min = new Vec3d(box.minX, box.minY, box.minZ);
      Vec3d max = new Vec3d(box.maxX, box.maxY, box.maxZ);
      Vec3d[] corners = {
            new Vec3d(min.x, min.y, min.z), new Vec3d(max.x, min.y, min.z),
            new Vec3d(max.x, min.y, max.z), new Vec3d(min.x, min.y, max.z),
            new Vec3d(min.x, max.y, min.z), new Vec3d(max.x, max.y, min.z),
            new Vec3d(max.x, max.y, max.z), new Vec3d(min.x, max.y, max.z)
      };
      int[][] edges = { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 0 }, { 4, 5 }, { 5, 6 }, { 6, 7 }, { 7, 4 }, { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 } };
      for (int[] edge : edges) {
         renderer.drawLine(corners[edge[0]], corners[edge[1]], width, color, depthTest);
      }
   }

   private boolean isLanded(ProjectileEntity projectile) {
      return projectile instanceof PersistentProjectileEntity persistent && persistent.isOnGround()
            || projectile instanceof TridentEntity trident && trident.isOnGround();
   }

   private boolean shouldTrack(ProjectileEntity projectile) {
      if (projectile == null || !projectile.isAlive() || !this.isEnabledType(projectile)) {
         return false;
      }

      Entity owner = projectile.getOwner();
      if (owner == null || !(owner instanceof PlayerEntity)) {
         return owners.get("Mobs");
      }
      return owner == mc.player ? owners.get("Mine") : owners.get("Other Players");
   }

   private boolean isEnabledType(ProjectileEntity projectile) {
      if (projectile instanceof EnderPearlEntity) {
         return projectileTypes.get("Pearl");
      }
      if (projectile instanceof TridentEntity) {
         return projectileTypes.get("Trident");
      }
      if (projectile instanceof PersistentProjectileEntity) {
         return projectileTypes.get("Arrows/Crossbow");
      }
      if (projectile instanceof ThrownItemEntity) {
         return projectileTypes.get("Throwables");
      }
      return projectileTypes.get("Others");
   }

   private Trajectory simulateEntityTrajectory(ProjectileEntity projectile, int maxSteps, double step) {
      Vec3d position = projectile.getLerpedPos(mc.getRenderTickCounter().getTickProgress(true));
      Vec3d velocity = projectile.getVelocity();
      double drag = projectile.isTouchingWater() ? WATER_DRAG : AIR_DRAG;
      return this.simulate(position, velocity, projectile.hasNoGravity(), drag, this.resolveGravity(projectile), maxSteps, step, false);
   }

   private Trajectory simulatePlayerPrediction(PlayerEntity player, int maxSteps, double step) {
      ItemStack stack = player.getMainHandStack();
      if (stack == null || stack.isEmpty()) {
         return null;
      }

      Item item = stack.getItem();
      boolean using = player.isUsingItem();
      Vec3d look = player.getRotationVec(1.0F);
      Vec3d velocity;
      double gravity;

      if (item instanceof BowItem) {
         if (!using) {
            return null;
         }
         float pull = BowItem.getPullProgress(stack.getMaxUseTime(player) - player.getItemUseTimeLeft());
         if (pull <= 0.03F) {
            return null;
         }
         velocity = look.multiply(Math.max(0.15F, pull) * BOW_SPEED);
         gravity = PROJECTILE_GRAVITY;
      } else if (item instanceof CrossbowItem) {
         if (!CrossbowItem.isCharged(stack) && !using) {
            return null;
         }
         velocity = look.multiply(CROSSBOW_SPEED);
         gravity = PROJECTILE_GRAVITY;
      } else if (item == Items.TRIDENT) {
         if (!using) {
            return null;
         }
         velocity = look.multiply(TRIDENT_SPEED);
         gravity = PROJECTILE_GRAVITY;
      } else if (this.isThrowableItem(item)) {
         velocity = look.multiply(THROWABLE_SPEED);
         gravity = THROWABLE_GRAVITY;
      } else {
         return null;
      }

      return this.simulate(this.getHandStart(player), velocity, false, AIR_DRAG, gravity, maxSteps, step, true);
   }

   private Vec3d getHandStart(PlayerEntity player) {
      Vec3d eye = player.getEyePos();
      Vec3d look = player.getRotationVec(1.0F).normalize();
      Vec3d right = look.crossProduct(new Vec3d(0.0, 1.0, 0.0));
      if (right.lengthSquared() < 1.0E-4) {
         right = new Vec3d(1.0, 0.0, 0.0);
      } else {
         right = right.normalize();
      }
      double side = player.getMainArm() == Arm.RIGHT ? 1.0 : -1.0;
      return eye.add(look.multiply(0.28)).add(right.multiply(0.20 * side)).add(0.0, -0.16, 0.0);
   }

   private boolean isThrowableItem(Item item) {
      return item == Items.ENDER_PEARL
            || item == Items.SNOWBALL
            || item == Items.EGG
            || item == Items.EXPERIENCE_BOTTLE
            || item == Items.SPLASH_POTION
            || item == Items.LINGERING_POTION
            || item == Items.WIND_CHARGE;
   }

   private double resolveGravity(ProjectileEntity projectile) {
      if (projectile instanceof PersistentProjectileEntity || projectile instanceof TridentEntity) {
         return PROJECTILE_GRAVITY;
      }
      return THROWABLE_GRAVITY;
   }

   private Trajectory simulate(
         Vec3d start,
         Vec3d initialVelocity,
         boolean hasNoGravity,
         double drag,
         double gravity,
         int maxSteps,
         double step,
         boolean allowEntityHit
   ) {
      if (mc.world == null) {
         return null;
      }

      List<Vec3d> points = new ArrayList<>(maxSteps + 1);
      Vec3d position = start;
      Vec3d velocity = initialVelocity;
      BlockPos impactBlock = null;
      Entity hitEntity = null;
      points.add(position);

      for (int i = 0; i < maxSteps; i++) {
         Vec3d next = position.add(velocity.multiply(step));

         if (allowEntityHit) {
            EntityHitResult entityHit = ProjectileUtil.getEntityCollision(
                  mc.world,
                  mc.player,
                  position,
                  next,
                  new Box(position, next).expand(1.0),
                  entity -> entity != null && entity.isAlive() && !entity.isSpectator() && entity != mc.player,
                  0.0F
            );
            if (entityHit != null && entityHit.getEntity() != null) {
               hitEntity = entityHit.getEntity();
               points.add(entityHit.getPos());
               break;
            }
         }

         HitResult hit = mc.world.raycast(new RaycastContext(
               position,
               next,
               RaycastContext.ShapeType.COLLIDER,
               RaycastContext.FluidHandling.NONE,
               mc.player
         ));
         if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            impactBlock = blockHit.getBlockPos();
            points.add(blockHit.getPos());
            break;
         }
         points.add(next);

         velocity = velocity.multiply(Math.pow(drag, step));
         if (!hasNoGravity) {
            velocity = velocity.add(0.0, -gravity * step, 0.0);
         }
         position = next;
         if (position.y < mc.world.getBottomY() - 4.0) {
            break;
         }
      }

      return new Trajectory(points, impactBlock, hitEntity);
   }

   private record Trajectory(List<Vec3d> points, BlockPos impactBlock, Entity hitEntity) {
   }
}