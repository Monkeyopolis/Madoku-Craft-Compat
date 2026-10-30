package madoku.craft.java.compat.levels;

import madoku.craft.java.attributes.HealthAPIManager;
import madoku.craft.java.attributes.HungerAPIManager;
import madoku.craft.java.attributes.LuckAPIManager;
import madoku.craft.java.attributes.AttributesAPIManager;
import madoku.craft.java.attributes.OxygenAPIManager;
import madoku.craft.java.compat.MadokuCompatModuleState;
import madoku.craft.java.levels.LevelsFeatureAPIManager;
import madoku.craft.java.levels.LevelsFeatureAdapter;

import net.minecraft.server.level.ServerPlayer;

/** Installs Levels bridges for Attributes and Pets. */
public final class MadokuLevelsFeatureAdapters {
	private MadokuLevelsFeatureAdapters() { }

	public static void initialize() {
		LevelsFeatureAPIManager.registerAdapter(new LevelsFeatureAdapter() {
			@Override
			public boolean useAttributesContainer() {
				return MadokuCompatModuleState.isLoaded(MadokuCompatModuleState.ATTRIBUTES_ID)
					&& AttributesAPIManager.isEnabled()
					&& (HungerAPIManager.isEnabled() || LuckAPIManager.isEnabled() || OxygenAPIManager.isEnabled());
			}

			@Override
			public boolean isHungerEnabled() {
				return MadokuCompatModuleState.isLoaded(MadokuCompatModuleState.ATTRIBUTES_ID)
					&& HungerAPIManager.isEnabled();
			}

			@Override
			public boolean isLuckEnabled() {
				return MadokuCompatModuleState.isLoaded(MadokuCompatModuleState.ATTRIBUTES_ID)
					&& LuckAPIManager.isEnabled();
			}

			@Override
			public boolean isOxygenEnabled() {
				return MadokuCompatModuleState.isLoaded(MadokuCompatModuleState.ATTRIBUTES_ID)
					&& OxygenAPIManager.isEnabled();
			}

			@Override
			public void handleMaximumHungerChanged(ServerPlayer player) {
				if (MadokuCompatModuleState.isLoaded(MadokuCompatModuleState.ATTRIBUTES_ID)) {
					HungerAPIManager.handleMaximumHungerChanged(player);
				}
			}

			@Override
			public void restoreJoinHunger(ServerPlayer player) {
				if (MadokuCompatModuleState.isLoaded(MadokuCompatModuleState.ATTRIBUTES_ID)) {
					HungerAPIManager.restoreJoinHunger(player);
				}
			}

			@Override
			public void handleMaximumOxygenChanged(ServerPlayer player) {
				if (MadokuCompatModuleState.isLoaded(MadokuCompatModuleState.ATTRIBUTES_ID)) {
					OxygenAPIManager.handleMaximumOxygenChanged(player);
				}
			}

			@Override
			public void restoreJoinHealth(ServerPlayer player) {
				if (MadokuCompatModuleState.isLoaded(MadokuCompatModuleState.ATTRIBUTES_ID)) {
					HealthAPIManager.restoreJoinHealth(player);
				}
			}
		});
	}
}
