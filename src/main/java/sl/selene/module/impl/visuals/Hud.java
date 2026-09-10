package sl.selene.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventScreen;
import sl.selene.event.input.MouseButtonEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.MultiBooleanSetting;
import sl.selene.module.impl.utils.Optimizer;
import sl.selene.module.impl.visuals.HUD.ArrayListHUD;
import sl.selene.module.impl.visuals.HUD.HotBarHUD;
import sl.selene.module.impl.visuals.HUD.HudEditor;
import sl.selene.module.impl.visuals.HUD.InformationHUD;
import sl.selene.module.impl.visuals.HUD.KeyBindHUD;
import sl.selene.module.impl.visuals.HUD.PotionsHUD;
import sl.selene.module.impl.visuals.HUD.TargetHUD;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.animation.util.Animation;
import sl.selene.util.render.animation.util.Easings;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.ui.gui.component.render.GlassStyle;

@IModule(
   name = "Hud",
   description = " ",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Hud extends Module {

   public static Animation animC = new Animation();
   public static MultiBooleanSetting element = new MultiBooleanSetting(
      "HUD Elements",
      new BooleanSetting("ArrayList", true),
      new BooleanSetting("Info", true),
      new BooleanSetting("Target HUD", true),
      new BooleanSetting("Notifications", true),
      new BooleanSetting("Bind List", true),
      new BooleanSetting("Potion List", true),
      new BooleanSetting("Bind Items", true),
      new BooleanSetting("Reach", true),
      new BooleanSetting("Hotbar", true)
   );
   public static MultiBooleanSetting notify = new MultiBooleanSetting(
      "Notification Settings",
      new BooleanSetting("Modules", true),
      new BooleanSetting("Low Health", true),
      new BooleanSetting("Low Armor Durability", true)
   );
   private static final float NOTIFICATION_EDGE_MARGIN = 18.0F;
   private static final float NOTIFICATION_HEIGHT = 46.0F;
   private static final float NOTIFICATION_SPACING = 8.0F;
   private static final float NOTIFICATION_TEXT_SIZE = 21.0F;
   private static final float NOTIFICATION_TOGGLE_WIDTH = 34.0F;
   private static final float NOTIFICATION_TOGGLE_HEIGHT = 16.0F;
   private static final float NOTIFICATION_TEXT_LEFT = 32.0F;
   private static final float NOTIFICATION_TEXT_TOGGLE_GAP = 13.0F;
   private static final float NOTIFICATION_RIGHT_PADDING = 16.0F;
   private static final float NOTIFICATION_MIN_WIDTH = 220.0F;
   private static final float NOTIFICATION_MAX_WIDTH = 430.0F;
   private static final float NOTIFICATION_ACCENT_X = 14.0F;
   private static final float NOTIFICATION_ACCENT_Y = 12.0F;
   private static final float NOTIFICATION_ACCENT_WIDTH = 4.0F;
   private static final float NOTIFICATION_ACCENT_HEIGHT = 20.0F;
   private static final float NOTIFICATION_PROGRESS_HEIGHT = 2.0F;
   private final List<Hud.Notification> notifications = new ArrayList<>();

   public static enum NotificationType {
      SUCCESS("on"),
      WARNING("warn"),
      INFO("cfg"),
      ERROR("off"),
      CONFIG("cfg");

      private final String legacyIcon;

      private NotificationType(String legacyIcon) {
         this.legacyIcon = legacyIcon;
      }

      public String legacyIcon() {
         return this.legacyIcon;
      }
   }

   public Hud() {
      this.addSettings(new Setting[]{element, notify});
   }

   @EventInit
   public void onRender(EventScreen e) {
      if (mc.player != null && mc.world != null) {
         Renderer2D r2 = e.renderer();
         if (r2 != null) {
            HotBarHUD.tick();
            if (element.get("Notifications")) {
               HudEditor.renderElement("notifications", r2, () -> this.renderNotifications(r2));
            }

            if (element.get("ArrayList")) {
               HudEditor.renderElement("arraylist", r2, () -> ArrayListHUD.arraylist(r2));
            }

            if (element.get("Info")) {
               HudEditor.renderElement("information", r2, () -> InformationHUD.information(r2));
            }

            if (element.get("Target HUD")) {
               HudEditor.renderElement("targetHUD", r2, () -> TargetHUD.targetHUD(r2, e.drawContext()));
            }

            if (element.get("Potion List")) {
               HudEditor.renderElement("potions", r2, () -> PotionsHUD.potions(r2));
            }

            if (element.get("Hotbar")) {
               HudEditor.renderElement("hotbar", r2, () -> HotBarHUD.hotbar(r2, e.drawContext()));
            }

            if (element.get("Bind List")) {
               HudEditor.renderElement("keybinds", r2, () -> KeyBindHUD.keybind(r2));
            }

            HudEditor.renderPopup(r2);
         }
      }
   }

   @EventInit
   public void onMouseButton(MouseButtonEvent event) {
      HudEditor.onMouseButton(event);
   }

   public void showNotification(String icon, String text, long duration) {
      this.trimNotifications();
      Hud.Notification notification = new Hud.Notification(icon, text, duration);
      this.notifications.add(notification);
   }

   public void showNotification(String icon, String text, long duration, int iconColor) {
      this.trimNotifications();
      Hud.Notification notification = new Hud.Notification(icon, text, duration, iconColor);
      this.notifications.add(notification);
   }

   private void trimNotifications() {
      int maxStored = Math.max(Optimizer.getMaxNotifications(), 12);
      if (this.notifications.size() >= maxStored) {
         this.notifications.subList(0, this.notifications.size() - maxStored + 1).clear();
      }
   }

   public void showNotification(String icon, String text) {
      this.showNotification(icon, text, 3000L);
   }

   public void showNotification(String text, Hud.NotificationType type, long duration) {
      String icon = type != null ? type.legacyIcon() : "cfg";
      this.showNotification(icon, text, duration);
   }

   public void showNotification(String text, Hud.NotificationType type) {
      this.showNotification(text, type, 3000L);
   }

   public void renderNotifications(Renderer2D matrix) {
      this.notifications.removeIf(notification -> {
         notification.animation.update();
         notification.yAnimation.update();
         return notification.isExpired() && notification.animation.getValue() <= 0.01;
      });
      if (this.notifications.isEmpty() || mc.getWindow() == null) {
         return;
      }

      int viewportWidth = mc.getWindow().getFramebufferWidth();
      int viewportHeight = mc.getWindow().getFramebufferHeight();
      if (viewportWidth <= 0 || viewportHeight <= 0) {
         return;
      }

      int maxNotifications = Math.max(1, Optimizer.getMaxNotifications());
      float measureMaxWidth = 0.0F;
      float measureTotalHeight = 0.0F;
      int measured = 0;
      for (int i = this.notifications.size() - 1; i >= 0 && measured < maxNotifications; i--) {
         Hud.Notification notification = this.notifications.get(i);
         boolean shouldShow = !notification.isExpired();
         notification.animation.run(shouldShow ? 1.0 : 0.0, Optimizer.getHudAnimationSpeed(0.16F), Easings.QUAD_OUT, false);
         float animValue = notification.animation.get();
         if (animValue > 0.01F) {
            measureMaxWidth = Math.max(measureMaxWidth, notificationWidth(matrix, notification));
            measureTotalHeight += (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING) * animValue;
            measured++;
         }
      }

      if (measureMaxWidth <= 0.0F) {
         return;
      }

      float containerHeight = Math.max(measureTotalHeight, NOTIFICATION_HEIGHT);
      float preferredX = viewportWidth - measureMaxWidth - NOTIFICATION_EDGE_MARGIN;
      float preferredY = viewportHeight - containerHeight - NOTIFICATION_EDGE_MARGIN;
      DraggableManager.DragSession dragSession = DraggableManager.getInstance()
            .beginDrag("notifications", preferredX, preferredY, measureMaxWidth, containerHeight);
      float anchorX = dragSession.positionX();
      float anchorY = dragSession.positionY();
      float stackRight = anchorX + measureMaxWidth;
      float currentY = anchorY + containerHeight - NOTIFICATION_HEIGHT;
      int rendered = 0;

      for (int i = this.notifications.size() - 1; i >= 0 && rendered < maxNotifications; i--) {
         Hud.Notification notification = this.notifications.get(i);
         float animValue = notification.animation.get();
         if (animValue > 0.01F) {
            float notificationWidth = notificationWidth(matrix, notification);
            float targetX = stackRight - notificationWidth;
            float slideX = (1.0F - animValue) * 18.0F;
            float animatedX = targetX + slideX;
            float scale = 0.97F + 0.03F * animValue;
            float centerX = animatedX + notificationWidth * 0.5F;
            float centerY = currentY + NOTIFICATION_HEIGHT * 0.5F;
            float toggleX = animatedX + notificationWidth - NOTIFICATION_RIGHT_PADDING - NOTIFICATION_TOGGLE_WIDTH;
            float textMaxWidth = Math.max(1.0F, toggleX - NOTIFICATION_TEXT_TOGGLE_GAP - (animatedX + NOTIFICATION_TEXT_LEFT));
            String label = displayNotificationText(matrix, notification, textMaxWidth);
            int textColor = Renderer2D.ColorUtil.rgba(255, 255, 255, Math.round(245.0F * animValue));
            int accentColor = notificationAccentColor(notification);
            matrix.pushScale(scale, scale, centerX, centerY);
            drawClientRect(matrix, animatedX, currentY, notificationWidth, NOTIFICATION_HEIGHT, 12.0F, animValue, 1.0F);
            matrix.rect(
                  animatedX + NOTIFICATION_ACCENT_X,
                  currentY + NOTIFICATION_ACCENT_Y,
                  NOTIFICATION_ACCENT_WIDTH,
                  NOTIFICATION_ACCENT_HEIGHT,
                  1.5F,
                  ColorUtil.replAlpha(accentColor, Math.round(225.0F * animValue))
            );
            matrix.text(
                  FontRegistry.INTER_SEMIBOLD,
                  animatedX + NOTIFICATION_TEXT_LEFT,
                  currentY + 29.0F,
                  NOTIFICATION_TEXT_SIZE,
                  label,
                  textColor
            );
            GlassStyle.toggle(
                  matrix,
                  toggleX,
                  currentY + (NOTIFICATION_HEIGHT - NOTIFICATION_TOGGLE_HEIGHT) * 0.5F,
                  NOTIFICATION_TOGGLE_WIDTH,
                  NOTIFICATION_TOGGLE_HEIGHT,
                  animValue,
                  notificationToggleProgress(notification)
            );
            float remaining = 1.0F - notification.getProgress();
            if (remaining > 0.0F) {
               matrix.rect(
                     animatedX + NOTIFICATION_RIGHT_PADDING,
                     currentY + NOTIFICATION_HEIGHT - NOTIFICATION_PROGRESS_HEIGHT - 5.0F,
                     (notificationWidth - NOTIFICATION_RIGHT_PADDING * 2.0F) * remaining,
                     NOTIFICATION_PROGRESS_HEIGHT,
                     1.0F,
                     ColorUtil.replAlpha(accentColor, Math.round(125.0F * animValue))
               );
            }
            matrix.popScale();
            currentY -= (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING) * animValue;
            rendered++;
         }
      }

      HudEditor.registerRect(anchorX, anchorY, measureMaxWidth, containerHeight);
      DraggableManager.getInstance().endDrag(dragSession);
   }

   private static float notificationWidth(Renderer2D renderer, Notification notification) {
      String text = notification.text == null ? "" : notification.text;
      float textWidth = renderer.measureText(FontRegistry.INTER_SEMIBOLD, text, NOTIFICATION_TEXT_SIZE).width;
      float contentWidth = NOTIFICATION_TEXT_LEFT + textWidth + NOTIFICATION_TEXT_TOGGLE_GAP
            + NOTIFICATION_TOGGLE_WIDTH + NOTIFICATION_RIGHT_PADDING;
      return Math.max(NOTIFICATION_MIN_WIDTH, Math.min(NOTIFICATION_MAX_WIDTH, contentWidth));
   }

   private static String displayNotificationText(Renderer2D renderer, Notification notification, float maxWidth) {
      String text = notification.text == null ? "" : notification.text;
      if (renderer.measureText(FontRegistry.INTER_SEMIBOLD, text, NOTIFICATION_TEXT_SIZE).width <= maxWidth) {
         return text;
      }

      String suffix = "...";
      int end = text.length();
      while (end > 0 && renderer.measureText(FontRegistry.INTER_SEMIBOLD, text.substring(0, end) + suffix, NOTIFICATION_TEXT_SIZE).width > maxWidth) {
         end--;
      }
      return end > 0 ? text.substring(0, end) + suffix : suffix;
   }

   private static int notificationAccentColor(Notification notification) {
      if (notification.iconColor != -1) {
         return ColorUtil.replAlpha(notification.iconColor, 255);
      }

      String icon = notification.icon;
      if (icon != null) {
         if ("on".equalsIgnoreCase(icon) || "success".equalsIgnoreCase(icon)) {
            return Renderer2D.ColorUtil.rgba(105, 224, 164, 255);
         }
         if ("off".equalsIgnoreCase(icon) || "error".equalsIgnoreCase(icon)) {
            return Renderer2D.ColorUtil.rgba(255, 112, 126, 255);
         }
         if ("warn".equalsIgnoreCase(icon) || "warning".equalsIgnoreCase(icon)) {
            return Renderer2D.ColorUtil.rgba(255, 198, 101, 255);
         }
      }
      return Renderer2D.ColorUtil.rgba(137, 181, 255, 255);
   }

   private static float notificationToggleProgress(Notification notification) {
      String icon = notification.icon;
      if (icon == null) {
         return 0.5F;
      }
      if ("on".equalsIgnoreCase(icon) || "success".equalsIgnoreCase(icon)) {
         return 1.0F;
      }
      if ("off".equalsIgnoreCase(icon) || "error".equalsIgnoreCase(icon)
            || "warn".equalsIgnoreCase(icon) || "warning".equalsIgnoreCase(icon)) {
         return 0.0F;
      }
      return 0.5F;
   }

   public static void drawClientRect(Renderer2D r2, float x, float y, float w, float h, float radius, float alpha, float thickness) {
      if (alpha <= 0.001F || w <= 0.0F || h <= 0.0F) {
         return;
      }

      if (Optimizer.shouldRenderHudShadow()) {
         r2.shadow(x, y, w, h, radius, 8.6F, 0.5F, ColorUtil.getColor(0, alpha * 0.15F));
      }

      GlassStyle.fill(r2, x, y, w, h, radius, alpha);
      r2.rect(
         x,
         y,
         w,
         h,
         radius,
         Renderer2D.ColorUtil.rgba(8, 12, 20, (int)(10.0F * alpha))
      );
      GlassStyle.outline(r2, x, y, w, h, radius, alpha);
   }

   @Environment(EnvType.CLIENT)
   public static class Notification {
      private String icon;
      private String text;
      private long createTime;
      private long duration;
      private Animation animation = new Animation();
      private Animation yAnimation = new Animation();
      private int iconColor = -1;

      public Notification(String icon, String text, long duration) {
         this.icon = icon;
         this.text = text;
         this.duration = duration;
         this.createTime = System.currentTimeMillis();
      }

      public Notification(String icon, String text, long duration, int iconColor) {
         this.icon = icon;
         this.text = text;
         this.duration = duration;
         this.createTime = System.currentTimeMillis();
         this.iconColor = iconColor;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() - this.createTime > this.duration;
      }

      public float getProgress() {
         return Math.min(1.0F, (float)(System.currentTimeMillis() - this.createTime) / (float)this.duration);
      }

      @Generated
      public String getIcon() {
         return this.icon;
      }

      @Generated
      public String getText() {
         return this.text;
      }

      @Generated
      public long getCreateTime() {
         return this.createTime;
      }

      @Generated
      public long getDuration() {
         return this.duration;
      }

      @Generated
      public Animation getAnimation() {
         return this.animation;
      }

      @Generated
      public Animation getYAnimation() {
         return this.yAnimation;
      }

      @Generated
      public int getIconColor() {
         return this.iconColor;
      }

      @Generated
      public void setIcon(String icon) {
         this.icon = icon;
      }

      @Generated
      public void setText(String text) {
         this.text = text;
      }

      @Generated
      public void setCreateTime(long createTime) {
         this.createTime = createTime;
      }

      @Generated
      public void setDuration(long duration) {
         this.duration = duration;
      }

      @Generated
      public void setAnimation(Animation animation) {
         this.animation = animation;
      }

      @Generated
      public void setYAnimation(Animation yAnimation) {
         this.yAnimation = yAnimation;
      }

      @Generated
      public void setIconColor(int iconColor) {
         this.iconColor = iconColor;
      }

      @Generated
      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof Hud.Notification other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else if (this.getCreateTime() != other.getCreateTime()) {
            return false;
         } else if (this.getDuration() != other.getDuration()) {
            return false;
         } else if (this.getIconColor() != other.getIconColor()) {
            return false;
         } else {
            Object this$icon = this.getIcon();
            Object other$icon = other.getIcon();
            if (this$icon == null ? other$icon == null : this$icon.equals(other$icon)) {
               Object this$text = this.getText();
               Object other$text = other.getText();
               if (this$text == null ? other$text == null : this$text.equals(other$text)) {
                  Object this$animation = this.getAnimation();
                  Object other$animation = other.getAnimation();
                  if (this$animation == null ? other$animation == null : this$animation.equals(other$animation)) {
                     Object this$yAnimation = this.getYAnimation();
                     Object other$yAnimation = other.getYAnimation();
                     return this$yAnimation == null ? other$yAnimation == null : this$yAnimation.equals(other$yAnimation);
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      @Generated
      protected boolean canEqual(Object other) {
         return other instanceof Hud.Notification;
      }

      @Generated
      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         long $createTime = this.getCreateTime();
         result = result * 59 + (int)($createTime >>> 32 ^ $createTime);
         long $duration = this.getDuration();
         result = result * 59 + (int)($duration >>> 32 ^ $duration);
         result = result * 59 + this.getIconColor();
         Object $icon = this.getIcon();
         result = result * 59 + ($icon == null ? 43 : $icon.hashCode());
         Object $text = this.getText();
         result = result * 59 + ($text == null ? 43 : $text.hashCode());
         Object $animation = this.getAnimation();
         result = result * 59 + ($animation == null ? 43 : $animation.hashCode());
         Object $yAnimation = this.getYAnimation();
         return result * 59 + ($yAnimation == null ? 43 : $yAnimation.hashCode());
      }

      @Generated
      @Override
      public String toString() {
         return "Hud.Notification(icon="
            + this.getIcon()
            + ", text="
            + this.getText()
            + ", createTime="
            + this.getCreateTime()
            + ", duration="
            + this.getDuration()
            + ", animation="
            + this.getAnimation()
            + ", yAnimation="
            + this.getYAnimation()
            + ", iconColor="
            + this.getIconColor()
            + ")";
      }
   }
}