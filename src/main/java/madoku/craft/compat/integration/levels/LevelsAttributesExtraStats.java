package madoku.craft.compat.integration.levels;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import madoku.craft.compat.MadokuCraftCompat;
import madoku.craft.config.StaticJsonSystem;
import madoku.craft.data.MadokuData;
import madoku.craft.levels.MadokuLevels;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LevelsAttributesExtraStats {
	private static final Logger LOGGER = LoggerFactory.getLogger(LevelsAttributesExtraStats.class);

	private static final String LEVELS_CONFIG_FOLDER_NAME = "madoku-craft-levels";
	private static final String LEVELS_CONFIG_FILE_NAME = "madoku-levels";
	private static final String DATA_FOLDER_NAME = "madoku-craft-compat";
	private static final String DATA_FILE_NAME = "levels-attributes-extra";
	private static final String PLAYER_LUCK_PER_LEVEL_KEY = "player_luck_per_level";
	private static final String PLAYER_HUNGER_PER_LEVEL_KEY = "player_hunger_per_level";
	private static final double DEFAULT_PLAYER_LUCK_PER_LEVEL = 0.02d;
	private static final double DEFAULT_PLAYER_HUNGER_PER_LEVEL = 2.0d;
	private static final Identifier LUCK_BONUS_MODIFIER_ID =
		Identifier.fromNamespaceAndPath(MadokuCraftCompat.MOD_ID, "levels_luck_bonus");
	private static final String HUNGER_DATA_FOLDER_NAME = "madoku-craft-hunger";
	private static final String HUNGER_DATA_FILE_NAME = "madoku-hunger";

	private static final Map<UUID, ExtraPlayerState> PLAYER_STATES = new HashMap<>();
	private static final Map<UUID, Integer> STARTUP_PERSISTED_HUNGER_POINTS = new HashMap<>();

	private static volatile double playerLuckPerLevel = DEFAULT_PLAYER_LUCK_PER_LEVEL;
	private static volatile double playerHungerPerLevel = DEFAULT_PLAYER_HUNGER_PER_LEVEL;
	private static volatile boolean reflectionInitialized;
	private static volatile boolean reflectionFailed;
	private static Field levelsDirtyPlayersField;
	private static Field levelsAvailablePointsField;
	private static Method ensurePlayerStateMethod;
	private static volatile boolean hungerReflectionInitialized;
	private static volatile boolean hungerReflectionFailed;
	private static Field hungerPlayerStatesField;
	private static java.lang.reflect.Constructor<?> hungerPlayerStateConstructor;
	private static Method hungerGetConfiguredMaximumMethod;
	private static Method hungerInitializePlayerStateMethod;
	private static Method hungerApplyFoodStateMethod;
	private static Method hungerSyncHudStateMethod;
	private static Field hungerPointsField;
	private static Field hungerLastSyncedCurrentField;
	private static Field hungerLastSyncedPendingField;
	private static Field hungerLastSyncedMaxField;

	private LevelsAttributesExtraStats() {
	}

	public static void loadPersistedData(MinecraftServer server) {
		loadConfig();
		PLAYER_STATES.clear();
		STARTUP_PERSISTED_HUNGER_POINTS.clear();
		if (server == null) {
			return;
		}

		MadokuData.createWorldData(server, DATA_FOLDER_NAME, DATA_FILE_NAME, createDefaultData());
		JsonObject data = MadokuData.loadWorldData(server, DATA_FOLDER_NAME, DATA_FILE_NAME);
		if (data == null) {
			return;
		}

		JsonArray players = data.has("players") && data.get("players").isJsonArray() ? data.getAsJsonArray("players") : new JsonArray();
		for (JsonElement element : players) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject playerData = element.getAsJsonObject();
			String uuidText = getString(playerData, "uuid", "");
			if (uuidText.isBlank()) {
				continue;
			}

			try {
				UUID playerId = UUID.fromString(uuidText);
				ExtraPlayerState state = new ExtraPlayerState(
					Math.max(0, getInt(playerData, CompatLevelStat.PLAYER_LUCK.id(), 0)),
					Math.max(0, getInt(playerData, CompatLevelStat.PLAYER_HUNGER.id(), 0))
				);
				PLAYER_STATES.put(playerId, state);
			} catch (IllegalArgumentException ignored) {
			}
		}

		loadPersistedHungerSnapshots(server);
		markAllPlayersDirty(server);
	}

	public static void savePersistedData(MinecraftServer server) {
		if (server == null) {
			return;
		}

		MadokuData.saveWorldData(server, DATA_FOLDER_NAME, DATA_FILE_NAME, toPersistedData());
	}

	public static void reset() {
		PLAYER_STATES.clear();
		STARTUP_PERSISTED_HUNGER_POINTS.clear();
	}

	public static boolean handleCustomStatUpgrade(ServerPlayer player, String statId) {
		CompatLevelStat stat = CompatLevelStat.fromId(statId);
		if (player == null || stat == null || !stat.isCustomAttributeStat()) {
			return false;
		}
		if (!stat.isEnabled()) {
			return true;
		}

		Object levelsState = ensureLevelsPlayerState(player);
		if (levelsState == null) {
			return true;
		}

		int availablePoints = getAvailablePoints(levelsState);
		if (availablePoints <= 0) {
			return true;
		}

		int maxStatLevel = Math.max(1, MadokuLevels.maxStatLevel());
		int currentLevel = getLevel(player.getUUID(), stat);
		if (currentLevel >= maxStatLevel) {
			return true;
		}

		setLevel(player.getUUID(), stat, Math.min(maxStatLevel, currentLevel + 1));
		setAvailablePoints(levelsState, availablePoints - 1);
		applyExtraPlayerAttributes(player);
		notifyHungerCapChanged(player);
		markDirty(player.getUUID());
		return true;
	}

	public static void applyExtraPlayerAttributes(ServerPlayer player) {
		if (player == null) {
			return;
		}

		AttributeInstance luckAttribute = player.getAttribute(Attributes.LUCK);
		if (luckAttribute != null) {
			luckAttribute.removeModifier(LUCK_BONUS_MODIFIER_ID);
			double bonus = CompatLevelStat.PLAYER_LUCK.isEnabled()
				? getLevel(player.getUUID(), CompatLevelStat.PLAYER_LUCK) * playerLuckPerLevel
				: 0.0d;
			if (bonus > 0.0d) {
				luckAttribute.addOrUpdateTransientModifier(
					new AttributeModifier(LUCK_BONUS_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE)
				);
			}
		}
	}

	public static int getPlayerHungerBonusPoints(ServerPlayer player) {
		if (player == null || !CompatLevelStat.PLAYER_HUNGER.isEnabled()) {
			return 0;
		}
		return (int) Math.round(getLevel(player.getUUID(), CompatLevelStat.PLAYER_HUNGER) * playerHungerPerLevel);
	}

	public static String appendCustomStatLevels(ServerPlayer player, String baseLevels) {
		Map<CompatLevelStat, Integer> extraLevels = new EnumMap<>(CompatLevelStat.class);
		if (player != null) {
			extraLevels.put(CompatLevelStat.PLAYER_LUCK, getLevel(player.getUUID(), CompatLevelStat.PLAYER_LUCK));
			extraLevels.put(CompatLevelStat.PLAYER_HUNGER, getLevel(player.getUUID(), CompatLevelStat.PLAYER_HUNGER));
		}
		return CompatLevelStat.appendLevels(baseLevels, extraLevels);
	}

	public static void restoreStartupPersistedHungerState(ServerPlayer player) {
		if (player == null) {
			return;
		}

		Integer persistedHungerPoints = STARTUP_PERSISTED_HUNGER_POINTS.remove(player.getUUID());
		if (persistedHungerPoints == null) {
			return;
		}

		initializeHungerReflection();
		if (hungerReflectionFailed) {
			return;
		}

		try {
			@SuppressWarnings("unchecked")
			Map<UUID, Object> playerStates = (Map<UUID, Object>) hungerPlayerStatesField.get(null);
			if (playerStates == null) {
				return;
			}

			Object state = playerStates.get(player.getUUID());
			if (state == null) {
				state = hungerPlayerStateConstructor.newInstance();
				playerStates.put(player.getUUID(), state);
			}

			int configuredMaximum = ((Number) hungerGetConfiguredMaximumMethod.invoke(null)).intValue();
			int maximumHungerPoints = Math.max(1, configuredMaximum + Math.max(0, getPlayerHungerBonusPoints(player)));
			hungerPointsField.setInt(state, Math.max(0, Math.min(maximumHungerPoints, persistedHungerPoints)));
		} catch (ReflectiveOperationException | RuntimeException exception) {
			LOGGER.warn("Failed to restore persisted MadokuHunger state for Levels/Attributes compat.", exception);
			hungerReflectionFailed = true;
		}
	}

	private static int getLevel(UUID playerId, CompatLevelStat stat) {
		if (playerId == null || stat == null) {
			return 0;
		}

		ExtraPlayerState state = PLAYER_STATES.get(playerId);
		if (state == null) {
			return 0;
		}

		return switch (stat) {
			case PLAYER_LUCK -> state.luckLevel;
			case PLAYER_HUNGER -> state.hungerLevel;
			default -> 0;
		};
	}

	private static void setLevel(UUID playerId, CompatLevelStat stat, int level) {
		if (playerId == null || stat == null) {
			return;
		}

		ExtraPlayerState state = PLAYER_STATES.computeIfAbsent(playerId, ignored -> new ExtraPlayerState(0, 0));
		int normalized = Math.max(0, level);
		switch (stat) {
			case PLAYER_LUCK -> state.luckLevel = normalized;
			case PLAYER_HUNGER -> state.hungerLevel = normalized;
			default -> {
			}
		}
	}

	private static void loadConfig() {
		playerLuckPerLevel = DEFAULT_PLAYER_LUCK_PER_LEVEL;
		playerHungerPerLevel = DEFAULT_PLAYER_HUNGER_PER_LEVEL;

		try {
			Path directory = StaticJsonSystem.getOrCreateGlobalSystemDirectory(LEVELS_CONFIG_FOLDER_NAME);
			Path configFile = resolveJsonFile(directory, LEVELS_CONFIG_FILE_NAME);
			JsonObject source = StaticJsonSystem.readJsonFile(configFile);
			playerLuckPerLevel = getDouble(source, PLAYER_LUCK_PER_LEVEL_KEY, DEFAULT_PLAYER_LUCK_PER_LEVEL);
			playerHungerPerLevel = getDouble(source, PLAYER_HUNGER_PER_LEVEL_KEY, DEFAULT_PLAYER_HUNGER_PER_LEVEL);
		} catch (IOException | RuntimeException exception) {
			LOGGER.warn("Failed to read Levels config for extended Attributes stats; using defaults.", exception);
		}
	}

	private static void loadPersistedHungerSnapshots(MinecraftServer server) {
		if (server == null) {
			return;
		}

		JsonObject data = MadokuData.loadWorldData(server, HUNGER_DATA_FOLDER_NAME, HUNGER_DATA_FILE_NAME);
		if (data == null) {
			return;
		}

		JsonArray players = data.has("players") && data.get("players").isJsonArray() ? data.getAsJsonArray("players") : null;
		if (players == null) {
			return;
		}

		for (JsonElement element : players) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject playerData = element.getAsJsonObject();
			String uuidText = getString(playerData, "uuid", "");
			if (uuidText.isBlank()) {
				continue;
			}

			try {
				UUID playerId = UUID.fromString(uuidText);
				STARTUP_PERSISTED_HUNGER_POINTS.put(playerId, Math.max(0, getInt(playerData, "hunger_points", 0)));
			} catch (IllegalArgumentException ignored) {
			}
		}
	}

	private static Path resolveJsonFile(Path directory, String fileName) {
		String normalized = fileName == null ? "" : fileName.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Config file name must not be blank.");
		}
		if (!normalized.endsWith(".json")) {
			normalized = normalized + ".json";
		}
		return directory.resolve(normalized);
	}

	private static JsonObject createDefaultData() {
		JsonObject root = new JsonObject();
		root.add("players", new JsonArray());
		return root;
	}

	private static JsonObject toPersistedData() {
		JsonObject root = new JsonObject();
		JsonArray players = new JsonArray();
		for (Map.Entry<UUID, ExtraPlayerState> entry : PLAYER_STATES.entrySet()) {
			JsonObject playerData = new JsonObject();
			playerData.addProperty("uuid", entry.getKey().toString());
			playerData.addProperty(CompatLevelStat.PLAYER_LUCK.id(), Math.max(0, entry.getValue().luckLevel));
			playerData.addProperty(CompatLevelStat.PLAYER_HUNGER.id(), Math.max(0, entry.getValue().hungerLevel));
			players.add(playerData);
		}
		root.add("players", players);
		return root;
	}

	private static int getInt(JsonObject object, String memberName, int fallback) {
		if (object == null || memberName == null || !object.has(memberName)) {
			return fallback;
		}
		JsonElement element = object.get(memberName);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsInt() : fallback;
	}

	private static String getString(JsonObject object, String memberName, String fallback) {
		if (object == null || memberName == null || !object.has(memberName)) {
			return fallback;
		}
		JsonElement element = object.get(memberName);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? element.getAsString() : fallback;
	}

	private static double getDouble(JsonObject object, String memberName, double fallback) {
		if (object == null || memberName == null || !object.has(memberName)) {
			return fallback;
		}
		JsonElement element = object.get(memberName);
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			return fallback;
		}
		try {
			return element.getAsDouble();
		} catch (RuntimeException exception) {
			return fallback;
		}
	}

	private static Object ensureLevelsPlayerState(ServerPlayer player) {
		initializeReflection();
		if (reflectionFailed || ensurePlayerStateMethod == null || player == null) {
			return null;
		}

		try {
			return ensurePlayerStateMethod.invoke(null, player);
		} catch (ReflectiveOperationException exception) {
			LOGGER.warn("Failed to access MadokuLevels player state for Levels/Attributes compat.", exception);
			reflectionFailed = true;
			return null;
		}
	}

	private static int getAvailablePoints(Object levelsState) {
		initializeReflection();
		if (reflectionFailed || levelsAvailablePointsField == null || levelsState == null) {
			return 0;
		}

		try {
			return levelsAvailablePointsField.getInt(levelsState);
		} catch (ReflectiveOperationException exception) {
			LOGGER.warn("Failed to read MadokuLevels available points for Levels/Attributes compat.", exception);
			reflectionFailed = true;
			return 0;
		}
	}

	private static void setAvailablePoints(Object levelsState, int availablePoints) {
		initializeReflection();
		if (reflectionFailed || levelsAvailablePointsField == null || levelsState == null) {
			return;
		}

		try {
			levelsAvailablePointsField.setInt(levelsState, Math.max(0, availablePoints));
		} catch (ReflectiveOperationException exception) {
			LOGGER.warn("Failed to update MadokuLevels available points for Levels/Attributes compat.", exception);
			reflectionFailed = true;
		}
	}

	@SuppressWarnings("unchecked")
	private static void markDirty(UUID playerId) {
		initializeReflection();
		if (reflectionFailed || levelsDirtyPlayersField == null || playerId == null) {
			return;
		}

		try {
			Set<UUID> dirtyPlayers = (Set<UUID>) levelsDirtyPlayersField.get(null);
			if (dirtyPlayers != null) {
				dirtyPlayers.add(playerId);
			}
		} catch (ReflectiveOperationException exception) {
			LOGGER.warn("Failed to mark MadokuLevels payload dirty for Levels/Attributes compat.", exception);
			reflectionFailed = true;
		}
	}

	@SuppressWarnings("unchecked")
	private static void markAllPlayersDirty(MinecraftServer server) {
		initializeReflection();
		if (reflectionFailed || levelsDirtyPlayersField == null || server == null) {
			return;
		}

		try {
			Set<UUID> dirtyPlayers = (Set<UUID>) levelsDirtyPlayersField.get(null);
			if (dirtyPlayers == null) {
				return;
			}
			server.getPlayerList().getPlayers().forEach(player -> dirtyPlayers.add(player.getUUID()));
		} catch (ReflectiveOperationException exception) {
			LOGGER.warn("Failed to mark existing players dirty for Levels/Attributes compat.", exception);
			reflectionFailed = true;
		}
	}

	private static void initializeReflection() {
		if (reflectionInitialized || reflectionFailed) {
			return;
		}

		try {
			Class<?> levelsClass = Class.forName("madoku.craft.levels.MadokuLevels");
			levelsDirtyPlayersField = levelsClass.getDeclaredField("DIRTY_PLAYERS");
			levelsDirtyPlayersField.setAccessible(true);
			ensurePlayerStateMethod = levelsClass.getDeclaredMethod("ensurePlayerState", ServerPlayer.class);
			ensurePlayerStateMethod.setAccessible(true);

			Class<?> playerStateClass = null;
			for (Class<?> nestedClass : levelsClass.getDeclaredClasses()) {
				if (nestedClass.getSimpleName().equals("PlayerState")) {
					playerStateClass = nestedClass;
					break;
				}
			}
			if (playerStateClass == null) {
				throw new IllegalStateException("MadokuLevels$PlayerState not found.");
			}
			levelsAvailablePointsField = playerStateClass.getDeclaredField("availablePoints");
			levelsAvailablePointsField.setAccessible(true);
			reflectionInitialized = true;
		} catch (ReflectiveOperationException | RuntimeException exception) {
			LOGGER.warn("Failed to initialize Levels/Attributes compat reflection hooks.", exception);
			reflectionFailed = true;
		}
	}

	private static void notifyHungerCapChanged(ServerPlayer player) {
		if (player == null) {
			return;
		}
		try {
			Class<?> hungerClass = Class.forName("madoku.craft.hunger.MadokuHunger");
			Method method = hungerClass.getDeclaredMethod("handleMaximumHungerChanged", ServerPlayer.class);
			method.setAccessible(true);
			method.invoke(null, player);
			return;
		} catch (NoSuchMethodException ignored) {
			refreshHungerStateFallback(player);
		} catch (ReflectiveOperationException ignored) {
			refreshHungerStateFallback(player);
		}
	}

	@SuppressWarnings("unchecked")
	private static void refreshHungerStateFallback(ServerPlayer player) {
		initializeHungerReflection();
		if (hungerReflectionFailed || player == null) {
			return;
		}

		try {
			Map<UUID, Object> playerStates = (Map<UUID, Object>) hungerPlayerStatesField.get(null);
			if (playerStates == null) {
				return;
			}

			Object state = playerStates.get(player.getUUID());
			if (state == null) {
				state = hungerPlayerStateConstructor.newInstance();
				playerStates.put(player.getUUID(), state);
			}

			int configuredMaximum = ((Number) hungerGetConfiguredMaximumMethod.invoke(null)).intValue();
			int maximumHungerPoints = Math.max(1, configuredMaximum + Math.max(0, getPlayerHungerBonusPoints(player)));
			hungerInitializePlayerStateMethod.invoke(null, player, state, maximumHungerPoints);
			hungerLastSyncedCurrentField.setInt(state, Integer.MIN_VALUE);
			hungerLastSyncedPendingField.setInt(state, Integer.MIN_VALUE);
			hungerLastSyncedMaxField.setInt(state, Integer.MIN_VALUE);

			int hungerPoints = Math.max(0, hungerPointsField.getInt(state));
			if (hungerApplyFoodStateMethod.getParameterCount() == 3) {
				hungerApplyFoodStateMethod.invoke(null, player, hungerPoints, maximumHungerPoints);
			} else {
				hungerApplyFoodStateMethod.invoke(null, player, hungerPoints);
			}
			hungerSyncHudStateMethod.invoke(null, player, state, maximumHungerPoints);
		} catch (ReflectiveOperationException | RuntimeException exception) {
			LOGGER.warn("Failed to refresh MadokuHunger state for Levels/Attributes compat.", exception);
			hungerReflectionFailed = true;
		}
	}

	private static void initializeHungerReflection() {
		if (hungerReflectionInitialized || hungerReflectionFailed) {
			return;
		}

		try {
			Class<?> hungerClass = Class.forName("madoku.craft.hunger.MadokuHunger");
			Class<?> playerStateClass = null;
			for (Class<?> nestedClass : hungerClass.getDeclaredClasses()) {
				if (nestedClass.getSimpleName().equals("PlayerState")) {
					playerStateClass = nestedClass;
					break;
				}
			}
			if (playerStateClass == null) {
				throw new IllegalStateException("MadokuHunger$PlayerState not found.");
			}

			hungerPlayerStatesField = hungerClass.getDeclaredField("PLAYER_STATES");
			hungerPlayerStatesField.setAccessible(true);
			hungerPlayerStateConstructor = playerStateClass.getDeclaredConstructor();
			hungerPlayerStateConstructor.setAccessible(true);
			hungerGetConfiguredMaximumMethod = hungerClass.getDeclaredMethod("getConfiguredMaximumHungerPoints");
			hungerGetConfiguredMaximumMethod.setAccessible(true);
			hungerInitializePlayerStateMethod = hungerClass.getDeclaredMethod("initializeHungerFromPlayer", ServerPlayer.class, playerStateClass, int.class);
			hungerInitializePlayerStateMethod.setAccessible(true);
			try {
				hungerApplyFoodStateMethod = hungerClass.getDeclaredMethod("applyFoodState", ServerPlayer.class, int.class, int.class);
			} catch (NoSuchMethodException ignored) {
				hungerApplyFoodStateMethod = hungerClass.getDeclaredMethod("applyFoodState", ServerPlayer.class, int.class);
			}
			hungerApplyFoodStateMethod.setAccessible(true);
			hungerSyncHudStateMethod = hungerClass.getDeclaredMethod("syncHudState", ServerPlayer.class, playerStateClass, int.class);
			hungerSyncHudStateMethod.setAccessible(true);

			hungerPointsField = playerStateClass.getDeclaredField("hungerPoints");
			hungerPointsField.setAccessible(true);
			hungerLastSyncedCurrentField = playerStateClass.getDeclaredField("lastSyncedCurrentHunger");
			hungerLastSyncedCurrentField.setAccessible(true);
			hungerLastSyncedPendingField = playerStateClass.getDeclaredField("lastSyncedPendingHunger");
			hungerLastSyncedPendingField.setAccessible(true);
			hungerLastSyncedMaxField = playerStateClass.getDeclaredField("lastSyncedMaxHunger");
			hungerLastSyncedMaxField.setAccessible(true);

			hungerReflectionInitialized = true;
		} catch (ReflectiveOperationException | RuntimeException exception) {
			LOGGER.warn("Failed to initialize MadokuHunger reflection hooks for Levels/Attributes compat.", exception);
			hungerReflectionFailed = true;
		}
	}

	private static final class ExtraPlayerState {
		private int luckLevel;
		private int hungerLevel;

		private ExtraPlayerState(int luckLevel, int hungerLevel) {
			this.luckLevel = luckLevel;
			this.hungerLevel = hungerLevel;
		}
	}
}
