package madoku.craft.compat.mixin.levels;

import madoku.craft.compat.integration.levels.LevelsAttributesExtraStats;
import madoku.craft.network.MadokuLevelsPayload;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "madoku.craft.levels.MadokuLevels", remap = false)
public abstract class LevelsAttributesExtraStatsCompatMixin {
	@Inject(method = "handleLevelUpRequest", at = @At("HEAD"), cancellable = true)
	private static void madokuCompat$handleExtraAttributeLevelUps(ServerPlayer player, String statId, CallbackInfo ci) {
		if (LevelsAttributesExtraStats.handleCustomStatUpgrade(player, statId)) {
			ci.cancel();
		}
	}

	@Inject(method = "applyPlayerAttributes", at = @At("TAIL"))
	private static void madokuCompat$applyExtraAttributeBonuses(ServerPlayer player, CallbackInfo ci) {
		LevelsAttributesExtraStats.applyExtraPlayerAttributes(player);
	}

	@Inject(method = "createPayload", at = @At("RETURN"), cancellable = true)
	private static void madokuCompat$appendExtraStatLevelsToPayload(
		ServerPlayer player,
		CallbackInfoReturnable<MadokuLevelsPayload> cir
	) {
		MadokuLevelsPayload payload = cir.getReturnValue();
		if (payload == null) {
			return;
		}

		cir.setReturnValue(
			new MadokuLevelsPayload(
				payload.username(),
				payload.level(),
				payload.currentXp(),
				payload.requiredXp(),
				payload.availablePoints(),
				payload.maxStatLevel(),
				LevelsAttributesExtraStats.appendCustomStatLevels(player, payload.statLevels())
			)
		);
	}
}
