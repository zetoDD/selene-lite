package sl.selene.module.impl.combat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class SpearUtil {
   private SpearUtil() {
   }

   public static boolean isSpear(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      var key = stack.getItem().getRegistryEntry().getKey().orElse(null);
      if (key == null) {
         return false;
      }
      return key.getValue().getPath().endsWith("_spear");
   }

   public static boolean isHoldingSpear() {
      if (mc() == null) {
         return false;
      }
      return isSpear(mc().player.getMainHandStack()) || isSpear(mc().player.getOffHandStack());
   }

   public static boolean isHoldingLungeSpear() {
      if (mc() == null) {
         return false;
      }
      ItemStack main = mc().player.getMainHandStack();
      return isSpear(main) && getLungeLevel(main) > 0;
   }

   public static int getLungeLevel(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return 0;
      }
      ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
      if (enchantments == null || enchantments.isEmpty()) {
         return 0;
      }
      int level = 0;
      for (var enchantment : enchantments.getEnchantments()) {
         if (enchantment.getIdAsString().toLowerCase().contains("lunge")) {
            level = Math.max(level, enchantments.getLevel(enchantment));
         }
      }
      return level;
   }

   public static int findSpearSlot(boolean requireLunge) {
      if (mc() == null || mc().player == null) {
         return -1;
      }
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc().player.getInventory().getStack(i);
         if (!isSpear(stack)) {
            continue;
         }
         if (requireLunge && getLungeLevel(stack) <= 0) {
            continue;
         }
         return i;
      }
      return -1;
   }

   public static int findSpearSlotWithoutLunge() {
      if (mc() == null || mc().player == null) {
         return -1;
      }
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc().player.getInventory().getStack(i);
         if (!isSpear(stack)) {
            continue;
         }
         if (getLungeLevel(stack) > 0) {
            continue;
         }
         return i;
      }
      return -1;
   }

   public static boolean hasSpearInHotbar(boolean requireLunge) {
      return findSpearSlot(requireLunge) != -1;
   }

   public static AttackRangeComponent attackRange(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         AttackRangeComponent range = stack.get(DataComponentTypes.ATTACK_RANGE);
         if (range != null) {
            return range;
         }
      }
      return mc() != null && mc().player != null ? AttackRangeComponent.defaultForEntity(mc().player) : null;
   }

   public static LivingEntity getCrosshairTarget() {
      return raycastTarget(attackRange(mc() != null && mc().player != null ? mc().player.getMainHandStack() : null));
   }

   public static LivingEntity getSpearCrosshairTarget() {
      EntityHitResult hit = getSpearCrosshairHit();
      return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
   }

   public static EntityHitResult getSpearCrosshairHit() {
      if (mc() == null || mc().player == null || mc().world == null) {
         return null;
      }
      AttackRangeComponent range = spearAttackRange();
      if (range == null) {
         return null;
      }
      float tickDelta = mc().getRenderTickCounter().getTickProgress(false);
      HitResult result = range.getHitResult(mc().player, tickDelta, EntityPredicates.CAN_HIT);
      if (!(result instanceof EntityHitResult entityHit)
            || !range.isWithinRange(mc().player, result.getPos())
            || !(entityHit.getEntity() instanceof LivingEntity living)
            || !living.isAlive()
            || living.isRemoved()) {
         return null;
      }
      return entityHit;
   }

   private static AttackRangeComponent spearAttackRange() {
      int slot = findSpearSlot(false);
      if (slot != -1 && mc() != null && mc().player != null) {
         ItemStack stack = mc().player.getInventory().getStack(slot);
         AttackRangeComponent range = stack.get(DataComponentTypes.ATTACK_RANGE);
         if (range != null) {
            return range;
         }
      }
      return attackRange(mc() != null && mc().player != null ? mc().player.getMainHandStack() : null);
   }

   private static LivingEntity raycastTarget(AttackRangeComponent range) {
      if (mc() == null || mc().player == null || mc().world == null || range == null) {
         return null;
      }
      float tickDelta = mc().getRenderTickCounter().getTickProgress(false);
      HitResult result = range.getHitResult(mc().player, tickDelta, EntityPredicates.CAN_HIT);
      if (result == null || result.getType() != HitResult.Type.ENTITY) {
         return null;
      }
      if (!(result instanceof EntityHitResult entityHit)) {
         return null;
      }
      if (!range.isWithinRange(mc().player, result.getPos())) {
         return null;
      }
      if (!(entityHit.getEntity() instanceof LivingEntity living)) {
         return null;
      }
      return living.isAlive() && !living.isRemoved() ? living : null;
   }

   public static boolean isTargetInRange(LivingEntity target) {
      if (target == null || mc() == null || mc().player == null) {
         return false;
      }
      AttackRangeComponent range = attackRange(mc().player.getMainHandStack());
      if (range == null) {
         return false;
      }
      return range.isWithinRange(mc().player, target.getBoundingBox().getCenter());
   }

   public static void sendStab(int count) {
      if (mc() == null || mc().player == null) {
         return;
      }
      for (int i = 0; i < Math.max(1, count); i++) {
         mc().player.networkHandler.sendPacket(
               new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STAB, BlockPos.ORIGIN, Direction.DOWN));
      }
   }

   public static void swapToSlot(int slot) {
      if (mc() != null && mc().player != null && slot >= 0 && slot < 9) {
         mc().player.getInventory().setSelectedSlot(slot);
      }
   }

   public static void syncSlot(int slot) {
      if (mc() != null && mc().player != null && slot >= 0 && slot < 9) {
         mc().player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   public static void clickAttack() {
      if (mc() == null || mc().options == null) {
         return;
      }
      KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(mc().options.attackKey.getBoundKeyTranslationKey()));
   }

   public static boolean validTarget(LivingEntity target) {
      if (target == null || mc() == null || mc().player == null) {
         return false;
      }
      if (target == mc().player || !target.isAlive() || target.isRemoved()) {
         return false;
      }
      return !target.isTeammate(mc().player);
   }

   public static float yawForVelocity(Vec3d velocity) {
      double horizontal = velocity.x * velocity.x + velocity.z * velocity.z;
      if (horizontal < 1.0E-6) {
         return mc() != null && mc().player != null ? mc().player.getYaw() : 0.0F;
      }
      return (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
   }

   private static net.minecraft.client.MinecraftClient mc() {
      return net.minecraft.client.MinecraftClient.getInstance();
   }
}