package madoku.craft.compat.mixin.smelting;

import madoku.craft.items.item.system.MadokuItem;
import madoku.craft.smelting.system.MadokuSmeltingManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 500)
public abstract class FurnaceFuelItemsSmeltingCompatMixin {
	@Inject(method = "getBurnDuration", at = @At("RETURN"), cancellable = true)
	private void madokuCompat$applyItemsThenSmeltingFuel(
		FuelValues fuelValues,
		ItemStack stack,
		CallbackInfoReturnable<Integer> cir
	) {
		if (!MadokuItem.isEnabled() || !MadokuSmeltingManager.isEnabled()) {
			return;
		}

		AbstractFurnaceBlockEntity self = (AbstractFurnaceBlockEntity) (Object) this;
		int vanillaFuelTicks = fuelValues.burnDuration(stack);
		int itemsFuelTicks = MadokuItem.adjustFuelTicks(stack, vanillaFuelTicks);
		int adjustedFuelTicks = MadokuSmeltingManager.getAdjustedFuelTicks(self, stack, itemsFuelTicks);
		if (adjustedFuelTicks != cir.getReturnValue()) {
			cir.setReturnValue(adjustedFuelTicks);
		}
	}
}
