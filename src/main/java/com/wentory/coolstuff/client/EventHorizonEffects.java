package com.wentory.coolstuff.client;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.BlackHoleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = Coolstuff.MODID, value = Dist.CLIENT)
public final class EventHorizonEffects {
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(
            Coolstuff.MODID, "shaders/post/event_horizon.json");
    private static final double EFFECT_START_DISTANCE = 56.0;
    private static final double EFFECT_FULL_DISTANCE = 4.0;
    private static boolean active;
    private static float smoothedStrength;
    private static float smoothedDanger;
    private static int impactTicks;
    private static float impactBase;
    private static int spawnTicks;
    private static float spawnBase;
    private static final Set<Integer> KNOWN_HOLES = new HashSet<>();

    private EventHorizonEffects() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            KNOWN_HOLES.clear();
            stop(minecraft);
            return;
        }

        BlackHoleEntity nearestHole = minecraft.level.getEntitiesOfClass(BlackHoleEntity.class,
                        minecraft.player.getBoundingBox().inflate(84.0))
                .stream().filter(hole -> !hole.isPacified())
                .min(Comparator.comparingDouble(hole -> hole.distanceToSqr(minecraft.player))).orElse(null);
        Set<Integer> visibleHoles = new HashSet<>();
        for (BlackHoleEntity hole : minecraft.level.getEntitiesOfClass(BlackHoleEntity.class,
                minecraft.player.getBoundingBox().inflate(160.0))) {
            if (hole.isPacified()) continue;
            visibleHoles.add(hole.getId());
            if (!KNOWN_HOLES.contains(hole.getId()) && hole.getRemainingTicks() >= 170) {
                triggerSpawn(hole.position());
            }
        }
        KNOWN_HOLES.retainAll(visibleHoles);
        KNOWN_HOLES.addAll(visibleHoles);
        double nearest = nearestHole == null ? Double.POSITIVE_INFINITY : nearestHole.distanceTo(minecraft.player);
        float horizonTarget = (float) Math.max(0.0, Math.min(1.0,
                (EFFECT_START_DISTANCE - nearest) / (EFFECT_START_DISTANCE - EFFECT_FULL_DISTANCE)));
        float dangerTarget = (float) Math.max(0.0, Math.min(1.0, (34.0 - nearest) / 4.0));
        smoothedDanger += (dangerTarget - smoothedDanger) * (dangerTarget > smoothedDanger ? 0.55F : 0.35F);
        float lensStrength = nearestHole == null ? 0.0F : (float) Math.max(0.0, 1.0 - nearest / 80.0);
        float[] lensCenter = nearestHole == null ? new float[]{0.5F, 0.5F, 0.0F}
                : projectToScreen(minecraft, nearestHole.position());
        lensStrength *= lensCenter[2];
        float impact = impactTicks > 0 ? impactBase * impactTicks / 50.0F : 0.0F;
        if (impactTicks > 0) impactTicks--;
        float spawnProgress = spawnTicks > 0 ? spawnTicks / 24.0F : 0.0F;
        float spawnImpact = spawnBase * spawnProgress * spawnProgress;
        if (spawnTicks > 0) spawnTicks--;
        float target = Math.max(horizonTarget, Math.max(impact, spawnImpact));
        smoothedStrength += (target - smoothedStrength) * (target > smoothedStrength ? 0.24F : 0.12F);

        if (Math.max(smoothedStrength, lensStrength) > 0.01F) {
            // Camera and game-mode changes can dispose Minecraft's current post chain
            // without notifying us. Treat a missing chain as inactive so it is restored
            // on this tick while preserving all effect timers and their current strength.
            if (active && minecraft.gameRenderer.currentEffect() == null) {
                active = false;
            }
            if (!active) {
                minecraft.gameRenderer.loadEffect(EFFECT);
                active = minecraft.gameRenderer.currentEffect() != null;
            }
            if (active && minecraft.gameRenderer.currentEffect() != null) {
                minecraft.gameRenderer.currentEffect().setUniform("Strength", smoothedStrength);
                minecraft.gameRenderer.currentEffect().setUniform("Impact", impact);
                minecraft.gameRenderer.currentEffect().setUniform("SpawnImpact", spawnImpact);
                minecraft.gameRenderer.currentEffect().setUniform("Danger", smoothedDanger);
                minecraft.gameRenderer.currentEffect().setUniform("LensX", lensCenter[0]);
                minecraft.gameRenderer.currentEffect().setUniform("LensY", lensCenter[1]);
                minecraft.gameRenderer.currentEffect().setUniform("LensStrength", lensStrength);
            }
        } else {
            stop(minecraft);
        }
    }

    private static float[] projectToScreen(Minecraft minecraft, Vec3 worldPosition) {
        Vec3 relative = worldPosition.subtract(minecraft.gameRenderer.getMainCamera().getPosition());
        Quaternionf inverseCamera = minecraft.gameRenderer.getMainCamera().rotation().conjugate(new Quaternionf());
        Vector3f cameraSpace = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z)
                .rotate(inverseCamera);
        float depth = -cameraSpace.z;
        if (depth <= 0.05F) return new float[]{0.5F, 0.5F, 0.0F};

        float width = minecraft.getWindow().getGuiScaledWidth();
        float height = minecraft.getWindow().getGuiScaledHeight();
        double fov = Math.toRadians(minecraft.options.fov().get());
        float focalLength = (float) (height * 0.5 / Math.tan(fov * 0.5));
        float x = width * 0.5F + cameraSpace.x * focalLength / depth;
        float y = height * 0.5F - cameraSpace.y * focalLength / depth;
        float margin = 0.18F;
        boolean visible = x >= -width * margin && x <= width * (1.0F + margin)
                && y >= -height * margin && y <= height * (1.0F + margin);
        return new float[]{x / width, 1.0F - y / height, visible ? 1.0F : 0.0F};
    }

    public static void triggerExplosion(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        double distance = minecraft.player.position().distanceTo(new Vec3(x, y, z));
        impactBase = (float) Math.max(0.15, 1.0 - distance / 150.0);
        impactTicks = 50;
    }

    private static void triggerSpawn(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        double distance = minecraft.player.position().distanceTo(position);
        spawnBase = (float) Math.max(0.25, 1.35 - distance / 120.0);
        spawnTicks = 24;
    }

    private static void stop(Minecraft minecraft) {
        smoothedStrength = 0.0F;
        smoothedDanger = 0.0F;
        impactTicks = 0;
        spawnTicks = 0;
        if (active) minecraft.gameRenderer.shutdownEffect();
        active = false;
    }
}
