package madoku.craft.compat;

import madoku.craft.compat.integration.hud.MobsHudDifficultySyncClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class MadokuCraftCompatClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FabricLoader loader = FabricLoader.getInstance();
		if (loader.isModLoaded("madoku-craft-hud") && loader.isModLoaded("madoku-craft-mobs")) {
			MobsHudDifficultySyncClient.initialize();
			MadokuCraftCompat.LOGGER.info("Madoku Craft Compat: HUD/Mobs client sync initialized.");
		}
	}
}
