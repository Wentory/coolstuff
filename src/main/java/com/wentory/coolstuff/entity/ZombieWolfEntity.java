package com.wentory.coolstuff.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class ZombieWolfEntity extends Wolf {
    private static final EntityDataAccessor<Boolean> HAS_COLLAR =
            SynchedEntityData.defineId(ZombieWolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COLLAR_COLOR =
            SynchedEntityData.defineId(ZombieWolfEntity.class, EntityDataSerializers.INT);
    private static final double SUPPORT_RADIUS = 12.0;
    private static final double PACK_SEARCH_RADIUS = 24.0;

    private int defensiveTicks;
    private int observedHurtTimestamp = -1;
    @Nullable
    private UUID zombieOwnerUuid;

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
        goalSelector.addGoal(3, new FollowZombieOwnerGoal());
        goalSelector.addGoal(7, new StayWithPackGoal());
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

        // Recover the owner for companions saved before owner UUIDs were added.
        if (hasDisplayCollar() && zombieOwnerUuid == null && tickCount % 40 == 0) {
            level().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,
                            getBoundingBox().inflate(16.0), Entity::isAlive)
                    .stream()
                    .min((left, right) -> Double.compare(distanceToSqr(left), distanceToSqr(right)))
                    .ifPresent(this::setZombieOwner);
        }

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
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof AbstractSkeleton) && super.canAttack(target);
    }

    public void setZombieOwner(LivingEntity owner) {
        zombieOwnerUuid = owner.getUUID();
    }

    @Nullable
    public LivingEntity getZombieOwner() {
        if (zombieOwnerUuid == null || !(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(zombieOwnerUuid);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
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
        if (zombieOwnerUuid != null) tag.putUUID("ZombieOwner", zombieOwnerUuid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("HasZombieCollar")) {
            setDisplayCollar(DyeColor.byId(tag.getInt("ZombieCollarColor")));
        }
        defensiveTicks = tag.getInt("DefensiveTicks");
        zombieOwnerUuid = tag.hasUUID("ZombieOwner") ? tag.getUUID("ZombieOwner") : null;
    }

    private final class FollowZombieOwnerGoal extends Goal {
        @Nullable
        private LivingEntity owner;
        private int pathRecalculationTicks;

        private FollowZombieOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!hasDisplayCollar() || isPassenger()) return false;
            LivingEntity candidate = getZombieOwner();
            if (candidate == null || distanceToSqr(candidate) < 25.0) return false;
            owner = candidate;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && !isPassenger() && distanceToSqr(owner) > 6.25;
        }

        @Override
        public void start() {
            pathRecalculationTicks = 0;
        }

        @Override
        public void stop() {
            owner = null;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (owner == null) return;
            getLookControl().setLookAt(owner, 10.0F, getMaxHeadXRot());
            if (--pathRecalculationTicks <= 0) {
                pathRecalculationTicks = adjustedTickDelay(10);
                getNavigation().moveTo(owner, 1.25);
            }
        }
    }

    private boolean isCalmPackMember() {
        return !hasDisplayCollar() && !isPassenger() && getTarget() == null && defensiveTicks <= 0;
    }

    private final class StayWithPackGoal extends Goal {
        private static final double FOLLOW_START_DISTANCE = 9.0;
        private static final double FOLLOW_STOP_DISTANCE = 7.0;
        private static final double FORMATION_RADIUS = 5.5;

        @Nullable
        private ZombieWolfEntity leader;
        private int pathRecalculationTicks;

        private StayWithPackGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!isCalmPackMember() || getRandom().nextInt(10) != 0) return false;
            leader = findPackLeader();
            return leader != null && leader != ZombieWolfEntity.this
                    && distanceToSqr(leader) > FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            return isCalmPackMember() && leader != null && leader.isAlive() && leader.isCalmPackMember()
                    && distanceToSqr(leader) > FOLLOW_STOP_DISTANCE * FOLLOW_STOP_DISTANCE;
        }

        @Override
        public void start() {
            pathRecalculationTicks = 0;
        }

        @Override
        public void stop() {
            leader = null;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (leader == null) return;
            if (--pathRecalculationTicks <= 0) {
                pathRecalculationTicks = adjustedTickDelay(10);
                ZombieWolfEntity electedLeader = findPackLeader();
                if (electedLeader == null || electedLeader == ZombieWolfEntity.this) {
                    stop();
                    return;
                }
                leader = electedLeader;
                Vec3 formationPoint = formationPointAround(leader);
                getNavigation().moveTo(formationPoint.x, formationPoint.y, formationPoint.z, 1.1);
            }
        }

        @Nullable
        private ZombieWolfEntity findPackLeader() {
            List<ZombieWolfEntity> packmates = level().getEntitiesOfClass(ZombieWolfEntity.class,
                    getBoundingBox().inflate(PACK_SEARCH_RADIUS),
                    wolf -> wolf.isAlive() && wolf.isCalmPackMember());
            if (packmates.isEmpty()) return null;
            return packmates.stream()
                    .min((left, right) -> Integer.compare(left.getId(), right.getId()))
                    .orElse(null);
        }

        private Vec3 formationPointAround(ZombieWolfEntity packLeader) {
            long seed = getUUID().getMostSignificantBits() ^ getUUID().getLeastSignificantBits();
            double angle = Math.floorMod(seed, 360L) * (Math.PI / 180.0);
            return packLeader.position().add(Math.cos(angle) * FORMATION_RADIUS, 0.0,
                    Math.sin(angle) * FORMATION_RADIUS);
        }
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
