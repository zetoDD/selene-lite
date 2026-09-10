package sl.selene.util.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.hit.HitResult;

@Environment(EnvType.CLIENT)
public final class CrosshairPin {
   private static HitResult pinned;
   private static Object owner;

   private CrosshairPin() {
   }

   public static boolean set(Object requester, HitResult hit) {
      if (requester == null || hit == null) {
         return false;
      }
      if (owner != null && owner != requester) {
         return false;
      }
      owner = requester;
      pinned = hit;
      return true;
   }

   public static void clear(Object requester) {
      if (owner == requester) {
         owner = null;
         pinned = null;
      }
   }

   public static HitResult get() {
      return pinned;
   }

   public static Object getOwner() {
      return owner;
   }
}