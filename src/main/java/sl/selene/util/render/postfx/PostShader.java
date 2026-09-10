package sl.selene.util.render.postfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20;
import sl.selene.util.render.backends.gl.ResourceUtils;

@Environment(EnvType.CLIENT)
final class PostShader {
   private static final Logger LOGGER = LogManager.getLogger("Selene ShaderPost");
   final int program;
   final int uTex0;
   final int uTexelSize;
   final int uDirection;
   final int uSigma;

   PostShader(String vertPath, String fragPath) {
      int vs = compile(35633, ResourceUtils.readText(vertPath), vertPath);
      int fs = compile(35632, ResourceUtils.readText(fragPath), fragPath);
      int createdProgram = GL20.glCreateProgram();
      if (createdProgram == 0) {
         GL20.glDeleteShader(vs);
         GL20.glDeleteShader(fs);
         String message = "Failed to create OpenGL program for post-processing shaders.";
         LOGGER.error(message);
         throw new IllegalStateException(message);
      } else {
         try {
            GL20.glAttachShader(createdProgram, vs);
            GL20.glAttachShader(createdProgram, fs);
            GL20.glLinkProgram(createdProgram);
            int linkStatus = GL20.glGetProgrami(createdProgram, 35714);
            if (linkStatus == 0) {
               String infoLog = GL20.glGetProgramInfoLog(createdProgram);
               LOGGER.error("Failed to link post shader program (vert: {}, frag: {}): {}", vertPath, fragPath, infoLog);
               throw new IllegalStateException("Failed to link post shader program: " + infoLog);
            }
         } catch (RuntimeException var8) {
            GL20.glDeleteProgram(createdProgram);
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            throw var8;
         }

         GL20.glDeleteShader(vs);
         GL20.glDeleteShader(fs);
         this.program = createdProgram;
         this.uTex0 = GL20.glGetUniformLocation(this.program, "uTex0");
         this.uTexelSize = GL20.glGetUniformLocation(this.program, "uTexelSize");
         this.uDirection = GL20.glGetUniformLocation(this.program, "uDirection");
         this.uSigma = GL20.glGetUniformLocation(this.program, "uSigma");
      }
   }

   private static int compile(int type, String src, String shaderPath) {
      int shader = GL20.glCreateShader(type);
      if (shader == 0) {
         String message = "Failed to create shader object for type " + shaderTypeName(type) + " (" + shaderPath + ").";
         LOGGER.error(message);
         throw new IllegalStateException(message);
      } else {
         GL20.glShaderSource(shader, src);
         GL20.glCompileShader(shader);
         int status = GL20.glGetShaderi(shader, 35713);
         if (status == 0) {
            String infoLog = GL20.glGetShaderInfoLog(shader);
            String message = "Failed to compile " + shaderTypeName(type) + " shader (" + shaderPath + "): " + infoLog;
            LOGGER.error(message);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException(message);
         } else {
            return shader;
         }
      }
   }

   private static String shaderTypeName(int type) {
      return switch (type) {
         case 35632 -> "fragment";
         case 35633 -> "vertex";
         default -> "unknown";
      };
   }
}