package sl.selene.util.other;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import sl.selene.util.render.math.animation.anim2.Interpolator;

@Environment(EnvType.CLIENT)
public final class Mathf implements IMinecraft {
   public static final Matrix4f lastProjMat = new Matrix4f();
   public static final Matrix4f lastModMat = new Matrix4f();
   public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

   public static float calculateXPosition(float x, float width) {
      return x - width / 2.0F;
   }

   public static float wrapAngleTo180(float angle) {
      angle %= 360.0F;
      if (angle >= 180.0F) {
         angle -= 360.0F;
      }

      if (angle < -180.0F) {
         angle += 360.0F;
      }

      return angle;
   }

   public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      int displayHeight = mc.getWindow().getHeight();
      int[] viewport = new int[4];
      GL11.glGetIntegerv(2978, viewport);
      Vector3f target = new Vector3f();
      double deltaX = pos.x - camera.getCameraPos().x;
      double deltaY = pos.y - camera.getCameraPos().y;
      double deltaZ = pos.z - camera.getCameraPos().z;
      Vector4f transformedCoordinates = new Vector4f((float)deltaX, (float)deltaY, (float)deltaZ, 1.0F).mul(lastWorldSpaceMatrix);
      Matrix4f matrixProj = new Matrix4f(lastProjMat);
      Matrix4f matrixModel = new Matrix4f(lastModMat);
      matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
      return new Vec3d(target.x / getScaleFactor(), (displayHeight - target.y) / getScaleFactor(), target.z);
   }

   public static double getScaleFactor() {
      return mc.getWindow().getScaleFactor();
   }

   public static float map(float value, float fromMin, float fromMax, float toMin, float toMax) {
      return fromMax - fromMin == 0.0F ? toMin : toMin + (toMax - toMin) * ((value - fromMin) / (fromMax - fromMin));
   }

   private static void validateRange(double min, double max) {
      if (max < min) {
         throw new IllegalArgumentException("max cannot be less than min.");
      }
   }

   public static double limitDecimals(double value, int decimalPlaces) {
      return Math.round(value * Math.pow(10.0, decimalPlaces)) / Math.pow(10.0, decimalPlaces);
   }

   public static float normalize(float value, float min, float max) {
      return (value - min) / (max - min);
   }

   public static Vector2f rotationToEntity(Entity target) {
      Vec3d Vec3d = new Vec3d(target.getX(), target.getY(), target.getZ()).subtract(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
      double magnitude = Math.hypot(Vec3d.x, Vec3d.z);
      return new Vector2f(
         (float)Math.toDegrees(Math.atan2(Vec3d.z, Vec3d.x)) - 90.0F, (float)(-Math.toDegrees(Math.atan2(Vec3d.y, magnitude)))
      );
   }

   static float wrapAngleTo180_float(float p_76142_0_) {
      if ((p_76142_0_ = p_76142_0_ % 360.0F) >= 180.0F) {
         p_76142_0_ -= 360.0F;
      }

      if (p_76142_0_ < -180.0F) {
         p_76142_0_ += 360.0F;
      }

      return p_76142_0_;
   }

   public static float valWave01(float value) {
      return (value > 0.5 ? 1.0F - value : value) * 2.0F;
   }

   public static double lerp(double a, double b, double f) {
      return a + f * (b - a);
   }

   public static int lerp(int a, int b, float f) {
      return a + (int)(f * (b - a));
   }

   public static float lerp(float a, float b, float f) {
      return a + f * (b - a);
   }

   public static boolean isInRegion(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public static double interpolate(double current, double old, double scale) {
      return old + (current - old) * scale;
   }

   public static Vec3d interpolate(Vec3d end, Vec3d start, float multiple) {
      return new Vec3d(
         interpolate(end.getX(), start.getX(), multiple),
         interpolate(end.getY(), start.getY(), multiple),
         interpolate(end.getZ(), start.getZ(), multiple)
      );
   }

   public static double interporate(double p_219803_0_, double p_219803_2_, double p_219803_4_) {
      return p_219803_2_ + p_219803_0_ * (p_219803_4_ - p_219803_2_);
   }

   public static int randomInt(int min, int max) {
      return min + (int)(Math.random() * (max - min + 1));
   }

   public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
      return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
   }

   public static float random1(float min, float max) {
      return (float)(Math.random() * (max - min) + min);
   }

   public static double randomWithUpdate(double min, double max, long ms, TimerUtil stopWatch) {
      double randomValue = 0.0;
      if (stopWatch.isReached(ms)) {
         randomValue = random1((float)min, (float)max);
         stopWatch.reset();
      }

      return randomValue;
   }

   public static float fast(float end, float start, float multiple) {
      return (1.0F - MathHelper.clamp(deltaTime() * multiple, 0.0F, 1.0F)) * end + MathHelper.clamp(deltaTime() * multiple, 0.0F, 1.0F) * start;
   }

   public static double getRandom(double min, double max) {
      if (min == max) {
         return min;
      } else {
         if (min > max) {
            double d = min;
            min = max;
            max = d;
         }

         return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
      }
   }

   public static float random(float min, float max) {
      return (float)(Math.random() * (max - min) + min);
   }

   public static double randomValue(double min, double max) {
      validateRange(min, max);
      return min + ThreadLocalRandom.current().nextDouble() * (max - min);
   }

   public static float randomValue(float min, float max) {
      validateRange(min, max);
      return min + ThreadLocalRandom.current().nextFloat() * (max - min);
   }

   public static double calcDiff(double a, double b) {
      return a - b;
   }

   public static float deltaTime() {
      float debugFPS = mc.getCurrentFps();
      return debugFPS > 0.0F ? 1.0F / debugFPS : 1.0F;
   }

   public static String formatTime(long millis) {
      long hours = millis / 3600000L;
      long minutes = millis % 3600000L / 60000L;
      long seconds = millis % 360000L % 60000L / 1000L;
      return String.format("%02d:%02d:%02d", hours, minutes, seconds);
   }

   public static double round(double value, int increment) {
      double num = Math.pow(10.0, increment);
      return Math.round(value * num) / num;
   }

   public static double round(double num, double increment) {
      double v = Math.round(num / increment) * increment;
      BigDecimal bd = new BigDecimal(v);
      bd = bd.setScale(2, RoundingMode.HALF_UP);
      return bd.doubleValue();
   }

   public static double round(double value) {
      return Math.round(value * 100.0) / 100.0;
   }

   public static double step(double value, double steps) {
      double roundedValue = Math.round(value / steps) * steps;
      return Math.round(roundedValue * 100.0) / 100.0;
   }

   public static double clamp(double min, double max, double value) {
      return Math.max(min, Math.min(max, value));
   }

   public static float clamp(float min, float max, float value) {
      return Math.max(min, Math.min(max, value));
   }

   public static int clamp(int min, int max, int value) {
      return Math.max(min, Math.min(max, value));
   }

   public static double clamp01(double value) {
      return clamp(0.0, 1.0, value);
   }

   public static float clamp01(float value) {
      return clamp(0.0F, 1.0F, value);
   }

   public static double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double deltaX = calcDiff(x2, x1);
      double deltaY = calcDiff(y2, y1);
      double deltaZ = calcDiff(z2, z1);
      return MathHelper.sqrt((float)(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ));
   }

   public static double getDistance(BlockPos pos1, BlockPos pos2) {
      double deltaX = calcDiff(pos1.getX(), pos2.getX());
      double deltaY = calcDiff(pos1.getY(), pos2.getY());
      double deltaZ = calcDiff(pos1.getZ(), pos2.getZ());
      return MathHelper.sqrt((float)(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ));
   }

   public static float limit(float current, float inputMin, float inputMax, float outputMin, float outputMax) {
      current = clamp(inputMin, inputMax, current);
      float distancePercentage = (current - inputMin) / (inputMax - inputMin);
      return Interpolator.lerp(outputMin, outputMax, distancePercentage);
   }

   @Generated
   private Mathf() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}