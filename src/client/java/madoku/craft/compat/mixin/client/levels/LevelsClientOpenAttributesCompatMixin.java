package madoku.craft.compat.mixin.client.levels;

import madoku.craft.compat.integration.levels.LevelsAttributesScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "madoku.craft.levels.MadokuLevelsClient", remap = false)
public abstract class LevelsClientOpenAttributesCompatMixin {
	@Redirect(
		method = "handleClientTick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
		)
	)
	private static void madokuCompat$openCompatLevelsScreen(Minecraft client, Screen screen) {
		client.setScreen(new LevelsAttributesScreen());
	}
}
