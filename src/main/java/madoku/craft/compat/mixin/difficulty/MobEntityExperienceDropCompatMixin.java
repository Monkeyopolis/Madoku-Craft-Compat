package madoku.craft.compat.mixin.difficulty;

import madoku.craft.difficulty.system.DifficultyScaledMob;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public class MobEntityExperienceDropCompatMixin {
	private static final double EXPERIENCE_SCALE_PER_ADJUSTMENT = 0.5d;

	@Shadow
	protected int experiencePoints;

	@Inject(method = "getExperienceToDrop", at = @At("RETURN"), cancellable = true)
	private void madokuCompat$scaleExperienceDropByDifficultyAdjustment(
			CallbackInfoReturnable<Integer> cir
	) {
		MobEntity self = (MobEntity) (Object) this;
		if (!(self instanceof DifficultyScaledMob scaledMob)) {
			return;
		}

		int adjustment = Math.max(0, scaledMob.madokuDifficulty$getSpawnAdjustment());
		if (adjustment <= 0) {
			return;
		}

		int currentDrop = cir.getReturnValueI();
		int mobsExperienceDrop = this.experiencePoints;
		if (currentDrop <= 0 || mobsExperienceDrop <= 0) {
			return;
		}

		double bonus = mobsExperienceDrop * (adjustment * EXPERIENCE_SCALE_PER_ADJUSTMENT);
		int scaledExperience = (int) Math.round(currentDrop + bonus);
		cir.setReturnValue(scaledExperience);
	}
}
