package madoku.craft.compat.mixin.farming;

import madoku.craft.farming.system.MadokuFarming;
import madoku.craft.items.item.system.MadokuItem;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MadokuFarming.class, priority = 500)
public abstract class FarmingItemsMetadataCompatMixin {
	@Inject(method = "onServerStarted", at = @At("TAIL"))
	private static void madokuCompat$applyFarmingItemMetadata(MinecraftServer server, CallbackInfo ci) {
		if (!MadokuItem.isEnabled()) {
			return;
		}
		MadokuFarming.applyCropItemMetadata();
	}
}
