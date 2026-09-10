package sl.selene.ui.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import sl.selene.Selene;
import sl.selene.config.GuiManager;
import sl.selene.module.api.Category;
import sl.selene.module.api.Manager;
import sl.selene.ui.gui.component.main.GuiCharTyped;
import sl.selene.ui.gui.component.main.GuiInit;
import sl.selene.ui.gui.component.main.GuiKeyPressed;
import sl.selene.ui.gui.component.main.GuiMouseDragged;
import sl.selene.ui.gui.component.main.GuiMouseReleased;
import sl.selene.ui.gui.component.main.GuiMouseScrolled;
import sl.selene.ui.gui.component.main.GuiShouldCloseOnEsc;
import sl.selene.ui.gui.component.mouse.GuiMouseClicked;
import sl.selene.ui.gui.component.render.GuiRender;
import sl.selene.ui.gui.map.GuiServerMapPanel;
import sl.selene.util.player.MovementManager;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.math.ScaledResolution;

@Environment(EnvType.CLIENT)
public class GuiClient extends Screen {
   public MinecraftClient mc = MinecraftClient.getInstance();

   public GuiClient() {
      super(Text.literal("Gui"));
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public void renderInGameBackground(DrawContext context) {
   }

   @Override
   public boolean mouseClicked(Click click, boolean bl) {
      Renderer2D renderer = Selene.getRenderer();
      return renderer != null && GuiMouseClicked.mouseClicked(renderer, click.x(), click.y(), click.button()) ? true : super.mouseClicked(click, bl);
   }

   @Override
   public boolean mouseReleased(Click click) {
      GuiMouseReleased.mouseReleased();
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseDragged(Click click, double pDragX, double pDragY) {
      return GuiMouseDragged.mouseDragged(click.x(), click.y(), click.button(), pDragX, pDragY) ? true : super.mouseDragged(click, pDragX, pDragY);
   }

   public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      return GuiMouseScrolled.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY) ? true : super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      return GuiKeyPressed.keyPressed(input.key(), input.scancode(), input.modifiers()) ? true : super.keyPressed(input);
   }

   @Override
   public boolean charTyped(CharInput input) {
      return GuiCharTyped.charTyped((char)input.codepoint(), input.modifiers()) ? true : super.charTyped(input);
   }

   public boolean shouldCloseOnEsc() {
      return GuiShouldCloseOnEsc.shouldCloseOnEsc();
   }

   public void close() {
      GuiSoundPlayer.playCloseSound();
      GuiServerMapPanel.setOpen(false);
      MovementManager.getInstance().unlockMovement("Search");
      GuiScreen.activeSearch = false;
      GuiScreen.searchText = "";
      GuiScreen.settingsPageOpen = false;
      GuiScreen.searchSelectAll = false;
      GuiScreen.configInputActive = false;
      GuiScreen.configInputText = "";
      Selene.get.guiManager.setGuiCategory(GuiScreen.selectedCategories);
      super.close();
   }

   public void tick() {
      super.tick();
      GuiServerMapPanel.tick();
      if (GuiScreen.exit && GuiScreen.alphaPC.isFinished()) {
         this.close();
         GuiScreen.exit = false;
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void init() {
      super.init();
      GuiInit.init();
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.mouse != null) {
         client.mouse.unlockCursor();
      }

      GuiScreen.width = 420.0F;
      GuiScreen.height = 268.0F;
      ScaledResolution scaledRes = new ScaledResolution(mc);
      int scaledWidth = scaledRes.getWidth();
      int scaledHeight = scaledRes.getHeight();
      GuiScreen.x = Math.max(0.0F, (scaledWidth - GuiScreen.width) / 2.0F);
      GuiScreen.y = Math.max(0.0F, (scaledHeight - GuiScreen.height) / 2.0F);
      GuiScreen.mainAnimation.reset();
      if (Selene.get.guiManager == null) {
         Selene.get.guiManager = new GuiManager();
         Selene.get.guiManager.init();
      }

      GuiScreen.categories = GuiScreen.resolveCategories();
      Category savedCategory = Selene.get.guiManager.getCurrentCategory();
      if (savedCategory == Category.Combat) {
         GuiScreen.selectedCategories = Category.Combat;
      } else if (savedCategory == Category.Movement) {
         GuiScreen.selectedCategories = Category.Movement;
      } else if (savedCategory == Category.Misc) {
         GuiScreen.selectedCategories = Category.Misc;
      } else if (savedCategory == Category.Donut) {
         GuiScreen.selectedCategories = Category.Donut;
      } else {
         GuiScreen.selectedCategories = savedCategory == Category.Utils ? Category.Utils : Category.Visuals;
      }
      Selene.get.guiManager.setGuiCategory(GuiScreen.selectedCategories);
      if (Selene.get.manager == null) {
         Selene.get.manager = new Manager();
      }

      GuiScreen.refreshCategoriesAndModules();
      GuiSoundPlayer.playOpenSound();
   }
}