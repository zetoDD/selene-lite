package sl.selene.ui.gui.component.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.module.api.Module;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.map.GuiServerMapPanel;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.ScaleHelper;
import sl.selene.util.render.math.ScaledResolution;

@Environment(EnvType.CLIENT)
public class GuiRender extends GuiScreen {

   public static void render(Renderer2D renderer2D, DrawContext p_281549_, int p_281550_, int p_282878_, float p_282465_) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.getWindow() != null) {
         int viewportWidth = client.getWindow().getFramebufferWidth();
         int viewportHeight = client.getWindow().getFramebufferHeight();
         if (viewportWidth > 0 && viewportHeight > 0) {

            float[] mouseCoords = ScaleHelper.calc((float)p_281550_, (float)p_282878_);
            int mouseX = (int)mouseCoords[0];
            int mouseY = (int)mouseCoords[1];
            int contentMouseX = mouseX;
            int contentMouseY = mouseY;
            GuiScreen.currentMouseX = contentMouseX;
            GuiScreen.currentMouseY = contentMouseY;
            GuiScreen.alphaPC.update();
            GuiScreen.settingPC.update();
            GuiScreen.alphaPC2.update();
            GuiScreen.alphaPC3.update();
            GuiScreen.openAnimation.update();
            GuiScreen.updateSliderElastic();
            GuiScreen.refreshCategoriesAndModules();
            if (GuiScreen.modules != null) {
               for (Module module : GuiScreen.modules) {
                  GuiScreen.getModuleSettingsAnimation(module).update();
                  GuiScreen.getModuleSettingsAlphaAnimation(module).update();
                  GuiScreen.getModuleBindAnimation(module).update();
               }
            }

            GuiScreen.alpha.run(1.0);
            float mainAlpha = GuiScreen.alphaPC.get();
            float openProgress = Math.max(0.0F, Math.min(1.0F, GuiScreen.openAnimation.get()));
            if (mainAlpha <= 0.001F) {
               return;
            }

            ScaledResolution scaledRes = new ScaledResolution(client);
            float scaledWidth = scaledRes.getWidth();
            float scaledHeight = scaledRes.getHeight();
            float renderScale = viewportWidth / scaledWidth;
            GuiScreen.renderScale = renderScale > 0.0F ? renderScale : 1.0F;

            float baseX = (scaledWidth - GuiScreen.width) / 2.0F;
            float baseY = (scaledHeight - GuiScreen.height) / 2.0F;
            float maxX = Math.max(0.0F, scaledWidth - GuiScreen.width);
            float maxY = Math.max(0.0F, scaledHeight - GuiScreen.height);
            GuiScreen.x = Math.max(0.0F, Math.min(maxX, baseX + GuiScreen.windowOffsetX));
            GuiScreen.y = Math.max(0.0F, Math.min(maxY, baseY + GuiScreen.windowOffsetY));
            MatrixStack pose = new MatrixStack();

            renderer2D.pushScale(GuiScreen.renderScale);
            try {
               float windowScale = (0.90F + 0.10F * openProgress) * (0.98F + 0.02F * mainAlpha);
               renderer2D.pushScale(windowScale, windowScale,
                     GuiScreen.x + GuiScreen.width * 0.5F, GuiScreen.y + GuiScreen.height * 0.5F);
               try {
                  GuiRenderBackground.renderBackground(renderer2D, pose, mainAlpha);
                  if (GuiScreen.serverMapOpen) {
                     GuiRenderLines.renderLines(renderer2D, pose, mainAlpha, true);
                     GuiServerMapPanel.renderPanel(renderer2D, pose, mainAlpha);
                  } else {
                     renderer2D.pushRoundedClipRect(
                           GuiScreen.x, GuiScreen.y,
                           GuiScreen.width, GuiScreen.HEADER_CLIP_HEIGHT,
                           8.0F, 8.0F, 0.0F, 0.0F);
                     try {
                        GuiRenderUpPanel.renderUpPanel(renderer2D, pose, mainAlpha);
                     } finally {
                        renderer2D.popClipRect();
                     }
                     renderer2D.pushRoundedClipRect(
                           GuiScreen.x, GuiScreen.y + 70.0F,
                           GuiScreen.SIDEBAR_WIDTH - GuiScreen.GAP, 176.0F,
                           8.0F, 8.0F, 8.0F, 8.0F);
                     try {
                        GuiRenderLeftPanel.renderLeftPanel(renderer2D, pose, mainAlpha);
                     } finally {
                        renderer2D.popClipRect();
                     }
                     GuiRenderLines.renderLines(renderer2D, pose, mainAlpha, false);
                     renderer2D.pushRoundedClipRect(
                           GuiScreen.x + GuiScreen.SIDEBAR_WIDTH, GuiScreen.y + GuiScreen.CONTENT_TOP,
                           GuiScreen.width - GuiScreen.SIDEBAR_WIDTH, GuiScreen.height - GuiScreen.CONTENT_TOP,
                           8.0F, 8.0F, 8.0F, 8.0F);
                     try {
                        if (GuiScreen.settingsPageOpen) {
                           GuiRenderSettings.render(renderer2D, pose, contentMouseX, contentMouseY, mainAlpha);
                        } else {
                           GuiRenderMain.renderMain(renderer2D, pose, contentMouseX, contentMouseY, mainAlpha * categoryAnimation.getOutput());
                        }
                     } finally {
                        renderer2D.popClipRect();
                     }
                     if (GuiScreen.configPopupOpen) {
                        GuiRenderConfigPopup.renderPopup(renderer2D, pose, contentMouseX, contentMouseY, mainAlpha);
                     }
                  }
               } finally {
                  renderer2D.popTransform();
               }
            } finally {
               renderer2D.popTransform();
            }
         }
      }
   }
}