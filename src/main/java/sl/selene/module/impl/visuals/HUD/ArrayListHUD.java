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
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.animation.anim.util.Animation2;
import sl.selene.util.render.math.animation.anim.util.Easings;
import sl.selene.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class ArrayListHUD {
   private static final float FONT_SIZE = 20.0F;
   private static final float ROW_HEIGHT = 24.0F;
   private static final float ROW_GAP = 2.0F;
   private static final float PILL_PADDING = 10.0F;
   private static final float PILL_RADIUS = 6.5F;
   private static final float SUFFIX_GAP = 7.0F;
   private static final float TEXT_BASELINE_OFFSET = 5.0F;
   private static final float ENTRY_ANIMATION_DURATION = 0.28F;
   private static final float ENTRY_SLIDE_DISTANCE = 18.0F;
   private static final float EDGE_MARGIN = 8.0F;
   private static final Map<Module, Animation2> entryAnimations = new HashMap<>();
   private static final Map<Module, Boolean> entryStates = new HashMap<>();

   public static MinecraftClient mc = MinecraftClient.getInstance();

   private static String suffix(Module module) {
      String suffix = module.getArrayListSuffix();
      return suffix == null ? "" : suffix;
   }

   private static float labelWidth(Renderer2D renderer, Module module) {
      String name = module.getDisplayName();
      String suffix = suffix(module);
      float width = renderer.measureText(FontRegistry.INTER_SEMIBOLD, name, FONT_SIZE).width;
      if (!suffix.isEmpty()) {
         width += SUFFIX_GAP + renderer.measureText(FontRegistry.INTER_SEMIBOLD, suffix, FONT_SIZE).width;
      }
      return width;
   }

   private static float pillWidth(Renderer2D renderer, Module module) {
      return PILL_PADDING * 2.0F + labelWidth(renderer, module);
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

      modules.sort((first, second) -> Float.compare(labelWidth(r2, second), labelWidth(r2, first)));
      renderRows(r2, modules);
   }

   private static float columnHeight(List<Module> modules) {
      float height = 0.0F;
      for (int i = 0; i < modules.size(); i++) {
         float progress = entryProgress(modules.get(i));
         height += ROW_HEIGHT * progress;
         if (i < modules.size() - 1) {
            height += ROW_GAP * progress;
         }
      }
      return height;
   }

   private static void renderRows(Renderer2D renderer, List<Module> modules) {
      float width = 0.0F;
      for (Module module : modules) {
         width = Math.max(width, pillWidth(renderer, module));
      }

      float height = Math.max(1.0F, columnHeight(modules));
      float preferredX = mc.getWindow().getFramebufferWidth() - width - EDGE_MARGIN;
      float preferredY = EDGE_MARGIN;
      DraggableManager.DragSession session = DraggableManager.getInstance().beginDrag(
            "arraylist", preferredX, preferredY, width, height
      );
      float columnRight = session.positionX() + width;
      float rowY = session.positionY();

      for (int i = 0; i < modules.size(); i++) {
         Module module = modules.get(i);
         float progress = entryProgress(module);
         if (progress > 0.001F) {
            drawRow(renderer, module, columnRight, rowY, progress);
         }
         rowY += ROW_HEIGHT * progress;
         if (i < modules.size() - 1) {
            rowY += ROW_GAP * progress;
         }
      }

      HudEditor.registerRect(session.positionX(), session.positionY(), width, height);
      DraggableManager.getInstance().endDrag(session);
   }

   private static void drawRow(Renderer2D renderer, Module module, float columnRight, float rowY, float progress) {
      String name = module.getDisplayName();
      String suffix = suffix(module);
      float pillWidth = pillWidth(renderer, module);
      float pillX = columnRight - pillWidth + ENTRY_SLIDE_DISTANCE * (progress - 1.0F);
      Hud.drawClientRect(renderer, pillX, rowY, pillWidth, ROW_HEIGHT, PILL_RADIUS, progress, 1.0F);

      int alpha = Math.round(255.0F * progress);
      float textX = pillX + PILL_PADDING;
      float textY = rowY + ROW_HEIGHT * 0.5F + TEXT_BASELINE_OFFSET;
      renderer.text(FontRegistry.INTER_SEMIBOLD, textX, textY, FONT_SIZE, name,
            ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), alpha));

      if (!suffix.isEmpty()) {
         float nameWidth = renderer.measureText(FontRegistry.INTER_SEMIBOLD, name, FONT_SIZE).width;
         float suffixX = textX + nameWidth + SUFFIX_GAP;
         renderer.text(FontRegistry.INTER_SEMIBOLD, suffixX, textY, FONT_SIZE, suffix,
               ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), alpha));
      }
   }
}

