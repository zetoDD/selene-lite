package sl.selene.event.impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public class KeyPressEvent extends Event {
   private final int key;
   private final int action;

   public KeyPressEvent(int key, int action) {
      this.key = key;
      this.action = action;
   }

   public int getKey() {
      return this.key;
   }

   public int getAction() {
      return this.action;
   }
}