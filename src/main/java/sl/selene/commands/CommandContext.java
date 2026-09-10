package sl.selene.commands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;

@Environment(EnvType.CLIENT)
public final class CommandContext {
   private final Command command;
   private final String alias;
   private final String rawInput;
   private final String arguments;

   CommandContext(Command command, String alias, String rawInput, String arguments) {
      this.command = command;
      this.alias = alias;
      this.rawInput = rawInput;
      this.arguments = arguments;
   }

   public Command command() {
      return this.command;
   }

   public String alias() {
      return this.alias;
   }

   public String rawInput() {
      return this.rawInput;
   }

   public String arguments() {
      return this.arguments;
   }

   public MinecraftClient client() {
      return MinecraftClient.getInstance();
   }

   public void sendSuccess(String message) {
      this.sendGradientMessage(Text.literal(message), 16748784, 8104191);
   }

   public void sendError(String message) {
      this.sendGradientMessage(Text.literal(message), 16739179, 16723555);
   }

   public void sendInfo(String message) {
      this.sendGradientMessage(Text.literal(message), 16757759, 8119295);
   }

   public void sendInfo(Text message) {
      this.sendGradientMessage(message, 16757759, 8119295);
   }

   private void sendGradientMessage(Text payload, int startColor, int endColor) {
      MutableText prefix = this.buildGradient("[Selene]", startColor, endColor);
      MutableText fullMessage = prefix.append(Text.literal(" ")).append(payload.copy());
      this.dispatchMessage(fullMessage);
   }

   private MutableText buildGradient(String text, int startColor, int endColor) {
      MutableText result = Text.empty();
      int[] codePoints = text.codePoints().toArray();
      int length = codePoints.length;

      for (int index = 0; index < length; index++) {
         double ratio = length <= 1 ? 0.0 : (double)index / (length - 1);
         int color = this.interpolateColor(startColor, endColor, ratio);
         String character = new String(Character.toChars(codePoints[index]));
         MutableText part = Text.literal(character).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
         result = result.append(part);
      }

      return result;
   }

   private int interpolateColor(int startColor, int endColor, double ratio) {
      double clampedRatio = Math.max(0.0, Math.min(1.0, ratio));
      int startR = startColor >> 16 & 0xFF;
      int startG = startColor >> 8 & 0xFF;
      int startB = startColor & 0xFF;
      int endR = endColor >> 16 & 0xFF;
      int endG = endColor >> 8 & 0xFF;
      int endB = endColor & 0xFF;
      int red = (int)Math.round(startR + (endR - startR) * clampedRatio);
      int green = (int)Math.round(startG + (endG - startG) * clampedRatio);
      int blue = (int)Math.round(startB + (endB - startB) * clampedRatio);
      return (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
   }

   private void dispatchMessage(MutableText text) {
      MinecraftClient client = this.client();
      if (client != null) {
         if (client.player != null) {
            client.player.sendMessage(text, false);
         } else if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(text);
         }
      }
   }
}