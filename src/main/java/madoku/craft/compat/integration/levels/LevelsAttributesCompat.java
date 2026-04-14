package madoku.craft.compat.integration.levels;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import madoku.craft.attributes.MadokuAttributes;
import madoku.craft.config.StaticJsonSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;

public final class LevelsAttributesCompat {
	private static final Logger LOGGER = LoggerFactory.getLogger(LevelsAttributesCompat.class);

	private static final String LEVELS_CONFIG_FOLDER_NAME = "madoku-craft-levels";
	private static final String LEVELS_CONFIG_FILE_NAME = "madoku-levels";
	private static final String MAX_STAT_LEVEL_KEY = "max_stat_level";
	private static final String MAX_STAT_LEVEL_ATTRIBUTES_KEY = "max_stat_level_attributes";
	private static final String PLAYER_ARMOR_PER_LEVEL_KEY = "player_armor_per_level";
	private static final String PLAYER_ARMOR_PER_LEVEL_ATTRIBUTES_KEY = "player_armor_per_level_attributes";

	private static final int DEFAULT_MAX_STAT_LEVEL_ATTRIBUTES = 10;
	private static final double DEFAULT_PLAYER_ARMOR_PER_LEVEL_ATTRIBUTES = 0.4d;

	private LevelsAttributesCompat() {
	}

	public static void applyAttributeAwareSettings() {
		MadokuAttributes.initialize();
		if (!MadokuAttributes.isEnabled()) {
			return;
		}

		Object currentSettings = getCurrentSettings();
		if (currentSettings == null) {
			return;
		}

		JsonObject source = readLevelsConfig();
		int currentMaxStatLevel = readIntField(currentSettings, "maxStatLevel", DEFAULT_MAX_STAT_LEVEL_ATTRIBUTES);
		double currentArmorPerLevel = readDoubleField(
			currentSettings,
			"playerArmorPerLevel",
			DEFAULT_PLAYER_ARMOR_PER_LEVEL_ATTRIBUTES
		);

		int attributeMaxStatLevel = getInt(
			source,
			MAX_STAT_LEVEL_ATTRIBUTES_KEY,
			getInt(source, MAX_STAT_LEVEL_KEY, currentMaxStatLevel)
		);
		double attributeArmorPerLevel = getDouble(
			source,
			PLAYER_ARMOR_PER_LEVEL_ATTRIBUTES_KEY,
			getDouble(source, PLAYER_ARMOR_PER_LEVEL_KEY, currentArmorPerLevel)
		);

		Object compatSettings = recreateSettings(currentSettings, attributeMaxStatLevel, attributeArmorPerLevel);
		if (compatSettings != null) {
			setCurrentSettings(compatSettings);
		}
	}

	private static JsonObject readLevelsConfig() {
		try {
			Path directory = StaticJsonSystem.getOrCreateGlobalSystemDirectory(LEVELS_CONFIG_FOLDER_NAME);
			Path configFile = resolveJsonFile(directory, LEVELS_CONFIG_FILE_NAME);
			return StaticJsonSystem.readJsonFile(configFile);
		} catch (IOException | RuntimeException exception) {
			LOGGER.warn("Failed to read Levels config for Attributes compat; using loaded Levels settings.", exception);
			return new JsonObject();
		}
	}

	private static Path resolveJsonFile(Path directory, String fileName) {
		String normalized = fileName == null ? "" : fileName.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Config file name must not be blank.");
		}
		if (!normalized.endsWith(".json")) {
			normalized = normalized + ".json";
		}
		return directory.resolve(normalized);
	}

	private static Object getCurrentSettings() {
		try {
			Field settingsField = Class.forName("madoku.craft.levels.MadokuLevels").getDeclaredField("settings");
			settingsField.setAccessible(true);
			return settingsField.get(null);
		} catch (ReflectiveOperationException exception) {
			LOGGER.warn("Failed to access MadokuLevels settings for Attributes compat.", exception);
			return null;
		}
	}

	private static void setCurrentSettings(Object settings) {
		try {
			Field settingsField = Class.forName("madoku.craft.levels.MadokuLevels").getDeclaredField("settings");
			settingsField.setAccessible(true);
			settingsField.set(null, settings);
		} catch (ReflectiveOperationException exception) {
			LOGGER.warn("Failed to update MadokuLevels settings for Attributes compat.", exception);
		}
	}

	private static Object recreateSettings(Object currentSettings, int maxStatLevel, double playerArmorPerLevel) {
		try {
			Class<?> settingsClass = currentSettings.getClass();
			Constructor<?> constructor = settingsClass.getDeclaredConstructor(
				double.class,
				double.class,
				int.class,
				int.class,
				double.class,
				double.class,
				double.class,
				double.class
			);
			constructor.setAccessible(true);
			return constructor.newInstance(
				readDoubleField(currentSettings, "baseXpRequirement", 5.0d),
				readDoubleField(currentSettings, "baseXpMultiplier", 0.10d),
				readIntField(currentSettings, "maxPlayerLevel", 40),
				maxStatLevel,
				readDoubleField(currentSettings, "healthPerLevel", 1.0d),
				readDoubleField(currentSettings, "playerDamagePerLevel", 0.2d),
				playerArmorPerLevel,
				readDoubleField(currentSettings, "playerMovementSpeedPerLevel", 0.001d)
			);
		} catch (ReflectiveOperationException exception) {
			LOGGER.warn("Failed to recreate MadokuLevels settings for Attributes compat.", exception);
			return null;
		}
	}

	private static int readIntField(Object target, String fieldName, int fallback) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.getInt(target);
		} catch (ReflectiveOperationException exception) {
			return fallback;
		}
	}

	private static double readDoubleField(Object target, String fieldName, double fallback) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.getDouble(target);
		} catch (ReflectiveOperationException exception) {
			return fallback;
		}
	}

	private static int getInt(JsonObject object, String memberName, int fallback) {
		if (object == null || memberName == null || memberName.isBlank()) {
			return fallback;
		}
		JsonElement element = object.get(memberName);
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			return fallback;
		}
		try {
			return element.getAsInt();
		} catch (RuntimeException exception) {
			return fallback;
		}
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
}
