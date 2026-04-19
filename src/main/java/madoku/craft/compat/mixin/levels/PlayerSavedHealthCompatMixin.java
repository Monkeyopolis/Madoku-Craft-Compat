package madoku.craft.compat.mixin.levels;

import madoku.craft.compat.access.PlayerSavedHealthAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerSavedHealthCompatMixin implements PlayerSavedHealthAccess {
	@Unique
	private static final String MADOKU_COMPAT_SAVED_HEALTH_KEY = "madoku_compat_saved_health_milli";

	@Unique
	private static final int MADOKU_COMPAT_UNSET_SAVED_HEALTH = Integer.MIN_VALUE;

	@Unique
	private int madokuCompat$savedHealthMilli = MADOKU_COMPAT_UNSET_SAVED_HEALTH;

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void madokuCompat$saveExactHealth(ValueOutput output, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		output.putInt(MADOKU_COMPAT_SAVED_HEALTH_KEY, Math.max(0, Math.round(player.getHealth() * 1000.0f)));
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void madokuCompat$loadExactHealth(ValueInput input, CallbackInfo ci) {
		int savedHealthMilli = input.getIntOr(MADOKU_COMPAT_SAVED_HEALTH_KEY, MADOKU_COMPAT_UNSET_SAVED_HEALTH);
		madokuCompat$savedHealthMilli = savedHealthMilli < 0 ? MADOKU_COMPAT_UNSET_SAVED_HEALTH : savedHealthMilli;
	}

	@Override
	public boolean madokuCompat$hasSavedHealth() {
		return madokuCompat$savedHealthMilli != MADOKU_COMPAT_UNSET_SAVED_HEALTH;
	}

	@Override
	public float madokuCompat$getSavedHealth() {
		if (!madokuCompat$hasSavedHealth()) {
			return 0.0f;
		}
		return madokuCompat$savedHealthMilli / 1000.0f;
	}
}
