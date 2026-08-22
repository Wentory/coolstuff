package com.wentory.coolstuff.server;

import com.wentory.coolstuff.config.CoolstuffConfig;
import net.minecraft.world.entity.monster.Ghast;

public final class UltraGhast {
    public static final String NBT_KEY = "coolstuff_ultra_ghast";
    private UltraGhast() {
    }

    public static boolean isUltra(Ghast ghast) {
        return ghast.getPersistentData().getBoolean(NBT_KEY);
    }

    public static void setUltra(Ghast ghast, boolean ultra) {
        ghast.getPersistentData().putBoolean(NBT_KEY, ultra);
    }

    public static double getNaturalSpawnChance() {
        return CoolstuffConfig.ULTRA_GHAST_SPAWN_CHANCE.get();
    }

    public static void setNaturalSpawnChance(double chance) {
        CoolstuffConfig.ULTRA_GHAST_SPAWN_CHANCE.set(Math.max(0.0, Math.min(1.0, chance)));
        if (CoolstuffConfig.SPEC.isLoaded()) CoolstuffConfig.SPEC.save();
    }
}
