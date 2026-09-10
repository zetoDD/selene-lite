package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.module.impl.visuals.ShulkerPreview;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
   @Inject(method = "render", at = @At("TAIL"))
   private void selene$renderShulkerPreview(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ShulkerPreview.renderPreview((HandledScreen<?>) (Object) this, context, mouseX, mouseY);
   }
}