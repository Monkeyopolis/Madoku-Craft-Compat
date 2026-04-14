package madoku.craft.compat.integration.mobs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import madoku.craft.attributes.MadokuAttributes;
import madoku.craft.config.StaticJsonSystem;
import madoku.craft.luck.MadokuLuck;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class AttributesMobsLuckCompat {
	private static final Logger LOGGER = LoggerFactory.getLogger(AttributesMobsLuckCompat.class);

	private static final String LUCK_CONFIG_DIRECTORY_NAME = "madoku-luck";
	private static final String LUCK_CONFIG_FILE_NAME = "madoku-luck";
	private static final String BASE_LUCK_KEY = "base_luck";
	private static final String CREEPER_GRIEF_REDUCTION_MULTIPLIER_KEY = "creeper_grief_reduction_multiplier";
	private static final String RANGED_ACCURACY_REDUCTION_MULTIPLIER_KEY = "ranged_accuracy_reduction_multiplier";

	private static final double DEFAULT_BASE_LUCK = 0.05d;
	private static final double DEFAULT_CREEPER_GRIEF_REDUCTION_MULTIPLIER = 0.5d;
	private static final double DEFAULT_RANGED_ACCURACY_REDUCTION_MULTIPLIER = 0.5d;

	private static volatile Settings settings = Settings.defaults();
	private static volatile boolean loaded;

	private AttributesMobsLuckCompat() {
	}

	public static void ensureLuckMobsConfigKeys() {
		try {
			Path configFile = MadokuAttributes.prepareSystemConfigFile(LUCK_CONFIG_DIRECTORY_NAME, LUCK_CONFIG_FILE_NAME);
			JsonObject source = StaticJsonSystem.readJsonFile(configFile);
			JsonObject merged = source == null ? new JsonObject() : source.deepCopy();
			merged.addProperty(
				CREEPER_GRIEF_REDUCTION_MULTIPLIER_KEY,
				getDouble(source, CREEPER_GRIEF_REDUCTION_MULTIPLIER_KEY, DEFAULT_CREEPER_GRIEF_REDUCTION_MULTIPLIER)
			);
			merged.addProperty(
				RANGED_ACCURACY_REDUCTION_MULTIPLIER_KEY,
				getDouble(source, RANGED_ACCURACY_REDUCTION_MULTIPLIER_KEY, DEFAULT_RANGED_ACCURACY_REDUCTION_MULTIPLIER)
			);
			StaticJsonSystem.writeManagedFile(configFile, merged, merged);
			settings = Settings.fromJson(merged);
			loaded = true;
		} catch (IOException | RuntimeException exception) {
			settings = Settings.defaults();
			loaded = true;
			LOGGER.warn("Failed to extend MadokuLuck config for Mobs compat; using defaults.", exception);
		}
	}

	public static double reduceHostileRangedAccuracyForTarget(LivingEntity target, double accuracy) {
		return reduceChanceByTargetPlayerLuck(target, accuracy, currentSettings().rangedAccuracyReductionMultiplier());
	}

	public static double reduceCreeperGriefChanceForTarget(LivingEntity target, double chance) {
		return reduceChanceByTargetPlayerLuck(target, chance, currentSettings().creeperGriefReductionMultiplier());
	}

	private static Settings currentSettings() {
		if (!loaded) {
			reloadSettings();
		}
		return settings;
	}

	private static synchronized void reloadSettings() {
		if (loaded) {
			return;
		}
		try {
			Path configFile = MadokuAttributes.prepareSystemConfigFile(LUCK_CONFIG_DIRECTORY_NAME, LUCK_CONFIG_FILE_NAME);
			JsonObject source = StaticJsonSystem.readJsonFile(configFile);
			settings = Settings.fromJson(source);
		} catch (IOException | RuntimeException exception) {
			settings = Settings.defaults();
			LOGGER.warn("Failed to read MadokuLuck config for Mobs compat; using defaults.", exception);
		}
		loaded = true;
	}

	private static double reduceChanceByTargetPlayerLuck(LivingEntity target, double baseValue, double reductionMultiplier) {
		double clampedBaseValue = Mth.clamp(baseValue, 0.0d, 1.0d);
		if (!MadokuAttributes.isEnabled() || !MadokuLuck.isEnabled() || !(target instanceof ServerPlayer player)) {
			return clampedBaseValue;
		}
		double sanitizedReductionMultiplier = Math.max(0.0d, reductionMultiplier);
		if (sanitizedReductionMultiplier <= 0.0d) {
			return clampedBaseValue;
		}

		double luckChance = resolveLuckProcChance(player, currentSettings().baseLuck());
		double reduction = luckChance * sanitizedReductionMultiplier;
		return Mth.clamp(clampedBaseValue - reduction, 0.0d, 1.0d);
	}

	private static double resolveLuckProcChance(ServerPlayer player, double fallbackBaseLuck) {
		return Mth.clamp(resolveLuckValue(player, fallbackBaseLuck), 0.0d, 1.0d);
	}

	private static double resolveLuckValue(ServerPlayer player, double fallbackBaseLuck) {
		AttributeInstance luckAttribute = player == null ? null : player.getAttribute(Attributes.LUCK);
		double luckValue = luckAttribute == null ? fallbackBaseLuck : luckAttribute.getValue();
		return Double.isFinite(luckValue) ? luckValue : fallbackBaseLuck;
	}

	private static double getDouble(JsonObject object, String memberName, double fallback) {
		if (object == null || memberName == null || memberName.isBlank()) {
			return fallback;
		}
		JsonElement element = object.get(memberName);
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			return fallback;
		}
		try {
			return element.getAsDouble();
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private record Settings(
		double baseLuck,
		double creeperGriefReductionMultiplier,
		double rangedAccuracyReductionMultiplier
	) {
		private static Settings defaults() {
			return new Settings(
				DEFAULT_BASE_LUCK,
				DEFAULT_CREEPER_GRIEF_REDUCTION_MULTIPLIER,
				DEFAULT_RANGED_ACCURACY_REDUCTION_MULTIPLIER
			);
		}

		private static Settings fromJson(JsonObject source) {
			Settings defaults = defaults();
			return new Settings(
				Mth.clamp(getDouble(source, BASE_LUCK_KEY, defaults.baseLuck), 0.0d, 1024.0d),
				Mth.clamp(
					getDouble(source, CREEPER_GRIEF_REDUCTION_MULTIPLIER_KEY, defaults.creeperGriefReductionMultiplier),
					0.0d,
					1.0d
				),
				Mth.clamp(
					getDouble(source, RANGED_ACCURACY_REDUCTION_MULTIPLIER_KEY, defaults.rangedAccuracyReductionMultiplier),
					0.0d,
					1.0d
				)
			);
		}
	}
}
