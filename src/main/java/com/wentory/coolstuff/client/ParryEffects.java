package com.wentory.coolstuff.client;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.config.CoolstuffClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Coolstuff.MODID, value = Dist.CLIENT)
public final class ParryEffects {
    private static final SoundEvent PARRY_SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "parry")
    );
    private static final SoundEvent PHASE_TWO_SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "phase2")
    );
    private static int impactTicks;
    private static int impactDuration = 9;
    private static int textTicks;
    private static int textDuration = 30;
    private static int comboLevel = 1;
    private static boolean cakeSplatText;
    private static Vec3 effectPosition = Vec3.ZERO;

    private ParryEffects() {
    }

    public static void playAt(double x, double y, double z, int combo) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        float visualIntensity = CoolstuffClientConfig.PARRY_VISUAL_INTENSITY.get().floatValue();
        float soundVolume = CoolstuffClientConfig.PARRY_SOUND_VOLUME.get().floatValue();
        boolean visible = isVisible(minecraft, new Vec3(x, y, z));
        comboLevel = Math.max(1, combo);
        cakeSplatText = false;
        impactDuration = 9 + Math.min(comboLevel * 2, 18);
        textDuration = comboLevel >= 100 ? 60 : 30 + Math.min(comboLevel * 2, 20);
        impactTicks = visible && visualIntensity > 0.0F ? impactDuration : 0;
        // Floating PARRY text is a separate readability feature and is not
        // controlled by the particles/flash/camera-shake intensity slider.
        textTicks = visible ? textDuration : 0;
        effectPosition = new Vec3(x, y, z);
        if (soundVolume > 0.0F) {
            minecraft.player.playSound(PARRY_SOUND,
                    Math.min(1.8F, 0.9F + comboLevel * 0.1F) * soundVolume,
                    Math.min(1.25F, 0.95F + comboLevel * 0.025F));
            playPhaseTransitionSound(minecraft, comboLevel, soundVolume);
        }

        int particleCount = visible ? Math.round(28 * visualIntensity) : 0;
        for (int i = 0; i < particleCount; i++) {
            double speed = 0.12 + minecraft.level.random.nextDouble() * (0.28 + comboLevel * 0.035);
            Vec3 direction = new Vec3(
                    minecraft.level.random.nextGaussian(),
                    minecraft.level.random.nextGaussian(),
                    minecraft.level.random.nextGaussian()
            ).normalize().scale(speed);
            minecraft.level.addParticle(i == 0 ? ParticleTypes.FLASH : ParticleTypes.CRIT,
                    x, y, z, direction.x, direction.y, direction.z);
        }
    }

    public static void playCakeSplatAt(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        Vec3 position = new Vec3(x, y, z);
        if (!isVisible(minecraft, position)) return;
        cakeSplatText = true;
        comboLevel = 1;
        impactTicks = 0;
        textDuration = 36;
        textTicks = textDuration;
        effectPosition = position;
    }

    private static boolean isVisible(Minecraft minecraft, Vec3 position) {
        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 direction = position.subtract(cameraPosition);
        if (direction.lengthSqr() < 0.01) return true;
        Vector3f cameraLook = minecraft.gameRenderer.getMainCamera().getLookVector();
        double facing = direction.normalize().dot(new Vec3(cameraLook.x, cameraLook.y, cameraLook.z));
        if (facing < 0.15) return false;

        HitResult hit = minecraft.level.clip(new ClipContext(cameraPosition, position,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(position) <= 1.0;
    }

    private static void playPhaseTransitionSound(Minecraft minecraft, int combo, float soundVolume) {
        SoundEvent transition = switch (combo) {
            case 5 -> PHASE_TWO_SOUND;
            case 10 -> SoundEvents.END_PORTAL_FRAME_FILL;
            case 20 -> SoundEvents.BEACON_ACTIVATE;
            default -> null;
        };
        if (transition != null) {
            minecraft.player.playSound(transition, 1.5F * soundVolume, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (impactTicks > 0) impactTicks--;
        if (textTicks > 0) textTicks--;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (impactTicks <= 0 && textTicks <= 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (impactTicks > 0) {
            float visualIntensity = CoolstuffClientConfig.PARRY_VISUAL_INTENSITY.get().floatValue();
            float impactProgress = impactTicks / (float) impactDuration;
            int maxFlash = Math.min(255, 135 + comboLevel * 15);
            int flashAlpha = Mth.clamp((int) (maxFlash * impactProgress * impactProgress * visualIntensity), 0, maxFlash);
            int flashColor = comboLevel >= 5 ? 0xFF7040 : comboLevel >= 3 ? 0xFFB040 : 0xFFF0C0;
            graphics.fill(0, 0, width, height, flashAlpha << 24 | flashColor);
        }

        if (textTicks <= 0) return;
        float progress = textTicks / (float) textDuration;
        float age = 1.0F - progress;
        Vec3 worldPoint = effectPosition.add(0.0, 0.25 + age * 1.15, 0.0);
        Vec3 relative = worldPoint.subtract(minecraft.gameRenderer.getMainCamera().getPosition());
        Quaternionf inverseCamera = minecraft.gameRenderer.getMainCamera().rotation().conjugate(new Quaternionf());
        Vector3f cameraSpace = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z).rotate(inverseCamera);
        float depth = -cameraSpace.z;
        if (depth <= 0.05F) return;

        double fovRadians = Math.toRadians(minecraft.options.fov().get());
        float focalLength = (float) (height * 0.5 / Math.tan(fovRadians * 0.5));
        float screenX = width * 0.5F + cameraSpace.x * focalLength / depth;
        float screenY = height * 0.5F - cameraSpace.y * focalLength / depth;
        if (screenX < -80 || screenX > width + 80 || screenY < -40 || screenY > height + 40) return;

        float appear = Mth.clamp(age * 7.0F, 0.0F, 1.0F);
        int visualCombo = Math.min(comboLevel, 8);
        float scale = (comboLevel >= 100 ? 3.0F : 1.65F + visualCombo * 0.13F) * (0.65F + appear * 0.35F);
        int alpha = Mth.clamp((int) (255.0F * appear * Mth.clamp(progress * 1.45F, 0.0F, 1.0F)), 0, 255);
        if (alpha <= 0) return;
        String text = cakeSplatText ? "SPLAT!"
                : comboLevel >= 100 ? "BLACK HOLE" : comboLevel > 1 ? "PARRY x" + comboLevel : "PARRY";
        int textWidth = minecraft.font.width(text);
        int textColor = cakeSplatText ? 0xFFF4E8
                : comboLevel >= 100 ? 0xB060FF : comboLevel >= 5 ? 0xFF5533 : comboLevel >= 3 ? 0xFFAA33 : 0xFFE45C;
        graphics.pose().pushPose();
        graphics.pose().translate(screenX, screenY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, text, -textWidth / 2, 0, alpha << 24 | textColor, true);
        graphics.pose().popPose();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (impactTicks <= 0) return;
        float visualIntensity = CoolstuffClientConfig.PARRY_VISUAL_INTENSITY.get().floatValue();
        int visualCombo = Math.min(comboLevel, 12);
        double strength = impactTicks / (double) impactDuration * (1.0 + visualCombo * 0.16)
                * visualIntensity;
        double phase = System.nanoTime() * 0.000000035;
        event.setYaw(event.getYaw() + (float) (Math.sin(phase * 1.7) * 1.4 * strength));
        event.setPitch(event.getPitch() + (float) (Math.cos(phase * 2.3) * 0.9 * strength));
        event.setRoll(event.getRoll() + (float) (Math.sin(phase) * 2.2 * strength));
    }
}
