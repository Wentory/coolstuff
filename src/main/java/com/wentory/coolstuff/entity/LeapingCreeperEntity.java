package com.wentory.coolstuff.entity;

import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import com.wentory.coolstuff.network.ParryEffectPayload;
import com.wentory.coolstuff.registry.ModDamageTypes;
import com.wentory.coolstuff.registry.ModEntities;
import com.wentory.coolstuff.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.joml.Vector3f;

public final class LeapingCreeperEntity extends Creeper {
    private static final DustParticleOptions YELLOW_SPORES =
            new DustParticleOptions(new Vector3f(0.95F, 0.78F, 0.08F), 1.25F);
    public final AnimationState chargeAnimationState = new AnimationState();
    private static final EntityDataAccessor<Integer> ATTACK_STATE = SynchedEntityData.defineId(
            LeapingCreeperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PREPARATION_TICKS = SynchedEntityData.defineId(
            LeapingCreeperEntity.class, EntityDataSerializers.INT);
    private static final int IDLE = 0;
    private static final int PREPARING = 1;
    private static final int FLYING = 2;
    private static final int REFLECTED = 3;
    private int flightTicks;
    private int launchCooldown;
    private UUID parriedBy;

    public LeapingCreeperEntity(EntityType<? extends LeapingCreeperEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return EntityType.CREEPER.getDefaultLootTable();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> goal instanceof SwellGoal);
        goalSelector.addGoal(2, new LeapAttackGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_STATE, IDLE);
        builder.define(PREPARATION_TICKS, 0);
    }

    public int getAttackState() {
        return entityData.get(ATTACK_STATE);
    }

    public float getPreparationProgress() {
        return getPreparationProgress(1.0F);
    }

    public float getPreparationProgress(float partialTick) {
        float interpolatedTicks = Math.max(0.0F, entityData.get(PREPARATION_TICKS) - 1.0F + partialTick);
        return Math.min(1.0F, interpolatedTicks / 20.0F);
    }

    public static double getSporeFartChance() {
        return CoolstuffConfig.SPORE_FART_CHANCE.get();
    }

    public static void setSporeFartChance(double chance) {
        CoolstuffConfig.SPORE_FART_CHANCE.set(Math.max(0.0, Math.min(1.0, chance)));
        if (CoolstuffConfig.SPEC.isLoaded()) CoolstuffConfig.SPEC.save();
    }

    private void setAttackState(int state) {
        entityData.set(ATTACK_STATE, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (getAttackState() == PREPARING) chargeAnimationState.startIfStopped(tickCount);
        else chargeAnimationState.stop();
        if (launchCooldown > 0) launchCooldown--;
        if (getAttackState() != FLYING && getAttackState() != REFLECTED) return;

        getNavigation().stop();
        setSwellDir(-1);
        flightTicks++;
        Vec3 velocity = getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal > 0.01) {
            setYRot((float) (Math.atan2(velocity.z, velocity.x) * 180.0 / Math.PI) - 90.0F);
            yBodyRot = getYRot();
        }

        if (!level().isClientSide()) {
            if (getAttackState() == FLYING && flightTicks > 2 && hitPlayer()) return;
            if (flightTicks > 2 && (horizontalCollision || verticalCollision)) {
                explodeNow();
                return;
            }
            if (getAttackState() == REFLECTED && flightTicks >= 34) explodeNow();
        }
    }

    private boolean hitPlayer() {
        List<Player> players = level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(0.45), player -> player.isAlive() && !player.isSpectator());
        if (players.isEmpty()) return false;
        Player player = players.getFirst();
        if (isBlockingWithShield(player)) {
            if (player.getTicksUsingItem() <= 4) {
                parry(player);
                return true;
            }
            damageShield(player, 50);
        }
        explodeNow();
        return true;
    }

    private static boolean isBlockingWithShield(Player player) {
        return player.isUsingItem() && player.getUseItem().is(Items.SHIELD);
    }

    private static void damageShield(Player player, int amount) {
        ItemStack shield = player.getUseItem();
        InteractionHand hand = player.getUsedItemHand();
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        shield.hurtAndBreak(amount, player, slot);
    }

    private void parry(Player player) {
        setAttackState(REFLECTED);
        parriedBy = player.getUUID();
        flightTicks = 0;
        setTarget(null);
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 0.01) horizontal = new Vec3(0.0, 0.0, 1.0);
        setDeltaMovement(horizontal.normalize().scale(1.55).add(0.0, 0.82 + look.y * 0.25, 0.0));
        hasImpulse = true;
        playSound(SoundEvents.ANVIL_LAND, 1.4F, 1.35F);
        if (level() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null, getX(), getY(), getZ(), 64.0,
                    new ParryEffectPayload(getX(), getY(), getZ(), 1));
        }
    }

    private void launchAt(LivingEntity target) {
        double distanceToTarget = distanceTo(target);
        Vec3 destination = target.getBoundingBox().getCenter();
        Vec3 targetMovement = new Vec3(target.getDeltaMovement().x, 0.0, target.getDeltaMovement().z);
        if (distanceToTarget > 4.0 && targetMovement.lengthSqr() > 0.0009) {
            destination = destination.add(targetMovement.normalize().scale(2.0));
        }
        Vec3 launchPosition = getEyePosition();
        Vec3 displacement = destination.subtract(launchPosition);
        double horizontalDistance = Math.sqrt(displacement.x * displacement.x + displacement.z * displacement.z);
        double flightTime = Math.max(8.0, Math.min(14.0, horizontalDistance * 1.45));
        double vx = displacement.x / flightTime;
        double vz = displacement.z / flightTime;
        double vy = (displacement.y + 0.5 * 0.05 * flightTime * flightTime) / flightTime;
        if (!(level() instanceof ServerLevel serverLevel)) return;
        LeapingCreeperProjectileEntity projectile = new LeapingCreeperProjectileEntity(
                ModEntities.LEAPING_CREEPER_PROJECTILE.orElseThrow().get(), serverLevel);
        projectile.setPos(launchPosition.x, launchPosition.y, launchPosition.z);
        projectile.setPoweredProjectile(isPowered());
        projectile.setCarriedEffects(getActiveEffects());
        boolean farted = serverLevel.random.nextDouble() < CoolstuffConfig.SPORE_FART_CHANCE.get();
        projectile.setFarted(farted);
        projectile.setDeltaMovement(vx, vy, vz);
        projectile.hasImpulse = true;
        serverLevel.addFreshEntity(projectile);
        projectile.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
        playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.1F, 0.72F);
        if (farted) {
            Vec3 horizontalLaunch = new Vec3(vx, 0.0, vz);
            if (horizontalLaunch.lengthSqr() < 0.001) horizontalLaunch = getLookAngle().multiply(1.0, 0.0, 1.0);
            Vec3 exhaustDirection = horizontalLaunch.lengthSqr() < 0.001
                    ? new Vec3(0.0, 0.0, -1.0)
                    : horizontalLaunch.normalize().scale(-1.0);
            Vec3 spores = position().add(exhaustDirection.scale(0.55)).add(0.0, 0.45, 0.0);
            serverLevel.playSound(null, spores.x, spores.y, spores.z, ModSounds.SPORE_FART.get(),
                    SoundSource.HOSTILE, 1.0F, 0.92F + serverLevel.random.nextFloat() * 0.16F);
            serverLevel.sendParticles(YELLOW_SPORES, spores.x, spores.y, spores.z,
                    24, 0.28, 0.22, 0.28, 0.045);
        }
        discard();
    }

    private void explodeNow() {
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) return;
        float radius = isPowered() ? 6.0F : 3.0F;
        boolean damageBlocks = getAttackState() == REFLECTED
                ? CoolstuffConfig.PARRIED_SPORE_CREEPER_BLOCK_DAMAGE.get()
                : CoolstuffConfig.SPORE_CREEPER_BLOCK_DAMAGE.get();
        Level.ExplosionInteraction interaction = damageBlocks
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        if (getAttackState() == REFLECTED && parriedBy != null) {
            Player owner = serverLevel.getPlayerByUUID(parriedBy);
            serverLevel.explode(this, ModDamageTypes.creeperParry(serverLevel, this, owner), null,
                    position(), radius, false, interaction);
        } else {
            serverLevel.explode(this, getX(), getY(), getZ(), radius, interaction);
        }
        SporeCreeperEffectCloud.spawnExplosionParticles(serverLevel, position(), isPowered());
        SporeCreeperEffectCloud.spawn(serverLevel, position(), getActiveEffects());
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AttackState", getAttackState());
        tag.putInt("PreparationTicks", entityData.get(PREPARATION_TICKS));
        tag.putInt("FlightTicks", flightTicks);
        if (parriedBy != null) tag.putUUID("ParriedBy", parriedBy);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setAttackState(tag.getInt("AttackState"));
        entityData.set(PREPARATION_TICKS, tag.getInt("PreparationTicks"));
        flightTicks = tag.getInt("FlightTicks");
        if (tag.hasUUID("ParriedBy")) parriedBy = tag.getUUID("ParriedBy");
    }

    private static final class LeapAttackGoal extends Goal {
        private final LeapingCreeperEntity creeper;
        private LivingEntity target;
        private int prepareTicks;

        private LeapAttackGoal(LeapingCreeperEntity creeper) {
            this.creeper = creeper;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!RestartRequiredConfig.sporeCreeper()) return false;
            target = creeper.getTarget();
            if (target == null || !target.isAlive() || !creeper.onGround() || creeper.launchCooldown > 0
                    || creeper.getAttackState() != IDLE) return false;
            double distance = creeper.distanceTo(target);
            return distance <= 8.0 && creeper.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && creeper.getAttackState() == PREPARING;
        }

        @Override
        public void start() {
            prepareTicks = 0;
            creeper.entityData.set(PREPARATION_TICKS, 0);
            creeper.getNavigation().stop();
            creeper.setAttackState(PREPARING);
        }

        @Override
        public void stop() {
            if (creeper.getAttackState() == PREPARING) {
                creeper.setAttackState(IDLE);
                creeper.entityData.set(PREPARATION_TICKS, 0);
                creeper.launchCooldown = 20;
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            creeper.getLookControl().setLookAt(target, 30.0F, 30.0F);
            prepareTicks++;
            creeper.entityData.set(PREPARATION_TICKS, prepareTicks);
            if (prepareTicks >= 20) creeper.launchAt(target);
        }
    }
}
