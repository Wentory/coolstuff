package com.wentory.coolstuff.registry;

import com.mojang.serialization.MapCodec;
import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.loot.StructureEnchantmentLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Coolstuff.MODID);

    static {
        MODIFIERS.register("structure_enchantment_books", () -> StructureEnchantmentLootModifier.CODEC);
    }

    private ModLootModifiers() {}
}
