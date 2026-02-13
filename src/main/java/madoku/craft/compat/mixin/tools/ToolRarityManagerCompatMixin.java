package madoku.craft.compat.mixin.tools;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.tools.system.ToolRarityManager")
public class ToolRarityManagerCompatMixin {
	private static final double ARMOR_STEP = 0.25d;
	private static final double TOUGHNESS_STEP = 0.25d;

	@Inject(method = "scaleArmorAttributes", at = @At("HEAD"), cancellable = true, remap = false)
	private static void madokuCompat$scaleArmorAttributesWithCustomSteps(
			ItemStack stack,
			double armorMultiplier,
			double toughnessMultiplier,
			CallbackInfo ci
	) {
		AttributeModifiersComponent current = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
		if (current == null) {
			return;
		}

		boolean changed = false;
		AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
		for (AttributeModifiersComponent.Entry entry : current.modifiers()) {
			EntityAttributeModifier modifier = entry.modifier();
			EntityAttributeModifier updatedModifier = modifier;
			if (madokuCompat$isScaledArmor(entry, modifier)) {
				double scaledValue = roundToStep(modifier.value() * armorMultiplier, ARMOR_STEP);
				updatedModifier = new EntityAttributeModifier(
						modifier.id(),
						scaledValue,
						modifier.operation()
				);
				changed = true;
			} else if (madokuCompat$isScaledArmorToughness(entry, modifier)) {
				double scaledValue = roundToStep(modifier.value() * toughnessMultiplier, TOUGHNESS_STEP);
				updatedModifier = new EntityAttributeModifier(
						modifier.id(),
						scaledValue,
						modifier.operation()
				);
				changed = true;
			}
			builder.add(entry.attribute(), updatedModifier, entry.slot(), entry.display());
		}

		if (changed) {
			stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
		}

		ci.cancel();
	}

	private static boolean madokuCompat$isScaledArmor(
			AttributeModifiersComponent.Entry entry,
			EntityAttributeModifier modifier
	) {
		return madokuCompat$isAddValueModifier(modifier)
				&& madokuCompat$isAttribute(entry.attribute(), EntityAttributes.ARMOR);
	}

	private static boolean madokuCompat$isScaledArmorToughness(
			AttributeModifiersComponent.Entry entry,
			EntityAttributeModifier modifier
	) {
		return madokuCompat$isAddValueModifier(modifier)
				&& madokuCompat$isAttribute(entry.attribute(), EntityAttributes.ARMOR_TOUGHNESS);
	}

	private static boolean madokuCompat$isAddValueModifier(EntityAttributeModifier modifier) {
		return modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE;
	}

	private static boolean madokuCompat$isAttribute(
			RegistryEntry<EntityAttribute> entry,
			RegistryEntry<EntityAttribute> target
	) {
		return entry.value() == target.value();
	}

	private static double roundToStep(double value, double step) {
		return Math.round(value / step) * step;
	}
}
