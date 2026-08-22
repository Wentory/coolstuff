package com.wentory.coolstuff.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class BlackHoleExplosionEntity extends Entity {
    public BlackHoleExplosionEntity(EntityType<? extends BlackHoleExplosionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0, 0.0, 0.0);
        if (!level().isClientSide() && tickCount >= 55) discard();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
