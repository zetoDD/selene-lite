package sl.selene.ui.gui.widget.bind.editor;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.module.api.Module;
import sl.selene.ui.gui.widget.bind.BindPopupModel;
import sl.selene.ui.gui.widget.bind.BindPopupRenderer;
import sl.selene.ui.gui.widget.settings.PopupOpenContext;
import sl.selene.ui.gui.widget.settings.SettingValueAccessor;
import sl.selene.ui.gui.widget.settings.SettingWidget;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
abstract class AbstractBindPopupSettingEditor<T> implements BindPopupSettingEditor {
   private static final PopupOpenContext NO_POPUP_CONTEXT = (module, setting, anchorX, anchorY, initialValue) -> {};
   protected static final String POPUP_LABEL = "New Value";
   private final BindPopupModel model;
   private final SettingWidget widget;
   private BindPopupRenderer.Rect bounds = new BindPopupRenderer.Rect(0.0F, 0.0F, 0.0F, 0.0F);

   AbstractBindPopupSettingEditor(BindPopupModel model, SettingWidget widget) {
      this.model = Objects.requireNonNull(model, "model");
      this.widget = Objects.requireNonNull(widget, "widget");
   }

   protected final BindPopupModel model() {
      return this.model;
   }

   protected final SettingWidget widget() {
      return this.widget;
   }

   protected final PopupOpenContext popupContext() {
      return NO_POPUP_CONTEXT;
   }

   protected static PopupOpenContext noPopupContext() {
      return NO_POPUP_CONTEXT;
   }

   protected static Module requireModule(BindPopupModel model) {
      Module module = model.module();
      if (module == null) {
         throw new IllegalStateException("Bind popup model is missing module context");
      } else {
         return module;
      }
   }

   @Override
   public void layout(BindPopupRenderer.Rect area) {
      Objects.requireNonNull(area, "area");
      this.bounds = area;
      this.widget.layout(area.x(), area.y(), area.width());
   }

   @Override
   public float getHeight() {
      return this.widget.getHeight();
   }

   @Override
   public void updateState() {
      this.widget.updateState();
   }

   @Override
   public void updateHover(double mouseX, double mouseY) {
      this.widget.updateHoverState(mouseX, mouseY);
   }

   @Override
   public boolean handleMouseClick(double mouseX, double mouseY, int button) {
      if (this.widget.isBlockingInteractions()) {
         return this.widget.handleOverlayClick(mouseX, mouseY, button) ? true : true;
      } else {
         return !this.bounds.contains(mouseX, mouseY) ? false : this.widget.handleMouseClick(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean handleMouseScroll(double mouseX, double mouseY, double horizontal, double vertical) {
      if (this.widget.isBlockingInteractions()) {
         return this.widget.handleOverlayScroll(mouseX, mouseY, horizontal, vertical) ? true : true;
      } else {
         return !this.bounds.contains(mouseX, mouseY) ? false : this.widget.handleMouseScroll(mouseX, mouseY, horizontal, vertical);
      }
   }

   @Override
   public void render(Renderer2D renderer, float alphaMultiplier, float expansionProgress) {
      this.widget.render(renderer, alphaMultiplier, expansionProgress, 0.0F);
   }

   @Override
   public void renderOverlay(Renderer2D renderer, float alphaMultiplier, float expansionProgress) {
      this.widget.renderOverlay(renderer, alphaMultiplier, expansionProgress);
   }

   @Override
   public boolean isBlocking() {
      return this.widget.isBlockingInteractions();
   }

   protected static <V> SettingValueAccessor<V> accessorFor(BindPopupModel model, V defaultValue, AbstractBindPopupSettingEditor.ValueAdapter<V> adapter) {
      Objects.requireNonNull(model, "model");
      Objects.requireNonNull(adapter, "adapter");
      return new SettingValueAccessor<V>() {
         @Override
         public V get() {
            return adapter.read(model);
         }

         @Override
         public void set(V value) {
            adapter.write(model, value);
         }

         @Override
         public V getDefault() {
            return defaultValue;
         }

         @Override
         public void reset() {
         }
      };
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   protected interface ValueAdapter<V> {
      V read(BindPopupModel var1);

      default void write(BindPopupModel model, V value) {
         model.setEditableTargetValue(value);
      }
   }
}