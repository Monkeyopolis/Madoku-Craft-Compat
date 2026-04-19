package madoku.craft.compat.mixin.levels;

import madoku.craft.compat.integration.levels.LevelsAttributesCompat;
import madoku.craft.levels.MadokuLevels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MadokuLevels.class, priority = 500)
public abstract class LevelsAttributesConfigCompatMixin {
	@Inject(method = "loadStaticConfig", at = @At("TAIL"))
	private static void madokuCompat$applyAttributeAwareLevelsConfig(CallbackInfo ci) {
		LevelsAttributesCompat.applyAttributeAwareSettings();
	}
}
