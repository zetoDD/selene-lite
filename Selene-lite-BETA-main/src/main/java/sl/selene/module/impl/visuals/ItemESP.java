package sl.selene.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.joml.Matrix4f;
import sl.selene.event.EventInit;
import sl.selene.event.render.WorldRenderEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.world.WorldRenderUtil;

@IModule(name = "Item ESP", description = "Makes dropped items glow with an optional tracer", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ItemESP extends Module {

      private static final Identifier GLOW_TEXTURE_C = Identifier.of("selene", "textures/world/dashbloom.png");
      private static final Identifier GLOW_TEXTURE_G = Identifier.of("selene", "textures/world/dashbloomsample.png");
      private static final RenderPipeline GLOW_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
                              .withLocation(Identifier.of("selene", "itemesp_glow"))
                              .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
                              .withCull(false)
                              .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                              .withDepthWrite(false)
                              .withBlend(BlendFunction.LIGHTNING)
                              .build());
      private static final RenderLayer GLOW_LAYER = RenderLayer.of("selene_itemesp_glow", RenderSetup.builder(GLOW_PIPELINE).expectedBufferSize(1024).translucent().texture("Sampler0", GLOW_TEXTURE_C).build());
      private static final RenderLayer GLOW_LAYER_G = RenderLayer.of("selene_itemesp_glow_g", RenderSetup.builder(GLOW_PIPELINE).expectedBufferSize(1024).translucent().texture("Sampler0", GLOW_TEXTURE_G).build());
      private static final RenderPipeline TRACER_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_COLOR_SNIPPET })
                              .withLocation(Identifier.of("selene", "itemesp_tracer"))
                              .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
                              .withCull(false)
                              .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                              .withDepthWrite(false)
                              .withBlend(BlendFunction.LIGHTNING)
                              .build());
      private static final RenderLayer TRACER_LAYER = RenderLayer.of(
                  "selene_itemesp_tracer",
                  RenderSetup.builder(TRACER_PIPELINE).expectedBufferSize(1024).translucent().build());

      public static BooleanSetting tracers = new BooleanSetting("Tracers", false);

      public ItemESP() {
            this.addSettings(new Setting[] { tracers });
      }

      @EventInit
      public void render(WorldRenderEvent event) {
            if (mc.world != null && mc.player != null) {
                  Immediate immediate = event.worldRenderer().bufferSource();
                  float tickDelta = event.worldRenderer().tickDelta();
                  MatrixStack stack = event.matrixStack();

                  for (Entity ent : mc.world.getEntities()) {
                        if (ent instanceof ItemEntity && mc.player.canSee(ent)) {
                              this.renderGlow(stack, immediate, ent, tickDelta);
                        }
                  }
            }
      }

      private void renderGlow(MatrixStack matrices, Immediate immediate, Entity target, float partialTicks) {
            if (target == null) {
                  return;
            }
            Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
            double x = target.lastRenderX + (target.getX() - target.lastRenderX) * partialTicks;
            double y = target.lastRenderY + (target.getY() - target.lastRenderY) * partialTicks;
            double z = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * partialTicks;

            float alphaPC = 1.0F;
            int fadeColor = ColorUtil.fade();
            int glowColor = ColorUtil.multAlpha(fadeColor, alphaPC);
            float glowSize = 0.35F;

            matrices.push();
            matrices.translate(x - cameraPos.x, y + 0.3 - cameraPos.y, z - cameraPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
            Matrix4f glowMatrix = matrices.peek().getPositionMatrix();
            WorldRenderUtil.drawGlow(immediate.getBuffer(GLOW_LAYER), glowMatrix, glowColor, 160,
                        glowSize * 3.0F);
            WorldRenderUtil.drawGlow(immediate.getBuffer(GLOW_LAYER_G), glowMatrix, glowColor, 140, glowSize);
            matrices.pop();

            if (tracers.get()) {
                  Vec3d eye = mc.player.getEyePos().subtract(cameraPos);
                  Matrix4f tracerMatrix = matrices.peek().getPositionMatrix();
                  VertexConsumer buffer = immediate.getBuffer(TRACER_LAYER);
                  int r = glowColor >> 16 & 0xFF;
                  int g = glowColor >> 8 & 0xFF;
                  int b = glowColor & 0xFF;
                  int a = 200;
                  buffer.vertex(tracerMatrix, (float) eye.x, (float) eye.y, (float) eye.z).color(r, g, b, a);
                  buffer.vertex(tracerMatrix, (float) (x - cameraPos.x), (float) (y + 0.3 - cameraPos.y),
                              (float) (z - cameraPos.z)).color(r, g, b, a);
            }
      }
}