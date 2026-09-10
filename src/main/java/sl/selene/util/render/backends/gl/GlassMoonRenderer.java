package sl.selene.util.render.backends.gl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import sl.selene.ui.gui.component.render.GlassStyle;
import sl.selene.util.render.core.RenderFrameMetrics;

@Environment(EnvType.CLIENT)
public final class GlassMoonRenderer {

   public record BlurInfo(int texture, int width, int height, float scaleX, float scaleY) {
      public boolean available() {
         return texture > 0 && width > 0 && height > 0;
      }
   }

   private static ShaderProgram program;
   private static boolean failed;
   private static int vao;
   private static int vbo;
   private static int uViewportLoc = -1;
   private static int uRectLoc = -1;
   private static int uMaskLoc = -1;
   private static int uBlurLoc = -1;
   private static int uBlurScaleLoc = -1;
   private static int uBlurOffsetLoc = -1;
   private static int uBlurAvailableLoc = -1;
   private static int uBlurPxLoc = -1;
   private static int uRimStrengthLoc = -1;
   private static int uGlobalAlphaLoc = -1;
   private static int uShinePhaseLoc = -1;
   private static int uFresnelLoc = -1;
   private static int uFresnelPowerLoc = -1;
   private static int uBaseAlphaLoc = -1;
   private static int uFresnelMixLoc = -1;
   private static int uDistortLoc = -1;
   private static int uTintLoc = -1;
   private static int uScissorLoc = -1;
   private static int uScissorEnabledLoc = -1;
   private static final IntBuffer VIEWPORT_BUF = BufferUtils.createIntBuffer(16);
   private static final IntBuffer SCISSOR_BUF = BufferUtils.createIntBuffer(16);

   private static final float BLUR_PX = 2.0F;

   private static final float RIM_STRENGTH = 2.4F;

   private GlassMoonRenderer() {
   }

   public static boolean draw(int maskTexture, BlurInfo blur, float x, float y, float w, float h,
         float globalAlpha, float[] transform) {
      if (maskTexture <= 0 || !(w > 0.0F) || !(h > 0.0F) || globalAlpha <= 0.0F) {
         return false;
      }
      ensureResources();
      if (program == null) {
         return false;
      }
      float[] bounds = computeBounds(x, y, w, h, transform);
      if (bounds == null) {
         return false;
      }

      GlState.Snapshot state = GlState.push();
      int savedUnit1 = 0;
      try {
         GL11.glGetIntegerv(2978, VIEWPORT_BUF);

         int vpW = VIEWPORT_BUF.get(2);
         int vpH = VIEWPORT_BUF.get(3);
         if (vpW <= 0 || vpH <= 0) {
            return false;
         }

         boolean scissorEnabled = GL11.glIsEnabled(3089);
         float[] scissorBox = new float[4];
         if (scissorEnabled) {
            GL11.glGetIntegerv(3088, SCISSOR_BUF);

            int sx = SCISSOR_BUF.get(0);
            int sy = SCISSOR_BUF.get(1);
            int sw = SCISSOR_BUF.get(2);
            int sh = SCISSOR_BUF.get(3);
            int top = vpH - (sy + sh);
            scissorBox[0] = (float) sx;
            scissorBox[1] = (float) top;
            scissorBox[2] = (float) (sx + sw);
            scissorBox[3] = (float) (top + sh);
         }

         GL11.glDisable(2929);

         GL11.glDisable(2884);

         GL11.glDisable(3089);

         GL11.glEnable(3042);

         GL14.glBlendFuncSeparate(1, 771, 1, 771);

         GL11.glColorMask(true, true, true, true);

         program.use();
         GL20.glUniform2f(uViewportLoc, (float) vpW, (float) vpH);
         GL20.glUniform4f(uRectLoc, bounds[0], bounds[1], bounds[2], bounds[3]);
         GL20.glUniform1i(uMaskLoc, 0);
         GL20.glUniform1i(uBlurLoc, 1);
         GL20.glUniform1f(uBlurPxLoc, BLUR_PX);
         GL20.glUniform1f(uRimStrengthLoc, RIM_STRENGTH);
         GL20.glUniform1f(uGlobalAlphaLoc, clamp01(globalAlpha));
         GL20.glUniform1f(uShinePhaseLoc, GlassStyle.shinePhase());
         GL20.glUniform4f(uFresnelLoc, 1.0F, 1.0F, 1.0F, 160.0F / 255.0F);
         GL20.glUniform1f(uFresnelPowerLoc, GlassStyle.OUTLINE_POWER);
         GL20.glUniform1f(uBaseAlphaLoc, GlassStyle.BASE_ALPHA);
         GL20.glUniform1f(uFresnelMixLoc, GlassStyle.OUTLINE_MIX);
         GL20.glUniform1f(uDistortLoc, GlassStyle.OUTLINE_DISTORT);
         GL20.glUniform4f(uTintLoc, 10.0F / 255.0F, 14.0F / 255.0F, 22.0F / 255.0F, 0.45F);

         boolean hasBlur = blur != null && blur.available();
         if (hasBlur) {
            GL20.glUniform1f(uBlurAvailableLoc, 1.0F);
            GL20.glUniform2f(uBlurScaleLoc,
                  blur.scaleX() / (float) blur.width(),
                  -blur.scaleY() / (float) blur.height());
            GL20.glUniform2f(uBlurOffsetLoc, 0.0F, 1.0F);
         } else {
            GL20.glUniform1f(uBlurAvailableLoc, 0.0F);
            GL20.glUniform2f(uBlurScaleLoc, 0.0F, 0.0F);
            GL20.glUniform2f(uBlurOffsetLoc, 0.0F, 0.0F);
         }

         if (scissorEnabled) {
            GL20.glUniform4f(uScissorLoc, scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            GL20.glUniform1f(uScissorEnabledLoc, 1.0F);
         } else {
            GL20.glUniform4f(uScissorLoc, 0.0F, 0.0F, 0.0F, 0.0F);
            GL20.glUniform1f(uScissorEnabledLoc, 0.0F);
         }

         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, maskTexture);

         GL13.glActiveTexture(33985);
         savedUnit1 = GL11.glGetInteger(32873);

         GL11.glBindTexture(3553, hasBlur ? blur.texture() : 0);

         GL30.glBindVertexArray(vao);
         GL15.glBindBuffer(34962, vbo);
         RenderFrameMetrics.getInstance().recordDrawCall(2);
         GL11.glDrawArrays(5, 0, 6);

      } finally {
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, savedUnit1);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, 0);
         GlState.pop(state);
      }

      return true;
   }

   private static float[] computeBounds(float x, float y, float w, float h, float[] transform) {
      float[] m = transform != null && transform.length >= 6
            ? transform
            : new float[] { 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F };
      float x0 = m[0] * x + m[1] * y + m[2];
      float y0 = m[3] * x + m[4] * y + m[5];
      float x1 = m[0] * (x + w) + m[1] * y + m[2];
      float y1 = m[3] * (x + w) + m[4] * y + m[5];
      float x2 = m[0] * (x + w) + m[1] * (y + h) + m[2];
      float y2 = m[3] * (x + w) + m[4] * (y + h) + m[5];
      float x3 = m[0] * x + m[1] * (y + h) + m[2];
      float y3 = m[3] * x + m[4] * (y + h) + m[5];
      float minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
      float maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
      float minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
      float maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));
      float tw = maxX - minX;
      float th = maxY - minY;
      if (!(tw > 0.0F) || !(th > 0.0F)) {
         return null;
      }
      return new float[] { minX, minY, tw, th };
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      }
      return value > 1.0F ? 1.0F : value;
   }

   private static void ensureResources() {
      if (program != null || failed) {
         return;
      }
      try {
         program = ShaderProgram.fromResources(
               "assets/selene/shaders/ui/logo_outline.vert",
               "assets/selene/shaders/ui/glass_moon.frag");
      } catch (Throwable error) {
         failed = true;
         System.out.println("[GlassMoonRenderer] Shader init failed, glass logo disabled: " + error.getMessage());
         return;
      }
      vao = GL30.glGenVertexArrays();
      vbo = GL15.glGenBuffers();

      float[] quad = new float[] { 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F };
      ByteBuffer data = BufferUtils.createByteBuffer(quad.length * 4);
      data.asFloatBuffer().put(quad).flip();

      GL30.glBindVertexArray(vao);
      GL15.glBindBuffer(34962, vbo);
      GL15.glBufferData(34962, data, 35044);

      GL20.glEnableVertexAttribArray(0);
      GL20.glVertexAttribPointer(0, 2, 5126, false, 8, 0);

      GL30.glBindVertexArray(0);
      GL15.glBindBuffer(34962, 0);

      uViewportLoc = program.getUniformLocation("uViewport");
      uRectLoc = program.getUniformLocation("uRect");
      uMaskLoc = program.getUniformLocation("uMask");
      uBlurLoc = program.getUniformLocation("uBlur");
      uBlurScaleLoc = program.getUniformLocation("uBlurScale");
      uBlurOffsetLoc = program.getUniformLocation("uBlurOffset");
      uBlurAvailableLoc = program.getUniformLocation("uBlurAvailable");
      uBlurPxLoc = program.getUniformLocation("uBlurPx");
      uRimStrengthLoc = program.getUniformLocation("uRimStrength");
      uGlobalAlphaLoc = program.getUniformLocation("uGlobalAlpha");
      uShinePhaseLoc = program.getUniformLocation("uShinePhase");
      uFresnelLoc = program.getUniformLocation("uFresnel");
      uFresnelPowerLoc = program.getUniformLocation("uFresnelPower");
      uBaseAlphaLoc = program.getUniformLocation("uBaseAlpha");
      uFresnelMixLoc = program.getUniformLocation("uFresnelMix");
      uDistortLoc = program.getUniformLocation("uDistort");
      uTintLoc = program.getUniformLocation("uTint");
      uScissorLoc = program.getUniformLocation("uScissor");
      uScissorEnabledLoc = program.getUniformLocation("uScissorEnabled");
   }
}