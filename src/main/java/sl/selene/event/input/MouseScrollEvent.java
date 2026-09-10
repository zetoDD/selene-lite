package sl.selene.event.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public final class MouseScrollEvent extends Event {
   private final long window;
   private final double horizontal;
   private final double vertical;
   private final double cursorX;
   private final double cursorY;

   public MouseScrollEvent(long window, double horizontal, double vertical, double cursorX, double cursorY) {
      this.window = window;
      this.horizontal = horizontal;
      this.vertical = vertical;
      this.cursorX = cursorX;
      this.cursorY = cursorY;
   }

   public long window() {
      return this.window;
   }

   public double horizontal() {
      return this.horizontal;
   }

   public double vertical() {
      return this.vertical;
   }

   public double cursorX() {
      return this.cursorX;
   }

   public double cursorY() {
      return this.cursorY;
   }
}