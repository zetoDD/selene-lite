package sl.selene.util.keyboard;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import sl.selene.util.other.IMinecraft;

@Environment(EnvType.CLIENT)
public class ScaleHelper implements IMinecraft {
   public static float size = 2.0F;

   public static void scalePre(MatrixStack pose) {
      pose.push();
      double guiScale = mc.getWindow().getScaleFactor();
      double scale = guiScale / (guiScale * guiScale);
      pose.scale((float)(scale * size), (float)(scale * size), 1.0F);
   }

   public static void scalePost(MatrixStack graphics) {
      graphics.pop();
   }

   public static void scaleStart(MatrixStack graphics, float x, float y, float scale) {
      graphics.push();
      graphics.translate(x, y, 0.0F);
      graphics.scale(scale, scale, 1.0F);
      graphics.translate(-x, -y, 0.0F);
   }

   public static void scaleEnd(MatrixStack graphics) {
      graphics.pop();
   }

   public static int calc(int value) {
      return (int)(value * mc.getWindow().getScaleFactor() / size);
   }

   public static int calc(float value) {
      return (int)(value * mc.getWindow().getScaleFactor() / size);
   }

   public static float calcFloat(float value) {
      return value * mc.getWindow().getScaleFactor() / size;
   }

   public static float[] calc(float mouseX, float mouseY) {
      double scale = mc.getWindow().getScaleFactor();
      mouseX = (float)(mouseX * scale / size);
      mouseY = (float)(mouseY * scale / size);
      return new float[]{mouseX, mouseY};
   }

   public static void scaleNonMatrix(MatrixStack pose, float x, float y, float scale) {
      pose.translate(x, y, 0.0F);
      pose.scale(scale, scale, 1.0F);
      pose.translate(-x, -y, 0.0F);
   }
}