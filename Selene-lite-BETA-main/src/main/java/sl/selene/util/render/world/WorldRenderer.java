package sl.selene.util.render.world;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class WorldRenderer implements AutoCloseable {
   private final Camera camera;
   private final MatrixStack matrixStack;
   private final Matrix4f positionMatrix;
   private final Matrix4f worldMatrix;
   private final Matrix4f projectionMatrix;
   private final BufferAllocator bufferAllocator;
   private final Immediate immediate;
   private final float tickDelta;
   private boolean closed;

   private WorldRenderer(
      Camera camera,
      MatrixStack matrixStack,
      Matrix4f positionMatrix,
      Matrix4f worldMatrix,
      Matrix4f projectionMatrix,
      BufferAllocator bufferAllocator,
      Immediate immediate,
      float tickDelta
   ) {
      this.camera = camera;
      this.matrixStack = matrixStack;
      this.positionMatrix = positionMatrix;
      this.worldMatrix = worldMatrix;
      this.projectionMatrix = projectionMatrix;
      this.bufferAllocator = bufferAllocator;
      this.immediate = immediate;
      this.tickDelta = tickDelta;
   }

   public static WorldRenderer begin(MinecraftClient client, RenderTickCounter tickCounter, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
      Objects.requireNonNull(client, "client");
      Objects.requireNonNull(tickCounter, "tickCounter");
      Objects.requireNonNull(camera, "camera");
      Objects.requireNonNull(positionMatrix, "positionMatrix");
      Objects.requireNonNull(projectionMatrix, "projectionMatrix");
      MatrixStack stack = new MatrixStack();
      Matrix4f basePositionMatrix = new Matrix4f(positionMatrix);
      Matrix4f baseWorldMatrix = new Matrix4f(basePositionMatrix);
      stack.multiplyPositionMatrix(new Matrix4f(basePositionMatrix));
      BufferAllocator allocator = new BufferAllocator(262144);
      Immediate immediate = VertexConsumerProvider.immediate(allocator);
      float tickDelta = tickCounter.getTickProgress(true);
      return new WorldRenderer(camera, stack, basePositionMatrix, baseWorldMatrix, new Matrix4f(projectionMatrix), allocator, immediate, tickDelta);
   }

   public Camera camera() {
      return this.camera;
   }

   public MatrixStack matrixStack() {
      return this.matrixStack;
   }

   public Matrix4f positionMatrix() {
      return new Matrix4f(this.positionMatrix);
   }

   public Matrix4f worldMatrix() {
      return new Matrix4f(this.worldMatrix);
   }

   public Matrix4f projectionMatrix() {
      return new Matrix4f(this.projectionMatrix);
   }

   public float tickDelta() {
      return this.tickDelta;
   }

   public Immediate bufferSource() {
      if (this.closed) {
         throw new IllegalStateException("Cannot access buffers after the world renderer has been closed.");
      } else {
         return this.immediate;
      }
   }

   public VertexConsumer getBuffer(RenderLayer layer) {
      Objects.requireNonNull(layer, "layer");
      if (this.closed) {
         throw new IllegalStateException("Cannot request buffers after the world renderer has been closed.");
      } else {
         return this.immediate.getBuffer(layer);
      }
   }

   public void drawQuad(Vec3d v0, Vec3d v1, Vec3d v2, Vec3d v3, int rgbaColor, boolean depthTest) {
      Objects.requireNonNull(v0, "v0");
      Objects.requireNonNull(v1, "v1");
      Objects.requireNonNull(v2, "v2");
      Objects.requireNonNull(v3, "v3");
      RenderLayer layer = depthTest ? WorldRenderLayers.POSITION_COLOR_QUADS() : WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH();
      WorldGeometryEmitter emitter = new WorldGeometryEmitter(this, this.matrixStack.peek(), this.getBuffer(layer));
      emitter.emitQuad(v0, v1, v2, v3, rgbaColor);
   }

   public void drawCube(Vec3d min, Vec3d max, int rgbaColor, boolean depthTest) {
      Objects.requireNonNull(min, "min");
      Objects.requireNonNull(max, "max");
      RenderLayer layer = depthTest ? WorldRenderLayers.POSITION_COLOR_QUADS() : WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH_BLEND();
      WorldGeometryEmitter emitter = new WorldGeometryEmitter(this, this.matrixStack.peek(), this.getBuffer(layer));
      emitter.emitCube(min, max, rgbaColor);
   }

   public void drawLine(Vec3d start, Vec3d end, double width, int rgbaColor, boolean depthTest) {
      Objects.requireNonNull(start, "start");
      Objects.requireNonNull(end, "end");
      if (!Double.isFinite(width)) {
         throw new IllegalArgumentException("Line width must be finite.");
      } else if (width < 0.0) {
         throw new IllegalArgumentException("Line width cannot be negative.");
      } else {
         RenderLayer layer = depthTest ? WorldRenderLayers.LINES(width) : WorldRenderLayers.LINES_NO_DEPTH(width);
         WorldGeometryEmitter emitter = new WorldGeometryEmitter(this, this.matrixStack.peek(), this.getBuffer(layer));
         emitter.emitLine(start, end, rgbaColor);
      }
   }

   public void drawTexturedQuad(
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
      this.drawTexturedQuad(null, v0, v1, v2, v3, u0, v0Coord, u1, v1Coord, u2, v2Coord, u3, v3Coord, rgbaColor, true);
   }

   public void drawTexturedQuad(
      Identifier texture,
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
      int rgbaColor,
      boolean depthTest
   ) {
      Objects.requireNonNull(v0, "v0");
      Objects.requireNonNull(v1, "v1");
      Objects.requireNonNull(v2, "v2");
      Objects.requireNonNull(v3, "v3");
      RenderLayer layer = texture != null
            ? (depthTest ? WorldRenderLayers.TEXTURED_QUADS(texture) : WorldRenderLayers.TEXTURED_QUADS_NO_DEPTH(texture))
            : WorldRenderLayers.TEXTURED_QUADS();
      WorldGeometryEmitter emitter = new WorldGeometryEmitter(this, this.matrixStack.peek(), this.getBuffer(layer));
      emitter.emitTexturedQuad(v0, v1, v2, v3, u0, v0Coord, u1, v1Coord, u2, v2Coord, u3, v3Coord, rgbaColor);
   }

   public void flush() {
      if (!this.closed) {
         this.immediate.draw();
      }
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;

         try {
            this.bufferAllocator.close();
         } catch (RuntimeException var2) {
            throw var2;
         } catch (Exception var3) {
            throw new IllegalStateException("Failed to release world renderer buffers.", var3);
         }
      }
   }
}