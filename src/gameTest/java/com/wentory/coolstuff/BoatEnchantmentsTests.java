package com.wentory.coolstuff;

import com.wentory.coolstuff.entity.EnchantedBoatAccess;
import com.wentory.coolstuff.registry.ModBlocks;
import com.wentory.coolstuff.registry.ModEnchantments;
import com.wentory.coolstuff.server.BoatEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Coolstuff.MODID)
@PrefixGameTestTemplate(false)
public final class BoatEnchantmentsTests {
    private static ItemStack enchantedBoat(GameTestHelper helper, boolean chest) {
        ItemStack stack = new ItemStack(chest ? Items.OAK_CHEST_BOAT : Items.OAK_BOAT);
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(registry.getOrThrow(ModEnchantments.ICEWAKE), 1);
        stack.enchant(registry.getOrThrow(ModEnchantments.CLAMBER), 1);
        return stack;
    }

    @GameTest(template = "boat_enchantments_test")
    public static void booksAndSurvivalAnvil(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var key : java.util.List.of(ModEnchantments.ICEWAKE, ModEnchantments.CLAMBER)) {
            var enchantment = registry.getOrThrow(key);
            helper.assertTrue(enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE), "Missing enchanting-table pool entry");
            helper.assertTrue(!enchantment.is(EnchantmentTags.TRADEABLE), "Boat books should not be traded");
            helper.assertTrue(!enchantment.is(EnchantmentTags.ON_RANDOM_LOOT), "Boat books should not be random loot");
            var options = EnchantmentHelper.getAvailableEnchantmentResults(30, new ItemStack(Items.BOOK),
                    java.util.stream.Stream.of(enchantment));
            helper.assertTrue(!options.isEmpty(), "An ordinary book cannot roll the boat enchantment");
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.enchant(enchantment, 1);
            menu.getSlot(0).set(new ItemStack(Items.OAK_BOAT));
            menu.getSlot(1).set(book);
            menu.createResult();
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(enchantment, menu.getSlot(2).getItem()) == 1,
                    "Survival anvil did not apply the book to a boat");
        }
        helper.assertTrue(Enchantment.areCompatible(registry.getOrThrow(ModEnchantments.ICEWAKE),
                registry.getOrThrow(ModEnchantments.CLAMBER)), "Boat enchantments should combine");
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void savePickAndDrop(GameTestHelper helper) {
        for (boolean chest : new boolean[]{false, true}) {
            Boat boat = chest ? new ChestBoat(helper.getLevel(), 0, 0, 0) : new Boat(helper.getLevel(), 0, 0, 0);
            ((EnchantedBoatAccess) boat).coolstuff$setBoatStack(enchantedBoat(helper, chest));
            CompoundTag saved = new CompoundTag();
            boat.saveWithoutId(saved);
            Boat restored = chest ? new ChestBoat(helper.getLevel(), 0, 0, 0) : new Boat(helper.getLevel(), 0, 0, 0);
            restored.load(saved);
            helper.assertTrue(BoatEnchantments.has(restored, ModEnchantments.ICEWAKE), "Icewake lost on reload");
            helper.assertTrue(BoatEnchantments.has(restored, ModEnchantments.CLAMBER), "Clamber lost on reload");
            var icewake = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.ICEWAKE);
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(icewake, restored.getPickResult()) == 1,
                    "Pick block lost enchantments");
            var drop = restored.spawnAtLocation(new ItemStack(restored.getDropItem()));
            helper.assertTrue(drop != null && EnchantmentHelper.getItemEnchantmentLevel(icewake, drop.getItem()) == 1,
                    "Boat drop lost enchantments");
            if (drop != null) drop.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void dispenserPreservesEnchantments(GameTestHelper helper) {
        helper.setBlock(4, 2, 4, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        helper.setBlock(5, 2, 4, Blocks.WATER);
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        var source = new BlockSource(helper.getLevel(), pos, helper.getLevel().getBlockState(pos),
                (DispenserBlockEntity) helper.getLevel().getBlockEntity(pos));
        new BoatDispenseItemBehavior(Boat.Type.OAK).execute(source, enchantedBoat(helper, false));
        var boats = helper.getEntities(net.minecraft.world.entity.EntityType.BOAT);
        helper.assertTrue(boats.size() == 1 && BoatEnchantments.has(boats.getFirst(), ModEnchantments.ICEWAKE),
                "Dispenser lost boat enchantments");
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test", timeoutTicks = 50)
    public static void icewakeSupportsBoatWithoutChangingWater(GameTestHelper helper) {
        for (int x = 2; x <= 9; x++) for (int z = 2; z <= 9; z++) helper.setBlock(x, 2, z, Blocks.WATER);
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        Boat boat = new Boat(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5);
        ((EnchantedBoatAccess) boat).coolstuff$setBoatStack(enchantedBoat(helper, false));
        helper.getLevel().addFreshEntity(boat);
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(boat, true);
        BoatEnchantments.tick(boat);
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.WATER), "Icewake modified the water block");
        helper.assertTrue(boat.getY() >= pos.getY() + 1.0, "Hull was embedded in the ice");
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.WATER), "Icewake replaced water while moving");
            helper.assertTrue(!Double.isNaN(BoatEnchantments.supportHeight(boat)), "Boat lost its virtual surface support");
            rider.stopRiding();
            boat.discard();
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.WATER), "Ice trail failed to restore water");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "boat_enchantments_test")
    public static void icewakeGlidesWithoutPlacingBlocks(GameTestHelper helper) throws Exception {
        for (int x = 1; x <= 11; x++) for (int z = 2; z <= 9; z++) helper.setBlock(x, 2, z, Blocks.WATER);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 5));
        Boat boat = new Boat(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 1.001, pos.getZ() + 0.5);
        ((EnchantedBoatAccess) boat).coolstuff$setBoatStack(enchantedBoat(helper, false));
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(boat, true);
        boat.positionRider(rider);
        BoatEnchantments.tick(boat);
        boat.setPos(boat.getX() + 1.5, boat.getY(), boat.getZ());
        boat.setDeltaMovement(Vec3.ZERO); // Mirrors a server receiving a vehicle position packet.
        boat.positionRider(rider);
        BoatEnchantments.tick(boat);
        helper.assertTrue(helper.getLevel().getBlockState(pos.offset(5, 0, 0)).is(Blocks.WATER),
                "Icewake created real ice ahead of the boat");
        boat.setDeltaMovement(1.5, -0.04, 0.2);
        var physics = Boat.class.getDeclaredMethod("floatBoat");
        physics.setAccessible(true);
        physics.invoke(boat);
        helper.assertTrue(Math.abs(boat.getDeltaMovement().x - 1.47) < 0.0001,
                "Virtual ice did not apply vanilla ice friction");
        helper.assertTrue(Math.abs(boat.getDeltaMovement().y) < 0.001, "Virtual ice let the boat sink into water");
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void icewakeRecoversFromDelayedIce(GameTestHelper helper) {
        for (int x = 2; x <= 9; x++) for (int z = 2; z <= 9; z++) helper.setBlock(x, 2, z, ModBlocks.ICEWAKE_ICE.get());
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        Boat boat = new Boat(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5);
        ((EnchantedBoatAccess) boat).coolstuff$setBoatStack(enchantedBoat(helper, false));
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(boat, true);
        boat.positionRider(rider);
        BoatEnchantments.tick(boat);
        helper.assertTrue(boat.getY() >= pos.getY() + 1.0, "Boat remained embedded in its own ice");
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void clamberClimbsOneBlock(GameTestHelper helper) {
        for (int x = 2; x <= 9; x++) for (int z = 2; z <= 9; z++) helper.setBlock(x, 1, z, Blocks.STONE);
        for (int z = 3; z <= 7; z++) helper.setBlock(6, 2, z, Blocks.STONE);
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        Boat boat = new Boat(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ() + 0.5);
        ((EnchantedBoatAccess) boat).coolstuff$setBoatStack(enchantedBoat(helper, false));
        boat.setOnGround(true);
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(boat, true);
        boat.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.6, -0.04, 0));
        helper.assertTrue(boat.getY() > pos.getY() + 0.99, "Boat did not climb a one-block ledge");
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void playerPlacementPreservesEnchantments(GameTestHelper helper) {
        for (int x = 2; x <= 9; x++) for (int z = 2; z <= 9; z++) helper.setBlock(x, 2, z, Blocks.WATER);
        BlockPos pos = helper.absolutePos(new BlockPos(5, 4, 5));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 90);
        ItemStack stack = enchantedBoat(helper, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        var boats = helper.getEntities(net.minecraft.world.entity.EntityType.BOAT);
        helper.assertTrue(boats.size() == 1 && BoatEnchantments.has(boats.getFirst(), ModEnchantments.ICEWAKE)
                && BoatEnchantments.has(boats.getFirst(), ModEnchantments.CLAMBER), "Player placement lost enchantments");
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void clamberFromWaterToShore(GameTestHelper helper) {
        for (int x = 2; x <= 5; x++) for (int z = 2; z <= 9; z++) helper.setBlock(x, 2, z, Blocks.WATER);
        for (int z = 3; z <= 7; z++) {
            helper.setBlock(6, 2, z, Blocks.STONE);
            helper.setBlock(6, 3, z, Blocks.STONE);
        }
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        Boat boat = new Boat(helper.getLevel(), pos.getX(), pos.getY() + 0.6, pos.getZ() + 0.5);
        ItemStack stack = new ItemStack(Items.OAK_BOAT);
        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModEnchantments.CLAMBER), 1);
        ((EnchantedBoatAccess) boat).coolstuff$setBoatStack(stack);
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(boat, true);
        boat.tick();
        helper.assertTrue(boat.isInWater(), "Test boat is not floating in water");
        boat.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.6, -0.04, 0));
        helper.assertTrue(boat.getY() >= pos.getY() + 2.0, "Boat failed to climb one block above the water surface");
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void clamberWorksThroughActualMove(GameTestHelper helper) {
        for (int x = 2; x <= 9; x++) for (int z = 2; z <= 9; z++) helper.setBlock(x, 1, z, Blocks.STONE);
        for (int z = 3; z <= 7; z++) helper.setBlock(6, 2, z, Blocks.STONE);
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        Boat boat = new Boat(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ() + 0.5);
        ((EnchantedBoatAccess) boat).coolstuff$setBoatStack(enchantedBoat(helper, false));
        boat.setOnGround(true);
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(boat, true);
        boat.positionRider(rider);
        for (int i = 0; i < 30; i++) {
            boat.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.04, -0.04, 0));
            boat.positionRider(rider);
        }
        helper.assertTrue(boat.getY() >= pos.getY() + 0.99 && boat.getX() > pos.getX() + 1.0,
                "Actual boat movement did not climb the ledge: " + boat.position());
        boat.setPos(pos.getX(), pos.getY(), pos.getZ() + 0.5);
        boat.setOnGround(true);
        boat.positionRider(rider);
        for (int i = 0; i < 30; i++) {
            boat.move(net.minecraft.world.entity.MoverType.PLAYER, new Vec3(0.04, -0.04, 0));
            boat.positionRider(rider);
        }
        helper.assertTrue(boat.getY() >= pos.getY() + 0.99 && boat.getX() > pos.getX() + 1.0,
                "Server vehicle movement did not climb the ledge");
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void clamberRejectsWallsAndCeilings(GameTestHelper helper) {
        for (int x = 2; x <= 9; x++) for (int z = 2; z <= 9; z++) helper.setBlock(x, 1, z, Blocks.STONE);
        for (int z = 3; z <= 7; z++) {
            helper.setBlock(6, 2, z, Blocks.STONE);
            helper.setBlock(6, 3, z, Blocks.STONE);
        }
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        Boat boat = new Boat(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ() + 0.5);
        ((EnchantedBoatAccess) boat).coolstuff$setBoatStack(enchantedBoat(helper, false));
        boat.setOnGround(true);
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(boat, true);
        boat.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.6, -0.04, 0));
        helper.assertTrue(boat.getY() == pos.getY(), "Boat climbed a two-block wall");
        helper.succeed();
    }

    @GameTest(template = "boat_enchantments_test")
    public static void clamberChecksPassengerHeadroom(GameTestHelper helper) {
        for (int x = 2; x <= 9; x++) for (int z = 2; z <= 9; z++) helper.setBlock(x, 1, z, Blocks.STONE);
        for (int z = 3; z <= 7; z++) helper.setBlock(6, 2, z, Blocks.STONE);
        for (int x = 4; x <= 7; x++) for (int z = 3; z <= 7; z++) helper.setBlock(x, 4, z, Blocks.STONE);
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        Boat boat = new Boat(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ() + 0.5);
        ((EnchantedBoatAccess) boat).coolstuff$setBoatStack(enchantedBoat(helper, false));
        boat.setOnGround(true);
        var rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.startRiding(boat, true);
        boat.positionRider(rider);
        boat.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.6, -0.04, 0));
        helper.assertTrue(boat.getY() == pos.getY(), "Boat pushed its passenger into a low ceiling");
        helper.succeed();
    }
}
