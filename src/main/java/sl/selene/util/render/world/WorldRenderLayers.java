package sl.selene.util.render.world;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class WorldRenderLayers {
      private static final RenderPipeline POSITION_COLOR_QUADS_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_COLOR_SNIPPET })
                              .withLocation(Identifier.of("selene", "pipeline/world/position_color_quads"))
                              .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
                              .withCull(false)
                              .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                              .withDepthWrite(true)
                              .build());
      private static final RenderPipeline POSITION_COLOR_QUADS_NO_DEPTH_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_COLOR_SNIPPET })
                              .withLocation(Identifier.of("selene", "pipeline/world/position_color_quads_no_depth"))
                              .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
                              .withCull(false)
                              .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                              .withDepthWrite(false)
                              .build());
      private static final RenderPipeline POSITION_COLOR_QUADS_NO_DEPTH_BLEND_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_COLOR_SNIPPET })
                              .withLocation(
                                          Identifier.of("selene", "pipeline/world/position_color_quads_no_depth_blend"))
                              .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
                              .withCull(false)
                              .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                              .withDepthWrite(false)
                              .withBlend(BlendFunction.LIGHTNING)
                              .build());
      private static final RenderPipeline LINES_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.RENDERTYPE_LINES_SNIPPET })
                              .withLocation(Identifier.of("selene", "pipeline/world/lines"))
                              .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, DrawMode.LINES)
                              .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                              .withDepthWrite(true)
                              .build());
      private static final RenderPipeline LINES_NO_DEPTH_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.RENDERTYPE_LINES_SNIPPET })
                              .withLocation(Identifier.of("selene", "pipeline/world/lines_no_depth"))
                              .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, DrawMode.LINES)
                              .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                              .withDepthWrite(false)
                              .build());
      private static final RenderPipeline TEXTURED_QUADS_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
                              .withLocation(Identifier.of("selene", "pipeline/world/textured_quads"))
                              .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
                              .withCull(false)
                              .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                              .withDepthWrite(false)
                              .withBlend(BlendFunction.TRANSLUCENT)
                              .build());
      private static final RenderPipeline TEXTURED_QUADS_ADDITIVE_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
                              .withLocation(Identifier.of("selene", "pipeline/world/textured_quads_additive"))
                              .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
                              .withCull(false)
                              .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                              .withDepthWrite(false)
                              .withBlend(BlendFunction.ADDITIVE)
                              .build());
      private static final RenderPipeline TEXTURED_QUADS_NO_DEPTH_ADDITIVE_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
                              .withLocation(Identifier.of("selene", "pipeline/world/textured_quads_no_depth_additive"))
                              .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
                              .withCull(false)
                              .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                              .withDepthWrite(false)
                              .withBlend(BlendFunction.ADDITIVE)
                              .build());
      private static final RenderPipeline TEXTURED_QUADS_NO_DEPTH_PIPELINE = RenderPipelines.register(
                  RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
                              .withLocation(Identifier.of("selene", "pipeline/world/textured_quads_no_depth"))
                              .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
                              .withCull(false)
                              .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                              .withDepthWrite(false)
                              .withBlend(BlendFunction.TRANSLUCENT)
                              .build());
      private static final RenderLayer POSITION_COLOR_QUADS_LAYER = RenderLayer.of("selene/world/position_color_quads", RenderSetup.builder(POSITION_COLOR_QUADS_PIPELINE).expectedBufferSize(1024).translucent().build());
      private static final RenderLayer POSITION_COLOR_QUADS_NO_DEPTH_LAYER = RenderLayer.of("selene/world/position_color_quads_no_depth", RenderSetup.builder(POSITION_COLOR_QUADS_NO_DEPTH_PIPELINE).expectedBufferSize(1024).translucent().build());
      private static final RenderLayer POSITION_COLOR_QUADS_NO_DEPTH_BLEND_LAYER = RenderLayer.of("selene/world/position_color_quads_no_depth_blend", RenderSetup.builder(POSITION_COLOR_QUADS_NO_DEPTH_BLEND_PIPELINE).expectedBufferSize(1024).translucent().build());
      private static final RenderLayer TEXTURED_QUADS_LAYER = RenderLayer.of("selene/world/textured_quads", RenderSetup.builder(TEXTURED_QUADS_PIPELINE).expectedBufferSize(1024).translucent().build());
      private static final Map<Double, RenderLayer> LINES_CACHE = new ConcurrentHashMap<>();
      private static final Map<Double, RenderLayer> LINES_NO_DEPTH_CACHE = new ConcurrentHashMap<>();

      private WorldRenderLayers() {
      }

      public static RenderLayer POSITION_COLOR_QUADS() {
            return POSITION_COLOR_QUADS_LAYER;
      }

      public static RenderLayer POSITION_COLOR_QUADS_NO_DEPTH() {
            return POSITION_COLOR_QUADS_NO_DEPTH_LAYER;
      }

      public static RenderLayer POSITION_COLOR_QUADS_NO_DEPTH_BLEND() {
            return POSITION_COLOR_QUADS_NO_DEPTH_BLEND_LAYER;
      }

      public static RenderLayer TEXTURED_QUADS() {
            return TEXTURED_QUADS_LAYER;
      }

      public static RenderLayer TEXTURED_QUADS_NO_DEPTH(Identifier texture) {
            return RenderLayer.of(texture.toString(), RenderSetup.builder(TEXTURED_QUADS_NO_DEPTH_PIPELINE).expectedBufferSize(1024).translucent().texture("Sampler0", texture).build());
      }

      public static RenderLayer TEXTURED_QUADS(Identifier texture) {
            return RenderLayer.of(texture.toString(), RenderSetup.builder(TEXTURED_QUADS_PIPELINE).expectedBufferSize(1024).translucent().texture("Sampler0", texture).build());
      }

      public static RenderLayer TEXTURED_QUADS_ADDITIVE(Identifier texture) {
            return RenderLayer.of(texture.toString(), RenderSetup.builder(TEXTURED_QUADS_ADDITIVE_PIPELINE).expectedBufferSize(1024).translucent().texture("Sampler0", texture).build());
      }

      public static RenderLayer TEXTURED_QUADS_NO_DEPTH_ADDITIVE(Identifier texture) {
            return RenderLayer.of(texture.toString(), RenderSetup.builder(TEXTURED_QUADS_NO_DEPTH_ADDITIVE_PIPELINE).expectedBufferSize(1024).translucent().texture("Sampler0", texture).build());
      }

      public static RenderLayer LINES(double width) {
            double normalizedWidth = normalizeWidth(width);
            return LINES_CACHE.computeIfAbsent(normalizedWidth,
                        value -> createLineLayer(value, "night/world/lines", LINES_PIPELINE));
      }

      public static RenderLayer LINES_NO_DEPTH(double width) {
            double normalizedWidth = normalizeWidth(width);
            return LINES_NO_DEPTH_CACHE.computeIfAbsent(normalizedWidth,
                        value -> createLineLayer(value, "night/world/lines_no_depth", LINES_NO_DEPTH_PIPELINE));
      }

      private static RenderLayer createLineLayer(double width, String baseName, RenderPipeline pipeline) {
            return RenderLayer.of(
                        baseName + "/" + (width == 0.0 ? "default" : Double.toHexString(width)),
                        RenderSetup.builder(pipeline)
                              .expectedBufferSize(256)
                              .translucent()
                              .build());
      }

      public static RenderLayer withRenderPassSetup(RenderLayer renderLayer, Consumer<RenderPass> renderPassSetup) {
            Objects.requireNonNull(renderLayer, "renderLayer");
            MoreMultiPhase.cast(renderLayer).withRenderPassSetup(renderPassSetup);
            return renderLayer;
      }

      private static double normalizeWidth(double width) {
            if (!Double.isFinite(width)) {
                  throw new IllegalArgumentException("Line width must be finite.");
            } else if (width < 0.0) {
                  throw new IllegalArgumentException("Line width cannot be negative.");
            } else {
                  return width == 0.0 ? 0.0 : width;
            }
      }
}