package sl.selene.util.render.backends.gl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
public final class GlState {
   private static final int GL_NONE = 0;
   private static final int GL_FRONT = 1028;
   private static final int GL_BACK = 1029;
   private static final int GL_LEFT = 1030;
   private static final int GL_RIGHT = 1031;
   private static final int GL_COLOR_ATTACHMENT0 = 36064;
   private static final int GL_COLOR_ATTACHMENT15 = 36079;

   private GlState() {
   }

   public static boolean isFramebufferSrgbSupported() {
      return false;
   }

   public static boolean isFramebufferSrgbEnabled() {
      return false;
   }

   public static void setFramebufferSrgbEnabled(boolean enabled) {
   }

   public static void disableFramebufferSrgb() {
   }

   public static GlState.Snapshot push() {
      GlState.Snapshot s = new GlState.Snapshot();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer buf4 = stack.mallocInt(4);
         IntBuffer buf1 = stack.mallocInt(1);
         s.framebuffer = GL11.glGetInteger(36160);
         s.drawFramebuffer = GL11.glGetInteger(36006);
         s.readFramebuffer = GL11.glGetInteger(36010);
         GL11.glGetIntegerv(2978, buf4);
         s.viewport[0] = buf4.get(0);
         s.viewport[1] = buf4.get(1);
         s.viewport[2] = buf4.get(2);
         s.viewport[3] = buf4.get(3);
         s.scissorEnabled = GL11.glIsEnabled(3089);
         GL11.glGetIntegerv(3088, buf4);
         s.scissorBox[0] = buf4.get(0);
         s.scissorBox[1] = buf4.get(1);
         s.scissorBox[2] = buf4.get(2);
         s.scissorBox[3] = buf4.get(3);
         s.depthTestEnabled = GL11.glIsEnabled(2929);
         s.cullFaceEnabled = GL11.glIsEnabled(2884);
         s.blendEnabled = GL11.glIsEnabled(3042);
         GL11.glGetIntegerv(32969, buf1);
         s.blendSrcRGB = buf1.get(0);
         GL11.glGetIntegerv(32968, buf1);
         s.blendDstRGB = buf1.get(0);
         GL11.glGetIntegerv(32971, buf1);
         s.blendSrcAlpha = buf1.get(0);
         GL11.glGetIntegerv(32970, buf1);
         s.blendDstAlpha = buf1.get(0);
         s.program = GL11.glGetInteger(35725);
         s.vao = GL11.glGetInteger(34229);
         s.arrayBuffer = GL11.glGetInteger(34964);
         s.elementArrayBuffer = GL11.glGetInteger(34965);
         s.activeTexture = GL11.glGetInteger(34016);
         s.texture2D = GL11.glGetInteger(32873);
         s.unpackAlignment = GL11.glGetInteger(3317);
         s.readBuffer = GL11.glGetInteger(3074);
         s.drawBuffer = GL11.glGetInteger(3073);
         ByteBuffer cm = stack.malloc(4);
         GL11.glGetBooleanv(3107, cm);
         s.colorMaskR = cm.get(0) != 0;
         s.colorMaskG = cm.get(1) != 0;
         s.colorMaskB = cm.get(2) != 0;
         s.colorMaskA = cm.get(3) != 0;
         ByteBuffer dm = stack.malloc(1);
         GL11.glGetBooleanv(2930, dm);
         s.depthMask = dm.get(0) != 0;
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }

      return s;
   }

   public static void pop(GlState.Snapshot s) {
      if (s != null) {
         GL20.glUseProgram(s.program);
         GL30.glBindVertexArray(s.vao);
         GL15.glBindBuffer(34962, s.arrayBuffer);
         GL15.glBindBuffer(34963, s.elementArrayBuffer);
         GL13.glActiveTexture(s.activeTexture);
         GL11.glBindTexture(3553, s.texture2D);
         GL11.glPixelStorei(3317, s.unpackAlignment);
         setEnabled(3089, s.scissorEnabled);
         setEnabled(2929, s.depthTestEnabled);
         setEnabled(2884, s.cullFaceEnabled);
         setEnabled(3042, s.blendEnabled);
         GL14.glBlendFuncSeparate(s.blendSrcRGB, s.blendDstRGB, s.blendSrcAlpha, s.blendDstAlpha);
         GL11.glColorMask(s.colorMaskR, s.colorMaskG, s.colorMaskB, s.colorMaskA);
         GL11.glDepthMask(s.depthMask);
         GL11.glViewport(s.viewport[0], s.viewport[1], s.viewport[2], s.viewport[3]);
         GL11.glScissor(s.scissorBox[0], s.scissorBox[1], s.scissorBox[2], s.scissorBox[3]);
         GL30.glBindFramebuffer(36160, s.framebuffer);
         GL30.glBindFramebuffer(36009, s.drawFramebuffer);
         GL30.glBindFramebuffer(36008, s.readFramebuffer);
         restoreDrawBuffer(s.drawFramebuffer, s.drawBuffer);
         restoreReadBuffer(s.readFramebuffer, s.readBuffer);
      }
   }

   private static void restoreDrawBuffer(int drawFramebuffer, int drawBuffer) {
      if (drawFramebuffer == 0) {
         if (isDefaultFramebufferDrawBuffer(drawBuffer)) {
            GL11.glDrawBuffer(drawBuffer);
         }
      } else if (isFramebufferAttachmentDrawBuffer(drawBuffer)) {
         GL11.glDrawBuffer(drawBuffer);
      }
   }

   private static void restoreReadBuffer(int readFramebuffer, int readBuffer) {
      if (readFramebuffer == 0) {
         if (isDefaultFramebufferReadBuffer(readBuffer)) {
            GL11.glReadBuffer(readBuffer);
         }
      } else if (isFramebufferAttachmentReadBuffer(readBuffer)) {
         GL11.glReadBuffer(readBuffer);
      }
   }

   private static boolean isDefaultFramebufferDrawBuffer(int buffer) {
      return buffer == GL_BACK || buffer == GL_FRONT || buffer == GL_LEFT || buffer == GL_RIGHT;
   }

   private static boolean isDefaultFramebufferReadBuffer(int buffer) {
      return buffer == GL_BACK || buffer == GL_FRONT || buffer == GL_LEFT || buffer == GL_RIGHT;
   }

   private static boolean isFramebufferAttachmentDrawBuffer(int buffer) {
      return buffer == GL_NONE || buffer >= GL_COLOR_ATTACHMENT0 && buffer <= GL_COLOR_ATTACHMENT15;
   }

   private static boolean isFramebufferAttachmentReadBuffer(int buffer) {
      return buffer == GL_NONE || buffer >= GL_COLOR_ATTACHMENT0 && buffer <= GL_COLOR_ATTACHMENT15;
   }

   private static void setEnabled(int cap, boolean enabled) {
      if (enabled) {
         GL11.glEnable(cap);
      } else {
         GL11.glDisable(cap);
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Snapshot {
      public int framebuffer;
      public int drawFramebuffer;
      public int readFramebuffer;
      public final int[] viewport = new int[4];
      public boolean scissorEnabled;
      public final int[] scissorBox = new int[4];
      public boolean depthTestEnabled;
      public boolean cullFaceEnabled;
      public boolean blendEnabled;
      public int blendSrcRGB;
      public int blendDstRGB;
      public int blendSrcAlpha;
      public int blendDstAlpha;
      public boolean colorMaskR;
      public boolean colorMaskG;
      public boolean colorMaskB;
      public boolean colorMaskA;
      public boolean depthMask;
      public int program;
      public int vao;
      public int arrayBuffer;
      public int elementArrayBuffer;
      public int activeTexture;
      public int texture2D;
      public int unpackAlignment;
      public int readBuffer;
      public int drawBuffer;
   }
}