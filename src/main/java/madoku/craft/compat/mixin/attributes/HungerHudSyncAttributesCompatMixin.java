package madoku.craft.compat.mixin.attributes;

import madoku.craft.compat.integration.hud.HungerHudSync;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(targets = "madoku.craft.hunger.MadokuHunger", remap = false)
public abstract class HungerHudSyncAttributesCompatMixin {
	@Shadow
	private static Map<UUID, Object> PLAYER_STATES;

	@Inject(method = "applyFoodState", at = @At("TAIL"))
	private static void madokuCompat$sendHudStateViaCompat(ServerPlayer player, int current, int max, CallbackInfo ci) {
		Object state = player == null ? null : PLAYER_STATES.get(player.getUUID());
		int pending = state instanceof HungerPlayerStateAccessor accessor ? Math.max(0, accessor.madokuCompat$getPendingHunger()) : 0;
		HungerHudSync.send(player, current, pending, max);
	}
}
