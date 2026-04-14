package madoku.craft.compat.mixin.client.hud;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "madoku.craft.hud.MadokuHud", remap = false)
public class OxygenHudAttributesCompatMixin {
	@Redirect(
			method = "updateOxygenState",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;getMaxAirSupply()I"
			)
	)
	private static int madokuCompat$useAttributesOxygenCap(LocalPlayer player) {
		return Math.max(1, madoku.craft.oxygen.MadokuOxygen.getMaximumOxygenTicksForEntity(player));
	}

	@Redirect(
			method = "renderOxygenHud",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;getMaxAirSupply()I"
			)
	)
	private static int madokuCompat$renderUsingAttributesOxygenCap(LocalPlayer player) {
		return Math.max(1, madoku.craft.oxygen.MadokuOxygen.getMaximumOxygenTicksForEntity(player));
	}
}
