package madoku.craft.java.compat.attributes;

import madoku.craft.java.attributes.LuckAPIManager;
import madoku.craft.java.attributes.LuckFeatureAdapter;
import madoku.craft.java.compat.MadokuCompatModuleState;
import madoku.craft.java.core.enchant.EnchantBooksAPIManager;
import madoku.craft.java.farming.FarmingAPIManager;
import madoku.craft.java.items.ItemsAPIManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Installs bridges between Attributes and other Madoku feature modules. */
public final class MadokuAttributeFeatureAdapters {
	private MadokuAttributeFeatureAdapters() { }

	public static void initialize() {
		madoku.craft.java.attributes.ArmorAPIManager.registerFeatureAdapter(new madoku.craft.java.attributes.ArmorFeatureAdapter() {
			@Override
			public double resolveBreachArmorEffectiveness(LivingEntity target, DamageSource source) {
				return EnchantBooksAPIManager.resolveBreachArmorEffectiveness(target, source);
			}

			@Override
			public double resolveArmorPointStep() {
				return isItemsEnabled() ? 0.10D : 0.15D;
			}

			private boolean isItemsEnabled() {
				return MadokuCompatModuleState.isLoaded(MadokuCompatModuleState.ITEMS_ID)
					&& ItemsAPIManager.isEnabled();
			}
		});
		LuckAPIManager.registerEnchantmentAdapter(EnchantBooksAPIManager::applyConfiguredFortune);
		if (!MadokuCompatModuleState.hasAll(
			MadokuCompatModuleState.ATTRIBUTES_ID,
			MadokuCompatModuleState.FARMING_ID
		)) {
			return;
		}
		LuckAPIManager.registerFeatureAdapter(new LuckFeatureAdapter() {
			@Override
			public boolean isManagedCrop(ServerLevel level, BlockPos pos, BlockState state) {
				return FarmingAPIManager.isManagedCrop(level, pos, state);
			}

			@Override
			public boolean isCropHarvestReady(ServerLevel level, BlockPos pos, BlockState state) {
				return FarmingAPIManager.isCropHarvestReady(level, pos, state);
			}
		});
	}
}
