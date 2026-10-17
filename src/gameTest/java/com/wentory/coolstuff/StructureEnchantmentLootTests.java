package com.wentory.coolstuff;

import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.registry.ModEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Coolstuff.MODID)
@PrefixGameTestTemplate(false)
public final class StructureEnchantmentLootTests {
    private static List<ItemStack> loot(GameTestHelper helper, String table) {
        var key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/" + table));
        var params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(net.minecraft.core.BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        return helper.getLevel().getServer().reloadableRegistries().getLootTable(key).getRandomItems(params);
    }

    private static int bookLevel(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key));
    }

    @GameTest(template = "boat_enchantments_test")
    public static void structureBooksHaveCorrectLocationsAndLevels(GameTestHelper helper) {
        double serveChance = CoolstuffConfig.SERVE_CHEST_CHANCE.get();
        double accelerationChance = CoolstuffConfig.ACCELERATION_CHEST_CHANCE.get();
        try {
            CoolstuffConfig.SERVE_CHEST_CHANCE.set(1.0);
            CoolstuffConfig.ACCELERATION_CHEST_CHANCE.set(1.0);
            var cityLoot = loot(helper, "ancient_city");
            helper.assertTrue(cityLoot.stream().filter(stack -> bookLevel(helper, stack, ModEnchantments.SERVE) == 1).count() == 1,
                    "Ancient City did not generate exactly one Serve I book at 100% chance");
            helper.assertTrue(cityLoot.stream().noneMatch(stack -> bookLevel(helper, stack, ModEnchantments.SERVE) > 1
                    || bookLevel(helper, stack, ModEnchantments.ACCELERATION) > 0), "Wrong book or level in Ancient City");
            for (String table : List.of("bastion_bridge", "bastion_hoglin_stable", "bastion_other", "bastion_treasure")) {
                var bastionLoot = loot(helper, table);
                helper.assertTrue(bastionLoot.stream().filter(stack -> bookLevel(helper, stack, ModEnchantments.ACCELERATION) == 1).count() == 1,
                        "Missing Acceleration I in " + table);
                helper.assertTrue(bastionLoot.stream().noneMatch(stack -> bookLevel(helper, stack, ModEnchantments.SERVE) > 0),
                        "Serve appeared in a Bastion chest");
            }
            helper.assertTrue(loot(helper, "simple_dungeon").stream().noneMatch(stack ->
                    bookLevel(helper, stack, ModEnchantments.SERVE) > 0 || bookLevel(helper, stack, ModEnchantments.ACCELERATION) > 0),
                    "Structure books appeared in an unrelated loot table");
        } finally {
            CoolstuffConfig.SERVE_CHEST_CHANCE.set(serveChance);
            CoolstuffConfig.ACCELERATION_CHEST_CHANCE.set(accelerationChance);
        }
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void zeroChanceDisablesStructureBooks(GameTestHelper helper) {
        double serveChance = CoolstuffConfig.SERVE_CHEST_CHANCE.get();
        double accelerationChance = CoolstuffConfig.ACCELERATION_CHEST_CHANCE.get();
        try {
            CoolstuffConfig.SERVE_CHEST_CHANCE.set(0.0);
            CoolstuffConfig.ACCELERATION_CHEST_CHANCE.set(0.0);
            helper.assertTrue(loot(helper, "ancient_city").stream().noneMatch(stack -> bookLevel(helper, stack, ModEnchantments.SERVE) > 0),
                    "Serve dropped at 0% chance");
            helper.assertTrue(loot(helper, "bastion_treasure").stream().noneMatch(stack -> bookLevel(helper, stack, ModEnchantments.ACCELERATION) > 0),
                    "Acceleration dropped at 0% chance");
        } finally {
            CoolstuffConfig.SERVE_CHEST_CHANCE.set(serveChance);
            CoolstuffConfig.ACCELERATION_CHEST_CHANCE.set(accelerationChance);
        }
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void twoServeOneBooksCombineIntoServeTwo(GameTestHelper helper) {
        var serve = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.SERVE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(serve, 1);
        menu.getSlot(0).set(book.copy());
        menu.getSlot(1).set(book.copy());
        menu.createResult();
        helper.assertTrue(bookLevel(helper, menu.getSlot(2).getItem(), ModEnchantments.SERVE) == 2,
                "Two Serve I books did not combine into Serve II on a survival anvil");
        helper.succeed();
    }
}
