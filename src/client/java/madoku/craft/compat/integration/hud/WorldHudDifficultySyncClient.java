package madoku.craft.compat.integration.hud;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class WorldHudDifficultySyncClient {
	private WorldHudDifficultySyncClient() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(
				WorldHudTotalDifficultyPayload.ID,
				(payload, context) -> {
					context.client().execute(() ->
							WorldHudDifficultyResolver.setServerSyncedTotalDifficulty(payload.totalDifficulty()));
				}
		);
	}
}
