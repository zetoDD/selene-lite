package sl.selene.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Colors {

   public static final int ACCENT = 0xFFFFFFFF;
   private static final int DEFAULT_CLIENT_PRIMARY = ACCENT;

   private Colors() {
   }

   public static int getDefaultClientPrimary() {
      return DEFAULT_CLIENT_PRIMARY;
   }

   public static int getClientPrimary() {
      return ACCENT;
   }
}