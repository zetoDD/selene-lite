package sl.selene.util.render.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sl.selene.util.color.ColorUtil;

@Environment(EnvType.CLIENT)
public final class WorldRenderUtil {
   private WorldRenderUtil() {
   }

   public static void drawGlow(VertexConsumer buffer, Matrix4f matrix, int color, int alpha, float size) {
      float halfSize = size / 2.0F;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      buffer.vertex(matrix, -halfSize, -halfSize, 0.0F)
         .color(r, g, b, alpha)
         .texture(0.0F, 1.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(0.0F, 0.0F, 1.0F);
      buffer.vertex(matrix, halfSize, -halfSize, 0.0F)
         .color(r, g, b, alpha)
         .texture(1.0F, 1.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(0.0F, 0.0F, 1.0F);
      buffer.vertex(matrix, halfSize, halfSize, 0.0F)
         .color(r, g, b, alpha)
         .texture(1.0F, 0.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(0.0F, 0.0F, 1.0F);
      buffer.vertex(matrix, -halfSize, halfSize, 0.0F)
         .color(r, g, b, alpha)
         .texture(0.0F, 0.0F)
         .overlay(OverlayTexture.DEFAULT_UV)
         .light(15728880)
         .normal(0.0F, 0.0F, 1.0F);
   }

   public static void drawSolidBoxOutline(
      WorldRenderer renderer,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      int rgbaColor,
      double halfThickness,
      boolean depthTest
   ) {
      double t = Math.max(1.0E-4, halfThickness);

      edgeBox(renderer, minX, minY, minZ, maxX, minY, minZ, t, rgbaColor, depthTest);
      edgeBox(renderer, minX, minY, maxZ, maxX, minY, maxZ, t, rgbaColor, depthTest);
      edgeBox(renderer, minX, minY, minZ, minX, minY, maxZ, t, rgbaColor, depthTest);
      edgeBox(renderer, maxX, minY, minZ, maxX, minY, maxZ, t, rgbaColor, depthTest);

      edgeBox(renderer, minX, maxY, minZ, maxX, maxY, minZ, t, rgbaColor, depthTest);
      edgeBox(renderer, minX, maxY, maxZ, maxX, maxY, maxZ, t, rgbaColor, depthTest);
      edgeBox(renderer, minX, maxY, minZ, minX, maxY, maxZ, t, rgbaColor, depthTest);
      edgeBox(renderer, maxX, maxY, minZ, maxX, maxY, maxZ, t, rgbaColor, depthTest);

      edgeBox(renderer, minX, minY, minZ, minX, maxY, minZ, t, rgbaColor, depthTest);
      edgeBox(renderer, maxX, minY, minZ, maxX, maxY, minZ, t, rgbaColor, depthTest);
      edgeBox(renderer, minX, minY, maxZ, minX, maxY, maxZ, t, rgbaColor, depthTest);
      edgeBox(renderer, maxX, minY, maxZ, maxX, maxY, maxZ, t, rgbaColor, depthTest);
   }

   private static void edgeBox(
      WorldRenderer renderer,
      double x1,
      double y1,
      double z1,
      double x2,
      double y2,
      double z2,
      double halfThickness,
      int rgbaColor,
      boolean depthTest
   ) {
      renderer.drawCube(
         new Vec3d(Math.min(x1, x2) - halfThickness, Math.min(y1, y2) - halfThickness, Math.min(z1, z2) - halfThickness),
         new Vec3d(Math.max(x1, x2) + halfThickness, Math.max(y1, y2) + halfThickness, Math.max(z1, z2) + halfThickness),
         rgbaColor,
         depthTest);
   }

   public static void drawBoxFill(
      VertexConsumer buffer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int[] colors, int fillAlpha
   ) {
      int[] c = new int[4];
      int[][] rgba = new int[4][4];

      for (int i = 0; i < 4; i++) {
         c[i] = ColorUtil.replAlpha(colors[i], fillAlpha);
         rgba[i][0] = c[i] >> 16 & 0xFF;
         rgba[i][1] = c[i] >> 8 & 0xFF;
         rgba[i][2] = c[i] & 0xFF;
         rgba[i][3] = c[i] >> 24 & 0xFF;
      }

      buffer.vertex(matrix, (float)minX, (float)minY, (float)minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float)maxX, (float)minY, (float)minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float)maxX, (float)minY, (float)maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float)minX, (float)minY, (float)maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float)minX, (float)maxY, (float)minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float)minX, (float)maxY, (float)maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float)maxX, (float)maxY, (float)maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float)maxX, (float)maxY, (float)minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float)minX, (float)minY, (float)maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float)maxX, (float)minY, (float)maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float)maxX, (float)maxY, (float)maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float)minX, (float)maxY, (float)maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float)maxX, (float)minY, (float)minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float)minX, (float)minY, (float)minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float)minX, (float)maxY, (float)minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float)maxX, (float)maxY, (float)minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float)minX, (float)minY, (float)minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float)minX, (float)minY, (float)maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float)minX, (float)maxY, (float)maxZ).color(rgba[3][0], rgba[3][1], rgba[3][2], rgba[3][3]);
      buffer.vertex(matrix, (float)minX, (float)maxY, (float)minZ).color(rgba[0][0], rgba[0][1], rgba[0][2], rgba[0][3]);
      buffer.vertex(matrix, (float)maxX, (float)minY, (float)maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
      buffer.vertex(matrix, (float)maxX, (float)minY, (float)minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float)maxX, (float)maxY, (float)minZ).color(rgba[1][0], rgba[1][1], rgba[1][2], rgba[1][3]);
      buffer.vertex(matrix, (float)maxX, (float)maxY, (float)maxZ).color(rgba[2][0], rgba[2][1], rgba[2][2], rgba[2][3]);
   }

   public static void drawBoxOutline(
      VertexConsumer buffer,
      Matrix4f matrix,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      int[] colors,
      int outlineAlpha,
      double dashLength,
      double gapLength
   ) {
      int[] c = new int[4];

      for (int i = 0; i < 4; i++) {
         c[i] = ColorUtil.replAlpha(colors[i], outlineAlpha);
      }

      drawDashedLineSegment(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, c[0], c[1], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, c[1], c[2], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, c[2], c[3], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, c[3], c[0], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, c[0], c[1], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, c[1], c[2], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, c[2], c[3], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, c[3], c[0], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, c[0], c[0], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, c[1], c[1], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, c[2], c[2], dashLength, gapLength);
      drawDashedLineSegment(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, c[3], c[3], dashLength, gapLength);
   }

   public static void drawDashedLineSegment(
      VertexConsumer buffer,
      Matrix4f matrix,
      double x1,
      double y1,
      double z1,
      double x2,
      double y2,
      double z2,
      int color1,
      int color2,
      double dashLength,
      double gapLength
   ) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      double dz = z2 - z1;
      double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(length < 0.001)) {
         double unitX = dx / length;
         double unitY = dy / length;
         double unitZ = dz / length;
         int r1 = color1 >> 16 & 0xFF;
         int g1 = color1 >> 8 & 0xFF;
         int b1 = color1 & 0xFF;
         int a1 = color1 >> 24 & 0xFF;
         int r2 = color2 >> 16 & 0xFF;
         int g2 = color2 >> 8 & 0xFF;
         int b2 = color2 & 0xFF;
         int a2 = color2 >> 24 & 0xFF;
         double segmentLength = dashLength + gapLength;

         for (double currentPos = 0.0; currentPos < length; currentPos += segmentLength) {
            double dashEnd = Math.min(currentPos + dashLength, length);
            if (dashEnd > currentPos) {
               double startX = x1 + unitX * currentPos;
               double startY = y1 + unitY * currentPos;
               double startZ = z1 + unitZ * currentPos;
               double endX = x1 + unitX * dashEnd;
               double endY = y1 + unitY * dashEnd;
               double endZ = z1 + unitZ * dashEnd;
               double t = currentPos / length;
               int r = (int)(r1 + (r2 - r1) * t);
               int g = (int)(g1 + (g2 - g1) * t);
               int b = (int)(b1 + (b2 - b1) * t);
               int a = (int)(a1 + (a2 - a1) * t);
               buffer.vertex(matrix, (float)startX, (float)startY, (float)startZ).color(r, g, b, a);
               t = dashEnd / length;
               r = (int)(r1 + (r2 - r1) * t);
               g = (int)(g1 + (g2 - g1) * t);
               b = (int)(b1 + (b2 - b1) * t);
               a = (int)(a1 + (a2 - a1) * t);
               buffer.vertex(matrix, (float)endX, (float)endY, (float)endZ).color(r, g, b, a);
            }
         }
      }
   }
}