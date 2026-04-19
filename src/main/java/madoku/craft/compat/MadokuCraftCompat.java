package madoku.craft.compat;

import madoku.craft.compat.integration.hud.HungerHudSync;
import madoku.craft.compat.integration.hud.WorldDifficultySync;
import madoku.craft.compat.integration.levels.LevelsAttributesExtraStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
		boolean attributesLoaded = loader.isModLoaded("madoku-craft-attributes");
		boolean levelsLoaded = loader.isModLoaded("madoku-craft-levels");
		if (loader.isModLoaded("madoku-craft-hud") && attributesLoaded) {
			HungerHudSync.initialize();
			LOGGER.info("Madoku Craft Compat: HUD/Attributes hunger sync initialized.");
		}
		if (loader.isModLoaded("madoku-craft-mobs")) {
			WorldDifficultySync.initialize();
			ServerLifecycleEvents.SERVER_STARTED.register(server -> {
				WorldDifficultySync.reset();
				WorldDifficultySync.broadcastNow(server);
			});
			ServerLifecycleEvents.SERVER_STOPPED.register(server -> WorldDifficultySync.reset());
			ServerTickEvents.END_SERVER_TICK.register(WorldDifficultySync::broadcastIfChanged);
			LOGGER.info("Madoku Craft Compat: HUD/Mobs world difficulty sync initialized.");
		}
		if (levelsLoaded && attributesLoaded) {
			ServerLifecycleEvents.SERVER_STARTED.register(LevelsAttributesExtraStats::loadPersistedData);
			ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
				LevelsAttributesExtraStats.savePersistedData(server);
				LevelsAttributesExtraStats.reset();
			});
			LOGGER.info("Madoku Craft Compat: Levels/Attributes extended stat sync initialized.");
		}
	}
}
