package madoku.craft.compat.mixin.attributes;

import madoku.craft.compat.integration.hud.HungerHudSync;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "madoku.craft.hunger.MadokuHunger", remap = false)
public abstract class HungerHudSyncAttributesCompatMixin {
	@Redirect(
		method = "syncHudState",
		at = @At(
			value = "INVOKE",
			target = "Lmadoku/craft/network/HungerStateSync;send(Lnet/minecraft/server/level/ServerPlayer;III)Z"
		)
	)
	private static boolean madokuCompat$sendHudStateViaCompat(ServerPlayer player, int current, int pending, int max) {
		return HungerHudSync.send(player, current, pending, max);
	}
}
