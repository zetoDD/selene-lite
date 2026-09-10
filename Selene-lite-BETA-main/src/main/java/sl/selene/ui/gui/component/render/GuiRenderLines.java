package sl.selene.ui.gui.component.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public class GuiRenderLines extends GuiScreen {
   public static void renderLines(Renderer2D renderer2D, MatrixStack pose, float mainAlpha, boolean mapMode) {
      float x0 = snap(GuiScreen.x);
      float x1 = snap(GuiScreen.x + GuiScreen.width);
      float headerBottom = snap(GuiScreen.y + GuiScreen.HEADER_HEIGHT);
      float pixel = pixelSize();

      if (mapMode) {
         UiIcons.horizontalShineLine(renderer2D, x0, headerBottom, x1 - x0, pixel, mainAlpha);
      }
   }

   private static float pixelSize() {
      return 1.0F / Math.max(1.0F, GuiScreen.renderScale);
   }

   private static float snap(float value) {
      float scale = Math.max(1.0F, GuiScreen.renderScale);
      return Math.round(value * scale) / scale;
   }
}