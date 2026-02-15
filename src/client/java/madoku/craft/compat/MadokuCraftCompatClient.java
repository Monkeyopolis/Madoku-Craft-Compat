package madoku.craft.compat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class MadokuCraftCompatClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FabricLoader loader = FabricLoader.getInstance();
		boolean hudLoaded = loader.isModLoaded("madoku-craft-hud");
		if (!hudLoaded) {
			return;
		}
		if (loader.isModLoaded("madoku-craft-health")) {
			MadokuCraftCompat.LOGGER.info("Madoku Craft Compat: HUD/Health integration enabled.");
		}
		if (loader.isModLoaded("madoku-craft-hunger")) {
			MadokuCraftCompat.LOGGER.info("Madoku Craft Compat: HUD/Hunger integration enabled.");
		}
		if (loader.isModLoaded("madoku-craft-armor")) {
			MadokuCraftCompat.LOGGER.info("Madoku Craft Compat: HUD/Armor integration enabled.");
		}
	}
}
