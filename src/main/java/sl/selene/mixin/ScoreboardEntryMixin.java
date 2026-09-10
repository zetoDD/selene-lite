package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sl.selene.module.impl.donut.NameHider;

@Environment(EnvType.CLIENT)
@Mixin(ScoreboardEntry.class)
public abstract class ScoreboardEntryMixin {

   @Shadow
   @Final
   private Text display;

   @Shadow
   private String owner;

   @Inject(method = "name", at = @At("HEAD"), cancellable = true)
   private void selene$nameHiderScoreboardName(CallbackInfoReturnable<Text> cir) {
      if (!NameHider.isHiding()) {
         return;
      }
      if (this.display != null) {
         if (NameHider.referencesRealName(this.display.getString())) {
            cir.setReturnValue(NameHider.hide(this.display));
         }
      } else if (NameHider.referencesRealName(this.owner)) {
         cir.setReturnValue(NameHider.hide(Text.literal(this.owner)));
      }
   }

   @Inject(method = "display", at = @At("HEAD"), cancellable = true)
   private void selene$nameHiderScoreboardDisplay(CallbackInfoReturnable<Text> cir) {
      if (!NameHider.isHiding() || this.display == null) {
         return;
      }
      if (NameHider.referencesRealName(this.display.getString())) {
         cir.setReturnValue(NameHider.hide(this.display));
      }
   }
}