package com.wentory.coolstuff.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.server.BoatEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Coolstuff.MODID, value = Dist.CLIENT)
public final class IcewakeRenderer {
    private static final double RADIUS = 2.2;
    private static final double FADE_TICKS = 18.0;
    private static final RenderType ICE = RenderType.entityTranslucent(
            ResourceLocation.withDefaultNamespace("textures/block/packed_ice.png"));
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new ByteBufferBuilder(262144));
    private static final Map<BlockPos, Tile> TILES = new HashMap<>();
    private static final Map<Integer, Vec3> PREVIOUS = new HashMap<>();
    private static ClientLevel previousLevel;

    private static final class Tile {
        double seen;
        float strength;
        Tile(double time) { seen = time; }

        float opacity(double time) {
            return strength * (float) Mth.clamp(1 - Math.max(0, time - seen - 2) / FADE_TICKS, 0, 1);
        }
    }

    private IcewakeRenderer() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != previousLevel) {
            TILES.clear();
            PREVIOUS.clear();
            previousLevel = level;
        }
        if (level == null || minecraft.isPaused()) return;
        double time = level.getGameTime();
        TILES.entrySet().removeIf(entry -> time - entry.getValue().seen > FADE_TICKS + 2
                || !level.hasChunkAt(entry.getKey()) || !water(level, entry.getKey()));
        java.util.Set<Integer> active = new java.util.HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Boat boat) || !BoatEnchantments.activeIcewake(boat)) continue;
            if (minecraft.player == null || minecraft.player.distanceToSqr(boat) > 128 * 128) continue;
            double surface = BoatEnchantments.supportHeight(boat);
            if (Double.isNaN(surface)) continue;
            active.add(boat.getId());
            Vec3 position = new Vec3(boat.getX(), surface - 0.015, boat.getZ());
            Vec3 previous = PREVIOUS.put(boat.getId(), position);
            if (previous == null || previous.distanceToSqr(position) > 64) previous = position;
            // One swept grid pass instead of repeatedly scanning the same cells.
            paint(level, previous, position, time);
        }
        PREVIOUS.keySet().retainAll(active);
    }

    private static boolean water(ClientLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource()
                && level.getBlockState(pos.above()).isAir();
    }

    private static void paint(ClientLevel level, Vec3 previous, Vec3 center, double time) {
        int y = Mth.floor(center.y - 0.5);
        double travelX = center.x - previous.x;
        double travelZ = center.z - previous.z;
        double lengthSquared = travelX * travelX + travelZ * travelZ;
        for (int x = Mth.floor(Math.min(previous.x, center.x) - RADIUS);
             x <= Mth.floor(Math.max(previous.x, center.x) + RADIUS); x++) {
            for (int z = Mth.floor(Math.min(previous.z, center.z) - RADIUS);
                 z <= Mth.floor(Math.max(previous.z, center.z) + RADIUS); z++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.hasChunkAt(pos) || !water(level, pos)) continue;
                    double dx = x + 0.5 - previous.x;
                    double dz = z + 0.5 - previous.z;
                    double along = lengthSquared < 0.00001 ? 0
                            : Mth.clamp((dx * travelX + dz * travelZ) / lengthSquared, 0, 1);
                    dx -= along * travelX;
                    dz -= along * travelZ;
                    double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > RADIUS) continue;
                // Whole water cells, not a radial gradient inside each quad.
                float strength = distance <= 1.45 ? 1F : distance <= 1.9 ? 0.7F : 0.4F;
                Tile tile = TILES.computeIfAbsent(pos, key -> new Tile(time));
                // A weaker fringe must not overwrite the solid ice just left
                // behind the hull. Preserve its own lifetime to form the wake.
                if (strength >= tile.opacity(time)) {
                    tile.strength = strength;
                    tile.seen = time;
                }
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || TILES.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level != previousLevel) return;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double time = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = new PoseStack();
        // AFTER_PARTICLES already has the camera rotation in RenderSystem's
        // model-view stack. Applying the event matrix again rotates the water
        // overlay twice, lifting it into a camera-dependent slanted plane.
        poses.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer vertices = BUFFERS.getBuffer(ICE);
        for (var entry : TILES.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!event.getFrustum().isVisible(new net.minecraft.world.phys.AABB(pos).inflate(0.03))) continue;
            Tile tile = entry.getValue();
            float opacity = tile.opacity(time);
            int light = LevelRenderer.getLightColor(level, pos.above());
            vertex(vertices, poses, pos, 0, 0, opacity, light);
            vertex(vertices, poses, pos, 0, 1, opacity, light);
            vertex(vertices, poses, pos, 1, 1, opacity, light);
            vertex(vertices, poses, pos, 1, 0, opacity, light);
        }
        BUFFERS.endBatch(ICE);
    }

    private static void vertex(VertexConsumer vertices, PoseStack poses, BlockPos pos, float x, float z,
                               float opacity, int light) {
        int alpha = Math.round(255 * opacity);
        vertices.addVertex(poses.last(), pos.getX() + x, pos.getY() + 1.006F, pos.getZ() + z)
                .setColor(255, 255, 255, alpha).setUv(x, z).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(poses.last(), 0, 1, 0);
    }
}
