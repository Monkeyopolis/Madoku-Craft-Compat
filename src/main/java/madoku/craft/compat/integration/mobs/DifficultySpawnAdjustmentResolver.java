package madoku.craft.compat.integration.mobs;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import madoku.craft.difficulty.config.DifficultyScalingConfig;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;

public final class DifficultySpawnAdjustmentResolver {
	private static final long TICKS_PER_DAY = 24_000L;

	private DifficultySpawnAdjustmentResolver() {
	}

	public static int resolve(ServerWorld world, BlockPos pos) {
		if (world == null || pos == null) {
			return 0;
		}

		DifficultyScalingConfig.Snapshot snapshot = DifficultyScalingConfig.get();
		if (!snapshot.enabled()) {
			return 0;
		}

		int biomeAdjustment = snapshot.biomesEnabled() ? snapshot.biomeAdjustment(resolveBiomeId(world, pos)) : 0;
		int structureAdjustment = snapshot.structuresEnabled()
				? resolveStructureAdjustment(world, pos, snapshot.structureAdjustments())
				: 0;
		int timeAdjustment = snapshot.timeEnabled() ? snapshot.timeAdjustment(resolveDayCount(world)) : 0;
		return Math.max(0, biomeAdjustment + structureAdjustment + timeAdjustment);
	}

	private static long resolveDayCount(ServerWorld world) {
		return Math.floorDiv(world.getServer().getOverworld().getTimeOfDay(), TICKS_PER_DAY);
	}

	private static Identifier resolveBiomeId(ServerWorld world, BlockPos pos) {
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

	private static int resolveStructureAdjustment(ServerWorld world, BlockPos pos,
			Map<Identifier, Integer> configuredAdjustments) {
		StructureContext context = resolveStructureContext(world, pos, configuredAdjustments);
		return context.adjustment();
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

	private static boolean isValidStart(StructureStart start) {
		return start != null && start != StructureStart.DEFAULT && start.hasChildren();
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

	private record StructureContext(Identifier structureId, int adjustment) {
		private static final StructureContext NONE = new StructureContext(null, 0);
	}
}
