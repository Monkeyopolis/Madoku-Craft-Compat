package madoku.craft.compat.integration.hud;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WorldHudTotalDifficultyPayload(int totalDifficulty) implements CustomPayload {
	public static final CustomPayload.Id<WorldHudTotalDifficultyPayload> ID = new CustomPayload.Id<>(
			Identifier.of("madoku-craft-compat", "world_hud_total_difficulty")
	);
	public static final PacketCodec<RegistryByteBuf, WorldHudTotalDifficultyPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			WorldHudTotalDifficultyPayload::totalDifficulty,
			WorldHudTotalDifficultyPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
