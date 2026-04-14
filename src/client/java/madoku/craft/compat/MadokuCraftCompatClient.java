package madoku.craft.compat;

import madoku.craft.compat.integration.hud.HungerHudSyncClient;
import madoku.craft.compat.integration.hud.WorldDifficultyClientState;
import madoku.craft.compat.integration.hud.WorldDifficultyPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class MadokuCraftCompatClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FabricLoader loader = FabricLoader.getInstance();
		if (loader.isModLoaded("madoku-craft-hud") && loader.isModLoaded("madoku-craft-attributes")) {
			HungerHudSyncClient.initialize();
			MadokuCraftCompat.LOGGER.info("Madoku Craft Compat: HUD/Attributes client sync initialized.");
		}
		if (loader.isModLoaded("madoku-craft-hud") && loader.isModLoaded("madoku-craft-mobs")) {
			ClientPlayNetworking.registerGlobalReceiver(
				WorldDifficultyPayload.TYPE,
				(payload, context) -> WorldDifficultyClientState.update(payload.level())
			);
			ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WorldDifficultyClientState.clear());
			MadokuCraftCompat.LOGGER.info("Madoku Craft Compat: HUD/Mobs world difficulty client sync initialized.");
		}
	}
}
