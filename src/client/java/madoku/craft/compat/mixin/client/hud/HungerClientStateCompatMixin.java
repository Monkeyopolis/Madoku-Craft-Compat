package madoku.craft.compat.mixin.client.hud;

import madoku.craft.compat.integration.HungerHudClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.Hunger.client.HungerClientState", remap = false)
public class HungerClientStateCompatMixin {
	@Inject(method = "update", at = @At("TAIL"))
	private static void madokuCompat$trackHungerState(int current, int pending, int max, int sprintThreshold,
			CallbackInfo ci) {
		HungerHudClientState.update(current, pending, max);
	}

	@Inject(method = "clear", at = @At("TAIL"))
	private static void madokuCompat$clearTrackedHungerState(CallbackInfo ci) {
		HungerHudClientState.clear();
	}
}
