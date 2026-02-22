package madoku.craft.compat.integration.hud;

import madoku.craft.compat.integration.mobs.DifficultySpawnAdjustmentResolver;
import madoku.craft.difficulty.config.DifficultyScalingConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class WorldHudDifficultySync {
	private static final int SYNC_INTERVAL_TICKS = 20;
	private static int tickCounter;

	private WorldHudDifficultySync() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(WorldHudTotalDifficultyPayload.ID, WorldHudTotalDifficultyPayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				sendToPlayer(handler.player));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter++;
			if (tickCounter < SYNC_INTERVAL_TICKS) {
				return;
			}
			tickCounter = 0;

			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				sendToPlayer(player);
			}
		});
	}

	private static void sendToPlayer(ServerPlayerEntity player) {
		if (player == null || player.isRemoved()) {
			return;
		}
		if (!(player.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		if (!ServerPlayNetworking.canSend(player, WorldHudTotalDifficultyPayload.ID)) {
			return;
		}
		int totalDifficulty = resolveTotalDifficulty(world, player);
		ServerPlayNetworking.send(player, new WorldHudTotalDifficultyPayload(totalDifficulty));
	}

	private static int resolveTotalDifficulty(ServerWorld world, ServerPlayerEntity player) {
		DifficultyScalingConfig.Snapshot snapshot = DifficultyScalingConfig.get();
		if (!snapshot.enabled()) {
			return 0;
		}

		int spawnAdjustment = DifficultySpawnAdjustmentResolver.resolve(world, player.getBlockPos());
		return Math.max(1, 1 + spawnAdjustment);
	}
}
