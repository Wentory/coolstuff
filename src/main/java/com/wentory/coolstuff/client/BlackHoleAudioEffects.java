package com.wentory.coolstuff.client;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.BlackHoleEntity;
import com.wentory.coolstuff.fireball.FireballPhase;
import com.wentory.coolstuff.fireball.FireballPhaseAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Coolstuff.MODID, value = Dist.CLIENT)
public final class BlackHoleAudioEffects {
    private static final double SILENCE_RADIUS = 64.0;
    private static final Map<Integer, BlackHoleAmbientSound> AMBIENT_SOUNDS = new HashMap<>();
    private static int absorptionTicks;

    private BlackHoleAudioEffects() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            absorptionTicks = 0;
            AMBIENT_SOUNDS.clear();
            return;
        }

        boolean formingNearby = minecraft.level.getEntitiesOfClass(LargeFireball.class,
                        minecraft.player.getBoundingBox().inflate(SILENCE_RADIUS))
                .stream().anyMatch(fireball -> FireballPhase.fromCombo(
                        ((FireballPhaseAccess) fireball).coolstuff$getParryCombo()) == FireballPhase.BLACK_HOLE
                        && fireball.distanceToSqr(minecraft.player) <= SILENCE_RADIUS * SILENCE_RADIUS);
        absorptionTicks = formingNearby ? Math.min(100, absorptionTicks + 1)
                : Math.max(0, absorptionTicks - 5);

        Set<Integer> present = new HashSet<>();
        for (BlackHoleEntity hole : minecraft.level.getEntitiesOfClass(BlackHoleEntity.class,
                minecraft.player.getBoundingBox().inflate(160.0))) {
            if (hole.isPacified()) continue;
            present.add(hole.getId());
            AMBIENT_SOUNDS.computeIfAbsent(hole.getId(), id -> {
                BlackHoleAmbientSound sound = new BlackHoleAmbientSound(hole);
                minecraft.getSoundManager().play(sound);
                return sound;
            });
        }
        AMBIENT_SOUNDS.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey())) return false;
            entry.getValue().stopNow();
            return true;
        });
    }

    public static float volumeMultiplier(SoundInstance sound) {
        if (absorptionTicks <= 0 || sound.isRelative() || sound.getSource() == SoundSource.MUSIC) return 1.0F;
        if (sound.getLocation().getNamespace().equals(Coolstuff.MODID)
                && sound.getLocation().getPath().startsWith("blackhole_")) return 1.0F;
        return 1.0F - absorptionTicks / 100.0F;
    }
}
