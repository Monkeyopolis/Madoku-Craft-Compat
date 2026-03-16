package madoku.craft.compat.mixin.client.hud;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import madoku.craft.compat.integration.HungerHudClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "madoku.craft.hud.MadokuHud", remap = false)
public class HungerHudAttributesCompatMixin {
	@Shadow
	private static RenderPipeline FOOD_PIPELINE;

	@Shadow
	private static int FOOD_SIZE;

	@Shadow
	private static int FOOD_TEXT_SPACING;

	@Shadow
	private static float HUNGER_TEXT_SCALE;

	@Shadow
	private static int COLOR;

	@Shadow
	private static Identifier selectFoodContainerTexture(boolean hasHungerEffect) {
		throw new AssertionError();
	}

	@Shadow
	private static Identifier selectFoodFillTexture(boolean hasHungerEffect, float hungerPercent) {
		throw new AssertionError();
	}

	@Shadow
	private static int computeFoodX(GuiGraphics context, Minecraft client, String hungerText, int configuredMax) {
		throw new AssertionError();
	}

	@Inject(
			method = "renderHungerHud",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;getFoodData()Lnet/minecraft/world/food/FoodData;"
			),
			cancellable = true
	)
	private static void madokuCompat$renderSyncedHunger(
			GuiGraphics context,
			DeltaTracker tickCounter,
			net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement oldElement,
			CallbackInfo ci) {
		int syncedCurrent = HungerHudClientState.current();
		int syncedMax = HungerHudClientState.max();
		if (syncedCurrent < 0 || syncedMax <= 0) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}

		int currentHunger = clampInt(syncedCurrent, 0, Math.max(1, syncedMax));
		int pendingHunger = Math.max(0, HungerHudClientState.pending());
		int maxHunger = Math.max(1, syncedMax);
		float hungerPercent = currentHunger / (float) maxHunger;
		long displayedHunger = (long) currentHunger + (long) pendingHunger;

		String hungerText = "Hunger: " + displayedHunger + "/" + maxHunger;
		int foodX = computeFoodX(context, client, hungerText, maxHunger);
		int foodY = context.guiHeight() - net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry
				.getHeight(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.FOOD_BAR);
		boolean hasHungerEffect = player.hasEffect(MobEffects.HUNGER);

		context.blitSprite(FOOD_PIPELINE, selectFoodContainerTexture(hasHungerEffect), foodX, foodY, FOOD_SIZE, FOOD_SIZE);
		Identifier fillTexture = selectFoodFillTexture(hasHungerEffect, hungerPercent);
		if (fillTexture != null) {
			context.blitSprite(FOOD_PIPELINE, fillTexture, foodX, foodY, FOOD_SIZE, FOOD_SIZE);
		}

		int textX = foodX + FOOD_SIZE + FOOD_TEXT_SPACING;
		int textY = foodY + 1;
		context.pose().pushMatrix();
		context.pose().scale(HUNGER_TEXT_SCALE, HUNGER_TEXT_SCALE);
		context.drawString(
				client.font,
				hungerText,
				Math.round(textX / HUNGER_TEXT_SCALE),
				Math.round(textY / HUNGER_TEXT_SCALE),
				COLOR,
				true
		);
		context.pose().popMatrix();
		ci.cancel();
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
