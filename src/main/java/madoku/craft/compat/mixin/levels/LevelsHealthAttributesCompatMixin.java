package madoku.craft.compat.mixin.levels;

import madoku.craft.compat.access.PlayerSavedHealthAccess;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.levels.MadokuLevels", remap = false)
public abstract class LevelsHealthAttributesCompatMixin {
	@Unique
	private static final float madokuCompat$HEALTH_EPSILON = 1.0e-4f;

	@Inject(method = "handlePlayerJoin", at = @At("TAIL"))
	private static void madokuCompat$restoreSavedJoinHealth(ServerPlayer player, CallbackInfo ci) {
		if (player == null) {
			return;
		}

		if (!(player instanceof PlayerSavedHealthAccess accessor) || !accessor.madokuCompat$hasSavedHealth()) {
			return;
		}

		float savedHealth = accessor.madokuCompat$getSavedHealth();
		float currentMaxHealth = player.getMaxHealth();
		if (savedHealth <= madokuCompat$HEALTH_EPSILON || currentMaxHealth <= madokuCompat$HEALTH_EPSILON) {
			return;
		}
		player.setHealth(Math.min(currentMaxHealth, savedHealth));
	}

	@Inject(method = "handlePlayerRespawn", at = @At("TAIL"))
	private static void madokuCompat$restoreRespawnHealthForLevelBonus(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive, CallbackInfo ci) {
		if (newPlayer == null || alive) {
			return;
		}

		float maxHealth = newPlayer.getMaxHealth();
		float respawnHealth = Math.max(0.5f, maxHealth * 0.5f);
		newPlayer.setHealth(Math.min(maxHealth, respawnHealth));
	}
}
