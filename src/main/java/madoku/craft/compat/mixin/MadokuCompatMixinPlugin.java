package madoku.craft.compat.mixin;

import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class MadokuCompatMixinPlugin implements IMixinConfigPlugin {
	private boolean hungerLoaded;
	private boolean healthLoaded;
	private boolean armorLoaded;
	private boolean toolsLoaded;
	private boolean hudLoaded;

	@Override
	public void onLoad(String mixinPackage) {
		FabricLoader loader = FabricLoader.getInstance();
		hungerLoaded = loader.isModLoaded("madoku-craft-hunger");
		healthLoaded = loader.isModLoaded("madoku-craft-health");
		armorLoaded = loader.isModLoaded("madoku-craft-armor");
		toolsLoaded = loader.isModLoaded("madoku-craft-tools");
		hudLoaded = loader.isModLoaded("madoku-craft-hud");
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.endsWith("HungerFeatureCompatMixin")) {
			return hungerLoaded && healthLoaded;
		}
		if (mixinClassName.endsWith("HungerFeatureAccessor")) {
			return hungerLoaded && healthLoaded;
		}
		if (mixinClassName.endsWith("MadokuHealthManagerCompatMixin")) {
			return hungerLoaded && healthLoaded;
		}
		if (mixinClassName.endsWith("CustomArmorSystemCompatMixin")) {
			return armorLoaded && toolsLoaded;
		}
		if (mixinClassName.endsWith("CustomToolsConfigCompatMixin")) {
			return armorLoaded && toolsLoaded;
		}
		if (mixinClassName.endsWith("ToolRarityManagerCompatMixin")) {
			return armorLoaded && toolsLoaded;
		}
		if (mixinClassName.endsWith("ClampedEntityAttributeAccessor")) {
			return armorLoaded && toolsLoaded;
		}
		if (mixinClassName.endsWith("HungerHudSystemCompatMixin")) {
			return hudLoaded && hungerLoaded;
		}
		if (mixinClassName.endsWith("HungerClientStateCompatMixin")) {
			return hudLoaded && hungerLoaded;
		}
		if (mixinClassName.endsWith("ArmorHudSystemCompatMixin")) {
			return hudLoaded && armorLoaded;
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
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
			IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
			IMixinInfo mixinInfo) {
	}
}
