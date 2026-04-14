package madoku.craft.compat;

import madoku.craft.compat.integration.hud.HungerHudSync;
import madoku.craft.compat.integration.hud.WorldDifficultySync;
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
		if (loader.isModLoaded("madoku-craft-hud") && loader.isModLoaded("madoku-craft-attributes")) {
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
	}
}
