package sl.selene.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Priority {
   public static final byte HIGHEST = 0;
   public static final byte HIGH = 1;
   public static final byte MEDIUM = 2;
   public static final byte LOW = 3;
   public static final byte LOWEST = 4;
   public static final byte[] VALUE_ARRAY = new byte[]{0, 1, 2, 3, 4};
}