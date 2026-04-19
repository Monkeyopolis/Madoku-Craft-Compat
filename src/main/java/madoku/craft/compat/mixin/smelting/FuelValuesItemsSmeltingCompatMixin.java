package madoku.craft.compat.mixin.smelting;

import madoku.craft.items.item.system.MadokuItem;
import madoku.craft.smelting.system.MadokuSmeltingManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FuelValues.class, priority = 1500)
public abstract class FuelValuesItemsSmeltingCompatMixin {
	@Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
	private void madokuCompat$allowConfiguredFuelWithoutOverridingVanilla(
		ItemStack stack,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (!MadokuSmeltingManager.isEnabled() || !MadokuItem.isEnabled()) {
			return;
		}

		if (MadokuItem.isConfiguredFuel(stack)) {
			cir.setReturnValue(Boolean.TRUE);
			return;
		}

		FuelValues self = (FuelValues) (Object) this;
		if (self.burnDuration(stack) > 0) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}
}
