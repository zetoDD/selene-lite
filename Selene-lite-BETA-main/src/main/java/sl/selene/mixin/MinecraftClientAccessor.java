package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public interface MinecraftClientAccessor {
   @Invoker("doAttack")
   boolean invokeDoAttack();

   @Accessor("attackCooldown")
   void setAttackCooldown(int var1);

   @Accessor("attackCooldown")
   int getAttackCooldown();

   @Accessor("itemUseCooldown")
   void setItemUseCooldown(int var1);

   @Accessor("itemUseCooldown")
   int getItemUseCooldown();
}