package sl.selene.event.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class ItemAnimationEvent extends Event {
   private final MatrixStack matrix;
   private final Hand hand;
   private final float swingProgress;
   private final float equipProgress;

   public ItemAnimationEvent(MatrixStack matrix, Hand hand, float swingProgress, float equipProgress) {
      this.matrix = matrix;
      this.hand = hand;
      this.swingProgress = swingProgress;
      this.equipProgress = equipProgress;
   }

   public MatrixStack getMatrix() {
      return this.matrix;
   }

   public Hand getHand() {
      return this.hand;
   }

   public boolean isRightHand() {
      return this.hand == Hand.MAIN_HAND;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }

   public float getEquipProgress() {
      return this.equipProgress;
   }
}