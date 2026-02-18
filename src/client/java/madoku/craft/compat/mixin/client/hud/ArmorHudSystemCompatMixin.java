package madoku.craft.compat.mixin.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import java.util.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.hud.ArmorHudSystem", remap = false)
public class ArmorHudSystemCompatMixin {
	@Shadow
	private static int cachedArmor;

	@Shadow
	private static int cachedArmorPieces;

	private static double exactArmorValue;

	@Inject(method = "lambda$init$0", at = @At("TAIL"))
	private static void madokuCompat$syncArmorFromAttribute(MinecraftClient client, ClientPlayerEntity player,
			CallbackInfo ci) {
		double armorValue = Math.max(0.0d, player.getAttributeValue(EntityAttributes.ARMOR));
		exactArmorValue = armorValue;
		int roundedArmor = (int) Math.round(armorValue);
		if (roundedArmor > cachedArmor) {
			cachedArmor = roundedArmor;
		}

		if (armorValue <= 0.0d) {
			return;
		}
		int derivedPieces = (int) Math.round(Math.min(armorValue, 20.0d) / 20.0d * 4.0d);
		if (derivedPieces < 1) {
			derivedPieces = 1;
		}
		if (derivedPieces > 4) {
			derivedPieces = 4;
		}
		if (derivedPieces > cachedArmorPieces) {
			cachedArmorPieces = derivedPieces;
		}
	}

	@Redirect(
			method = "renderArmor",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
			)
	)
	private static void madokuCompat$renderQuarterArmorText(DrawContext context, TextRenderer renderer,
			String text, int x, int y, int color) {
		context.drawTextWithShadow(renderer, madokuCompat$formatQuarterArmorText(text), x, y, color);
	}

	private static String madokuCompat$formatQuarterArmorText(String fallback) {
		double shown = Math.max(0.0d, exactArmorValue);
		double roundedToQuarter = Math.round(shown * 4.0d) / 4.0d;
		String formatted = madokuCompat$trimNumber(roundedToQuarter);
		if (fallback == null || fallback.isEmpty()) {
			return formatted;
		}

		int start = -1;
		for (int i = 0; i < fallback.length(); i++) {
			char current = fallback.charAt(i);
			if (Character.isDigit(current) || current == '-') {
				start = i;
				break;
			}
		}
		if (start < 0) {
			return formatted;
		}

		int end = start;
		while (end < fallback.length()) {
			char current = fallback.charAt(end);
			if (!Character.isDigit(current) && current != '.' && current != '-') {
				break;
			}
			end++;
		}

		return fallback.substring(0, start) + formatted + fallback.substring(end);
	}

	private static String madokuCompat$trimNumber(double value) {
		String text = String.format(Locale.ROOT, "%.2f", value);
		while (text.endsWith("0")) {
			text = text.substring(0, text.length() - 1);
		}
		if (text.endsWith(".")) {
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}
}
