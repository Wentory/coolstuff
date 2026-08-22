package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class CakeDaze {
    private static final String TICKS_TAG = Coolstuff.MODID + ":cake_daze_ticks";
    private static final int DURATION = 10;

    private CakeDaze() {
    }

    public static void apply(Mob mob) {
        if (mob.getMaxHealth() > 50.0F || mob.getBbWidth() > 1.5F || mob.getBbHeight() > 2.5F || mob.isNoAi()) {
            return;
        }
        mob.getPersistentData().putInt(TICKS_TAG, DURATION);
        mob.getNavigation().stop();
        mob.setNoAi(true);
        mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
    }

    @SubscribeEvent
    public static void onMobTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        int ticks = mob.getPersistentData().getInt(TICKS_TAG);
        if (ticks <= 0) return;

        mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
        if (ticks == 1) {
            mob.getPersistentData().remove(TICKS_TAG);
            mob.setNoAi(false);
        } else {
            mob.getPersistentData().putInt(TICKS_TAG, ticks - 1);
        }
    }
}
