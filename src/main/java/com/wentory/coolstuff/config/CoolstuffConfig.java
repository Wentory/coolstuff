package com.wentory.coolstuff.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CoolstuffConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue BLACK_HOLE_PULL_CREATIVE_PLAYERS;
    public static final ModConfigSpec.BooleanValue ENABLE_PARRY;
    public static final ModConfigSpec.BooleanValue ENABLE_GHAST_PARRY;
    public static final ModConfigSpec.BooleanValue ENABLE_ULTRA_GHAST;
    public static final ModConfigSpec.BooleanValue ENABLE_SPORE_CREEPER;
    public static final ModConfigSpec.BooleanValue SPORE_CREEPER_BLOCK_DAMAGE;
    public static final ModConfigSpec.BooleanValue PARRIED_SPORE_CREEPER_BLOCK_DAMAGE;
    public static final ModConfigSpec.BooleanValue ENABLE_SUGAR_TRANSFORMATION;
    public static final ModConfigSpec.BooleanValue ENABLE_THROWABLE_CAKES;
    public static final ModConfigSpec.BooleanValue ENABLE_CAKE_FILLINGS;
    public static final ModConfigSpec.BooleanValue ENABLE_BLACK_HOLES;
    public static final ModConfigSpec.BooleanValue ENABLE_CAKE_BLACK_HOLE_PACIFICATION;
    public static final ModConfigSpec.BooleanValue ENABLE_FIREBALL_LAUNCHER;
    public static final ModConfigSpec.BooleanValue ENABLE_ZOMBIE_WOLF;
    public static final ModConfigSpec.BooleanValue ENABLE_EMISSIVE_TRIMS;
    public static final ModConfigSpec.BooleanValue ENABLE_FROSTLING;
    public static final ModConfigSpec.DoubleValue GHAST_PARRY_CHANCE;
    public static final ModConfigSpec.DoubleValue ULTRA_GHAST_SPAWN_CHANCE;
    public static final ModConfigSpec.DoubleValue SPORE_CREEPER_SPAWN_CHANCE;
    public static final ModConfigSpec.DoubleValue SPORE_FART_CHANCE;
    public static final ModConfigSpec.DoubleValue SHIELD_SKELETON_SPAWN_CHANCE;
    public static final ModConfigSpec.DoubleValue SHIELD_SKELETON_PARRY_CHANCE;
    public static final ModConfigSpec.DoubleValue ZOMBIE_WOLF_SPAWN_CHANCE;
    public static final ModConfigSpec.DoubleValue FROSTLING_SPAWN_CHANCE;
    public static final ModConfigSpec.DoubleValue WOLF_JOCKEY_REPLACEMENT_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("mechanics");
        ENABLE_PARRY = toggle(builder, "parry", "Fireball parry combos and their effects.");
        ENABLE_GHAST_PARRY = toggle(builder, "ghastParry", "Ghasts can parry reflected fireballs.");
        ENABLE_ULTRA_GHAST = toggle(builder, "ultraGhast", "UltraGhasts and their burst attacks.");
        ENABLE_SPORE_CREEPER = toggle(builder, "sporeCreeper", "Natural Spore Creeper spawning.");
        SPORE_CREEPER_BLOCK_DAMAGE = builder
                .comment("Whether normal Spore Creeper explosions damage blocks.")
                .define("sporeCreeperBlockDamage", false);
        PARRIED_SPORE_CREEPER_BLOCK_DAMAGE = builder
                .comment("Whether parried Spore Creeper explosions damage blocks.")
                .define("parriedSporeCreeperBlockDamage", true);
        ENABLE_SUGAR_TRANSFORMATION = toggle(builder, "sugarTransformation", "Creepers transform after eating sugar.");
        ENABLE_THROWABLE_CAKES = toggle(builder, "throwableCakes", "Cakes can be thrown.");
        ENABLE_CAKE_FILLINGS = toggle(builder, "cakeFillings", "Cake filling effects are applied on hit.");
        ENABLE_BLACK_HOLES = toggle(builder, "blackHoles", "Fireballs can transform into black holes.");
        ENABLE_CAKE_BLACK_HOLE_PACIFICATION = toggle(builder, "cakePacification", "Cakes peacefully pacify black holes.");
        ENABLE_FIREBALL_LAUNCHER = toggle(builder, "fireballLauncher", "The Fireball Launcher can be used.");
        ENABLE_ZOMBIE_WOLF = toggle(builder, "zombieWolf", "Zombie Wolf encounters can spawn with zombies.");
        ENABLE_EMISSIVE_TRIMS = toggle(builder, "emissiveTrims", "Armor trims can be made emissive with glow ink.");
        ENABLE_FROSTLING = toggle(builder, "frostling", "Zombies in snowy biomes can become Frostlings.");
        builder.pop();
        builder.push("chances");
        GHAST_PARRY_CHANCE = chance(builder, "ghastParry", 0.30, "Chance that a ghast parries a reflected fireball.");
        ULTRA_GHAST_SPAWN_CHANCE = chance(builder, "ultraGhastSpawn", 0.10, "Natural UltraGhast spawn chance.");
        SPORE_CREEPER_SPAWN_CHANCE = chance(builder, "sporeCreeperSpawn", 0.10, "Natural Spore Creeper spawn chance.");
        SPORE_FART_CHANCE = chance(builder, "sporeFart", 0.05, "Chance that a Spore Creeper farts when launching.");
        SHIELD_SKELETON_SPAWN_CHANCE = chance(builder, "shieldSkeletonSpawn", 0.10,
                "Natural chance for a skeleton to spawn with defensive shield AI.");
        SHIELD_SKELETON_PARRY_CHANCE = chance(builder, "shieldSkeletonParry", 0.12,
                "Chance that a blocking Shield Skeleton parries an incoming projectile.");
        ZOMBIE_WOLF_SPAWN_CHANCE = chance(builder, "zombieWolfSpawn", 0.05,
                "Chance that a natural zombie spawn becomes a Zombie Wolf encounter.");
        FROSTLING_SPAWN_CHANCE = chance(builder, "frostlingSpawn", 0.90,
                "Chance that a zombie spawning in a snowy biome becomes a Frostling.");
        WOLF_JOCKEY_REPLACEMENT_CHANCE = chance(builder, "wolfJockeyReplacement", 0.50,
                "Chance that a Chicken Jockey is replaced by a Wolf Jockey.");
        builder.pop();
        builder.push("blackHole");
        BLACK_HOLE_PULL_CREATIVE_PLAYERS = builder
                .comment("Whether black holes pull players in Creative mode.")
                .define("pullCreativePlayers", false);
        builder.pop();
        SPEC = builder.build();
    }

    private CoolstuffConfig() {
    }

    private static ModConfigSpec.BooleanValue toggle(ModConfigSpec.Builder builder, String name, String comment) {
        return builder.comment(comment).define(name, true);
    }

    private static ModConfigSpec.DoubleValue chance(ModConfigSpec.Builder builder, String name,
                                                     double defaultValue, String comment) {
        return builder.comment(comment).defineInRange(name, defaultValue, 0.0, 1.0);
    }
}
