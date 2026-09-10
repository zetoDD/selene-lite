package sl.selene.ui.gui.widget.popup;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PopupPositioner {
   private final float positionMargin;
   private final float cursorHorizontalOffset;
   private final float cursorVerticalOffset;

   public PopupPositioner(float positionMargin, float cursorHorizontalOffset, float cursorVerticalOffset) {
      if (!Float.isFinite(positionMargin) || positionMargin < 0.0F) {
         throw new IllegalArgumentException("positionMargin must be a non-negative finite value");
      } else if (!Float.isFinite(cursorHorizontalOffset) || cursorHorizontalOffset < 0.0F) {
         throw new IllegalArgumentException("cursorHorizontalOffset must be a non-negative finite value");
      } else if (Float.isFinite(cursorVerticalOffset) && !(cursorVerticalOffset < 0.0F)) {
         this.positionMargin = positionMargin;
         this.cursorHorizontalOffset = cursorHorizontalOffset;
         this.cursorVerticalOffset = cursorVerticalOffset;
      } else {
         throw new IllegalArgumentException("cursorVerticalOffset must be a non-negative finite value");
      }
   }

   public PopupPositioner.PopupPlacement resolve(double anchorX, double anchorY, float popupWidth, float popupHeight, int viewportWidth, int viewportHeight) {
      return this.resolve(anchorX, anchorY, popupWidth, popupHeight, viewportWidth, viewportHeight, 1.0F);
   }

   public PopupPositioner.PopupPlacement resolve(
      double anchorX, double anchorY, float popupWidth, float popupHeight, int viewportWidth, int viewportHeight, float layoutScale
   ) {
      float sanitizedViewportWidth = Math.max(1, viewportWidth);
      float sanitizedViewportHeight = Math.max(1, viewportHeight);
      float sanitizedWidth = Math.max(0.0F, popupWidth);
      float sanitizedHeight = Math.max(0.0F, popupHeight);
      float sanitizedScale = sanitizeScale(layoutScale);
      float anchorXValue = this.sanitizeAnchor(anchorX, sanitizedViewportWidth);
      float anchorYValue = this.sanitizeAnchor(anchorY, sanitizedViewportHeight);
      float resolvedX = this.resolveHorizontal(anchorXValue, sanitizedWidth, sanitizedViewportWidth, sanitizedScale);
      float resolvedY = this.resolveVertical(anchorYValue, sanitizedHeight, sanitizedViewportHeight, sanitizedScale);
      return new PopupPositioner.PopupPlacement(resolvedX, resolvedY);
   }

   private float resolveHorizontal(float anchor, float popupWidth, float viewportWidth, float layoutScale) {
      float margin = this.positionMargin;
      float sanitizedAnchor = clamp(anchor, margin, Math.max(margin, viewportWidth - margin));
      float minLeft = this.computeScaledMinCoordinate(viewportWidth, margin, layoutScale);
      float maxLeft = this.computeScaledMaxHorizontal(popupWidth, viewportWidth, margin, layoutScale);
      if (maxLeft < minLeft) {
         float fallbackMax = Math.max(margin, viewportWidth - margin - popupWidth);
         float fallbackCenter = viewportWidth * 0.5F - popupWidth * 0.5F;
         return clamp(fallbackCenter, margin, fallbackMax);
      } else {
         float rightCandidate = sanitizedAnchor + this.cursorHorizontalOffset;
         if (rightCandidate >= minLeft && rightCandidate <= maxLeft) {
            return rightCandidate;
         } else {
            float leftCandidate = sanitizedAnchor - this.cursorHorizontalOffset - popupWidth;
            if (leftCandidate >= minLeft && leftCandidate <= maxLeft) {
               return leftCandidate;
            } else {
               float centered = sanitizedAnchor - popupWidth * 0.5F;
               return clamp(centered, minLeft, maxLeft);
            }
         }
      }
   }

   private float resolveVertical(float anchor, float popupHeight, float viewportHeight, float layoutScale) {
      float margin = this.positionMargin;
      float sanitizedAnchor = clamp(anchor, margin, Math.max(margin, viewportHeight - margin));
      float minTop = this.computeScaledMinCoordinate(viewportHeight, margin, layoutScale);
      float maxTop = this.computeScaledMaxVertical(popupHeight, viewportHeight, margin, layoutScale);
      if (maxTop < minTop) {
         float fallbackMax = Math.max(margin, viewportHeight - margin - popupHeight);
         float fallbackCenter = viewportHeight * 0.5F - popupHeight * 0.5F;
         return clamp(fallbackCenter, margin, fallbackMax);
      } else {
         float belowCandidate = sanitizedAnchor + this.cursorVerticalOffset;
         if (belowCandidate >= minTop && belowCandidate <= maxTop) {
            return belowCandidate;
         } else {
            float aboveCandidate = sanitizedAnchor - this.cursorVerticalOffset - popupHeight;
            if (aboveCandidate >= minTop && aboveCandidate <= maxTop) {
               return aboveCandidate;
            } else {
               float centered = sanitizedAnchor - popupHeight * 0.5F;
               return clamp(centered, minTop, maxTop);
            }
         }
      }
   }

   private float computeScaledMinCoordinate(float viewportSize, float margin, float layoutScale) {
      float sanitizedScale = sanitizeScale(layoutScale);
      if (Float.isFinite(sanitizedScale) && !(sanitizedScale <= 0.001F)) {
         float center = viewportSize * 0.5F;
         return center + (margin - center) / sanitizedScale;
      } else {
         return margin;
      }
   }

   private float computeScaledMaxHorizontal(float popupWidth, float viewportWidth, float margin, float layoutScale) {
      float sanitizedScale = sanitizeScale(layoutScale);
      if (Float.isFinite(sanitizedScale) && !(sanitizedScale <= 0.001F)) {
         float center = viewportWidth * 0.5F;
         float rightBound = viewportWidth - margin;
         return center + (rightBound - center) / sanitizedScale - popupWidth;
      } else {
         return viewportWidth - margin - popupWidth;
      }
   }

   private float computeScaledMaxVertical(float popupHeight, float viewportHeight, float margin, float layoutScale) {
      float sanitizedScale = sanitizeScale(layoutScale);
      if (Float.isFinite(sanitizedScale) && !(sanitizedScale <= 0.001F)) {
         float center = viewportHeight * 0.5F;
         float bottomBound = viewportHeight - margin;
         return center + (bottomBound - center) / sanitizedScale - popupHeight;
      } else {
         return viewportHeight - margin - popupHeight;
      }
   }

   private float sanitizeAnchor(double coordinate, float viewportSize) {
      float sanitized = sanitizeCoordinate(coordinate);
      if (Float.isNaN(sanitized)) {
         return viewportSize * 0.5F;
      } else {
         float margin = this.positionMargin;
         return clamp(sanitized, margin, Math.max(margin, viewportSize - margin));
      }
   }

   private static float sanitizeCoordinate(double coordinate) {
      if (!Double.isFinite(coordinate)) {
         return Float.NaN;
      } else if (coordinate > Float.MAX_VALUE) {
         return Float.MAX_VALUE;
      } else {
         return coordinate < -Float.MAX_VALUE ? -Float.MAX_VALUE : (float)coordinate;
      }
   }

   private static float sanitizeScale(float layoutScale) {
      if (!Float.isFinite(layoutScale)) {
         return 1.0F;
      } else {
         return layoutScale <= 0.001F ? 1.0F : layoutScale;
      }
   }

   private static float clamp(float value, float min, float max) {
      if (value < min) {
         return min;
      } else {
         return value > max ? max : value;
      }
   }

   @Environment(EnvType.CLIENT)
   public record PopupPlacement(float x, float y) {
      public PopupPlacement(float x, float y) {
         if (Float.isFinite(x) && Float.isFinite(y)) {
            this.x = x;
            this.y = y;
         } else {
            throw new IllegalArgumentException("Popup placement coordinates must be finite");
         }
      }
   }
}