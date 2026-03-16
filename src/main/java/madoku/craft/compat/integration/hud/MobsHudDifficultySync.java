package madoku.craft.compat.integration.hud;

import madoku.craft.mobs.difficulty.system.MadokuDifficulty;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MobsHudDifficultySync {
	private static boolean initialized = false;
	private static final Map<UUID, Integer> lastDifficultyByPlayer = new HashMap<>();

	private MobsHudDifficultySync() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		PayloadTypeRegistry.playS2C().register(MobsHudDifficultyPayload.TYPE, MobsHudDifficultyPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncSinglePlayer(handler.player, true));
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			reset();
			broadcastNow(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> reset());
		ServerTickEvents.END_SERVER_TICK.register(MobsHudDifficultySync::broadcastIfChanged);
		initialized = true;
	}

	public static void reset() {
		lastDifficultyByPlayer.clear();
	}

	public static void broadcastNow(MinecraftServer server) {
		broadcast(server, true);
	}

	public static void broadcastIfChanged(MinecraftServer server) {
		broadcast(server, false);
	}

	private static void broadcast(MinecraftServer server, boolean force) {
		if (server == null) {
			return;
		}
		Set<UUID> activePlayers = new HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			activePlayers.add(player.getUUID());
			syncSinglePlayer(player, force);
		}
		lastDifficultyByPlayer.keySet().removeIf(uuid -> !activePlayers.contains(uuid));
	}

	private static void syncSinglePlayer(ServerPlayer player, boolean force) {
		if (player == null) {
			return;
		}
		int difficultyLevel = Math.max(1, MadokuDifficulty.resolveHudDifficultyLevel(player));
		UUID playerId = player.getUUID();
		Integer previous = lastDifficultyByPlayer.get(playerId);
		boolean shouldSend = force || previous == null || previous != difficultyLevel;
		if (shouldSend && ServerPlayNetworking.canSend(player, MobsHudDifficultyPayload.TYPE)) {
			ServerPlayNetworking.send(player, new MobsHudDifficultyPayload(difficultyLevel));
		}
		lastDifficultyByPlayer.put(playerId, difficultyLevel);
	}
}
