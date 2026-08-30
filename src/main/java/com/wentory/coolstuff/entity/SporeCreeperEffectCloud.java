package com.wentory.coolstuff.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import org.joml.Vector3f;

final class SporeCreeperEffectCloud {
    private static final DustParticleOptions YELLOW_SPORES =
            new DustParticleOptions(new Vector3f(0.8196F, 0.7647F, 0.0F), 1.35F);
    private static final DustParticleOptions ORANGE_SPORES =
            new DustParticleOptions(new Vector3f(0.8196F, 0.6784F, 0.1647F), 1.2F);

    private SporeCreeperEffectCloud() {
    }

    static void spawnExplosionParticles(ServerLevel level, Vec3 position, boolean powered) {
        double spread = powered ? 2.2 : 1.35;
        int yellowCount = powered ? 60 : 34;
        int orangeCount = powered ? 30 : 16;
        level.sendParticles(YELLOW_SPORES, position.x, position.y + 0.35, position.z,
                yellowCount, spread, spread * 0.75, spread, 0.08);
        level.sendParticles(ORANGE_SPORES, position.x, position.y + 0.35, position.z,
                orangeCount, spread, spread * 0.75, spread, 0.065);
    }

    static void spawn(ServerLevel level, Vec3 position, Collection<MobEffectInstance> effects) {
        if (effects.isEmpty()) return;
        AreaEffectCloud cloud = new AreaEffectCloud(level, position.x, position.y, position.z);
        cloud.setRadius(2.5F);
        cloud.setRadiusOnUse(-0.5F);
        cloud.setWaitTime(10);
        cloud.setDuration(cloud.getDuration() / 2);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        for (MobEffectInstance effect : effects) cloud.addEffect(new MobEffectInstance(effect));
        level.addFreshEntity(cloud);
    }
}