package madoku.craft.compat.system;

import madoku.craft.compat.MadokuCraftCompat;
import madoku.craft.compat.mixin.attribute.ClampedEntityAttributeAccessor;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;

public final class ArmorAttributeLimitSystem {
	private ArmorAttributeLimitSystem() {
	}

	public static void init() {
		CompatConfigSystem.ArmorSettings settings = CompatConfigSystem.get().armor();
		applyMaxLimit(EntityAttributes.ARMOR, settings.armorMaxValue());
		applyMaxLimit(EntityAttributes.ARMOR_TOUGHNESS, settings.armorToughnessMaxValue());
		MadokuCraftCompat.LOGGER.info(
				"Madoku Craft Compat: Applied armor attribute caps (armor={}, toughness={})",
				settings.armorMaxValue(),
				settings.armorToughnessMaxValue()
		);
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

}
