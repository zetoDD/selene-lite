package sl.selene.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import sl.selene.Selene;
import sl.selene.module.api.Module;
import sl.selene.module.impl.visuals.Hud;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.keyboard.Keyboard;
import sl.selene.util.keyboard.ScaledResolution;
import sl.selene.util.render.animation.util.Animation;
import sl.selene.util.render.animation.util.Easings;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.ui.gui.component.render.GlassStyle;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public class KeyBindHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();
   private static final List<Module> bindings = new ArrayList<>();
   private static final Animation widthAnimation = new Animation();
   private static final Animation heightAnimation = new Animation();
   public static Animation anim2 = new Animation();

   private static boolean shouldDisplay(Module module) {
      boolean isActiveOrAnimating = module.enable || module.mAnim.get() != 0.0;
      boolean hasBind = module.bind > 0 || module.bind <= -100;
      return isActiveOrAnimating && hasBind;
   }

   private static void sortModules() {
      bindings.clear();
      bindings.addAll(
         Selene.get
            .manager
            .getModules()
            .stream()
            .filter(KeyBindHUD::shouldDisplay)
            .sorted(Comparator.comparing(module -> module.name))
            .collect(Collectors.toList())
      );
   }

   public static void keybind(Renderer2D r2) {
      ScaledResolution sr = new ScaledResolution(mc);
      sortModules();
      anim2.update();
      boolean expand = !bindings.isEmpty();
      String headerName = "Binds";
      float headerTextWidth = r2.measureText(FontRegistry.INTER_MEDIUM, headerName, 20.0F).width;
      float minWidth = 34.0F + headerTextWidth;
      float calculatedWidth = minWidth;
      if (expand) {
         for (Module module : bindings) {
            module.mAnim.update();
            float animPC = module.mAnim.get();
            if (animPC > 0.0F) {
               String keyName = Keyboard.keyName(module.bind);
               String moduleName = module.name;
               float moduleNameWidth = r2.measureText(FontRegistry.INTER_MEDIUM, moduleName, 20.0F).width;
               float keyNameWidth = r2.measureText(FontRegistry.INTER_MEDIUM, "[" + keyName + "]", 18.0F).width;
               float totalWidth = 38.0F + moduleNameWidth + keyNameWidth + 10.0F;
               calculatedWidth = Math.max(calculatedWidth, totalWidth * animPC);
            }
         }
      }

      calculatedWidth = Math.max(calculatedWidth, 110.0F);
      float headerHeight = 12.0F;
      float calculatedHeight = headerHeight;
      if (expand) {
         float offset = 0.0F;

         for (Module modulex : bindings) {
            modulex.mAnim.update();
            float animPC = modulex.mAnim.get();
            offset += 16.0F * animPC;
         }

         if (offset > 0.0F) {
            calculatedHeight = 36.0F + headerHeight + offset + 2.0F;
         }
      }

      calculatedHeight = Math.max(calculatedHeight, 36.0F);
      boolean isEmpty = bindings.isEmpty();
      boolean closeCondition = isEmpty && !(mc.currentScreen instanceof ChatScreen);
      anim2.run(closeCondition ? 0.0 : 1.0, 0.15F, Easings.QUAD_OUT);
      widthAnimation.update();
      heightAnimation.update();
      widthAnimation.run(calculatedWidth, 0.15, Easings.QUART_OUT);
      heightAnimation.run(Math.max(headerHeight, calculatedHeight - 4.0F), 0.15, Easings.QUART_OUT);
      float animatedWidth = widthAnimation.get();
      float animatedHeight = heightAnimation.get();
      if (!closeCondition || anim2.get() != 0.0F) {
         float preferredX = (sr.getWidth() - animatedWidth) / 2.0F;
         float preferredY = (sr.getHeight() - animatedHeight) / 2.0F;
         DraggableManager.DragSession session = DraggableManager.getInstance().beginDrag("keybinds", preferredX, preferredY, animatedWidth, animatedHeight);
         float x = session.positionX();
         float y = session.positionY() - 40.0F + 40.0F * anim2.get();
         HudEditor.registerRect(x, y, animatedWidth, animatedHeight);
         Hud.drawClientRect(r2, x, y, animatedWidth, animatedHeight, 13.0F, 1.0F * anim2.get(), 1.0F);
         UiIcons.binds(r2, x + 19.0F, y + 16.0F, 15.0F, anim2.get());
         r2.shadow(
            x + 17.0F,
            y + 16.0F,
            0.3F,
            0.3F,
            9.0F,
            6.5F,
            0.5F,
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(75.0F * anim2.get()))
         );
         r2.rect(x + 32.0F, y + 11.5F, 2.0F, 10.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(51.0F * anim2.get())));
         r2.text(FontRegistry.INTER_MEDIUM, x + 42.0F, y + 20.5F, 20.0F, "Binds", ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), anim2.get()));
         r2.rect(x, y + 30.0F, animatedWidth, 1.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(25.0F * anim2.get())));
         if (expand) {
            float offset = 0.0F;

            for (Module modulex : bindings) {
               modulex.mAnim.update();
               float animPC = modulex.mAnim.get();
               if (animPC > 0.0F) {
                  String keyName = Keyboard.keyName(modulex.bind);
                  String moduleName = modulex.name;
                  float keyNameWidth = r2.measureText(FontRegistry.INTER_MEDIUM, keyName, 18.0F).width;
                  int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(animPC * 255.0F));
                  int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(animPC * 255.0F));
                  float addx = 28.0F - 28.0F * animPC;
                  r2.text(FontRegistry.INTER_MEDIUM, x + 12.0F + addx, y + 40.0F + headerHeight + offset, 20.0F, moduleName, textColor);
                  GlassStyle.card(
                     r2,
                     x + animatedWidth - 16.0F - keyNameWidth + addx,
                     y + 28.0F + headerHeight + offset,
                     12.0F + keyNameWidth - 5.0F,
                     13.0F,
                     animPC
                  );
                  r2.text(
                     FontRegistry.INTER_MEDIUM,
                     x + animatedWidth - 14.2F - keyNameWidth - addx,
                     y + 37.8F + headerHeight + offset,
                     18.0F,
                     keyName,
                     mainColor
                  );
                  offset += 16.0F * animPC;
               }
            }
         }

         DraggableManager.getInstance().endDrag(session);
      }
   }
}