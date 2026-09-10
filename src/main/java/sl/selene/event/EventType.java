package sl.selene.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EventType {
   public static final byte START = -1;
   public static final byte PRE = 0;
   public static final byte ON = 1;
   public static final byte POST = 2;
   public static final byte SEND = 3;
   public static final byte RECIEVE = 4;
}