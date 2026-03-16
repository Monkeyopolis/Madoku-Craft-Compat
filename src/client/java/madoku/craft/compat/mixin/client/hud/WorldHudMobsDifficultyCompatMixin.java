package madoku.craft.compat.mixin.client.hud;

import madoku.craft.compat.integration.hud.MobsHudDifficultyClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.hud.MadokuHud", remap = false)
public class WorldHudMobsDifficultyCompatMixin {
	@Shadow
	private static int lineOffset(Minecraft client, int lines) {
		throw new AssertionError();
	}

	@Shadow
	private static void drawScaledString(GuiGraphics context, Minecraft client, String text, int x, int y, int color) {
		throw new AssertionError();
	}

	@Inject(
			method = "renderWorldHud",
			at = @At(
					value = "INVOKE",
					target = "Lmadoku/craft/hud/MadokuHud;drawScaledString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/Minecraft;Ljava/lang/String;III)V",
					ordinal = 2,
					shift = At.Shift.AFTER
			)
	)
	private static void madokuCompat$renderMobsDifficultyLine(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		int fourthLineY = lineOffset(client, 3);
		drawScaledString(
				context,
				client,
				"Difficulty: " + MobsHudDifficultyClientState.displayText(),
				4,
				fourthLineY,
				0xFFFFFFFF
		);
	}
}
