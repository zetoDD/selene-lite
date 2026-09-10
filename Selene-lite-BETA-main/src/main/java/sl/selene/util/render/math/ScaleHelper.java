package sl.selene.util.render.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public class ScaleHelper {
   public static float size = 2.0F;

   public static void scale_pre() {
      ScaledResolution scaledRes = new ScaledResolution(MinecraftClient.getInstance());
      double scale = ScaledResolution.getScaleFactor() / Math.pow(ScaledResolution.getScaleFactor(), 2.0);
      GL11.glPushMatrix();
      GL11.glScaled(scale * size, scale * size, scale * size);
   }

   public static void scale_post() {
      GL11.glScaled(size, size, size);
      GL11.glPopMatrix();
   }

   public static void scaleStart(float x, float y, float scale) {
      MatrixStack poseStack = new MatrixStack();
      poseStack.push();
      poseStack.translate(x, y, 0.0F);
      poseStack.scale(scale, scale, 1.0F);
      poseStack.translate(-x, -y, 0.0F);
   }

   public static void scaleEnd() {
      MatrixStack poseStack = new MatrixStack();
      poseStack.pop();
   }

   public static int calc(int value) {
      ScaledResolution rs = new ScaledResolution(MinecraftClient.getInstance());
      int sf = ScaledResolution.getScaleFactor();
      return sf > 1 ? Math.round(value / (float)sf) : value;
   }

   public static int calc(float value) {
      ScaledResolution rs = new ScaledResolution(MinecraftClient.getInstance());
      int sf = ScaledResolution.getScaleFactor();
      return sf > 1 ? Math.round(value / sf) : Math.round(value);
   }

   public static float[] calc(float mouseX, float mouseY) {
      ScaledResolution rs = new ScaledResolution(MinecraftClient.getInstance());
      int sf = ScaledResolution.getScaleFactor();
      if (sf > 1) {
         mouseX = mouseX / sf;
         mouseY = mouseY / sf;
      }
      return new float[]{mouseX, mouseY};
   }

   public static float[] calcFromScaled(float mouseX, float mouseY) {
      MinecraftClient mc = MinecraftClient.getInstance();
      int modSf = ScaledResolution.getScaleFactor();
      int vanillaSf = mc.getWindow() != null ? mc.getWindow().getScaleFactor() : modSf;
      if (vanillaSf > 0 && modSf > 0 && vanillaSf != modSf) {
         mouseX = mouseX * vanillaSf / modSf;
         mouseY = mouseY * vanillaSf / modSf;
      }
      return new float[]{mouseX, mouseY};
   }

   public static int calcFromScaled(int value) {
      MinecraftClient mc = MinecraftClient.getInstance();
      int modSf = ScaledResolution.getScaleFactor();
      int vanillaSf = mc.getWindow() != null ? mc.getWindow().getScaleFactor() : modSf;
      return vanillaSf > 0 && modSf > 0 && vanillaSf != modSf
            ? Math.round(value * (float)vanillaSf / modSf)
            : value;
   }

   public static void scaleNonMatrix(float x, float y, float scale) {
      MatrixStack poseStack = new MatrixStack();
      poseStack.translate(x, y, 0.0F);
      poseStack.scale(scale, scale, 1.0F);
      poseStack.translate(-x, -y, 0.0F);
   }
}