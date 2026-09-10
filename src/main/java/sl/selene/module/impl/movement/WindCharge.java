package sl.selene.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import sl.selene.event.EventInit;
import sl.selene.event.lifecycle.ClientTickEvent;
import sl.selene.module.api.Category;
import sl.selene.module.api.IModule;
import sl.selene.module.api.Module;
import sl.selene.module.api.setting.Setting;
import sl.selene.module.api.setting.impl.BindSettings;
import sl.selene.module.api.setting.impl.BooleanSetting;
import sl.selene.module.api.setting.impl.SliderSetting;
import sl.selene.util.keyboard.Keyboard;

@IModule(
   name = "WindCharge",
   description = "Hold the key to launch yourself with a wind charge at your feet",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class WindCharge extends Module {

   public static BindSettings boostKey = new BindSettings("Boost Key", -1);
   public static BooleanSetting aimDown = new BooleanSetting("Aim Down", true);
   public static BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
   public static SliderSetting aimTicks = new SliderSetting("Aim Ticks", 1.0F, 1.0F, 3.0F, 1.0F, false);

   private State state = State.IDLE;
   private int tickCount = 0;
   private int chargeSlot = -1;
   private int originalSlot = -1;
   private float originalPitch = 0.0F;
   private boolean keyWasDown = false;

   public WindCharge() {
      this.addSettings(new Setting[] { boostKey, aimDown, swapBack, aimTicks });
   }

   @Override
   public void onDisable() {
      super.onDisable();
      restoreAll();
      keyWasDown = false;
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
         restoreAll();
         keyWasDown = false;
         return;
      }

      if (state == State.IDLE) {
         boolean keyDown = isKeyDown(boostKey.get());
         boolean keyPressed = keyDown && !keyWasDown;
         keyWasDown = keyDown;
         if (keyPressed) {
            startBoost();
         }
         return;
      }

      tickCount++;
      switch (state) {
         case AIM -> {
            if (tickCount == 1 && aimDown.get()) {
               mc.player.setPitch(90.0F);
            }
            if (tickCount >= (int) aimTicks.get()) {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               tickCount = 0;
               state = State.RESTORE;
            }
         }
         case RESTORE -> {
            if (tickCount >= 1) {
               restoreAll();
            }
         }
         default -> state = State.IDLE;
      }
   }

   private void startBoost() {
      if (mc.player.isUsingItem()) {
         return;
      }
      if (mc.player.getItemCooldownManager().isCoolingDown(Items.WIND_CHARGE.getDefaultStack())) {
         return;
      }
      chargeSlot = findInHotbar();
      if (chargeSlot == -1) {
         return;
      }
      originalSlot = mc.player.getInventory().getSelectedSlot();
      originalPitch = mc.player.getPitch();
      mc.player.getInventory().setSelectedSlot(chargeSlot);
      tickCount = 0;
      state = State.AIM;
   }

   private int findInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) {
            return i;
         }
      }
      return -1;
   }

   private void restoreAll() {
      if (originalSlot != -1 && mc.player != null) {
         if (swapBack.get()) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
         }
         mc.player.setPitch(originalPitch);
      }
      originalSlot = -1;
      chargeSlot = -1;
      state = State.IDLE;
      tickCount = 0;
   }

   private boolean isKeyDown(int keyCode) {
      return Keyboard.isKeyDown(keyCode);
   }

   private enum State {
      IDLE,
      AIM,
      RESTORE
   }
}