package sl.selene.util.render.backends.gl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public final class StencilHelper {
   private StencilHelper() {
   }

   public static void initStencil() {
      GL11.glEnable(2960);
      GL11.glClearStencil(0);
      GL11.glClear(1024);
      GL11.glStencilFunc(519, 1, 255);
      GL11.glStencilOp(7680, 7680, 7681);
      GL11.glColorMask(false, false, false, false);
      GL11.glDepthMask(false);
   }

   public static void bindReadStencilBuffer(int ref) {
      GL11.glColorMask(true, true, true, true);
      GL11.glDepthMask(true);
      GL11.glStencilFunc(514, ref, 255);
      GL11.glStencilOp(7680, 7680, 7680);
   }

   public static void uninitStencilBuffer() {
      GL11.glDisable(2960);
      GL11.glStencilFunc(519, 0, 255);
      GL11.glStencilOp(7680, 7680, 7680);
   }
}