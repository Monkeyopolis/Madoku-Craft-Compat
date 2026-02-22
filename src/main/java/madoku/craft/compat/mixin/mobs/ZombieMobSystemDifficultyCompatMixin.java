package madoku.craft.compat.mixin.mobs;

import madoku.craft.compat.integration.mobs.MobsDifficultyAttributeBridge;
import madoku.craft.mobs.system.ZombieMobConfig;
import madoku.craft.mobs.system.ZombieMobSystem;
import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ZombieMobSystem.class, remap = false)
public class ZombieMobSystemDifficultyCompatMixin {
	@Inject(method = "applyConfig", at = @At("RETURN"))
	private static void madokuCompat$reapplyDifficultyBaseStats(
			ZombieEntity zombie,
			ZombieMobConfig.ZombieTypeConfig config,
			CallbackInfo ci
	) {
		MobsDifficultyAttributeBridge.reapplyDifficultyAttributes(zombie);
	}
}
