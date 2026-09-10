package sl.selene.module.impl.visuals.HUD;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import sl.selene.Selene;
import sl.selene.event.input.MouseButtonEvent;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.ui.gui.component.render.GlassStyle;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public final class HudEditor {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final float MIN_SCALE = 0.5F;
   private static final float MAX_SCALE = 2.0F;
   private static final float POPUP_WIDTH = 224.0F;
   private static final float POPUP_HEIGHT = 104.0F;
   private static final float POPUP_MARGIN = 8.0F;
   private static final float POPUP_RADIUS = 11.0F;
   private static final float POPUP_PADDING = 14.0F;
   private static final float SLIDER_X = 14.0F;
   private static final float SLIDER_Y = 61.0F;
   private static final float SLIDER_W = 196.0F;
   private static final float SLIDER_H = 8.0F;
   private static final float SLIDER_KNOB_RADIUS = 7.0F;
   private static final Map<String, Float> elementScales = new LinkedHashMap<>();
   private static final Map<String, HudEditor.Bounds> elementBounds = new LinkedHashMap<>();
   private static final Map<String, HudEditor.Bounds> frameBounds = new HashMap<>();
   private static HudEditor.RenderContext currentContext;
   private static boolean popupOpen = false;
   private static String selectedElementId;
   private static float popupX;
   private static float popupY;
   private static boolean sliderDragging = false;

   private HudEditor() {
   }

   public static float getOriginX(String id) {
      HudEditor.Bounds b = elementBounds.get(id);
      return b != null ? b.x : 0.0F;
   }

   public static float getOriginY(String id) {
      HudEditor.Bounds b = elementBounds.get(id);
      return b != null ? b.y : 0.0F;
   }

   public static void renderElement(String id, Renderer2D renderer, Runnable renderCall) {
      if (renderer != null && renderCall != null && id != null && !id.isBlank()) {
         float scale = getScale(id);
         HudEditor.Bounds previous = elementBounds.get(id);
         float originX = previous != null ? previous.x : 0.0F;
         float originY = previous != null ? previous.y : 0.0F;
         frameBounds.remove(id);
         currentContext = new HudEditor.RenderContext(id, scale, originX, originY);
         renderer.pushScale(scale, scale, originX, originY);

         try {
            renderCall.run();
         } catch (Throwable ignored) {

            currentContext = null;

            try {
               renderCall.run();
            } catch (Throwable ignoredFallback) {
            }

            return;
         } finally {
            try {
               renderer.popScale();
            } catch (Throwable ignored) {
            }

            currentContext = null;
         }

         HudEditor.Bounds frame = frameBounds.get(id);
         if (frame != null) {
            elementBounds.put(id, frame);
         }
      }
   }

   public static void registerRect(float x, float y, float w, float h) {
      if (currentContext != null && !(w <= 0.0F) && !(h <= 0.0F)) {
         float scale = currentContext.scale;
         float transformedX = currentContext.originX + (x - currentContext.originX) * scale;
         float transformedY = currentContext.originY + (y - currentContext.originY) * scale;
         float transformedW = w * scale;
         float transformedH = h * scale;
         HudEditor.Bounds next = new HudEditor.Bounds(transformedX, transformedY, transformedW, transformedH);
         frameBounds.merge(currentContext.id, next, HudEditor::union);
      }
   }

   public static void renderPopup(Renderer2D renderer) {
      if (renderer != null && popupOpen && selectedElementId != null) {
         if (sliderDragging) {
            tickSliderDrag();
         }

         clampPopupToViewport();
         float popupWidth = popupWidth();
         float sliderWidth = sliderWidth();
         int textColor = Renderer2D.ColorUtil.getTextColor(1, 1);
         int mutedColor = ColorUtil.replAlpha(textColor, 172);
         int accentColor = Renderer2D.ColorUtil.getMainColor(1, 1);
         int trackColor = ColorUtil.replAlpha(textColor, 40);
         int valueBackground = ColorUtil.replAlpha(accentColor, 28);
         int valueOutline = ColorUtil.replAlpha(textColor, 62);
         float scale = getScale(selectedElementId);
         float progress = clamp01((scale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE));
         float sx = popupX + SLIDER_X;
         float sy = popupY + SLIDER_Y;
         float trackCenterY = sy + SLIDER_H * 0.5F;
         float knobX = sx + sliderWidth * progress;
         String valueText = String.format(java.util.Locale.ROOT, "%.2fx", scale);
         float valueWidth = 65.0F;
         float valueX = popupX + popupWidth - POPUP_PADDING - valueWidth;

         GlassStyle.panel(renderer, popupX, popupY, popupWidth, POPUP_HEIGHT, POPUP_RADIUS, POPUP_RADIUS, POPUP_RADIUS, POPUP_RADIUS, 1.0F);
         renderer.text(FontRegistry.INTER_SEMIBOLD, popupX + POPUP_PADDING, popupY + 22.0F, 16.0F,
               displayName(selectedElementId), textColor);
         renderer.text(FontRegistry.INTER_MEDIUM, popupX + POPUP_PADDING, popupY + 37.0F, 11.0F,
               "HUD scale", mutedColor);
         renderer.rect(valueX, popupY + 10.0F, valueWidth, 26.0F, 8.0F, valueBackground);
         renderer.rectOutline(valueX, popupY + 10.0F, valueWidth, 26.0F, 8.0F, valueOutline, 1.0F);
         renderer.text(FontRegistry.INTER_SEMIBOLD, valueX + valueWidth - 9.0F, popupY + 28.0F, 17.0F,
               valueText, textColor, "r");

         renderer.rect(sx, sy, sliderWidth, SLIDER_H, SLIDER_H * 0.5F, trackColor);
         if (progress > 0.0F) {
            renderer.rect(sx, sy, sliderWidth * progress, SLIDER_H, SLIDER_H * 0.5F,
                  ColorUtil.replAlpha(accentColor, 190));
         }
         renderer.circle(knobX, trackCenterY, SLIDER_KNOB_RADIUS + 1.5F, 0.0F, 1.0F,
               ColorUtil.replAlpha(accentColor, 72));
         renderer.circle(knobX, trackCenterY, SLIDER_KNOB_RADIUS, 0.0F, 1.0F, textColor);
         renderer.circle(knobX, trackCenterY, 3.0F, 0.0F, 1.0F, ColorUtil.replAlpha(accentColor, 210));

         renderer.text(FontRegistry.INTER_MEDIUM, sx, popupY + 93.0F, 11.0F, "0.50x", mutedColor);
         renderer.text(FontRegistry.INTER_MEDIUM, sx + sliderWidth, popupY + 93.0F, 11.0F, "2.00x", mutedColor, "r");
      }
   }

   public static void onMouseButton(MouseButtonEvent event) {
      if (event != null && mc.currentScreen instanceof ChatScreen) {
         float mx = (float)event.cursorX();
         float my = (float)event.cursorY();
         if (event.isPress() && event.button() == 1) {
            String hitId = findHitElement(mx, my);
            if (hitId != null) {
               selectedElementId = hitId;
               popupOpen = true;
               sliderDragging = false;
               placePopup(mx, my);
               event.cancel();
            }
         }

         if (popupOpen && event.button() == 0) {
            if (event.isPress()) {
               if (isInsideSlider(mx, my)) {
                  sliderDragging = true;
                  setScaleFromMouse(mx);
                  event.cancel();
               } else if (!isInsidePopup(mx, my)) {
                  popupOpen = false;
                  sliderDragging = false;
               }
            } else if (event.isRelease()) {
               sliderDragging = false;
            }
         }
      }
   }

   public static void closeAllPopups() {
      popupOpen = false;
      sliderDragging = false;
      selectedElementId = null;
   }

   public static Map<String, Float> snapshotScales() {
      return new LinkedHashMap<>(elementScales);
   }

   public static void loadScales(Map<String, Float> scales) {
      elementScales.clear();
      if (scales != null) {
         scales.forEach((id, scale) -> {
            if (id != null && !id.isBlank()) {
               elementScales.put(id, clampScale(scale));
            }
         });
      }
   }

   public static float getScale(String id) {
      return clampScale(elementScales.getOrDefault(id, 1.0F));
   }

   private static void setScale(String id, float scale) {
      float clamped = clampScale(scale);
      float previous = getScale(id);
      if (Math.abs(previous - clamped) > 0.0005F) {
         elementScales.put(id, clamped);
         if (Selene.get != null && Selene.get.configManager != null) {
            Selene.get.configManager.autoSave();
         }
      }
   }

   private static void tickSliderDrag() {
      if (mc.getWindow() == null) {
         sliderDragging = false;
      } else {
         long handle = mc.getWindow().getHandle();
         if (handle == 0L || GLFW.glfwGetMouseButton(handle, 0) != 1) {
            sliderDragging = false;
         } else {
            double[] x = new double[1];
            double[] y = new double[1];
            GLFW.glfwGetCursorPos(handle, x, y);
            setScaleFromMouse((float)x[0]);
         }
      }
   }

   private static void setScaleFromMouse(float mouseX) {
      if (selectedElementId != null) {
         float sx = popupX + SLIDER_X;
         float progress = clamp01((mouseX - sx) / sliderWidth());
         float scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * progress;
         setScale(selectedElementId, scale);
      }
   }

   private static boolean isInsideSlider(float mx, float my) {
      float sx = popupX + SLIDER_X;
      float sy = popupY + SLIDER_Y - 8.0F;
      float sliderWidth = sliderWidth();
      return mx >= sx - 6.0F && mx <= sx + sliderWidth + 6.0F && my >= sy && my <= sy + SLIDER_H + 16.0F;
   }

   private static boolean isInsidePopup(float mx, float my) {
      return mx >= popupX && mx <= popupX + popupWidth() && my >= popupY && my <= popupY + POPUP_HEIGHT;
   }

   private static void placePopup(float mouseX, float mouseY) {
      float width = popupWidth();
      float viewportWidth = popupViewportWidth();
      float viewportHeight = popupViewportHeight();
      float rightX = mouseX + 10.0F;
      float leftX = mouseX - width - 10.0F;
      float belowY = mouseY + 10.0F;
      float aboveY = mouseY - POPUP_HEIGHT - 10.0F;
      popupX = rightX + width <= viewportWidth - POPUP_MARGIN ? rightX : leftX;
      popupY = belowY + POPUP_HEIGHT <= viewportHeight - POPUP_MARGIN ? belowY : aboveY;
      clampPopupToViewport();
   }

   private static void clampPopupToViewport() {
      float width = popupWidth();
      float viewportWidth = popupViewportWidth();
      float viewportHeight = popupViewportHeight();
      popupX = Math.max(POPUP_MARGIN, Math.min(popupX, Math.max(POPUP_MARGIN, viewportWidth - width - POPUP_MARGIN)));
      popupY = Math.max(POPUP_MARGIN, Math.min(popupY, Math.max(POPUP_MARGIN, viewportHeight - POPUP_HEIGHT - POPUP_MARGIN)));
   }

   private static float popupWidth() {
      float viewportWidth = popupViewportWidth();
      return Math.min(POPUP_WIDTH, Math.max(170.0F, viewportWidth - POPUP_MARGIN * 2.0F));
   }

   private static float sliderWidth() {
      return Math.min(SLIDER_W, Math.max(40.0F, popupWidth() - SLIDER_X * 2.0F));
   }

   private static float popupViewportWidth() {
      if (mc.getWindow() != null && mc.getWindow().getFramebufferWidth() > 0) {
         return mc.getWindow().getFramebufferWidth();
      }
      return POPUP_WIDTH + POPUP_MARGIN * 2.0F;
   }

   private static float popupViewportHeight() {
      if (mc.getWindow() != null && mc.getWindow().getFramebufferHeight() > 0) {
         return mc.getWindow().getFramebufferHeight();
      }
      return POPUP_HEIGHT + POPUP_MARGIN * 2.0F;
   }

   private static String displayName(String id) {
      if (id == null) {
         return "HUD";
      }
      return switch (id) {
         case "arraylist" -> "Array List";
         case "notifications" -> "Notifications";
         case "information" -> "Information";
         case "targetHUD" -> "Target HUD";
         case "potions" -> "Potion List";
         case "hotbar" -> "Hotbar";
         case "keybinds" -> "Bind List";
         default -> id;
      };
   }

   private static String findHitElement(float mx, float my) {
      for (Map.Entry<String, HudEditor.Bounds> entry : elementBounds.entrySet()) {
         HudEditor.Bounds bounds = entry.getValue();
         if (bounds != null && mx >= bounds.x && mx <= bounds.x + bounds.w && my >= bounds.y && my <= bounds.y + bounds.h) {
            return entry.getKey();
         }
      }

      return DraggableManager.getInstance().findHitElement(mx, my);
   }

   private static HudEditor.Bounds union(HudEditor.Bounds a, HudEditor.Bounds b) {
      float minX = Math.min(a.x, b.x);
      float minY = Math.min(a.y, b.y);
      float maxX = Math.max(a.x + a.w, b.x + b.w);
      float maxY = Math.max(a.y + a.h, b.y + b.h);
      return new HudEditor.Bounds(minX, minY, maxX - minX, maxY - minY);
   }

   private static float clampScale(float scale) {
      if (!Float.isFinite(scale)) {
         return 1.0F;
      } else if (scale < MIN_SCALE) {
         return MIN_SCALE;
      } else {
         return scale > MAX_SCALE ? MAX_SCALE : scale;
      }
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      } else {
         return value > 1.0F ? 1.0F : value;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class RenderContext {
      private final String id;
      private final float scale;
      private final float originX;
      private final float originY;

      private RenderContext(String id, float scale, float originX, float originY) {
         this.id = id;
         this.scale = scale;
         this.originX = originX;
         this.originY = originY;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Bounds {
      private final float x;
      private final float y;
      private final float w;
      private final float h;

      private Bounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
      }
   }
}