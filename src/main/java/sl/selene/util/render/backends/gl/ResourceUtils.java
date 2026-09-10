package sl.selene.util.render.backends.gl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.BufferUtils;

@Environment(EnvType.CLIENT)
public final class ResourceUtils {
   private ResourceUtils() {
   }

   public static String readText(String path) {
      ClassLoader cl = ResourceUtils.class.getClassLoader();

      try {
         String var6;
         try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) {
               throw new IllegalStateException("Resource not found: " + path);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
               StringBuilder sb = new StringBuilder();

               String line;
               while ((line = br.readLine()) != null) {
                  sb.append(line).append('\n');
               }

               var6 = sb.toString();
            }
         }

         return var6;
      } catch (IOException var11) {
         throw new RuntimeException("Failed to read resource: " + path, var11);
      }
   }

   public static ByteBuffer readBinary(String path) {
      ClassLoader cl = ResourceUtils.class.getClassLoader();

      try {
         ByteBuffer var5;
         try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) {
               throw new IllegalStateException("Resource not found: " + path);
            }

            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            var5 = buffer;
         }

         return var5;
      } catch (IOException var8) {
         throw new RuntimeException("Failed to read resource: " + path, var8);
      }
   }
}