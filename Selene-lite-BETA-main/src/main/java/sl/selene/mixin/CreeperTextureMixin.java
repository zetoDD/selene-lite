package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({CreeperEntityRenderer.class})
public abstract class CreeperTextureMixin {
   @Unique
   private static final Identifier CUSTOM_CREEPER_TEXTURE = Identifier.of("selene", "textures/creeper/creeper.png");

   @Inject(
      method = {"getTexture(Lnet/minecraft/client/render/entity/state/CreeperEntityRenderState;)Lnet/minecraft/util/Identifier;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void replaceCreeperTexture(CreeperEntityRenderState state, CallbackInfoReturnable<Identifier> cir) {
      Identifier original = (Identifier)cir.getReturnValue();
      cir.setReturnValue(CUSTOM_CREEPER_TEXTURE);
   }
}