package madoku.craft.compat.mixin.client.hud;

import madoku.craft.compat.integration.HungerHudClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.hud.HungerHudSystem", remap = false)
public class HungerHudSystemCompatMixin {
	@Shadow
	private static int cachedFoodLevel;

	@Shadow
	private static int cachedMaxFoodLevel;

	@Inject(method = "lambda$init$0", at = @At("TAIL"))
	private static void madokuCompat$syncHudFoodLevel(MinecraftClient client, ClientPlayerEntity player,
			CallbackInfo ci) {
		int syncedFood = HungerHudClientState.toHudCurrentPoints();
		int syncedMax = HungerHudClientState.toHudMaxPoints();
		if (syncedFood >= 0 && syncedMax > 0) {
			cachedMaxFoodLevel = syncedMax;
			cachedFoodLevel = Math.min(syncedFood, syncedMax);
		}
	}
}
