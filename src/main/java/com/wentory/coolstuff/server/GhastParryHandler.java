package com.wentory.coolstuff.server;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.LeapingCreeperEntity;
import com.wentory.coolstuff.network.ParryEffectPayload;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class GhastParryHandler {
    private GhastParryHandler() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!CoolstuffConfig.ENABLE_GHAST_PARRY.get() || !CoolstuffConfig.ENABLE_PARRY.get()) return;
        if (!(event.getProjectile() instanceof LargeFireball fireball)
                || BlackHole.isActive(fireball)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Ghast ghast)
                || !(fireball.getOwner() instanceof Player)
                || !(fireball.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target = ghast.getTarget();
        if (!(target instanceof Player) || !target.isAlive()
                || level.random.nextDouble() >= CoolstuffConfig.GHAST_PARRY_CHANCE.get()) {
            return;
        }

        Vec3 origin = hit.getLocation();
        Vec3 direction = target.getEyePosition().subtract(origin).normalize();
        double speed = Math.max(0.6, fireball.getDeltaMovement().length());

        event.setCanceled(true);
        fireball.setOwner(ghast);
        fireball.setDeltaMovement(direction.scale(speed));
        fireball.accelerationPower = 0.1;
        fireball.setPos(origin.add(direction.scale(0.7)));
        fireball.hasImpulse = true;

        FireballCombo.parry(fireball);
    }

    @SubscribeEvent
    public static void onNaturalGhastSpawn(FinalizeSpawnEvent event) {
        if (RestartRequiredConfig.ultraGhast()
                && event.getEntity() instanceof Ghast ghast
                && event.getSpawnType() == MobSpawnType.NATURAL
                && event.getLevel().getRandom().nextDouble() < UltraGhast.getNaturalSpawnChance()) {
            UltraGhast.setUltra(ghast, true);
            DebugMode.markAndAnnounce(ghast, "UltraGhast");
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("coolstuff")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("ghastParryChance")
                                .executes(context -> {
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Ghast parry chance: " + formatPercent()), false);
                                            return (int) Math.round(CoolstuffConfig.GHAST_PARRY_CHANCE.get() * 100.0);
                                })
                                .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0, 100.0))
                                        .executes(context -> {
                                            double percent = DoubleArgumentType.getDouble(context, "percent");
                                            CoolstuffConfig.GHAST_PARRY_CHANCE.set(percent / 100.0);
                                            if (CoolstuffConfig.SPEC.isLoaded()) CoolstuffConfig.SPEC.save();
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Ghast parry chance set to " + formatPercent()), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("ultraGhast")
                                .executes(context -> setNearestGhastUltra(context.getSource(), true))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setNearestGhastUltra(
                                                context.getSource(), BoolArgumentType.getBool(context, "enabled")))))
                        .then(Commands.literal("ultraGhastChance")
                                .executes(context -> {
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Natural UltraGhast chance: "
                                                    + formatPercent(UltraGhast.getNaturalSpawnChance())), false);
                                    return (int) Math.round(UltraGhast.getNaturalSpawnChance() * 100.0);
                                })
                                .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0, 100.0))
                                        .executes(context -> {
                                            double percent = DoubleArgumentType.getDouble(context, "percent");
                                            UltraGhast.setNaturalSpawnChance(percent / 100.0);
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Natural UltraGhast chance set to "
                                                            + formatPercent(UltraGhast.getNaturalSpawnChance())), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("blackHole")
                                .then(Commands.literal("spawn")
                                        .executes(context -> BlackHole.spawnEternal(context.getSource())))
                                .then(Commands.literal("remove")
                                        .executes(context -> BlackHole.removeNearestEternal(context.getSource()))))
                        .then(Commands.literal("debug")
                                .executes(context -> DebugMode.status(context.getSource()))
                                .then(Commands.literal("sporeFartChance")
                                        .executes(context -> {
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    "Spore Creeper fart chance: " + formatPercent(
                                                            LeapingCreeperEntity.getSporeFartChance())), false);
                                            return (int) Math.round(LeapingCreeperEntity.getSporeFartChance() * 100.0);
                                        })
                                        .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0, 100.0))
                                                .executes(context -> {
                                                    double percent = DoubleArgumentType.getDouble(context, "percent");
                                                    LeapingCreeperEntity.setSporeFartChance(percent / 100.0);
                                                    context.getSource().sendSuccess(() -> Component.literal(
                                                            "Spore Creeper fart chance set to " + formatPercent(
                                                                    LeapingCreeperEntity.getSporeFartChance())), true);
                                                    return 1;
                                                })))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> DebugMode.set(context.getSource(),
                                                BoolArgumentType.getBool(context, "enabled")))))
        );
    }

    private static int setNearestGhastUltra(net.minecraft.commands.CommandSourceStack source, boolean enabled) {
        Vec3 position = source.getPosition();
        Ghast nearest = source.getLevel().getEntitiesOfClass(
                        Ghast.class, new AABB(position, position).inflate(32.0), Ghast::isAlive
                ).stream()
                .min(java.util.Comparator.comparingDouble(ghast -> ghast.distanceToSqr(position)))
                .orElse(null);

        if (nearest == null) {
            source.sendFailure(Component.literal("No ghast found within 32 blocks"));
            return 0;
        }

        UltraGhast.setUltra(nearest, enabled);
        source.sendSuccess(() -> Component.literal(enabled
                ? "Nearest ghast is now an UltraGhast"
                : "Nearest ghast is no longer an UltraGhast"), true);
        return 1;
    }

    private static String formatPercent() {
        return formatPercent(CoolstuffConfig.GHAST_PARRY_CHANCE.get());
    }

    private static String formatPercent(double chance) {
        double percent = chance * 100.0;
        return percent == Math.rint(percent)
                ? String.format("%.0f%%", percent)
                : String.format("%.2f%%", percent);
    }
}
