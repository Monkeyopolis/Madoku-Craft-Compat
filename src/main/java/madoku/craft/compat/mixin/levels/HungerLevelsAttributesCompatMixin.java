package madoku.craft.compat.mixin.levels;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import madoku.craft.compat.integration.levels.LevelsAttributesExtraStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.hunger.MadokuHunger", remap = false)
public abstract class HungerLevelsAttributesCompatMixin {
	@Inject(method = "handlePlayerJoin", at = @At("HEAD"))
	private static void madokuCompat$restorePersistedBonusHunger(ServerPlayer player, CallbackInfo ci) {
		LevelsAttributesExtraStats.restoreStartupPersistedHungerState(player);
	}

	@ModifyExpressionValue(
		method = "applyFoodState",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I"
		)
	)
	private static int madokuCompat$expandMaxForFoodApplication(int configuredMaximum, ServerPlayer player, int currentHunger) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	@ModifyExpressionValue(
		method = "hasEnoughFoodToDoExhaustiveManoeuvres",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I"
		)
	)
	private static int madokuCompat$expandMaxForSprintGate(int configuredMaximum, Player player) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	@ModifyExpressionValue(
		method = "canConsumeFood",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I"
		)
	)
	private static int madokuCompat$expandMaxForConsumeCheck(int configuredMaximum, ServerPlayer player, boolean ignoreHunger) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	@ModifyExpressionValue(
		method = "onFoodConsumed",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I"
		)
	)
	private static int madokuCompat$expandMaxForFoodUse(int configuredMaximum, ServerPlayer player, int nutrition) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	@ModifyExpressionValue(
		method = "drainHunger",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I"
		)
	)
	private static int madokuCompat$expandMaxForDrain(int configuredMaximum, ServerPlayer player, int amount) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	@ModifyExpressionValue(
		method = "applySaturationEffectTick",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I"
		)
	)
	private static int madokuCompat$expandMaxForSaturation(int configuredMaximum, ServerPlayer player, int amplifier) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	@ModifyExpressionValue(
		method = "handlePlayerJoin",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I",
			ordinal = 0
		)
	)
	private static int madokuCompat$expandMaxForJoinInit(int configuredMaximum, ServerPlayer player) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	@ModifyExpressionValue(
		method = "handlePlayerJoin",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I",
			ordinal = 1
		)
	)
	private static int madokuCompat$expandMaxForJoinHudSync(int configuredMaximum, ServerPlayer player) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	@ModifyExpressionValue(
		method = "handlePlayerRespawn",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I"
		)
	)
	private static int madokuCompat$expandMaxForRespawn(int configuredMaximum, ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		return expandMaximumHungerPoints(configuredMaximum, newPlayer);
	}

	@ModifyExpressionValue(
		method = "handleBlockBreak",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I"
		)
	)
	private static int madokuCompat$expandMaxForBlockBreak(int configuredMaximum, Player player) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	@ModifyExpressionValue(
		method = "processPlayer",
		at = @At(
			value = "FIELD",
			target = "Lmadoku/craft/hunger/MadokuHunger$Settings;maximumHungerPoints:I"
		)
	)
	private static int madokuCompat$expandMaxForTick(int configuredMaximum, ServerPlayer player, long gameplayTick, long currentAbsoluteDayTime) {
		return expandMaximumHungerPoints(configuredMaximum, player);
	}

	private static int expandMaximumHungerPoints(int configuredMaximum, Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return Math.max(1, configuredMaximum);
		}
		return expandMaximumHungerPoints(configuredMaximum, serverPlayer);
	}

	private static int expandMaximumHungerPoints(int configuredMaximum, ServerPlayer player) {
		return Math.max(1, configuredMaximum + Math.max(0, LevelsAttributesExtraStats.getPlayerHungerBonusPoints(player)));
	}
}
