package madoku.craft.compat.mixin.levels;

import madoku.craft.compat.integration.levels.LevelsAttributesExtraStats;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "madoku.craft.hunger.MadokuHunger", remap = false)
public abstract class HungerLevelsAttributesCompatMixin {
	@Inject(method = "resolveMaximumHungerPoints", at = @At("RETURN"), cancellable = true)
	private static void madokuCompat$addLevelsHungerBonus(ServerPlayer player, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(Math.max(1, cir.getReturnValueI() + LevelsAttributesExtraStats.getPlayerHungerBonusPoints(player)));
	}
}
