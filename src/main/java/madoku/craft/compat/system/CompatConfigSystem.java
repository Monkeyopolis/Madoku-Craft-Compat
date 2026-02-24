package madoku.craft.compat.system;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import madoku.craft.API.system.MadokuJSONSystem;

public final class CompatConfigSystem {
	private static final String FEATURE_ID = "madoku_craft_compat";
	private static final String JSON_FOLDER_ID = "Compat";

	private static final String KEY_TOOLS_ROOT = "madoku_craft_tools";
	private static final String KEY_TOOLS_ARMOR_DEFAULTS_MIGRATION_PENDING = "armor_defaults_migration_pending";

	private static final String KEY_COMPAT_MOBS_ROOT = "madoku_craft_compat_mobs";
	private static final String KEY_DIFFICULTY_SCALING = "difficulty_scaling";
	private static final String KEY_CREEPER = "creeper";
	private static final String KEY_SPIDER = "spider";
	private static final String KEY_SKELETON = "skeleton";
	private static final String KEY_CREEPER_FUSE_LENGTH_STEP = "fuse_length";
	private static final String KEY_CREEPER_EXPLOSION_DESTRUCTION_CHANCE_STEP = "explosion_destruction_chance";
	private static final String KEY_SPIDER_SCALE_STEP = "scale";
	private static final String KEY_SKELETON_ATTACK_INTERVAL_STEP = "attack_interval";
	private static final String KEY_SKELETON_RANGED_ATTACK_STEP = "ranged_attack";
	private static final String KEY_SKELETON_ATTACK_ACCURACY_STEP = "attack_accuracy";

	private static final double DEFAULT_CREEPER_FUSE_LENGTH_STEP = 0.05d;
	private static final double DEFAULT_CREEPER_EXPLOSION_DESTRUCTION_CHANCE_STEP = 0.025d;
	private static final double DEFAULT_SPIDER_SCALE_STEP = 0.025d;
	private static final double DEFAULT_SKELETON_ATTACK_INTERVAL_STEP = 0.5d;
	private static final double DEFAULT_SKELETON_RANGED_ATTACK_STEP = 0.5d;
	private static final double DEFAULT_SKELETON_ATTACK_ACCURACY_STEP = 0.025d;

	private static volatile Snapshot snapshot = Snapshot.defaults();
	private static volatile boolean initialized;

	private CompatConfigSystem() {
	}

	public static synchronized void init() {
		JsonObject defaults = buildDefaults();
		MadokuJSONSystem.ManagedJSON config = MadokuJSONSystem.load(JSON_FOLDER_ID, FEATURE_ID, defaults);
		SettingsLoadResult load = readSettings(config.getRoot());
		if (load.changed()) {
			config.save();
		}
		snapshot = load.snapshot();
		initialized = true;
	}

	public static Snapshot get() {
		if (!initialized) {
			init();
		}
		return snapshot;
	}

	public static boolean isToolsArmorDefaultsMigrationPending() {
		return get().tools().armorDefaultsMigrationPending();
	}

	public static synchronized void markToolsArmorDefaultsMigrationCompleted() {
		if (!isToolsArmorDefaultsMigrationPending()) {
			return;
		}

		JsonObject defaults = buildDefaults();
		MadokuJSONSystem.ManagedJSON config = MadokuJSONSystem.load(JSON_FOLDER_ID, FEATURE_ID, defaults);
		JsonObject root = config.getRoot();
		JsonObject toolsRoot = getOrCreateObject(root, KEY_TOOLS_ROOT);
		if (setBoolean(toolsRoot, KEY_TOOLS_ARMOR_DEFAULTS_MIGRATION_PENDING, false)) {
			config.save();
		}

		Snapshot current = snapshot;
		snapshot = new Snapshot(current.mobCompatScaling(), new ToolsSettings(false));
		initialized = true;
	}

	private static JsonObject buildDefaults() {
		JsonObject root = new JsonObject();

		JsonObject toolsRoot = new JsonObject();
		toolsRoot.addProperty(KEY_TOOLS_ARMOR_DEFAULTS_MIGRATION_PENDING, true);
		root.add(KEY_TOOLS_ROOT, toolsRoot);

		JsonObject compatMobsRoot = new JsonObject();
		JsonObject scalingRoot = new JsonObject();

		JsonObject creeper = new JsonObject();
		creeper.addProperty(KEY_CREEPER_FUSE_LENGTH_STEP, DEFAULT_CREEPER_FUSE_LENGTH_STEP);
		creeper.addProperty(
				KEY_CREEPER_EXPLOSION_DESTRUCTION_CHANCE_STEP,
				DEFAULT_CREEPER_EXPLOSION_DESTRUCTION_CHANCE_STEP);
		scalingRoot.add(KEY_CREEPER, creeper);

		JsonObject spider = new JsonObject();
		spider.addProperty(KEY_SPIDER_SCALE_STEP, DEFAULT_SPIDER_SCALE_STEP);
		scalingRoot.add(KEY_SPIDER, spider);

		JsonObject skeleton = new JsonObject();
		skeleton.addProperty(KEY_SKELETON_ATTACK_INTERVAL_STEP, DEFAULT_SKELETON_ATTACK_INTERVAL_STEP);
		skeleton.addProperty(KEY_SKELETON_RANGED_ATTACK_STEP, DEFAULT_SKELETON_RANGED_ATTACK_STEP);
		skeleton.addProperty(KEY_SKELETON_ATTACK_ACCURACY_STEP, DEFAULT_SKELETON_ATTACK_ACCURACY_STEP);
		scalingRoot.add(KEY_SKELETON, skeleton);

		compatMobsRoot.add(KEY_DIFFICULTY_SCALING, scalingRoot);
		root.add(KEY_COMPAT_MOBS_ROOT, compatMobsRoot);

		return root;
	}

	private static SettingsLoadResult readSettings(JsonObject root) {
		boolean changed = false;

		JsonObject toolsRoot = getOrCreateObject(root, KEY_TOOLS_ROOT);
		JsonObject compatMobsRoot = getOrCreateObject(root, KEY_COMPAT_MOBS_ROOT);
		JsonObject scalingRoot = getOrCreateObject(compatMobsRoot, KEY_DIFFICULTY_SCALING);
		JsonObject creeper = getOrCreateObject(scalingRoot, KEY_CREEPER);
		JsonObject spider = getOrCreateObject(scalingRoot, KEY_SPIDER);
		JsonObject skeleton = getOrCreateObject(scalingRoot, KEY_SKELETON);

		boolean toolsArmorDefaultsMigrationPending = readBoolean(
				toolsRoot,
				KEY_TOOLS_ARMOR_DEFAULTS_MIGRATION_PENDING,
				true);
		changed |= setBoolean(
				toolsRoot,
				KEY_TOOLS_ARMOR_DEFAULTS_MIGRATION_PENDING,
				toolsArmorDefaultsMigrationPending);

		double creeperFuseStep = sanitizeNonNegative(
				readDouble(creeper, KEY_CREEPER_FUSE_LENGTH_STEP, DEFAULT_CREEPER_FUSE_LENGTH_STEP),
				DEFAULT_CREEPER_FUSE_LENGTH_STEP);
		double creeperExplosionDestructionChanceStep = sanitizeNonNegative(
				readDouble(
						creeper,
						KEY_CREEPER_EXPLOSION_DESTRUCTION_CHANCE_STEP,
						DEFAULT_CREEPER_EXPLOSION_DESTRUCTION_CHANCE_STEP),
				DEFAULT_CREEPER_EXPLOSION_DESTRUCTION_CHANCE_STEP);
		double spiderScaleStep = sanitizeNonNegative(
				readDouble(spider, KEY_SPIDER_SCALE_STEP, DEFAULT_SPIDER_SCALE_STEP),
				DEFAULT_SPIDER_SCALE_STEP);
		double skeletonAttackIntervalStep = sanitizeNonNegative(
				readDouble(skeleton, KEY_SKELETON_ATTACK_INTERVAL_STEP, DEFAULT_SKELETON_ATTACK_INTERVAL_STEP),
				DEFAULT_SKELETON_ATTACK_INTERVAL_STEP);
		double skeletonRangedAttackStep = sanitizeNonNegative(
				readDouble(skeleton, KEY_SKELETON_RANGED_ATTACK_STEP, DEFAULT_SKELETON_RANGED_ATTACK_STEP),
				DEFAULT_SKELETON_RANGED_ATTACK_STEP);
		double skeletonAttackAccuracyStep = sanitizeNonNegative(
				readDouble(skeleton, KEY_SKELETON_ATTACK_ACCURACY_STEP, DEFAULT_SKELETON_ATTACK_ACCURACY_STEP),
				DEFAULT_SKELETON_ATTACK_ACCURACY_STEP);

		changed |= setDouble(creeper, KEY_CREEPER_FUSE_LENGTH_STEP, creeperFuseStep);
		changed |= setDouble(
				creeper,
				KEY_CREEPER_EXPLOSION_DESTRUCTION_CHANCE_STEP,
				creeperExplosionDestructionChanceStep);
		changed |= setDouble(spider, KEY_SPIDER_SCALE_STEP, spiderScaleStep);
		changed |= setDouble(skeleton, KEY_SKELETON_ATTACK_INTERVAL_STEP, skeletonAttackIntervalStep);
		changed |= setDouble(skeleton, KEY_SKELETON_RANGED_ATTACK_STEP, skeletonRangedAttackStep);
		changed |= setDouble(skeleton, KEY_SKELETON_ATTACK_ACCURACY_STEP, skeletonAttackAccuracyStep);

		return new SettingsLoadResult(
				new Snapshot(
						new MobCompatScalingSettings(
								creeperFuseStep,
								creeperExplosionDestructionChanceStep,
								spiderScaleStep,
								skeletonAttackIntervalStep,
								skeletonRangedAttackStep,
								skeletonAttackAccuracyStep),
						new ToolsSettings(toolsArmorDefaultsMigrationPending)),
				changed);
	}

	private static JsonObject getOrCreateObject(JsonObject parent, String key) {
		JsonElement existing = parent.get(key);
		if (existing instanceof JsonObject object) {
			return object;
		}
		JsonObject created = new JsonObject();
		parent.add(key, created);
		return created;
	}

	private static double readDouble(JsonObject root, String key, double fallback) {
		JsonElement element = root.get(key);
		if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
			double value = primitive.getAsDouble();
			if (Double.isFinite(value)) {
				return value;
			}
		}
		return fallback;
	}

	private static boolean setDouble(JsonObject root, String key, double value) {
		JsonElement element = root.get(key);
		if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
			if (Double.compare(primitive.getAsDouble(), value) == 0) {
				return false;
			}
		}
		root.addProperty(key, value);
		return true;
	}

	private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
		JsonElement element = root.get(key);
		if (element instanceof JsonPrimitive primitive && primitive.isBoolean()) {
			return primitive.getAsBoolean();
		}
		return fallback;
	}

	private static boolean setBoolean(JsonObject root, String key, boolean value) {
		JsonElement element = root.get(key);
		if (element instanceof JsonPrimitive primitive && primitive.isBoolean()) {
			if (primitive.getAsBoolean() == value) {
				return false;
			}
		}
		root.addProperty(key, value);
		return true;
	}

	private static double sanitizeNonNegative(double value, double fallback) {
		return Double.isFinite(value) && value >= 0.0d ? value : fallback;
	}

	public record Snapshot(MobCompatScalingSettings mobCompatScaling, ToolsSettings tools) {
		private static Snapshot defaults() {
			return new Snapshot(
					new MobCompatScalingSettings(
							DEFAULT_CREEPER_FUSE_LENGTH_STEP,
							DEFAULT_CREEPER_EXPLOSION_DESTRUCTION_CHANCE_STEP,
							DEFAULT_SPIDER_SCALE_STEP,
							DEFAULT_SKELETON_ATTACK_INTERVAL_STEP,
							DEFAULT_SKELETON_RANGED_ATTACK_STEP,
							DEFAULT_SKELETON_ATTACK_ACCURACY_STEP),
					new ToolsSettings(true));
		}
	}

	public record MobCompatScalingSettings(
			double creeperFuseLengthAdjustmentStep,
			double creeperExplosionDestructionChanceAdjustmentStep,
			double spiderScaleAdjustmentStep,
			double skeletonAttackIntervalAdjustmentStep,
			double skeletonRangedAttackAdjustmentStep,
			double skeletonAttackAccuracyAdjustmentStep) {
	}

	public record ToolsSettings(boolean armorDefaultsMigrationPending) {
	}

	private record SettingsLoadResult(Snapshot snapshot, boolean changed) {
	}
}
