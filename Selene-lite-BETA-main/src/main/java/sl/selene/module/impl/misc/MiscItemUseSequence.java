package sl.selene.module.impl.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import sl.selene.module.api.Module;

@Environment(EnvType.CLIENT)
final class MiscItemUseSequence {
   private static final int USE_TIMEOUT_TICKS = 160;

   private static final int MIN_CONSUME_TICKS = 28;

   private Phase phase = Phase.IDLE;
   private int ticks;
   private int usingTicks;
   private int restoreSlot = -1;
   private int targetSlot = -1;
   private boolean useStarted;
   private boolean lastUseSuccessful;

   boolean isActive() {
      return phase != Phase.IDLE;
   }

   boolean consumeLastUseSuccessful() {
      boolean result = lastUseSuccessful;
      lastUseSuccessful = false;
      return result;
   }

   void start(int targetHotbarSlot, int previousHotbarSlot) {
      targetSlot = targetHotbarSlot;
      restoreSlot = previousHotbarSlot;
      phase = Phase.SELECT;
      ticks = 0;
      usingTicks = 0;
      useStarted = false;
      lastUseSuccessful = false;
   }

   void tick(int delayTicks) {
      if (phase == Phase.IDLE || Module.mc.player == null || Module.mc.interactionManager == null || Module.mc.options == null) {
         return;
      }

      int delay = Math.max(1, delayTicks);
      switch (phase) {
         case SELECT -> {
            Module.mc.player.getInventory().setSelectedSlot(targetSlot);
            phase = Phase.WAIT_BEFORE_USE;
            ticks = 0;
         }
         case WAIT_BEFORE_USE -> {
            ticks++;
            if (ticks >= delay) {
               phase = Phase.START_USE;
               ticks = 0;
            }
         }
         case START_USE -> {
            Module.mc.options.useKey.setPressed(true);
            Module.mc.interactionManager.interactItem(Module.mc.player, Hand.MAIN_HAND);
            phase = Phase.WAIT_USING;
            ticks = 0;
            usingTicks = 0;
            useStarted = false;
         }
         case WAIT_USING -> {
            Module.mc.options.useKey.setPressed(true);
            if (Module.mc.player.isUsingItem()) {
               useStarted = true;
               usingTicks++;
               ticks = 0;
            } else if (useStarted) {
               if (usingTicks >= MIN_CONSUME_TICKS) {
                  lastUseSuccessful = true;
                  finish();
               } else {
                  ticks++;
                  if (ticks >= delay) {
                     phase = Phase.START_USE;
                     ticks = 0;
                     usingTicks = 0;
                     useStarted = false;
                  }
               }
            } else {
               ticks++;
               if (ticks % delay == 0) {
                  Module.mc.interactionManager.interactItem(Module.mc.player, Hand.MAIN_HAND);
               }
               if (ticks >= USE_TIMEOUT_TICKS) {
                  finish();
               }
            }
         }
         case IDLE -> {
         }
      }
   }

   void cancel() {
      if (phase != Phase.IDLE) {
         finish();
      }
   }

   private void finish() {
      if (Module.mc.options != null) {
         Module.mc.options.useKey.setPressed(false);
      }
      if (Module.mc.player != null && restoreSlot >= 0 && restoreSlot <= 8) {
         Module.mc.player.getInventory().setSelectedSlot(restoreSlot);
      }
      phase = Phase.IDLE;
      ticks = 0;
      usingTicks = 0;
      restoreSlot = -1;
      targetSlot = -1;
      useStarted = false;
   }

   private enum Phase {
      IDLE,
      SELECT,
      WAIT_BEFORE_USE,
      START_USE,
      WAIT_USING
   }
}