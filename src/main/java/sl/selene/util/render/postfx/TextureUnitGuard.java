package sl.selene.util.render.postfx;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

@Environment(EnvType.CLIENT)
final class TextureUnitGuard implements AutoCloseable {
   private final int unit;
   private final int previousActiveUnit;
   private final int[] targets;
   private final int[] bindings;
   private boolean closed;

   private TextureUnitGuard(int unit, int previousActiveUnit, int[] targets, int[] bindings) {
      this.unit = unit;
      this.previousActiveUnit = previousActiveUnit;
      this.targets = targets;
      this.bindings = bindings;
   }

   static TextureUnitGuard capture(int unit, int... requestedTargets) {
      int previousActive = Math.max(0, GL11.glGetInteger(34016) - 33984);
      int[] targets = deduplicateTargets(requestedTargets);
      int[] bindings = new int[targets.length];
      if (targets.length > 0) {
         GL13.glActiveTexture(33984 + unit);

         for (int i = 0; i < targets.length; i++) {
            bindings[i] = GL11.glGetInteger(bindingEnumForTarget(targets[i]));
         }

         GL13.glActiveTexture(33984 + previousActive);
      }

      return new TextureUnitGuard(unit, previousActive, targets, bindings);
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         if (this.targets.length > 0) {
            GL13.glActiveTexture(33984 + this.unit);

            for (int i = 0; i < this.targets.length; i++) {
               GL11.glBindTexture(this.targets[i], this.bindings[i]);
            }
         }

         GL13.glActiveTexture(33984 + this.previousActiveUnit);
      }
   }

   private static int[] deduplicateTargets(int[] requestedTargets) {
      if (requestedTargets != null && requestedTargets.length != 0) {
         int[] copy = Arrays.copyOf(requestedTargets, requestedTargets.length);
         int count = 0;

         for (int value : copy) {
            if (value > 0) {
               boolean exists = false;

               for (int i = 0; i < count; i++) {
                  if (copy[i] == value) {
                     exists = true;
                     break;
                  }
               }

               if (!exists) {
                  copy[count++] = value;
               }
            }
         }

         return Arrays.copyOf(copy, count);
      } else {
         return new int[0];
      }
   }

   private static int bindingEnumForTarget(int target) {
      switch (target) {
         case 3552:
            return 32872;
         case 3553:
            return 32873;
         case 32879:
            return 32874;
         case 34037:
            return 34038;
         case 34067:
            return 34068;
         case 35864:
            return 35868;
         case 35866:
            return 35869;
         case 35882:
            return 35884;
         case 36873:
            return 36874;
         default:
            return 32873;
      }
   }
}