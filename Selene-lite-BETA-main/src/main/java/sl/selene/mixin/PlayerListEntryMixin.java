package sl.selene.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.network.PlayerListEntry;

@Environment(EnvType.CLIENT)
@Mixin({ PlayerListEntry.class })
public abstract class PlayerListEntryMixin {
}