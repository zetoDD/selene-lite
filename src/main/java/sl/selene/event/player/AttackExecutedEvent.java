package sl.selene.event.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import sl.selene.event.Event;

@Environment(EnvType.CLIENT)
public final class AttackExecutedEvent extends Event {
   private final Entity target;

   public AttackExecutedEvent(Entity target) {
      this.target = target;
   }

   public Entity getTarget() {
      return target;
   }
}
