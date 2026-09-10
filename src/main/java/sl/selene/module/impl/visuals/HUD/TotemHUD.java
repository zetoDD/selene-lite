package sl.selene.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import sl.selene.module.impl.visuals.Hud;
import sl.selene.module.impl.visuals.HUD.HudEditor;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.UiIcons;

@Environment(EnvType.CLIENT)
public class TotemHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();

   public static void totem(Renderer2D r2) {
      if (mc.player == null || mc.getWindow() == null) {
         return;
      }

      int count = countTotems();
      boolean inOffhand = !mc.player.getOffHandStack().isEmpty()
            && mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;

      String label = inOffhand ? "Totem: " + count + " +off" : "Totem: " + count;
      float fontSize = 24.0F;
      float textW = r2.measureText(FontRegistry.INTER_MEDIUM, label, fontSize).width;
      float iconSize = 15.0F;
      float padding = 12.0F;
      float iconGap = 6.0F;
      float width = padding * 2.0F + iconSize + iconGap + textW;
      float height = 36.0F;

      float screenX = mc.getWindow().getWidth() / 2.0F;
      float screenY = mc.getWindow().getHeight() - 76.0F;

      DraggableManager.DragSession session = DraggableManager.getInstance().beginDrag(
         "totem", screenX - width / 2.0F, screenY - height / 2.0F, width, height
      );
      float x = session.positionX();
      float y = session.positionY();
      float cy = y + height / 2.0F;

      Hud.drawClientRect(r2, x, y, width, height, 9.0F, 1.0F, 1.0F);
      UiIcons.armor(r2, x + padding + iconSize * 0.5F, cy, iconSize, 1.0F);
      r2.text(
         FontRegistry.INTER_MEDIUM,
         x + padding + iconSize + iconGap,
         y + height / 2.0F + 2.0F,
         fontSize,
         label,
         Renderer2D.ColorUtil.getTextColor(1, 1)
      );

      HudEditor.registerRect(x, y, width, height);
      DraggableManager.getInstance().endDrag(session);
   }

   private static int countTotems() {
      if (mc.player == null) {
         return 0;
      }
      int count = 0;
      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == Items.TOTEM_OF_UNDYING) {
            count += stack.getCount();
         }
      }
      return count;
   }
}