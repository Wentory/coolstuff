package com.wentory.coolstuff.registry;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.config.BootstrapConfig;
import com.wentory.coolstuff.item.CannonItem;
import com.wentory.coolstuff.item.BadmintonItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import java.util.Optional;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Coolstuff.MODID);

    public static final DeferredItem<CannonItem> FIREBALL_CANNON = ITEMS.registerItem(
            "fireball_cannon", CannonItem::new, new Item.Properties().durability(64));
    public static final DeferredItem<BadmintonItem> BADMINTON = ITEMS.registerItem(
            "badminton", BadmintonItem::new, new Item.Properties().durability(100)
                    .attributes(ItemAttributeModifiers.builder()
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(
                                            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "badminton_attack_damage"),
                                            1.0, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ENTITY_INTERACTION_RANGE,
                                    new AttributeModifier(
                                            ResourceLocation.fromNamespaceAndPath(Coolstuff.MODID, "badminton_attack_range"),
                                            1.0, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build()));
    public static final DeferredItem<Item> GHAST_CORE = ITEMS.registerSimpleItem(
            "ghast_core", new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));

    public static final Optional<DeferredItem<DeferredSpawnEggItem>> SPORE_CREEPER_SPAWN_EGG =
            BootstrapConfig.SPORE_CREEPER ? Optional.of(ITEMS.register("spore_creeper_spawn_egg",
                    () -> new DeferredSpawnEggItem(
                            () -> ModEntities.LEAPING_CREEPER.orElseThrow().get(),
                            0x477A3F, 0xE0C43A, new Item.Properties()))) : Optional.empty();

    public static final Optional<DeferredItem<DeferredSpawnEggItem>> FROSTLING_SPAWN_EGG =
            BootstrapConfig.FROSTLING ? Optional.of(ITEMS.register("frostling_spawn_egg",
                    () -> new DeferredSpawnEggItem(
                            () -> ModEntities.FROSTLING.orElseThrow().get(),
                            0x3B7774, 0xB7E8EF, new Item.Properties()))) : Optional.empty();

    public static final Optional<DeferredItem<DeferredSpawnEggItem>> ZOMBIE_WOLF_SPAWN_EGG =
            BootstrapConfig.ZOMBIE_WOLF ? Optional.of(ITEMS.register("zombie_wolf_spawn_egg",
                    () -> new DeferredSpawnEggItem(
                            () -> ModEntities.ZOMBIE_WOLF.orElseThrow().get(),
                            0x3F7045, 0xC7CCB8, new Item.Properties()))) : Optional.empty();

    private ModItems() {
    }
}
