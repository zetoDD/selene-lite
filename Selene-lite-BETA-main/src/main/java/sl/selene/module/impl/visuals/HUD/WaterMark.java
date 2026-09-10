package sl.selene.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.texture.TextureLoader;

@Environment(EnvType.CLIENT)
public class WaterMark {
   private static final String LABEL = "Selene";
   private static final float LOGO_SIZE = 34.0F;
   private static final float LABEL_FONT_SIZE = 36.0F;
   private static final float LABEL_OUTLINE_WIDTH = 1.65F;
   private static final float LABEL_SIDE_PADDING = 3.0F;
   private static final float LABEL_GAP = 10.0F;
   private static final float WATERMARK_HEIGHT = 38.0F;

   public static MinecraftClient mc = MinecraftClient.getInstance();

   public static void waterMark(Renderer2D r2) {
      if (mc.player == null || mc.getWindow() == null) {
         return;
      }

      float labelTextWidth = r2.measureText(FontRegistry.INTER_SEMIBOLD, LABEL, LABEL_FONT_SIZE).width;
      float labelBoundsPadding = LABEL_OUTLINE_WIDTH + LABEL_SIDE_PADDING;
      float totalWidth = LOGO_SIZE + LABEL_GAP + labelTextWidth + labelBoundsPadding * 2.0F;
      float totalHeight = Math.max(LOGO_SIZE, WATERMARK_HEIGHT);
      DraggableManager.DragSession session = DraggableManager.getInstance().beginDrag(
         "watermark", 10.0F, 10.0F, totalWidth, totalHeight
      );
      float x = session.positionX();
      float y = session.positionY();
      float logoY = y + (totalHeight - LOGO_SIZE) * 0.5F;
      int logoTexture = TextureLoader.loadBase64White("assets/selene/logo.png.b64");

      if (logoTexture > 0) {
         boolean glassLogoDrawn = r2.logoGlass(logoTexture, x, logoY, LOGO_SIZE, LOGO_SIZE, 1.0F);
         if (!glassLogoDrawn) {
            r2.drawRgbaTexture(
               logoTexture,
               x,
               logoY,
               LOGO_SIZE,
               LOGO_SIZE,
               Renderer2D.ColorUtil.rgba(255, 255, 255, 255),
               true
            );
         }
      } else {
         r2.circle(
            x + LOGO_SIZE * 0.5F,
            logoY + LOGO_SIZE * 0.5F,
            LOGO_SIZE * 0.42F,
            0.0F,
            1.0F,
            Renderer2D.ColorUtil.rgba(255, 255, 255, 255)
         );
      }

      float labelX = x + LOGO_SIZE + LABEL_GAP + labelBoundsPadding;
      float labelCenterY = y + totalHeight * 0.5F;
      float labelBaseline = labelCenterY + FontRegistry.centeredBaselineOffset(
         FontRegistry.INTER_SEMIBOLD,
         LABEL.codePointAt(0),
         LABEL_FONT_SIZE * 0.5F
      );
      r2.textOutline(
         FontRegistry.INTER_SEMIBOLD,
         labelX,
         labelBaseline,
         LABEL_FONT_SIZE,
         LABEL,
         Renderer2D.ColorUtil.rgba(255, 255, 255, 250),
         LABEL_OUTLINE_WIDTH
      );

      HudEditor.registerRect(x, y, totalWidth, totalHeight);
      DraggableManager.getInstance().endDrag(session);
   }
}