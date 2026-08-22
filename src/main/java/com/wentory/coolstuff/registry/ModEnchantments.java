package com.wentory.coolstuff.registry;

import com.wentory.coolstuff.Coolstuff;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {
    public static final ResourceKey<Enchantment> SERVE = ResourceKey.create(
            Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "serve"));
    public static final ResourceKey<Enchantment> ACCELERATION = ResourceKey.create(
            Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "acceleration"));

    private ModEnchantments() {
    }
}
