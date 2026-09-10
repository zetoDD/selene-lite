package sl.selene.ui.gui.component.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4f;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class RenderUtil {
   private RenderUtil() {
   }

   public static void drawRoundedCorner(float x, float y, float width, float height, Vector4f radii, int color) {
   }

   public static void drawRoundedCorner(Renderer2D renderer2D, float x, float y, float width, float height, Vector4f radii, int color) {
      renderer2D.rect(x, y, width, height, radii.x, radii.y, radii.z, radii.w, color);
   }
}