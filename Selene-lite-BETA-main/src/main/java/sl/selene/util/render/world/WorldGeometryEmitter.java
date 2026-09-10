package sl.selene.util.render.world;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import sl.selene.util.render.utils.Color;

@Environment(EnvType.CLIENT)
public final class WorldGeometryEmitter {
   private final Camera camera;
   private final Matrix4f positionMatrix;
   private final Matrix3f normalMatrix;
   private final VertexConsumer consumer;
   private final Vec3d cameraPosition;

   public WorldGeometryEmitter(WorldRenderer renderer, Entry entry, VertexConsumer consumer) {
      this(Objects.requireNonNull(renderer, "renderer").camera(), entry, consumer);
   }

   public WorldGeometryEmitter(Camera camera, Entry entry, VertexConsumer consumer) {
      this.camera = Objects.requireNonNull(camera, "camera");
      Objects.requireNonNull(entry, "entry");
      this.consumer = Objects.requireNonNull(consumer, "consumer");
      this.cameraPosition = this.camera.getCameraPos();
      this.positionMatrix = new Matrix4f(entry.getPositionMatrix());
      this.normalMatrix = new Matrix3f(entry.getNormalMatrix());
   }

   public void emitQuad(Vec3d v0, Vec3d v1, Vec3d v2, Vec3d v3, int rgbaColor) {
      this.emitQuad(v0, v1, v2, v3, rgbaColor, rgbaColor, rgbaColor, rgbaColor);
   }

   public void emitQuad(Vec3d v0, Vec3d v1, Vec3d v2, Vec3d v3, int rgbaColor0, int rgbaColor1, int rgbaColor2, int rgbaColor3) {
      Objects.requireNonNull(v0, "v0");
      Objects.requireNonNull(v1, "v1");
      Objects.requireNonNull(v2, "v2");
      Objects.requireNonNull(v3, "v3");
      this.writeColorVertex(v0, rgbaColor0);
      this.writeColorVertex(v1, rgbaColor1);
      this.writeColorVertex(v2, rgbaColor2);
      this.writeColorVertex(v3, rgbaColor3);
   }

   public void emitCube(Vec3d min, Vec3d max, int rgbaColor) {
      Objects.requireNonNull(min, "min");
      Objects.requireNonNull(max, "max");
      if (!(min.x > max.x) && !(min.y > max.y) && !(min.z > max.z)) {
         Vec3d p000 = new Vec3d(min.x, min.y, min.z);
         Vec3d p001 = new Vec3d(min.x, min.y, max.z);
         Vec3d p010 = new Vec3d(min.x, max.y, min.z);
         Vec3d p011 = new Vec3d(min.x, max.y, max.z);
         Vec3d p100 = new Vec3d(max.x, min.y, min.z);
         Vec3d p101 = new Vec3d(max.x, min.y, max.z);
         Vec3d p110 = new Vec3d(max.x, max.y, min.z);
         Vec3d p111 = new Vec3d(max.x, max.y, max.z);
         this.emitQuad(p000, p100, p110, p010, rgbaColor);
         this.emitQuad(p001, p011, p111, p101, rgbaColor);
         this.emitQuad(p000, p001, p101, p100, rgbaColor);
         this.emitQuad(p010, p110, p111, p011, rgbaColor);
         this.emitQuad(p000, p010, p011, p001, rgbaColor);
         this.emitQuad(p100, p101, p111, p110, rgbaColor);
      } else {
         throw new IllegalArgumentException("Minimum corner must be less than or equal to maximum corner.");
      }
   }

   public void emitLine(Vec3d start, Vec3d end, int rgbaColor) {
      this.emitLine(start, end, rgbaColor, rgbaColor);
   }

   public void emitLine(Vec3d start, Vec3d end, int startColor, int endColor) {
      Objects.requireNonNull(start, "start");
      Objects.requireNonNull(end, "end");
      Vector3f normal = this.computeLineNormal(start, end);
      this.writeLineVertex(start, startColor, normal);
      this.writeLineVertex(end, endColor, normal);
   }

   public void emitTexturedQuad(
      Vec3d v0,
      Vec3d v1,
      Vec3d v2,
      Vec3d v3,
      float u0,
      float v0Coord,
      float u1,
      float v1Coord,
      float u2,
      float v2Coord,
      float u3,
      float v3Coord,
      int rgbaColor
   ) {
      this.emitTexturedQuad(v0, v1, v2, v3, u0, v0Coord, u1, v1Coord, u2, v2Coord, u3, v3Coord, rgbaColor, rgbaColor, rgbaColor, rgbaColor);
   }

   public void emitTexturedQuad(
      Vec3d v0,
      Vec3d v1,
      Vec3d v2,
      Vec3d v3,
      float u0,
      float v0Coord,
      float u1,
      float v1Coord,
      float u2,
      float v2Coord,
      float u3,
      float v3Coord,
      int color0,
      int color1,
      int color2,
      int color3
   ) {
      Objects.requireNonNull(v0, "v0");
      Objects.requireNonNull(v1, "v1");
      Objects.requireNonNull(v2, "v2");
      Objects.requireNonNull(v3, "v3");
      this.writeTexturedVertex(v0, u0, v0Coord, color0);
      this.writeTexturedVertex(v1, u1, v1Coord, color1);
      this.writeTexturedVertex(v2, u2, v2Coord, color2);
      this.writeTexturedVertex(v3, u3, v3Coord, color3);
   }

   private void writeColorVertex(Vec3d worldPos, int rgbaColor) {
      Vec3d cameraSpace = this.toCameraSpace(worldPos);
      VertexConsumer vertex = this.consumer
         .vertex(this.positionMatrix, (float)cameraSpace.x, (float)cameraSpace.y, (float)cameraSpace.z);
      vertex.color(Color.red(rgbaColor), Color.green(rgbaColor), Color.blue(rgbaColor), Color.alpha(rgbaColor));
      this.finalizeVertex(vertex);
   }

   private void writeTexturedVertex(Vec3d worldPos, float u, float v, int rgbaColor) {
      Vec3d cameraSpace = this.toCameraSpace(worldPos);
      VertexConsumer vertex = this.consumer
         .vertex(this.positionMatrix, (float)cameraSpace.x, (float)cameraSpace.y, (float)cameraSpace.z);
      vertex.texture(u, v);
      vertex.color(Color.red(rgbaColor), Color.green(rgbaColor), Color.blue(rgbaColor), Color.alpha(rgbaColor));
      this.finalizeVertex(vertex);
   }

   private void writeLineVertex(Vec3d worldPos, int rgbaColor, Vector3f normal) {
      Vec3d cameraSpace = this.toCameraSpace(worldPos);
      VertexConsumer vertex = this.consumer
         .vertex(this.positionMatrix, (float)cameraSpace.x, (float)cameraSpace.y, (float)cameraSpace.z);
      vertex.color(Color.red(rgbaColor), Color.green(rgbaColor), Color.blue(rgbaColor), Color.alpha(rgbaColor));
      vertex.normal(normal.x, normal.y, normal.z);
      this.finalizeVertex(vertex);
   }

   private void finalizeVertex(VertexConsumer vertex) {
      Objects.requireNonNull(vertex, "vertex");

      try {
         Method nextMethod = vertex.getClass().getMethod("next");
         nextMethod.invoke(vertex);
      } catch (NoSuchMethodException var5) {
      } catch (IllegalAccessException var6) {
         throw new IllegalStateException("Unable to access vertex finalization method", var6);
      } catch (InvocationTargetException var7) {
         Throwable cause = var7.getCause();
         if (cause instanceof RuntimeException runtime) {
            throw runtime;
         }

         if (cause instanceof Error error) {
            throw error;
         }

         throw new IllegalStateException("Vertex finalization failed", cause);
      }
   }

   private Vec3d toCameraSpace(Vec3d worldPos) {
      return worldPos.subtract(this.cameraPosition);
   }

   private Vector3f computeLineNormal(Vec3d start, Vec3d end) {
      Vec3d delta = end.subtract(start);
      Vector3f normal = new Vector3f((float)delta.x, (float)delta.y, (float)delta.z);
      if (normal.lengthSquared() <= 1.0E-6F) {
         normal.set(0.0F, 1.0F, 0.0F);
      }

      normal.normalize();
      this.normalMatrix.transform(normal);
      if (normal.lengthSquared() <= 1.0E-6F) {
         normal.set(0.0F, 1.0F, 0.0F);
      }

      normal.normalize();
      return normal;
   }
}