package madoku.craft.compat.mixin.client.levels;

import madoku.craft.compat.integration.levels.LevelsAttributesClientState;
import madoku.craft.network.MadokuLevelsPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.levels.MadokuLevelsClientState", remap = false)
public abstract class LevelsClientStateAttributesCompatMixin {
	@Inject(method = "applyPayload", at = @At("TAIL"))
	private static void madokuCompat$mirrorLevelsPayload(MadokuLevelsPayload payload, CallbackInfo ci) {
		LevelsAttributesClientState.applyPayload(payload);
	}

	@Inject(method = "clear", at = @At("TAIL"))
	private static void madokuCompat$clearMirroredLevelsPayload(CallbackInfo ci) {
		LevelsAttributesClientState.clear();
	}
}
