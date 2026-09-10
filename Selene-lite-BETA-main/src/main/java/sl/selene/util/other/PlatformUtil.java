package sl.selene.util.other;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PlatformUtil {
   private static final String OS_NAME = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

   private PlatformUtil() {
   }

   public static boolean isMac() {
      return OS_NAME.contains("mac");
   }

   public static boolean isWindows() {
      return OS_NAME.contains("win");
   }

   public static boolean isLinux() {
      return OS_NAME.contains("nux") || OS_NAME.contains("linux");
   }
}