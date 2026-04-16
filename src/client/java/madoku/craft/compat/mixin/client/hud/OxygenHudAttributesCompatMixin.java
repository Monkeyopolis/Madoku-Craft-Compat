package madoku.craft.compat.mixin.client.hud;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import madoku.craft.oxygen.MadokuOxygen;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.hud.MadokuHud", remap = false)
public abstract class OxygenHudAttributesCompatMixin {
	@Shadow
	private static RenderPipeline OXYGEN_PIPELINE;

	@Shadow
	private static int OXYGEN_SIZE;

	@Shadow
	private static int OXYGEN_TEXT_SPACING;

	@Shadow
	private static int OXYGEN_X_OFFSET_RIGHT;

	@Shadow
	private static int OXYGEN_RIGHT_EDGE;

	@Shadow
	private static int SECOND_LEFT_VANILLA_AIR_SLOT_INDEX;

	@Shadow
	private static int OXYGEN_POP_TICKS_PER_SECOND_LOSS;

	@Shadow
	private static float OXYGEN_TEXT_SCALE;

	@Shadow
	private static int COLOR;

	@Shadow
	private static int cachedAirSupply;

	@Shadow
	private static int cachedMaxAirSupply;

	@Shadow
	private static int cachedOxygenPoints;

	@Shadow
	private static int previousDisplayedOxygenSeconds;

	@Shadow
	private static int oxygenPopTicksRemaining;

	@Shadow
	private static long lastOxygenStateUpdateTick;

	@Shadow
	private static Identifier selectOxygenTexture(int oxygenPoints) {
		throw new AssertionError();
	}

	@Inject(
		method = "renderOxygenHud",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;getInstance()Lnet/minecraft/client/Minecraft;"
		),
		cancellable = true
	)
	private static void madokuCompat$renderUsingAttributesOxygenState(
		GuiGraphicsExtractor context,
		DeltaTracker tickCounter,
		HudElement oldElement,
		CallbackInfo ci
	) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null) {
			return;
		}
		if (player.isSpectator()) {
			return;
		}

		madokuCompat$updateOxygenState(player, level.getGameTime());

		boolean shouldRender = cachedAirSupply < cachedMaxAirSupply || player.isEyeInFluid(FluidTags.WATER);
		if (!shouldRender) {
			ci.cancel();
			return;
		}

		String oxygenText = madokuCompat$buildOxygenTextFromSeconds(cachedAirSupply, cachedMaxAirSupply);
		int oxygenX = madokuCompat$computeOxygenX(context, client, oxygenText, cachedMaxAirSupply);
		int oxygenY = context.guiHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.AIR_BAR);
		context.blitSprite(OXYGEN_PIPELINE, selectOxygenTexture(cachedOxygenPoints), oxygenX, oxygenY, OXYGEN_SIZE, OXYGEN_SIZE);

		int textX = oxygenX + OXYGEN_SIZE + OXYGEN_TEXT_SPACING;
		int textY = oxygenY + 1;
		context.pose().pushMatrix();
		context.pose().scale(OXYGEN_TEXT_SCALE, OXYGEN_TEXT_SCALE);
		context.text(
			client.font,
			oxygenText,
			Math.round(textX / OXYGEN_TEXT_SCALE),
			Math.round(textY / OXYGEN_TEXT_SCALE),
			COLOR,
			true
		);
		context.pose().popMatrix();
		ci.cancel();
	}

	private static void madokuCompat$updateOxygenState(LocalPlayer player, long gameTime) {
		if (lastOxygenStateUpdateTick == gameTime) {
			return;
		}

		lastOxygenStateUpdateTick = gameTime;
		cachedMaxAirSupply = Math.max(1, MadokuOxygen.getMaximumOxygenTicksForEntity(player));
		cachedAirSupply = madokuCompat$fromDisplayedAirSupply(player.getAirSupply(), cachedMaxAirSupply);
		cachedOxygenPoints = madokuCompat$toOxygenPoints(cachedAirSupply, cachedMaxAirSupply);

		int displayedSeconds = madokuCompat$toDisplaySeconds(cachedAirSupply);
		if (previousDisplayedOxygenSeconds >= 0
			&& displayedSeconds < previousDisplayedOxygenSeconds
			&& displayedSeconds > 0) {
			oxygenPopTicksRemaining = OXYGEN_POP_TICKS_PER_SECOND_LOSS;
			player.playSound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.75F, 1.0F);
		} else if (oxygenPopTicksRemaining > 0) {
			oxygenPopTicksRemaining--;
		}

		previousDisplayedOxygenSeconds = displayedSeconds;
	}

	private static int madokuCompat$computeOxygenX(
		GuiGraphicsExtractor context,
		Minecraft client,
		String oxygenText,
		int maxAirSupply
	) {
		int oxygenRightEdge = context.guiWidth() / 2 + OXYGEN_RIGHT_EDGE;
		String baselineText = madokuCompat$buildOxygenBaselineText(maxAirSupply);
		int baselineWidth = madokuCompat$getScaledTextWidth(client, baselineText, OXYGEN_TEXT_SCALE);
		int currentWidth = madokuCompat$getScaledTextWidth(client, oxygenText, OXYGEN_TEXT_SCALE);
		int baseX = oxygenRightEdge - OXYGEN_SIZE - (SECOND_LEFT_VANILLA_AIR_SLOT_INDEX * 8) + OXYGEN_X_OFFSET_RIGHT;
		return baseX + (baselineWidth - currentWidth);
	}

	private static int madokuCompat$getScaledTextWidth(Minecraft client, String text, float scale) {
		return Math.round(client.font.width(text) * scale);
	}

	private static int madokuCompat$toOxygenPoints(int airSupply, int maxAirSupply) {
		double ratio = (Math.max(0, airSupply) * 10.0d) / Math.max(1, maxAirSupply);
		return Math.max(0, Math.min(10, (int) Math.ceil(ratio)));
	}

	private static int madokuCompat$toDisplaySeconds(int airTicks) {
		return (int) Math.ceil(Math.max(0, airTicks) / 20.0d);
	}

	private static String madokuCompat$buildOxygenTextFromSeconds(int currentAirSupply, int maxAirSupply) {
		int normalizedMax = Math.max(1, maxAirSupply);
		int normalizedCurrent = Math.max(0, Math.min(normalizedMax, currentAirSupply));
		int maxSeconds = Math.max(1, madokuCompat$toDisplaySeconds(normalizedMax));
		int currentSeconds = Math.max(0, Math.min(maxSeconds, madokuCompat$toDisplaySeconds(normalizedCurrent)));
		return "Oxygen: " + currentSeconds + "/" + maxSeconds;
	}

	private static String madokuCompat$buildOxygenBaselineText(int maxAirSupply) {
		int maxSeconds = Math.max(1, madokuCompat$toDisplaySeconds(Math.max(1, maxAirSupply)));
		return "Oxygen: " + maxSeconds + "/" + maxSeconds;
	}

	private static int madokuCompat$fromDisplayedAirSupply(int displayedAirSupply, int maxAirSupply) {
		int normalizedDisplayed = madokuCompat$clampInt(displayedAirSupply, 0, 300);
		int normalizedMax = Math.max(1, maxAirSupply);
		if (normalizedDisplayed <= 0) {
			return 0;
		}
		if (normalizedDisplayed >= 300) {
			return normalizedMax;
		}

		double ratio = normalizedDisplayed / 300.0d;
		return madokuCompat$clampInt(Math.max(1, (int) Math.round(ratio * normalizedMax)), 1, normalizedMax);
	}

	private static int madokuCompat$clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
