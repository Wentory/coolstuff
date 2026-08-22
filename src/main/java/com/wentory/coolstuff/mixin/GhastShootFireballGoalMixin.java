package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.server.UltraGhast;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import net.minecraft.world.entity.monster.Ghast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastShootFireballGoal")
public abstract class GhastShootFireballGoalMixin {
    @Shadow @Final private Ghast ghast;
    @Shadow public int chargeTime;

    private boolean coolstuff$burstActive;
    private int coolstuff$shotsRemaining;

    @Inject(method = "tick", at = @At("TAIL"))
    private void coolstuff$continueUltraBurst(CallbackInfo ci) {
        if (!RestartRequiredConfig.ultraGhast() || !UltraGhast.isUltra(ghast)) {
            coolstuff$burstActive = false;
            coolstuff$shotsRemaining = 0;
            return;
        }

        // Vanilla sets chargeTime to -40 immediately after firing. Re-arm it at 15,
        // keeping the charging/open-mouth state active until the whole burst is done.
        if (chargeTime != -40) return;

        if (!coolstuff$burstActive) {
            int burstSize = 3 + ghast.getRandom().nextInt(3);
            coolstuff$shotsRemaining = burstSize - 1;
            coolstuff$burstActive = true;
        } else {
            coolstuff$shotsRemaining--;
        }

        if (coolstuff$shotsRemaining > 0) {
            chargeTime = 15;
            ghast.setCharging(true);
        } else {
            coolstuff$burstActive = false;
            ghast.setCharging(false);
        }
    }

    @Inject(method = "stop", at = @At("TAIL"))
    private void coolstuff$cancelInterruptedBurst(CallbackInfo ci) {
        coolstuff$burstActive = false;
        coolstuff$shotsRemaining = 0;
    }
}
