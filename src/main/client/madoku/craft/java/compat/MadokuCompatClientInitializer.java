package madoku.craft.java.compat;

import madoku.craft.java.core.module.MadokuClientRuntime;
import madoku.craft.java.core.module.MadokuModule;
import madoku.craft.java.core.module.MadokuModuleIds;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint for the optional Madoku compatibility jar. */
public final class MadokuCompatClientInitializer implements ClientModInitializer, MadokuModule {
	@Override public net.minecraft.resources.Identifier id() { return MadokuModuleIds.COMPAT; }
	@Override public void onInitializeClient() { MadokuClientRuntime.initialize(this); }
	@Override public void initialize() { MadokuCompatClient.initialize(); }
}
