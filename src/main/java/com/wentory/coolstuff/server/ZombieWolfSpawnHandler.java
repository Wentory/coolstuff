package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import com.wentory.coolstuff.entity.ZombieWolfEntity;
import com.wentory.coolstuff.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class ZombieWolfSpawnHandler {
    private ZombieWolfSpawnHandler() {
    }

    @SubscribeEvent
    public static void onZombieSpawn(FinalizeSpawnEvent event) {
        if (!RestartRequiredConfig.zombieWolf()
                || event.getSpawnType() != MobSpawnType.NATURAL
                || !(event.getEntity() instanceof Zombie zombie)
                || zombie.getType() != EntityType.ZOMBIE
                || !(zombie.level() instanceof ServerLevel level)
                || !level.canSeeSky(zombie.blockPosition())) {
            return;
        }

        if (zombie.getVehicle() instanceof Chicken chicken) {
            if (level.getRandom().nextDouble() < CoolstuffConfig.WOLF_JOCKEY_REPLACEMENT_CHANCE.get()) {
                replaceChickenJockey(level, zombie, chicken);
            }
            return;
        }

        if (level.getRandom().nextDouble() < CoolstuffConfig.ZOMBIE_WOLF_SPAWN_CHANCE.get()) {
            spawnOwnedWolf(level, zombie);
        }
    }

    private static void spawnOwnedWolf(ServerLevel level, Zombie owner) {
        ZombieWolfEntity wolf = createWolf(level, owner.getX() + 1.0, owner.getY(), owner.getZ(), true, true);
        if (wolf != null) {
            wolf.setZombieOwner(owner);
            level.addFreshEntity(wolf);
        }
    }

    private static void replaceChickenJockey(ServerLevel level, Zombie rider, Chicken chicken) {
        rider.stopRiding();
        chicken.discard();
        ZombieWolfEntity wolf = createWolf(level, rider.getX(), rider.getY(), rider.getZ(), true, false);
        if (wolf == null) return;
        wolf.setZombieOwner(rider);
        if (level.getRandom().nextFloat() < 0.60F) equipArmor(wolf);
        level.addFreshEntity(wolf);
        rider.startRiding(wolf, true);
    }

    private static ZombieWolfEntity createWolf(ServerLevel level, double x, double y, double z,
                                                boolean hasOwner, boolean allowNormalArmor) {
        var type = ModEntities.ZOMBIE_WOLF.orElse(null);
        if (type == null) return null;
        ZombieWolfEntity wolf = type.get().create(level);
        if (wolf == null) return null;
        wolf.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (hasOwner) {
            DyeColor[] colors = DyeColor.values();
            wolf.setDisplayCollar(colors[level.getRandom().nextInt(colors.length)]);
        }
        if (allowNormalArmor && level.getRandom().nextFloat() < 0.10F) equipArmor(wolf);
        return wolf;
    }

    private static void equipArmor(ZombieWolfEntity wolf) {
        wolf.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WOLF_ARMOR));
    }
}
