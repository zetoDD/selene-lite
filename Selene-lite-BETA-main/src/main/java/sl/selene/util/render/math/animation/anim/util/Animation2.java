package sl.selene.util.render.math.animation.anim.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Animation2 {
   private long start;
   private double duration;
   private double fromValue;
   private double toValue;
   private double value;
   private double prevValue;
   private Easing easing = Easings.SINE_OUT;
   private boolean debug = false;
   private Runnable finishAction;

   public Animation2 run(double valueTo, double duration) {
      return this.run(valueTo, duration, Easings.SINE_OUT, false);
   }

   public Animation2 run(double valueTo, double duration, Easing easing) {
      return this.run(valueTo, duration, easing, false);
   }

   public Animation2 run(double valueTo, double duration, boolean safe) {
      return this.run(valueTo, duration, Easings.SINE_OUT, safe);
   }

   public Animation2 run(double valueTo, double duration, Easing easing, boolean safe) {
      if (this.check(safe, valueTo)) {
         if (this.isDebug()) {
            System.out.println("Animate cancelled due to target val equals from val");
         }
      } else {
         this.setEasing(easing).setDuration(duration * 1000.0).setStart(System.currentTimeMillis()).setFromValue(this.getValue()).setToValue(valueTo);
         if (this.isDebug()) {
            System.out
               .println(
                  "#animate {\n    to value: " + this.getToValue() + "\n    from value: " + this.getValue() + "\n    duration: " + this.getDuration() + "\n}"
               );
         }
      }

      return this;
   }

   public boolean update() {
      this.setPrevValue(this.getValue());
      boolean alive = this.isAlive();
      if (alive) {
         this.setValue(this.interpolate(this.getFromValue(), this.getToValue(), this.getEasing().ease(this.calculatePart())));
      } else {
         this.setStart(0L);
         this.setValue(this.getToValue());
         if (this.finishAction != null) {
            this.finishAction.run();
            this.finishAction = null;
         }
      }

      return alive;
   }

   public boolean isAlive() {
      return !this.isFinished();
   }

   public boolean isFinished() {
      return this.calculatePart() >= 1.0;
   }

   public double calculatePart() {
      return this.duration == 0.0 ? 1.0 : (System.currentTimeMillis() - this.getStart()) / this.getDuration();
   }

   public boolean check(boolean safe, double valueTo) {
      return safe && this.isAlive() && (valueTo == this.getFromValue() || valueTo == this.getToValue() || valueTo == this.getValue());
   }

   public double interpolate(double start, double end, double pct) {
      return start + (end - start) * pct;
   }

   public Animation2 setStart(long start) {
      this.start = start;
      return this;
   }

   public Animation2 setDuration(double duration) {
      this.duration = duration;
      return this;
   }

   public Animation2 setFromValue(double fromValue) {
      this.fromValue = fromValue;
      return this;
   }

   public Animation2 setToValue(double toValue) {
      this.toValue = toValue;
      return this;
   }

   public Animation2 setValue(double value) {
      this.value = value;
      return this;
   }

   public Animation2 setPrevValue(double prevValue) {
      this.prevValue = prevValue;
      return this;
   }

   public Animation2 setEasing(Easing easing) {
      this.easing = easing;
      return this;
   }

   public Animation2 setDebug(boolean debug) {
      this.debug = debug;
      return this;
   }

   public Animation2 onFinished(Runnable action) {
      this.finishAction = action;
      return this;
   }

   public float get() {
      return (float)this.getValue();
   }

   public float getPrev() {
      return (float)this.getPrevValue();
   }

   public void set(double value) {
      this.run(value, 1.0E-13);
      this.update();
      this.setValue(value);
   }

   public long getStart() {
      return this.start;
   }

   public double getDuration() {
      return this.duration;
   }

   public double getFromValue() {
      return this.fromValue;
   }

   public double getToValue() {
      return this.toValue;
   }

   public double getValue() {
      return this.value;
   }

   public double getPrevValue() {
      return this.prevValue;
   }

   public Easing getEasing() {
      return this.easing;
   }

   public boolean isDebug() {
      return this.debug;
   }
}