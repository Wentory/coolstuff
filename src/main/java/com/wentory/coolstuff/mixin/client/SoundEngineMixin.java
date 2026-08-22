package com.wentory.coolstuff.mixin.client;

import com.wentory.coolstuff.client.BlackHoleAudioEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Inject(method = "tickNonPaused", at = @At("TAIL"))
    private void coolstuff$absorbWorldSounds(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        instanceToChannel.forEach((sound, handle) -> {
            float base = Mth.clamp(sound.getVolume()
                    * minecraft.options.getSoundSourceVolume(sound.getSource()), 0.0F, 1.0F);
            float volume = base * BlackHoleAudioEffects.volumeMultiplier(sound);
            handle.execute(channel -> channel.setVolume(volume));
        });
    }
}
