package madoku.craft.compat.mixin;

import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class MadokuCompatMixinPlugin implements IMixinConfigPlugin {
	private boolean hudLoaded;
	private boolean attributesLoaded;
	private boolean mobsLoaded;
	private boolean itemsLoaded;
	private boolean smeltingLoaded;
	private boolean farmingLoaded;
	private boolean levelsLoaded;
	private boolean petsLoaded;

	@Override
	public void onLoad(String mixinPackage) {
		FabricLoader loader = FabricLoader.getInstance();
		hudLoaded = loader.isModLoaded("madoku-craft-hud");
		attributesLoaded = loader.isModLoaded("madoku-craft-attributes");
		mobsLoaded = loader.isModLoaded("madoku-craft-mobs");
		itemsLoaded = loader.isModLoaded("madoku-craft-items");
		smeltingLoaded = loader.isModLoaded("madoku-craft-smelting");
		farmingLoaded = loader.isModLoaded("madoku-craft-farming");
		levelsLoaded = loader.isModLoaded("madoku-craft-levels");
		petsLoaded = loader.isModLoaded("madoku-craft-pets");
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.endsWith("HungerHudSyncAttributesCompatMixin")) {
			return hudLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("HungerHudAttributesCompatMixin")) {
			return hudLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("ItemFoodAttributesCompatMixin")) {
			return hudLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("OxygenHudAttributesCompatMixin")) {
			return hudLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("PlayerEatAttributesCompatMixin")) {
			return hudLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("MadokuLuckFarmingCompatMixin")) {
			return farmingLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("WorldHudMobsDifficultyCompatMixin")) {
			return hudLoaded && mobsLoaded;
		}
		if (mixinClassName.endsWith("MadokuLuckMobsConfigCompatMixin")) {
			return attributesLoaded && mobsLoaded;
		}
		if (mixinClassName.endsWith("MadokuMobAttributesLuckCompatMixin")) {
			return attributesLoaded && mobsLoaded;
		}
		if (mixinClassName.endsWith("MadokuMobConfigPetsHagCompatMixin")) {
			return petsLoaded && mobsLoaded;
		}
		if (mixinClassName.endsWith("MadokuDifficultyConfigPetsHagCompatMixin")) {
			return petsLoaded && mobsLoaded;
		}
		if (mixinClassName.endsWith("MadokuMobPetsHagCompatMixin")) {
			return petsLoaded && mobsLoaded;
		}
		if (mixinClassName.endsWith("FurnaceFuelItemsSmeltingCompatMixin")) {
			return itemsLoaded && smeltingLoaded;
		}
		if (mixinClassName.endsWith("FuelValuesItemsSmeltingCompatMixin")) {
			return itemsLoaded && smeltingLoaded;
		}
		if (mixinClassName.endsWith("FarmingItemsMetadataCompatMixin")) {
			return itemsLoaded && farmingLoaded;
		}
		if (mixinClassName.endsWith("BlockFarmingAttributesCompatMixin")) {
			return farmingLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("LevelsAttributesConfigCompatMixin")) {
			return levelsLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("LevelsAttributesExtraStatsCompatMixin")) {
			return levelsLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("HungerLevelsAttributesCompatMixin")) {
			return levelsLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("LevelsClientOpenAttributesCompatMixin")) {
			return levelsLoaded && attributesLoaded;
		}
		if (mixinClassName.endsWith("LevelsClientStateAttributesCompatMixin")) {
			return levelsLoaded && attributesLoaded;
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
