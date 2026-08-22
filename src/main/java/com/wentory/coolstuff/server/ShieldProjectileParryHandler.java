package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.network.ParryEffectPayload;
import com.wentory.coolstuff.network.SnowballHitPayload;
import com.wentory.coolstuff.entity.FrostlingEntity;
import com.wentory.coolstuff.snow.SnowHeat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class ShieldProjectileParryHandler {
    private static final String PARRIED_SNOWBALL = "coolstuff_parried_snowball";
    private static final int PARRY_WINDOW_TICKS = 5;
    private static final String SNOW_HIT_DURATIONS = "coolstuff_snow_hit_durations";
    private static final int SNOW_HIT_DURATION = 600;
    private static final int FREEZING_HIT_COUNT = 5;
    private static final int MAX_SNOW_SPLATS = 12;

    private ShieldProjectileParryHandler() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().level() instanceof ServerLevel level)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)) return;

        Projectile projectile = event.getProjectile();
        Entity hitEntity = hit.getEntity();
        if (CoolstuffConfig.ENABLE_PARRY.get()
                && hitEntity instanceof Player player
                && isParryable(projectile) && isTimedShieldParry(player)
                && isInFrontOfShield(projectile, player)) {
            event.setCanceled(true);
            reflect(projectile, player);
            return;
        }

        if (!(projectile instanceof Snowball snowball) || !(hitEntity instanceof LivingEntity living)) return;
        boolean parried = snowball.getPersistentData().getBoolean(PARRIED_SNOWBALL);
        if (living instanceof FrostlingEntity && !parried) {
            event.setCanceled(true);
            snowball.discard();
            return;
        }
        if (parried) {
            living.hurt(level.damageSources().thrown(snowball, snowball.getOwner()), 1.0F);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        if (living instanceof ServerPlayer serverPlayer) {
            registerSnowHit(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer, new SnowballHitPayload(level.random.nextInt(Integer.MAX_VALUE)));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int meltSpeed = SnowHeat.meltMultiplier(player.serverLevel(), player.blockPosition());
        int[] current = player.getPersistentData().getIntArray(SNOW_HIT_DURATIONS);
        int[] active = java.util.Arrays.stream(current)
                .map(duration -> duration - meltSpeed).filter(duration -> duration > 0).toArray();
        if (active.length != current.length) {
            player.getPersistentData().putIntArray(SNOW_HIT_DURATIONS, active);
        } else if (active.length > 0) {
            player.getPersistentData().putIntArray(SNOW_HIT_DURATIONS, active);
        }
        if (active.length >= FREEZING_HIT_COUNT) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0,
                    false, false, true));
            player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze(), player.getTicksFrozen() + 5));
        }
    }

    private static void registerSnowHit(ServerPlayer player) {
        int[] old = java.util.Arrays.stream(player.getPersistentData().getIntArray(SNOW_HIT_DURATIONS))
                .filter(duration -> duration > 0).toArray();
        int kept = Math.min(old.length, MAX_SNOW_SPLATS - 1);
        int[] updated = new int[kept + 1];
        if (kept > 0) System.arraycopy(old, old.length - kept, updated, 0, kept);
        updated[kept] = SNOW_HIT_DURATION;
        player.getPersistentData().putIntArray(SNOW_HIT_DURATIONS, updated);
    }

    private static boolean isParryable(Projectile projectile) {
        if (projectile instanceof Snowball || projectile instanceof ThrownEgg) return true;
        // ThrownTrident also extends AbstractArrow, so explicitly reject it.
        return projectile instanceof AbstractArrow
                && !(projectile instanceof net.minecraft.world.entity.projectile.ThrownTrident);
    }

    private static boolean isTimedShieldParry(Player player) {
        return player.isUsingItem() && player.getUseItem().is(Items.SHIELD)
                && player.getTicksUsingItem() <= PARRY_WINDOW_TICKS;
    }

    private static boolean isInFrontOfShield(Projectile projectile, Player player) {
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        Vec3 incomingFrom = projectile.getDeltaMovement().scale(-1.0).multiply(1.0, 0.0, 1.0);
        if (look.lengthSqr() < 1.0E-6) return false;
        if (incomingFrom.lengthSqr() < 1.0E-6) {
            incomingFrom = projectile.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        }
        return incomingFrom.lengthSqr() >= 1.0E-6
                && look.normalize().dot(incomingFrom.normalize()) > 0.0;
    }

    private static void reflect(Projectile projectile, Player player) {
        Vec3 direction = player.getLookAngle().normalize();
        double speed = Math.max(0.9, projectile.getDeltaMovement().length() * 1.08);
        projectile.setOwner(player);
        projectile.setPos(player.getEyePosition().add(direction.scale(0.9)));
        projectile.setDeltaMovement(direction.scale(speed));
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        if (projectile instanceof Snowball) projectile.getPersistentData().putBoolean(PARRIED_SNOWBALL, true);
        PacketDistributor.sendToPlayersTrackingEntity(projectile,
                new ParryEffectPayload(projectile.getX(), projectile.getY(), projectile.getZ(), 1));
    }
}
