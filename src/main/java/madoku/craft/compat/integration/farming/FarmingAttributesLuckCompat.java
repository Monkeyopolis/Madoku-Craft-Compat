package madoku.craft.compat.integration.farming;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import madoku.craft.luck.MadokuLuck;
import madoku.craft.farming.system.MadokuFarming;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public final class FarmingAttributesLuckCompat {
	private static final Logger LOGGER = LoggerFactory.getLogger(FarmingAttributesLuckCompat.class);

	private static final double FARMING_DROP_MULTIPLIER = 0.5d;
	private static final double DEFAULT_BASE_LUCK = 0.05d;

	private static final ThreadLocal<BlockDropScaleState> BLOCK_DROP_SCALE_STATE = new ThreadLocal<>();

	private static volatile Field activeDropContextField;
	private static volatile Field luckSettingsField;

	private FarmingAttributesLuckCompat() {
	}

	public static boolean applyManagedCropLootDrops(LootContext lootContext, ObjectArrayList<ItemStack> stacks) {
		if (!MadokuLuck.isEnabled() || lootContext == null || stacks == null || stacks.isEmpty()) {
			return false;
		}

		ActiveContext context = resolveActiveContext();
		if (!isManagedCropContext(context)) {
			return false;
		}

		if (context.player().isCreative()) {
			return true;
		}

		double dropChance = resolveLuckProcChance(context.player());
		if (dropChance <= 0.0d) {
			return true;
		}

		RandomSource random = lootContext.getRandom();
		double roll = random == null ? 1.0d : random.nextDouble();
		if (roll >= dropChance) {
			return true;
		}

		scaleStacks(stacks, FARMING_DROP_MULTIPLIER, random);
		return true;
	}

	public static ItemStack applyManagedCropBlockDrop(Level level, BlockPos pos, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return stack;
		}
		if (!(level instanceof ServerLevel serverLevel) || pos == null || !MadokuLuck.isEnabled()) {
			clearBlockDropScaleState();
			return stack;
		}

		ActiveContext context = resolveActiveContext();
		if (!isManagedCropContext(context) || !samePosition(context, serverLevel, pos)) {
			clearBlockDropScaleState();
			return stack;
		}

		if (context.player().isCreative()) {
			cacheBlockDropScaleState(serverLevel, pos, false);
			return stack;
		}

		BlockDropScaleState scaleState = BLOCK_DROP_SCALE_STATE.get();
		if (scaleState == null || !scaleState.matches(serverLevel, pos)) {
			double dropChance = resolveLuckProcChance(context.player());
			boolean applyBonus = false;
			if (dropChance > 0.0d) {
				RandomSource random = serverLevel.getRandom();
				double roll = random == null ? 1.0d : random.nextDouble();
				applyBonus = roll < dropChance;
			}
			scaleState = cacheBlockDropScaleState(serverLevel, pos, applyBonus);
		}

		if (!scaleState.applyBonus()) {
			return stack;
		}

		int extraCount = calculateExtraCount(stack.getCount(), FARMING_DROP_MULTIPLIER, serverLevel.getRandom());
		if (extraCount > 0) {
			stack.grow(extraCount);
		}
		return stack;
	}

	private static BlockDropScaleState cacheBlockDropScaleState(ServerLevel level, BlockPos pos, boolean applyBonus) {
		BlockDropScaleState state = new BlockDropScaleState(level.dimension().toString(), pos.immutable(), applyBonus);
		BLOCK_DROP_SCALE_STATE.set(state);
		return state;
	}

	private static void clearBlockDropScaleState() {
		BLOCK_DROP_SCALE_STATE.remove();
	}

	private static boolean samePosition(ActiveContext context, ServerLevel level, BlockPos pos) {
		return context != null
			&& context.level() == level
			&& context.pos() != null
			&& context.pos().equals(pos);
	}

	private static boolean isManagedCropContext(ActiveContext context) {
		return context != null
			&& context.level() != null
			&& context.player() != null
			&& context.pos() != null
			&& context.state() != null
			&& MadokuFarming.isManagedCrop(context.level(), context.pos(), context.state())
			&& MadokuFarming.isCropHarvestReady(context.level(), context.pos(), context.state());
	}

	private static ActiveContext resolveActiveContext() {
		try {
			Field field = activeDropContextField;
			if (field == null) {
				field = MadokuLuck.class.getDeclaredField("ACTIVE_DROP_CONTEXT");
				field.setAccessible(true);
				activeDropContextField = field;
			}

			Object threadLocal = field.get(null);
			if (!(threadLocal instanceof ThreadLocal<?> activeContextThreadLocal)) {
				return null;
			}

			Object context = activeContextThreadLocal.get();
			if (context == null) {
				return null;
			}

			ServerLevel level = (ServerLevel) readField(context, "level");
			ServerPlayer player = (ServerPlayer) readField(context, "player");
			BlockPos pos = (BlockPos) readField(context, "pos");
			BlockState state = (BlockState) readField(context, "state");
			if (level == null || player == null || pos == null || state == null) {
				return null;
			}
			return new ActiveContext(level, player, pos, state);
		} catch (ReflectiveOperationException | RuntimeException exception) {
			LOGGER.warn("Failed to resolve MadokuLuck active block-drop context for Farming compat.", exception);
			return null;
		}
	}

	private static Object readField(Object target, String fieldName) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}

	private static double resolveLuckProcChance(ServerPlayer player) {
		return Mth.clamp(resolveLuckValue(player), 0.0d, 1.0d);
	}

	private static double resolveLuckValue(ServerPlayer player) {
		AttributeInstance luckAttribute = player == null ? null : player.getAttribute(Attributes.LUCK);
		double baseLuck = resolveBaseLuck();
		double luckValue = luckAttribute == null ? baseLuck : luckAttribute.getValue();
		return Double.isFinite(luckValue) ? luckValue : baseLuck;
	}

	private static double resolveBaseLuck() {
		try {
			Field settings = luckSettingsField;
			if (settings == null) {
				settings = MadokuLuck.class.getDeclaredField("settings");
				settings.setAccessible(true);
				luckSettingsField = settings;
			}

			Object settingsObject = settings.get(null);
			if (settingsObject == null) {
				return DEFAULT_BASE_LUCK;
			}

			Field baseLuckField = settingsObject.getClass().getDeclaredField("baseLuck");
			baseLuckField.setAccessible(true);
			double baseLuck = baseLuckField.getDouble(settingsObject);
			return Double.isFinite(baseLuck) ? baseLuck : DEFAULT_BASE_LUCK;
		} catch (ReflectiveOperationException | RuntimeException exception) {
			return DEFAULT_BASE_LUCK;
		}
	}

	private static int calculateExtraCount(int originalCount, double dropBonusMultiplier, RandomSource random) {
		if (originalCount <= 0 || dropBonusMultiplier <= 0.0d) {
			return 0;
		}

		double rawExtraCount = originalCount * dropBonusMultiplier;
		if (!Double.isFinite(rawExtraCount) || rawExtraCount <= 0.0d) {
			return 0;
		}

		int guaranteedExtraCount = (int) Math.floor(rawExtraCount);
		double fractionalExtraCount = rawExtraCount - guaranteedExtraCount;
		if (fractionalExtraCount > 0.0d && random != null && random.nextDouble() < fractionalExtraCount) {
			guaranteedExtraCount++;
		}
		return guaranteedExtraCount;
	}

	private static void scaleStacks(ObjectArrayList<ItemStack> stacks, double dropBonusMultiplier, RandomSource random) {
		for (ItemStack stack : stacks) {
			if (stack == null || stack.isEmpty()) {
				continue;
			}
			int extraCount = calculateExtraCount(stack.getCount(), dropBonusMultiplier, random);
			if (extraCount > 0) {
				stack.grow(extraCount);
			}
		}
	}

	private record ActiveContext(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
	}

	private record BlockDropScaleState(String dimensionId, BlockPos pos, boolean applyBonus) {
		private boolean matches(ServerLevel level, BlockPos otherPos) {
			return level != null
				&& otherPos != null
				&& dimensionId.equals(level.dimension().toString())
				&& pos.equals(otherPos);
		}
	}
}
