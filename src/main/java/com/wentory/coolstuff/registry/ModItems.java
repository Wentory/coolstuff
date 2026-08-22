package com.wentory.coolstuff.registry;

import com.wentory.coolstuff.Coolstuff;
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

    private ModItems() {
    }
}
