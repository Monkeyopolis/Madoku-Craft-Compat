package madoku.craft.compat.mixin.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import java.util.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.hud.HealthHudSystem", remap = false)
public class HealthHudSystemCompatMixin {
	@Shadow
	private static float cachedHealth;

	@Shadow
	private static float cachedMaxHealth;

	@Inject(method = "lambda$init$0", at = @At("TAIL"))
	private static void madokuCompat$stabilizeHealthCache(MinecraftClient client, ClientPlayerEntity player,
			CallbackInfo ci) {
		if (cachedHealth < 0.0f) {
			cachedHealth = 0.0f;
		}
		if (cachedMaxHealth < 1.0f) {
			cachedMaxHealth = 1.0f;
		}
		if (cachedHealth > cachedMaxHealth) {
			cachedMaxHealth = cachedHealth;
		}
	}

	@Redirect(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
			)
	)
	private static void madokuCompat$renderAdjustedHealthText(DrawContext context, TextRenderer renderer,
			String text, int x, int y, int color) {
		context.drawTextWithShadow(renderer, madokuCompat$formatHealthText(text), x, y, color);
	}

	private static String madokuCompat$formatHealthText(String fallback) {
		String prefix = "";
		if (fallback != null && !fallback.isEmpty()) {
			int firstDigit = -1;
			for (int i = 0; i < fallback.length(); i++) {
				if (Character.isDigit(fallback.charAt(i))) {
					firstDigit = i;
					break;
				}
			}
			if (firstDigit > 0) {
				prefix = fallback.substring(0, firstDigit);
			}
		}

		double health = Math.max(0.0d, cachedHealth);
		double max = Math.max(1.0d, cachedMaxHealth);
		if (health > max) {
			max = health;
		}
		return prefix + madokuCompat$formatDecimal(health) + "/" + madokuCompat$formatDecimal(max);
	}

	private static String madokuCompat$formatDecimal(double value) {
		if (Math.abs(value - Math.rint(value)) < 1.0e-9d) {
			return Integer.toString((int) Math.rint(value));
		}
		String formatted = String.format(Locale.ROOT, "%.2f", value);
		while (formatted.endsWith("0")) {
			formatted = formatted.substring(0, formatted.length() - 1);
		}
		if (formatted.endsWith(".")) {
			formatted = formatted.substring(0, formatted.length() - 1);
		}
		return formatted;
	}
}
