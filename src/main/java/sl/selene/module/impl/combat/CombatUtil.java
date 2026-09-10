package sl.selene.module.impl.combat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class CombatUtil {

   public static final double MAX_REACH = 3.0D;

   private CombatUtil() {
   }

   public static double clampReach(double reach) {
      return Math.min(Math.max(0.5D, reach), MAX_REACH);
   }

   public enum TargetType {
      PLAYERS,
      MOBS,
      ALL
   }

   public static boolean canAttack(Entity entity, MinecraftClient mc) {
      if (entity == null || mc.player == null) {
         return false;
      }
      if (!(entity instanceof LivingEntity living)) {
         return false;
      }
      if (!living.isAlive() || living.isDead()) {
         return false;
      }
      if (living instanceof ArmorStandEntity) {
         return false;
      }
      if (living == mc.player || living.getUuid().equals(mc.player.getUuid())) {
         return false;
      }
      if (living instanceof PlayerEntity other && mc.player.isTeammate(other)) {
         return false;
      }
      return true;
   }

   public static boolean matchesType(LivingEntity target, TargetType type, MinecraftClient mc) {
      if (type == TargetType.ALL) {
         return true;
      }
      if (target instanceof PlayerEntity) {
         return type == TargetType.PLAYERS;
      }
      if (target instanceof Monster) {
         return type == TargetType.MOBS;
      }
      if (target instanceof PassiveEntity) {
         return type == TargetType.MOBS;
      }
      return type == TargetType.MOBS;
   }

   public static LivingEntity findTarget(float range, TargetType type, MinecraftClient mc) {
      if (mc.player == null || mc.world == null) {
         return null;
      }

      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      double rangeSquared = (double) range * range;
      Vec3d eye = mc.player.getEyePos();

      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof LivingEntity living) || !canAttack(entity, mc)) {
            continue;
         }
         if (!matchesType(living, type, mc)) {
            continue;
         }
         double distance = living.squaredDistanceTo(eye);
         if (distance > rangeSquared) {
            continue;
         }
         if (distance < bestDistance) {
            bestDistance = distance;
            best = living;
         }
      }
      return best;
   }

   public static Vec3d aimPoint(LivingEntity target) {
      return aimPoint(target, "Chest");
   }

   public static Vec3d aimPoint(LivingEntity target, String mode) {
      if (mode == null) {
         mode = "Chest";
      }
      double y;
      switch (mode) {
         case "Head" -> y = target.getEyeY();
         case "Feet" -> y = target.getY() + 0.1;
         default -> y = target.getY() + Math.min(target.getHeight() * 0.55F, 1.35F);
      }
      return new Vec3d(target.getX(), y, target.getZ());
   }

   public static boolean isWeapon(net.minecraft.item.ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      var key = stack.getItem().getRegistryEntry().getKey().orElse(null);
      if (key == null) {
         return false;
      }
      String path = key.getValue().getPath();
      return path.endsWith("_sword") || path.endsWith("_axe") || path.equals("mace");
   }

   public static boolean isWeaponType(net.minecraft.item.ItemStack stack, String mode) {
      if (mode == null || mode.equals("Any")) {
         return true;
      }
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      var key = stack.getItem().getRegistryEntry().getKey().orElse(null);
      if (key == null) {
         return false;
      }
      String path = key.getValue().getPath();
      boolean sword = path.endsWith("_sword");
      boolean axe = path.endsWith("_axe");
      boolean trident = path.endsWith("_trident");
      boolean mace = path.equals("mace");
      return switch (mode) {
         case "Sword" -> sword;
         case "Axe" -> axe;
         default -> sword || axe || trident || mace;

      };
   }

   public static boolean matchesTargetMode(LivingEntity living, String mode) {
      if (mode == null || mode.equals("Everything")) {
         return true;
      }
      if (living instanceof PlayerEntity) {
         return mode.equals("Players");
      }
      return switch (mode) {
         case "Players" -> false;
         case "Hostiles" -> living instanceof Monster;
         default -> true;

      };
   }

   public static float[] rotationTo(MinecraftClient mc, Vec3d aim) {
      Vec3d eye = mc.player.getEyePos();
      double dx = aim.getX() - eye.getX();
      double dy = aim.getY() - eye.getY();
      double dz = aim.getZ() - eye.getZ();
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
      return new float[] { MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0F, 90.0F) };
   }

   public static float angleTo(float[] rotation, MinecraftClient mc) {
      float yawDelta = MathHelper.wrapDegrees(rotation[0] - mc.player.getYaw());
      float pitchDelta = rotation[1] - mc.player.getPitch();
      return (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
   }

   public static float random(float min, float max) {
      return min + (float) Math.random() * (max - min);
   }
}