package sl.selene.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventChangeWorld;
import sl.selene.event.player.EventJump;
import sl.selene.event.render.WorldRenderEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.ModeSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.render.math.animation.anim.util.Easing;
import sl.selene.util.render.math.animation.anim.util.Easings;

@IModule(name = "Jump Circle", description = "Renders a clean expanding white ring where you jump", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class JumpCircle extends Module {
   public static final SliderSetting ringCount = new SliderSetting("Rings", 2.0F, 1.0F, 5.0F, 1.0F, false);
   public static final SliderSetting ringSize = new SliderSetting("Size", 2.0F, 0.5F, 5.0F, 0.1F, false);
   public static final SliderSetting lifetime = new SliderSetting("Lifetime", 0.8F, 0.2F, 3.0F, 0.1F, false);
   public static final ModeSetting easing = new ModeSetting("Easing", "Smooth", "Smooth", "Back", "Bounce");

   private static final int MAX_EFFECTS = 12;
   private static final int QUAD_BUFFER_SIZE_BYTES = 2048;
   private static final int WHITE = 0xFFFFFFFF;
   private static final RenderPipeline RING_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
               .withLocation(Identifier.of("selene", "pipeline/world/jump_ring"))
               .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
               .withCull(false)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withBlend(BlendFunction.LIGHTNING)
               .build());
   private static final Identifier JUMP_TEXTURE = Identifier.of("selene", "textures/world/jump_2.png");
   private static final RenderLayer JUMP_LAYER = RenderLayer.of(
         "selene/jump_ring",
         RenderSetup.builder(RING_PIPELINE)
               .expectedBufferSize(QUAD_BUFFER_SIZE_BYTES)
               .translucent()
               .texture("Sampler0", JUMP_TEXTURE)
               .build());

   private final List<JumpCircle.Circle> circles = new ArrayList<>();

   private static final long SPAWN_DEBOUNCE_MS = 250L;
   private long lastSpawnTime = 0L;

   public JumpCircle() {
      this.addSettings(new Setting[] { ringCount, ringSize, lifetime, easing });
   }

   @Override
   public void onDisable() {
      this.circles.clear();
      this.lastSpawnTime = 0L;
      super.onDisable();
   }

   @EventInit
   public void onJump(EventJump e) {
      if (mc.player == null) {
         return;
      }

      long now = System.currentTimeMillis();

      if (now - this.lastSpawnTime < SPAWN_DEBOUNCE_MS) {
         return;
      }
      this.lastSpawnTime = now;

      while (this.circles.size() >= MAX_EFFECTS) {
         this.circles.remove(0);
      }

      this.circles.add(
            new JumpCircle.Circle(
                  new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).add(0.0, 0.05, 0.0)));
   }

   @EventInit
   public void onChange(EventChangeWorld e) {
      this.circles.clear();
      this.lastSpawnTime = 0L;
   }

   @EventInit
   public void onRender(WorldRenderEvent e) {
      if (this.circles.isEmpty()) {
         return;
      }

      long lifetimeMs = (long) (lifetime.get() * 1000.0F);
      this.circles.removeIf(c -> System.currentTimeMillis() - c.time >= lifetimeMs);
      if (this.circles.isEmpty()) {
         return;
      }

      Immediate immediate = e.worldRenderer().bufferSource();
      MatrixStack pose = e.matrixStack();
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      VertexConsumer buffer = immediate.getBuffer(JUMP_LAYER);
      Easing ease = switch (easing.get()) {
         case "Back" -> Easings.BACK_OUT;
         case "Bounce" -> Easings.BOUNCE_OUT;
         default -> Easings.CIRC_OUT;
      };
      int count = (int) ringCount.get();
      float maxSize = ringSize.get();
      long now = System.currentTimeMillis();

      for (JumpCircle.Circle c : this.circles) {
         long elapsed = now - c.time;
         if (elapsed <= 0L) {
            continue;
         }

         long ringDurationMs = Math.max(1L, lifetimeMs / count);

         for (int i = 0; i < count; i++) {
            long delayMs = count <= 1 ? 0L : (long) ((lifetimeMs - ringDurationMs) * i / (float) (count - 1));
            float progress = (float) (elapsed - delayMs) / (float) ringDurationMs;
            if (progress <= 0.0F || progress >= 1.0F) {
               continue;
            }

            float eased = (float) ease.ease(progress);
            float radius = maxSize * eased;
            if (radius <= 0.01F) {
               continue;
            }

            float fade = 1.0F - (float) ease.ease(progress);
            int alpha = (int) (255.0F * fade);
            int rgba = (WHITE & 0x00FFFFFF) | (alpha << 24);

            double posX = c.vector3d.x - cameraPos.x;
            double posY = c.vector3d.y - cameraPos.y;
            double posZ = c.vector3d.z - cameraPos.z;
            pose.push();
            pose.translate(posX, posY, posZ);
            pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            Entry entry = pose.peek();
            Matrix4f matrix4f = entry.getPositionMatrix();
            Matrix3f normalMatrix = entry.getNormalMatrix();
            this.drawTexturedQuad(buffer, matrix4f, normalMatrix, -radius / 2.0F, -radius / 2.0F, radius, radius, rgba);
            pose.pop();
         }
      }
   }

   private void drawTexturedQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, float x, float y,
         float width, float height, int rgba) {
      int r = rgba >> 16 & 0xFF;
      int g = rgba >> 8 & 0xFF;
      int b = rgba & 0xFF;
      int a = rgba >> 24 & 0xFF;
      Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F);
      normalMatrix.transform(normal);
      normal.normalize();
      float x2 = x + width;
      float y2 = y + height;
      buffer.vertex(matrix, x, y, 0.0F)
            .color(r, g, b, a)
            .texture(0.0F, 1.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(15728880)
            .normal(normal.x, normal.y, normal.z);
      buffer.vertex(matrix, x2, y, 0.0F)
            .color(r, g, b, a)
            .texture(1.0F, 1.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(15728880)
            .normal(normal.x, normal.y, normal.z);
      buffer.vertex(matrix, x2, y2, 0.0F)
            .color(r, g, b, a)
            .texture(1.0F, 0.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(15728880)
            .normal(normal.x, normal.y, normal.z);
      buffer.vertex(matrix, x, y2, 0.0F)
            .color(r, g, b, a)
            .texture(0.0F, 0.0F)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(15728880)
            .normal(normal.x, normal.y, normal.z);
   }

   @Environment(EnvType.CLIENT)
   private static final class Circle {
      private final Vec3d vector3d;
      private final long time;

      private Circle(Vec3d vector3d) {
         this.vector3d = vector3d;
         this.time = System.currentTimeMillis();
      }
   }
}