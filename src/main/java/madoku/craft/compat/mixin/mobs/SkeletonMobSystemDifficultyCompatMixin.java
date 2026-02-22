package madoku.craft.compat.mixin.mobs;

import madoku.craft.compat.MadokuCraftCompat;
import madoku.craft.compat.integration.mobs.MobsDifficultyAttributeBridge;
import madoku.craft.compat.system.CompatConfigSystem;
import madoku.craft.difficulty.system.DifficultyScaledMob;
import madoku.craft.mobs.system.SkeletonMobConfig;
import madoku.craft.mobs.system.SkeletonMobSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SkeletonMobSystem.class, remap = false)
public class SkeletonMobSystemDifficultyCompatMixin {
	private static final ThreadLocal<AbstractSkeletonEntity> CURRENT_SKELETON = new ThreadLocal<>();

	@Inject(method = "applyCustomRangedShot", at = @At("HEAD"))
	private static void madokuCompat$captureSkeletonForRangedShot(
			AbstractSkeletonEntity skeleton,
			LivingEntity target,
			float pullProgress,
			CallbackInfoReturnable<Boolean> cir
	) {
		CURRENT_SKELETON.set(skeleton);
	}

	@Inject(method = "applyCustomRangedShot", at = @At("RETURN"))
	private static void madokuCompat$clearSkeletonAfterRangedShot(
			AbstractSkeletonEntity skeleton,
			LivingEntity target,
			float pullProgress,
			CallbackInfoReturnable<Boolean> cir
	) {
		CURRENT_SKELETON.remove();
	}

	@Inject(method = "resolveScaledAttackAccuracy", at = @At("RETURN"), cancellable = true)
	private static void madokuCompat$applyDifficultyAttackAccuracyScaling(
			double baseAttackAccuracy,
			Difficulty difficulty,
			boolean hardcore,
			CallbackInfoReturnable<Double> cir
	) {
		double scaledAccuracy = cir.getReturnValueD();

		AbstractSkeletonEntity skeleton = CURRENT_SKELETON.get();
		int spawnAdjustment = resolveSpawnAdjustment(skeleton);
		if (spawnAdjustment <= 0) {
			MadokuCraftCompat.LOGGER.info(
					"[difficulty-scale][skeleton] attack_accuracy baseInput={} resolvedByDifficulty={} difficulty={} hardcore={} spawnAdjustment={} entity={}",
					baseAttackAccuracy,
					scaledAccuracy,
					difficulty,
					hardcore,
					spawnAdjustment,
					madokuCompat$describeEntity(skeleton));
			return;
		}

		double step = CompatConfigSystem.get().mobCompatScaling().skeletonAttackAccuracyAdjustmentStep();
		double finalAccuracy = MathHelper.clamp(
				scaledAccuracy + (spawnAdjustment * step),
				0.0d,
				1.0d
		);
		cir.setReturnValue(finalAccuracy);
		MadokuCraftCompat.LOGGER.info(
				"[difficulty-scale][skeleton] attack_accuracy baseInput={} resolvedByDifficulty={} step={} spawnAdjustment={} final={} difficulty={} hardcore={} entity={}",
				baseAttackAccuracy,
				scaledAccuracy,
				step,
				spawnAdjustment,
				finalAccuracy,
				difficulty,
				hardcore,
				madokuCompat$describeEntity(skeleton));
	}

	@Inject(method = "resolveScaledRangedDamage", at = @At("RETURN"), cancellable = true)
	private static void madokuCompat$applyDifficultyRangedDamageScaling(
			double baseRangedDamage,
			Difficulty difficulty,
			boolean hardcore,
			CallbackInfoReturnable<Double> cir
	) {
		double scaledRangedDamage = cir.getReturnValueD();

		AbstractSkeletonEntity skeleton = CURRENT_SKELETON.get();
		int spawnAdjustment = resolveSpawnAdjustment(skeleton);
		if (spawnAdjustment <= 0) {
			MadokuCraftCompat.LOGGER.info(
					"[difficulty-scale][skeleton] ranged_attack baseInput={} resolvedByDifficulty={} difficulty={} hardcore={} spawnAdjustment={} entity={}",
					baseRangedDamage,
					scaledRangedDamage,
					difficulty,
					hardcore,
					spawnAdjustment,
					madokuCompat$describeEntity(skeleton));
			return;
		}

		double step = CompatConfigSystem.get().mobCompatScaling().skeletonRangedAttackAdjustmentStep();
		double finalRangedAttack = Math.max(
				0.0d,
				scaledRangedDamage + (spawnAdjustment * step)
		);
		cir.setReturnValue(finalRangedAttack);
		MadokuCraftCompat.LOGGER.info(
				"[difficulty-scale][skeleton] ranged_attack baseInput={} resolvedByDifficulty={} step={} spawnAdjustment={} final={} difficulty={} hardcore={} entity={}",
				baseRangedDamage,
				scaledRangedDamage,
				step,
				spawnAdjustment,
				finalRangedAttack,
				difficulty,
				hardcore,
				madokuCompat$describeEntity(skeleton));
	}

	@Inject(method = "resolveRangedAttackIntervalTicks", at = @At("RETURN"), cancellable = true)
	private static void madokuCompat$applyDifficultyAttackIntervalScaling(
			AbstractSkeletonEntity skeleton,
			CallbackInfoReturnable<Integer> cir
	) {
		int originalIntervalTicks = cir.getReturnValueI();
		if (originalIntervalTicks <= 0) {
			return;
		}

		int spawnAdjustment = resolveSpawnAdjustment(skeleton);
		if (spawnAdjustment <= 0) {
			MadokuCraftCompat.LOGGER.info(
					"[difficulty-scale][skeleton] attack_interval originalTicks={} spawnAdjustment={} entity={}",
					originalIntervalTicks,
					spawnAdjustment,
					madokuCompat$describeEntity(skeleton));
			return;
		}

		double step = CompatConfigSystem.get().mobCompatScaling().skeletonAttackIntervalAdjustmentStep();
		double reducedIntervalTicks = originalIntervalTicks - (spawnAdjustment * step);
		int finalIntervalTicks = Math.max(1, (int) Math.round(reducedIntervalTicks));
		cir.setReturnValue(finalIntervalTicks);
		MadokuCraftCompat.LOGGER.info(
				"[difficulty-scale][skeleton] attack_interval originalTicks={} step={} spawnAdjustment={} reducedTicks={} finalTicks={} entity={}",
				originalIntervalTicks,
				step,
				spawnAdjustment,
				reducedIntervalTicks,
				finalIntervalTicks,
				madokuCompat$describeEntity(skeleton));
	}

	@Inject(method = "applyConfig", at = @At("RETURN"))
	private static void madokuCompat$reapplyDifficultyBaseStats(
			LivingEntity entity,
			SkeletonMobConfig config,
			CallbackInfo ci
	) {
		MobsDifficultyAttributeBridge.reapplyDifficultyAttributes(entity);
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
