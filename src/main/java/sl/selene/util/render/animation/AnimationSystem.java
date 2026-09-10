package sl.selene.util.render.animation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AnimationSystem {
   private static final AnimationSystem INSTANCE = new AnimationSystem();
   private final Object lock = new Object();
   private final List<AnimationSystem.Animated> animatedObjects = new ArrayList<>();
   private long lastTickNanos = System.nanoTime();
   private float lastDeltaSeconds = 0.0F;

   private AnimationSystem() {
   }

   public static AnimationSystem getInstance() {
      return INSTANCE;
   }

   public void tick() {
      long now = System.nanoTime();
      long elapsed = now - this.lastTickNanos;
      this.lastTickNanos = now;
      if (elapsed < 0L) {
         elapsed = 0L;
      }

      float delta = (float)elapsed / 1.0E9F;
      if (delta < 1.0E-4F) {
         delta = 1.0E-4F;
      } else if (delta > 0.06666667F) {
         delta = 0.06666667F;
      }

      this.lastDeltaSeconds = delta;
      synchronized (this.lock) {
         if (!this.animatedObjects.isEmpty()) {
            Iterator<AnimationSystem.Animated> iterator = this.animatedObjects.iterator();

            while (iterator.hasNext()) {
               AnimationSystem.Animated animated = iterator.next();
               boolean keepUpdating = animated.update(delta);
               if (!keepUpdating) {
                  iterator.remove();
               }
            }
         }
      }
   }

   public float getLastDeltaSeconds() {
      return this.lastDeltaSeconds;
   }

   public void ensureRegistered(AnimationSystem.Animated animated) {
      if (animated != null) {
         synchronized (this.lock) {
            if (!this.animatedObjects.contains(animated)) {
               this.animatedObjects.add(animated);
            }
         }
      }
   }

   public void unregister(AnimationSystem.Animated animated) {
      if (animated != null) {
         synchronized (this.lock) {
            this.animatedObjects.remove(animated);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Animated {
      boolean update(float var1);
   }
}