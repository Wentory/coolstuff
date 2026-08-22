package com.wentory.coolstuff.entity;

import com.wentory.coolstuff.cake.CakeFilling;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import com.wentory.coolstuff.mixin.CreeperAccessor;
import com.wentory.coolstuff.registry.ModSounds;
import com.wentory.coolstuff.network.ParryEffectPayload;
import com.wentory.coolstuff.server.CreeperTransformation;
import com.wentory.coolstuff.server.CakeDaze;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import org.joml.Vector3f;

public final class ThrownCakeEntity extends ThrowableProjectile {
    public static final int FLYING = 0;
    public static final int ATTACHED = 1;
    public static final int WALL = 2;
    public static final int DROPPED = 3;
    public static final int SPLATTED = 4;
    public static final int CEILING = 5;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(
            ThrownCakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE_START = SynchedEntityData.defineId(
            ThrownCakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACHED_ID = SynchedEntityData.defineId(
            ThrownCakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SURFACE_FACE = SynchedEntityData.defineId(
            ThrownCakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> IMPACT_POSITION = SynchedEntityData.defineId(
            ThrownCakeEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> WALL_START = SynchedEntityData.defineId(
            ThrownCakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WALL_STOP_AGE = SynchedEntityData.defineId(
            ThrownCakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> WALL_POSITION = SynchedEntityData.defineId(
            ThrownCakeEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Direction> WALL_FACE = SynchedEntityData.defineId(
            ThrownCakeEntity.class, EntityDataSerializers.DIRECTION);
    private UUID attachedUuid;
    private CakeFilling filling = CakeFilling.NONE;

    public ThrownCakeEntity(EntityType<? extends ThrownCakeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, FLYING);
        builder.define(STATE_START, 0);
        builder.define(ATTACHED_ID, -1);
        builder.define(SURFACE_FACE, Direction.UP.get3DDataValue());
        builder.define(IMPACT_POSITION, new Vector3f());
        builder.define(WALL_START, -1);
        builder.define(WALL_STOP_AGE, -1);
        builder.define(WALL_POSITION, new Vector3f());
        builder.define(WALL_FACE, Direction.NORTH);
    }

    @Override
    protected double getDefaultGravity() {
        return getCakeState() == FLYING || getCakeState() == DROPPED ? 0.045 : 0.0;
    }

    @Override
    public void tick() {
        int state = getCakeState();
        if (state == FLYING || state == DROPPED) {
            super.tick();
            return;
        }
        setOldPosAndRot();
        tickCount++;
        if (state == ATTACHED) tickAttached();
        else if (state == WALL) tickWall();
        else if (state == CEILING && getStateAge(0.0F) >= 40.0F && !level().isClientSide()) beginCeilingDrop();
        else if (state == SPLATTED && getStateAge(0.0F) >= 180.0F && !level().isClientSide()) {
            BlockPos signalPos = blockPosition();
            discard();
            updateRedstoneSignal(signalPos);
        }
    }

    private void tickAttached() {
        Entity target = getAttachedEntity();
        if (!(target instanceof LivingEntity living) || !target.isAlive()) {
            if (!level().isClientSide()) beginDrop();
            return;
        }
        float age = getStateAge(0.0F);
        double slide = Math.min(0.85, age / 48.0 * 0.85);
        Vec3 forward = living.getLookAngle().normalize();
        double faceDistance = living.getBbWidth() * 0.5 + 0.035;
        BlockPos oldSignalPos = blockPosition();
        setPos(living.getX() + forward.x * faceDistance,
                living.getEyeY() - 0.22 - slide + forward.y * 0.25,
                living.getZ() + forward.z * faceDistance);
        updateRedstoneSignal(oldSignalPos);
        setYRot(living.getYHeadRot());
        setXRot(living.getXRot());
        if (age >= 48.0F && !level().isClientSide()) beginDrop();
    }

    private void tickWall() {
        float age = getStateAge(0.0F);
        Vec3 outward = Vec3.atLowerCornerOf(getSurfaceFace().getNormal()).scale(0.035);
        Vec3 previous = position();
        BlockPos oldSignalPos = blockPosition();
        Vec3 next = getImpactPosition().add(outward).add(0.0, -Math.min(2.7, age * 0.025), 0.0);

        // Probe below the outer half of the cake, away from the wall itself.
        // The rendered wall cake is roughly one block tall, centred on this entity.
        Vec3 probeOffset = Vec3.atLowerCornerOf(getSurfaceFace().getNormal()).scale(0.22);
        Vec3 probeFrom = previous.add(probeOffset).add(0.0, -0.48, 0.0);
        Vec3 probeTo = next.add(probeOffset).add(0.0, -0.55, 0.0);
        BlockHitResult floorHit = level().clip(new ClipContext(
                probeFrom, probeTo, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (floorHit.getType() == HitResult.Type.BLOCK && floorHit.getDirection() == Direction.UP) {
            setPos(next.x, floorHit.getLocation().y + 0.5, next.z);
            entityData.set(WALL_STOP_AGE, Math.max(0, tickCount - entityData.get(WALL_START)));
            setCakeState(SPLATTED);
            updateRedstoneSignal(oldSignalPos);
            return;
        }

        setPos(next);
        updateRedstoneSignal(oldSignalPos);
        if (age >= 108.0F && !level().isClientSide()) beginDrop();
    }

    private void beginDrop() {
        BlockPos signalPos = blockPosition();
        setCakeState(DROPPED);
        entityData.set(ATTACHED_ID, -1);
        attachedUuid = null;
        noPhysics = false;
        setDeltaMovement(0.0, -0.08, 0.0);
        hasImpulse = true;
        updateRedstoneSignal(signalPos);
    }

    private void beginCeilingDrop() {
        // Sandwich law: once it lets go, the frosted top (+Y in the model)
        // turns downward. Surface face UP produces exactly that orientation.
        setImpact(position(), Direction.UP);
        beginDrop();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide() || getCakeState() != FLYING) return;
        Entity target = result.getEntity();
        if (target instanceof LeapingCreeperEntity sporeCreeper) target = disarm(sporeCreeper);
        if (target instanceof LivingEntity living && target.isAlive()) {
            if (living instanceof Player player && isTimedShieldSplat(player)) {
                splatOnShield(player);
                return;
            }
            // A cake to the face is still technically a serving of cake.
            if (living instanceof Player player) {
                player.getFoodData().eat(2, 0.1F);
            }
            if (applyFillingTo(living)) return;
            if (living instanceof Mob mob) CakeDaze.apply(mob);
            attachTo(living);
        }
    }

    private boolean isTimedShieldSplat(Player player) {
        return player.isUsingItem() && player.getUseItem().is(Items.SHIELD)
                && player.getTicksUsingItem() <= 5;
    }

    private void splatOnShield(Player player) {
        Vec3 impact = position();
        splatEffects();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.65F, 1.25F);
        PacketDistributor.sendToPlayersTrackingEntity(this,
                new ParryEffectPayload(impact.x, impact.y, impact.z, 1, ParryEffectPayload.CAKE_SPLAT));
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return getCakeState() == FLYING && super.canHitEntity(target);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide() || (getCakeState() != FLYING && getCakeState() != DROPPED)) return;
        if (RestartRequiredConfig.cakeFillings() && getCakeState() == FLYING && filling == CakeFilling.GUNPOWDER) {
            explodeGunpowder();
            return;
        }
        splatOnSurface(result.getLocation(), result.getDirection());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide() && !isRemoved() && getCakeState() == FLYING
                && result.getType() != HitResult.Type.MISS) {
            splatOnSurface(result.getLocation(), Direction.UP);
        }
    }

    private void attachTo(LivingEntity target) {
        entityData.set(ATTACHED_ID, target.getId());
        attachedUuid = target.getUUID();
        setCakeState(ATTACHED);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        splatEffects();
    }

    private void splatOnSurface(Vec3 location, Direction face) {
        boolean secondaryDrop = getCakeState() == DROPPED;
        setImpact(location, face);
        setPos(location.add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.025)));
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        if (face.getAxis().isHorizontal()) {
            entityData.set(WALL_START, tickCount);
            entityData.set(WALL_STOP_AGE, -1);
            entityData.set(WALL_POSITION,
                    new Vector3f((float) location.x, (float) location.y, (float) location.z));
            entityData.set(WALL_FACE, face);
            setCakeState(WALL);
        } else if (face == Direction.DOWN) {
            setCakeState(CEILING);
        } else {
            setCakeState(SPLATTED);
        }
        if (!secondaryDrop) splatEffects();
        updateRedstoneSignal(blockPosition());
    }

    private void splatEffects() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CAKE)),
                getX(), getY(), getZ(), 18, 0.32, 0.22, 0.32, 0.08);
        serverLevel.playSound(null, getX(), getY(), getZ(), ModSounds.CAKE_SPLAT.get(),
                SoundSource.PLAYERS, 1.0F, 0.92F + random.nextFloat() * 0.12F);
    }

    private boolean applyFillingTo(LivingEntity target) {
        if (!RestartRequiredConfig.cakeFillings()) return false;
        switch (filling) {
            case GUNPOWDER -> {
                explodeGunpowder();
                return true;
            }
            case STONE -> target.hurt(level().damageSources().thrown(this, getOwner()), 1.0F);
            case ICE -> {
                target.hurt(level().damageSources().thrown(this, getOwner()), 1.0F);
                freezeNearlyCompletely(target);
            }
            case SNOW -> freezeNearlyCompletely(target);
            case CHORUS -> chorusTeleport(target);
            case END_ROD -> target.hurt(level().damageSources().thrown(this, getOwner()), 3.0F);
            case CANDLE -> target.hurt(level().damageSources().thrown(this, getOwner()), 1.0F);
            case LIGHTNING_ROD -> target.hurt(level().damageSources().thrown(this, getOwner()), 2.0F);
            case SLIME -> {
                Vec3 direction = getDeltaMovement().normalize();
                target.push(direction.x * 0.65, 0.25, direction.z * 0.65);
                target.hurtMarked = true;
            }
            case HONEY -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            case GLOW_INK -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            case INK -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            case PUFFERFISH -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            case PHANTOM_MEMBRANE -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
            case ECHO -> {
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                level().playSound(null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.PLAYERS, 1.0F, 1.25F);
            }
            default -> {
            }
        }
        return false;
    }

    private static void freezeNearlyCompletely(LivingEntity target) {
        target.setTicksFrozen(Math.max(target.getTicksFrozen(),
                Math.max(1, (int) (target.getTicksRequiredToFreeze() * 0.9F))));
    }

    private void chorusTeleport(LivingEntity target) {
        double oldX = target.getX();
        double oldY = target.getY();
        double oldZ = target.getZ();
        for (int attempt = 0; attempt < 16; attempt++) {
            double x = oldX + (random.nextDouble() - 0.5) * 16.0;
            double y = Math.max(level().getMinBuildHeight(),
                    Math.min(level().getMaxBuildHeight() - 1, oldY + random.nextInt(16) - 8));
            double z = oldZ + (random.nextDouble() - 0.5) * 16.0;
            if (target.randomTeleport(x, y, z, true)) {
                level().playSound(null, oldX, oldY, oldZ, SoundEvents.CHORUS_FRUIT_TELEPORT,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                target.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                return;
            }
        }
    }

    private void explodeGunpowder() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        splatEffects();
        serverLevel.explode(getOwner(), getX(), getY(), getZ(), 1.5F, Level.ExplosionInteraction.NONE);
        discard();
    }

    private void updateRedstoneSignal(BlockPos previousPos) {
        if (!RestartRequiredConfig.cakeFillings() || filling != CakeFilling.REDSTONE || level().isClientSide()) return;
        level().updateNeighborsAt(previousPos, Blocks.AIR);
        BlockPos currentPos = blockPosition();
        if (!currentPos.equals(previousPos)) level().updateNeighborsAt(currentPos, Blocks.AIR);
    }

    public void setFilling(CakeFilling filling) {
        this.filling = filling == null ? CakeFilling.NONE : filling;
    }

    public CakeFilling getFilling() {
        return filling;
    }

    private Creeper disarm(LeapingCreeperEntity sporeCreeper) {
        if (!(level() instanceof ServerLevel serverLevel)) return sporeCreeper;
        Creeper ordinary = EntityType.CREEPER.create(serverLevel);
        if (ordinary == null) return sporeCreeper;
        CreeperTransformation.copyState(sporeCreeper, ordinary);
        ordinary.getEntityData().set(CreeperAccessor.coolstuff$poweredData(), sporeCreeper.isPowered());
        sporeCreeper.discard();
        serverLevel.addFreshEntity(ordinary);
        return ordinary;
    }

    private void setCakeState(int state) {
        entityData.set(STATE, state);
        entityData.set(STATE_START, tickCount);
    }

    private void setImpact(Vec3 position, Direction face) {
        entityData.set(IMPACT_POSITION, new Vector3f((float) position.x, (float) position.y, (float) position.z));
        entityData.set(SURFACE_FACE, face.get3DDataValue());
    }

    public int getCakeState() {
        return entityData.get(STATE);
    }

    public float getStateAge(float partialTick) {
        return tickCount + partialTick - entityData.get(STATE_START);
    }

    public Direction getSurfaceFace() {
        return Direction.from3DDataValue(entityData.get(SURFACE_FACE));
    }

    public Vec3 getImpactPosition() {
        Vector3f position = entityData.get(IMPACT_POSITION);
        return new Vec3(position.x, position.y, position.z);
    }

    public float getSmearProgress(float partialTick) {
        int start = entityData.get(WALL_START);
        if (start < 0) return 0.0F;
        int stopAge = entityData.get(WALL_STOP_AGE);
        float age = stopAge >= 0 ? stopAge : tickCount + partialTick - start;
        return Math.min(1.0F, age / 108.0F);
    }

    public float getWallAge(float partialTick) {
        int start = entityData.get(WALL_START);
        return start < 0 ? -1.0F : tickCount + partialTick - start;
    }

    public Vec3 getWallPosition() {
        Vector3f position = entityData.get(WALL_POSITION);
        return new Vec3(position.x, position.y, position.z);
    }

    public Direction getWallFace() {
        return entityData.get(WALL_FACE);
    }

    public Entity getAttachedEntity() {
        int id = entityData.get(ATTACHED_ID);
        return id < 0 ? null : level().getEntity(id);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CakeState", getCakeState());
        tag.putString("CakeFilling", filling.id());
        tag.putInt("StateStart", entityData.get(STATE_START));
        tag.putInt("SurfaceFace", entityData.get(SURFACE_FACE));
        Vector3f impact = entityData.get(IMPACT_POSITION);
        tag.putFloat("ImpactX", impact.x);
        tag.putFloat("ImpactY", impact.y);
        tag.putFloat("ImpactZ", impact.z);
        tag.putInt("WallStart", entityData.get(WALL_START));
        tag.putInt("WallStopAge", entityData.get(WALL_STOP_AGE));
        Vector3f wall = entityData.get(WALL_POSITION);
        tag.putFloat("WallX", wall.x);
        tag.putFloat("WallY", wall.y);
        tag.putFloat("WallZ", wall.z);
        tag.putInt("WallFace", entityData.get(WALL_FACE).get3DDataValue());
        if (attachedUuid != null) tag.putUUID("AttachedUuid", attachedUuid);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(STATE, tag.getInt("CakeState"));
        filling = CakeFilling.fromId(tag.getString("CakeFilling"));
        entityData.set(STATE_START, tag.getInt("StateStart"));
        entityData.set(SURFACE_FACE, tag.getInt("SurfaceFace"));
        entityData.set(IMPACT_POSITION, new Vector3f(
                tag.getFloat("ImpactX"), tag.getFloat("ImpactY"), tag.getFloat("ImpactZ")));
        entityData.set(WALL_START, tag.getInt("WallStart"));
        entityData.set(WALL_STOP_AGE, tag.contains("WallStopAge") ? tag.getInt("WallStopAge") : -1);
        entityData.set(WALL_POSITION, new Vector3f(
                tag.getFloat("WallX"), tag.getFloat("WallY"), tag.getFloat("WallZ")));
        entityData.set(WALL_FACE, Direction.from3DDataValue(tag.getInt("WallFace")));
        if (tag.hasUUID("AttachedUuid")) attachedUuid = tag.getUUID("AttachedUuid");
        if (attachedUuid != null && level() instanceof ServerLevel serverLevel) {
            Entity attached = serverLevel.getEntity(attachedUuid);
            if (attached != null) entityData.set(ATTACHED_ID, attached.getId());
        }
        noPhysics = getCakeState() != FLYING && getCakeState() != DROPPED;
    }
}
