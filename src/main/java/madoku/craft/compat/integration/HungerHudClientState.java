package madoku.craft.compat.integration;

public final class HungerHudClientState {
	private static volatile int current = -1;
	private static volatile int pending = 0;
	private static volatile int max = -1;

	private HungerHudClientState() {
	}

	public static void update(int updatedCurrent, int updatedPending, int updatedMax) {
		current = Math.max(0, updatedCurrent);
		pending = Math.max(0, updatedPending);
		max = Math.max(1, updatedMax);
	}

	public static void clear() {
		current = -1;
		pending = 0;
		max = -1;
	}

	public static int current() {
		return current;
	}

	public static int pending() {
		return pending;
	}

	public static int max() {
		return max;
	}
}
