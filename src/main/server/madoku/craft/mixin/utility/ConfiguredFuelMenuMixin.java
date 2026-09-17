package madoku.craft.mixin.utility;

import madoku.craft.java.items.ItemsAPIManager;
import madoku.craft.java.utility.smelting.SmeltingAPIManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows configured Items fuels into the 26.3 furnace fuel slot. */
@Mixin(AbstractFurnaceMenu.class)
public abstract class ConfiguredFuelMenuMixin {
	@Inject(method = "isFuel(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
	private void madokuCraft$allowConfiguredFuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue() || !SmeltingAPIManager.isEnabled() || !ItemsAPIManager.isEnabled()) {
			return;
		}

		if (ItemsAPIManager.isConfiguredFuel(stack)) {
			cir.setReturnValue(true);
		}
	}
}
