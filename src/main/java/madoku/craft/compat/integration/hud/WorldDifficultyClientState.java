package madoku.craft.compat.integration.hud;

public final class WorldDifficultyClientState {
	private static volatile int difficultyLevel = 1;
	private static volatile boolean hasServerDifficulty = false;

	private WorldDifficultyClientState() {
	}

	public static void update(int level) {
		difficultyLevel = Math.max(1, level);
		hasServerDifficulty = true;
	}

	public static void clear() {
		difficultyLevel = 1;
		hasServerDifficulty = false;
	}

	public static String displayText() {
		if (!hasServerDifficulty) {
			return "1";
		}
		return Integer.toString(Math.max(1, difficultyLevel));
	}
}
