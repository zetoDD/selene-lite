package sl.selene.module.impl.visuals.HUD;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import sl.selene.event.EventInit;
import sl.selene.event.player.AttackEvent;
import sl.selene.module.impl.visuals.Hud;
import sl.selene.module.impl.visuals.HUD.HudEditor;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.util.render.animation.util.Easings;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.ScaledResolution;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public class InformationHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();
   private static double prevX = 0.0;
   private static double prevY = 0.0;
   private static double prevZ = 0.0;
   private static long prevTime = 0L;
   private static float bps = 0.0F;
   private static double lastReach = 0.0;

   @EventInit
   public static void onAttack(AttackEvent event) {
      if (mc.player == null || event == null || event.isCancelled()) {
         return;
      }

      Entity target = event.getTarget();
      if (target == null || !target.isAlive()) {
         return;
      }

      Vec3d eye = mc.player.getEyePos();
      Box box = target.getBoundingBox();
      double closestX = Math.max(box.minX, Math.min(eye.x, box.maxX));
      double closestY = Math.max(box.minY, Math.min(eye.y, box.maxY));
      double closestZ = Math.max(box.minZ, Math.min(eye.z, box.maxZ));
      double dx = eye.x - closestX;
      double dy = eye.y - closestY;
      double dz = eye.z - closestZ;
      lastReach = Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static void information(Renderer2D r2) {
      Hud.animC.update();
      Color mainColorGlow = Renderer2D.ColorUtil.getColor(Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 50));
      ScaledResolution sr = new ScaledResolution(mc);
      boolean chat = mc.currentScreen instanceof ChatScreen;
      Hud.animC.run(chat ? 1.0 : 0.0, 0.8F, Easings.CIRC_OUT, false);
      float preferredY = sr.getHeight() * 2 - 75.0F + (20.0F + -20.0F * Hud.animC.get());
      long currentTime = System.currentTimeMillis();
      if (prevTime != 0L && currentTime - prevTime >= 50L) {
         double dx = mc.player.getX() - prevX;
         double dy = mc.player.getY() - prevY;
         double dz = mc.player.getZ() - prevZ;
         double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
         double timeDelta = (currentTime - prevTime) / 1000.0;
         if (timeDelta > 0.0) {
            bps = (float)(distance / timeDelta);
         }

         prevX = mc.player.getX();
         prevY = mc.player.getY();
         prevZ = mc.player.getZ();
         prevTime = currentTime;
      } else if (prevTime == 0L) {
         prevX = mc.player.getX();
         prevY = mc.player.getY();
         prevZ = mc.player.getZ();
         prevTime = currentTime;
      }

      int playerX = (int)mc.player.getX();
      int playerY = (int)mc.player.getY();
      int playerZ = (int)mc.player.getZ();
      String xStr = String.valueOf(playerX);
      String yStr = String.valueOf(playerY);
      String zStr = String.valueOf(playerZ);
      float fontSize = 28.0F;
      boolean showReach = Hud.element.get("Reach");
      String bpsValue = String.format("%.1f", bps);
      String reachValue = String.format("%.2f", lastReach);

      float xStrW = r2.measureText(FontRegistry.INTER_MEDIUM, xStr, fontSize).width;
      float xLabelW = r2.measureText(FontRegistry.INTER_MEDIUM, "x ", fontSize).width;
      float yStrW = r2.measureText(FontRegistry.INTER_MEDIUM, yStr, fontSize).width;
      float yLabelW = r2.measureText(FontRegistry.INTER_MEDIUM, "y ", fontSize).width;
      float zStrW = r2.measureText(FontRegistry.INTER_MEDIUM, zStr, fontSize).width;
      float zLabelW = r2.measureText(FontRegistry.INTER_MEDIUM, "z", fontSize).width;
      float bpsValW = r2.measureText(FontRegistry.INTER_MEDIUM, bpsValue, fontSize).width;
      float bpsLabelW = r2.measureText(FontRegistry.INTER_MEDIUM, "b/s", fontSize).width;
      float reachValW = r2.measureText(FontRegistry.INTER_MEDIUM, reachValue, fontSize).width;
      float reachLabelW = r2.measureText(FontRegistry.INTER_MEDIUM, "m", fontSize).width;

      float coordsTextW = xStrW + xLabelW + yStrW + yLabelW + zStrW + zLabelW;

      float iconSquare = 40.64F;
      float dividerW = 2.34F;
      float dividerGap = 15.0F;
      float segIcon = 15.0F;
      float iconTextGap = 4.0F;
      float leftPad = 42.0F;
      float rightPad = 16.0F;

      float coordsTextStart = leftPad;
      float div1Rel = coordsTextStart + coordsTextW + dividerGap;
      float bpsIconRel = div1Rel + dividerW + dividerGap;
      float bpsTextStartRel = bpsIconRel + segIcon + iconTextGap;
      float bpsEndRel = bpsTextStartRel + bpsValW + bpsLabelW;
      float div2Rel = bpsEndRel + dividerGap;
      float reachIconRel = div2Rel + dividerW + dividerGap;
      float reachTextStartRel = reachIconRel + segIcon + iconTextGap;
      float reachEndRel = reachTextStartRel + reachValW + reachLabelW;

      float contentWidth = (showReach ? reachEndRel : bpsEndRel) + rightPad;
      float contentGap = 49.6F - iconSquare;
      float boundsWidth = iconSquare + contentGap + contentWidth;
      float boundsHeight = iconSquare;

      DraggableManager.DragSession dragSession = DraggableManager.getInstance().beginDrag(
         "information", 20.0F, preferredY, boundsWidth, boundsHeight
      );
      float x = dragSession.positionX();
      float y = dragSession.positionY();
      float cx = x + iconSquare + contentGap;
      float cy = y + 20.32F;
      float textY = y + 25.0F;

      Hud.drawClientRect(r2, x, y, iconSquare, iconSquare, 13.0F, 1.0F, 1.0F);
      r2.shadow(x + 15.0F, y + 22.0F, 0.1F, 0.1F, 8.0F, 10.0F, 0.1F, mainColorGlow.getRGB());
      UiIcons.info(r2, x + 20.32F, cy, 18.0F, 1.0F);

      Hud.drawClientRect(r2, cx, y, contentWidth, iconSquare, 13.0F, 1.0F, 1.0F);

      UiIcons.coordinates(r2, cx + 30.0F, cy, 16.0F, 1.0F);
      float textX = cx + coordsTextStart;
      r2.text(FontRegistry.INTER_MEDIUM, textX, textY, fontSize, xStr, Renderer2D.ColorUtil.getTextColor(1, 1));
      textX += xStrW;
      r2.text(FontRegistry.INTER_MEDIUM, textX, textY, fontSize, "x ", Renderer2D.ColorUtil.getMainColor(1, 1));
      textX += xLabelW;
      r2.text(FontRegistry.INTER_MEDIUM, textX, textY, fontSize, yStr, Renderer2D.ColorUtil.getTextColor(1, 1));
      textX += yStrW;
      r2.text(FontRegistry.INTER_MEDIUM, textX, textY, fontSize, "y ", Renderer2D.ColorUtil.getMainColor(1, 1));
      textX += yLabelW;
      r2.text(FontRegistry.INTER_MEDIUM, textX, textY, fontSize, zStr, Renderer2D.ColorUtil.getTextColor(1, 1));
      textX += zStrW;
      r2.text(FontRegistry.INTER_MEDIUM, textX, textY, fontSize, "z", Renderer2D.ColorUtil.getMainColor(1, 1));

      r2.rect(cx + div1Rel, y + 15.0F, dividerW, 11.21F, 4.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 80));
      UiIcons.speed(r2, cx + bpsIconRel + segIcon * 0.5F, cy, segIcon, 1.0F);
      float bpsTextX = cx + bpsTextStartRel;
      r2.text(FontRegistry.INTER_MEDIUM, bpsTextX, textY, fontSize, bpsValue, Renderer2D.ColorUtil.getTextColor(1, 1));
      bpsTextX += bpsValW;
      r2.text(FontRegistry.INTER_MEDIUM, bpsTextX, textY, fontSize, "b/s", Renderer2D.ColorUtil.getMainColor(1, 1));

      if (showReach) {
         r2.rect(cx + div2Rel, y + 15.0F, dividerW, 11.21F, 4.0F, Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 80));
         UiIcons.crosshair(r2, cx + reachIconRel + segIcon * 0.5F, cy, segIcon, 1.0F);
         float reachTextX = cx + reachTextStartRel;
         r2.text(FontRegistry.INTER_MEDIUM, reachTextX, textY, fontSize, reachValue, Renderer2D.ColorUtil.getTextColor(1, 1));
         reachTextX += reachValW;
         r2.text(FontRegistry.INTER_MEDIUM, reachTextX, textY, fontSize, "m", Renderer2D.ColorUtil.getMainColor(1, 1));
      }

      HudEditor.registerRect(x, y, boundsWidth, boundsHeight);
      DraggableManager.getInstance().endDrag(dragSession);
   }
}