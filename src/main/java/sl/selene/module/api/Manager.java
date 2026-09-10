package sl.selene.module.api;

import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;
import sl.selene.module.impl.combat.AimAssist;
import sl.selene.module.impl.combat.ShieldBreaker;
import sl.selene.module.impl.combat.TriggerBot;
import sl.selene.module.impl.movement.STap;
import sl.selene.module.impl.movement.SnapTap;
import sl.selene.module.impl.movement.ToggleSprint;
import sl.selene.module.impl.movement.WTap;
import sl.selene.module.impl.visuals.AspectRation;
import sl.selene.module.impl.visuals.AmethystESP;
import sl.selene.module.impl.visuals.ChunkFinder;
import sl.selene.module.impl.visuals.HoleESP;
import sl.selene.module.impl.visuals.StorageESP;
import sl.selene.module.impl.visuals.ESP;
import sl.selene.module.impl.visuals.Hud;
import sl.selene.module.impl.visuals.Keystrokes;
import sl.selene.module.impl.visuals.ItemESP;
import sl.selene.module.impl.visuals.JumpCircle;
import sl.selene.module.impl.visuals.Animations;
import sl.selene.module.impl.visuals.Crosshair;
import sl.selene.module.impl.visuals.CustomHitbox;
import sl.selene.module.impl.visuals.ShulkerPreview;
import sl.selene.module.impl.visuals.FullBright;
import sl.selene.module.impl.visuals.Projectile;
import sl.selene.module.impl.utils.Optimizer;
import sl.selene.module.impl.utils.Zoom;
import sl.selene.module.impl.donut.Freecam;
import sl.selene.module.impl.donut.NameHider;

@Environment(EnvType.CLIENT)
public class Manager {
   public ArrayList<Module> module = new ArrayList<>();

   public Manager() {
      this.module.add(new AimAssist());
      this.module.add(new TriggerBot());
      this.module.add(new ShieldBreaker());
      this.module.add(new ToggleSprint());
      this.module.add(new WTap());
      this.module.add(new STap());
      this.module.add(new SnapTap());
      this.module.add(new ESP());
      this.module.add(new JumpCircle());
      this.module.add(new Hud());
      this.module.add(new Keystrokes());
      this.module.add(new ItemESP());
      this.module.add(new Projectile());
      this.module.add(new FullBright());
      this.module.add(new AspectRation());
      this.module.add(new Animations());
      this.module.add(new Crosshair());
      this.module.add(new CustomHitbox());
      this.module.add(new ShulkerPreview());
      this.module.add(new Optimizer());
      this.module.add(new Zoom());
      this.module.add(new Freecam());
      this.module.add(new HoleESP());
      this.module.add(new StorageESP());
      this.module.add(new AmethystESP());
      this.module.add(new ChunkFinder());
      this.module.add(new NameHider());
   }

   public ArrayList<Module> getModules() {
      return this.module;
   }

   public <T extends Module> T get(Class<T> clazz) {
      return this.module.stream().filter(module -> clazz.isAssignableFrom(module.getClass())).map(clazz::cast).findFirst().orElse(null);
   }

   public Module getModule(Class<?> class1) {
      for (Module module1 : this.module) {
         if (module1.getClass() == class1) {
            return module1;
         }
      }

      return null;
   }

   public ArrayList<Module> getType(Category category) {
      ArrayList<Module> modules = new ArrayList<>();

      for (Module module1 : this.module) {
         if (module1.category == category) {
            modules.add(module1);
         }
      }

      return modules;
   }

   public Module[] getBind(int bind) {
      return Selene.get.manager.module.stream().filter(module -> module.bind == bind).toArray(Module[]::new);
   }
}