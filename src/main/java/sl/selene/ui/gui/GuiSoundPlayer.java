package sl.selene.ui.gui;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class GuiSoundPlayer {
   private static Clip openClip;
   private static Clip closeClip;

   public static void preload() {
      Thread thread = new Thread(() -> {
         openClip = loadClip("open");
         closeClip = loadClip("close");
         System.out.println("[Selene] GUI sounds preloaded");
      }, "SeleneSoundPreload");
      thread.setDaemon(true);
      thread.start();
   }

   public static void playOpenSound() {
      playClip(openClip);
   }

   public static void playCloseSound() {
      playClip(closeClip);
   }

   private static Clip loadClip(String location) {
      try {
         String resourcePath = "/assets/selene/sound/wav/" + location + ".wav";
         InputStream inputStream = GuiSoundPlayer.class.getResourceAsStream(resourcePath);
         if (inputStream == null) {
            System.out.println("[Selene] Sound not found: " + resourcePath);
            return null;
         }

         BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
         AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedInputStream);
         Clip clip = AudioSystem.getClip();
         clip.open(audioStream);

         FloatControl volumeControl = (FloatControl) clip.getControl(Type.MASTER_GAIN);
         volumeControl.setValue((float) (Math.log(0.5) / Math.log(10.0) * 20.0));
         return clip;
      } catch (Exception var5) {
         System.out.println("[Selene] Sound load failed (" + location + "): " + var5.getMessage());
         return null;
      }
   }

   private static void playClip(Clip clip) {
      if (clip == null) {
         return;
      }
      try {
         clip.setFramePosition(0);
         clip.start();
      } catch (Exception var2) {
         System.out.println("[Selene] Sound playback failed: " + var2.getMessage());
      }
   }

   public static void releaseAll() {
      closeClip(openClip);
      openClip = null;
      closeClip(closeClip);
      closeClip = null;
   }

   private static void closeClip(Clip clip) {
      if (clip == null) return;
      try {
         if (clip.isRunning()) clip.stop();
      } catch (Exception ignored) {
      }
      try {
         if (clip.isOpen()) clip.close();
      } catch (Exception ignored) {
      }
   }
}