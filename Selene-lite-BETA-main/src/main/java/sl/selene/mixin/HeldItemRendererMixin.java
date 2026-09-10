package sl.selene.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.event.EventManager;
import sl.selene.event.render.HandAnimationEvent;
import sl.selene.event.render.RenderItemEvent;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
   @Shadow
   private ItemStack mainHand;
   @Shadow
   private ItemStack offHand;
   @Shadow
   private float equipProgressMainHand;
   @Shadow
   private float lastEquipProgressMainHand;
   @Shadow
   private float equipProgressOffHand;
   @Shadow
   private float lastEquipProgressOffHand;

   @Shadow
   protected abstract void renderFirstPersonItem(
      AbstractClientPlayerEntity var1, float var2, float var3, Hand var4, float var5, ItemStack var6, float var7, MatrixStack var8, OrderedRenderCommandQueue var9, int var10
   );

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")}
   )
   private void onRenderFirstPersonItem(
      AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci
   ) {
      RenderItemEvent renderItemEvent = new RenderItemEvent(matrices, hand, stack);
      EventManager.call(renderItemEvent);
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V"
      )}
   )
   private void handAnimationHook(
      HeldItemRenderer instance,
      float swingProgress,
      MatrixStack matrices,
      int armX,
      Arm arm,
      Operation<Void> original,
      @Local(ordinal = 0,argsOnly = true) AbstractClientPlayerEntity player,
      @Local(ordinal = 0,argsOnly = true) Hand hand,
      @Local(ordinal = 0,argsOnly = true) ItemStack stack
   ) {
      HandAnimationEvent event = new HandAnimationEvent(matrices, hand, swingProgress, stack);
      EventManager.call(event);

      if (!event.isCancelled()) {
         original.call(instance, swingProgress, matrices, armX, arm);
      }
   }

   @Unique
   private boolean night$isChargedCrossbow(ItemStack stack) {
      return stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
   }
}