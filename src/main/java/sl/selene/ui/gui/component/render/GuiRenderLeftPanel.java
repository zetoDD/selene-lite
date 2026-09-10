package sl.selene.ui.gui.component.render;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.module.api.Category;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.animation.util.Easings;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public class GuiRenderLeftPanel extends GuiScreen {
   private static final float CATEGORY_ICON_X = 74.34F;
   private static final float CATEGORY_ICON_SIZE = 12.0F;

   public static void renderLeftPanel(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      if (mainAlpha <= 0.001F) {
         return;
      }

      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int mainColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(102.0F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int textTwoColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(102.0F * mainAlpha));
      boolean vanillaStyle = GuiScreen.isVanillaStyle();
      if (vanillaStyle) {
         renderer2D.rect(
            GuiScreen.x,
            GuiScreen.y + GuiScreen.HEADER_HEIGHT,
            GuiScreen.SIDEBAR_WIDTH,
            GuiScreen.height - GuiScreen.HEADER_HEIGHT - GuiScreen.SIDEBAR_BOTTOM_INSET,
            1.25F,
            Renderer2D.ColorUtil.replAlpha(new Color(44, 44, 44).getRGB(), (int)(225.0F * mainAlpha))
         );
      } else {

         GlassStyle.panel(
            renderer2D,
            GuiScreen.x,
            GuiScreen.y + 70.0F,
            GuiScreen.SIDEBAR_WIDTH - GuiScreen.GAP,
            176.0F,
            8.0F,
            8.0F,
            8.0F,
            8.0F,
            mainAlpha
         );
      }
      if (GuiScreen.settingsPageOpen) {
         GuiRenderSettings.renderSidebar(renderer2D, mainAlpha);
         return;
      }

      float downY = 0.0F;

      for (Category category : GuiScreen.categories) {
         category.anim33.update();
         category.anim33.run(category == selectedCategories ? 1.0 : 0.0, 1.0, Easings.QUART_OUT);
         float sidebarWidth = GuiScreen.SIDEBAR_WIDTH - GuiScreen.GAP;
         float rowHover = GuiScreen.easeHover(
               GuiScreen.categoryHoverAnimations.getOrDefault(category, 0.0F),
               GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, GuiScreen.x, GuiScreen.y + 82.0F + downY, sidebarWidth, 21.325F)
                  && category != selectedCategories
         );
         GuiScreen.categoryHoverAnimations.put(category, rowHover);
         float anim = category.anim33.get();
         if (anim > 0.01F) {
            renderer2D.rect(GuiScreen.x + 4.0F, GuiScreen.y + 82.0F + downY + 1.0F, sidebarWidth - 8.0F, 19.325F, 5.0F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(13.0F * anim * mainAlpha)));
         }
         if (rowHover > 0.01F) {
            renderer2D.rect(GuiScreen.x + 4.0F, GuiScreen.y + 82.0F + downY + 1.0F, sidebarWidth - 8.0F, 19.325F, 5.0F,
                  Renderer2D.ColorUtil.rgba(255, 255, 255, (int)(10.0F * rowHover * mainAlpha)));
         }
         float iconAlpha = mainAlpha * (0.62F + 0.38F * category.anim33.get()) + 0.12F * rowHover * mainAlpha;
         float iconCenterX = GuiScreen.x + CATEGORY_ICON_X + CATEGORY_ICON_SIZE * 0.5F;
         float iconCenterY = GuiScreen.y + 92.635F + downY;
         UiIcons.category(renderer2D, category, iconCenterX, iconCenterY, CATEGORY_ICON_SIZE, Math.min(1.0F, iconAlpha));
         renderer2D.shadow(
            iconCenterX,
            iconCenterY,
            0.1F,
            0.1F,
            8.0F,
            5.7F,
            0.1F,
            UiIcons.whiteColor(iconAlpha * 0.2F)
         );
         renderer2D.text(
            FontRegistry.INTER_MEDIUM,
            GuiScreen.x + 14.0F,
            GuiScreen.y + 88.405F + downY + 7.0F + 0.2F,
            14.0F,
            category.getName(),
            textColor
         );
         downY += 22.0F;
      }

   }
}