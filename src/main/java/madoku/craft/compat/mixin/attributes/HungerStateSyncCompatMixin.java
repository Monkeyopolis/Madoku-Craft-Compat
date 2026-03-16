package madoku.craft.compat.mixin.attributes;

import madoku.craft.compat.integration.HungerHudClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.network.HungerStateSync", remap = false)
public class HungerStateSyncCompatMixin {
	@Inject(method = "updateClientState", at = @At("TAIL"))
	private static void madokuCompat$updateHudHungerState(int current, int pending, int max, CallbackInfo ci) {
		HungerHudClientState.update(current, pending, max);
	}

	@Inject(method = "clearClientState", at = @At("TAIL"))
	private static void madokuCompat$clearHudHungerState(CallbackInfo ci) {
		HungerHudClientState.clear();
	}
}
