package madoku.craft.compat.mixin.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;
import java.util.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;

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
					target = "Lnet/fabricmc/fabric/api/client/rendering/v1/hud/HudStatusBarHeightRegistry;getHeight(Lnet/minecraft/util/Identifier;)I"
			)
	)
	private static int madokuCompat$stableArmorHudHeight(Identifier identifier) {
		int baseHeight = HudStatusBarHeightRegistry.getHeight(identifier);
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) {
			return baseHeight;
		}

		float totalHealth = Math.max(20.0f, player.getMaxHealth() + player.getAbsorptionAmount());
		int rows = (int) Math.ceil(totalHealth / 20.0f);
		int extraRows = Math.max(0, rows - 1);
		int adjustedHeight = baseHeight - (extraRows * 10);
		return Math.max(0, adjustedHeight);
	}

	@Redirect(
			method = "renderArmor",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
			)
	)
	private static void madokuCompat$renderAdjustedArmorText(DrawContext context, TextRenderer renderer,
			String text, int x, int y, int color) {
		context.drawTextWithShadow(renderer, madokuCompat$formatArmorText(text), x, y, color);
	}

	private static String madokuCompat$formatArmorText(String fallback) {
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

		double shown = Math.max(0.0d, exactArmorValue);
		String number;
		if (Math.abs(shown - Math.rint(shown)) < 1.0e-9d) {
			number = Integer.toString((int) Math.rint(shown));
		} else {
			number = String.format(Locale.ROOT, "%.2f", shown);
			while (number.endsWith("0")) {
				number = number.substring(0, number.length() - 1);
			}
			if (number.endsWith(".")) {
				number = number.substring(0, number.length() - 1);
			}
		}

		return prefix + number;
	}
}
