package sl.selene.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import sl.selene.event.EventInit;
import sl.selene.event.impl.EventScreen;
import sl.selene.event.input.MouseButtonEvent;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.module.impl.visuals.HUD.HudEditor;
import sl.selene.module.impl.visuals.HUD.KeystrokesHUD;
import sl.selene.util.render.animation.util.Animation;
import sl.selene.util.render.animation.util.Easings;

@IModule(
   name = "Keystrokes",
   description = "Shows pressed keys and CPS in HUD style",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Keystrokes extends Module {

   public static SliderSetting animationSpeed = new SliderSetting("Animation Speed", 0.15F, 0.08F, 0.35F, 0.01F, false);
   public static BooleanSetting showCps = new BooleanSetting("Show CPS", true);
   public static SliderSetting keySize = new SliderSetting("Key Size", 34.0F, 26.0F, 48.0F, 1.0F, false);
   public static BooleanSetting showShadow = new BooleanSetting("Shadows", true);

   private static final Animation wAnim = new Animation();
   private static final Animation aAnim = new Animation();
   private static final Animation sAnim = new Animation();
   private static final Animation dAnim = new Animation();
   private static final Animation spaceAnim = new Animation();
   private static final Animation lmbAnim = new Animation();
   private static final Animation rmbAnim = new Animation();

   private static int leftClicks;
   private static long cpsWindowStart;

   public Keystrokes() {
      this.addSettings(new Setting[] { animationSpeed, showCps, keySize, showShadow });
   }

   @Override
   public void onEnable() {
      super.onEnable();
      resetCps();
   }

   @Override
   protected void onConfigLoadEnable() {
      this.mAnim.set(1.0);
      resetCps();
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      this.mAnim.update();
      if (mc.player == null || mc.getWindow() == null) {
         return;
      }

      long now = System.currentTimeMillis();
      if (cpsWindowStart == 0L || now - cpsWindowStart >= 1000L) {
         leftClicks = 0;
         cpsWindowStart = now;
      }

      long window = mc.getWindow().getHandle();
      updateKeyAnim(wAnim, isForwardPressed());
      updateKeyAnim(aAnim, isLeftPressed());
      updateKeyAnim(sAnim, isBackPressed());
      updateKeyAnim(dAnim, isRightPressed());
      updateKeyAnim(spaceAnim, isSpacePressed());
      updateKeyAnim(lmbAnim, GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
      updateKeyAnim(rmbAnim, GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS);
   }

   @EventInit
   public void onMouse(MouseButtonEvent event) {
      if (!this.enable || mc == null || mc.getWindow() == null || event.window() != mc.getWindow().getHandle()) {
         return;
      }

      if (!event.isPress()) {
         return;
      }

      if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         leftClicks++;
      }
   }

   @EventInit
   public void onRender(EventScreen event) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      HudEditor.renderElement("keystrokes", event.renderer(), () -> KeystrokesHUD.render(
         event.renderer(),
         showCps.get(),
         keySize.get(),
         showShadow.get(),
         (float)this.mAnim.get(),
         wAnim.get(),
         aAnim.get(),
         sAnim.get(),
         dAnim.get(),
         spaceAnim.get(),
         lmbAnim.get(),
         rmbAnim.get(),
         leftClicks
      ));
   }

   private static void updateKeyAnim(Animation animation, boolean pressed) {
      animation.update();
      animation.run(pressed ? 1.0 : 0.0, animationSpeed.get(), Easings.QUAD_OUT);
   }

   private static boolean isForwardPressed() {
      return mc.options.forwardKey.isPressed();
   }

   private static boolean isBackPressed() {
      return mc.options.backKey.isPressed();
   }

   private static boolean isLeftPressed() {
      return mc.options.leftKey.isPressed();
   }

   private static boolean isRightPressed() {
      return mc.options.rightKey.isPressed();
   }

   private static boolean isSpacePressed() {
      return mc.options.jumpKey.isPressed();
   }

   private static void resetCps() {
      leftClicks = 0;
      cpsWindowStart = System.currentTimeMillis();
   }
}