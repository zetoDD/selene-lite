package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sl.selene.event.EventManager;
import sl.selene.event.input.KeepSprintEvent;
import sl.selene.module.impl.donut.NameHider;
import sl.selene.util.other.IMinecraft;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntity.class})
public abstract class PlayerEntityMixin implements IMinecraft {
   @Inject(
      method = {"knockbackTarget"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V",
         shift = Shift.AFTER
      )}
   )
   public void attackHook(CallbackInfo callbackInfo) {
      EventManager.call(new KeepSprintEvent());
   }

   @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
   private void selene$nameHiderName(CallbackInfoReturnable<Text> cir) {
      if ((Object) this == MinecraftClient.getInstance().player && NameHider.isHiding()) {
         cir.setReturnValue(Text.literal(NameHider.fakeName()));
      }
   }

   @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
   private void selene$nameHiderDisplayName(CallbackInfoReturnable<Text> cir) {
      if ((Object) this == MinecraftClient.getInstance().player && NameHider.isHiding()) {
         cir.setReturnValue(Text.literal(NameHider.fakeName()));
      }
   }
}