package sl.selene;

import java.io.File;
import lombok.Generated;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import sl.selene.cfg.ConfigManager;
import sl.selene.client.MenuKeyHandler;
import sl.selene.client.SeleneKeyBindings;
import sl.selene.commands.CommandBootstrap;
import sl.selene.config.GuiManager;
import sl.selene.config.friend.FriendManager;
import sl.selene.config.target.TargetManager;
import sl.selene.event.EventManager;
import sl.selene.event.RenderHandler;
import sl.selene.event.render.RenderEvent;
import sl.selene.module.api.Manager;
import sl.selene.module.bind.BindingManager;
import sl.selene.module.impl.visuals.Hud;
import sl.selene.module.impl.visuals.HUD.InformationHUD;
import sl.selene.ui.draggable.DraggableManager;
import sl.selene.ui.gui.GuiClient;
import sl.selene.ui.gui.GuiScreen;
import sl.selene.ui.gui.component.render.GlassStyle;
import sl.selene.util.render.animation.AnimationSystem;
import sl.selene.util.color.ColorUtil;
import sl.selene.util.render.backends.RenderBackend;
import sl.selene.util.render.backends.gl.GlBackend;
import sl.selene.util.render.backends.gl.GlState;
import sl.selene.util.render.capture.EntityFramebufferCaptureManager;
import sl.selene.util.render.core.Renderer2D;
import sl.selene.util.render.texture.TextureLoader;
import sl.selene.util.render.text.FontObject;
import sl.selene.util.render.utils.SoundUtil;
import sl.selene.util.render.text.FontRegistry;
import sl.selene.util.render.ui.SeleneIcons;

@Environment(EnvType.CLIENT)
public class Selene implements ClientModInitializer {

   public static Selene get;
   public Manager manager;
   public final String name = "Selene";
   public final String version = "1.0";
   public final String title = "1.21.11";
   public final File preRoot = new File(System.getProperty("user.home"), ".selene");
   public final File root = new File(this.preRoot, "Selene");
   public final String rootRes = "selene";
   public GuiManager guiManager;
   public ConfigManager configManager;
   public FriendManager friendManager;
   public TargetManager targetManager;
   public GuiClient guiClient;
   private static RenderBackend backend;
   private static Renderer2D renderer;
   private static FontObject uiFont;
   private static volatile boolean initialized = false;
   private static volatile boolean modInitialized = false;

   public static Renderer2D getRenderer() {
      ensureRendererInitialized();
      return renderer;
   }

   public static boolean isModInitialized() {
      return modInitialized;
   }

   public void onInitializeClient() {
      System.out.println("[Selene] onInitializeClient() START");
      if (FabricLoader.getInstance().isModLoaded("night")) {
         throw new IllegalStateException(
               "Mod \"night\" is loaded together with \"selene\". Remove night*.jar from mods/ — only one client build is allowed.");
      }

      get = this;
      migrateLegacyRoot();
      if (!this.root.exists()) {
         this.root.mkdirs();
      }
      this.manager = new Manager();
      this.friendManager = new FriendManager();
      FriendManager.init();
      this.targetManager = new TargetManager();
      TargetManager.init();
      this.configManager = new ConfigManager();
      this.guiManager = new GuiManager();
      this.guiManager.init();
      GuiScreen.selectedCategories = this.guiManager.getCurrentCategory();
      CommandBootstrap.initialize();
      SeleneKeyBindings.register();
      MenuKeyHandler.register();
      BindingManager.getInstance().initialize();
      if (this.configManager != null) {
         this.configManager.load();
         boolean loadedAutoSave = false;
         for (String autoName : ConfigManager.AUTO_SAVE_ALIASES) {
            if (this.configManager.findConfig(autoName) != null && this.configManager.loadConfig(autoName)) {
               loadedAutoSave = true;
               break;
            }
         }
         if (!loadedAutoSave && this.configManager.findConfig("default") != null) {
            this.configManager.loadConfig("default");
         }
      }

      SeleneKeyBindings.syncFromMenuModule();

      Hud hudModule = this.manager != null ? this.manager.get(Hud.class) : null;
      if (hudModule != null && !hudModule.enable) {
         hudModule.setState(true);
         if (this.configManager != null) {
            this.configManager.autoSave();
         }
      }

      this.guiClient = new GuiClient();
      sl.selene.ui.gui.GuiSoundPlayer.preload();
      RenderHandler.register();
      EventManager.register(new InformationHUD());
      EventManager.register(this);
      ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdownClientResources());
      modInitialized = true;
      System.out.println("[Selene] onInitializeClient() COMPLETE - modInitialized=" + modInitialized);
   }

   private static void shutdownClientResources() {
      ConfigManager.shutdown();
      ColorUtil.shutdownCacheCleaner();
      SoundUtil.releaseAll();
      sl.selene.ui.gui.GuiSoundPlayer.releaseAll();
      EntityFramebufferCaptureManager.getInstance().setEnabled(false);
      TextureLoader.clearCache();
      SeleneIcons.clearCache();
      if (backend != null) {
         backend.destroy();
         backend = null;
      }
      initialized = false;
      renderer = null;
   }

   private void migrateLegacyRoot() {
      try {

         File oldZeroPreRoot = new File(System.getProperty("user.home"), ".zerodlc");
         File oldZeroRoot = new File(oldZeroPreRoot, "ZeroDLC");
         if (oldZeroRoot.exists() && !this.root.exists()) {
            if (!this.preRoot.exists()) {
               this.preRoot.mkdirs();
            }
            oldZeroRoot.renameTo(this.root);
         }

         if (!this.root.exists()) {
            File oldPreRoot = new File(System.getProperty("user.home"), ".nightdlc");
            File oldRoot = new File(oldPreRoot, "NightDLC");
            if (oldRoot.exists()) {
               if (!this.preRoot.exists()) {
                  this.preRoot.mkdirs();
               }
               oldRoot.renameTo(this.root);
            }
         }
      } catch (Exception ignored) {
      }
   }

   public static void ensureRendererInitialized() {
      if (!initialized) {
         onInit();
      }
   }

   public static synchronized void reloadVisualRenderer() {
      EntityFramebufferCaptureManager.getInstance().setEnabled(false);
      TextureLoader.clearCache();
      SeleneIcons.clearCache();
      FontRegistry.shutdown();
      if (backend != null) {
         backend.destroy();
         backend = null;
      }
      renderer = null;
      uiFont = null;
      initialized = false;
      onInit();
   }

   private static synchronized void onInit() {
      if (!initialized) {
         backend = new GlBackend();
         TextureLoader.initialize(backend);
         renderer = new Renderer2D(backend);
         FontRegistry.initialize(backend, renderer);
         uiFont = FontRegistry.INTER_MEDIUM;
         initialized = true;
         System.out.println("[Selene] Visual renderer initialized (OpenGL)");
      }
   }

   public static void onRender() {
      if (modInitialized) {
         GlState.Snapshot snapshot = GlState.push();

         try {
            if (!initialized) {
               onInit();
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getWindow() == null) {
               return;
            }

            int width = client.getWindow().getFramebufferWidth();
            int height = client.getWindow().getFramebufferHeight();
            if (width <= 0 || height <= 0) {
               return;
            }

            AnimationSystem.getInstance().tick();
            DraggableManager draggableManager = DraggableManager.getInstance();
            draggableManager.beginFrame(client, renderer, width, height);
            boolean rendererBegun = false;

            try {
               renderer.begin(width, height);
               rendererBegun = true;

               try {

                  renderer.prepareBlur(GlassStyle.BACKDROP_RADIUS);
                  EventManager.call(new RenderEvent(client, renderer, uiFont, width, height));
               } finally {
                  if (rendererBegun) {
                     renderer.end();
                  }
               }
            } finally {
               draggableManager.endFrame();
            }
         } finally {
            GlState.pop(snapshot);
         }
      }
   }

   @Generated
   public Manager getManager() {
      return this.manager;
   }

   @Generated
   public String getName() {
      return "Selene";
   }

   @Generated
   public String getVersion() {
      return "1.0";
   }

   @Generated
   public String getTitle() {
      return "1.21.11";
   }

   @Generated
   public File getPreRoot() {
      return this.preRoot;
   }

   @Generated
   public File getRoot() {
      return this.root;
   }

   @Generated
   public String getRootRes() {
      return "selene";
   }

   @Generated
   public GuiManager getGuiManager() {
      return this.guiManager;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Generated
   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   @Generated
   public GuiClient getGuiClient() {
      return this.guiClient;
   }

   @Generated
   public void setManager(Manager manager) {
      this.manager = manager;
   }

   @Generated
   public void setGuiManager(GuiManager guiManager) {
      this.guiManager = guiManager;
   }

   @Generated
   public void setConfigManager(ConfigManager configManager) {
      this.configManager = configManager;
   }

   @Generated
   public void setFriendManager(FriendManager friendManager) {
      this.friendManager = friendManager;
   }

   @Generated
   public void setGuiClient(GuiClient guiClient) {
      this.guiClient = guiClient;
   }
}