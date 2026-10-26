package madoku.craft.mixin.attributes;

import madoku.craft.java.attributes.LuckAPIManager;
import madoku.craft.java.core.loot.LootFeatureAPIManager;
import madoku.craft.java.core.loot.LootTableEntitiesAPIManager;
import madoku.craft.java.mob.MobEntityTypeAPIManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLuckLootMixin {
	@Inject(
		method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void madokuCraft$applyManagedMobDrops(
		ServerLevel level,
		DamageSource damageSource,
		boolean causedByPlayer,
		ResourceKey<LootTable> lootTableKey,
		Consumer<ItemStack> consumer,
		CallbackInfo ci
	) {
		LivingEntity livingEntity = (LivingEntity) (Object) this;
		if (livingEntity == null || level == null || consumer == null || livingEntity.level().isClientSide()) {
			return;
		}
		if (!LootFeatureAPIManager.isMobEnabled()) {
			return;
		}

		String configuredReference = "";
		if (MobEntityTypeAPIManager.isType(livingEntity.getType(), "minecraft:bee")) {
			if (!LootFeatureAPIManager.isBeeCustomMobDropsEnabled(livingEntity)) {
				return;
			}
			configuredReference = LootFeatureAPIManager.resolveBeeMobDropsConfigReference(livingEntity);
		} else if (isZombieType(livingEntity.getType())) {
			if (!LootFeatureAPIManager.isZombieCustomMobDropsEnabled(livingEntity)) {
				return;
			}
			configuredReference = LootFeatureAPIManager.resolveZombieMobDropsConfigReference(livingEntity);
		} else if (isPiglinType(livingEntity.getType())) {
			if (!LootFeatureAPIManager.isPiglinCustomMobDropsEnabled(livingEntity)) {
				return;
			}
			configuredReference = LootFeatureAPIManager.resolvePiglinMobDropsConfigReference(livingEntity);
		} else if (MobEntityTypeAPIManager.isType(livingEntity.getType(), "minecraft:pillager")) {
			if (!LootFeatureAPIManager.isPillagerCustomMobDropsEnabled(livingEntity)) {
				return;
			}
			configuredReference = LootFeatureAPIManager.resolvePillagerMobDropsConfigReference(livingEntity);
		} else {
			return;
		}

		List<ItemStack> generated = LootTableEntitiesAPIManager.generateManagedLootForReference(
			configuredReference,
			resolveLootPlayer(damageSource),
			level.getRandom()
		);
		if (generated == null) {
			return;
		}
		for (ItemStack stack : generated) {
			if (stack != null && !stack.isEmpty()) {
				consumer.accept(stack);
			}
		}
		ci.cancel();
	}

	@ModifyVariable(
		method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
		at = @At("HEAD"),
		argsOnly = true,
		index = 5
	)
	private Consumer<ItemStack> madokuCraft$wrapMobLuckLootConsumer(
		Consumer<ItemStack> consumer,
		ServerLevel level,
		DamageSource damageSource,
		boolean causedByPlayer,
		ResourceKey<LootTable> lootTableKey
	) {
		LivingEntity livingEntity = (LivingEntity) (Object) this;
		Consumer<ItemStack> normalizedConsumer = stack -> {
			consumer.accept(stack);
		};

		return LuckAPIManager.wrapMobDeathLootConsumer(
			level,
			livingEntity,
			damageSource,
			causedByPlayer,
			normalizedConsumer
		);
	}

	private static boolean isZombieType(EntityType<?> type) {
		return MobEntityTypeAPIManager.isType(type, "minecraft:zombie")
			|| MobEntityTypeAPIManager.isType(type, "minecraft:husk")
			|| MobEntityTypeAPIManager.isType(type, "minecraft:drowned")
			|| MobEntityTypeAPIManager.isType(type, "minecraft:zombie_villager");
	}

	private static boolean isPiglinType(EntityType<?> type) {
		return MobEntityTypeAPIManager.isType(type, "minecraft:piglin")
			|| MobEntityTypeAPIManager.isType(type, "minecraft:zombified_piglin");
	}

	private static ServerPlayer resolveLootPlayer(DamageSource source) {
		if (source == null) {
			return null;
		}
		Entity attacker = source.getEntity();
		if (attacker instanceof ServerPlayer serverPlayer) {
			return serverPlayer;
		}
		Entity direct = source.getDirectEntity();
		if (direct instanceof ServerPlayer serverPlayer) {
			return serverPlayer;
		}
		return null;
	}
}
