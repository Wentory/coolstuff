package com.wentory.coolstuff.registry;

import com.mojang.serialization.MapCodec;
import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.config.EnabledContentCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, Coolstuff.MODID);

    static {
        CONDITIONS.register("content_enabled", () -> EnabledContentCondition.CODEC);
    }

    private ModConditions() {}
}
