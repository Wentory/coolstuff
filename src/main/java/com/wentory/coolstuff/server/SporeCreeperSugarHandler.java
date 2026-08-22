package com.wentory.coolstuff.server;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.entity.LeapingCreeperEntity;
import com.wentory.coolstuff.registry.ModEntities;
import com.wentory.coolstuff.mixin.CreeperAccessor;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import com.wentory.coolstuff.entity.CreeperSugarTransformationAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;

@EventBusSubscriber(modid = Coolstuff.MODID)
public final class SporeCreeperSugarHandler {
    private static final double SEARCH_RADIUS = 10.0;
    private static final String TRANSFORM_TICKS = "coolstuff_sugar_transform_ticks";
    private static final int TRANSFORMATION_DURATION = 60;

    private SporeCreeperSugarHandler() {
    }

    @SubscribeEvent
    public static void onCreeperTick(EntityTickEvent.Post event) {
        if (event.getEntity().getClass() != Creeper.class
                || !(event.getEntity().level() instanceof ServerLevel serverLevel)) return;

        Creeper creeper = (Creeper) event.getEntity();
        if (!CoolstuffConfig.ENABLE_SUGAR_TRANSFORMATION.get()
                || !RestartRequiredConfig.sporeCreeper()) {
            creeper.getPersistentData().remove(TRANSFORM_TICKS);
            ((CreeperSugarTransformationAccess) creeper).coolstuff$setSugarTransforming(false);
            return;
        }
        int transformingTicks = creeper.getPersistentData().getInt(TRANSFORM_TICKS);
        if (transformingTicks > 0) {
            tickTransformation(serverLevel, creeper, transformingTicks);
            return;
        }
        ItemEntity sugar = serverLevel.getEntitiesOfClass(ItemEntity.class,
                        new AABB(creeper.blockPosition()).inflate(SEARCH_RADIUS),
                        item -> item.isAlive() && !item.getItem().isEmpty() && item.getItem().is(Items.SUGAR))
                .stream().min(Comparator.comparingDouble(creeper::distanceToSqr)).orElse(null);
        if (sugar == null) return;

        creeper.setTarget(null);
        creeper.setSwellDir(-1);
        creeper.getLookControl().setLookAt(sugar, 30.0F, 30.0F);
        creeper.getNavigation().moveTo(sugar, 1.18);

        if (creeper.distanceToSqr(sugar) <= 1.7) consumeAndBeginTransformation(creeper, sugar);
    }

    private static void consumeAndBeginTransformation(Creeper creeper, ItemEntity sugar) {
        ItemStack stack = sugar.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) sugar.discard();
        else sugar.setItem(stack);

        creeper.setTarget(null);
        creeper.setSwellDir(-1);
        creeper.getNavigation().stop();
        creeper.getPersistentData().putInt(TRANSFORM_TICKS, TRANSFORMATION_DURATION);
        ((CreeperSugarTransformationAccess) creeper).coolstuff$setSugarTransforming(true);
    }

    private static void tickTransformation(ServerLevel level, Creeper creeper, int ticks) {
        ((CreeperSugarTransformationAccess) creeper).coolstuff$setSugarTransforming(true);
        creeper.setTarget(null);
        creeper.setSwellDir(-1);
        creeper.getNavigation().stop();
        if (ticks > 1) {
            creeper.getPersistentData().putInt(TRANSFORM_TICKS, ticks - 1);
            return;
        }

        var type = ModEntities.LEAPING_CREEPER.orElse(null);
        if (type == null) return;
        LeapingCreeperEntity spore = new LeapingCreeperEntity(type.get(), level);
        CreeperTransformation.copyState(creeper, spore);
        spore.getEntityData().set(CreeperAccessor.coolstuff$poweredData(), creeper.isPowered());
        level.playSound(null, creeper.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 1.15F);
        creeper.discard();
        level.addFreshEntity(spore);
        DebugMode.markAndAnnounce(spore, "Spore Creeper (sugar overload)");
    }
}
