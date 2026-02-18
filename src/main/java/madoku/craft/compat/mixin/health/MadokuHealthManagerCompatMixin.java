package madoku.craft.compat.mixin.health;

import madoku.craft.Health.system.MadokuHealthManager;
import madoku.craft.compat.integration.HungerCompat;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MadokuHealthManager.class, remap = false)
public class MadokuHealthManagerCompatMixin {
	@Inject(method = "drainHunger", at = @At("HEAD"), cancellable = true)
	private static void madokuCompat$drainHunger(ServerPlayerEntity player, int amount,
			CallbackInfo ci) {
		if (HungerCompat.drainHunger(player, amount)) {
			ci.cancel();
		}
	}

	@Inject(method = "addPendingFromFood", at = @At("HEAD"), cancellable = true)
	private void madokuCompat$blockFoodFromHealth(ServerPlayerEntity player, double amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (!HungerCompat.isHealthFoodBridgeActive() && isHealthFoodMixinCall()) {
			cir.setReturnValue(false);
		}
	}

	@Redirect(
			method = "onPlayerRespawn",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/player/HungerManager;setFoodLevel(I)V"
			)
	)
	private void madokuCompat$skipRespawnFoodLevel(HungerManager manager, int level) {
	}

	@Redirect(
			method = "onPlayerRespawn",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/player/HungerManager;setSaturationLevel(F)V"
			)
	)
	private void madokuCompat$skipRespawnSaturation(HungerManager manager, float level) {
	}

	private static boolean isHealthFoodMixinCall() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (StackTraceElement element : stack) {
			if ("madoku.craft.Health.mixin.ItemStackMixin".equals(element.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
