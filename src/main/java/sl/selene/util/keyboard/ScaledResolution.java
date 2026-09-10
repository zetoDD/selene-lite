package sl.selene.util.keyboard;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ScaledResolution {
   private final double scaledWidthD;
   private final double scaledHeightD;
   private int scaledWidth;
   private int scaledHeight;
   private static int scaleFactor;

   public ScaledResolution(MinecraftClient minecraftClient) {
      this.scaledWidth = minecraftClient.getWindow().getWidth();
      this.scaledHeight = minecraftClient.getWindow().getHeight();
      scaleFactor = 1;
      int i = 2;
      if (i == 0) {
         i = 1000;
      }

      while (scaleFactor < i && this.scaledWidth / (scaleFactor + 1) >= 320 && this.scaledHeight / (scaleFactor + 1) >= 240) {
         scaleFactor++;
      }

      this.scaledWidthD = (double)this.scaledWidth / scaleFactor;
      this.scaledHeightD = (double)this.scaledHeight / scaleFactor;
      this.scaledWidth = MathHelper.ceil(this.scaledWidthD);
      this.scaledHeight = MathHelper.ceil(this.scaledHeightD);
   }

   public int getScaledWidth() {
      return this.scaledWidth;
   }

   public int getScaledHeight() {
      return this.scaledHeight;
   }

   public int getWidth() {
      return this.scaledWidth;
   }

   public int getHeight() {
      return this.scaledHeight;
   }

   public double getScaledWidth_double() {
      return this.scaledWidthD;
   }

   public double getScaledHeight_double() {
      return this.scaledHeightD;
   }

   public static int getScaleFactor() {
      return scaleFactor;
   }
}