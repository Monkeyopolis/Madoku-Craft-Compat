package madoku.craft.compat.integration;

public final class HungerHudClientState {
	private static final double HUNGER_UNIT_SCALE = 8.0d;

	private static volatile int currentPoints;
	private static volatile int max;
	private static volatile boolean hasData;

	private HungerHudClientState() {
	}

	public static void update(int currentHunger, int pendingHunger, int maxHunger) {
		int safeCurrent = Math.max(0, currentHunger);
		max = Math.max(0, maxHunger);
		currentPoints = safeCurrent;
		if (max > 0 && currentPoints > max) {
			currentPoints = max;
		}
		hasData = max > 0;
	}

	public static void clear() {
		currentPoints = 0;
		max = 0;
		hasData = false;
	}

	public static int toHudCurrentPoints() {
		if (!hasData || max <= 0) {
			return -1;
		}
		return Math.max(0, madokuCompat$toDisplayPoints(currentPoints));
	}

	public static int toHudMaxPoints() {
		if (!hasData || max <= 0) {
			return -1;
		}
		return Math.max(1, madokuCompat$toDisplayPoints(max));
	}

	private static int madokuCompat$toDisplayPoints(int rawUnits) {
		double displayValue = Math.max(0.0d, rawUnits) / HUNGER_UNIT_SCALE;
		return (int) Math.round(displayValue);
	}
}
