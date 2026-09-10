package sl.selene.util.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;

public final class CrosshairTargetUtil {
   private CrosshairTargetUtil() {
   }

   public static LivingEntity getLivingCrosshairTarget() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null || mc.world == null) {
         return null;
      }

      Entity aimed = mc.targetedEntity;
      if (!(aimed instanceof LivingEntity living)) {
         return null;
      }

      if (!living.isAlive() || living instanceof ArmorStandEntity) {
         return null;
      }

      if (living == mc.player || living.getUuid().equals(mc.player.getUuid())) {
         return null;
      }

      return living;
   }
}