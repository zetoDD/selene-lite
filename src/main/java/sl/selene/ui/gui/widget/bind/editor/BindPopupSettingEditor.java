package sl.selene.ui.gui.widget.bind.editor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.ui.gui.widget.bind.BindPopupRenderer;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public interface BindPopupSettingEditor {
   void layout(BindPopupRenderer.Rect var1);

   float getHeight();

   void updateState();

   void updateHover(double var1, double var3);

   boolean handleMouseClick(double var1, double var3, int var5);

   boolean handleMouseScroll(double var1, double var3, double var5, double var7);

   void render(Renderer2D var1, float var2, float var3);

   void renderOverlay(Renderer2D var1, float var2, float var3);

   boolean isBlocking();
}