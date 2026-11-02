package madoku.craft.java.compat;

import java.util.Set;

import madoku.craft.java.attributes.AttributesClientAPIManager;
import madoku.craft.java.core.season.PlayerClimatePayloadAPIManager;
import madoku.craft.java.core.season.SeasonClientAPIManager;
import madoku.craft.java.core.season.SeasonPayloadAPIManager;
import madoku.craft.java.core.time.TimePayloadAPIManager;
import madoku.craft.java.core.module.MadokuModuleIds;
import madoku.craft.java.core.module.MadokuModuleLifecycleManager;
import madoku.craft.java.hud.HudClientAPIManager;
import madoku.craft.java.mob.MobPayloadManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Routes feature payloads into client-side HUD and seasonal state. */
public final class MadokuCompatClient {
	private static boolean initialized;

	private MadokuCompatClient() { }

	public static void initialize() {
		if (initialized) return;
		initialized = true;

		MadokuModuleLifecycleManager lifecycle = MadokuModuleLifecycleManager.client();
		lifecycle.whenModulesRunning(Set.of(MadokuModuleIds.HUD), MadokuCompatClient::registerHudIntegration);
		lifecycle.whenModulesRunning(
			Set.of(MadokuModuleIds.ATTRIBUTES, MadokuModuleIds.HUD),
			MadokuCompatClient::registerAttributesHudIntegration
		);
		lifecycle.whenModulesRunning(
			Set.of(MadokuModuleIds.MOBS, MadokuModuleIds.HUD),
			MadokuCompatClient::registerMobsHudIntegration
		);
	}

	private static void registerHudIntegration() {
		ClientPlayNetworking.registerGlobalReceiver(TimePayloadAPIManager.TYPE,
			(payload, context) -> HudClientAPIManager.setServerTime(payload.day(), payload.hour(), payload.minute()));
		ClientPlayNetworking.registerGlobalReceiver(SeasonPayloadAPIManager.TYPE, (payload, context) ->
			context.client().execute(() -> {
				SeasonClientAPIManager.update(
					payload.season(),
					payload.temperatureOffset(),
					payload.humidityOffset(),
					payload.weatherCondition(),
					payload.seasonDay(),
					payload.seasonLengthDays()
				);
				SeasonClientAPIManager.refresh(context.client().level);
				HudClientAPIManager.setServerSeason(payload.season());
				HudClientAPIManager.setServerSeasonProgress(payload.seasonDay(), payload.seasonLengthDays());
			})
		);
		ClientPlayNetworking.registerGlobalReceiver(PlayerClimatePayloadAPIManager.TYPE,
			(payload, context) -> context.client().execute(() ->
				HudClientAPIManager.setServerClimate(payload.temperature(), payload.humidity())
			));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			HudClientAPIManager.reset();
		});
	}

	private static void registerAttributesHudIntegration() {
		AttributesClientAPIManager.addHungerListener(HudClientAPIManager::setServerHunger);
	}

	private static void registerMobsHudIntegration() {
		ClientPlayNetworking.registerGlobalReceiver(MobPayloadManager.TYPE,
			(payload, context) -> HudClientAPIManager.setServerDifficulty(payload.level()));
	}
}
