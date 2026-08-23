package com.wentory.coolstuff.server;

import com.wentory.coolstuff.cake.CakeFilling;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.config.RestartRequiredConfig;
import com.wentory.coolstuff.entity.ThrownCakeEntity;
import com.wentory.coolstuff.registry.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public final class CakeDispenserBehavior extends DefaultDispenseItemBehavior {
    private CakeDispenserBehavior() {
    }

    public static void register() {
        DispenserBlock.registerBehavior(Items.CAKE, new CakeDispenserBehavior());
    }

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        if (!CoolstuffConfig.ENABLE_THROWABLE_CAKES.get()) {
            return super.execute(source, stack);
        }

        Direction direction = source.state().getValue(DispenserBlock.FACING);
        Vec3 directionVector = Vec3.atLowerCornerOf(direction.getNormal());
        Vec3 spawnPosition = source.center().add(directionVector.scale(0.7));

        ThrownCakeEntity cake = new ThrownCakeEntity(ModEntities.THROWN_CAKE.get(), source.level());
        cake.setFilling(RestartRequiredConfig.cakeFillings()
                ? CakeFilling.fromStack(stack)
                : CakeFilling.NONE);
        cake.setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        cake.shoot(directionVector.x, directionVector.y, directionVector.z, 1.15F, 0.7F);
        source.level().addFreshEntity(cake);
        source.level().playSound(null, source.pos(), SoundEvents.SNOWBALL_THROW,
                SoundSource.BLOCKS, 0.8F, 0.72F);
        stack.shrink(1);
        return stack;
    }
}