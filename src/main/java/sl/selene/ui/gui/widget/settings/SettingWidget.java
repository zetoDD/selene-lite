package sl.selene.ui.gui.widget.settings;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.setting.Setting;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public interface SettingWidget {
   void updateState();

   default void updateVisibility(boolean visible) {
   }

   void layout(float var1, float var2, float var3);

   float getHeight();

   default float getCurrentHeight() {
      return this.getHeight();
   }

   default void render(Renderer2D renderer, float alphaMultiplier, float expansionProgress) {
      this.render(renderer, alphaMultiplier, expansionProgress, 0.0F);
   }

   default void render(Renderer2D renderer, float alphaMultiplier, float expansionProgress, float scrollOffset) {
      this.render(renderer, alphaMultiplier, expansionProgress);
   }

   void updateHoverState(double var1, double var3);

   default void renderOverlay(Renderer2D renderer, float alphaMultiplier, float expansionProgress) {
   }

   default boolean isBlockingInteractions() {
      return false;
   }

   default boolean handleOverlayClick(double mouseX, double mouseY, int button) {
      return false;
   }

   default boolean handleOverlayScroll(double mouseX, double mouseY, double horizontal, double vertical) {
      return false;
   }

   default void collapse() {
   }

   boolean handleMouseClick(double var1, double var3, int var5);

   default boolean handleMouseScroll(double mouseX, double mouseY, double horizontal, double vertical) {
      return false;
   }

   default Setting getSetting() {
      return null;
   }

   default boolean supportsBinding() {
      return false;
   }
}