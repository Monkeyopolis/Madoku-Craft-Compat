package madoku.craft.mixin.utility;

import madoku.craft.java.items.ItemsAPIManager;
import madoku.craft.java.items.ItemsCategoriesAPIManager;
import madoku.craft.java.utility.smelting.SmeltingAPIManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class ConfiguredFuelValuesMixin {
	@Inject(method = "getBurnDuration", at = @At("RETURN"), cancellable = true)
	private void madokuCraft$applyConfiguredFuelDuration(
		ServerLevel level,
		ItemStack stack,
		CallbackInfoReturnable<Integer> cir
	) {
		if (!SmeltingAPIManager.isEnabled() || !ItemsAPIManager.isEnabled()) {
			return;
		}

		if (!ItemsAPIManager.isConfiguredFuel(stack) || cir.getReturnValue() > 0) {
			return;
		}

		int configuredDuration = ItemsCategoriesAPIManager.adjustFuelTicks(stack, cir.getReturnValue());
		if (configuredDuration > 0) {
			cir.setReturnValue(configuredDuration);
		}
	}
}

