package sl.selene.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import sl.selene.util.color.ColorUtil;

@Environment(EnvType.CLIENT)
public final class CustomHitboxRenderer {
   private CustomHitboxRenderer() {
   }

   public static void drawHitbox(Entity entity, float tickProgress) {
      CustomHitbox module = CustomHitbox.getModule();
      if (module == null || !module.enable || !CustomHitbox.shouldRenderEntity(entity)) {
         return;
      }

      double x = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * tickProgress;
      double y = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * tickProgress;
      double z = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * tickProgress;

      Box source = entity.getBoundingBox();
      Box box = new Box(
         source.minX - entity.getX() + x,
         source.minY - entity.getY() + y,
         source.minZ - entity.getZ() + z,
         source.maxX - entity.getX() + x,
         source.maxY - entity.getY() + y,
         source.maxZ - entity.getZ() + z
      );

      int outlineColor = ColorUtil.replAlpha(CustomHitbox.color.getRGB(), 220);
      int eyeColor = ColorUtil.replAlpha(0xFFFF3333, 220);
      int lookColor = ColorUtil.replAlpha(0xFFFFFFFF, 220);
      float lineWidth = CustomHitbox.lineWidth.get();

      DrawStyle style = DrawStyle.stroked(outlineColor, lineWidth);
      GizmoDrawing.box(box, style);

      if (CustomHitbox.showEyeLine.get()) {
         double centerX = (box.minX + box.maxX) * 0.5;
         double centerZ = (box.minZ + box.maxZ) * 0.5;
         GizmoDrawing.line(
            new Vec3d(centerX, box.maxY, centerZ),
            new Vec3d(centerX, box.maxY + 0.12, centerZ),
            eyeColor,
            lineWidth
         );
      }

      if (CustomHitbox.showLookVector.get() && entity instanceof LivingEntity living) {
         float yaw = MathHelper.lerp(tickProgress, living.lastYaw, living.getYaw());
         float pitch = MathHelper.lerp(tickProgress, living.lastPitch, living.getPitch());
         double yawRad = Math.toRadians(yaw);
         double pitchRad = Math.toRadians(pitch);
         double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
         double dirY = -Math.sin(pitchRad);
         double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
         double eyeY = box.minY + entity.getStandingEyeHeight();
         double eyeX = (box.minX + box.maxX) * 0.5;
         double eyeZ = (box.minZ + box.maxZ) * 0.5;
         double len = CustomHitbox.lookLength.get();
         Vec3d start = new Vec3d(eyeX, eyeY, eyeZ);
         Vec3d end = start.add(dirX * len, dirY * len, dirZ * len);
         GizmoDrawing.arrow(start, end, lookColor, lineWidth);
      }
   }
}