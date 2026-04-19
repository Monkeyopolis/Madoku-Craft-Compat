package madoku.craft.compat.mixin.mobs;

import com.google.gson.JsonObject;
import madoku.craft.compat.integration.mobs.PetsMobsHagCompat;
import madoku.craft.mobs.difficulty.system.MadokuDifficultyConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = MadokuDifficultyConfig.class, priority = 500)
public abstract class MadokuDifficultyConfigPetsHagCompatMixin {
	@Inject(method = "buildDefaultMobScalingFileDefaults(DDDDDD)Ljava/util/Map;", at = @At("RETURN"))
	private static void madokuCompat$addHagDifficultyDefaults(
		double health,
		double movementSpeed,
		double armor,
		double damage,
		double knockbackResistance,
		double experienceDrop,
		CallbackInfoReturnable<Map<String, JsonObject>> cir
	) {
		PetsMobsHagCompat.addHagDifficultyScalingDefaults(
			cir.getReturnValue(),
			health,
			movementSpeed,
			armor,
			damage,
			knockbackResistance,
			experienceDrop
		);
	}

	@Inject(method = "buildDynamicMobScalingDefaults", at = @At("HEAD"), cancellable = true)
	private static void madokuCompat$buildHagDynamicDifficultyDefaults(
		String fileKey,
		CallbackInfoReturnable<JsonObject> cir
	) {
		if (PetsMobsHagCompat.isHagFileKey(fileKey)) {
			cir.setReturnValue(PetsMobsHagCompat.buildDynamicHagDifficultyScalingDefaults());
		}
	}
}
