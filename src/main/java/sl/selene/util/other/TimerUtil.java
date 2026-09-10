package sl.selene.util.other;

import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class TimerUtil {
   private long startTime;
   public long lastMS = System.currentTimeMillis();

   public void reset() {
      this.lastMS = System.currentTimeMillis();
   }

   public boolean isReached(long time) {
      return System.currentTimeMillis() - this.lastMS > time;
   }

   public void setLastMS(long newValue) {
      this.lastMS = System.currentTimeMillis() + newValue;
   }

   public void setTime(long time) {
      this.lastMS = time;
   }

   public boolean finished(double delay) {
      return System.currentTimeMillis() - delay >= this.startTime;
   }

   public boolean every(double delay) {
      boolean finished = this.finished(delay);
      if (finished) {
         this.reset();
      }

      return finished;
   }

   public long elapsedTime() {
      return System.currentTimeMillis() - this.startTime;
   }

   public void setMs(long ms) {
      this.startTime = System.currentTimeMillis() - ms;
   }

   public long getTime() {
      return System.currentTimeMillis() - this.lastMS;
   }

   public boolean isRunning() {
      return System.currentTimeMillis() - this.lastMS <= 0L;
   }

   public boolean hasTimeElapsed(long l) {
      return System.currentTimeMillis() - this.lastMS > l;
   }

   public boolean hasTimeElapsed() {
      return this.lastMS < System.currentTimeMillis();
   }

   public boolean hasTimeElapsed(long time, boolean reset) {
      if (System.currentTimeMillis() - this.lastMS > time) {
         if (reset) {
            this.reset();
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean hasReached(double milliseconds) {
      return this.getTimePassed() >= milliseconds;
   }

   public long getTimePassed() {
      return System.currentTimeMillis() - this.lastMS;
   }

   public long setTimePassed(int delay) {
      return System.currentTimeMillis() + delay;
   }

   public long getLastMS() {
      return this.lastMS;
   }

   public void setLastMC() {
      this.lastMS = System.currentTimeMillis();
   }

   public boolean finished(long delay) {
      return System.currentTimeMillis() - this.lastMS >= delay;
   }

   public boolean every(long delay) {
      if (System.currentTimeMillis() - this.lastMS >= delay) {
         this.reset();
         return true;
      } else {
         return false;
      }
   }

   public boolean passed(long ms) {
      return System.currentTimeMillis() - this.lastMS >= ms;
   }

   @Generated
   public long getStartTime() {
      return this.startTime;
   }

   @Environment(EnvType.CLIENT)
   public static class legacyTime {
      private long lastMS;

      private legacyTime() {
         this.reset();
      }

      public static TimerUtil.legacyTime create() {
         return new TimerUtil.legacyTime();
      }

      public void reset() {
         this.lastMS = System.currentTimeMillis();
      }

      public long elapsedTime() {
         return System.currentTimeMillis() - this.lastMS;
      }

      public boolean hasReached(long time) {
         return this.elapsedTime() >= time;
      }

      public boolean hasReached(long time, boolean reset) {
         boolean hasElapsed = this.elapsedTime() >= time;
         if (hasElapsed && reset) {
            this.reset();
         }

         return hasElapsed;
      }

      public boolean hasReached(double ms) {
         return this.elapsedTime() >= ms;
      }

      public boolean delay(long ms) {
         boolean hasDelayElapsed = this.elapsedTime() - ms >= 0L;
         if (hasDelayElapsed) {
            this.reset();
         }

         return hasDelayElapsed;
      }

      @Generated
      public void setLastMS(long lastMS) {
         this.lastMS = lastMS;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class satosTime {
      public long startTime;
      public long mc = System.currentTimeMillis();

      public void reset() {
         this.mc = System.currentTimeMillis();
      }

      public long getMc() {
         return System.currentTimeMillis() - this.mc;
      }

      public boolean hasReached(long n) {
         return System.currentTimeMillis() - this.mc > n;
      }

      public boolean hasReachedSecond(float s) {
         long n = (long)(s * 1000.0F);
         return System.currentTimeMillis() - this.mc > n;
      }

      public boolean finished(double delay) {
         return System.currentTimeMillis() - delay >= this.startTime;
      }

      public boolean hasReached(long time, boolean reset) {
         boolean hasElapsed = this.getMc() >= time;
         if (hasElapsed && reset) {
            this.reset();
         }

         return hasElapsed;
      }

      private long getCurrentMS() {
         return System.currentTimeMillis();
      }

      public long getTime() {
         return this.getCurrentMS() - this.mc;
      }

      @Generated
      public void setStartTime(long startTime) {
         this.startTime = startTime;
      }

      @Generated
      public void setMc(long mc) {
         this.mc = mc;
      }
   }
}