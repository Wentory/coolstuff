package com.wentory.coolstuff.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.registry.ModEnchantments;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.Set;

public final class StructureEnchantmentLootModifier extends LootModifier {
    public static final MapCodec<StructureEnchantmentLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, StructureEnchantmentLootModifier::new));
    private static final ResourceLocation ANCIENT_CITY = ResourceLocation.withDefaultNamespace("chests/ancient_city");
    private static final Set<ResourceLocation> BASTIONS = Set.of(
            ResourceLocation.withDefaultNamespace("chests/bastion_bridge"),
            ResourceLocation.withDefaultNamespace("chests/bastion_hoglin_stable"),
            ResourceLocation.withDefaultNamespace("chests/bastion_other"),
            ResourceLocation.withDefaultNamespace("chests/bastion_treasure"));

    public StructureEnchantmentLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        ResourceKey<Enchantment> enchantment;
        double chance;
        if (ANCIENT_CITY.equals(table)) {
            enchantment = ModEnchantments.SERVE;
            chance = CoolstuffConfig.SERVE_CHEST_CHANCE.get();
        } else if (BASTIONS.contains(table)) {
            enchantment = ModEnchantments.ACCELERATION;
            chance = CoolstuffConfig.ACCELERATION_CHEST_CHANCE.get();
        } else {
            return generatedLoot;
        }
        if (context.getRandom().nextFloat() < chance) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.enchant(context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), 1);
            generatedLoot.add(book);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
