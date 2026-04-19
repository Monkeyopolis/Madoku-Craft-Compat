package madoku.craft.compat.mixin.client.hud;

import madoku.craft.compat.integration.HungerHudClientState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEatAttributesCompatMixin {
	@Inject(method = "canEat(Z)Z", at = @At("HEAD"), cancellable = true)
	private void madokuCompat$gateClientCanEat(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof LocalPlayer && !HungerHudClientState.canConsume(ignoreHunger)) {
			cir.setReturnValue(false);
		}
	}
}
