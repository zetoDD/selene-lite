package sl.selene.event.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class RenderItemEvent extends Event {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final MatrixStack matrix;
   private final Hand hand;
   private final ItemStack stack;

   public RenderItemEvent(MatrixStack matrix, Hand hand, ItemStack stack) {
      this.matrix = matrix;
      this.hand = hand;
      this.stack = stack;
   }

   public MatrixStack getMatrix() {
      return this.matrix;
   }

   public Hand getHand() {
      return this.hand;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public boolean isEmptyHand() {
      return this.stack == null || this.stack.isEmpty();
   }

   public boolean isRightHand() {
      if (mc != null && mc.player != null) {
         net.minecraft.util.Arm mainArm = mc.player.getMainArm();
         boolean isMainHandRight = (mainArm == net.minecraft.util.Arm.RIGHT && this.hand == Hand.MAIN_HAND);
         boolean isOffHandRight = (mainArm == net.minecraft.util.Arm.LEFT && this.hand == Hand.OFF_HAND);
         return isMainHandRight || isOffHandRight;
      }
      return this.hand == Hand.MAIN_HAND;
   }
}