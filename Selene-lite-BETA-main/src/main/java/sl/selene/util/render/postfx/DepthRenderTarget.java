package sl.selene.util.render.postfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@Environment(EnvType.CLIENT)
public final class DepthRenderTarget {
   public int fbo = 0;
   public int colorTex = 0;
   public int depthTex = 0;
   public int width = 0;
   public int height = 0;

   public void ensure(int w, int h) {
      if (w > 0 && h > 0) {
         if (this.fbo == 0 || this.colorTex == 0 || this.depthTex == 0 || this.width != w || this.height != h) {
            this.destroy();
            this.width = w;
            this.height = h;
            this.colorTex = GL11.glGenTextures();
            GL11.glBindTexture(3553, this.colorTex);
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            GL11.glTexImage2D(3553, 0, 32856, this.width, this.height, 0, 6408, 5121, 0L);
            GL11.glBindTexture(3553, 0);
            this.depthTex = GL11.glGenTextures();
            GL11.glBindTexture(3553, this.depthTex);
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            GL11.glTexParameteri(3553, 34892, 0);
            GL11.glTexImage2D(3553, 0, 33190, this.width, this.height, 0, 6402, 5125, 0L);
            GL11.glBindTexture(3553, 0);
            this.fbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(36160, this.fbo);
            GL30.glFramebufferTexture2D(36160, 36064, 3553, this.colorTex, 0);
            GL30.glFramebufferTexture2D(36160, 36096, 3553, this.depthTex, 0);
            GL11.glDrawBuffer(36064);
            GL11.glReadBuffer(36064);
            int status = GL30.glCheckFramebufferStatus(36160);
            GL30.glBindFramebuffer(36160, 0);
            if (status != 36053) {
               this.destroy();
               throw new IllegalStateException("DepthRenderTarget incomplete: status=" + status);
            }
         }
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

      if (this.depthTex != 0) {
         GL11.glDeleteTextures(this.depthTex);
         this.depthTex = 0;
      }

      this.width = 0;
      this.height = 0;
   }
}