package sl.selene.util.render.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class WorldProjection {
   private WorldProjection() {
   }

   public static Vector2d project(Vec3d worldPos) {
      return project(worldPos.x, worldPos.y, worldPos.z);
   }

   public static Vector2d project(double x, double y, double z) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.gameRenderer == null || mc.getWindow() == null) {
         return null;
      }

      Camera camera = mc.gameRenderer.getCamera();
      if (camera == null) {
         return null;
      }

      Vec3d cameraPos = camera.getCameraPos();
      Vector3f relative = new Vector3f(
         (float) (x - cameraPos.x),
         (float) (y - cameraPos.y),
         (float) (z - cameraPos.z)
      );

      Quaternionf rotation = new Quaternionf(camera.getRotation());
      rotation.conjugate();
      relative.rotate(rotation);

      if (relative.z >= -0.001F) {
         return null;
      }

      float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
      double fov = resolveFov(mc.gameRenderer, camera, tickDelta);
      float scaledHeight = mc.getWindow().getScaledHeight();
      float scaledWidth = mc.getWindow().getScaledWidth();
      float halfHeight = scaledHeight * 0.5F;
      float tanHalfFov = (float) Math.tan(Math.toRadians(fov * 0.5));
      float scale = halfHeight / (-relative.z * tanHalfFov);

      double screenX = scaledWidth * 0.5 + relative.x * scale;
      double screenY = scaledHeight * 0.5 - relative.y * scale;
      if (!Double.isFinite(screenX) || !Double.isFinite(screenY)) {
         return null;
      }

      return new Vector2d(screenX, screenY);
   }

   public static float toFramebufferX(float scaledX) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.getWindow() == null) {
         return scaledX;
      }

      float scaledW = mc.getWindow().getScaledWidth();
      float fbW = mc.getWindow().getFramebufferWidth();
      return scaledW > 0.0F ? scaledX * (fbW / scaledW) : scaledX;
   }

   public static float toFramebufferY(float scaledY) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc == null || mc.getWindow() == null) {
         return scaledY;
      }

      float scaledH = mc.getWindow().getScaledHeight();
      float fbH = mc.getWindow().getFramebufferHeight();
      return scaledH > 0.0F ? scaledY * (fbH / scaledH) : scaledY;
   }

   private static double resolveFov(GameRenderer gameRenderer, Camera camera, float tickDelta) {
      try {
         var method = GameRenderer.class.getDeclaredMethod("getFov", Camera.class, float.class, boolean.class);
         method.setAccessible(true);
         return (Double) method.invoke(gameRenderer, camera, tickDelta, true);
      } catch (ReflectiveOperationException ignored) {
         MinecraftClient mc = MinecraftClient.getInstance();
         double base = mc != null ? mc.options.getFov().getValue().intValue() : 70;
         return base;
      }
   }
}