package madoku.craft.compat.mixin.attributes;

import madoku.craft.compat.integration.hud.HungerHudSync;
import madoku.craft.compat.integration.levels.LevelsAttributesExtraStats;
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
	private static void madokuCompat$sendHudStateViaCompat(ServerPlayer player, int current, CallbackInfo ci) {
		if (player == null) {
			return;
		}

		Object state = PLAYER_STATES.get(player.getUUID());
		if (!(state instanceof HungerPlayerStateAccessor accessor)) {
			return;
		}

		int max = Math.max(
			1,
			madoku.craft.hunger.MadokuHunger.getConfiguredMaximumHungerPoints()
				+ Math.max(0, LevelsAttributesExtraStats.getPlayerHungerBonusPoints(player))
		);
		current = Math.max(0, current);
		int pending = Math.max(0, accessor.madokuCompat$getPendingHunger());
		HungerHudSync.send(player, current, pending, max);
	}
}
