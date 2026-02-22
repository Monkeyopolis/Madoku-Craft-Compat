package madoku.craft.compat.mixin.mobs;

import madoku.craft.compat.MadokuCraftCompat;
import madoku.craft.mobs.system.SkeletonMobSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityDamageCompatMixin {
	@Unique
	private static final ThreadLocal<Entity> CURRENT_TARGET = new ThreadLocal<>();
	@Unique
	private static final ThreadLocal<Float> PRE_HEALTH = new ThreadLocal<>();
	@Unique
	private static final ThreadLocal<Float> PRE_ABSORPTION = new ThreadLocal<>();
	@Unique
	private static final ThreadLocal<Float> APPLIED_DAMAGE = new ThreadLocal<>();
	@Unique
	private static final ThreadLocal<Float> REQUESTED_DAMAGE = new ThreadLocal<>();

	@Inject(method = "onEntityHit", at = @At("HEAD"))
	private void madokuCompat$captureHitContext(EntityHitResult hitResult, CallbackInfo ci) {
		PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
		if (projectile.getEntityWorld().isClient()) {
			madokuCompat$clearHitContext();
			return;
		}

		Entity target = hitResult.getEntity();
		if (!(target instanceof LivingEntity livingTarget)) {
			madokuCompat$clearHitContext();
			return;
		}

		CURRENT_TARGET.set(target);
		PRE_HEALTH.set(livingTarget.getHealth());
		PRE_ABSORPTION.set(livingTarget.getAbsorptionAmount());
		APPLIED_DAMAGE.remove();
		REQUESTED_DAMAGE.remove();
	}

	@Inject(method = "onEntityHit", at = @At("RETURN"))
	private void madokuCompat$logEffectiveHitDamage(EntityHitResult hitResult, CallbackInfo ci) {
		PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
		if (projectile.getEntityWorld().isClient()) {
			madokuCompat$clearHitContext();
			return;
		}

		Entity target = CURRENT_TARGET.get();
		Float preHealth = PRE_HEALTH.get();
		Float preAbsorption = PRE_ABSORPTION.get();
		Float appliedDamage = APPLIED_DAMAGE.get();
		Float requestedDamage = REQUESTED_DAMAGE.get();
		madokuCompat$clearHitContext();
		if (target == null || preHealth == null || preAbsorption == null || appliedDamage == null) {
			return;
		}
		if (!(target instanceof LivingEntity livingTarget)) {
			return;
		}

		float postHealth = livingTarget.getHealth();
		float postAbsorption = livingTarget.getAbsorptionAmount();
		float effectiveLoss = Math.max(0.0f,
				(preHealth + preAbsorption) - (postHealth + postAbsorption));
		MadokuCraftCompat.LOGGER.info(
				"[difficulty-scale][projectile] hit_result requestedDamage={} appliedDamage={} effectiveLoss={} targetHealthBefore={} targetHealthAfter={} absorptionBefore={} absorptionAfter={} armor={} toughness={} projectile={} owner={} target={}",
				requestedDamage,
				appliedDamage,
				effectiveLoss,
				preHealth,
				postHealth,
				preAbsorption,
				postAbsorption,
				livingTarget.getArmor(),
				livingTarget.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS),
				madokuCompat$describeEntity(projectile),
				madokuCompat$describeEntity(projectile.getOwner()),
				madokuCompat$describeEntity(livingTarget));
	}

	@ModifyArg(
			method = "onEntityHit",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/Entity;sidedDamage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
			),
			index = 1,
			require = 0
	)
	private float madokuCompat$applyDamageArgSided(float requestedDamage) {
		return madokuCompat$applyDamageArg("sided", requestedDamage);
	}

	@ModifyArg(
			method = "onEntityHit",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z"
			),
			index = 2,
			require = 0
	)
	private float madokuCompat$applyDamageArgNew(float requestedDamage) {
		return madokuCompat$applyDamageArg("new", requestedDamage);
	}

	@ModifyArg(
			method = "onEntityHit",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
			),
			index = 1,
			require = 0
	)
	private float madokuCompat$applyDamageArgOld(float requestedDamage) {
		return madokuCompat$applyDamageArg("old", requestedDamage);
	}

	@Unique
	private float madokuCompat$applyDamageArg(String apiPath, float requestedDamage) {
		PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
		if (projectile.getEntityWorld().isClient()) {
			return requestedDamage;
		}

		Float finalDamage = APPLIED_DAMAGE.get();
		Float fixedOverride = null;
		if (finalDamage == null) {
			fixedOverride = SkeletonMobSystem.consumeFixedSkeletonArrowDamage(projectile);
			finalDamage = fixedOverride != null ? fixedOverride : requestedDamage;
			APPLIED_DAMAGE.set(finalDamage);
		}
		REQUESTED_DAMAGE.set(requestedDamage);

		Entity target = CURRENT_TARGET.get();
		if (target instanceof LivingEntity livingTarget) {
			MadokuCraftCompat.LOGGER.info(
					"[difficulty-scale][projectile] damage_call api={} requestedDamage={} fixedOverride={} finalDamage={} targetHealth={} targetAbsorption={} armor={} toughness={} projectile={} owner={} target={}",
					apiPath,
					requestedDamage,
					fixedOverride,
					finalDamage,
					livingTarget.getHealth(),
					livingTarget.getAbsorptionAmount(),
					livingTarget.getArmor(),
					livingTarget.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS),
					madokuCompat$describeEntity(projectile),
					madokuCompat$describeEntity(projectile.getOwner()),
					madokuCompat$describeEntity(target));
		} else {
			MadokuCraftCompat.LOGGER.info(
					"[difficulty-scale][projectile] damage_call api={} requestedDamage={} fixedOverride={} finalDamage={} projectile={} owner={} target={}",
					apiPath,
					requestedDamage,
					fixedOverride,
					finalDamage,
					madokuCompat$describeEntity(projectile),
					madokuCompat$describeEntity(projectile.getOwner()),
					madokuCompat$describeEntity(target));
		}

		return finalDamage;
	}

	@Unique
	private static void madokuCompat$clearHitContext() {
		CURRENT_TARGET.remove();
		PRE_HEALTH.remove();
		PRE_ABSORPTION.remove();
		APPLIED_DAMAGE.remove();
		REQUESTED_DAMAGE.remove();
	}

	@Unique
	private static String madokuCompat$describeEntity(Entity entity) {
		if (entity == null) {
			return "null";
		}
		return entity.getType().toString() + "#" + entity.getUuidAsString();
	}
}
