package madoku.craft.compat.mixin.attributes;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import madoku.craft.compat.integration.farming.FarmingAttributesLuckCompat;
import madoku.craft.luck.MadokuLuck;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MadokuLuck.class, priority = 500)
public abstract class MadokuLuckFarmingCompatMixin {
	@Inject(method = "applyGeneratedLoot", at = @At("HEAD"), cancellable = true)
	private static void madokuCompat$applyManagedCropLuckDrops(
		LootContext lootContext,
		ObjectArrayList<ItemStack> stacks,
		CallbackInfo ci
	) {
		if (FarmingAttributesLuckCompat.applyManagedCropLootDrops(lootContext, stacks)) {
			ci.cancel();
		}
	}
}
