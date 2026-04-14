package madoku.craft.compat.mixin.mobs;

import com.google.gson.JsonObject;
import madoku.craft.compat.integration.mobs.PetsMobsHagCompat;
import madoku.craft.entity.MadokuEntities;
import madoku.craft.mobs.mob.system.MadokuMob;
import madoku.craft.mobs.mob.system.MadokuMobConfig;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MadokuMob.class, priority = 500)
public abstract class MadokuMobPetsHagCompatMixin {
	@Shadow
	private static JsonObject root(String key) {
		throw new AssertionError();
	}

	@Shadow
	private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
		throw new AssertionError();
	}

	@Shadow
	private static boolean applyUniversalStats(LivingEntity entity, JsonObject root) {
		throw new AssertionError();
	}

	@Inject(method = "applyMobSpawnOverridesFromGenericMixin", at = @At("TAIL"))
	private static void madokuCompat$applyHagSpawnOverrides(
		Mob mob,
		ServerLevelAccessor world,
		DifficultyInstance difficulty,
		EntitySpawnReason spawnReason,
		CallbackInfo ci
	) {
		if (mob == null || mob.getType() != MadokuEntities.HAG) {
			return;
		}
		JsonObject hagRoot = root(PetsMobsHagCompat.HAG_FILE_KEY);
		if (readBoolean(hagRoot, MadokuMobConfig.FIELD_ENABLED, true)) {
			applyUniversalStats(mob, hagRoot);
		}
	}

	@Inject(method = "applyLoadedEntityRules", at = @At("RETURN"), cancellable = true)
	private static void madokuCompat$applyHagLoadedEntityRules(
		LivingEntity entity,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (Boolean.TRUE.equals(cir.getReturnValue()) || entity == null || entity.getType() != MadokuEntities.HAG) {
			return;
		}
		JsonObject hagRoot = root(PetsMobsHagCompat.HAG_FILE_KEY);
		cir.setReturnValue(readBoolean(hagRoot, MadokuMobConfig.FIELD_ENABLED, true) && applyUniversalStats(entity, hagRoot));
	}
}
