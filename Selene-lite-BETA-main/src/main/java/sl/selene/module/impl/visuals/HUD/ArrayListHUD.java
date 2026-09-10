package sl.selene.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import sl.selene.Selene;
import sl.selene.module.api.Category;
import sl.selene.module.api.Module;
import sl.selene.module.impl.visuals.Hud;
import sl.selene.module.impl.visuals.HUD.HudEditor;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.animation.anim.util.Animation2;
import sl.selene.util.render.math.animation.anim.util.Easings;
import sl.selene.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class ArrayListHUD {
   private static final float FONT_SIZE = 20.0F;
   private static final float HEADER_FONT_SIZE = 18.0F;
   private static final float HEADER_BOTTOM = 25.0F;
   private static final float ROW_TOP = 31.0F;
   private static final float ROW_HEIGHT = 22.0F;
   private static final float ROW_GAP = 4.0F;
   private static final float PANEL_PADDING_RIGHT = 14.0F;
   private static final float TEXT_LEFT = 24.0F;
   private static final float STATUS_CENTER_X = 13.0F;
   private static final float PANEL_BOTTOM_PADDING = 8.0F;
   private static final float RADIUS = 13.0F;
   private static final float PLAIN_FONT_SIZE = 18.0F;
   private static final float PLAIN_ROW_HEIGHT = 15.0F;
   private static final float PLAIN_TEXT_BASELINE = 11.0F;
   private static final float ENTRY_ANIMATION_DURATION = 0.28F;
   private static final float ENTRY_SLIDE_DISTANCE = 16.0F;
   private static final int PLAIN_TOP_COLOR = Renderer2D.ColorUtil.rgba(92, 198, 161, 255);
   private static final int PLAIN_BOTTOM_COLOR = Renderer2D.ColorUtil.rgba(46, 145, 110, 255);
   private static final Map<Module, Animation2> entryAnimations = new HashMap<>();
   private static final Map<Module, Boolean> entryStates = new HashMap<>();

   public static MinecraftClient mc = MinecraftClient.getInstance();

   private static String suffix(Module module) {
      String suffix = module.getArrayListSuffix();
      return suffix == null ? "" : suffix;
   }

   private static String plainLabel(Module module) {
      String name = module.getDisplayName();
      String suffix = suffix(module);
      return suffix.isEmpty() ? name : name + " " + suffix;
   }

   private static float rowLabelWidth(Renderer2D renderer, Module module) {
      String name = module.getDisplayName();
      String suffix = suffix(module);
      float width = renderer.measureText(FontRegistry.INTER_SEMIBOLD, name, FONT_SIZE).width;
      if (!suffix.isEmpty()) {
         width += 7.0F + renderer.measureText(FontRegistry.INTER_SEMIBOLD, suffix, FONT_SIZE).width;
      }
      return width;
   }

   private static float rowWidth(Renderer2D renderer, Module module) {
      return TEXT_LEFT + rowLabelWidth(renderer, module) + PANEL_PADDING_RIGHT;
   }

   private static float plainLabelWidth(Renderer2D renderer, Module module) {
      return renderer.measureText(FontRegistry.INTER_MEDIUM, plainLabel(module), PLAIN_FONT_SIZE).width;
   }

   private static List<Module> animatedModules() {
      List<Module> modules = new ArrayList<>();
      if (Selene.get == null || Selene.get.manager == null) {
         return modules;
      }

      for (Module module : Selene.get.manager.module) {
         if (module.category == Category.Visuals) {
            continue;
         }

         boolean enabled = module.enable;
         Animation2 animation = entryAnimations.computeIfAbsent(module, key -> new Animation2());
         Boolean previousState = entryStates.get(module);
         if (previousState == null) {
            entryStates.put(module, enabled);
            animation.set(enabled ? 1.0 : 0.0);
         } else if (previousState.booleanValue() != enabled) {
            entryStates.put(module, enabled);
            animation.run(enabled ? 1.0 : 0.0, ENTRY_ANIMATION_DURATION, Easings.QUART_OUT);
         }

         animation.update();
         if (enabled || entryProgress(module) > 0.01F) {
            modules.add(module);
         }
      }
      return modules;
   }

   private static float entryProgress(Module module) {
      Animation2 animation = entryAnimations.get(module);
      if (animation == null) {
         return module.enable ? 1.0F : 0.0F;
      }
      float progress = animation.get();
      if (progress < 0.0F) {
         return 0.0F;
      }
      return progress > 1.0F ? 1.0F : progress;
   }

   public static void arraylist(Renderer2D r2) {
      if (mc.player == null || mc.world == null || mc.getWindow() == null
            || Selene.get == null || Selene.get.manager == null) {
         return;
      }

      List<Module> modules = animatedModules();
      if (modules.isEmpty()) {
         return;
      }

      modules.sort((first, second) -> Float.compare(rowLabelWidth(r2, second), rowLabelWidth(r2, first)));
      renderPlain(r2, modules);
   }

   private static void renderPlain(Renderer2D renderer, List<Module> modules) {
      float maxWidth = 0.0F;
      float height = 0.0F;
      for (Module module : modules) {
         maxWidth = Math.max(maxWidth, plainLabelWidth(renderer, module));
         height += PLAIN_ROW_HEIGHT * entryProgress(module);
      }

      float width = maxWidth + 2.0F;
      height = Math.max(1.0F, height);
      DraggableManager.DragSession session = DraggableManager.getInstance().beginDrag(
            "arraylist", 8.0F, 8.0F, width, height
      );
      float x = session.positionX();
      float y = session.positionY();
      float rowY = y;

      for (int i = 0; i < modules.size(); i++) {
         Module module = modules.get(i);
         float progress = entryProgress(module);
         float rowProgress = modules.size() <= 1 ? 0.0F : (float)i / (modules.size() - 1);
         int textColor = ColorUtil.replAlpha(ColorUtil.overCol(PLAIN_TOP_COLOR, PLAIN_BOTTOM_COLOR, rowProgress),
               Math.round(255.0F * progress));
         int shadowColor = Renderer2D.ColorUtil.rgba(0, 0, 0, Math.round(180.0F * progress));
         String label = plainLabel(module);
         renderer.text(FontRegistry.INTER_MEDIUM, x + 1.0F, rowY + PLAIN_TEXT_BASELINE + 1.0F,
               PLAIN_FONT_SIZE, label, shadowColor);
         renderer.text(FontRegistry.INTER_MEDIUM, x, rowY + PLAIN_TEXT_BASELINE,
               PLAIN_FONT_SIZE, label, textColor);
         rowY += PLAIN_ROW_HEIGHT * progress;
      }

      HudEditor.registerRect(x, y, width, height);
      DraggableManager.getInstance().endDrag(session);
   }

   private static float animatedRowsHeight(List<Module> modules, float rowHeight, float gap) {
      float height = 0.0F;
      for (int i = 0; i < modules.size(); i++) {
         float progress = entryProgress(modules.get(i));
         height += rowHeight * progress;
         if (i < modules.size() - 1) {
            height += gap * progress;
         }
      }
      return height;
   }

   private static void renderUnified(Renderer2D renderer, List<Module> modules) {
      float maxLabelWidth = 0.0F;
      for (Module module : modules) {
         maxLabelWidth = Math.max(maxLabelWidth, rowLabelWidth(renderer, module));
      }

      String header = "Active modules";
      float headerWidth = renderer.measureText(FontRegistry.INTER_SEMIBOLD, header, HEADER_FONT_SIZE).width;
      float panelWidth = Math.max(110.0F, Math.max(headerWidth + 28.0F, TEXT_LEFT + maxLabelWidth + PANEL_PADDING_RIGHT));
      float panelHeight = ROW_TOP + animatedRowsHeight(modules, ROW_HEIGHT, 0.0F) + PANEL_BOTTOM_PADDING;
      panelHeight = Math.max(ROW_TOP + PANEL_BOTTOM_PADDING + 1.0F, panelHeight);
      float preferredX = mc.getWindow().getFramebufferWidth() - panelWidth - 14.0F;
      float preferredY = 14.0F;
      DraggableManager.DragSession session = DraggableManager.getInstance().beginDrag(
            "arraylist", preferredX, preferredY, panelWidth, panelHeight
      );
      float x = session.positionX();
      float y = session.positionY();

      int dividerColor = Renderer2D.ColorUtil.rgba(255, 255, 255, 42);
      rendererHudPanel(renderer, x, y, panelWidth, panelHeight);
      renderer.text(FontRegistry.INTER_SEMIBOLD, x + 14.0F, y + 18.0F, HEADER_FONT_SIZE, header,
            Renderer2D.ColorUtil.getTextColor(1, 1));
      renderer.rect(x + 12.0F, y + HEADER_BOTTOM, panelWidth - 24.0F, 1.0F, 0.5F, dividerColor);

      float rowY = y + ROW_TOP;
      for (Module module : modules) {
         float progress = entryProgress(module);
         renderModuleRow(renderer, module, x, rowY, progress);
         rowY += ROW_HEIGHT * progress;
      }

      HudEditor.registerRect(x, y, panelWidth, panelHeight);
      DraggableManager.getInstance().endDrag(session);
   }

   private static void renderSeparated(Renderer2D renderer, List<Module> modules) {
      float maxWidth = 0.0F;
      for (Module module : modules) {
         maxWidth = Math.max(maxWidth, rowWidth(renderer, module));
      }

      float panelHeight = animatedRowsHeight(modules, ROW_HEIGHT, ROW_GAP);
      panelHeight = Math.max(1.0F, panelHeight);
      float preferredX = mc.getWindow().getFramebufferWidth() - maxWidth - 14.0F;
      float preferredY = 14.0F;
      DraggableManager.DragSession session = DraggableManager.getInstance().beginDrag(
            "arraylist", preferredX, preferredY, maxWidth, panelHeight
      );
      float x = session.positionX();
      float y = session.positionY();
      float rowY = y;

      for (Module module : modules) {
         float progress = entryProgress(module);
         float width = rowWidth(renderer, module);
         float rowX = x + maxWidth - width;
         Hud.drawClientRect(renderer, rowX, rowY, width, ROW_HEIGHT, 10.0F, progress, 1.0F);
         renderModuleRow(renderer, module, rowX, rowY, progress);
         rowY += (ROW_HEIGHT + ROW_GAP) * progress;
      }

      HudEditor.registerRect(x, y, maxWidth, panelHeight);
      DraggableManager.getInstance().endDrag(session);
   }

   private static void rendererHudPanel(Renderer2D renderer, float x, float y, float width, float height) {
      Hud.drawClientRect(renderer, x, y, width, height, RADIUS, 1.0F, 1.0F);
   }

   private static void renderModuleRow(Renderer2D renderer, Module module, float x, float y, float progress) {
      String name = module.getDisplayName();
      String suffix = suffix(module);
      float slideX = ENTRY_SLIDE_DISTANCE * (progress - 1.0F);
      float textX = x + TEXT_LEFT + slideX;
      float textY = y + 16.0F;
      int textColor = ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), Math.round(255.0F * progress));
      int shadowColor = Renderer2D.ColorUtil.rgba(0, 0, 0, Math.round(130.0F * progress));
      int dotColor = Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(220.0F * progress));
      renderer.circle(x + STATUS_CENTER_X + slideX, y + ROW_HEIGHT * 0.5F, 2.25F, 0.0F, 1.0F, dotColor);
      renderer.text(FontRegistry.INTER_SEMIBOLD, textX + 1.0F, textY + 1.0F, FONT_SIZE, name, shadowColor);
      renderer.text(FontRegistry.INTER_SEMIBOLD, textX, textY, FONT_SIZE, name, textColor);
      if (!suffix.isEmpty()) {
         float nameWidth = renderer.measureText(FontRegistry.INTER_SEMIBOLD, name, FONT_SIZE).width;
         float suffixX = textX + nameWidth + 7.0F;
         renderer.text(FontRegistry.INTER_SEMIBOLD, suffixX + 1.0F, textY + 1.0F, FONT_SIZE, suffix, shadowColor);
         renderer.text(FontRegistry.INTER_SEMIBOLD, suffixX, textY, FONT_SIZE, suffix, textColor);
      }
   }
}