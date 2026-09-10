package sl.selene.module.impl.misc;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import sl.selene.Selene;
import sl.selene.module.api.Module;
import sl.selene.module.impl.visuals.Hud;

@Environment(EnvType.CLIENT)
final class MiscUtil {
   private MiscUtil() {
   }

   static void sendCommand(String command) {
      if (Module.mc.player == null || Module.mc.player.networkHandler == null || command == null) {
         return;
      }

      String raw = command.trim();
      if (raw.isEmpty()) {
         return;
      }

      String withoutSlash = raw.startsWith("/") ? raw.substring(1) : raw;
      Module.mc.player.networkHandler.sendChatCommand(withoutSlash);
   }

   static void notifyPlayer(String message) {
      notifyPlayer(message, Hud.NotificationType.INFO);
   }

   static void notifySuccess(String message) {
      notifyPlayer(message, Hud.NotificationType.SUCCESS);
   }

   static void notifyFailure(String message) {
      notifyPlayer(message, Hud.NotificationType.ERROR);
   }

   static void notifyPlayer(String message, Hud.NotificationType type) {
      if (Module.mc.player != null) {
         Module.mc.player.sendMessage(Text.literal(message), false);
      }
      Hud hud = Selene.get != null && Selene.get.manager != null ? Selene.get.manager.get(Hud.class) : null;
      if (hud != null) {
         hud.showNotification(message, type, 5000L);
      }
   }

   static void failAndDisable(Module module, String message) {
      if (Module.mc.player != null) {
         Module.mc.player.sendMessage(Text.literal(message), false);
         if (Module.mc.world != null) {
            Module.mc.world.playSound(
               Module.mc.player,
               Module.mc.player.getX(),
               Module.mc.player.getY(),
               Module.mc.player.getZ(),
               SoundEvents.ENTITY_PLAYER_LEVELUP,
               SoundCategory.PLAYERS,
               1.0F,
               1.0F
            );
         }
      }
      if (module.enable) {
         module.toggle();
      }
   }

   static boolean hasRedBossBar() {
      if (Module.mc.inGameHud == null) {
         return false;
      }
      try {
         Field field = Module.mc.inGameHud.getBossBarHud().getClass().getDeclaredField("bossBars");
         field.setAccessible(true);
         @SuppressWarnings("unchecked")
         Map<UUID, BossBar> bars = (Map<UUID, BossBar>) field.get(Module.mc.inGameHud.getBossBarHud());
         for (BossBar bar : bars.values()) {
            if (bar.getColor() == BossBar.Color.RED) {
               return true;
            }
         }
      } catch (ReflectiveOperationException ignored) {
      }
      return false;
   }

   static boolean hasInvisibilityPotion(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (!stack.isOf(Items.POTION) && !stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.LINGERING_POTION)) {
         return false;
      }
      PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
      if (contents == null) {
         return false;
      }
      for (StatusEffectInstance effect : contents.getEffects()) {
         if (effect.getEffectType().matches(StatusEffects.INVISIBILITY)) {
            return true;
         }
      }
      return false;
   }

   static int findInvisibilityHotbarSlot() {
      for (int i = 0; i < 9; i++) {
         if (hasInvisibilityPotion(Module.mc.player.getInventory().getStack(i))) {
            return i;
         }
      }
      return -1;
   }

   static boolean isFood(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      FoodComponent food = stack.get(DataComponentTypes.FOOD);
      return food != null;
   }

   static boolean isGoldenFood(ItemStack stack) {
      return stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
   }

   static int findFoodHotbarSlot() {
      int fallback = -1;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = Module.mc.player.getInventory().getStack(i);
         if (!isFood(stack)) {
            continue;
         }
         if (!isGoldenFood(stack)) {
            return i;
         }
         if (fallback == -1) {
            fallback = i;
         }
      }
      return fallback;
   }
}