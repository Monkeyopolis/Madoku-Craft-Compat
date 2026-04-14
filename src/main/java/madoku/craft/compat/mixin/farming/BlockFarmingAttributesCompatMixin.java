package madoku.craft.compat.mixin.farming;

import madoku.craft.compat.integration.farming.FarmingAttributesLuckCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Block.class, priority = 500)
public abstract class BlockFarmingAttributesCompatMixin {
	@ModifyVariable(
		method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
		at = @At("HEAD"),
		argsOnly = true,
		index = 2
	)
	private static ItemStack madokuCompat$scaleManagedCropBlockDrop(ItemStack stack, Level level, BlockPos pos) {
		return FarmingAttributesLuckCompat.applyManagedCropBlockDrop(level, pos, stack);
	}
}
