package com.wentory.coolstuff.registry;

import com.wentory.coolstuff.Coolstuff;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> BLACK_HOLE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "black_hole"));
    public static final ResourceKey<DamageType> CREEPER_PARRY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "creeper_parry"));
    public static final ResourceKey<DamageType> CREEPER_PARRY_FART = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "creeper_parry_fart"));
    public static final ResourceKey<DamageType> SPORE_CREEPER = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "spore_creeper"));
    public static final ResourceKey<DamageType> SPORE_CREEPER_FART = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "spore_creeper_fart"));

    private ModDamageTypes() {
    }

    public static DamageSource blackHole(Level level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(BLACK_HOLE));
    }

    public static DamageSource creeperParry(Level level, Entity creeper, Player player) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(CREEPER_PARRY), creeper, player);
    }

    public static DamageSource creeperParry(Level level, Entity creeper, Player player, boolean farted) {
        ResourceKey<DamageType> type = farted ? CREEPER_PARRY_FART : CREEPER_PARRY;
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(type), creeper, player);
    }

    public static DamageSource sporeCreeper(Level level, Entity creeper, boolean farted) {
        ResourceKey<DamageType> type = farted ? SPORE_CREEPER_FART : SPORE_CREEPER;
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(type), creeper);
    }
}
