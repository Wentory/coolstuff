package com.wentory.coolstuff.client;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.snow.SnowHeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = Coolstuff.MODID, value = Dist.CLIENT)
public final class SnowballScreenEffects {
    private static final ResourceLocation SPLAT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Coolstuff.MODID, "textures/gui/overlay/snowball_splat.png");
    private static final int SPLAT_SIZE = 128;
    private static final List<Splat> SPLATS = new ArrayList<>();

    private SnowballScreenEffects() {
    }

    public static void trigger(int effectSeed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Random random = new Random(effectSeed);
        if (SPLATS.size() >= 12) SPLATS.removeFirst();
        SPLATS.add(new Splat(random.nextFloat(), random.nextFloat(),
                0.8F + random.nextFloat() * 0.2F));
        minecraft.player.setYRot(minecraft.player.getYRot() + (random.nextFloat() - 0.5F) * 3.0F);
        minecraft.player.setXRot(Mth.clamp(minecraft.player.getXRot()
                + (random.nextFloat() - 0.5F) * 1.8F, -90.0F, 90.0F));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        int meltSpeed = minecraft.level != null && minecraft.player != null
                ? SnowHeat.meltMultiplier(minecraft.level, minecraft.player.blockPosition()) : 1;
        Iterator<Splat> iterator = SPLATS.iterator();
        while (iterator.hasNext()) {
            Splat splat = iterator.next();
            splat.ticks -= meltSpeed;
            if (splat.ticks <= 0) iterator.remove();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (SPLATS.isEmpty()) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (Splat splat : SPLATS) {
            int x = Math.round(splat.x * Math.max(0, width - SPLAT_SIZE));
            float fadeProgress = Mth.clamp(splat.ticks / 120.0F, 0.0F, 1.0F);
            float fade = fadeProgress * fadeProgress * (3.0F - 2.0F * fadeProgress);
            float meltProgress = 1.0F - fadeProgress;
            int y = Math.round(splat.y * Math.max(0, height - SPLAT_SIZE)
                    + meltProgress * meltProgress * 100.0F);
            drawSplat(graphics, x, y, splat.opacity * fade);
        }
    }

    private static void drawSplat(GuiGraphics graphics, int x, int y, float alpha) {
        RenderSystem.setShaderTexture(0, SPLAT_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix, x, y + SPLAT_SIZE, 0).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(matrix, x + SPLAT_SIZE, y + SPLAT_SIZE, 0).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(matrix, x + SPLAT_SIZE, y, 0).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
        buffer.addVertex(matrix, x, y, 0).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static final class Splat {
        private final float x;
        private final float y;
        private final float opacity;
        private int ticks = 600;

        private Splat(float x, float y, float opacity) {
            this.x = x;
            this.y = y;
            this.opacity = opacity;
        }
    }
}
