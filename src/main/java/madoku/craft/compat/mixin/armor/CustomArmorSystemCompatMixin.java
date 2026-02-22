package madoku.craft.compat.mixin.armor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "madoku.craft.armor.system.CustomArmorSystem")
public class CustomArmorSystemCompatMixin {
	private static final double ARMOR_POINT_STEP = 0.25d;
	private static final double ARMOR_POINT_DAMAGE_REDUCTION = 0.125d;
	private static final double TOUGHNESS_POINT_STEP = 0.25d;
	private static final double TOUGHNESS_PERCENT_REDUCTION = 0.0025d;

	@Inject(method = "applyCustomArmor", at = @At("HEAD"), cancellable = true, remap = false)
	private static void madokuCraftCompat$applyAdjustedArmorFormula(LivingEntity entity, DamageSource damageSource,
			float amount,
			CallbackInfoReturnable<Float> cir) {
		if (!damageSource.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
			entity.damageArmor(damageSource, amount);
		}

		double armorPoints = roundToStep(entity.getAttributeValue(EntityAttributes.ARMOR), ARMOR_POINT_STEP);
		double armorToughnessPoints = roundToStep(
				entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS),
				TOUGHNESS_POINT_STEP
		);

		double flatReduction = (armorPoints / ARMOR_POINT_STEP) * ARMOR_POINT_DAMAGE_REDUCTION;
		double reduced = amount - flatReduction;
		if (reduced < 0.0d) {
			reduced = 0.0d;
		}

		double toughnessMultiplier = 1.0d
				- ((armorToughnessPoints / TOUGHNESS_POINT_STEP) * TOUGHNESS_PERCENT_REDUCTION);
		if (toughnessMultiplier < 0.0d) {
			toughnessMultiplier = 0.0d;
		}

		cir.setReturnValue((float) (reduced * toughnessMultiplier));
	}

	private static double roundToStep(double value, double step) {
		return Math.round(value / step) * step;
	}
}
