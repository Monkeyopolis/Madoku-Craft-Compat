package madoku.craft.compat.mixin.mobs;

import com.google.gson.JsonObject;
import madoku.craft.compat.integration.mobs.PetsMobsHagCompat;
import madoku.craft.mobs.mob.system.MadokuMobConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = MadokuMobConfig.class, priority = 500)
public abstract class MadokuMobConfigPetsHagCompatMixin {
	@Inject(method = "buildDefaultMobFileDefaults", at = @At("RETURN"))
	private static void madokuCompat$addHagMobDefaults(CallbackInfoReturnable<Map<String, JsonObject>> cir) {
		PetsMobsHagCompat.addHagMobDefaults(cir.getReturnValue());
	}

	@Inject(method = "buildDynamicMobDefaults", at = @At("HEAD"), cancellable = true)
	private static void madokuCompat$buildDynamicHagMobDefaults(
		String fileKey,
		CallbackInfoReturnable<JsonObject> cir
	) {
		if (PetsMobsHagCompat.isHagFileKey(fileKey)) {
			cir.setReturnValue(PetsMobsHagCompat.buildHagMobDefaults());
		}
	}
}
