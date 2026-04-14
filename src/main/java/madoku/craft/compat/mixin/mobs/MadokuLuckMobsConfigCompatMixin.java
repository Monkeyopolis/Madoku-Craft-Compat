package madoku.craft.compat.mixin.mobs;

import madoku.craft.compat.integration.mobs.AttributesMobsLuckCompat;
import madoku.craft.luck.MadokuLuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MadokuLuck.class, priority = 500)
public abstract class MadokuLuckMobsConfigCompatMixin {
	@Inject(method = "loadStaticConfig", at = @At("TAIL"))
	private static void madokuCompat$extendLuckConfigForMobs(CallbackInfo ci) {
		AttributesMobsLuckCompat.ensureLuckMobsConfigKeys();
	}
}
