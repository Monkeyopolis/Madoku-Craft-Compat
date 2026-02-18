package madoku.craft.compat.mixin.hunger;

import madoku.craft.Hunger.system.HungerConfig;
import madoku.craft.Hunger.system.HungerFeature;
import madoku.craft.Hunger.system.PlayerHungerData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = HungerFeature.class, remap = false)
public interface HungerFeatureAccessor {
	@Accessor("config")
	HungerConfig madokuCompat$getConfig();

	@Invoker("getPlayerData")
	PlayerHungerData madokuCompat$getPlayerData(ServerPlayerEntity player);

	@Invoker("calculateMaxHunger")
	int madokuCompat$calculateMaxHunger(ServerPlayerEntity player);

	@Invoker("toHungerUnits")
	int madokuCompat$toHungerUnits(int points);

	@Invoker("markDirty")
	void madokuCompat$markDirty();
}
