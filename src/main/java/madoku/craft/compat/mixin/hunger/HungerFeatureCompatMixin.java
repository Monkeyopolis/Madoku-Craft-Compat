package madoku.craft.compat.mixin.hunger;

import madoku.craft.Hunger.system.HungerConfig;
import madoku.craft.Hunger.system.HungerFeature;
import madoku.craft.Hunger.system.PlayerHungerData;
import madoku.craft.compat.integration.HungerCompat;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HungerFeature.class, remap = false)
public class HungerFeatureCompatMixin {
	@Inject(method = "onFoodConsumed", at = @At("HEAD"))
	private void madokuCompat$onFoodConsumed(ServerPlayerEntity player, FoodComponent food,
			CallbackInfo ci) {
		if (player == null || food == null) {
			return;
		}
		int nutrition = food.nutrition();
		if (nutrition <= 0) {
			return;
		}

		HungerFeatureAccessor accessor = (HungerFeatureAccessor) this;
		HungerConfig config = accessor.madokuCompat$getConfig();
		if (config == null || !config.isFeatureEnabled()) {
			return;
		}

		PlayerHungerData data = accessor.madokuCompat$getPlayerData(player);
		if (data == null) {
			return;
		}

		int max = accessor.madokuCompat$toHungerUnits(config.maximumHungerPoints);
		int availableUnits = Math.max(0, max - data.currentHungerPoints - data.pendingHungerPoints);
		int unitsPerNutrition = Math.max(1, accessor.madokuCompat$toHungerUnits(1));
		int added = Math.min(nutrition, availableUnits / unitsPerNutrition);
		if (added > 0) {
			HungerCompat.addPendingHealthFromFood(player, added);
		}
	}
}
