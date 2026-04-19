package madoku.craft.compat.integration.mobs;

import com.google.gson.JsonObject;
import madoku.craft.mobs.difficulty.system.MadokuDifficultyConfig;
import madoku.craft.mobs.mob.system.MadokuMobConfig;

import java.util.Locale;
import java.util.Map;

public final class PetsMobsHagCompat {
	public static final String HAG_FILE_KEY = "hag";
	public static final String HAG_MOB_ID = "madoku-craft-pets:hag";

	private PetsMobsHagCompat() {
	}

	public static boolean isHagFileKey(String fileKey) {
		String normalized = fileKey == null ? "" : fileKey.trim().toLowerCase(Locale.ROOT);
		return HAG_FILE_KEY.equals(normalized) || HAG_MOB_ID.equals(normalized);
	}

	public static void addHagMobDefaults(Map<String, JsonObject> defaults) {
		if (defaults != null) {
			defaults.put(HAG_FILE_KEY, buildHagMobDefaults());
		}
	}

	public static JsonObject buildHagMobDefaults() {
		JsonObject root = new JsonObject();
		root.addProperty(MadokuMobConfig.FIELD_HEALTH, 40.0d);
		root.addProperty(MadokuMobConfig.FIELD_ARMOR, 1.0d);
		root.addProperty(MadokuMobConfig.FIELD_MOVEMENT_SPEED, 0.25d);
		root.addProperty(MadokuMobConfig.FIELD_KNOCKBACK_RESISTANCE, 0.2d);
		root.addProperty(MadokuMobConfig.FIELD_EXPERIENCE_DROP, 11);
		root.addProperty(MadokuMobConfig.FIELD_ENABLED, true);
		return root;
	}

	public static void addHagDifficultyScalingDefaults(
		Map<String, JsonObject> defaults,
		double health,
		double movementSpeed,
		double armor,
		double damage,
		double knockbackResistance,
		double experienceDrop
	) {
		if (defaults != null) {
			defaults.put(
				HAG_FILE_KEY,
				MadokuDifficultyConfig.buildMobScalingDefaults(
					HAG_MOB_ID,
					health,
					movementSpeed,
					armor,
					damage,
					knockbackResistance,
					experienceDrop,
					null
				)
			);
		}
	}

	public static JsonObject buildDynamicHagDifficultyScalingDefaults() {
		return MadokuDifficultyConfig.buildMobScalingDefaults(HAG_MOB_ID);
	}
}
