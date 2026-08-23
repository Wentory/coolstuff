package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.network.ParryEffectPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class ShieldSkeletonHandler {
    private static final String SHIELD_AI = "coolstuff_shield_skeleton";
    private static final String REACTION = "coolstuff_shield_reaction";
    private static final String BLOCKING = "coolstuff_shield_blocking";
    private static final String RELEASE_DELAY = "coolstuff_shield_release_delay";
    private static final String ALERT_TICKS = "coolstuff_shield_alert_ticks";
    private static final String CROSSBOW_AIM_TICKS = "coolstuff_shield_crossbow_aim_ticks";
    private static final String CROSSBOW_REACTION_TICKS = "coolstuff_shield_crossbow_reaction_ticks";
    private static final String PIERCING_AWARE_TICKS = "coolstuff_shield_piercing_aware_ticks";
    private static final String PIERCING_OWNER = "coolstuff_shield_piercing_owner";
    private static final String COOLDOWN = "coolstuff_shield_cooldown";
    private static final int THINK_TICKS = 6;
    private static final String PARRY_CHECKED = "coolstuff_shield_skeleton_parry_checked";

    private ShieldSkeletonHandler() {
    }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL
                || event.getEntity().getClass() != Skeleton.class
                || event.getLevel().getRandom().nextDouble()
                >= CoolstuffConfig.SHIELD_SKELETON_SPAWN_CHANCE.get()) return;

        Skeleton skeleton = (Skeleton) event.getEntity();
        makeShieldSkeleton(skeleton);
        DebugMode.markAndAnnounce(skeleton, "Shield Skeleton");
    }

    public static void makeShieldSkeleton(Skeleton skeleton) {
        skeleton.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        skeleton.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        skeleton.getPersistentData().putBoolean(SHIELD_AI, true);
    }

    @SubscribeEvent
    public static void onSkeletonTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Skeleton skeleton)
                || skeleton.level().isClientSide()
                || !skeleton.getPersistentData().getBoolean(SHIELD_AI)) return;
        if (!skeleton.getOffhandItem().is(Items.SHIELD)) return;

        int cooldown = skeleton.getPersistentData().getInt(COOLDOWN);
        if (cooldown > 0) skeleton.getPersistentData().putInt(COOLDOWN, cooldown - 1);
        int alertTicks = skeleton.getPersistentData().getInt(ALERT_TICKS);
        if (alertTicks > 0) {
            alertTicks--;
            skeleton.getPersistentData().putInt(ALERT_TICKS, alertTicks);
        }
        boolean onGuard = alertTicks > 0;
        int piercingAwareTicks = skeleton.getPersistentData().getInt(PIERCING_AWARE_TICKS);
        if (piercingAwareTicks > 0) {
            Entity target = skeleton.getTarget();
            if (!(target instanceof Player player)
                    || !skeleton.getPersistentData().hasUUID(PIERCING_OWNER)
                    || !player.getUUID().equals(skeleton.getPersistentData().getUUID(PIERCING_OWNER))) {
                forgetPiercing(skeleton);
            } else {
                skeleton.getPersistentData().putInt(PIERCING_AWARE_TICKS, piercingAwareTicks - 1);
            }
        }

        Entity incoming = findIncomingProjectile(skeleton);
        Player aimingPlayer = findAimingPlayer(skeleton, onGuard);
        boolean chargedCrossbowAimed = aimingPlayer != null && hasChargedCrossbow(aimingPlayer);
        Player closePlayer = findClosePlayer(skeleton);
        Entity threat = incoming != null ? incoming : aimingPlayer != null ? aimingPlayer : closePlayer;
        int blockingTicks = skeleton.getPersistentData().getInt(BLOCKING);

        if (blockingTicks > 0) {
            blockingTicks++;
            int releaseDelay = threat == null
                    ? skeleton.getPersistentData().getInt(RELEASE_DELAY) + 1 : 0;
            skeleton.getPersistentData().putInt(RELEASE_DELAY, releaseDelay);
            faceThreat(skeleton, threat);
            if (!skeleton.isUsingItem() || !skeleton.getUseItem().is(Items.SHIELD)) {
                skeleton.stopUsingItem();
                skeleton.startUsingItem(InteractionHand.OFF_HAND);
            }
            skeleton.getNavigation().stop();

            if (incoming != null && skeleton.distanceToSqr(incoming) <= 6.25 && tryParry(skeleton, incoming)) {
                skeleton.getPersistentData().putInt(BLOCKING, 0);
                skeleton.getPersistentData().putInt(REACTION, 0);
                skeleton.getPersistentData().putInt(RELEASE_DELAY, 0);
                skeleton.getPersistentData().putInt(COOLDOWN, 12 + skeleton.getRandom().nextInt(9));
                skeleton.getPersistentData().putInt(ALERT_TICKS, 60);
                skeleton.stopUsingItem();
                return;
            }

            // Never voluntarily expose itself while its current target is still
            // aiming or standing in melee range. Once the threat is gone, keep
            // the shield up briefly so bow-release timing cannot cheese it.
            if (releaseDelay >= 14) {
                skeleton.stopUsingItem();
                skeleton.getPersistentData().putInt(BLOCKING, 0);
                skeleton.getPersistentData().putInt(REACTION, 0);
                skeleton.getPersistentData().putInt(RELEASE_DELAY, 0);
                skeleton.getPersistentData().putInt(COOLDOWN, 8 + skeleton.getRandom().nextInt(9));
                skeleton.getPersistentData().putInt(ALERT_TICKS, 60);
            } else {
                skeleton.getPersistentData().putInt(BLOCKING, blockingTicks);
            }
            return;
        }

        if (cooldown > 0 && !onGuard) return;
        int reaction = skeleton.getPersistentData().getInt(REACTION);
        // An arrow that is already on course is no longer something to "think" about.
        if (incoming != null) {
            resetCrossbowReaction(skeleton);
            reaction = THINK_TICKS;
        } else if (closePlayer != null) {
            resetCrossbowReaction(skeleton);
            reaction += 2;
        }
        else if (chargedCrossbowAimed) {
            int aimedTicks = skeleton.getPersistentData().getInt(CROSSBOW_AIM_TICKS) + 1;
            int requiredTicks = skeleton.getPersistentData().getInt(CROSSBOW_REACTION_TICKS);
            if (requiredTicks <= 0) {
                requiredTicks = onGuard ? 10 : 30 + skeleton.getRandom().nextInt(21);
                skeleton.getPersistentData().putInt(CROSSBOW_REACTION_TICKS, requiredTicks);
            }
            skeleton.getPersistentData().putInt(CROSSBOW_AIM_TICKS, aimedTicks);
            reaction = aimedTicks >= requiredTicks ? THINK_TICKS : 0;
        } else if (aimingPlayer != null) {
            resetCrossbowReaction(skeleton);
            reaction = onGuard ? THINK_TICKS : reaction + 1;
        } else {
            resetCrossbowReaction(skeleton);
            reaction = Math.max(0, reaction - 2);
        }

        if (reaction >= THINK_TICKS) {
            faceThreat(skeleton, threat);
            skeleton.getNavigation().stop();
            skeleton.stopUsingItem();
            skeleton.startUsingItem(InteractionHand.OFF_HAND);
            skeleton.getPersistentData().putInt(BLOCKING, 1);
            skeleton.getPersistentData().putInt(REACTION, 0);
            skeleton.getPersistentData().putInt(RELEASE_DELAY, 0);
            skeleton.getPersistentData().putInt(ALERT_TICKS, 0);
            resetCrossbowReaction(skeleton);
        } else {
            skeleton.getPersistentData().putInt(REACTION, reaction);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Skeleton skeleton)
                || !skeleton.getPersistentData().getBoolean(SHIELD_AI)) return;

        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow
                && arrow.getPierceLevel() > 0
                && arrow.getOwner() instanceof Player player
                && player == skeleton.getTarget()) {
            skeleton.getPersistentData().putInt(PIERCING_AWARE_TICKS, 200);
            skeleton.getPersistentData().putUUID(PIERCING_OWNER, player.getUUID());
            if (skeleton.distanceToSqr(player) > 3.25 * 3.25) {
                skeleton.stopUsingItem();
                skeleton.getPersistentData().putInt(BLOCKING, 0);
                skeleton.getPersistentData().putInt(REACTION, 0);
            }
        }

        if (!skeleton.isUsingItem() || !skeleton.getUseItem().is(Items.SHIELD)) return;

        if (event.getSource().getEntity() instanceof Player player
                && player.getMainHandItem().getItem() instanceof AxeItem) {
            skeleton.stopUsingItem();
            skeleton.getPersistentData().putInt(BLOCKING, 0);
            skeleton.getPersistentData().putInt(REACTION, 0);
            skeleton.getPersistentData().putInt(RELEASE_DELAY, 0);
            // Match vanilla's five-second shield disable after an axe hit.
            skeleton.getPersistentData().putInt(COOLDOWN, 100);
            skeleton.playSound(SoundEvents.SHIELD_BREAK, 1.0F, 0.9F);
            return;
        }

        // The bow goal may try to swap the active hand before our post-tick. The
        // shield state itself is authoritative and preserves the player's 5-tick raise time.
        if (skeleton.getPersistentData().getInt(BLOCKING) < 5
                || event.getSource().is(DamageTypeTags.BYPASSES_SHIELD)
                || event.getSource().getDirectEntity() instanceof AbstractArrow arrow
                && arrow.getPierceLevel() > 0
                || !isAttackInFront(skeleton, event.getSource().getSourcePosition())) return;

        event.setCanceled(true);
        skeleton.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.9F + skeleton.getRandom().nextFloat() * 0.2F);
        ItemStack shield = skeleton.getOffhandItem();
        int durabilityDamage = Math.max(1, 1 + (int) Math.floor(event.getAmount()));
        shield.hurtAndBreak(durabilityDamage, skeleton, EquipmentSlot.OFFHAND);

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof net.minecraft.world.entity.LivingEntity livingAttacker) {
            Vec3 away = livingAttacker.position().subtract(skeleton.position()).normalize().scale(0.35);
            livingAttacker.push(away.x, 0.1, away.z);
        }
    }

    private static Player findAimingPlayer(Skeleton skeleton, boolean onGuard) {
        if (!(skeleton.getTarget() instanceof Player player)
                || !player.isAlive() || player.isSpectator()
                || skeleton.distanceToSqr(player) > 24.0 * 24.0
                || !isPreparingRangedAttack(player, onGuard)
                || !skeleton.getSensing().hasLineOfSight(player)
                || !isAimedAt(player, skeleton)) return null;
        if (knowsPiercingCrossbow(skeleton, player)
                && hasChargedCrossbow(player)
                && skeleton.distanceToSqr(player) > 3.25 * 3.25) return null;
        return player;
    }

    private static Player findClosePlayer(Skeleton skeleton) {
        if (!(skeleton.getTarget() instanceof Player player)
                || !player.isAlive() || player.isSpectator()
                || skeleton.distanceToSqr(player) > 3.25 * 3.25
                || !skeleton.getSensing().hasLineOfSight(player)) return null;
        return player;
    }

    private static boolean isPreparingRangedAttack(Player player, boolean onGuard) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.is(Items.CROSSBOW) && CrossbowItem.isCharged(main)
                || off.is(Items.CROSSBOW) && CrossbowItem.isCharged(off)) return true;
        if (!player.isUsingItem() || player.getTicksUsingItem() < (onGuard ? 1 : 5)) return false;
        ItemStack used = player.getUseItem();
        return used.is(Items.BOW) || used.is(Items.CROSSBOW) || used.is(Items.TRIDENT);
    }

    private static boolean hasChargedCrossbow(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(Items.CROSSBOW) && CrossbowItem.isCharged(main)
                || off.is(Items.CROSSBOW) && CrossbowItem.isCharged(off);
    }

    private static void resetCrossbowReaction(Skeleton skeleton) {
        skeleton.getPersistentData().putInt(CROSSBOW_AIM_TICKS, 0);
        skeleton.getPersistentData().putInt(CROSSBOW_REACTION_TICKS, 0);
    }

    private static boolean knowsPiercingCrossbow(Skeleton skeleton, Player player) {
        return skeleton.getPersistentData().getInt(PIERCING_AWARE_TICKS) > 0
                && skeleton.getPersistentData().hasUUID(PIERCING_OWNER)
                && player.getUUID().equals(skeleton.getPersistentData().getUUID(PIERCING_OWNER));
    }

    private static void forgetPiercing(Skeleton skeleton) {
        skeleton.getPersistentData().putInt(PIERCING_AWARE_TICKS, 0);
        skeleton.getPersistentData().remove(PIERCING_OWNER);
    }

    private static boolean isAimedAt(Player player, Skeleton skeleton) {
        Vec3 toSkeleton = skeleton.getEyePosition().subtract(player.getEyePosition());
        double distance = toSkeleton.length();
        if (distance < 0.01) return true;
        double requiredAccuracy = Math.max(0.94, 0.985 - skeleton.getBbWidth() / distance);
        return player.getLookAngle().dot(toSkeleton.scale(1.0 / distance)) >= requiredAccuracy;
    }

    private static Projectile findIncomingProjectile(Skeleton skeleton) {
        return skeleton.level().getEntitiesOfClass(Projectile.class,
                        new AABB(skeleton.blockPosition()).inflate(16.0), projectile -> {
                            if (!projectile.isAlive() || projectile.getOwner() == skeleton
                                    || projectile.getOwner() != skeleton.getTarget()
                                    || projectile.getDeltaMovement().lengthSqr() < 0.01) return false;
                            if (projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0
                                    && projectile.getOwner() instanceof Player player
                                    && knowsPiercingCrossbow(skeleton, player)
                                    && skeleton.distanceToSqr(player) > 3.25 * 3.25) return false;
                            Vec3 towardSkeleton = skeleton.getEyePosition().subtract(projectile.position()).normalize();
                            return projectile.getDeltaMovement().normalize().dot(towardSkeleton) > 0.88;
                        }).stream().min(Comparator.comparingDouble(skeleton::distanceToSqr)).orElse(null);
    }

    private static boolean tryParry(Skeleton skeleton, Entity incomingEntity) {
        if (!(incomingEntity instanceof Projectile projectile)
                || projectile.getPersistentData().getBoolean(PARRY_CHECKED)) return false;
        // Piercing crossbow bolts are the intended hard counter to the shield.
        if (projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) return false;
        projectile.getPersistentData().putBoolean(PARRY_CHECKED, true);
        if (skeleton.getRandom().nextDouble() >= CoolstuffConfig.SHIELD_SKELETON_PARRY_CHANCE.get()) return false;

        Entity originalOwner = projectile.getOwner();
        Vec3 destination = originalOwner != null && originalOwner.isAlive()
                ? originalOwner.getEyePosition()
                : skeleton.getTarget() != null ? skeleton.getTarget().getEyePosition()
                : skeleton.getEyePosition().add(skeleton.getLookAngle().scale(8.0));
        Vec3 direction = destination.subtract(skeleton.getEyePosition()).normalize();
        double speed = Math.max(0.9, projectile.getDeltaMovement().length() * 1.08);

        projectile.setOwner(skeleton);
        projectile.setPos(skeleton.getEyePosition().add(direction.scale(0.9)));
        projectile.setDeltaMovement(direction.scale(speed));
        if (projectile instanceof AbstractHurtingProjectile hurtingProjectile) {
            hurtingProjectile.accelerationPower = 0.1;
        }
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;

        if (projectile instanceof LargeFireball fireball && CoolstuffConfig.ENABLE_PARRY.get()) {
            FireballCombo.parry(fireball);
        } else {
            PacketDistributor.sendToPlayersTrackingEntity(projectile,
                    new ParryEffectPayload(projectile.getX(), projectile.getY(), projectile.getZ(), 1));
        }
        return true;
    }

    private static void faceThreat(Skeleton skeleton, Entity threat) {
        if (threat == null) return;
        skeleton.getLookControl().setLookAt(threat, 60.0F, 60.0F);
        Vec3 direction = threat.position().subtract(skeleton.position());
        if (direction.lengthSqr() > 0.001) {
            skeleton.setYRot((float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90.0F);
            skeleton.yBodyRot = skeleton.getYRot();
            skeleton.yHeadRot = skeleton.getYRot();
        }
    }

    private static boolean isAttackInFront(Skeleton skeleton, Vec3 sourcePosition) {
        if (sourcePosition == null) return false;
        Vec3 towardSource = sourcePosition.subtract(skeleton.position());
        towardSource = new Vec3(towardSource.x, 0.0, towardSource.z);
        if (towardSource.lengthSqr() < 1.0E-4) return true;
        Vec3 facing = skeleton.getViewVector(1.0F);
        facing = new Vec3(facing.x, 0.0, facing.z);
        return facing.normalize().dot(towardSource.normalize()) > 0.0;
    }

}
