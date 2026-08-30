package com.wentory.coolstuff.registry;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.config.BootstrapConfig;
import com.wentory.coolstuff.entity.BlackHoleEntity;
import com.wentory.coolstuff.entity.BlackHoleExplosionEntity;
import com.wentory.coolstuff.entity.LeapingCreeperEntity;
import com.wentory.coolstuff.entity.LeapingCreeperProjectileEntity;
import com.wentory.coolstuff.entity.ThrownCakeEntity;
import com.wentory.coolstuff.entity.FrostlingEntity;
import com.wentory.coolstuff.entity.ZombieWolfEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.Optional;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Coolstuff.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BlackHoleEntity>> BLACK_HOLE =
            ENTITY_TYPES.register("black_hole", () -> EntityType.Builder
                    .of(BlackHoleEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .build(Coolstuff.MODID + ":black_hole"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlackHoleExplosionEntity>> BLACK_HOLE_EXPLOSION =
            ENTITY_TYPES.register("black_hole_explosion", () -> EntityType.Builder
                    .of(BlackHoleExplosionEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build(Coolstuff.MODID + ":black_hole_explosion"));

    public static final Optional<DeferredHolder<EntityType<?>, EntityType<LeapingCreeperEntity>>> LEAPING_CREEPER =
            BootstrapConfig.SPORE_CREEPER ? Optional.of(ENTITY_TYPES.register("spore_creeper", () -> EntityType.Builder
                    .of(LeapingCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(Coolstuff.MODID + ":spore_creeper"))) : Optional.empty();

    public static final Optional<DeferredHolder<EntityType<?>, EntityType<LeapingCreeperProjectileEntity>>> LEAPING_CREEPER_PROJECTILE =
            BootstrapConfig.SPORE_CREEPER ? Optional.of(ENTITY_TYPES.register("spore_creeper_projectile", () -> EntityType.Builder
                    .<LeapingCreeperProjectileEntity>of(LeapingCreeperProjectileEntity::new, MobCategory.MISC)
                    .sized(0.72F, 0.72F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build(Coolstuff.MODID + ":spore_creeper_projectile"))) : Optional.empty();

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownCakeEntity>> THROWN_CAKE =
            ENTITY_TYPES.register("thrown_cake", () -> EntityType.Builder
                    .<ThrownCakeEntity>of(ThrownCakeEntity::new, MobCategory.MISC)
                    .sized(0.45F, 0.45F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(Coolstuff.MODID + ":thrown_cake"));

    public static final Optional<DeferredHolder<EntityType<?>, EntityType<FrostlingEntity>>> FROSTLING =
            BootstrapConfig.FROSTLING ? Optional.of(ENTITY_TYPES.register("frosted", () -> EntityType.Builder
                    .of(FrostlingEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(Coolstuff.MODID + ":frosted"))) : Optional.empty();

    public static final Optional<DeferredHolder<EntityType<?>, EntityType<ZombieWolfEntity>>> ZOMBIE_WOLF =
            BootstrapConfig.ZOMBIE_WOLF ? Optional.of(ENTITY_TYPES.register("zombie_wolf", () -> EntityType.Builder
                    .of(ZombieWolfEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.85F)
                    .clientTrackingRange(8)
                    .build(Coolstuff.MODID + ":zombie_wolf"))) : Optional.empty();

    private ModEntities() {
    }
}
