package sl.selene.util.player;
import net.minecraft.client.MinecraftClient;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import sl.selene.util.other.IMinecraft;

@Environment(EnvType.CLIENT)
public class MovementManager implements IMinecraft {
   private static final MovementManager INSTANCE = new MovementManager();
   public final Set<String> lockRequests = new HashSet<>();

   private MovementManager() {
   }

   public static MovementManager getInstance() {
      return INSTANCE;
   }

   public void lockMovement(String moduleName) {
      if (mc.player != null && mc.player.isAlive() && mc.world != null) {
         this.lockRequests.add(moduleName);
         this.setMovementKeys(false);
      }
   }

   public void unlockMovement(String moduleName) {
      if (mc.player != null && mc.player.isAlive() && mc.world != null) {
         this.lockRequests.remove(moduleName);
         if (this.lockRequests.isEmpty() && mc.currentScreen == null) {
            this.setMovementKeys(true);
         }
      }
   }

   public boolean isMovementLocked() {
      return !this.lockRequests.isEmpty();
   }

   private void setMovementKeys(boolean state) {
      KeyBinding[] movementKeys = new KeyBinding[]{
         mc.options.forwardKey,
         mc.options.backKey,
         mc.options.leftKey,
         mc.options.rightKey,
         mc.options.jumpKey,
         mc.options.sprintKey
      };
      long handle = mc.getWindow().getHandle();

      for (KeyBinding keyBinding : movementKeys) {
         boolean pressed = state && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), keyBinding.getDefaultKey().getCode());
         keyBinding.setPressed(pressed);
      }
   }
}