package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sl.selene.module.impl.visuals.CustomHitbox;
import sl.selene.module.impl.visuals.CustomHitboxRenderer;

@Environment(EnvType.CLIENT)
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
   @Inject(
      method = "drawHitbox",
      at = @At("HEAD"),
      cancellable = true
   )
   private void selene$replaceVanillaHitbox(Entity entity, float tickProgress, boolean inLocalServer, CallbackInfo ci) {
      if (!CustomHitbox.isActive() || !CustomHitbox.shouldRenderEntity(entity)) {
         return;
      }

      CustomHitboxRenderer.drawHitbox(entity, tickProgress);
      ci.cancel();
   }
}