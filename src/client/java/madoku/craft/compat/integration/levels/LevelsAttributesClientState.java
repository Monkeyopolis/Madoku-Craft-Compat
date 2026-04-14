package madoku.craft.compat.integration.levels;

import madoku.craft.network.MadokuLevelsPayload;

import java.util.EnumMap;
import java.util.List;

public final class LevelsAttributesClientState {
	private static Snapshot snapshot = Snapshot.empty();
	private static int version = 0;

	private LevelsAttributesClientState() {
	}

	public static void applyPayload(MadokuLevelsPayload payload) {
		if (payload == null) {
			return;
		}

		List<CompatLevelStat> visibleStats = CompatLevelStat.visibleStats();
		snapshot = new Snapshot(
			payload.username(),
			Math.max(1, payload.level()),
			Math.max(0, payload.currentXp()),
			Math.max(1, payload.requiredXp()),
			Math.max(0, payload.availablePoints()),
			Math.max(1, payload.maxStatLevel()),
			visibleStats.size() > 4,
			visibleStats,
			CompatLevelStat.decodeLevels(payload.statLevels())
		);
		version++;
	}

	public static Snapshot snapshot() {
		return snapshot;
	}

	public static int version() {
		return version;
	}

	public static void clear() {
		snapshot = Snapshot.empty();
		version++;
	}

	public record Snapshot(
		String username,
		int level,
		int currentXp,
		int requiredXp,
		int availablePoints,
		int maxStatLevel,
		boolean useAttributesContainer,
		List<CompatLevelStat> visibleStats,
		EnumMap<CompatLevelStat, Integer> statLevels
	) {
		private static Snapshot empty() {
			return new Snapshot(
				"",
				1,
				0,
				1,
				0,
				1,
				true,
				CompatLevelStat.visibleStats(),
				CompatLevelStat.createDefaultLevels()
			);
		}

		public boolean hasData() {
			return username != null && !username.isBlank();
		}

		public int statLevel(CompatLevelStat stat) {
			return statLevels.getOrDefault(stat, CompatLevelStat.DEFAULT_STAT_LEVEL);
		}
	}
}
