package sl.selene.ui.gui.component.render;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public class GuiRenderBackground extends GuiScreen {
   public static void renderBackground(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      boolean vanillaStyle = GuiScreen.isVanillaStyle();
      float radius = vanillaStyle ? 1.25F : 8.0F;
      if (vanillaStyle) {
         renderer2D.rectOutline(
            GuiScreen.x,
            GuiScreen.y,
            GuiScreen.width,
            GuiScreen.height,
            radius,
            Renderer2D.ColorUtil.replAlpha(new Color(90, 90, 90).getRGB(), (int)(210.0F * mainAlpha)),
            1.0F
         );
         renderer2D.rect(
            GuiScreen.x,
            GuiScreen.y,
            GuiScreen.width,
            GuiScreen.height,
            radius,
            Renderer2D.ColorUtil.replAlpha(new Color(44, 44, 44).getRGB(), (int)(225.0F * mainAlpha))
         );
         return;
      }

      float bodyX = GuiScreen.x + GuiScreen.SIDEBAR_WIDTH;
      float bodyY = GuiScreen.y + GuiScreen.CONTENT_TOP;
      float bodyW = GuiScreen.width - GuiScreen.SIDEBAR_WIDTH;
      float bodyH = GuiScreen.height - GuiScreen.CONTENT_TOP;
      if (mainAlpha > 0.1F) {
         GlassStyle.panel(renderer2D, bodyX, bodyY, bodyW, bodyH, radius, radius, radius, radius, mainAlpha);
      } else {
         int rimColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(100.0F * mainAlpha));
         renderer2D.rect(bodyX, bodyY, bodyW, bodyH, radius, Renderer2D.ColorUtil.rgba(8, 12, 20, (int)(14.0F * mainAlpha)));
         renderer2D.rectOutline(bodyX, bodyY, bodyW, bodyH, radius, rimColor, 1.0F);
      }
   }
}