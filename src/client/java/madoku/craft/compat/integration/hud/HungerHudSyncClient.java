package madoku.craft.compat.integration.hud;

import madoku.craft.compat.integration.HungerHudClientState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class HungerHudSyncClient {
	private static boolean initialized = false;

	private HungerHudSyncClient() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		ClientPlayNetworking.registerGlobalReceiver(
			HungerHudPayload.TYPE,
			(payload, context) -> HungerHudClientState.update(payload.current(), payload.pending(), payload.max())
		);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HungerHudClientState.clear());
		initialized = true;
	}
}
