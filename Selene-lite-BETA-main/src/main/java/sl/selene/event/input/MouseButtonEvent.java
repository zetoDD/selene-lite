package sl.selene.event.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public final class MouseButtonEvent extends Event {
   private final long window;
   private final int button;
   private final int action;
   private final int modifiers;
   private final double cursorX;
   private final double cursorY;

   public MouseButtonEvent(long window, int button, int action, int modifiers, double cursorX, double cursorY) {
      this.window = window;
      this.button = button;
      this.action = action;
      this.modifiers = modifiers;
      this.cursorX = cursorX;
      this.cursorY = cursorY;
   }

   public long window() {
      return this.window;
   }

   public int button() {
      return this.button;
   }

   public int action() {
      return this.action;
   }

   public int modifiers() {
      return this.modifiers;
   }

   public double cursorX() {
      return this.cursorX;
   }

   public double cursorY() {
      return this.cursorY;
   }

   public boolean isPress() {
      return this.action == 1;
   }

   public boolean isRelease() {
      return this.action == 0;
   }
}