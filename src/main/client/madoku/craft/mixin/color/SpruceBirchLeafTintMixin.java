package madoku.craft.mixin.color;

import madoku.craft.java.color.ClientColorContext;
import madoku.craft.java.core.season.ClientSeasonalPrecipitationState;
import madoku.craft.java.core.season.EnvironmentTransitionConfigAPIManager;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the seasonal foliage intent on 1.21.11's BlockColor API. */
@Mixin(BlockColors.class)
public final class SpruceBirchLeafTintMixin {
	private static final BlockColor SEASONAL_FOLIAGE = SpruceBirchLeafTintMixin::seasonalFoliageColor;

	@Inject(method = "createDefault", at = @At("RETURN"))
	private static void madokuCraft$registerSeasonalLeafTint(CallbackInfoReturnable<BlockColors> cir) {
		BlockColors colors = cir.getReturnValue();
		if (colors != null) {
			colors.register(SEASONAL_FOLIAGE, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES);
		}
	}

	private static int seasonalFoliageColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
		if (level == null || pos == null
			|| !EnvironmentTransitionConfigAPIManager.getSettings().transitionColorEnabled()
			|| !ClientSeasonalPrecipitationState.isSynchronized()) {
			return state == null ? 0xFF619961 : BiomeColors.getAverageFoliageColor(level, pos);
		}
		ClientColorContext.force(true);
		try {
			return BiomeColors.getAverageFoliageColor(level, pos);
		} finally {
			ClientColorContext.clear();
		}
	}
}
