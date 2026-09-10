package sl.selene.util.other;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.Window;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public interface IMinecraft {
   MinecraftClient mc = MinecraftClient.getInstance();
   Window mw = mc.getWindow();
}