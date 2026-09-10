package sl.selene.util.keyboard;

import java.util.HashMap;
import java.util.Map;
import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public enum Keyboard {
   KEY_SPACE("SPACE", 32),
   KEY_APOSTROPHE("APOSTROPHE", 39),
   KEY_COMMA("COMMA", 44),
   KEY_MINUS("MINUS", 45),
   KEY_PERIOD("PERIOD", 46),
   KEY_SLASH("SLASH", 47),
   KEY_0("0", 48),
   KEY_1("1", 49),
   KEY_2("2", 50),
   KEY_3("3", 51),
   KEY_4("4", 52),
   KEY_5("5", 53),
   KEY_6("6", 54),
   KEY_7("7", 55),
   KEY_8("8", 56),
   KEY_9("9", 57),
   KEY_SEMICOLON("SEMICOLON", 59),
   KEY_EQUAL("EQUAL", 61),
   KEY_A("A", 65),
   KEY_B("B", 66),
   KEY_C("C", 67),
   KEY_D("D", 68),
   KEY_E("E", 69),
   KEY_F("F", 70),
   KEY_G("G", 71),
   KEY_H("H", 72),
   KEY_I("I", 73),
   KEY_J("J", 74),
   KEY_K("K", 75),
   KEY_L("L", 76),
   KEY_M("M", 77),
   KEY_N("N", 78),
   KEY_O("O", 79),
   KEY_P("P", 80),
   KEY_Q("Q", 81),
   KEY_R("R", 82),
   KEY_S("S", 83),
   KEY_T("T", 84),
   KEY_U("U", 85),
   KEY_V("V", 86),
   KEY_W("W", 87),
   KEY_X("X", 88),
   KEY_Y("Y", 89),
   KEY_Z("Z", 90),
   KEY_LEFT_BRACKET("LBRACKET", 91),
   KEY_BACKSLASH("BACKSLASH", 92),
   KEY_RIGHT_BRACKET("RBRACKET", 93),
   KEY_GRAVE("GRAVE", 96),
   KEY_WORLD_1("WORLD1", 161),
   KEY_WORLD_2("WORLD2", 162),
   KEY_ESCAPE("ESCAPE", 256),
   KEY_ENTER("ENTER", 257),
   KEY_TAB("TAB", 258),
   KEY_BACKSPACE("BACKSPACE", 259),
   KEY_INSERT("INSERT", 260),
   KEY_DELETE("DELETE", 261),
   KEY_RIGHT("RIGHT", 262),
   KEY_LEFT("LEFT", 263),
   KEY_DOWN("DOWN", 264),
   KEY_UP("UP", 265),
   KEY_PAGE_UP("PAGEUP", 266),
   KEY_PAGE_DOWN("PAGEDOWN", 267),
   KEY_HOME("HOME", 268),
   KEY_END("END", 269),
   KEY_CAPS_LOCK("CAPSLOCK", 280),
   KEY_SCROLL_LOCK("SCROLLLOCK", 281),
   KEY_NUM_LOCK("NUMLOCK", 282),
   KEY_PRINT_SCREEN("PRINTSCREEN", 283),
   KEY_PAUSE("PAUSE", 284),
   KEY_F1("F1", 290),
   KEY_F2("F2", 291),
   KEY_F3("F3", 292),
   KEY_F4("F4", 293),
   KEY_F5("F5", 294),
   KEY_F6("F6", 295),
   KEY_F7("F7", 296),
   KEY_F8("F8", 297),
   KEY_F9("F9", 298),
   KEY_F10("F10", 299),
   KEY_F11("F11", 300),
   KEY_F12("F12", 301),
   KEY_F13("F13", 302),
   KEY_F14("F14", 303),
   KEY_F15("F15", 304),
   KEY_F16("F16", 305),
   KEY_F17("F17", 306),
   KEY_F18("F18", 307),
   KEY_F19("F19", 308),
   KEY_F20("F20", 309),
   KEY_F21("F21", 310),
   KEY_F22("F22", 311),
   KEY_F23("F23", 312),
   KEY_F24("F24", 313),
   KEY_F25("F25", 314),
   KEY_KP_0("NUMPAD0", 320),
   KEY_KP_1("NUMPAD1", 321),
   KEY_KP_2("NUMPAD2", 322),
   KEY_KP_3("NUMPAD3", 323),
   KEY_KP_4("NUMPAD4", 324),
   KEY_KP_5("NUMPAD5", 325),
   KEY_KP_6("NUMPAD6", 326),
   KEY_KP_7("NUMPAD7", 327),
   KEY_KP_8("NUMPAD8", 328),
   KEY_KP_9("NUMPAD9", 329),
   KEY_KP_DECIMAL("KPDECIMAL", 330),
   KEY_KP_DIVIDE("KPDIVIDE", 331),
   KEY_KP_MULTIPLY("KPMULTIPLY", 332),
   KEY_KP_SUBTRACT("KPSUBTRACT", 333),
   KEY_KP_ADD("KPADD", 334),
   KEY_KP_ENTER("KPENTER", 335),
   KEY_KP_EQUAL("KPEQUAL", 336),
   KEY_LEFT_SHIFT("LSHIFT", 340),
   KEY_LEFT_CONTROL("LCONTROL", 341),
   KEY_LEFT_ALT("LALT", 342),
   KEY_LEFT_SUPER("LSUPER", 343),
   KEY_RIGHT_SHIFT("RSHIFT", 344),
   KEY_RIGHT_CONTROL("RCONTROL", 345),
   KEY_RIGHT_ALT("RALT", 346),
   KEY_RIGHT_SUPER("RSUPER", 347),
   KEY_MENU("MENU", 348),
   KEY_LAST("LAST", 348),
   MOUSE_1("MOUSE1", 0),
   MOUSE_2("MOUSE2", 1),
   MOUSE_3("MOUSE3", 2),
   MOUSE_4("MOUSE4", 3),
   MOUSE_5("MOUSE5", 4),
   MOUSE_6("MOUSE6", 5),
   MOUSE_7("MOUSE7", 6),
   MOUSE_8("MOUSE8", 7),
   MOUSE_LAST("MOUSELAST", 7),
   MOUSE_LEFT("MOUSELEFT", 0),
   MOUSE_RIGHT("MOUSERIGHT", 1),
   MOUSE_MIDDLE("MOUSEMIDDLE", 2),
   KEY_NONE("NONE", -1);

   private final String name;
   private final int key;
   private static final Map<Integer, Keyboard> KEY_CODE_MAP = new HashMap<>();
   private static final Map<String, Keyboard> KEY_NAME_MAP = new HashMap<>();

   @Override
   public String toString() {
      return this.name;
   }

   public static String keyName(int keyCode) {
      if (keyCode <= -100) {
         int mouseButton = -100 - keyCode;
         return "MB" + (mouseButton + 1);
      }
      Keyboard key = KEY_CODE_MAP.getOrDefault(keyCode, KEY_NONE);
      return key.name;
   }

   public static boolean isKeyDown(int keyCode) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.getWindow() == null) {
         return false;
      }
      long handle = client.getWindow().getHandle();
      if (handle == 0L) {
         return false;
      }
      if (keyCode <= -100) {
         int mouseButton = -100 - keyCode;
         return mouseButton >= 0 && mouseButton <= 7
            && GLFW.glfwGetMouseButton(handle, mouseButton) == GLFW.GLFW_PRESS;
      }
      if (keyCode <= 0) {
         return false;
      }
      return InputUtil.isKeyPressed(client.getWindow(), keyCode);
   }

   public static int keyCode(String keyName) {
      Keyboard key = KEY_NAME_MAP.getOrDefault(keyName.toLowerCase(), KEY_NONE);
      return key.key;
   }

   public static Keyboard findByKeyCode(int keyCode) {
      return KEY_CODE_MAP.getOrDefault(keyCode, KEY_NONE);
   }

   public boolean isKey(int keyCode) {
      return keyCode == this.getKey();
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   private Keyboard(final String name, final int key) {
      this.name = name;
      this.key = key;
   }

   static {
      for (Keyboard key : values()) {
         KEY_CODE_MAP.put(key.key, key);
         KEY_NAME_MAP.put(key.name.toLowerCase(), key);
      }
   }
}