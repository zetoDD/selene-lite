package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.Selene;
import sl.selene.module.impl.movement.SnapTap;

@Environment(EnvType.CLIENT)
@Mixin(KeyboardInput.class)
public abstract class SnapTapMixin {

   @Inject(
      method = "tick",
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/input/KeyboardInput;playerInput:Lnet/minecraft/util/PlayerInput;",
         opcode = 181,
         shift = Shift.AFTER
      )
   )
   private void selene$applySnapTap(CallbackInfo ci) {
      if (Selene.get == null || Selene.get.manager == null) {
         return;
      }
      SnapTap snapTap = Selene.get.manager.get(SnapTap.class);
      if (snapTap == null || !snapTap.enable) {
         return;
      }
      KeyboardInput self = (KeyboardInput) (Object) this;
      PlayerInput current = self.playerInput;
      PlayerInput modified = new PlayerInput(
         SnapTap.getForward(current),
         SnapTap.getBack(current),
         SnapTap.getLeft(current),
         SnapTap.getRight(current),
         current.jump(),
         current.sneak(),
         current.sprint()
      );
      self.playerInput = modified;
   }
}