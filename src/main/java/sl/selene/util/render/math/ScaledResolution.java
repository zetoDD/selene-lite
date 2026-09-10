package sl.selene.util.render.math;

import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.option.SimpleOption;

@Environment(EnvType.CLIENT)
public class ScaledResolution {
   private final double scaledWidthD;
   private final double scaledHeightD;
   private int scaledWidth;
   private int scaledHeight;
   private static int scaleFactor;

   public ScaledResolution(MinecraftClient mc) {
      if (mc != null && mc.getWindow() != null) {
         this.scaledWidth = mc.getWindow().getWidth();
         this.scaledHeight = mc.getWindow().getHeight();
         scaleFactor = 1;
         boolean forceUnicodeFont = false;

         try {
            GameOptions options = mc.options;

            try {
               Method getMethod = GameOptions.class.getMethod("getForceUnicodeFont");
               SimpleOption<Boolean> flag = (SimpleOption<Boolean>)getMethod.invoke(options);
               forceUnicodeFont = flag != null && (Boolean)flag.getValue();
            } catch (NoSuchMethodException var7) {
               Method forceUnicodeFontMethod = GameOptions.class.getDeclaredMethod("forceUnicodeFont");
               forceUnicodeFontMethod.setAccessible(true);
               SimpleOption<Boolean> flagx = (SimpleOption<Boolean>)forceUnicodeFontMethod.invoke(options);
               forceUnicodeFont = flagx != null && (Boolean)flagx.getValue();
            }
         } catch (Exception var8) {
         }

         int i = 2;

         while (scaleFactor < i && this.scaledWidth / (scaleFactor + 1) >= 320 && this.scaledHeight / (scaleFactor + 1) >= 240) {
            scaleFactor++;
         }

         if (forceUnicodeFont && scaleFactor % 2 != 0 && scaleFactor != 1) {
            scaleFactor--;
         }

         this.scaledWidthD = (double)this.scaledWidth / scaleFactor;
         this.scaledHeightD = (double)this.scaledHeight / scaleFactor;
         this.scaledWidth = MathHelper.ceil(this.scaledWidthD);
         this.scaledHeight = MathHelper.ceil(this.scaledHeightD);
      } else {
         this.scaledWidth = 1920;
         this.scaledHeight = 1080;
         scaleFactor = 1;
         this.scaledWidthD = this.scaledWidth;
         this.scaledHeightD = this.scaledHeight;
      }
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