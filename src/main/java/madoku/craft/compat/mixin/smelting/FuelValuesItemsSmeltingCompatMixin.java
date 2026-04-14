package madoku.craft.compat.mixin.smelting;

import madoku.craft.items.item.system.MadokuItem;
import madoku.craft.smelting.system.MadokuSmeltingManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FuelValues.class, priority = 500)
public abstract class FuelValuesItemsSmeltingCompatMixin {
	@Redirect(
		method = "isFuel",
		at = @At(
			value = "INVOKE",
			target = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;setReturnValue(Ljava/lang/Object;)V"
		)
	)
	private void madokuCompat$allowConfiguredFuelWithoutOverridingVanilla(
		CallbackInfoReturnable<Boolean> cir,
		Object value,
		ItemStack stack
	) {
		if (!MadokuSmeltingManager.isEnabled() || !MadokuItem.isEnabled()) {
			cir.setReturnValue((Boolean) value);
			return;
		}

		if (Boolean.TRUE.equals(value)) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}
}
