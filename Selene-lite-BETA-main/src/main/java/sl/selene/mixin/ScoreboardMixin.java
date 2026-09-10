package sl.selene.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sl.selene.module.impl.donut.NameHider;

@Environment(EnvType.CLIENT)
@Mixin(Scoreboard.class)
public abstract class ScoreboardMixin {

   @Inject(method = "getScoreboardEntries", at = @At("RETURN"), cancellable = true)
   private void selene$nameHiderScoreboard(ScoreboardObjective objective,
         CallbackInfoReturnable<Collection<ScoreboardEntry>> cir) {
      if (!NameHider.isHiding()) {
         return;
      }
      Collection<ScoreboardEntry> entries = cir.getReturnValue();
      if (entries == null || entries.isEmpty()) {
         return;
      }
      List<ScoreboardEntry> rewritten = null;
      for (ScoreboardEntry entry : entries) {
         Text shown = entry.name();
         String text = shown == null ? entry.owner() : shown.getString();
         if (!NameHider.referencesRealName(text)) {
            if (rewritten != null) {
               rewritten.add(entry);
            }
            continue;
         }
         if (rewritten == null) {
            rewritten = new ArrayList<>(entries.size());
            for (ScoreboardEntry seen : entries) {
               if (seen == entry) {
                  break;
               }
               rewritten.add(seen);
            }
         }
         Text display = NameHider.hide(shown == null ? Text.literal(entry.owner()) : shown);
         rewritten.add(new ScoreboardEntry(entry.owner(), entry.value(), display, entry.numberFormatOverride()));
      }
      if (rewritten != null) {
         cir.setReturnValue(rewritten);
      }
   }
}