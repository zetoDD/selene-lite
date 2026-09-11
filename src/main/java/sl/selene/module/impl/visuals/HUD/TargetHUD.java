package sl.selene.module.impl.visuals.HUD;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import sl.selene.module.impl.visuals.Hud;
import sl.selene.util.player.CrosshairTargetUtil;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.module.impl.visuals.HUD.HudEditor;
import sl.selene.util.render.animation.util.Animation;
import sl.selene.util.render.animation.util.Easings;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.ScaledResolution;
import sl.selene.util.render.motion.Motion;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public class TargetHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();
   private static float animatedHealthWidth = 0.0F;
   private static float animatedAbsorptionWidth = 0.0F;
   private static float animatedEntityHealthWidth = 0.0F;
   public static Animation openAnimation = new Animation();
   private static final List<TargetHUD.PendingItemRender> pendingItems = new ArrayList<>();
   private static float lastScale = 1.0F;
   private static float lastOriginX = 0.0F;
   private static float lastOriginY = 0.0F;

   public static void targetHUD(Renderer2D r2, DrawContext drawContext) {
      ScaledResolution sr = new ScaledResolution(mc);
      float baseWidth = 194.14F;
      float height = 63.34F;
      Entity currentTarget = resolveTarget();
      boolean chatOpen = mc.currentScreen instanceof ChatScreen;
      boolean shouldShow = false;
      if (currentTarget != null) {
         shouldShow = true;
      } else if (chatOpen && mc.player != null) {
         currentTarget = mc.player;
         shouldShow = true;
      }

      openAnimation.update();
      openAnimation.run(shouldShow ? 1.0 : 0.0, 0.8F, Easings.ELASTIC_OUT);
      float alpha = openAnimation.get();
      if (alpha <= 0.001F) {
         animatedHealthWidth = 0.0F;
         animatedAbsorptionWidth = 0.0F;
         animatedEntityHealthWidth = 0.0F;
      } else {
         float nameWidth = 0.0F;
         if (currentTarget != null) {
            String name = currentTarget instanceof CreeperEntity ? "Sad Creeper"
                  : currentTarget.getName().getString();
            nameWidth = r2.measureText(FontRegistry.INTER_MEDIUM, name, 28.0F).width;
         }

         float width = Math.max(baseWidth, 74.0F + nameWidth + 20.0F);
         float preferredX = (sr.getWidth() - width) / 2.0F;
         float preferredY = (sr.getHeight() - height) / 2.0F;
         DraggableManager.DragSession session = DraggableManager.getInstance().beginDrag("targetHUD", preferredX,
               preferredY, width, height);
         float x = session.positionX();
         float y = session.positionY() + (20.0F - 20.0F * alpha);

         float guiScale = mc.getWindow() != null ? mc.getWindow().getScaleFactor() : 1.0F;
         lastScale = HudEditor.getScale("targetHUD");
         lastOriginX = x / guiScale;
         lastOriginY = y / guiScale;

         HudEditor.registerRect(x, y, width, height);
         Hud.drawClientRect(r2, x, y, width, height, 13.0F, alpha, 1.0F);
         if (currentTarget instanceof PlayerEntity targetPlayer) {
            renderPlayer(r2, drawContext, targetPlayer, x, y, width, height, alpha);
         } else if (currentTarget instanceof LivingEntity livingEntity) {
            renderLiving(r2, livingEntity, x, y, width, height, alpha);
         }

         DraggableManager.getInstance().endDrag(session);
      }
   }

   private static Entity resolveTarget() {
      return CrosshairTargetUtil.getLivingCrosshairTarget();
   }

   private static void renderPlayer(Renderer2D r2, DrawContext drawContext, PlayerEntity targetPlayer, float x, float y,
         float width, float height, float alpha) {
      if (mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(targetPlayer.getUuid());
         if (entry != null) {
            drawPlayerHeadCustom(r2, entry.getSkinTextures().body().id(), x + 10.0F, y + 10.0F, 44.0F, alpha);
         }
      }

      float maxHP = targetPlayer.getMaxHealth();
      float hp = targetPlayer.getHealth();
      float abs = targetPlayer.getAbsorptionAmount();
      float barWidth = 105.0F;
      animatedHealthWidth = Motion.smooth(animatedHealthWidth, Math.min(hp / maxHP * barWidth, barWidth),
            Motion.BAR_SPEED);
      animatedAbsorptionWidth = Motion.smooth(animatedAbsorptionWidth,
            Math.min(abs / maxHP * barWidth, barWidth), Motion.BAR_SPEED);
      r2.pushAlpha(alpha);
      r2.text(FontRegistry.INTER_MEDIUM, x + 74.0F, y + 22.0F, 28.0F, targetPlayer.getName().getString(), -1);
      String hpString = String.format("%.1f", hp + abs);
      float hpTextX = x + 74.0F;
      r2.text(FontRegistry.INTER_MEDIUM, hpTextX, y + 40.0F, 24.0F, "HP - ", -1);
      hpTextX += r2.measureText(FontRegistry.INTER_MEDIUM, "HP - ", 24.0F).width;
      r2.text(FontRegistry.INTER_MEDIUM, hpTextX, y + 40.0F, 24.0F, hpString, Renderer2D.ColorUtil.getMainColor(1, 1));
      r2.rect(x + 74.0F, y + 48.0F, barWidth, 5.0F, 4.0F,
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 40));
      if (animatedHealthWidth > 0.0F) {
         r2.rect(x + 74.0F, y + 48.0F, animatedHealthWidth, 5.0F, 4.0F, Renderer2D.ColorUtil.getMainColor(1, 1));
      }

      if (animatedAbsorptionWidth > 0.0F) {
         r2.rect(x + 74.0F, y + 48.0F, animatedAbsorptionWidth, 5.0F, 4.0F, new Color(16767232).getRGB());
      }

      r2.popAlpha();
      if (targetPlayer.getArmor() > 0) {
         float armorAnim = (1.0F - alpha) * 20.0F;
         float armorX = x + width - 100.0F + armorAnim;
         float armorY = y - 35.0F - armorAnim;
         Hud.drawClientRect(r2, armorX, armorY, 100.0F, 26.0F, 8.0F, alpha, 1.0F);
         EquipmentSlot[] slots = new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
               EquipmentSlot.FEET };

         for (int i = 0; i < 4; i++) {
            float slotX = armorX + 6.0F + i * 24.0F;
            float slotY = armorY + 5.0F;
            int slotCol = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1),
                  (int) (40.0F * alpha));
            r2.rect(slotX, slotY, 17.0F, 17.0F, 3.0F, slotCol);
            ItemStack stack = targetPlayer.getEquippedStack(slots[i]);
            if (!stack.isEmpty()) {
               float guiScale = mc.getWindow().getScaleFactor();
               float rawX = (slotX + 4.5F) / guiScale;
               float rawY = (slotY + 4.5F) / guiScale;
               float scaledX = lastOriginX + (rawX - lastOriginX) * lastScale;
               float scaledY = lastOriginY + (rawY - lastOriginY) * lastScale;
               pendingItems.add(new TargetHUD.PendingItemRender(targetPlayer, stack, scaledX, scaledY, i, 0.5F * lastScale));
            } else {
               UiIcons.armor(r2, slotX + 8.5F, slotY + 8.5F, 11.0F, alpha * 0.45F);
            }
         }
      }
   }

   private static void renderLiving(Renderer2D r2, LivingEntity entity, float x, float y, float width, float height,
         float alpha) {
      r2.pushAlpha(alpha);
      UiIcons.crosshair(r2, x + 32.0F, y + 34.0F, 22.0F, alpha * 0.45F);
      String name = entity instanceof CreeperEntity ? "Sad Creeper" : entity.getName().getString();
      r2.text(FontRegistry.INTER_MEDIUM, x + 74.0F, y + 22.0F, 28.0F, name, -1);
      float targetWidth = Math.min(entity.getHealth() / entity.getMaxHealth() * 105.0F, 105.0F);
      animatedEntityHealthWidth = Motion.smooth(animatedEntityHealthWidth, targetWidth, Motion.BAR_SPEED);
      r2.rect(x + 74.0F, y + 48.0F, 105.0F, 5.0F, 4.0F,
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 40));
      r2.rect(x + 74.0F, y + 48.0F, animatedEntityHealthWidth, 5.0F, 4.0F, Renderer2D.ColorUtil.getMainColor(1, 1));
      r2.popAlpha();
   }

   public static void renderPendingItems(DrawContext drawContext) {
      if (!pendingItems.isEmpty()) {
         for (TargetHUD.PendingItemRender item : pendingItems) {
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(item.x, item.y);
            drawContext.getMatrices().scale(item.scale, item.scale);
            drawContext.drawItem(item.stack, -4, -4, item.seed);
            drawContext.drawStackOverlay(mc.textRenderer, item.stack, -4, -4);
            drawContext.getMatrices().popMatrix();
         }

         pendingItems.clear();
      }
   }

   private static void drawPlayerHeadCustom(Renderer2D r2, Identifier skinTexture, float x, float y, float size,
         float alpha) {
      try {
         TextureManager textureManager = mc.getTextureManager();
         if (textureManager == null) {
            return;
         }

         AbstractTexture texture = textureManager.getTexture(skinTexture);
         if (texture == null) {
            return;
         }

         if (!(texture.getGlTexture() instanceof GlTexture glTexture)) {
            return;
         }

         int textureId = glTexture.getGlId();
         if (textureId <= 0) {
            return;
         }

         GlStateManager._bindTexture(textureId);
         r2.pushAlpha(alpha);
         r2.drawRgbaTextureWithUVRounded(textureId, x, y, size, size, 0.125F, 0.125F, 0.25F, 0.25F, 7.0F);
         r2.drawRgbaTextureWithUVRounded(textureId, x, y, size, size, 0.625F, 0.125F, 0.75F, 0.25F, 7.0F);
         r2.popAlpha();
      } catch (Exception var11) {
      }
   }

   @Environment(EnvType.CLIENT)
   private static class PendingItemRender {
      PlayerEntity player;
      ItemStack stack;
      float x;
      float y;
      int seed;
      float scale;

      PendingItemRender(PlayerEntity player, ItemStack stack, float x, float y, int seed, float scale) {
         this.player = player;
         this.stack = stack;
         this.x = x;
         this.y = y;
         this.seed = seed;
         this.scale = scale;
      }
   }
}