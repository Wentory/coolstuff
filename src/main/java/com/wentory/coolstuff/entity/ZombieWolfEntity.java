package com.wentory.coolstuff.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

public final class ZombieWolfEntity extends Wolf {
    private static final EntityDataAccessor<Boolean> HAS_COLLAR =
            SynchedEntityData.defineId(ZombieWolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COLLAR_COLOR =
            SynchedEntityData.defineId(ZombieWolfEntity.class, EntityDataSerializers.INT);
    private static final double SUPPORT_RADIUS = 12.0;

    private int defensiveTicks;
    private int observedHurtTimestamp = -1;

    public ZombieWolfEntity(EntityType<? extends ZombieWolfEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected int getBaseExperienceReward() {
        return 3;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_COLLAR, false);
        builder.define(COLLAR_COLOR, DyeColor.RED.getId());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> goal instanceof SitWhenOrderedToGoal);
        goalSelector.addGoal(2, new AvoidPlayerWhenAloneGoal());
        targetSelector.addGoal(3, new SupportedPlayerTargetGoal());
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
    }

    @Override
    public void aiStep() {
        if (isOrderedToSit()) setOrderedToSit(false);
        super.aiStep();
        if (level().isClientSide()) return;

        if (isAlive() && isSunBurnTick()) igniteForSeconds(8.0F);

        int hurtTimestamp = getLastHurtByMobTimestamp();
        if (hurtTimestamp != observedHurtTimestamp) {
            observedHurtTimestamp = hurtTimestamp;
            if (getLastHurtByMob() != null) defensiveTicks = 100;
        }
        if (defensiveTicks > 0) defensiveTicks--;

        LivingEntity target = getTarget();
        if (target instanceof AbstractSkeleton) {
            setTarget(null);
            return;
        }
        if (target instanceof Player && defensiveTicks <= 0 && !hasUndeadSupport()) {
            setTarget(null);
        }
    }

    public boolean hasUndeadSupport() {
        return !level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SUPPORT_RADIUS),
                entity -> entity != this && entity.isAlive() && entity.getType().is(EntityTypeTags.UNDEAD)).isEmpty();
    }

    private boolean shouldAvoidPlayers() {
        return defensiveTicks <= 0 && !hasUndeadSupport();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal otherAnimal) {
        return false;
    }

    public boolean hasDisplayCollar() {
        return entityData.get(HAS_COLLAR);
    }

    public void setDisplayCollar(DyeColor color) {
        entityData.set(HAS_COLLAR, true);
        entityData.set(COLLAR_COLOR, color.getId());
        setTame(true, false);
    }

    public DyeColor getDisplayCollarColor() {
        return DyeColor.byId(entityData.get(COLLAR_COLOR));
    }

    @Override
    public DyeColor getCollarColor() {
        return getDisplayCollarColor();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasZombieCollar", hasDisplayCollar());
        tag.putInt("ZombieCollarColor", getDisplayCollarColor().getId());
        tag.putInt("DefensiveTicks", defensiveTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("HasZombieCollar")) {
            setDisplayCollar(DyeColor.byId(tag.getInt("ZombieCollarColor")));
        }
        defensiveTicks = tag.getInt("DefensiveTicks");
    }

    private final class AvoidPlayerWhenAloneGoal extends AvoidEntityGoal<Player> {
        private AvoidPlayerWhenAloneGoal() {
            super(ZombieWolfEntity.this, Player.class, 10.0F, 1.0, 1.25);
        }

        @Override
        public boolean canUse() {
            return shouldAvoidPlayers() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return shouldAvoidPlayers() && super.canContinueToUse();
        }
    }

    private final class SupportedPlayerTargetGoal extends NearestAttackableTargetGoal<Player> {
        private SupportedPlayerTargetGoal() {
            super(ZombieWolfEntity.this, Player.class, true);
        }

        @Override
        public boolean canUse() {
            return hasUndeadSupport() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return (defensiveTicks > 0 || hasUndeadSupport()) && super.canContinueToUse();
        }
    }
}
