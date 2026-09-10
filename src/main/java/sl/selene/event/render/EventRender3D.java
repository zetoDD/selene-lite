package sl.selene.event.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class EventRender3D extends Event {
   private final MatrixStack matrixStack;
   private final float tickDelta;

   public EventRender3D(MatrixStack matrixStack, float tickDelta) {
      this.matrixStack = matrixStack;
      this.tickDelta = tickDelta;
   }

   public MatrixStack getMatrixStack() {
      return this.matrixStack;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }
}