package sl.selene.util.render.postfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

@Environment(EnvType.CLIENT)
public final class RenderTarget {
   public int fbo = 0;
   public int colorTex = 0;
   public int width = 0;
   public int height = 0;

   public void ensure(int w, int h) {
      if (w > 0 && h > 0) {
         if (w != this.width || h != this.height || this.fbo == 0 || this.colorTex == 0) {
            this.destroy();
            this.width = w;
            this.height = h;
            int prevActiveTexture = GL11.glGetInteger(34016);
            int prevTextureBinding;
            if (prevActiveTexture != 33984) {
               GL13.glActiveTexture(33984);
               prevTextureBinding = GL11.glGetInteger(32873);
            } else {
               prevTextureBinding = GL11.glGetInteger(32873);
            }

            int prevFramebuffer = GL11.glGetInteger(36006);
            int prevDrawBuffer = GL11.glGetInteger(3073);
            int prevReadBuffer = GL11.glGetInteger(3074);
            boolean restoreActiveTexture = prevActiveTexture != 33984;

            try {
               this.colorTex = GL11.glGenTextures();
               GL11.glBindTexture(3553, this.colorTex);
               GL11.glTexParameteri(3553, 10241, 9729);
               GL11.glTexParameteri(3553, 10240, 9729);
               GL11.glTexParameteri(3553, 10242, 33071);
               GL11.glTexParameteri(3553, 10243, 33071);
               GL11.glTexImage2D(3553, 0, 32856, this.width, this.height, 0, 6408, 5121, 0L);
               this.fbo = GL30.glGenFramebuffers();
               GL30.glBindFramebuffer(36160, this.fbo);
               GL30.glFramebufferTexture2D(36160, 36064, 3553, this.colorTex, 0);
               GL11.glDrawBuffer(36064);
               GL11.glReadBuffer(0);
               int status = GL30.glCheckFramebufferStatus(36160);
               if (status != 36053) {
                  throw new IllegalStateException("FBO incomplete: status=" + status);
               }
            } catch (Error | RuntimeException var13) {
               this.destroy();
               throw var13;
            } finally {
               GL30.glBindFramebuffer(36160, prevFramebuffer);
               GL11.glDrawBuffer(prevDrawBuffer);
               GL11.glReadBuffer(prevReadBuffer);
               GL11.glBindTexture(3553, prevTextureBinding);
               if (restoreActiveTexture) {
                  GL13.glActiveTexture(prevActiveTexture);
               }
            }
         }
      }
   }

   public void bind() {
      GL30.glBindFramebuffer(36160, this.fbo);
      GL11.glViewport(0, 0, this.width, this.height);
   }

   public static void bindDefault(int w, int h) {
      GL30.glBindFramebuffer(36160, 0);
      GL11.glViewport(0, 0, w, h);
      GL11.glDrawBuffer(1029);
      GL11.glReadBuffer(1029);
   }

   public void generateMips() {
      if (this.colorTex != 0) {
         GL11.glBindTexture(3553, this.colorTex);
         GL30.glGenerateMipmap(3553);
         GL11.glBindTexture(3553, 0);
      }
   }

   public void destroy() {
      if (this.fbo != 0) {
         GL30.glDeleteFramebuffers(this.fbo);
         this.fbo = 0;
      }

      if (this.colorTex != 0) {
         GL11.glDeleteTextures(this.colorTex);
         this.colorTex = 0;
      }

      this.width = this.height = 0;
   }
}