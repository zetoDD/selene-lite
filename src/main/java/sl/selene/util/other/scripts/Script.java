package sl.selene.util.other.scripts;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.util.other.StopWatch;

@Environment(EnvType.CLIENT)
public class Script {
   private final StopWatch time = new StopWatch();
   private final List<Script.ScriptStep> scriptSteps = Lists.newCopyOnWriteArrayList();
   private final List<Script.ScriptTickStep> scriptTickSteps = Lists.newCopyOnWriteArrayList();
   private int currentStepIndex;
   private int currentTickStepIndex;
   private boolean interrupt;
   private Script.LoopStrategy loopStrategy = new Script.FiniteLoopStrategy(1);

   public Script() {
      this.cleanup();
   }

   public Script addStep(int delay, ScriptAction action) {
      return this.addStep(delay, action, () -> true, 0);
   }

   public Script addStep(int delay, ScriptAction action, BooleanSupplier condition) {
      return this.addStep(delay, action, condition, 0);
   }

   public Script addStep(int delay, ScriptAction action, int priority) {
      return this.addStep(delay, action, () -> true, priority);
   }

   public Script addStep(int delay, ScriptAction action, BooleanSupplier condition, int priority) {
      this.scriptSteps.add(new Script.ScriptStep(delay, action, condition, priority));
      Collections.sort(this.scriptSteps);
      return this;
   }

   public Script addTickStep(int ticks, ScriptAction action) {
      return this.addTickStep(ticks, action, () -> true, 0);
   }

   public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition) {
      return this.addTickStep(ticks, action, condition, 0);
   }

   public Script addTickStep(int ticks, ScriptAction action, int priority) {
      return this.addTickStep(ticks, action, () -> true, priority);
   }

   public Script addTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
      this.scriptTickSteps.add(new Script.ScriptTickStep(ticks, action, condition, priority));
      Collections.sort(this.scriptTickSteps);
      return this;
   }

   public void resetTime() {
      this.time.reset();
   }

   public void resetStepIndex() {
      this.currentStepIndex = 0;
      this.currentTickStepIndex = 0;
   }

   public Script cleanupIfFinished() {
      if (this.isFinished()) {
         this.cleanup();
      }

      return this;
   }

   public Script cleanup() {
      this.scriptSteps.clear();
      this.scriptTickSteps.clear();
      this.resetTime();
      this.resetStepIndex();
      return this;
   }

   public void update() {
      if ((!this.scriptSteps.isEmpty() || !this.scriptTickSteps.isEmpty()) && !this.interrupt) {
         this.scriptSteps.forEach(step -> {
            if (this.currentStepIndex < this.scriptSteps.size()) {
               Script.ScriptStep currentStep = this.scriptSteps.get(this.currentStepIndex);
               if (currentStep.condition().getAsBoolean() && this.time.finished(currentStep.delay())) {
                  currentStep.action().perform();
                  this.currentStepIndex++;
                  this.resetTime();
                  if (this.loopStrategy.shouldLoop(this.currentStepIndex, this.scriptSteps.size())) {
                     this.resetStepIndex();
                     this.loopStrategy.onLoop();
                  }
               }
            }
         });
         this.scriptTickSteps.forEach(step -> {
            if (this.currentTickStepIndex < this.scriptTickSteps.size()) {
               Script.ScriptTickStep currentTickStep = this.scriptTickSteps.get(this.currentTickStepIndex);
               if (currentTickStep.condition().getAsBoolean() && currentTickStep.ticks() <= 0) {
                  currentTickStep.action().perform();
                  this.currentTickStepIndex++;
                  this.resetTime();
                  if (this.loopStrategy.shouldLoop(this.currentTickStepIndex, this.scriptTickSteps.size())) {
                     this.resetStepIndex();
                     this.loopStrategy.onLoop();
                  }
               }

               currentTickStep.decrementTicks();
            }
         });
         this.currentStepIndex = Math.min(this.currentStepIndex, this.scriptSteps.size());
         this.currentTickStepIndex = Math.min(this.currentTickStepIndex, this.scriptTickSteps.size());
      }
   }

   public Script setLoopStrategy(Script.LoopStrategy loopStrategy) {
      this.loopStrategy = loopStrategy;
      return this;
   }

   public boolean isFinished() {
      return this.currentStepIndex >= this.scriptSteps.size()
         && this.currentTickStepIndex >= this.scriptTickSteps.size()
         && !this.interrupt
         && this.loopStrategy.isFinished();
   }

   @Generated
   public StopWatch getTime() {
      return this.time;
   }

   @Generated
   public List<Script.ScriptStep> getScriptSteps() {
      return this.scriptSteps;
   }

   @Generated
   public List<Script.ScriptTickStep> getScriptTickSteps() {
      return this.scriptTickSteps;
   }

   @Generated
   public int getCurrentStepIndex() {
      return this.currentStepIndex;
   }

   @Generated
   public int getCurrentTickStepIndex() {
      return this.currentTickStepIndex;
   }

   @Generated
   public boolean isInterrupt() {
      return this.interrupt;
   }

   @Generated
   public Script.LoopStrategy getLoopStrategy() {
      return this.loopStrategy;
   }

   @Generated
   public void setCurrentStepIndex(int currentStepIndex) {
      this.currentStepIndex = currentStepIndex;
   }

   @Generated
   public void setCurrentTickStepIndex(int currentTickStepIndex) {
      this.currentTickStepIndex = currentTickStepIndex;
   }

   @Generated
   public void setInterrupt(boolean interrupt) {
      this.interrupt = interrupt;
   }

   @Environment(EnvType.CLIENT)
   public static class FiniteLoopStrategy implements Script.LoopStrategy {
      private final int loopCount;
      private int currentLoop;

      public FiniteLoopStrategy(int loopCount) {
         this.loopCount = loopCount - 1;
      }

      @Override
      public boolean shouldLoop(int currentStepIndex, int totalSteps) {
         return currentStepIndex >= totalSteps && this.currentLoop < this.loopCount;
      }

      @Override
      public void onLoop() {
         this.currentLoop++;
      }

      @Override
      public boolean isFinished() {
         return this.currentLoop >= this.loopCount;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class InfiniteLoopStrategy implements Script.LoopStrategy {
      @Override
      public boolean shouldLoop(int currentStepIndex, int totalSteps) {
         return currentStepIndex >= totalSteps;
      }

      @Override
      public void onLoop() {
      }

      @Override
      public boolean isFinished() {
         return false;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface LoopStrategy {
      boolean shouldLoop(int var1, int var2);

      void onLoop();

      boolean isFinished();
   }

   @Environment(EnvType.CLIENT)
   public static final class ScriptStep implements Comparable<Script.ScriptStep> {
      private int delay;
      private ScriptAction action;
      private BooleanSupplier condition;
      private int priority;

      public ScriptStep(int delay, ScriptAction action, BooleanSupplier condition, int priority) {
         this.delay = delay;
         this.action = action;
         this.condition = condition;
         this.priority = priority;
      }

      public int compareTo(Script.ScriptStep otherStep) {
         return Integer.compare(otherStep.priority(), this.priority());
      }

      @Generated
      public int delay() {
         return this.delay;
      }

      @Generated
      public ScriptAction action() {
         return this.action;
      }

      @Generated
      public BooleanSupplier condition() {
         return this.condition;
      }

      @Generated
      public int priority() {
         return this.priority;
      }

      @Generated
      public Script.ScriptStep delay(int delay) {
         this.delay = delay;
         return this;
      }

      @Generated
      public Script.ScriptStep action(ScriptAction action) {
         this.action = action;
         return this;
      }

      @Generated
      public Script.ScriptStep condition(BooleanSupplier condition) {
         this.condition = condition;
         return this;
      }

      @Generated
      public Script.ScriptStep priority(int priority) {
         this.priority = priority;
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class ScriptTickStep implements Comparable<Script.ScriptTickStep> {
      private int ticks;
      private ScriptAction action;
      private BooleanSupplier condition;
      private int priority;

      public ScriptTickStep(int ticks, ScriptAction action, BooleanSupplier condition, int priority) {
         this.ticks = ticks;
         this.action = action;
         this.condition = condition;
         this.priority = priority;
      }

      public int compareTo(Script.ScriptTickStep otherStep) {
         return Integer.compare(otherStep.priority(), this.priority());
      }

      public void decrementTicks() {
         this.ticks--;
      }

      @Generated
      public int ticks() {
         return this.ticks;
      }

      @Generated
      public ScriptAction action() {
         return this.action;
      }

      @Generated
      public BooleanSupplier condition() {
         return this.condition;
      }

      @Generated
      public int priority() {
         return this.priority;
      }

      @Generated
      public Script.ScriptTickStep ticks(int ticks) {
         this.ticks = ticks;
         return this;
      }

      @Generated
      public Script.ScriptTickStep action(ScriptAction action) {
         this.action = action;
         return this;
      }

      @Generated
      public Script.ScriptTickStep condition(BooleanSupplier condition) {
         this.condition = condition;
         return this;
      }

      @Generated
      public Script.ScriptTickStep priority(int priority) {
         this.priority = priority;
         return this;
      }
   }
}