package madoku.craft.compat.mixin.attribute;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClampedEntityAttribute.class)
public interface ClampedEntityAttributeAccessor {
	@Mutable
	@Accessor("maxValue")
	void madokuCompat$setMaxValue(double value);
}
