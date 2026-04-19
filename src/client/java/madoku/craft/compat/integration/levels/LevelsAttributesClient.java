package madoku.craft.compat.integration.levels;

import madoku.craft.network.MadokuLevelUpPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class LevelsAttributesClient {
	private LevelsAttributesClient() {
	}

	public static void requestStatUpgrade(CompatLevelStat stat) {
		if (stat == null) {
			return;
		}

		ClientPlayNetworking.send(new MadokuLevelUpPayload(stat.id()));
	}
}
