package com.wentory.coolstuff.mixin;

import com.wentory.coolstuff.entity.ZombieWolfEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGoalMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @ModifyArg(
            method = "canUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            index = 2
    )
    private Predicate<LivingEntity> coolstuff$ignoreZombieWolves(Predicate<LivingEntity> original) {
        if (!(mob instanceof AbstractSkeleton)) return original;
        return original.and(entity -> !(entity instanceof ZombieWolfEntity));
    }
}
