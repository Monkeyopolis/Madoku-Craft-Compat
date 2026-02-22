package madoku.craft.compat;

import madoku.craft.compat.integration.hud.WorldHudDifficultyResolver;
import madoku.craft.compat.integration.hud.WorldHudDifficultySyncClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

public class MadokuCraftCompatClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FabricLoader loader = FabricLoader.getInstance();
		boolean hudLoaded = loader.isModLoaded("madoku-craft-hud");
		if (!hudLoaded) {
			return;
		}
		boolean difficultyLoaded = loader.isModLoaded("madoku-craft-difficulty");
		if (difficultyLoaded) {
			WorldHudDifficultySyncClient.init();
			ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			{
				WorldHudDifficultyResolver.clearStructureCache();
				WorldHudDifficultyResolver.clearServerSyncedTotalDifficulty();
			});
			ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			{
				WorldHudDifficultyResolver.clearStructureCache();
				WorldHudDifficultyResolver.clearServerSyncedTotalDifficulty();
			});
		}

		if (loader.isModLoaded("madoku-craft-armor")) {
			MadokuCraftCompat.LOGGER.info("Madoku Craft Compat: HUD/Armor integration enabled.");
		}
	}
}
