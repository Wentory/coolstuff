package com.wentory.coolstuff.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class BlackHoleEntity extends Entity {
    private static final EntityDataAccessor<Integer> REMAINING_TICKS =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ETERNAL =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PACIFIED =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.BOOLEAN);

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(REMAINING_TICKS, 200);
        builder.define(ETERNAL, false);
        builder.define(PACIFIED, false);
    }

    public int getRemainingTicks() {
        return entityData.get(REMAINING_TICKS);
    }

    public void setRemainingTicks(int ticks) {
        entityData.set(REMAINING_TICKS, ticks);
    }

    public boolean isEternal() {
        return entityData.get(ETERNAL);
    }

    public void setEternal(boolean eternal) {
        entityData.set(ETERNAL, eternal);
    }

    public boolean isPacified() {
        return entityData.get(PACIFIED);
    }

    public void setPacified(boolean pacified) {
        entityData.set(PACIFIED, pacified);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setRemainingTicks(tag.getInt("RemainingTicks"));
        setEternal(tag.getBoolean("Eternal"));
        setPacified(tag.getBoolean("Pacified"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("RemainingTicks", getRemainingTicks());
        tag.putBoolean("Eternal", isEternal());
        tag.putBoolean("Pacified", isPacified());
    }
}
