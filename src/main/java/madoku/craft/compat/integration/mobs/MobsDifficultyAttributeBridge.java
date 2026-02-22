package madoku.craft.compat.integration.mobs;

import madoku.craft.compat.MadokuCraftCompat;
import madoku.craft.difficulty.config.DifficultyScalingConfig;
import madoku.craft.difficulty.mixin.CreeperEntityAccessor;
import madoku.craft.difficulty.system.DifficultyScaledMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class MobsDifficultyAttributeBridge {
	private static final double EPSILON = 1.0E-9d;
	private static final Identifier HEALTH_BONUS_ID = Identifier.of("madoku-craft-compat", "difficulty_health_bonus");
	private static final Identifier MOVEMENT_SPEED_BONUS_ID = Identifier
			.of("madoku-craft-compat", "difficulty_movement_speed_bonus");
	private static final Identifier ARMOR_BONUS_ID = Identifier.of("madoku-craft-compat", "difficulty_armor_bonus");
	private static final Identifier DAMAGE_BONUS_ID = Identifier.of("madoku-craft-compat", "difficulty_damage_bonus");
	private static final Identifier KNOCKBACK_BONUS_ID = Identifier
			.of("madoku-craft-compat", "difficulty_knockback_resistance_bonus");

	private MobsDifficultyAttributeBridge() {
	}

	public static void reapplyDifficultyAttributes(LivingEntity entity) {
		if (!(entity instanceof DifficultyScaledMob scaledMob)) {
			return;
		}

		DifficultyScalingConfig.Snapshot snapshot = DifficultyScalingConfig.get();
		int adjustment = Math.max(0, scaledMob.madokuDifficulty$getSpawnAdjustment());
		if (!snapshot.enabled() || adjustment <= 0) {
			clearModifier(entity, EntityAttributes.MAX_HEALTH, HEALTH_BONUS_ID);
			clearModifier(entity, EntityAttributes.MOVEMENT_SPEED, MOVEMENT_SPEED_BONUS_ID);
			clearModifier(entity, EntityAttributes.ARMOR, ARMOR_BONUS_ID);
			clearModifier(entity, EntityAttributes.ATTACK_DAMAGE, DAMAGE_BONUS_ID);
			clearModifier(entity, EntityAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK_BONUS_ID);
			return;
		}

		DifficultyScalingConfig.StatIncrements increments = snapshot.increments();
		boolean healthChanged = applyAddValueModifier(
				entity,
				EntityAttributes.MAX_HEALTH,
				HEALTH_BONUS_ID,
				increments.health() * adjustment,
				false);
		applyAddValueModifier(
				entity,
				EntityAttributes.MOVEMENT_SPEED,
				MOVEMENT_SPEED_BONUS_ID,
				increments.movementSpeed() * adjustment,
				false);
		applyAddValueModifier(
				entity,
				EntityAttributes.ARMOR,
				ARMOR_BONUS_ID,
				increments.armor() * adjustment,
				false);
		applyAddValueModifier(
				entity,
				EntityAttributes.ATTACK_DAMAGE,
				DAMAGE_BONUS_ID,
				increments.damage() * adjustment,
				false);
		applyAddValueModifier(
				entity,
				EntityAttributes.KNOCKBACK_RESISTANCE,
				KNOCKBACK_BONUS_ID,
				increments.knockbackResistance() * adjustment,
				true);

		if (healthChanged) {
			entity.setHealth((float) entity.getAttributeValue(EntityAttributes.MAX_HEALTH));
		}
	}

	public static void reapplyCreeperExplosionPower(CreeperEntity creeper) {
		if (!(creeper instanceof DifficultyScaledMob scaledMob)) {
			MadokuCraftCompat.LOGGER.info(
					"[difficulty-scale][creeper] explosion_power skipped: entity is not DifficultyScaledMob entity={}",
					madokuCompat$describeEntity(creeper));
			return;
		}

		DifficultyScalingConfig.Snapshot snapshot = DifficultyScalingConfig.get();
		if (!snapshot.enabled()) {
			MadokuCraftCompat.LOGGER.info(
					"[difficulty-scale][creeper] explosion_power skipped: difficulty scaling disabled entity={}",
					madokuCompat$describeEntity(creeper));
			return;
		}

		int adjustment = Math.max(0, scaledMob.madokuDifficulty$getSpawnAdjustment());
		if (adjustment <= 0) {
			MadokuCraftCompat.LOGGER.info(
					"[difficulty-scale][creeper] explosion_power skipped: spawnAdjustment={} entity={}",
					adjustment,
					madokuCompat$describeEntity(creeper));
			return;
		}

		double increment = snapshot.increments().explosionPower();
		double bonus = increment * adjustment;
		if (Math.abs(bonus) < EPSILON) {
			MadokuCraftCompat.LOGGER.info(
					"[difficulty-scale][creeper] explosion_power skipped: increment={} spawnAdjustment={} computedBonus={} entity={}",
					increment,
					adjustment,
					bonus,
					madokuCompat$describeEntity(creeper));
			return;
		}

		CreeperEntityAccessor accessor = (CreeperEntityAccessor) creeper;
		int currentRadius = accessor.madokuDifficulty$getExplosionRadius();
		int scaledRadius = Math.max(1, (int) Math.round(currentRadius + bonus));
		accessor.madokuDifficulty$setExplosionRadius(scaledRadius);
		MadokuCraftCompat.LOGGER.info(
				"[difficulty-scale][creeper] explosion_power currentRadius={} increment={} spawnAdjustment={} bonus={} finalRadius={} entity={}",
				currentRadius,
				increment,
				adjustment,
				bonus,
				scaledRadius,
				madokuCompat$describeEntity(creeper));
	}

	private static boolean applyAddValueModifier(LivingEntity entity, RegistryEntry<EntityAttribute> attribute,
			Identifier modifierId, double requestedBonus, boolean clampToUnitRange) {
		EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
		if (instance == null) {
			return false;
		}

		EntityAttributeModifier existing = instance.getModifier(modifierId);
		double previousBonus = existing == null ? 0.0d : existing.value();
		if (existing != null) {
			instance.removeModifier(modifierId);
		}

		double appliedBonus = requestedBonus;
		if (clampToUnitRange) {
			double baseValue = instance.getBaseValue();
			appliedBonus = MathHelper.clamp(baseValue + requestedBonus, 0.0d, 1.0d) - baseValue;
		}

		if (Math.abs(appliedBonus) < EPSILON) {
			return Math.abs(previousBonus) >= EPSILON;
		}

		instance.addPersistentModifier(new EntityAttributeModifier(
				modifierId,
				appliedBonus,
				EntityAttributeModifier.Operation.ADD_VALUE));
		return Math.abs(appliedBonus - previousBonus) >= EPSILON;
	}

	private static void clearModifier(LivingEntity entity, RegistryEntry<EntityAttribute> attribute,
			Identifier modifierId) {
		EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
		if (instance != null && instance.getModifier(modifierId) != null) {
			instance.removeModifier(modifierId);
		}
	}

	private static String madokuCompat$describeEntity(LivingEntity entity) {
		if (entity == null) {
			return "null";
		}
		return entity.getType().toString() + "#" + entity.getUuidAsString();
	}
}
