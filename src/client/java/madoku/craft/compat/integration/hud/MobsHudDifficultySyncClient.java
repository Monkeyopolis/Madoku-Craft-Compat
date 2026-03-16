package madoku.craft.compat.integration.hud;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MobsHudDifficultySyncClient {
	private static boolean initialized = false;

	private MobsHudDifficultySyncClient() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		ClientPlayNetworking.registerGlobalReceiver(
				MobsHudDifficultyPayload.TYPE,
				(payload, context) -> MobsHudDifficultyClientState.update(payload.level())
		);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MobsHudDifficultyClientState.clear());
		initialized = true;
	}
}
