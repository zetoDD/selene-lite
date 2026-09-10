package sl.selene.mixin.plugin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class SeleneMixinPlugin implements IMixinConfigPlugin {

   private static final String CAMERA_CLASS = "net.minecraft.client.render.Camera";

   private static final Set<String> LUNAR_SKIP = Set.of(
         "MouseMixin",
         "MouseClickMixin",
         "MouseScrollMixin",
         "GameRendererMixin",
         "GameRendererCrosshairMixin",
         "LightmapTextureManagerMixin",
         "FogRendererMixin",
         "WorldRendererMixin",
         "WorldRendererEntityCaptureMixin",
         "RenderLayerMultiPhaseMixin",
         "CameraMixin",
         "ClientPlayerEntityMixin",
         "KeyboardInputMixin",
         "SnapTapMixin",
         "InGameHudMixin",
         "InGameOverlayRendererMixin",
         "ItemPhysicsMixin"
   );

   private static Boolean lunarClient;

   @Override
   public void onLoad(String mixinPackage) {
   }

   @Override
   public String getRefMapperConfig() {
      return null;
   }

   @Override
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (CAMERA_CLASS.equals(targetClassName) && isLunarClient()) {
         return false;
      }

      if (!isLunarClient()) {
         return true;
      }

      String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
      return !LUNAR_SKIP.contains(simpleName);
   }

   private static boolean isLunarClient() {
      if (lunarClient != null) {
         return lunarClient;
      }

      lunarClient = detectLunar();
      if (lunarClient) {
         System.out.println("[Selene] Lunar Client detected — skipping incompatible mixins");
      }
      return lunarClient;
   }

   private static boolean detectLunar() {
      if (FabricLoader.getInstance().isModLoaded("ichor")) {
         return true;
      }
      return classExists("com.moonsworth.lunar.genesis.Genesis")
            || classExists("com.moonsworth.lunar.genesis.ClientGameBootstrap");
   }

   @Override
   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   @Override
   public List<String> getMixins() {
      return null;
   }

   @Override
   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   @Override
   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   private static boolean classExists(String name) {
      try {
         Class.forName(name, false, SeleneMixinPlugin.class.getClassLoader());
         return true;
      } catch (ClassNotFoundException ignored) {
         return false;
      }
   }
}