package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class NuclearExplosion {
    private static final int HORIZONTAL_RADIUS = 32;
    private static final int VERTICAL_RADIUS = 20;
    private static final int MAX_SCANNED_PER_TICK = 16_384;
    private static final int MAX_DESTROYED_PER_TICK = 2_048;
    private static final double SHOCKWAVE_RADIUS = 64.0;
    private static final String DEBRIS_TAG = "coolstuff_nuclear_debris";
    private static final Map<ServerLevel, List<BlastJob>> ACTIVE = new IdentityHashMap<>();

    private NuclearExplosion() {
    }

    public static void start(ServerLevel level, Vec3 center, Entity source) {
        ACTIVE.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new BlastJob(BlockPos.containing(center)));
        applyShockwave(level, center, source);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<BlastJob> jobs = ACTIVE.get(level);
        if (jobs == null) return;
        Iterator<BlastJob> iterator = jobs.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().process(level)) iterator.remove();
        }
        if (jobs.isEmpty()) ACTIVE.remove(level);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity instanceof FallingBlockEntity
                && entity.getPersistentData().getBoolean(DEBRIS_TAG)
                && entity.tickCount >= 55) {
            entity.discard();
        }
    }

    private static void applyShockwave(ServerLevel level, Vec3 center, Entity source) {
        AABB area = new AABB(center, center).inflate(SHOCKWAVE_RADIUS);
        for (Entity entity : level.getEntities(source, area, NuclearExplosion::canAffect)) {
            Vec3 away = entity.getBoundingBox().getCenter().subtract(center);
            double distance = away.length();
            if (distance < 0.01 || distance > SHOCKWAVE_RADIUS) continue;
            double intensity = 1.0 - distance / SHOCKWAVE_RADIUS;
            entity.setDeltaMovement(entity.getDeltaMovement().add(
                    away.normalize().scale(1.5 + intensity * 5.5)).add(0.0, 0.6 + intensity * 1.8, 0.0));
            entity.hurtMarked = true;
            if (entity instanceof LivingEntity living) {
                living.hurt(level.damageSources().explosion(source, null), (float) (8.0 + intensity * 52.0));
            }
            if (entity instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
        }
    }

    private static boolean canAffect(Entity entity) {
        return entity.isAlive() && !(entity instanceof Player player && player.isSpectator());
    }

    private static final class BlastJob {
        private final BlockPos center;
        private final List<BlockPos> offsets = new ArrayList<>();
        private int index;
        private int age;
        private int debrisSpawned;

        private BlastJob(BlockPos center) {
            this.center = center;
            for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
                for (int y = -VERTICAL_RADIUS; y <= VERTICAL_RADIUS; y++) {
                    for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                        double normalized = normalizedDistance(x, y, z);
                        if (normalized <= 1.0) offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
            offsets.sort(Comparator.comparingDouble(pos -> normalizedDistance(pos.getX(), pos.getY(), pos.getZ())));
        }

        private boolean process(ServerLevel level) {
            int scanned = 0;
            int destroyed = 0;
            BlockPos.MutableBlockPos worldPos = new BlockPos.MutableBlockPos();
            while (index < offsets.size() && scanned < MAX_SCANNED_PER_TICK
                    && destroyed < MAX_DESTROYED_PER_TICK) {
                scanned++;
                BlockPos offset = offsets.get(index++);
                worldPos.set(center.getX() + offset.getX(), center.getY() + offset.getY(),
                        center.getZ() + offset.getZ());
                if (level.hasChunkAt(worldPos)) {
                    BlockState state = level.getBlockState(worldPos);
                    if (!state.isAir() && state.getDestroySpeed(level, worldPos) >= 0.0F) {
                        boolean surface = level.getBlockState(worldPos.above()).isAir();
                        if (surface && debrisSpawned < 96 && level.random.nextFloat() < 0.22F) {
                            FallingBlockEntity debris = FallingBlockEntity.fall(level, worldPos, state);
                            Vec3 away = Vec3.atCenterOf(worldPos).subtract(Vec3.atCenterOf(center));
                            Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
                            if (horizontal.lengthSqr() < 0.01) horizontal = new Vec3(1.0, 0.0, 0.0);
                            double distanceFactor = Math.min(1.0, horizontal.length() / HORIZONTAL_RADIUS);
                            debris.setDeltaMovement(horizontal.normalize().scale(0.75 + distanceFactor * 1.65)
                                    .add(0.0, 1.1 + level.random.nextDouble() * 1.5, 0.0));
                            debris.getPersistentData().putBoolean(DEBRIS_TAG, true);
                            debris.dropItem = false;
                            debris.hasImpulse = true;
                            debrisSpawned++;
                        } else {
                            level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 18);
                        }
                        destroyed++;
                    }
                }
            }
            age++;
            if (age % 2 == 0) renderShockwave(level);
            return index >= offsets.size();
        }

        private void renderShockwave(ServerLevel level) {
            double progress = Math.min(1.0, age / 45.0);
            double radius = 3.0 + progress * HORIZONTAL_RADIUS;
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2.0 * i / 16.0;
                level.sendParticles(ParticleTypes.EXPLOSION,
                        center.getX() + 0.5 + Math.cos(angle) * radius,
                        center.getY() + 0.5,
                        center.getZ() + 0.5 + Math.sin(angle) * radius,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        private static double normalizedDistance(int x, int y, int z) {
            return x * x / (double) (HORIZONTAL_RADIUS * HORIZONTAL_RADIUS)
                    + z * z / (double) (HORIZONTAL_RADIUS * HORIZONTAL_RADIUS)
                    + y * y / (double) (VERTICAL_RADIUS * VERTICAL_RADIUS);
        }
    }
}
