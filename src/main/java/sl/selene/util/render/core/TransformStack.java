package sl.selene.util.render.core;

import java.util.ArrayDeque;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TransformStack {
   private final ArrayDeque<float[]> stack = new ArrayDeque<>();

   public TransformStack() {
      this.pushIdentity();
   }

   public void clear() {
      this.stack.clear();
      this.pushIdentity();
   }

   public void pushIdentity() {
      this.stack.push(new float[]{1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F});
   }

   public void pushRotation(float degrees) {
      float rad = (float)Math.toRadians(degrees);
      float c = (float)Math.cos(rad);
      float s = (float)Math.sin(rad);
      float[] r = new float[]{c, -s, 0.0F, s, c, 0.0F, 0.0F, 0.0F, 1.0F};
      float[] top = this.stack.peek();
      this.stack.push(mul(top, r));
   }

   public void pushTranslation(float tx, float ty) {
      float[] t = new float[]{1.0F, 0.0F, tx, 0.0F, 1.0F, ty, 0.0F, 0.0F, 1.0F};
      float[] top = this.stack.peek();
      this.stack.push(mul(top, t));
   }

   public void pushTranslationInv(float tx, float ty) {
      this.pushTranslation(-tx, -ty);
   }

   public void pushScale(float sx, float sy, float originX, float originY) {
      float translateX = originX - originX * sx;
      float translateY = originY - originY * sy;
      float[] s = new float[]{sx, 0.0F, translateX, 0.0F, sy, translateY, 0.0F, 0.0F, 1.0F};
      float[] top = this.stack.peek();
      this.stack.push(mul(top, s));
   }

   public void pushScale(float scale, float originX, float originY) {
      this.pushScale(scale, scale, originX, originY);
   }

   public void replaceTop(float[] matrix) {
      if (matrix == null) {
         throw new IllegalArgumentException("matrix must not be null");
      } else if (matrix.length != 9) {
         throw new IllegalArgumentException("matrix must have length 9");
      } else {
         for (float value : matrix) {
            if (!Float.isFinite(value)) {
               throw new IllegalArgumentException("matrix entries must be finite");
            }
         }

         if (this.stack.isEmpty()) {
            throw new IllegalStateException("cannot replace top matrix on an empty stack");
         } else {
            float[] copy = Arrays.copyOf(matrix, matrix.length);
            this.stack.pop();
            this.stack.push(copy);
         }
      }
   }

   public void pop() {
      if (this.stack.size() > 1) {
         this.stack.pop();
      }
   }

   public void popN(int count) {
      for (int i = 0; i < count; i++) {
         if (this.stack.size() > 1) {
            this.stack.pop();
         }
      }
   }

   public float[] current() {
      return this.stack.peek();
   }

   private static float[] mul(float[] a, float[] b) {
      return new float[]{
         a[0] * b[0] + a[1] * b[3] + a[2] * b[6],
         a[0] * b[1] + a[1] * b[4] + a[2] * b[7],
         a[0] * b[2] + a[1] * b[5] + a[2] * b[8],
         a[3] * b[0] + a[4] * b[3] + a[5] * b[6],
         a[3] * b[1] + a[4] * b[4] + a[5] * b[7],
         a[3] * b[2] + a[4] * b[5] + a[5] * b[8],
         a[6] * b[0] + a[7] * b[3] + a[8] * b[6],
         a[6] * b[1] + a[7] * b[4] + a[8] * b[7],
         a[6] * b[2] + a[7] * b[5] + a[8] * b[8]
      };
   }
}