package madoku.craft.compat.mixin.mobs;

import madoku.craft.compat.integration.mobs.AttributesMobsLuckCompat;
import madoku.craft.mobs.difficulty.system.MadokuDifficulty;
import madoku.craft.mobs.mob.system.MadokuMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MadokuMob.class, priority = 500)
public abstract class MadokuMobAttributesLuckCompatMixin {
	@Redirect(
		method = "applyCustomSkeletonRangedAttack",
		at = @At(
			value = "INVOKE",
			target = "Lmadoku/craft/mobs/difficulty/system/MadokuDifficulty;resolveMobAttackAccuracyScaling(Lnet/minecraft/world/entity/Mob;D)D"
		)
	)
	private static double madokuCompat$reduceSkeletonRangedAccuracy(
		Mob mob,
		double accuracy,
		AbstractSkeleton skeleton,
		LivingEntity target,
		float pullProgress
	) {
		double scaledAccuracy = MadokuDifficulty.resolveMobAttackAccuracyScaling(mob, accuracy);
		return AttributesMobsLuckCompat.reduceHostileRangedAccuracyForTarget(target, scaledAccuracy);
	}

	@Redirect(
		method = "applyPillagerProjectileAccuracyOverride",
		at = @At(
			value = "INVOKE",
			target = "Lmadoku/craft/mobs/difficulty/system/MadokuDifficulty;resolveMobAttackAccuracyScaling(Lnet/minecraft/world/entity/Mob;D)D"
		)
	)
	private static double madokuCompat$reduceHostileProjectileAccuracy(
		Mob mob,
		double accuracy,
		Projectile projectile,
		LivingEntity shooter,
		LivingEntity target,
		double velocityX,
		double velocityY,
		double velocityZ,
		float speed,
		float divergence
	) {
		double scaledAccuracy = MadokuDifficulty.resolveMobAttackAccuracyScaling(mob, accuracy);
		return AttributesMobsLuckCompat.reduceHostileRangedAccuracyForTarget(target, scaledAccuracy);
	}

	@Redirect(
		method = "applyCreeperExplosionOverride",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/Mth;clamp(DDD)D"
		)
	)
	private static double madokuCompat$reduceCreeperGriefChance(
		double value,
		double min,
		double max,
		Creeper creeper,
		ServerLevel level,
		Entity source,
		double x,
		double y,
		double z,
		float vanillaPower,
		Level.ExplosionInteraction vanillaInteraction
	) {
		double chance = Mth.clamp(value, min, max);
		return AttributesMobsLuckCompat.reduceCreeperGriefChanceForTarget(creeper == null ? null : creeper.getTarget(), chance);
	}
}
