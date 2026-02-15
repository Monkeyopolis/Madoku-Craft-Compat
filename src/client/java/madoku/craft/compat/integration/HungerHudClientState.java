package madoku.craft.compat.integration;

public final class HungerHudClientState {
	private static volatile int displayPoints;
	private static volatile int max;
	private static volatile boolean hasData;

	private HungerHudClientState() {
	}

	public static void update(int currentHunger, int pendingHunger, int maxHunger) {
		int safeCurrent = Math.max(0, currentHunger);
		int safePending = Math.max(0, pendingHunger);
		max = Math.max(0, maxHunger);
		displayPoints = safeCurrent + safePending;
		if (max > 0 && displayPoints > max) {
			displayPoints = max;
		}
		hasData = max > 0;
	}

	public static void clear() {
		displayPoints = 0;
		max = 0;
		hasData = false;
	}

	public static int toHudFoodLevel() {
		if (!hasData || max <= 0) {
			return -1;
		}
		double ratio = (double) displayPoints / (double) max;
		int scaled = (int) Math.round(ratio * 20.0d);
		if (scaled < 0) {
			return 0;
		}
		if (scaled > 20) {
			return 20;
		}
		return scaled;
	}

	public static String toHudText(String fallback) {
		if (!hasData || max <= 0) {
			return fallback;
		}
		int shownMax = Math.max(1, max);
		int shownPoints = Math.max(0, displayPoints);
		if (shownPoints > shownMax) {
			shownPoints = shownMax;
		}

		String prefix = "";
		if (fallback != null && !fallback.isEmpty()) {
			int firstDigit = -1;
			for (int i = 0; i < fallback.length(); i++) {
				if (Character.isDigit(fallback.charAt(i))) {
					firstDigit = i;
					break;
				}
			}
			if (firstDigit > 0) {
				prefix = fallback.substring(0, firstDigit);
			}
		}

		return prefix + shownPoints + "/" + shownMax;
	}
}
