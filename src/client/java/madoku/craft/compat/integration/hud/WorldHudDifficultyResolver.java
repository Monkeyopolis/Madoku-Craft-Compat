package madoku.craft.compat.integration.hud;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import madoku.craft.difficulty.config.DifficultyScalingConfig;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.structure.Structure;

public final class WorldHudDifficultyResolver {
	private static final long TICKS_PER_DAY = 24_000L;
	private static final int STRUCTURE_CACHE_MAX_ENTRIES = 1024;
	private static boolean configInitialized;
	private static final Map<StructureCacheKey, StructureChunkCache> STRUCTURE_LOOKUP_CACHE = Collections
			.synchronizedMap(new LinkedHashMap<>(STRUCTURE_CACHE_MAX_ENTRIES, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<StructureCacheKey, StructureChunkCache> eldest) {
					return size() > STRUCTURE_CACHE_MAX_ENTRIES;
				}
			});

	private WorldHudDifficultyResolver() {
	}

	public static void clearStructureCache() {
		STRUCTURE_LOOKUP_CACHE.clear();
	}

	public static int resolveTotalDifficulty(MinecraftClient client, ClientPlayerEntity player) {
		if (client == null || player == null) {
			return 0;
		}

		ensureConfigInitialized();
		DifficultyScalingConfig.Snapshot snapshot = DifficultyScalingConfig.get();
		if (!snapshot.enabled()) {
			return 0;
		}

		BlockPos pos = player.getBlockPos();
		int biomeAdjustment = resolveBiomeAdjustment(snapshot, player, pos);
		int timeAdjustment = snapshot.timeEnabled() ? resolveTimeAdjustment(client, player.getEntityWorld(), snapshot) : 0;
		int structureAdjustment = snapshot.structuresEnabled() ? resolveStructureAdjustment(client, player, pos, snapshot) : 0;

		return Math.max(1, 1 + biomeAdjustment + structureAdjustment + timeAdjustment);
	}

	private static synchronized void ensureConfigInitialized() {
		if (configInitialized) {
			return;
		}
		DifficultyScalingConfig.init();
		configInitialized = true;
	}

	private static int resolveBiomeAdjustment(DifficultyScalingConfig.Snapshot snapshot, ClientPlayerEntity player,
			BlockPos pos) {
		if (!snapshot.biomesEnabled()) {
			return 0;
		}
		return snapshot.biomeAdjustment(resolveBiomeId(player.getEntityWorld(), pos));
	}

	private static Identifier resolveBiomeId(World world, BlockPos pos) {
		try {
			RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
			Optional<RegistryKey<Biome>> key = biomeEntry.getKey();
			if (key.isPresent()) {
				return key.get().getValue();
			}

			Registry<Biome> biomeRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
			return biomeRegistry.getId(biomeEntry.value());
		} catch (Exception exception) {
			return null;
		}
	}

	private static int resolveTimeAdjustment(MinecraftClient client, World playerWorld,
			DifficultyScalingConfig.Snapshot snapshot) {
		long day = Math.floorDiv(resolveOverworldTimeOfDay(client, playerWorld), TICKS_PER_DAY);
		return snapshot.timeAdjustment(day);
	}

	private static long resolveOverworldTimeOfDay(MinecraftClient client, World playerWorld) {
		MinecraftServer server = client.getServer();
		if (server != null) {
			ServerWorld overworld = server.getOverworld();
			if (overworld != null) {
				return overworld.getTimeOfDay();
			}
		}
		return playerWorld.getTimeOfDay();
	}

	private static int resolveStructureAdjustment(MinecraftClient client, ClientPlayerEntity player, BlockPos pos,
			DifficultyScalingConfig.Snapshot snapshot) {
		ServerWorld world = resolveLookupWorld(client, player);
		if (world != null) {
			StructureContext context = resolveStructureContext(world, pos, snapshot.structureAdjustments());
			return context.adjustment();
		}

		// Dedicated servers are not directly accessible from the client, so keep a local fallback.
		StructureLookup lookup = resolveClientStructureLookup(player.getEntityWorld(), pos);
		if (!lookup.hasStructure()) {
			return 0;
		}

		int unknownAdjustment = DifficultyScalingConfig.defaultUnknownAdjustment();
		Identifier structureId = lookup.structureId();
		if (structureId == null) {
			return unknownAdjustment;
		}
		return snapshot.structureAdjustments().getOrDefault(structureId, unknownAdjustment);
	}

	private static ServerWorld resolveLookupWorld(MinecraftClient client, ClientPlayerEntity player) {
		RegistryKey<World> worldKey = player.getEntityWorld().getRegistryKey();
		MinecraftServer server = client.getServer();
		if (server != null) {
			return server.getWorld(worldKey);
		}
		return null;
	}

	private static StructureContext resolveStructureContext(ServerWorld world, BlockPos pos,
			Map<Identifier, Integer> configuredAdjustments) {
		if (!configuredAdjustments.isEmpty()) {
			Predicate<RegistryEntry<Structure>> configuredPredicate = entry -> entry.getKey()
					.map(RegistryKey::getValue)
					.map(configuredAdjustments::containsKey)
					.orElse(false);
			StructureStart configuredStart = findStructureContaining(world, pos, configuredPredicate);
			if (isValidStart(configuredStart)) {
				Identifier structureId = resolveStructureId(world, configuredStart);
				return structureContextFromId(structureId, configuredAdjustments);
			}
		}

		StructureStart start = findStructureContaining(world, pos, entry -> true);
		if (!isValidStart(start)) {
			return StructureContext.NONE;
		}

		Identifier structureId = resolveStructureId(world, start);
		return structureContextFromId(structureId, configuredAdjustments);
	}

	private static StructureStart findStructureContaining(ServerWorld world, BlockPos pos,
			Predicate<RegistryEntry<Structure>> predicate) {
		try {
			return world.getStructureAccessor().getStructureContaining(pos, predicate);
		} catch (Exception exception) {
			return StructureStart.DEFAULT;
		}
	}

	private static Identifier resolveStructureId(ServerWorld world, StructureStart start) {
		Registry<Structure> structureRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
		return structureRegistry.getId(start.getStructure());
	}

	private static StructureContext structureContextFromId(Identifier structureId,
			Map<Identifier, Integer> configuredAdjustments) {
		if (structureId == null) {
			return new StructureContext(null, DifficultyScalingConfig.defaultUnknownAdjustment());
		}

		return new StructureContext(
				structureId,
				configuredAdjustments.getOrDefault(structureId, DifficultyScalingConfig.defaultUnknownAdjustment()));
	}

	private static StructureLookup resolveClientStructureLookup(World world, BlockPos pos) {
		ChunkPos chunkPos = new ChunkPos(pos);
		StructureCacheKey cacheKey = new StructureCacheKey(
				world.getRegistryKey(),
				System.identityHashCode(world),
				chunkPos.toLong());
		StructureChunkCache cached = STRUCTURE_LOOKUP_CACHE.get(cacheKey);
		if (cached != null) {
			return cached.lookupAt(pos);
		}

		StructureChunkBuild built = buildStructureChunkCache(world, chunkPos);
		if (built.cacheable()) {
			STRUCTURE_LOOKUP_CACHE.put(cacheKey, built.chunkCache());
		}
		return built.chunkCache().lookupAt(pos);
	}

	private static StructureChunkBuild buildStructureChunkCache(World world, ChunkPos playerChunkPos) {
		Chunk playerChunk = world.getChunk(playerChunkPos.x, playerChunkPos.z, ChunkStatus.STRUCTURE_STARTS, false);
		if (playerChunk == null) {
			return StructureChunkBuild.uncacheable(StructureChunkCache.EMPTY);
		}

		DynamicRegistryManager registryManager = world.getRegistryManager();
		Registry<Structure> structureRegistry = registryManager.getOrThrow(RegistryKeys.STRUCTURE);
		Map<StructureCandidateKey, StructureCandidate> candidates = new LinkedHashMap<>();

		collectLocalStructureStarts(playerChunk, structureRegistry, candidates);
		boolean complete = collectReferencedStructureStarts(world, playerChunk, structureRegistry, candidates);

		StructureChunkCache chunkCache = new StructureChunkCache(List.copyOf(candidates.values()));
		return complete ? StructureChunkBuild.cacheable(chunkCache) : StructureChunkBuild.uncacheable(chunkCache);
	}

	private static void collectLocalStructureStarts(Chunk playerChunk, Registry<Structure> structureRegistry,
			Map<StructureCandidateKey, StructureCandidate> candidates) {
		for (Map.Entry<Structure, StructureStart> entry : playerChunk.getStructureStarts().entrySet()) {
			Structure structure = entry.getKey();
			Identifier structureId = structureRegistry.getId(structure);
			if (structureId == null) {
				continue;
			}
			addCandidate(candidates, structureId, entry.getValue());
		}
	}

	private static boolean collectReferencedStructureStarts(World world, Chunk playerChunk,
			Registry<Structure> structureRegistry, Map<StructureCandidateKey, StructureCandidate> candidates) {
		boolean complete = true;
		for (Map.Entry<Structure, LongSet> entry : playerChunk.getStructureReferences().entrySet()) {
			Structure structure = entry.getKey();
			Identifier structureId = structureRegistry.getId(structure);
			if (structureId == null) {
				continue;
			}

			LongSet refs = entry.getValue();
			if (refs == null || refs.isEmpty()) {
				continue;
			}

			LongIterator refIterator = refs.iterator();
			while (refIterator.hasNext()) {
				long packedRef = refIterator.nextLong();
				int x = ChunkPos.getPackedX(packedRef);
				int z = ChunkPos.getPackedZ(packedRef);
				Chunk refChunk = world.getChunk(x, z, ChunkStatus.STRUCTURE_STARTS, false);
				if (refChunk == null) {
					complete = false;
					continue;
				}
				StructureStart start = refChunk.getStructureStart(structure);
				if (!isValidStart(start)) {
					complete = false;
					continue;
				}
				addCandidate(candidates, structureId, start);
			}
		}
		return complete;
	}

	private static void addCandidate(Map<StructureCandidateKey, StructureCandidate> candidates,
			Identifier structureId, StructureStart start) {
		if (structureId == null || !isValidStart(start)) {
			return;
		}

		StructureCandidateKey candidateKey = new StructureCandidateKey(structureId, start.getPos().toLong());
		if (candidates.containsKey(candidateKey)) {
			return;
		}

		BlockBox box = start.getBoundingBox();
		candidates.put(candidateKey, new StructureCandidate(
				structureId,
				box.getMinX(),
				box.getMinY(),
				box.getMinZ(),
				box.getMaxX(),
				box.getMaxY(),
				box.getMaxZ()));
	}

	private static boolean isValidStart(StructureStart start) {
		return start != null && start != StructureStart.DEFAULT && start.hasChildren();
	}

	private record StructureCacheKey(RegistryKey<World> worldKey, int worldInstanceId, long chunkKey) {
	}

	private record StructureCandidateKey(Identifier structureId, long startChunkKey) {
	}

	private record StructureCandidate(
			Identifier structureId,
			int minX,
			int minY,
			int minZ,
			int maxX,
			int maxY,
			int maxZ) {
		private boolean contains(BlockPos pos) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return x >= minX && x <= maxX
					&& y >= minY && y <= maxY
					&& z >= minZ && z <= maxZ;
		}
	}

	private record StructureChunkCache(List<StructureCandidate> candidates) {
		private static final StructureChunkCache EMPTY = new StructureChunkCache(List.of());

		private StructureLookup lookupAt(BlockPos pos) {
			for (StructureCandidate candidate : candidates) {
				if (candidate.contains(pos)) {
					return new StructureLookup(true, candidate.structureId());
				}
			}
			return StructureLookup.NONE;
		}
	}

	private record StructureChunkBuild(StructureChunkCache chunkCache, boolean cacheable) {
		private static StructureChunkBuild cacheable(StructureChunkCache chunkCache) {
			return new StructureChunkBuild(chunkCache, true);
		}

		private static StructureChunkBuild uncacheable(StructureChunkCache chunkCache) {
			return new StructureChunkBuild(chunkCache, false);
		}
	}

	private record StructureLookup(boolean hasStructure, Identifier structureId) {
		private static final StructureLookup NONE = new StructureLookup(false, null);
	}

	private record StructureContext(Identifier structureId, int adjustment) {
		private static final StructureContext NONE = new StructureContext(null, 0);
	}

}
