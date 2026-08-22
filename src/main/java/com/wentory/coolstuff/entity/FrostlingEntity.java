package com.wentory.coolstuff.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class FrostlingEntity extends Zombie implements RangedAttackMob {
    private static final EntityDataAccessor<Boolean> HAS_SNOWBALL =
            SynchedEntityData.defineId(FrostlingEntity.class, EntityDataSerializers.BOOLEAN);
    private int snowballCooldown;

    public FrostlingEntity(EntityType<? extends FrostlingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_SNOWBALL, false);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        // Use the level RNG: freshly constructed entities spawned in a rapid
        // command burst can have correlated first values in their own RNGs.
        setHasSnowball(level.getRandom().nextBoolean());
        return result;
    }

    public boolean hasSnowball() {
        return entityData.get(HAS_SNOWBALL);
    }

    public void setHasSnowball(boolean value) {
        entityData.set(HAS_SNOWBALL, value);
        setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                value ? new ItemStack(Items.SNOWBALL) : ItemStack.EMPTY);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!hasSnowball() || !(level() instanceof ServerLevel)) return;
        Snowball snowball = new Snowball(level(), this);
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - 0.2 - snowball.getY();
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        snowball.shoot(dx, dy + horizontal * 0.12, dz, 1.25F, 7.0F);
        playSound(SoundEvents.SNOWBALL_THROW, 1.0F, 0.9F + random.nextFloat() * 0.2F);
        level().addFreshEntity(snowball);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || !hasSnowball()) return;
        if (snowballCooldown > 0) snowballCooldown--;
        LivingEntity target = getTarget();
        if (snowballCooldown <= 0 && target != null && target.isAlive()
                && distanceToSqr(target) <= 144.0 && getSensing().hasLineOfSight(target)) {
            performRangedAttack(target, (float) Math.sqrt(distanceToSqr(target)) / 12.0F);
            snowballCooldown = 35 + random.nextInt(21);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this);
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasSnowball", hasSnowball());
        tag.putInt("SnowballCooldown", snowballCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHasSnowball(tag.getBoolean("HasSnowball"));
        snowballCooldown = tag.getInt("SnowballCooldown");
    }
}
