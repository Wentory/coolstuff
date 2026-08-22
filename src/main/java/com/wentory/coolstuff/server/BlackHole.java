package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.BlackHoleEntity;
import com.wentory.coolstuff.entity.BlackHoleExplosionEntity;
import com.wentory.coolstuff.entity.ThrownCakeEntity;
import com.wentory.coolstuff.registry.ModEntities;
import com.wentory.coolstuff.registry.ModDamageTypes;
import com.wentory.coolstuff.registry.ModSounds;
import com.wentory.coolstuff.network.BlackHoleImpactPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.wentory.coolstuff.config.CoolstuffConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class BlackHole {
    private static final String FORMING_TAG = "coolstuff_black_hole_forming";
    private static final String FORMATION_TICKS_TAG = "coolstuff_black_hole_formation_ticks";
    private static final int FORMATION_TICKS = 20 * 10;
    private static final int LIFETIME_TICKS = 20 * 10;
    private static final int COLLAPSE_TICKS = 30;
    private static final int CAKE_PACIFY_TICKS = 30;
    private static final double PULL_RADIUS = 32.0;
    private static final int MAX_ACTIVE_DEBRIS = 50;
    private static final int DEBRIS_PER_TICK = 3;
    private static final String DEBRIS_TAG = "coolstuff_black_hole_debris";
    private static final Map<ServerLevel, Set<BlockPos>> TRACKED_CAKES = new WeakHashMap<>();

    private BlackHole() {
    }

    public static boolean isActive(LargeFireball fireball) {
        return fireball.getPersistentData().getBoolean(FORMING_TAG);
    }

    public static void activate(LargeFireball fireball) {
        if (!CoolstuffConfig.ENABLE_BLACK_HOLES.get()
                || !(fireball.level() instanceof ServerLevel level) || fireball.isRemoved()) return;

        fireball.getPersistentData().putBoolean(FORMING_TAG, true);
        fireball.getPersistentData().putInt(FORMATION_TICKS_TAG, FORMATION_TICKS);
        fireball.setDeltaMovement(Vec3.ZERO);
        fireball.accelerationPower = 0.0;
        fireball.setNoGravity(true);
        fireball.noPhysics = true;
        fireball.setInvulnerable(true);
        fireball.hasImpulse = true;
        level.playSound(null, fireball.blockPosition(), ModSounds.BLACK_HOLE_FORMATION.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private static void finishFormation(ServerLevel level, LargeFireball fireball) {

        BlackHoleEntity blackHole = new BlackHoleEntity(ModEntities.BLACK_HOLE.get(), level);
        blackHole.setPos(fireball.getX(), fireball.getY(), fireball.getZ());
        blackHole.setNoGravity(true);
        blackHole.setInvulnerable(true);
        blackHole.setRemainingTicks(LIFETIME_TICKS);
        level.addFreshEntity(blackHole);
        level.playSound(null, blackHole.blockPosition(), ModSounds.BLACK_HOLE_SPAWN.get(),
                SoundSource.HOSTILE, 1.0F, 0.85F);
        fireball.discard();
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LargeFireball fireball
                && fireball.level() instanceof ServerLevel level
                && isActive(fireball)) {
            if (!CoolstuffConfig.ENABLE_BLACK_HOLES.get()) {
                fireball.discard();
                return;
            }
            tickFormation(level, fireball);
            return;
        }
        if (!(event.getEntity() instanceof BlackHoleEntity blackHole)
                || !(blackHole.level() instanceof ServerLevel level)) return;
        if (!CoolstuffConfig.ENABLE_BLACK_HOLES.get()) {
            blackHole.discard();
            return;
        }

        if (blackHole.isPacified()) {
            tickCakePacification(level, blackHole);
            return;
        }

        boolean eternal = blackHole.isEternal();
        int remaining = LIFETIME_TICKS;
        if (!eternal) {
            remaining = blackHole.getRemainingTicks() - 1;
            if (remaining <= 0) {
                collapse(level, blackHole);
                return;
            }
            blackHole.setRemainingTicks(remaining);
        }
        blackHole.setDeltaMovement(Vec3.ZERO);
        if (CoolstuffConfig.ENABLE_CAKE_BLACK_HOLE_PACIFICATION.get()) pullTrackedCake(level, blackHole);

        if (!eternal && remaining <= COLLAPSE_TICKS) {
            if (remaining % 4 == 0) level.playSound(null, blackHole.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 0.7F, 0.55F + remaining * 0.025F);
        }

        Vec3 center = blackHole.position();
        AABB area = blackHole.getBoundingBox().inflate(PULL_RADIUS);
        int activeDebris = 0;
        for (Entity entity : level.getEntities(blackHole, area, BlackHole::canPull)) {
            Vec3 offset = center.subtract(entity.getBoundingBox().getCenter());
            double distance = offset.length();
            if (distance < 0.01 || distance > PULL_RADIUS) continue;
            if (CoolstuffConfig.ENABLE_CAKE_BLACK_HOLE_PACIFICATION.get()
                    && distance < 2.5 && (entity instanceof ThrownCakeEntity
                    || entity instanceof FallingBlockEntity fallingCake
                    && fallingCake.getBlockState().is(Blocks.CAKE))) {
                entity.discard();
                pacifyWithCake(level, blackHole);
                return;
            }
            if (entity instanceof FallingBlockEntity && distance < 2.5) {
                entity.discard();
                level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY(), entity.getZ(),
                        5, 0.25, 0.25, 0.25, 0.02);
                continue;
            }
            if (entity instanceof FallingBlockEntity
                    && entity.getPersistentData().getBoolean(DEBRIS_TAG)) activeDebris++;
            double proximity = 1.0 - distance / PULL_RADIUS;
            double strength = 0.12 + proximity * proximity * 0.95;
            if (entity instanceof Player) strength *= 2.2;
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.84).add(offset.normalize().scale(strength)));
            entity.hasImpulse = true;
            entity.hurtMarked = true;
            entity.fallDistance = 0.0F;
            if (entity instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
            if (entity instanceof LivingEntity living && distance < 4.0 && blackHole.tickCount % 5 == 0)
                living.hurt(ModDamageTypes.blackHole(level), 8.0F);
        }

        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            int wanted = Math.min(DEBRIS_PER_TICK, MAX_ACTIVE_DEBRIS - activeDebris);
            int launched = 0;
            for (int attempt = 0; attempt < 60 && launched < wanted; attempt++) {
                launched += ripOutBlock(level, center, blackHole);
            }
        }
    }

    @SubscribeEvent
    public static void onCakePlaced(BlockEvent.EntityPlaceEvent event) {
        if (!CoolstuffConfig.ENABLE_CAKE_BLACK_HOLE_PACIFICATION.get()
                || !(event.getLevel() instanceof ServerLevel level) || !event.getPlacedBlock().is(Blocks.CAKE)) return;
        TRACKED_CAKES.computeIfAbsent(level, ignored -> new HashSet<>()).add(event.getPos().immutable());
    }

    @SubscribeEvent
    public static void onCakeBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Set<BlockPos> cakes = TRACKED_CAKES.get(level);
        if (cakes != null) cakes.remove(event.getPos());
    }

    public static int spawnEternal(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlackHoleEntity blackHole = new BlackHoleEntity(ModEntities.BLACK_HOLE.get(), level);
        Vec3 position = source.getPosition();
        blackHole.setPos(position.x, position.y, position.z);
        blackHole.setEternal(true);
        blackHole.setRemainingTicks(LIFETIME_TICKS);
        blackHole.setNoGravity(true);
        blackHole.setInvulnerable(true);
        level.addFreshEntity(blackHole);
        level.playSound(null, blackHole.blockPosition(), ModSounds.BLACK_HOLE_SPAWN.get(),
                SoundSource.HOSTILE, 1.0F, 0.85F);
        source.sendSuccess(() -> Component.literal("Spawned an eternal black hole"), true);
        return 1;
    }

    public static int removeNearestEternal(CommandSourceStack source) {
        Vec3 position = source.getPosition();
        BlackHoleEntity nearest = source.getLevel().getEntitiesOfClass(BlackHoleEntity.class,
                        new AABB(position, position).inflate(128.0), BlackHoleEntity::isEternal)
                .stream().min(java.util.Comparator.comparingDouble(hole -> hole.distanceToSqr(position)))
                .orElse(null);
        if (nearest == null) {
            source.sendFailure(Component.literal("No eternal black hole found within 128 blocks"));
            return 0;
        }
        nearest.discard();
        source.sendSuccess(() -> Component.literal("Removed the nearest eternal black hole"), true);
        return 1;
    }

    private static void tickFormation(ServerLevel level, LargeFireball fireball) {
        int remaining = fireball.getPersistentData().getInt(FORMATION_TICKS_TAG) - 1;
        fireball.getPersistentData().putInt(FORMATION_TICKS_TAG, remaining);
        fireball.setDeltaMovement(Vec3.ZERO);
        fireball.accelerationPower = 0.0;
        if (remaining <= 0) {
            finishFormation(level, fireball);
            return;
        }
        if (remaining % 20 == 0) {
            float progress = 1.0F - remaining / (float) FORMATION_TICKS;
            level.playSound(null, fireball.blockPosition(), SoundEvents.BEACON_AMBIENT,
                    SoundSource.HOSTILE, 0.7F + progress, 0.55F + progress * 0.9F);
        }
        if (remaining < 60 && remaining % 3 == 0) {
            level.sendParticles(ParticleTypes.FLASH, fireball.getX(), fireball.getY(), fireball.getZ(),
                    1, 0.15, 0.15, 0.15, 0.0);
        }
    }

    private static int ripOutBlock(ServerLevel level, Vec3 center, BlackHoleEntity blackHole) {
        BlockPos pos = BlockPos.containing(
                center.x + (level.random.nextDouble() - 0.5) * PULL_RADIUS * 1.5,
                center.y + (level.random.nextDouble() - 0.5) * PULL_RADIUS,
                center.z + (level.random.nextDouble() - 0.5) * PULL_RADIUS * 1.5);
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.CAKE)) {
            launchCakeBlock(level, pos, state, center);
            return 0;
        }
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()
                || state.getDestroySpeed(level, pos) < 0.0F) return 0;
        level.removeBlock(pos, false);
        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
        prepareDebris(falling);
        Vec3 pull = center.subtract(falling.position());
        if (pull.lengthSqr() > 0.01) falling.setDeltaMovement(pull.normalize().scale(0.48));
        return 1;
    }

    private static void pullTrackedCake(ServerLevel level, BlackHoleEntity blackHole) {
        Set<BlockPos> cakes = TRACKED_CAKES.get(level);
        if (cakes == null || cakes.isEmpty()) return;
        BlockPos selected = null;
        double nearestDistance = PULL_RADIUS * PULL_RADIUS;
        for (BlockPos pos : cakes) {
            if (!level.getBlockState(pos).is(Blocks.CAKE)) continue;
            double distance = pos.distToCenterSqr(blackHole.getX(), blackHole.getY(), blackHole.getZ());
            if (distance <= nearestDistance) {
                selected = pos;
                nearestDistance = distance;
            }
        }
        if (selected == null) return;
        BlockState state = level.getBlockState(selected);
        cakes.remove(selected);
        launchCakeBlock(level, selected, state, blackHole.position());
    }

    private static void launchCakeBlock(ServerLevel level, BlockPos pos, BlockState state, Vec3 center) {
        Set<BlockPos> cakes = TRACKED_CAKES.get(level);
        if (cakes != null) cakes.remove(pos);
        FallingBlockEntity cake = FallingBlockEntity.fall(level, pos, state);
        prepareDebris(cake);
        Vec3 pull = center.subtract(cake.position());
        if (pull.lengthSqr() > 0.01) cake.setDeltaMovement(pull.normalize().scale(0.55));
    }

    private static void prepareDebris(FallingBlockEntity debris) {
        debris.getPersistentData().putBoolean(DEBRIS_TAG, true);
        debris.disableDrop();
        debris.noPhysics = true;
    }

    private static void pacifyWithCake(ServerLevel level, BlackHoleEntity blackHole) {
        if (blackHole.isPacified()) return;
        blackHole.setPacified(true);
        blackHole.setEternal(false);
        blackHole.setRemainingTicks(CAKE_PACIFY_TICKS);
        blackHole.setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.HEART, blackHole.getX(), blackHole.getY(), blackHole.getZ(),
                18, 1.8, 1.8, 1.8, 0.08);
    }

    private static void tickCakePacification(ServerLevel level, BlackHoleEntity blackHole) {
        blackHole.setDeltaMovement(Vec3.ZERO);
        int remaining = blackHole.getRemainingTicks() - 1;
        blackHole.setRemainingTicks(Math.max(0, remaining));
        if (remaining > 0 && remaining % 3 == 0) {
            double spread = 0.35 + (remaining / (double) CAKE_PACIFY_TICKS) * 1.4;
            level.sendParticles(ParticleTypes.HEART, blackHole.getX(), blackHole.getY(), blackHole.getZ(),
                    3, spread, spread, spread, 0.02);
        }
        if (remaining <= 0) blackHole.discard();
    }

    private static void collapse(ServerLevel level, BlackHoleEntity blackHole) {
        Vec3 center = blackHole.position();
        BlackHoleExplosionEntity visual = new BlackHoleExplosionEntity(
                ModEntities.BLACK_HOLE_EXPLOSION.get(), level);
        visual.setPos(center);
        level.addFreshEntity(visual);
        level.playSound(null, blackHole.blockPosition(), ModSounds.BLACK_HOLE_EXPLOSION.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z,
                6, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
                4, 3.0, 3.0, 3.0, 0.0);
        PacketDistributor.sendToPlayersNear(level, null, center.x, center.y, center.z, 150.0,
                new BlackHoleImpactPayload(center.x, center.y, center.z));
        NuclearExplosion.start(level, center, blackHole);
        blackHole.discard();
    }

    private static boolean canPull(Entity entity) {
        if (!entity.isAlive()) return false;
        if (!(entity instanceof Player player)) return true;
        if (player.isSpectator()) return false;
        return !player.isCreative() || CoolstuffConfig.BLACK_HOLE_PULL_CREATIVE_PLAYERS.get();
    }
}
