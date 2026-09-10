package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public interface ClientPlayerInteractionManagerAccessor {
   @Accessor("blockBreakingCooldown")
   void setBlockBreakingCooldown(int var1);

   @Accessor("blockBreakingCooldown")
   int getBlockBreakingCooldown();

   @Invoker("syncSelectedSlot")
   void invokeSyncSelectedSlot();

   @Invoker("attackWithPiercingWeapon")
   void invokeAttackWithPiercingWeapon(net.minecraft.component.type.PiercingWeaponComponent var1);
}