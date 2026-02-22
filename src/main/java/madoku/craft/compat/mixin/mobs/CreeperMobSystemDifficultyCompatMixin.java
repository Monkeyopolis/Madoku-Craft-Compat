package madoku.craft.compat.mixin.mobs;

import madoku.craft.compat.MadokuCraftCompat;
import madoku.craft.compat.integration.mobs.MobsDifficultyAttributeBridge;
import madoku.craft.compat.system.CompatConfigSystem;
import madoku.craft.difficulty.config.DifficultyScalingConfig;
import madoku.craft.difficulty.mixin.CreeperEntityAccessor;
import madoku.craft.difficulty.system.DifficultyScaledMob;
import madoku.craft.mobs.system.CreeperMobConfig;
import madoku.craft.mobs.system.CreeperMobSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World.ExplosionSourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CreeperMobSystem.class, remap = false)
public class CreeperMobSystemDifficultyCompatMixin {
	private static final double MIN_FUSE_LENGTH_SECONDS = 0.05d;
	private static final ThreadLocal<CreeperEntity> CURRENT_CREEPER = new ThreadLocal<>();

	@Inject(method = "applyConfig", at = @At("HEAD"))
	private static void madokuCompat$captureCreeperForConfig(
			CreeperEntity creeper,
			CreeperMobConfig config,
			CallbackInfo ci
	) {
		CURRENT_CREEPER.set(creeper);
	}

	@Inject(method = "applyConfig", at = @At("RETURN"))
	private static void madokuCompat$clearCreeperAfterConfig(
			CreeperEntity creeper,
			CreeperMobConfig config,
			CallbackInfo ci
	) {
		MobsDifficultyAttributeBridge.reapplyDifficultyAttributes(creeper);
		MobsDifficultyAttributeBridge.reapplyCreeperExplosionPower(creeper);
		CURRENT_CREEPER.remove();
	}

	@ModifyArg(
			method = "applyConfig",
			at = @At(
					value = "INVOKE",
					target = "Lmadoku/craft/mobs/system/CreeperMobSystem;resolveFuseTicks(D)I"
			),
			index = 0
	)
	private static double madokuCompat$applyDifficultyFuseScaling(double baseFuseLengthSeconds) {
		int spawnAdjustment = resolveSpawnAdjustment(CURRENT_CREEPER.get());
		if (spawnAdjustment <= 0) {
			return baseFuseLengthSeconds;
		}
		double step = CompatConfigSystem.get().mobCompatScaling().creeperFuseLengthAdjustmentStep();
		return Math.max(
				MIN_FUSE_LENGTH_SECONDS,
				baseFuseLengthSeconds - (spawnAdjustment * step)
		);
	}

	@Inject(method = "applyExplosionOverride", at = @At("HEAD"))
	private static void madokuCompat$captureCreeperForExplosionOverride(
			CreeperEntity creeper,
			ServerWorld world,
			double x,
			double y,
			double z,
			float power,
			ExplosionSourceType sourceType,
			CallbackInfo ci
	) {
		CURRENT_CREEPER.set(creeper);
	}

	@Inject(method = "applyExplosionOverride", at = @At("RETURN"))
	private static void madokuCompat$clearCreeperAfterExplosionOverride(
			CreeperEntity creeper,
			ServerWorld world,
			double x,
			double y,
			double z,
			float power,
			ExplosionSourceType sourceType,
			CallbackInfo ci
	) {
		CURRENT_CREEPER.remove();
	}

	@Redirect(
			method = "applyExplosionOverride",
			at = @At(
					value = "INVOKE",
					target = "Lmadoku/craft/mobs/system/CreeperMobConfig;resolveExplosionDestructionChance(Lnet/minecraft/world/Difficulty;ZLmadoku/craft/mobs/system/CreeperMobConfig$CreeperVariantConfig;)Ljava/lang/Double;"
			)
	)
	private static Double madokuCompat$applyDifficultyExplosionDestructionChanceScaling(
			CreeperMobConfig config,
			Difficulty difficulty,
			boolean hardcore,
			CreeperMobConfig.CreeperVariantConfig variant
	) {
		Double chance = config.resolveExplosionDestructionChance(difficulty, hardcore, variant);
		if (chance == null) {
			return null;
		}

		int spawnAdjustment = resolveSpawnAdjustment(CURRENT_CREEPER.get());
		if (spawnAdjustment <= 0) {
			return chance;
		}
		double step = CompatConfigSystem.get().mobCompatScaling().creeperExplosionDestructionChanceAdjustmentStep();

		return MathHelper.clamp(
				chance + (spawnAdjustment * step),
				0.0d,
				1.0d
		);
	}

	@ModifyArg(
			method = "applyExplosionOverride",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/world/ServerWorld;createExplosion(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/world/World$ExplosionSourceType;)V"
			),
			index = 4
	)
	private static float madokuCompat$applyDifficultyExplosionPowerScaling(float originalPower) {
		CreeperEntity creeper = CURRENT_CREEPER.get();
		if (creeper == null) {
			return originalPower;
		}

		DifficultyScalingConfig.Snapshot snapshot = DifficultyScalingConfig.get();
		if (!snapshot.enabled()) {
			return originalPower;
		}

		int spawnAdjustment = resolveSpawnAdjustment(creeper);
		if (spawnAdjustment <= 0) {
			return originalPower;
		}

		double increment = snapshot.increments().explosionPower();
		double bonus = increment * spawnAdjustment;
		float finalPower = Math.max(0.0f, (float) (originalPower + bonus));
		MadokuCraftCompat.LOGGER.info(
				"[difficulty-scale][creeper] explosion_override_power originalPower={} increment={} spawnAdjustment={} bonus={} finalPower={} entity={}",
				originalPower,
				increment,
				spawnAdjustment,
				bonus,
				finalPower,
				madokuCompat$describeEntity(creeper));
		return finalPower;
	}

	@Inject(method = "resolveFixedPlayerExplosionDamage", at = @At("RETURN"), cancellable = true)
	private static void madokuCompat$scaleFixedPlayerExplosionDamage(
			CreeperEntity creeper,
			float explosionPower,
			CallbackInfoReturnable<Float> cir
	) {
		if (creeper == null) {
			return;
		}

		int spawnAdjustment = resolveSpawnAdjustment(creeper);
		if (spawnAdjustment <= 0) {
			return;
		}

		float currentDamage = cir.getReturnValueF();
		int scaledRadius = ((CreeperEntityAccessor) creeper).madokuDifficulty$getExplosionRadius();
		float effectivePower = Math.max(Math.max(0.0f, explosionPower), (float) scaledRadius);
		float targetDamage = Math.max(0.0f, effectivePower * 5.0f);
		float finalDamage = Math.max(currentDamage, targetDamage);
		if (finalDamage <= currentDamage) {
			return;
		}

		cir.setReturnValue(finalDamage);
		MadokuCraftCompat.LOGGER.info(
				"[difficulty-scale][creeper] fixed_player_damage baseDamage={} explosionPowerArg={} scaledRadius={} effectivePower={} spawnAdjustment={} finalDamage={} entity={}",
				currentDamage,
				explosionPower,
				scaledRadius,
				effectivePower,
				spawnAdjustment,
				finalDamage,
				madokuCompat$describeEntity(creeper));
	}

	private static int resolveSpawnAdjustment(Entity entity) {
		if (entity instanceof DifficultyScaledMob scaledMob) {
			return Math.max(0, scaledMob.madokuDifficulty$getSpawnAdjustment());
		}
		return 0;
	}

	private static String madokuCompat$describeEntity(Entity entity) {
		if (entity == null) {
			return "null";
		}
		return entity.getType().toString() + "#" + entity.getUuidAsString();
	}
}
