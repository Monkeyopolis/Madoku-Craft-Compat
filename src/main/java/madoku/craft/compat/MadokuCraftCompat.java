package madoku.craft.compat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MadokuCraftCompat implements ModInitializer {
	public static final String MOD_ID = "madoku-craft-compat";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		boolean hungerLoaded = FabricLoader.getInstance().isModLoaded("madoku-craft-hunger");
		boolean healthLoaded = FabricLoader.getInstance().isModLoaded("madoku-craft-health");
		if (hungerLoaded && healthLoaded) {
			LOGGER.info("Madoku Craft Compat: Hunger/Health integration enabled.");
		}
	}
}
