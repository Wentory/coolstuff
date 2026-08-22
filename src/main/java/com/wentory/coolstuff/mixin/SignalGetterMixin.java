package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.cake.CakeFilling;
import com.wentory.coolstuff.entity.ThrownCakeEntity;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignalGetter.class)
public interface SignalGetterMixin {
    @Inject(method = "getSignal", at = @At("RETURN"), cancellable = true)
    private void coolstuff$redstoneCakeSignal(BlockPos pos, Direction direction,
                                               CallbackInfoReturnable<Integer> cir) {
        if (!RestartRequiredConfig.cakeFillings() || cir.getReturnValue() >= 5
                || !((Object) this instanceof Level level)) return;
        AABB area = new AABB(pos).inflate(0.3);
        boolean poweredByCake = !level.getEntitiesOfClass(ThrownCakeEntity.class, area,
                cake -> cake.getFilling() == CakeFilling.REDSTONE
                        && cake.getCakeState() != ThrownCakeEntity.FLYING
                        && cake.getCakeState() != ThrownCakeEntity.DROPPED).isEmpty();
        if (poweredByCake) cir.setReturnValue(5);
    }
}
