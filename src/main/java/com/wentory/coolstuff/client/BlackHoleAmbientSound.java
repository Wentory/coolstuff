package com.wentory.coolstuff.client;

import com.wentory.coolstuff.entity.BlackHoleEntity;
import com.wentory.coolstuff.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

final class BlackHoleAmbientSound extends AbstractTickableSoundInstance {
    private final BlackHoleEntity blackHole;

    BlackHoleAmbientSound(BlackHoleEntity blackHole) {
        super(ModSounds.BLACK_HOLE_AMBIENT.get(), SoundSource.AMBIENT, RandomSource.create());
        this.blackHole = blackHole;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        updatePosition();
    }

    @Override
    public void tick() {
        if (blackHole.isRemoved() || blackHole.isPacified()) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        x = blackHole.getX();
        y = blackHole.getY();
        z = blackHole.getZ();
    }

    void stopNow() {
        stop();
    }
}
