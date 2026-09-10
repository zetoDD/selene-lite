package sl.selene.util.render.postfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import sl.selene.util.render.core.RenderFrameMetrics;

@Environment(EnvType.CLIENT)
final class FullScreenQuad {
   private int vao = 0;
   private int vbo = 0;

   public void ensure() {
      if (this.vao == 0) {
         this.vao = GL30.glGenVertexArrays();
         this.vbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.vao);
         GL15.glBindBuffer(34962, this.vbo);
         float[] quad = new float[]{
            -1.0F,
            -1.0F,
            0.0F,
            0.0F,
            1.0F,
            -1.0F,
            1.0F,
            0.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            0.0F,
            1.0F
         };
         GL15.glBufferData(34962, quad, 35044);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 2, 5126, false, 16, 0L);
         GL20.glEnableVertexAttribArray(1);
         GL20.glVertexAttribPointer(1, 2, 5126, false, 16, 8L);
         GL15.glBindBuffer(34962, 0);
         GL30.glBindVertexArray(0);
      }
   }

   public void bindAndDraw() {
      GL30.glBindVertexArray(this.vao);
      RenderFrameMetrics.getInstance().recordDrawCall(2);
      GL11.glDrawArrays(4, 0, 6);
      GL30.glBindVertexArray(0);
   }

   public void destroy() {
      if (this.vbo != 0) {
         GL15.glDeleteBuffers(this.vbo);
         this.vbo = 0;
      }

      if (this.vao != 0) {
         GL30.glDeleteVertexArrays(this.vao);
         this.vao = 0;
      }
   }
}