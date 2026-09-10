package sl.selene.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.util.render.math.animation.anim.util.Easings;

@Environment(EnvType.CLIENT)
public class GuiInit extends GuiScreen {
   public static void init() {
      GuiScreen.animation = GuiScreen.animation.animate(1.0, 0.2F);
      GuiScreen.alphaPC.set(0.0);
      GuiScreen.alphaPC.run(1.0, 0.4F, Easings.CIRC_OUT);
      GuiScreen.openAnimation.set(0.0);
      GuiScreen.openAnimation.run(1.0, 0.52F, Easings.CUBIC_OUT);
      GuiScreen.exit = false;
      GuiScreen.mainAnimation.reset();
      GuiScreen.alpha.run(1.0);
      GuiScreen.resetWindowPosition();
      GuiScreen.cardHoverAnimations.clear();
      GuiScreen.categoryHoverAnimations.clear();
      GuiScreen.settingsNavHover = new float[5];
      GuiScreen.hoverSettingsIsland = 0.0F;
      GuiScreen.hoverConfigIsland = 0.0F;
      GuiScreen.hoverSearchIsland = 0.0F;
      GuiScreen.hoverCloseIsland = 0.0F;
      GuiScreen.configInputActive = false;
      GuiScreen.configInputText = "";
      GuiScreen.configPopupOpen = false;
      GuiScreen.configStatusText = "";
      GuiScreen.configStatusUntil = 0L;
      GuiScreen.configDeleteArmed = false;
      GuiScreen.configDeleteArmedUntil = 0L;
   }
}