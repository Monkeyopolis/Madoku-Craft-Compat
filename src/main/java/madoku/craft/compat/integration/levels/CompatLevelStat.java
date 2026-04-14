package madoku.craft.compat.integration.levels;

import madoku.craft.compat.MadokuCraftCompat;
import madoku.craft.hunger.MadokuHunger;
import madoku.craft.luck.MadokuLuck;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum CompatLevelStat {
	HEALTH("health", "Health", "health"),
	PLAYER_DAMAGE("player_damage", "Strength", "strength"),
	PLAYER_ARMOR("player_armor", "Defense", "defense"),
	PLAYER_LUCK("player_luck", "Luck", "luck"),
	PLAYER_HUNGER("player_hunger", "Hunger", "hunger"),
	PLAYER_MOVEMENT_SPEED("player_movement_speed", "Speed", "speed");

	public static final int DEFAULT_STAT_LEVEL = 0;

	private final String id;
	private final String label;
	private final String iconTextureName;

	CompatLevelStat(String id, String label, String iconTextureName) {
		this.id = id;
		this.label = label;
		this.iconTextureName = iconTextureName;
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public Identifier iconTexture() {
		return Identifier.fromNamespaceAndPath(
			MadokuCraftCompat.MOD_ID,
			"textures/icons/" + iconTextureName + ".png"
		);
	}

	public boolean isCustomAttributeStat() {
		return this == PLAYER_LUCK || this == PLAYER_HUNGER;
	}

	public boolean isEnabled() {
		return switch (this) {
			case PLAYER_LUCK -> MadokuLuck.isEnabled();
			case PLAYER_HUNGER -> MadokuHunger.isEnabled();
			default -> true;
		};
	}

	public static CompatLevelStat fromId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}

		String normalized = id.trim();
		for (CompatLevelStat stat : values()) {
			if (stat.id.equalsIgnoreCase(normalized)) {
				return stat;
			}
		}

		return null;
	}

	public static List<CompatLevelStat> vanillaVisibleStats() {
		return List.of(HEALTH, PLAYER_DAMAGE, PLAYER_ARMOR, PLAYER_MOVEMENT_SPEED);
	}

	public static List<CompatLevelStat> attributeVisibleStats() {
		return List.of(HEALTH, PLAYER_DAMAGE, PLAYER_ARMOR, PLAYER_LUCK, PLAYER_HUNGER, PLAYER_MOVEMENT_SPEED);
	}

	public static List<CompatLevelStat> attributeVisibleStatsWithoutHunger() {
		return List.of(HEALTH, PLAYER_DAMAGE, PLAYER_ARMOR, PLAYER_LUCK, PLAYER_MOVEMENT_SPEED);
	}

	public static List<CompatLevelStat> attributeVisibleStatsWithoutLuck() {
		return List.of(HEALTH, PLAYER_DAMAGE, PLAYER_ARMOR, PLAYER_HUNGER, PLAYER_MOVEMENT_SPEED);
	}

	public static List<CompatLevelStat> visibleStats() {
		if (!MadokuHunger.isEnabled() && !MadokuLuck.isEnabled()) {
			return vanillaVisibleStats();
		}
		if (!MadokuHunger.isEnabled()) {
			return attributeVisibleStatsWithoutHunger();
		}
		if (!MadokuLuck.isEnabled()) {
			return attributeVisibleStatsWithoutLuck();
		}
		return attributeVisibleStats();
	}

	public static EnumMap<CompatLevelStat, Integer> createDefaultLevels() {
		EnumMap<CompatLevelStat, Integer> levels = new EnumMap<>(CompatLevelStat.class);
		for (CompatLevelStat stat : values()) {
			levels.put(stat, DEFAULT_STAT_LEVEL);
		}
		return levels;
	}

	public static EnumMap<CompatLevelStat, Integer> decodeLevels(String encodedLevels) {
		EnumMap<CompatLevelStat, Integer> decoded = createDefaultLevels();
		if (encodedLevels == null || encodedLevels.isBlank()) {
			return decoded;
		}

		String[] entries = encodedLevels.split(";");
		for (String entry : entries) {
			String[] pair = entry.split("=", 2);
			if (pair.length != 2) {
				continue;
			}

			CompatLevelStat stat = fromId(pair[0]);
			if (stat == null) {
				continue;
			}

			try {
				decoded.put(stat, Math.max(0, Integer.parseInt(pair[1].trim())));
			} catch (NumberFormatException ignored) {
				decoded.put(stat, DEFAULT_STAT_LEVEL);
			}
		}

		return decoded;
	}

	public static String appendLevels(String baseLevels, Map<CompatLevelStat, Integer> extraLevels) {
		StringBuilder builder = new StringBuilder(baseLevels == null ? "" : baseLevels.trim());
		if (extraLevels == null || extraLevels.isEmpty()) {
			return builder.toString();
		}

		for (CompatLevelStat stat : values()) {
			if (!stat.isCustomAttributeStat()) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(';');
			}
			builder.append(stat.id()).append('=').append(Math.max(0, extraLevels.getOrDefault(stat, DEFAULT_STAT_LEVEL)));
		}

		return builder.toString();
	}
}
