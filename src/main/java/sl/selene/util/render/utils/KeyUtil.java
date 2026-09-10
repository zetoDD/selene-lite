package sl.selene.util.render.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class KeyUtil {
   public static final Map<String, Integer> keyMap = new HashMap<>();
   public static final Map<Integer, String> reverseKeyMap = new HashMap<>();
   public static MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean isKeyDown(int keyCode) {
      return sl.selene.util.keyboard.Keyboard.isKeyDown(keyCode);
   }

   public static String getKey(int key) {
      if (key == -1) {
         return "KEY";
      } else if (key <= -100) {
         int mouseButton = -100 - key;
         return "MB" + (mouseButton + 1);
      } else if (key == 32) {
         return "Space";
      } else if (key == 39) {
         return "Apostrophe";
      } else if (key == 44) {
         return "Comma";
      } else if (key == 45) {
         return "Minus";
      } else if (key == 46) {
         return "Period";
      } else if (key == 47) {
         return "Slash";
      } else if (key == 48) {
         return "0";
      } else if (key == 49) {
         return "1";
      } else if (key == 50) {
         return "2";
      } else if (key == 51) {
         return "3";
      } else if (key == 52) {
         return "4";
      } else if (key == 53) {
         return "5";
      } else if (key == 54) {
         return "6";
      } else if (key == 55) {
         return "7";
      } else if (key == 56) {
         return "8";
      } else if (key == 57) {
         return "9";
      } else if (key == 59) {
         return "SemiColon";
      } else if (key == 61) {
         return "Equal";
      } else if (key == 65) {
         return "A";
      } else if (key == 66) {
         return "B";
      } else if (key == 67) {
         return "C";
      } else if (key == 68) {
         return "D";
      } else if (key == 69) {
         return "E";
      } else if (key == 70) {
         return "F";
      } else if (key == 71) {
         return "G";
      } else if (key == 72) {
         return "H";
      } else if (key == 73) {
         return "I";
      } else if (key == 74) {
         return "J";
      } else if (key == 75) {
         return "K";
      } else if (key == 76) {
         return "L";
      } else if (key == 77) {
         return "M";
      } else if (key == 78) {
         return "N";
      } else if (key == 79) {
         return "O";
      } else if (key == 80) {
         return "P";
      } else if (key == 81) {
         return "Q";
      } else if (key == 82) {
         return "R";
      } else if (key == 83) {
         return "S";
      } else if (key == 84) {
         return "T";
      } else if (key == 85) {
         return "U";
      } else if (key == 86) {
         return "V";
      } else if (key == 87) {
         return "W";
      } else if (key == 88) {
         return "X";
      } else if (key == 89) {
         return "Y";
      } else if (key == 90) {
         return "Z";
      } else if (key == 91) {
         return "LeftBracket";
      } else if (key == 92) {
         return "BackSlash";
      } else if (key == 93) {
         return "RightBracket";
      } else if (key == 96) {
         return "GraveAccent";
      } else if (key == 161) {
         return "World1";
      } else if (key == 162) {
         return "World2";
      } else if (key == 256) {
         return "Escape";
      } else if (key == 257) {
         return "Enter";
      } else if (key == 258) {
         return "Tab";
      } else if (key == 259) {
         return "BackSpace";
      } else if (key == 260) {
         return "Insert";
      } else if (key == 261) {
         return "Delete";
      } else if (key == 262) {
         return "Right";
      } else if (key == 263) {
         return "Left";
      } else if (key == 264) {
         return "Down";
      } else if (key == 265) {
         return "Up";
      } else if (key == 266) {
         return "PageUp";
      } else if (key == 267) {
         return "PageDown";
      } else if (key == 268) {
         return "Home";
      } else if (key == 269) {
         return "End";
      } else if (key == 280) {
         return "CapsLock";
      } else if (key == 281) {
         return "ScrollLock";
      } else if (key == 282) {
         return "NumLock";
      } else if (key == 283) {
         return "PrintScreen";
      } else if (key == 284) {
         return "Pause";
      } else if (key == 290) {
         return "F1";
      } else if (key == 291) {
         return "F2";
      } else if (key == 292) {
         return "F3";
      } else if (key == 293) {
         return "F4";
      } else if (key == 294) {
         return "F5";
      } else if (key == 295) {
         return "F6";
      } else if (key == 296) {
         return "F7";
      } else if (key == 297) {
         return "F8";
      } else if (key == 298) {
         return "F9";
      } else if (key == 299) {
         return "F10";
      } else if (key == 300) {
         return "F11";
      } else if (key == 301) {
         return "F12";
      } else if (key == 302) {
         return "F13";
      } else if (key == 303) {
         return "F14";
      } else if (key == 304) {
         return "F15";
      } else if (key == 305) {
         return "F16";
      } else if (key == 306) {
         return "F17";
      } else if (key == 307) {
         return "F18";
      } else if (key == 308) {
         return "F19";
      } else if (key == 309) {
         return "F20";
      } else if (key == 310) {
         return "F21";
      } else if (key == 311) {
         return "F22";
      } else if (key == 312) {
         return "F23";
      } else if (key == 313) {
         return "F24";
      } else if (key == 314) {
         return "F25";
      } else if (key == 320) {
         return "NUM 0";
      } else if (key == 321) {
         return "NUM 1";
      } else if (key == 322) {
         return "NUM 2";
      } else if (key == 323) {
         return "NUM 3";
      } else if (key == 324) {
         return "NUM 4";
      } else if (key == 325) {
         return "NUM 5";
      } else if (key == 326) {
         return "NUM 6";
      } else if (key == 327) {
         return "NUM 7";
      } else if (key == 328) {
         return "NUM 8";
      } else if (key == 329) {
         return "NUM 9";
      } else if (key == 330) {
         return "Decimal";
      } else if (key == 331) {
         return "Divide";
      } else if (key == 332) {
         return "Multiply";
      } else if (key == 333) {
         return "Subtract";
      } else if (key == 334) {
         return "Add";
      } else if (key == 335) {
         return "NUM Enter";
      } else if (key == 336) {
         return "NUM Equal";
      } else if (key == 340) {
         return "LeftShift";
      } else if (key == 341) {
         return "LeftControl";
      } else if (key == 342) {
         return "LeftAlt";
      } else if (key == 343) {
         return "LeftSuper";
      } else if (key == 344) {
         return "RightShift";
      } else if (key == 345) {
         return "RightControl";
      } else if (key == 346) {
         return "RightAlt";
      } else if (key == 347) {
         return "RightSuper";
      } else {
         return key == 348 ? "Menu" : "ERROR";
      }
   }

   private static void getBindCMD() {
      keyMap.put("A", 65);
      keyMap.put("B", 66);
      keyMap.put("C", 67);
      keyMap.put("D", 68);
      keyMap.put("E", 69);
      keyMap.put("F", 70);
      keyMap.put("G", 71);
      keyMap.put("H", 72);
      keyMap.put("I", 73);
      keyMap.put("J", 74);
      keyMap.put("K", 75);
      keyMap.put("L", 76);
      keyMap.put("M", 77);
      keyMap.put("N", 78);
      keyMap.put("O", 79);
      keyMap.put("P", 80);
      keyMap.put("Q", 81);
      keyMap.put("R", 82);
      keyMap.put("S", 83);
      keyMap.put("T", 84);
      keyMap.put("U", 85);
      keyMap.put("V", 86);
      keyMap.put("W", 87);
      keyMap.put("X", 88);
      keyMap.put("Y", 89);
      keyMap.put("Z", 90);
      keyMap.put("0", 48);
      keyMap.put("1", 49);
      keyMap.put("2", 50);
      keyMap.put("3", 51);
      keyMap.put("4", 52);
      keyMap.put("5", 53);
      keyMap.put("6", 54);
      keyMap.put("7", 55);
      keyMap.put("8", 56);
      keyMap.put("9", 57);
      keyMap.put("F1", 290);
      keyMap.put("F2", 291);
      keyMap.put("F3", 292);
      keyMap.put("F4", 293);
      keyMap.put("F5", 294);
      keyMap.put("F6", 295);
      keyMap.put("F7", 296);
      keyMap.put("F8", 297);
      keyMap.put("F9", 298);
      keyMap.put("F10", 299);
      keyMap.put("F11", 300);
      keyMap.put("F12", 301);
      keyMap.put("F13", 302);
      keyMap.put("F14", 303);
      keyMap.put("F15", 304);
      keyMap.put("F16", 305);
      keyMap.put("F17", 306);
      keyMap.put("F18", 307);
      keyMap.put("F19", 308);
      keyMap.put("F20", 309);
      keyMap.put("F21", 310);
      keyMap.put("F22", 311);
      keyMap.put("F23", 312);
      keyMap.put("F24", 313);
      keyMap.put("F25", 314);
      keyMap.put("NUM 0", 320);
      keyMap.put("NUM 1", 321);
      keyMap.put("NUM 2", 322);
      keyMap.put("NUM 3", 323);
      keyMap.put("NUM 4", 324);
      keyMap.put("NUM 5", 325);
      keyMap.put("NUM 6", 326);
      keyMap.put("NUM 7", 327);
      keyMap.put("NUM 8", 328);
      keyMap.put("NUM 9", 329);
      keyMap.put("Decimal", 330);
      keyMap.put("Divide", 331);
      keyMap.put("Multiply", 332);
      keyMap.put("Subtract", 333);
      keyMap.put("Add", 334);
      keyMap.put("NUM Enter", 335);
      keyMap.put("NUM Equal", 336);
      keyMap.put("Space", 32);
      keyMap.put("Enter", 257);
      keyMap.put("Escape", 256);
      keyMap.put("Tab", 258);
      keyMap.put("Home", 268);
      keyMap.put("Insert", 260);
      keyMap.put("Delete", 261);
      keyMap.put("End", 269);
      keyMap.put("PageUp", 266);
      keyMap.put("PageDown", 267);
      keyMap.put("Right", 262);
      keyMap.put("Left", 263);
      keyMap.put("Down", 264);
      keyMap.put("Up", 265);
      keyMap.put("RightShift", 344);
      keyMap.put("LeftShift", 340);
      keyMap.put("RightControl", 345);
      keyMap.put("LeftControl", 341);
      keyMap.put("RightAlt", 346);
      keyMap.put("LeftAlt", 342);
      keyMap.put("RightSuper", 347);
      keyMap.put("LeftSuper", 343);
      keyMap.put("Menu", 348);
      keyMap.put("CapsLock", 280);
      keyMap.put("NumLock", 282);
      keyMap.put("ScrollLock", 281);
      keyMap.put("PrintScreen", 283);
      keyMap.put("Pause", 284);
      keyMap.put("Apostrophe", 39);
      keyMap.put("Slash", 47);
      keyMap.put("Minus", 45);
      keyMap.put("Equal", 61);
      keyMap.put("BackSpace", 259);
      keyMap.put("BackSlash", 92);
      keyMap.put("Period", 46);
      keyMap.put("Comma", 44);
      keyMap.put("SemiColon", 59);
      keyMap.put("LeftBracket", 91);
      keyMap.put("RightBracket", 93);
      keyMap.put("GraveAccent", 96);
      keyMap.put("World1", 161);
      keyMap.put("World2", 162);
   }

   private static void reverseMappings() {
      for (Entry<String, Integer> entry : keyMap.entrySet()) {
         reverseKeyMap.put(entry.getValue(), entry.getKey());
      }
   }

   static {
      getBindCMD();
      reverseMappings();
   }
}