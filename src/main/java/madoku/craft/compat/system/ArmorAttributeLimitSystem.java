package madoku.craft.compat.system;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import madoku.craft.API.system.MadokuJSONSystem;
import madoku.craft.compat.MadokuCraftCompat;
import madoku.craft.compat.mixin.attribute.ClampedEntityAttributeAccessor;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;

public final class ArmorAttributeLimitSystem {
	private static final String FEATURE_ID = "madoku_craft_compat";
	private static final String JSON_FOLDER_ID = "Compat";
	private static final String KEY_ARMOR_MAX_VALUE = "armorMaxValue";
	private static final String KEY_ARMOR_TOUGHNESS_MAX_VALUE = "armorToughnessMaxValue";
	private static final double DEFAULT_MAX_VALUE = 100.0d;

	private ArmorAttributeLimitSystem() {
	}

	public static void init() {
		JsonObject defaults = new JsonObject();
		defaults.addProperty(KEY_ARMOR_MAX_VALUE, DEFAULT_MAX_VALUE);
		defaults.addProperty(KEY_ARMOR_TOUGHNESS_MAX_VALUE, DEFAULT_MAX_VALUE);

		MadokuJSONSystem.ManagedJSON config = MadokuJSONSystem.load(JSON_FOLDER_ID, FEATURE_ID, defaults);
		SettingsLoadResult load = readSettings(config.getRoot());
		if (load.changed()) {
			config.save();
		}

		applyMaxLimit(EntityAttributes.ARMOR, load.settings().armorMaxValue());
		applyMaxLimit(EntityAttributes.ARMOR_TOUGHNESS, load.settings().armorToughnessMaxValue());
		MadokuCraftCompat.LOGGER.info(
				"Madoku Craft Compat: Applied armor attribute caps (armor={}, toughness={})",
				load.settings().armorMaxValue(),
				load.settings().armorToughnessMaxValue()
		);
	}

	private static SettingsLoadResult readSettings(JsonObject root) {
		boolean changed = false;

		double armorMax = sanitizeMax(readDouble(root, KEY_ARMOR_MAX_VALUE, DEFAULT_MAX_VALUE));
		changed |= setDouble(root, KEY_ARMOR_MAX_VALUE, armorMax);

		double toughnessMax = sanitizeMax(readDouble(root, KEY_ARMOR_TOUGHNESS_MAX_VALUE, DEFAULT_MAX_VALUE));
		changed |= setDouble(root, KEY_ARMOR_TOUGHNESS_MAX_VALUE, toughnessMax);

		return new SettingsLoadResult(new ArmorLimitSettings(armorMax, toughnessMax), changed);
	}

	private static double sanitizeMax(double value) {
		if (!Double.isFinite(value) || value <= 0.0d) {
			return DEFAULT_MAX_VALUE;
		}
		return value;
	}

	private static void applyMaxLimit(RegistryEntry<EntityAttribute> entry, double maxValue) {
		EntityAttribute attribute = entry.value();
		String attributeId = entry.getKey().map(key -> key.getValue().toString()).orElse("unknown_attribute");
		if (!(attribute instanceof ClampedEntityAttribute clamped)) {
			MadokuCraftCompat.LOGGER.warn(
					"Madoku Craft Compat: Attribute {} is not clamped, cannot apply max value {}",
					attributeId,
					maxValue
			);
			return;
		}

		((ClampedEntityAttributeAccessor) clamped).madokuCompat$setMaxValue(maxValue);
	}

	private static double readDouble(JsonObject root, String key, double fallback) {
		JsonElement element = root.get(key);
		if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
			double value = primitive.getAsDouble();
			if (Double.isFinite(value)) {
				return value;
			}
		}
		return fallback;
	}

	private static boolean setDouble(JsonObject root, String key, double value) {
		JsonElement element = root.get(key);
		if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
			if (Double.compare(primitive.getAsDouble(), value) == 0) {
				return false;
			}
		}
		root.addProperty(key, value);
		return true;
	}

	private record ArmorLimitSettings(double armorMaxValue, double armorToughnessMaxValue) {
	}

	private record SettingsLoadResult(ArmorLimitSettings settings, boolean changed) {
	}
}
