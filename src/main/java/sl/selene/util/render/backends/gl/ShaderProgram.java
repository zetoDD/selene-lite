package sl.selene.util.render.backends.gl;

import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
public final class ShaderProgram {

   private final int programId;

   public ShaderProgram(String vertexSource, String fragmentSource) {
      int vs = compile(35633, vertexSource);
      int fs = compile(35632, fragmentSource);
      this.programId = GL20.glCreateProgram();
      GL20.glAttachShader(this.programId, vs);
      GL20.glAttachShader(this.programId, fs);
      GL20.glLinkProgram(this.programId);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer status = stack.mallocInt(1);
         GL20.glGetProgramiv(this.programId, 35714, status);
         if (status.get(0) == 0) {
            String log = GL20.glGetProgramInfoLog(this.programId);
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            GL20.glDeleteProgram(this.programId);
            throw new IllegalStateException("Program link failed: " + log);
         }
      } catch (Throwable var9) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (stack != null) {
         stack.close();
      }

      GL20.glDetachShader(this.programId, vs);
      GL20.glDetachShader(this.programId, fs);
      GL20.glDeleteShader(vs);
      GL20.glDeleteShader(fs);
   }

   public static ShaderProgram fromResources(String vertexPath, String fragmentPath) {
      String vs = ResourceUtils.readText(vertexPath);
      String fs = ResourceUtils.readText(fragmentPath);
      return new ShaderProgram(vs, fs);
   }

   private static int compile(int type, String source) {
      int shader = GL20.glCreateShader(type);
      GL20.glShaderSource(shader, source);
      GL20.glCompileShader(shader);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer status = stack.mallocInt(1);
         GL20.glGetShaderiv(shader, 35713, status);
         if (status.get(0) == 0) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + log);
         }
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }

      return shader;
   }

   public void use() {
      GL20.glUseProgram(this.programId);
   }

   public void delete() {
      GL20.glDeleteProgram(this.programId);
   }

   public int id() {
      return this.programId;
   }

   public int getUniformLocation(String name) {
      return GL20.glGetUniformLocation(this.programId, name);
   }
}