package sl.selene.util.render.world;

import com.mojang.blaze3d.systems.RenderPass;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface MoreMultiPhase {
   Object withRenderPassSetup(Consumer<RenderPass> var1);

   static MoreMultiPhase cast(Object multiPhase) {
      Objects.requireNonNull(multiPhase, "multiPhase");
      return (MoreMultiPhase) multiPhase;
   }
}