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
	private void madokuCompat$combineItemsAndSmeltingFuel(
		FuelValues fuelValues,
		ItemStack stack,
		CallbackInfoReturnable<Integer> cir
	) {
		int combinedFuelTicks = resolveVanillaFuelTicks(fuelValues, stack);
		if (MadokuItem.isEnabled()) {
			combinedFuelTicks = MadokuItem.adjustFuelTicks(stack, combinedFuelTicks);
		}
		if (MadokuSmeltingManager.isEnabled()) {
			AbstractFurnaceBlockEntity self = (AbstractFurnaceBlockEntity) (Object) this;
			combinedFuelTicks = MadokuSmeltingManager.getAdjustedFuelTicks(self, stack, combinedFuelTicks);
		}

		int currentFuelTicks = cir.getReturnValue();
		if (combinedFuelTicks != currentFuelTicks) {
			cir.setReturnValue(combinedFuelTicks);
		}
	}

	private static int resolveVanillaFuelTicks(FuelValues fuelValues, ItemStack stack) {
		if (fuelValues == null || stack == null || stack.isEmpty()) {
			return 0;
		}
		return fuelValues.burnDuration(stack);
	}
}
