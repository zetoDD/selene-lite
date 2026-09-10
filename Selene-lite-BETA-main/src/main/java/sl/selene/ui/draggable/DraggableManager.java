package sl.selene.ui.draggable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import sl.selene.Selene;
import sl.selene.util.render.animation.AnimationSystem;
import sl.selene.util.render.animation.Easings;
import sl.selene.util.render.animation.SpringConfig;
import sl.selene.util.render.animation.SpringFloatAnimator;
import sl.selene.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public final class DraggableManager {
   private static final DraggableManager INSTANCE = new DraggableManager();
   private static final SpringConfig DRAG_ALPHA_SPRING = SpringConfig.of(2.5F, 0.9F);

   private static final SpringConfig DRAG_POSITION_SPRING = SpringConfig.of(3.0F, 1.0F);
   private static final float POSITION_TOLERANCE = 0.5F;
   private static final float POSITION_VELOCITY_TOLERANCE = 0.5F;

   private static final float MOMENTUM_DECEL = 0.998F;

   private static final float MOMENTUM_MIN_PROJECTION = 3.0F;
   private final Map<String, DraggableManager.DragState> dragStates = new HashMap<>();
   private final Map<String, DraggableManager.NormalizedPosition> persistedPositions = new ConcurrentHashMap<>();
   private Renderer2D renderer;
   private boolean frameActive;
   private float cursorX;
   private float cursorY;
   private boolean pointerValid;
   private boolean pointerPressed;
   private boolean uiOpen;
   private int viewportWidth;
   private int viewportHeight;

   private DraggableManager() {
   }

   public static DraggableManager getInstance() {
      return INSTANCE;
   }

   public void beginFrame(MinecraftClient client, Renderer2D renderer, int viewportWidth, int viewportHeight) {
      this.renderer = Objects.requireNonNull(renderer, "renderer");
      this.viewportWidth = Math.max(0, viewportWidth);
      this.viewportHeight = Math.max(0, viewportHeight);
      this.frameActive = true;
      this.pointerValid = false;
      this.pointerPressed = false;
      this.uiOpen = false;
      this.cursorX = 0.0F;
      this.cursorY = 0.0F;
      if (client != null) {
         this.uiOpen = client.currentScreen != null;
         Window window = client.getWindow();
         if (window != null) {
            long handle = window.getHandle();
            if (handle != 0L) {
               double[] cx = new double[1];
               double[] cy = new double[1];
               GLFW.glfwGetCursorPos(handle, cx, cy);
               double rawX = cx[0];
               double rawY = cy[0];
               if (Double.isFinite(rawX) && Double.isFinite(rawY)) {
                  float fx = (float)rawX;
                  float fy = (float)rawY;
                  if (Float.isFinite(fx) && Float.isFinite(fy)) {
                     this.cursorX = fx;
                     this.cursorY = fy;
                     this.pointerValid = true;
                  }
               }

               if (this.pointerValid && this.uiOpen) {
                  this.pointerPressed = GLFW.glfwGetMouseButton(handle, 0) == 1;
               }
            }
         }
      }
   }

   public void endFrame() {
      this.frameActive = false;
      this.renderer = null;
      this.pointerValid = false;
      this.pointerPressed = false;
      this.uiOpen = false;
      this.viewportWidth = 0;
      this.viewportHeight = 0;
   }

   public DraggableManager.DragSession beginDrag(String id, float preferredX, float preferredY, float width, float height) {
      this.ensureFrame();
      Objects.requireNonNull(id, "id");
      if (Float.isFinite(preferredX)
         && Float.isFinite(preferredY)
         && Float.isFinite(width)
         && Float.isFinite(height)
         && !(width <= 0.0F)
         && !(height <= 0.0F)
         && this.viewportWidth > 0
         && this.viewportHeight > 0) {
         String persistedId = normalizeId(id);
         DraggableManager.NormalizedPosition persisted = persistedId == null ? null : this.persistedPositions.get(persistedId);
         DraggableManager.DragState state = this.dragStates.computeIfAbsent(id, key -> new DraggableManager.DragState());
         boolean wasDragging = state.dragging;
         boolean canInteract = this.pointerValid && this.uiOpen;
         float resolvedPreferredX = this.resolvePreferredX(persisted, preferredX, width);
         float resolvedPreferredY = this.resolvePreferredY(persisted, preferredY, height);
         if (!state.dragging && !state.settling) {
            state.positionX = resolvedPreferredX;
            state.positionY = resolvedPreferredY;
         }

         if (!this.pointerPressed || !canInteract) {
            state.dragging = false;
         } else if (!state.dragging) {

            boolean hovered = containsPoint(this.cursorX, this.cursorY, state.positionX, state.positionY, width, height);
            if (hovered) {
               state.dragging = true;
               state.settling = false;
               state.offsetX = this.cursorX - state.positionX;
               state.offsetY = this.cursorY - state.positionY;
               state.posXAnimator.snapTo(state.positionX);
               state.posYAnimator.snapTo(state.positionY);
               state.clearSamples();
            }
         }

         float maxX = Math.max(0.0F, this.viewportWidth - width);
         float maxY = Math.max(0.0F, this.viewportHeight - height);
         boolean activelyDragging = state.dragging && this.pointerPressed && canInteract;
         float positionX;
         float positionY;
         if (activelyDragging) {
            float rawX = this.cursorX - state.offsetX;
            float rawY = this.cursorY - state.offsetY;
            positionX = clamp(rawX, 0.0F, maxX);
            positionY = clamp(rawY, 0.0F, maxY);
            state.positionX = positionX;
            state.positionY = positionY;

            state.posXAnimator.snapTo(positionX);
            state.posYAnimator.snapTo(positionY);
            state.pushSample(rawX, rawY);
         } else {
            if (wasDragging) {

               float[] vel = state.releaseVelocity();
               float projX = projectMomentum(vel[0]);
               float projY = projectMomentum(vel[1]);
               float targetX = clamp(state.positionX + projX, 0.0F, maxX);
               float targetY = clamp(state.positionY + projY, 0.0F, maxY);
               if (Math.abs(projX) > MOMENTUM_MIN_PROJECTION || Math.abs(projY) > MOMENTUM_MIN_PROJECTION) {
                  state.settling = true;
                  state.posXAnimator.setTarget(targetX);
                  state.posYAnimator.setTarget(targetY);
                  positionX = clamp(state.posXAnimator.getValue(), 0.0F, maxX);
                  positionY = clamp(state.posYAnimator.getValue(), 0.0F, maxY);
                  this.storePersistedPosition(persistedId, targetX, targetY);
               } else {
                  state.settling = false;
                  positionX = clamp(state.positionX, 0.0F, maxX);
                  positionY = clamp(state.positionY, 0.0F, maxY);
                  this.storePersistedPosition(persistedId, positionX, positionY);
               }
               state.clearSamples();
            } else if (state.settling) {

               positionX = clamp(state.posXAnimator.getValue(), 0.0F, maxX);
               positionY = clamp(state.posYAnimator.getValue(), 0.0F, maxY);
               if (state.posXAnimator.isSettled() && state.posYAnimator.isSettled()) {
                  state.settling = false;
                  this.storePersistedPosition(persistedId, positionX, positionY);
               }
            } else {
               positionX = clamp(state.positionX, 0.0F, maxX);
               positionY = clamp(state.positionY, 0.0F, maxY);
            }
            state.positionX = positionX;
            state.positionY = positionY;
            state.dragging = false;
         }
         state.lastWidth = width;
         state.lastHeight = height;

         float targetAlpha = activelyDragging ? 0.65F : 1.0F;
         state.alphaAnimator.setTarget(targetAlpha);
         float animatedAlpha = clamp01(state.alphaAnimator.getValue());
         boolean alphaAdjusted = false;
         if (shouldApplyAnimatedAlpha(state, animatedAlpha, targetAlpha, activelyDragging)) {
            this.renderer.pushAlpha(animatedAlpha);
            alphaAdjusted = true;
         }

         return new DraggableManager.DragSession(id, positionX, positionY, width, height, activelyDragging, alphaAdjusted);
      } else {
         this.disposeDragState(this.dragStates.remove(id));
         return new DraggableManager.DragSession(id, preferredX, preferredY, width, height, false, false);
      }
   }

   public void endDrag(DraggableManager.DragSession session) {
      if (session != null) {
         if (session.alphaAdjusted() && this.renderer != null) {
            this.renderer.popAlpha();
         }

         if (!session.dragging()) {
            DraggableManager.DragState state = this.dragStates.get(session.id());
            if (state == null) {
               this.dragStates.remove(session.id());
               return;
            }

            if (this.canDisposeState(state)) {
               this.disposeDragState(this.dragStates.remove(session.id()));
            }
         }
      }
   }

   public void loadNormalizedPositions(Map<String, DraggableManager.NormalizedPosition> positions) {
      this.disposeAllDragStates();
      this.dragStates.clear();
      this.persistedPositions.clear();
      if (positions != null && !positions.isEmpty()) {
         for (Entry<String, DraggableManager.NormalizedPosition> entry : positions.entrySet()) {
            String id = normalizeId(entry.getKey());
            DraggableManager.NormalizedPosition position = entry.getValue();
            if (id != null && position != null) {
               try {
                  this.persistedPositions.put(id, new DraggableManager.NormalizedPosition(position.x(), position.y()));
               } catch (IllegalArgumentException var7) {
               }
            }
         }
      }
   }

   public String findHitElement(float x, float y) {
      if (!Float.isFinite(x) || !Float.isFinite(y) || this.dragStates.isEmpty()) {
         return null;
      }

      String hitId = null;
      for (Entry<String, DraggableManager.DragState> entry : this.dragStates.entrySet()) {
         DraggableManager.DragState state = entry.getValue();
         if (state != null
            && state.lastWidth > 0.0F
            && state.lastHeight > 0.0F
            && containsPoint(x, y, state.positionX, state.positionY, state.lastWidth, state.lastHeight)) {
            hitId = entry.getKey();
         }
      }

      return hitId;
   }

   public Map<String, DraggableManager.NormalizedPosition> snapshotNormalizedPositions() {
      if (this.persistedPositions.isEmpty()) {
         return Map.of();
      } else {
         List<String> keys = new ArrayList<>(this.persistedPositions.keySet());
         keys.sort(String.CASE_INSENSITIVE_ORDER);
         Map<String, DraggableManager.NormalizedPosition> snapshot = new LinkedHashMap<>();

         for (String key : keys) {
            DraggableManager.NormalizedPosition position = this.persistedPositions.get(key);
            if (position != null) {
               snapshot.put(key, position);
            }
         }

         return Collections.unmodifiableMap(snapshot);
      }
   }

   private void ensureFrame() {
      if (!this.frameActive || this.renderer == null) {
         throw new IllegalStateException("beginFrame must be called before beginDrag");
      }
   }

   private float resolvePreferredX(DraggableManager.NormalizedPosition persisted, float fallback, float width) {
      if (persisted != null && this.viewportWidth > 0) {
         float max = Math.max(0.0F, this.viewportWidth - width);
         return clamp(persisted.x() * this.viewportWidth, 0.0F, max);
      } else {
         return fallback;
      }
   }

   private float resolvePreferredY(DraggableManager.NormalizedPosition persisted, float fallback, float height) {
      if (persisted != null && this.viewportHeight > 0) {
         float max = Math.max(0.0F, this.viewportHeight - height);
         return clamp(persisted.y() * this.viewportHeight, 0.0F, max);
      } else {
         return fallback;
      }
   }

   private void storePersistedPosition(String normalizedId, float x, float y) {
      if (this.viewportWidth > 0 && this.viewportHeight > 0) {
         if (normalizedId != null) {
            float normalizedX = x / this.viewportWidth;
            float normalizedY = y / this.viewportHeight;
            if (Float.isFinite(normalizedX) && Float.isFinite(normalizedY)) {
               DraggableManager.NormalizedPosition updated;
               try {
                  updated = new DraggableManager.NormalizedPosition(normalizedX, normalizedY);
               } catch (IllegalArgumentException var10) {
                  return;
               }

               DraggableManager.NormalizedPosition previous = this.persistedPositions.get(normalizedId);
               if (!approximatelyEquals(previous, updated)) {
                  this.persistedPositions.put(normalizedId, updated);

                  try {
                     if (Selene.get != null && Selene.get.configManager != null) {
                        Selene.get.configManager.autoSave();
                     }
                  } catch (Exception var9) {
                  }
               }
            }
         }
      }
   }

   private static boolean approximatelyEquals(DraggableManager.NormalizedPosition first, DraggableManager.NormalizedPosition second) {
      if (first == second) {
         return true;
      } else {
         return first != null && second != null ? Math.abs(first.x() - second.x()) <= 5.0E-4F && Math.abs(first.y() - second.y()) <= 5.0E-4F : false;
      }
   }

   private static boolean approximatelyEquals(float first, float second, float tolerance) {
      return Math.abs(first - second) <= tolerance;
   }

   private static SpringFloatAnimator createAlphaAnimator() {
      SpringFloatAnimator animator = new SpringFloatAnimator(AnimationSystem.getInstance(), DRAG_ALPHA_SPRING, 1.0F, 0.0F, 1.0F, 5.0E-4F, 5.0E-4F);
      animator.setOutputTransform(Easings.SMOOTH_STEP);
      return animator;
   }

   private static SpringFloatAnimator createPositionAnimator() {

      return new SpringFloatAnimator(AnimationSystem.getInstance(), DRAG_POSITION_SPRING, 0.0F, 0.0F, 1.0E6F,
            POSITION_TOLERANCE, POSITION_VELOCITY_TOLERANCE);
   }

   private static float projectMomentum(float velocityPxPerSec) {
      if (!Float.isFinite(velocityPxPerSec)) {
         return 0.0F;
      }
      return (float) ((velocityPxPerSec / 1000.0) * MOMENTUM_DECEL / (1.0 - MOMENTUM_DECEL));
   }

   private static boolean shouldApplyAnimatedAlpha(DraggableManager.DragState state, float animatedAlpha, float targetAlpha, boolean activelyDragging) {
      if (state == null) {
         return false;
      } else if (activelyDragging) {
         return true;
      } else if (animatedAlpha < 0.999F) {
         return true;
      } else {
         return targetAlpha < 0.999F ? true : !state.alphaAnimator.isSettled();
      }
   }

   private boolean canDisposeState(DraggableManager.DragState state) {
      if (state == null) {
         return true;
      } else {
         if (state.settling) {
            return false;

         }
         float target = state.alphaAnimator.getTarget();
         float rawValue = state.alphaAnimator.getRawValue();
         boolean restingTarget = approximatelyEquals(target, 1.0F, 5.0E-4F);
         boolean restingValue = approximatelyEquals(rawValue, 1.0F, 5.0E-4F);
         return restingTarget && restingValue && state.alphaAnimator.isSettled();
      }
   }

   private void disposeAllDragStates() {
      if (!this.dragStates.isEmpty()) {
         for (DraggableManager.DragState state : this.dragStates.values()) {
            this.disposeDragState(state);
         }
      }
   }

   private void disposeDragState(DraggableManager.DragState state) {
      if (state != null) {
         state.alphaAnimator.snapTo(1.0F);
         state.posXAnimator.snapTo(state.posXAnimator.getValue());
         state.posYAnimator.snapTo(state.posYAnimator.getValue());
         state.settling = false;
      }
   }

   private static boolean containsPoint(float px, float py, float x, float y, float width, float height) {
      return px >= x && px <= x + width && py >= y && py <= y + height;
   }

   private static float clamp(float value, float min, float max) {
      if (value < min) {
         return min;
      } else {
         return value > max ? max : value;
      }
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      } else {
         return value > 1.0F ? 1.0F : value;
      }
   }

   private static String normalizeId(String id) {
      if (id == null) {
         return null;
      } else {
         String trimmed = id.trim();
         return trimmed.isEmpty() ? null : trimmed;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class DragSession {
      private final String id;
      private final float positionX;
      private final float positionY;
      private final float width;
      private final float height;
      private final boolean dragging;
      private final boolean alphaAdjusted;

      private DragSession(String id, float positionX, float positionY, float width, float height, boolean dragging, boolean alphaAdjusted) {
         this.id = id;
         this.positionX = positionX;
         this.positionY = positionY;
         this.width = width;
         this.height = height;
         this.dragging = dragging;
         this.alphaAdjusted = alphaAdjusted;
      }

      public String id() {
         return this.id;
      }

      public float positionX() {
         return this.positionX;
      }

      public float positionY() {
         return this.positionY;
      }

      public float width() {
         return this.width;
      }

      public float height() {
         return this.height;
      }

      public boolean dragging() {
         return this.dragging;
      }

      public boolean alphaAdjusted() {
         return this.alphaAdjusted;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class DragState {
      private boolean dragging;
      private boolean settling;
      private float offsetX;
      private float offsetY;
      private float positionX;
      private float positionY;
      private float lastWidth;
      private float lastHeight;
      private final SpringFloatAnimator alphaAnimator = DraggableManager.createAlphaAnimator();
      private final SpringFloatAnimator posXAnimator = DraggableManager.createPositionAnimator();
      private final SpringFloatAnimator posYAnimator = DraggableManager.createPositionAnimator();

      private final float[] sampleTimes = new float[4];
      private final float[] sampleXs = new float[4];
      private final float[] sampleYs = new float[4];
      private int sampleIdx;
      private boolean samplesFull;

      void clearSamples() {
         this.sampleIdx = 0;
         this.samplesFull = false;
      }

      void pushSample(float x, float y) {
         float t = (float) System.currentTimeMillis();
         this.sampleTimes[this.sampleIdx] = t;
         this.sampleXs[this.sampleIdx] = x;
         this.sampleYs[this.sampleIdx] = y;
         this.sampleIdx = (this.sampleIdx + 1) % 4;
         if (this.sampleIdx == 0) {
            this.samplesFull = true;
         }
      }

      float[] releaseVelocity() {
         if (this.sampleIdx == 0 && !this.samplesFull) {
            return new float[] { 0.0F, 0.0F };
         }
         int oldest = this.samplesFull ? this.sampleIdx : 0;
         int newest = (this.sampleIdx - 1 + 4) % 4;
         float dt = (this.sampleTimes[newest] - this.sampleTimes[oldest]) / 1000.0F;
         if (dt <= 0.0F) {
            return new float[] { 0.0F, 0.0F };
         }
         float vx = (this.sampleXs[newest] - this.sampleXs[oldest]) / dt;
         float vy = (this.sampleYs[newest] - this.sampleYs[oldest]) / dt;
         return new float[] { vx, vy };
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class NormalizedPosition {
      private final float x;
      private final float y;

      public NormalizedPosition(float x, float y) {
         if (Float.isFinite(x) && Float.isFinite(y)) {
            this.x = DraggableManager.clamp01(x);
            this.y = DraggableManager.clamp01(y);
         } else {
            throw new IllegalArgumentException("Normalized coordinates must be finite");
         }
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }
   }
}