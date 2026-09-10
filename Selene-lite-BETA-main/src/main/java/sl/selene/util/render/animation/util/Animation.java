package sl.selene.util.render.animation.util;

import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Animation {
   private long start;
   private double duration;
   private double fromValue;
   private double toValue;
   private double value;
   private double prevValue;
   private Easing easing = Easings.LINEAR;
   private boolean debug = false;
   private Runnable finishAction;

   public Animation run(double valueTo, double duration) {
      return this.run(valueTo, duration, Easings.LINEAR, false);
   }

   public Animation run(double valueTo, double duration, Easing easing) {
      return this.run(valueTo, duration, easing, false);
   }

   public Animation run(double valueTo, double duration, boolean safe) {
      return this.run(valueTo, duration, Easings.LINEAR, safe);
   }

   public Animation run(double valueTo, double duration, Easing easing, boolean safe) {
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
      return (System.currentTimeMillis() - this.getStart()) / this.getDuration();
   }

   public boolean check(boolean safe, double valueTo) {
      return safe && this.isAlive() && (valueTo == this.getFromValue() || valueTo == this.getToValue() || valueTo == this.getValue());
   }

   public double interpolate(double start, double end, double pct) {
      return start + (end - start) * pct;
   }

   public Animation setStart(long start) {
      this.start = start;
      return this;
   }

   public Animation setDuration(double duration) {
      this.duration = duration;
      return this;
   }

   public Animation setFromValue(double fromValue) {
      this.fromValue = fromValue;
      return this;
   }

   public Animation setToValue(double toValue) {
      this.toValue = toValue;
      return this;
   }

   public Animation setValue(double value) {
      this.value = value;
      return this;
   }

   public Animation setPrevValue(double prevValue) {
      this.prevValue = prevValue;
      return this;
   }

   public Animation setEasing(Easing easing) {
      this.easing = easing;
      return this;
   }

   public Animation setDebug(boolean debug) {
      this.debug = debug;
      return this;
   }

   public Animation onFinished(Runnable action) {
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
      this.run(value, 0.0);
      this.update();
      this.setValue(value);
   }

   @Generated
   public long getStart() {
      return this.start;
   }

   @Generated
   public double getDuration() {
      return this.duration;
   }

   @Generated
   public double getFromValue() {
      return this.fromValue;
   }

   @Generated
   public double getToValue() {
      return this.toValue;
   }

   @Generated
   public double getValue() {
      return this.value;
   }

   @Generated
   public double getPrevValue() {
      return this.prevValue;
   }

   @Generated
   public Easing getEasing() {
      return this.easing;
   }

   @Generated
   public boolean isDebug() {
      return this.debug;
   }

   @Generated
   public Runnable getFinishAction() {
      return this.finishAction;
   }
}