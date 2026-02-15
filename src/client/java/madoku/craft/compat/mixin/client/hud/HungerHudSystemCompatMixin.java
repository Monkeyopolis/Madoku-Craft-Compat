package madoku.craft.compat.mixin.client.hud;

import madoku.craft.compat.integration.HungerHudClientState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.hud.HungerHudSystem", remap = false)
public class HungerHudSystemCompatMixin {
	@Shadow
	private static int cachedFoodLevel;

	@Inject(method = "lambda$init$0", at = @At("TAIL"))
	private static void madokuCompat$syncHudFoodLevel(MinecraftClient client, ClientPlayerEntity player,
			CallbackInfo ci) {
		int scaledFood = HungerHudClientState.toHudFoodLevel();
		if (scaledFood >= 0) {
			cachedFoodLevel = scaledFood;
		}
	}

	@Redirect(
			method = "renderFood",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
			)
	)
	private static void madokuCompat$renderAdjustedHungerText(DrawContext context, TextRenderer renderer,
			String text, int x, int y, int color) {
		context.drawTextWithShadow(renderer, HungerHudClientState.toHudText(text), x, y, color);
	}
}
