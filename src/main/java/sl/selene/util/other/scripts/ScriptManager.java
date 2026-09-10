package sl.selene.util.other.scripts;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ScriptManager {
   private final Map<String, Script> scripts = new ConcurrentHashMap<>();

   public Optional<Script> getScript(String name) {
      return this.isNullOrEmpty(name) ? Optional.empty() : Optional.of(this.scripts.computeIfAbsent(name, x -> new Script()));
   }

   public Script addScript(String name, Script script) {
      if (!this.isNullOrEmpty(name) && script != null) {
         return this.scripts.put(name, script);
      } else {
         throw new IllegalArgumentException("Script name or instance cannot be null or empty");
      }
   }

   public boolean containsScript(String name) {
      return !this.isNullOrEmpty(name) && this.scripts.containsKey(name);
   }

   public boolean finished(String name) {
      return !this.isNullOrEmpty(name) && this.getScript(name).isPresent() && this.getScript(name).get().isFinished();
   }

   public void removeScript(String name) {
      if (!this.isNullOrEmpty(name)) {
         this.scripts.remove(name);
      }
   }

   public void cleanupScript(String name) {
      if (!this.isNullOrEmpty(name)) {
         this.scripts.computeIfPresent(name, (k, v) -> {
            v.cleanup();
            return (Script)v;
         });
      }
   }

   public void cleanupAll() {
      this.scripts.forEach((k, v) -> v.cleanup());
   }

   public void clearAll() {
      this.scripts.clear();
   }

   public void updateScript(String name) {
      this.updateScript(name, () -> true);
   }

   public void updateScript(String name, Supplier<Boolean> condition) {
      if (condition.get() && !this.isNullOrEmpty(name)) {
         this.scripts.computeIfPresent(name, (k, v) -> {
            v.update();
            return (Script)v;
         });
      }
   }

   public void updateAll() {
      this.scripts.values().forEach(Script::update);
   }

   public Set<String> getAllScriptNames() {
      return Collections.unmodifiableSet(this.scripts.keySet());
   }

   public Map<String, Script> getAllScripts() {
      return Collections.unmodifiableMap(this.scripts);
   }

   private boolean isNullOrEmpty(String str) {
      return str == null || str.trim().isEmpty();
   }
}