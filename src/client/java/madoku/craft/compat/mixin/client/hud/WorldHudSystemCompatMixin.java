package madoku.craft.compat.mixin.client.hud;

import madoku.craft.compat.integration.hud.WorldHudDifficultyResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.hud.WorldHudSystem", remap = false)
public class WorldHudSystemCompatMixin {
	private static String cachedDifficultyText = "Difficulty: 0";
	private static boolean cachedDifficultyVisible;

	@Inject(method = "lambda$init$0", at = @At("TAIL"))
	private static void madokuCompat$cacheWorldDifficulty(MinecraftClient client, ClientPlayerEntity player,
			CallbackInfo ci) {
		int totalDifficulty = WorldHudDifficultyResolver.resolveTotalDifficulty(client, player);
		cachedDifficultyVisible = totalDifficulty > 0;
		if (!cachedDifficultyVisible) {
			return;
		}
		cachedDifficultyText = "Difficulty: " + totalDifficulty;
	}

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lorg/joml/Matrix3x2fStack;popMatrix()Lorg/joml/Matrix3x2fStack;",
					shift = At.Shift.BEFORE
			)
	)
	private static void madokuCompat$renderDifficultyLine(DrawContext context, RenderTickCounter tickCounter,
			CallbackInfo ci) {
		if (!cachedDifficultyVisible) {
			return;
		}

		TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
		if (renderer == null) {
			return;
		}
		context.drawTextWithShadow(renderer, cachedDifficultyText, Math.round(5.0f), Math.round(42.5f), -1);
	}
}
