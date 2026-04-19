package madoku.craft.compat.mixin.attributes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "madoku.craft.hunger.MadokuHunger$PlayerState", remap = false)
public interface HungerPlayerStateAccessor {
	@Accessor("hungerPoints")
	int madokuCompat$getHungerPoints();

	@Accessor("pendingHunger")
	int madokuCompat$getPendingHunger();
}
