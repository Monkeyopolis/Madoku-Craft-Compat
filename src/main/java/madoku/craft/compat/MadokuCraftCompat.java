package madoku.craft.compat;

import madoku.craft.compat.integration.hud.MobsHudDifficultySync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MadokuCraftCompat implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-compat";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		FabricLoader loader = FabricLoader.getInstance();
		if (loader.isModLoaded("madoku-craft-mobs")) {
			MobsHudDifficultySync.initialize();
			LOGGER.info("Madoku Craft Compat: Mobs difficulty HUD sync initialized.");
		}
	}
}
