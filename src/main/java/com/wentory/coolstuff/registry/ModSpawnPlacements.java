package com.wentory.coolstuff.registry;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = Coolstuff.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModSpawnPlacements {
    private ModSpawnPlacements() {
    }

    @SubscribeEvent
    public static void register(RegisterSpawnPlacementsEvent event) {
        ModEntities.FROSTLING.ifPresent(holder -> event.register(holder.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE));
        ModEntities.ZOMBIE_WOLF.ifPresent(holder -> event.register(holder.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> RestartRequiredConfig.zombieWolf()
                        && level.getDifficulty() != Difficulty.PEACEFUL
                        && Monster.isDarkEnoughToSpawn(level, pos, random)
                        && Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }
}
