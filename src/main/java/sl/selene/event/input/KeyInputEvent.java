package sl.selene.event.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public final class KeyInputEvent extends Event {
   private final long window;
   private final int key;
   private final int scancode;
   private final int action;
   private final int modifiers;

   public KeyInputEvent(long window, int key, int scancode, int action, int modifiers) {
      this.window = window;
      this.key = key;
      this.scancode = scancode;
      this.action = action;
      this.modifiers = modifiers;
   }

   public long window() {
      return this.window;
   }

   public int key() {
      return this.key;
   }

   public int scancode() {
      return this.scancode;
   }

   public int action() {
      return this.action;
   }

   public int modifiers() {
      return this.modifiers;
   }
}