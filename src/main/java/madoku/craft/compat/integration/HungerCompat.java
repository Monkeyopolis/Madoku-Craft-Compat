package madoku.craft.compat.integration;

import madoku.craft.Health.system.MadokuHealthManager;
import madoku.craft.Hunger.system.HungerFeature;
import madoku.craft.Hunger.system.PlayerHungerData;
import madoku.craft.compat.mixin.hunger.HungerFeatureAccessor;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HungerCompat {
	private static final ThreadLocal<Boolean> HEALTH_FOOD_CALL =
			ThreadLocal.withInitial(() -> Boolean.FALSE);

	private HungerCompat() {
	}

	public static boolean syncVanillaFoodLevel(ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}
		HungerFeature feature = HungerFeature.getInstance();
		if (feature == null) {
			return false;
		}
		HungerFeatureAccessor accessor = (HungerFeatureAccessor) (Object) feature;
		PlayerHungerData data = accessor.madokuCompat$getPlayerData(player);
		if (data == null) {
			return false;
		}
		int max = accessor.madokuCompat$calculateMaxHunger(player);
		int foodLevel = toVanillaFoodLevel(data.currentHungerPoints, max);
		applyVanillaFoodLevel(player, foodLevel);
		return true;
	}

	public static boolean drainHunger(ServerPlayerEntity player, int amount) {
		if (player == null || amount <= 0) {
			return false;
		}
		HungerFeature feature = HungerFeature.getInstance();
		if (feature == null) {
			return false;
		}
		HungerFeatureAccessor accessor = (HungerFeatureAccessor) (Object) feature;
		PlayerHungerData data = accessor.madokuCompat$getPlayerData(player);
		if (data == null) {
			return false;
		}
		int hungerUnits = accessor.madokuCompat$toHungerUnits(amount);
		if (hungerUnits <= 0 || data.currentHungerPoints <= 0) {
			return false;
		}
		int drained = Math.min(hungerUnits, data.currentHungerPoints);
		data.currentHungerPoints -= drained;
		if (data.queuedHungerDepletionPoints > data.currentHungerPoints) {
			data.queuedHungerDepletionPoints = data.currentHungerPoints;
		}
		accessor.madokuCompat$markDirty();
		syncVanillaFoodLevel(player);
		return true;
	}

	public static boolean addPendingHealthFromFood(ServerPlayerEntity player, double amount) {
		MadokuHealthManager manager = MadokuHealthManager.getInstance();
		if (manager == null || !manager.isFeatureEnabled()) {
			return false;
		}
		Boolean previous = HEALTH_FOOD_CALL.get();
		HEALTH_FOOD_CALL.set(Boolean.TRUE);
		try {
			return manager.addPendingFromFood(player, amount);
		} finally {
			HEALTH_FOOD_CALL.set(previous);
		}
	}

	public static boolean isHealthFoodBridgeActive() {
		return HEALTH_FOOD_CALL.get();
	}

	private static void applyVanillaFoodLevel(ServerPlayerEntity player, int foodLevel) {
		HungerFeature.beginVanillaSync();
		try {
			HungerManager manager = player.getHungerManager();
			manager.setFoodLevel(foodLevel);
			manager.setSaturationLevel(0.0f);
		} finally {
			HungerFeature.endVanillaSync();
		}
	}

	private static int toVanillaFoodLevel(int current, int max) {
		if (max <= 0) {
			return 0;
		}
		float ratio = (float) current / (float) max;
		int level = Math.round(ratio * 20.0f);
		if (level < 0) {
			return 0;
		}
		if (level > 20) {
			return 20;
		}
		return level;
	}
}
