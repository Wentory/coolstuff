package com.wentory.coolstuff.mixin.client;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.world.entity.AnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HierarchicalModel.class)
public interface HierarchicalModelAccessor {
    @Invoker("animate")
    void coolstuff$animate(AnimationState state, AnimationDefinition animation, float ageInTicks);

    @Invoker("applyStatic")
    void coolstuff$applyStatic(AnimationDefinition animation);
}
