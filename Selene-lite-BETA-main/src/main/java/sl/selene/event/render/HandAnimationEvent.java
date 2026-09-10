package sl.selene.event.render;

import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class HandAnimationEvent extends Event {
   private MatrixStack matrices;
   private Hand hand;
   private float swingProgress;
   private ItemStack stack;

   @Generated
   public HandAnimationEvent(MatrixStack matrices, Hand hand, float swingProgress, ItemStack stack) {
      this.matrices = matrices;
      this.hand = hand;
      this.swingProgress = swingProgress;
      this.stack = stack;
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
   }

   @Generated
   public Hand getHand() {
      return this.hand;
   }

   @Generated
   public float getSwingProgress() {
      return this.swingProgress;
   }

   @Generated
   public ItemStack getStack() {
      return this.stack;
   }

   @Generated
   public boolean isEmptyHand() {
      return this.stack == null || this.stack.isEmpty();
   }

   @Generated
   public void setMatrices(MatrixStack matrices) {
      this.matrices = matrices;
   }

   @Generated
   public void setHand(Hand hand) {
      this.hand = hand;
   }

   @Generated
   public void setSwingProgress(float swingProgress) {
      this.swingProgress = swingProgress;
   }
}