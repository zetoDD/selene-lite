package sl.selene.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import sl.selene.mixin.HandledScreenAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import sl.selene.Selene;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;

@IModule(
   name = "Shulker Preview",
   description = "Shows shulker contents on hover in a GUI",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class ShulkerPreview extends Module {
   public static final SliderSetting scale = new SliderSetting("Scale", 1.0F, 0.7F, 1.4F, 0.05F, false);
   public static final BooleanSetting showEmptySlots = new BooleanSetting("Empty Slots", true);

   public ShulkerPreview() {
      this.addSettings(new Setting[] { scale, showEmptySlots });
   }

   public static void renderPreview(HandledScreen<?> screen, DrawContext context, int mouseX, int mouseY) {
      if (!isEnabled()) {
         return;
      }

      Slot slot = ((HandledScreenAccessor) screen).selene$getSlotAt(mouseX, mouseY);
      if (slot == null || !slot.hasStack()) {
         return;
      }

      ItemStack stack = slot.getStack();
      if (!isShulkerBox(stack)) {
         return;
      }

      ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
      if (container == null) {
         return;
      }

      List<ItemStack> items = new ArrayList<>();
      container.iterateNonEmpty().forEach(items::add);
      if (items.isEmpty() && !showEmptySlots.get()) {
         return;
      }

      float slotSize = 18.0F * scale.get();
      int columns = 9;
      int rows = 3;
      float panelW = columns * slotSize + 8.0F;
      float panelH = rows * slotSize + 8.0F;
      float drawX = mouseX + 12.0F;
      float drawY = mouseY + 12.0F;
      int sw = screen.width;
      int sh = screen.height;
      if (drawX + panelW > sw) {
         drawX = mouseX - panelW - 12.0F;
      }

      if (drawY + panelH > sh) {
         drawY = mouseY - panelH - 12.0F;
      }

      int bg = 0xC0101010;
      int outline = 0x80FFFFFF;
      int ix = (int) drawX;
      int iy = (int) drawY;
      int iw = (int) panelW;
      int ih = (int) panelH;
      context.fill(ix, iy, ix + iw, iy + ih, bg);
      context.fill(ix, iy, ix + iw, iy + 1, outline);
      context.fill(ix, iy + ih - 1, ix + iw, iy + ih, outline);
      context.fill(ix, iy, ix + 1, iy + ih, outline);
      context.fill(ix + iw - 1, iy, ix + iw, iy + ih, outline);

      List<ItemStack> allStacks = new ArrayList<>();
      container.stream().forEach(allStacks::add);
      while (allStacks.size() < 27) {
         allStacks.add(ItemStack.EMPTY);
      }

      for (int i = 0; i < 27; i++) {
         ItemStack item = allStacks.get(i);
         if (item.isEmpty() && !showEmptySlots.get()) {
            continue;
         }

         int col = i % columns;
         int row = i / columns;
         float itemX = drawX + 4.0F + col * slotSize;
         float itemY = drawY + 4.0F + row * slotSize;
         context.getMatrices().pushMatrix();
         context.getMatrices().translate(itemX, itemY);
         context.getMatrices().scale(scale.get(), scale.get());
         if (!item.isEmpty()) {
            context.drawItem(item, 0, 0);
            context.drawStackOverlay(mc.textRenderer, item, 0, 0);
         }

         context.getMatrices().popMatrix();
      }
   }

   private static boolean isEnabled() {
      if (Selene.get == null || Selene.get.manager == null) {
         return false;
      }

      ShulkerPreview module = Selene.get.manager.get(ShulkerPreview.class);
      return module != null && module.enable;
   }

   private static boolean isShulkerBox(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
   }
}