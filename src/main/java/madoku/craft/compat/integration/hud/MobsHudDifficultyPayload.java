package madoku.craft.compat.integration.hud;

import madoku.craft.compat.MadokuCraftCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MobsHudDifficultyPayload(int level) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MobsHudDifficultyPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MadokuCraftCompat.MOD_ID, "mobs_hud_difficulty"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MobsHudDifficultyPayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT,
					MobsHudDifficultyPayload::level,
					MobsHudDifficultyPayload::new
			);

	@Override
	public Type<MobsHudDifficultyPayload> type() {
		return TYPE;
	}
}
