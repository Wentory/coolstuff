package com.wentory.coolstuff.entity;

import com.wentory.coolstuff.network.ParryEffectPayload;
import com.wentory.coolstuff.registry.ModDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public final class LeapingCreeperProjectileEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> POWERED = SynchedEntityData.defineId(
            LeapingCreeperProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> REFLECTED = SynchedEntityData.defineId(
            LeapingCreeperProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FARTED = SynchedEntityData.defineId(
            LeapingCreeperProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private int flightTicks;
    private UUID parriedBy;

    public LeapingCreeperProjectileEntity(EntityType<? extends LeapingCreeperProjectileEntity> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(POWERED, false);
        builder.define(REFLECTED, false);
        builder.define(FARTED, false);
    }

    public boolean isPoweredProjectile() {
        return entityData.get(POWERED);
    }

    public void setPoweredProjectile(boolean powered) {
        entityData.set(POWERED, powered);
    }

    public boolean isReflected() {
        return entityData.get(REFLECTED);
    }

    public boolean hasFarted() {
        return entityData.get(FARTED);
    }

    public void setFarted(boolean farted) {
        entityData.set(FARTED, farted);
    }

    @Override
    public void tick() {
        super.tick();
        flightTicks++;
        Vec3 velocity = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !level().isClientSide()) {
            if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Player player
                    && !isReflected() && isBlockingWithShield(player)) {
                if (player.getTicksUsingItem() <= 4) {
                    parry(player);
                } else {
                    damageShield(player, 50);
                    explodeNow();
                }
            } else {
                onHit(hit);
            }
            if (isRemoved()) return;
            velocity = getDeltaMovement();
        }

        move(MoverType.SELF, velocity);
        double horizontal = velocity.horizontalDistance();
        setYRot((float) (Math.atan2(velocity.x, velocity.z) * 180.0 / Math.PI));
        setXRot((float) (Math.atan2(velocity.y, Math.max(0.001, horizontal)) * 180.0 / Math.PI));
        setDeltaMovement(velocity.scale(0.995).add(0.0, -0.05, 0.0));
        if (!level().isClientSide() && ((isReflected() && flightTicks >= 34)
                || (!isReflected() && flightTicks >= 50))) explodeNow();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        explodeNow();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explodeNow();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (target instanceof LeapingCreeperEntity || target instanceof LeapingCreeperProjectileEntity) return false;
        return super.canHitEntity(target) && !(isReflected() && flightTicks < 5 && parriedBy != null
                && target.getUUID().equals(parriedBy));
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
        entityData.set(REFLECTED, true);
        parriedBy = player.getUUID();
        setOwner(player);
        flightTicks = 0;
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

    private void explodeNow() {
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) return;
        float radius = isPoweredProjectile() ? 6.0F : 3.0F;
        if (isReflected() && parriedBy != null) {
            Player owner = serverLevel.getPlayerByUUID(parriedBy);
            serverLevel.explode(this, ModDamageTypes.creeperParry(serverLevel, this, owner, hasFarted()), null,
                    position(), radius, false, Level.ExplosionInteraction.MOB);
        } else {
            serverLevel.explode(this, ModDamageTypes.sporeCreeper(serverLevel, this, hasFarted()), null,
                    position(), radius, false, Level.ExplosionInteraction.MOB);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Powered", isPoweredProjectile());
        tag.putBoolean("Reflected", isReflected());
        tag.putBoolean("Farted", hasFarted());
        tag.putInt("FlightTicks", flightTicks);
        if (parriedBy != null) tag.putUUID("ParriedBy", parriedBy);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPoweredProjectile(tag.getBoolean("Powered"));
        entityData.set(REFLECTED, tag.getBoolean("Reflected"));
        setFarted(tag.getBoolean("Farted"));
        flightTicks = tag.getInt("FlightTicks");
        if (tag.hasUUID("ParriedBy")) parriedBy = tag.getUUID("ParriedBy");
    }
}
