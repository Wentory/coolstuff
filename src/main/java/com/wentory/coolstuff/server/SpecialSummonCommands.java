package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.ZombieWolfEntity;
import com.wentory.coolstuff.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class SpecialSummonCommands {
    private SpecialSummonCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("coolstuff")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summon")
                        .then(Commands.literal("ultra_ghast")
                                .executes(context -> spawnUltraGhast(context.getSource())))
                        .then(Commands.literal("shield_skeleton")
                                .executes(context -> spawnShieldSkeleton(context.getSource())))
                        .then(Commands.literal("zombie_wolf_pack")
                                .executes(context -> spawnWolfPack(context.getSource())))
                        .then(Commands.literal("zombie_wolf_owner")
                                .executes(context -> spawnOwnedWolf(context.getSource())))
                        .then(Commands.literal("wolf_jockey")
                                .executes(context -> spawnWolfJockey(context.getSource())))));
    }
    private static int spawnUltraGhast(CommandSourceStack source) {
        Ghast ghast = EntityType.GHAST.create(source.getLevel());
        if (ghast == null) return failed(source, "Could not create an UltraGhast");
        placeAndFinalize(ghast, source, 0.0, 0.0);
        UltraGhast.setUltra(ghast, true);
        source.getLevel().addFreshEntity(ghast);
        return succeeded(source, "Summoned an UltraGhast");
    }


    private static int spawnShieldSkeleton(CommandSourceStack source) {
        Skeleton skeleton = EntityType.SKELETON.create(source.getLevel());
        if (skeleton == null) return failed(source, "Could not create a Shield Skeleton");
        placeAndFinalize(skeleton, source, 0.0, 0.0);
        ShieldSkeletonHandler.makeShieldSkeleton(skeleton);
        source.getLevel().addFreshEntity(skeleton);
        return succeeded(source, "Summoned a Shield Skeleton");
    }



    private static int spawnWolfPack(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int count = 3 + level.random.nextInt(3);
        int spawned = 0;
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0 * index / count;
            ZombieWolfEntity wolf = createWolf(source, Math.cos(angle) * 2.5, Math.sin(angle) * 2.5,
                    false, false);
            if (wolf != null) {
                level.addFreshEntity(wolf);
                spawned++;
            }
        }
        if (spawned == 0) return 0;
        int result = spawned;
        source.sendSuccess(() -> Component.literal("Summoned a natural Zombie Wolf pack (" + result + ")"), true);
        return spawned;
    }

    private static int spawnOwnedWolf(CommandSourceStack source) {
        Zombie owner = createZombie(source, 0.0, 0.0, false);
        ZombieWolfEntity wolf = createWolf(source, 1.5, 0.0, true, false);
        if (owner == null || wolf == null) return failed(source, "Could not create a Zombie Wolf encounter");
        wolf.setZombieOwner(owner);
        source.getLevel().addFreshEntity(owner);
        source.getLevel().addFreshEntity(wolf);
        return succeeded(source, "Summoned a Zombie with its Zombie Wolf");
    }

    private static int spawnWolfJockey(CommandSourceStack source) {
        Zombie rider = createZombie(source, 0.0, 0.0, true);
        ZombieWolfEntity wolf = createWolf(source, 0.0, 0.0, true, true);
        if (rider == null || wolf == null) return failed(source, "Could not create a Wolf Jockey");
        wolf.setZombieOwner(rider);
        source.getLevel().addFreshEntity(wolf);
        source.getLevel().addFreshEntity(rider);
        rider.startRiding(wolf, true);
        return succeeded(source, "Summoned an armored Wolf Jockey");
    }

    private static Zombie createZombie(CommandSourceStack source, double offsetX, double offsetZ, boolean baby) {
        Zombie zombie = EntityType.ZOMBIE.create(source.getLevel());
        if (zombie == null) return null;
        placeAndFinalize(zombie, source, offsetX, offsetZ);
        zombie.setBaby(baby);
        return zombie;
    }

    private static ZombieWolfEntity createWolf(CommandSourceStack source, double offsetX, double offsetZ,
                                                boolean collar, boolean armor) {
        var holder = ModEntities.ZOMBIE_WOLF.orElse(null);
        if (holder == null) {
            source.sendFailure(Component.literal("Zombie Wolves are disabled in the config"));
            return null;
        }
        ZombieWolfEntity wolf = holder.get().create(source.getLevel());
        if (wolf == null) return null;
        placeAndFinalize(wolf, source, offsetX, offsetZ);
        if (collar) {
            DyeColor[] colors = DyeColor.values();
            wolf.setDisplayCollar(colors[source.getLevel().random.nextInt(colors.length)]);
        }
        if (armor) wolf.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WOLF_ARMOR));
        return wolf;
    }

    private static void placeAndFinalize(Mob mob, CommandSourceStack source, double offsetX, double offsetZ) {
        ServerLevel level = source.getLevel();
        Vec3 position = source.getPosition().add(offsetX, 0.0, offsetZ);
        BlockPos blockPos = BlockPos.containing(position);
        mob.moveTo(position.x, position.y, position.z, level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPos), MobSpawnType.COMMAND, null);
    }

    private static int succeeded(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int failed(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}