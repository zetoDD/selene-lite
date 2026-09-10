package sl.selene.util.render.utils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sl.selene.Selene;

@Environment(EnvType.CLIENT)
public class SoundUtil {
   private static final int MAX_WAV_CLIPS = 8;
   private static Clip currentClip = null;
   private static final List<Clip> CLIPS_LIST = new ArrayList<>();
   private static final List<Clip> MP3_CLIPS = new ArrayList<>();

   public static void playSound_mp3(String sound, float value, boolean nonstop) {
      closeClip(currentClip);
      currentClip = null;
      purgeFinishedClips();

      try (InputStream is = Selene.class.getResourceAsStream("/assets/" + "selene" + "/sound/mp3/" + sound)) {
         if (is == null) {
            System.out.println("Sound not found!");
            return;
         }

         try (BufferedInputStream bis = new BufferedInputStream(is);
              AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis)) {
            if (audioInputStream == null) {
               System.out.println("Sound not found!");
               return;
            }

            Clip clip = AudioSystem.getClip();
            currentClip = clip;
            clip.open(audioInputStream);
            MP3_CLIPS.add(clip);
            clip.start();
            FloatControl floatControl = (FloatControl)clip.getControl(Type.MASTER_GAIN);
            float min = floatControl.getMinimum();
            float max = floatControl.getMaximum();
            float volumeInDecibels = (float)(min * (1.0 - value / 100.0) + max * (value / 100.0));
            floatControl.setValue(volumeInDecibels);
            if (nonstop) {
               Clip loopClip = clip;
               clip.addLineListener(event -> {
                  if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP && loopClip.isOpen()) {
                     loopClip.setFramePosition(0);
                     loopClip.start();
                  }
               });
            }
         }
      } catch (Exception var10) {
         closeClip(currentClip);
         currentClip = null;
         var10.printStackTrace();
      }
   }

   public static void playSound_wav(String location, float volume) {
      purgeFinishedClips();

      String resourcePath = "/assets/" + "selene" + "/sound/wav/" + location + ".wav";
      InputStream inputStream = SoundUtil.class.getResourceAsStream(resourcePath);
      if (inputStream == null) {
         return;
      }

      try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
           AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedInputStream)) {
         Clip clip = AudioSystem.getClip();
         clip.open(audioStream);
         float volumeVal = Math.max(0.001f, Math.min(1.0F, volume));
         FloatControl volumeControl = (FloatControl)clip.getControl(Type.MASTER_GAIN);
         float min = volumeControl.getMinimum();
         float max = volumeControl.getMaximum();
         float volumeInDecibels = (float)(Math.log(volumeVal) / Math.log(10.0) * 20.0);
         volumeControl.setValue(Math.max(min, Math.min(max, volumeInDecibels)));
         clip.start();
         while (CLIPS_LIST.size() >= MAX_WAV_CLIPS) {
            closeClip(CLIPS_LIST.remove(0));
         }
         CLIPS_LIST.add(clip);
      } catch (Exception var6) {
         var6.printStackTrace();
      }
   }

   public static void releaseAll() {
      closeClip(currentClip);
      currentClip = null;
      for (Clip clip : new ArrayList<>(CLIPS_LIST)) {
         closeClip(clip);
      }
      CLIPS_LIST.clear();
      for (Clip clip : new ArrayList<>(MP3_CLIPS)) {
         closeClip(clip);
      }
      MP3_CLIPS.clear();
   }

   private static void purgeFinishedClips() {
      for (int i = CLIPS_LIST.size() - 1; i >= 0; i--) {
         Clip clip = CLIPS_LIST.get(i);
         if (clip == null || !clip.isOpen() || !clip.isRunning()) {
            closeClip(clip);
            CLIPS_LIST.remove(i);
         }
      }
      for (int i = MP3_CLIPS.size() - 1; i >= 0; i--) {
         Clip clip = MP3_CLIPS.get(i);
         if (clip == null || !clip.isOpen() || !clip.isRunning()) {
            closeClip(clip);
            MP3_CLIPS.remove(i);
         }
      }
   }

   private static void closeClip(Clip clip) {
      if (clip == null) {
         return;
      }

      try {
         if (clip.isRunning()) {
            clip.stop();
         }
      } catch (Exception ignored) {
      }

      try {
         if (clip.isOpen()) {
            clip.close();
         }
      } catch (Exception ignored) {
      }
   }
}