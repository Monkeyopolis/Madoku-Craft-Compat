package madoku.craft.compat.mixin.smelting;

import madoku.craft.items.item.system.MadokuItem;
import madoku.craft.smelting.system.MadokuSmeltingManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 500)
public abstract class FurnaceFuelItemsSmeltingCompatMixin {
	@Redirect(
		method = "getBurnDuration",
		at = @At(
			value = "INVOKE",
			target = "Lmadoku/craft/items/item/system/MadokuItem;adjustFuelTicks(Lnet/minecraft/world/item/ItemStack;I)I"
		)
	)
	private int madokuCompat$combineItemsAndSmeltingFuel(ItemStack stack, int originalFuelTicks) {
		int itemsFuelTicks = MadokuItem.adjustFuelTicks(stack, originalFuelTicks);
		if (!MadokuSmeltingManager.isEnabled() || stack == null || stack.isEmpty()) {
			return itemsFuelTicks;
		}

		AbstractFurnaceBlockEntity self = (AbstractFurnaceBlockEntity) (Object) this;
		return MadokuSmeltingManager.getAdjustedFuelTicks(self, stack, itemsFuelTicks);
	}

	@Redirect(
		method = "getBurnDuration",
		at = @At(
			value = "INVOKE",
			target = "Lmadoku/craft/smelting/system/MadokuSmeltingManager;getAdjustedFuelTicks(Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;Lnet/minecraft/world/item/ItemStack;I)I"
		)
	)
	private int madokuCompat$skipLegacySmeltingFuelAdjustment(
		AbstractFurnaceBlockEntity furnace,
		ItemStack stack,
		int originalFuelTicks
	) {
		if (!MadokuItem.isEnabled()) {
			return MadokuSmeltingManager.getAdjustedFuelTicks(furnace, stack, originalFuelTicks);
		}
		return originalFuelTicks;
	}
}
