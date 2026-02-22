package madoku.craft.compat.mixin.mobs;

import madoku.craft.compat.integration.mobs.DifficultySpawnAdjustmentResolver;
import madoku.craft.compat.integration.mobs.MobsDifficultyAttributeBridge;
import madoku.craft.compat.system.CompatConfigSystem;
import madoku.craft.difficulty.system.DifficultyScaledMob;
import madoku.craft.mobs.system.SpiderMobConfig;
import madoku.craft.mobs.system.SpiderMobSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpiderMobSystem.class, remap = false)
public class SpiderMobSystemDifficultyCompatMixin {
	private static final double MIN_SCALE = 0.05d;
	private static final ThreadLocal<SpiderEntity> CURRENT_SPIDER = new ThreadLocal<>();

	@Inject(method = "applyConfig", at = @At("HEAD"))
	private static void madokuCompat$captureSpiderForScaling(
			SpiderEntity spider,
			SpiderMobConfig config,
			Difficulty difficulty,
			CallbackInfo ci
	) {
		ensureSpawnAdjustment(spider);
		CURRENT_SPIDER.set(spider);
	}

	@Inject(method = "applyConfig", at = @At("RETURN"))
	private static void madokuCompat$clearSpiderAfterScaling(
			SpiderEntity spider,
			SpiderMobConfig config,
			Difficulty difficulty,
			CallbackInfo ci
	) {
		MobsDifficultyAttributeBridge.reapplyDifficultyAttributes(spider);
		CURRENT_SPIDER.remove();
	}

	@Inject(method = "resolveScaleByDifficulty", at = @At("RETURN"), cancellable = true)
	private static void madokuCompat$applyDifficultyScaleScaling(
			double baseScale,
			double difficultyStep,
			Difficulty difficulty,
			boolean hardcore,
			CallbackInfoReturnable<Double> cir
	) {
		double scaledSize = cir.getReturnValueD();

		int spawnAdjustment = resolveSpawnAdjustment(CURRENT_SPIDER.get());
		if (spawnAdjustment <= 0) {
			return;
		}
		double step = CompatConfigSystem.get().mobCompatScaling().spiderScaleAdjustmentStep();

		cir.setReturnValue(Math.max(MIN_SCALE, scaledSize + (spawnAdjustment * step)));
	}

	private static int resolveSpawnAdjustment(Entity entity) {
		if (entity instanceof DifficultyScaledMob scaledMob) {
			int adjustment = Math.max(0, scaledMob.madokuDifficulty$getSpawnAdjustment());
			if (adjustment > 0) {
				return adjustment;
			}
			ensureSpawnAdjustment(entity);
			return Math.max(0, scaledMob.madokuDifficulty$getSpawnAdjustment());
		}
		return 0;
	}

	private static void ensureSpawnAdjustment(Entity entity) {
		if (!(entity instanceof DifficultyScaledMob scaledMob)) {
			return;
		}
		if (scaledMob.madokuDifficulty$getSpawnAdjustment() > 0) {
			return;
		}
		if (!(entity.getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		int computed = DifficultySpawnAdjustmentResolver.resolve(serverWorld, entity.getBlockPos());
		if (computed > 0) {
			scaledMob.madokuDifficulty$setSpawnAdjustment(computed);
		}
	}
}
