package com.wentory.coolstuff.server;

import com.wentory.coolstuff.config.CoolstuffConfig;
import com.wentory.coolstuff.entity.EnchantedBoatAccess;
import com.wentory.coolstuff.registry.ModBlocks;
import com.wentory.coolstuff.registry.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.TreeSet;

public final class BoatEnchantments {
    private BoatEnchantments() {}

    public static boolean has(Boat boat, ResourceKey<Enchantment> key) {
        ItemStack stack = ((EnchantedBoatAccess) boat).coolstuff$getBoatStack();
        if (stack.isEmpty()) return false;
        return EnchantmentHelper.getItemEnchantmentLevel(
                boat.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), stack) > 0;
    }

    public static void copyEnchantments(Boat boat, ItemStack target) {
        ItemStack source = ((EnchantedBoatAccess) boat).coolstuff$getBoatStack();
        if (source.has(DataComponents.ENCHANTMENTS)) {
            target.set(DataComponents.ENCHANTMENTS, source.get(DataComponents.ENCHANTMENTS));
        }
    }

    public static boolean activeIcewake(Boat boat) {
        return !boat.isRemoved() && boat.getControllingPassenger() instanceof Player
                && CoolstuffConfig.ENABLE_ICEWAKE.get() && has(boat, ModEnchantments.ICEWAKE);
    }

    public static double supportHeight(Boat boat) {
        if (!activeIcewake(boat) || boat.isUnderWater()) return Double.NaN;
        AABB hull = boat.getBoundingBox().deflate(0.05);
        for (int y = Mth.floor(boat.getY() + 0.1); y >= Mth.floor(boat.getY()) - 1; y--) {
            double top = y + 1.015;
            if (top - boat.getY() > 0.8 || boat.getY() - top > 0.25) continue;
            for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(hull.minX), y, Mth.floor(hull.minZ),
                    Mth.floor(hull.maxX), y, Mth.floor(hull.maxZ))) {
                if (surface(boat, pos) && clearAbove(boat, Math.max(0, top - boat.getY()), 0, 0)) return top;
            }
        }
        return Double.NaN;
    }

    public static void tick(Boat boat) {
        double top = supportHeight(boat);
        if (Double.isNaN(top)) return;
        boat.setPos(boat.getX(), top, boat.getZ());
        boat.setOnGround(true);
        boat.fallDistance = 0;
        boat.setDeltaMovement(boat.getDeltaMovement().multiply(1, 0, 1));
    }

    private static boolean surface(Boat boat, BlockPos pos) {
        BlockState state = boat.level().getBlockState(pos);
        return (state.is(Blocks.WATER) && state.getFluidState().isSource() || state.is(ModBlocks.ICEWAKE_ICE.get()))
                && boat.level().getBlockState(pos.above()).isAir();
    }

    public static Vec3 stepMotion(Boat boat, Vec3 motion) {
        if (!CoolstuffConfig.ENABLE_CLAMBER.get() || !has(boat, ModEnchantments.CLAMBER)
                || !(boat.getControllingPassenger() instanceof Player) || motion.horizontalDistanceSqr() < 0.00001
                || motion.y > 0.1 || !(boat.onGround() || boat.isInWater() || boat.horizontalCollision)) return motion;
        AABB box = boat.getBoundingBox().deflate(0.001);
        if (boat.level().noCollision(boat, box.move(motion.x, 0.0, motion.z))) return motion;
        double waterLift = 0.0;
        if (boat.isInWater()) {
            BlockPos water = BlockPos.containing(boat.getX(), boat.getY(), boat.getZ());
            if (boat.level().getBlockState(water).is(Blocks.WATER)
                    && boat.level().getBlockState(water.above()).isAir()) {
                waterLift = Math.max(0.0, water.getY() + 1.0 - boat.getY());
            }
        }
        double maxLift = 1.001 + waterLift;
        TreeSet<Double> heights = new TreeSet<>();
        for (var shape : boat.level().getBlockCollisions(boat, box.expandTowards(motion.x, maxLift, motion.z))) {
            if (!shape.isEmpty()) {
                double height = shape.bounds().maxY - boat.getY();
                if (height > 0.01 && height <= maxLift) heights.add(height + 0.001);
            }
        }
        for (double height : heights) {
            if (clearAbove(boat, height, motion.x, motion.z)
                    && !boat.level().noCollision(boat, box.move(motion.x, height - 0.02, motion.z))) {
                boat.setDeltaMovement(boat.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                boat.fallDistance = 0.0F;
                // Let vanilla resolve the upward leg before horizontal movement.
                // Teleporting first would apply the old negative Y delta at the
                // foot of the ledge and drop the hull back into its side.
                return new Vec3(motion.x, height, motion.z);
            }
        }
        return motion;
    }

    private static boolean clearAbove(Boat boat, double lift, double x, double z) {
        AABB hull = boat.getBoundingBox().deflate(0.001);
        if (!boat.level().noCollision(boat, hull.expandTowards(0.0, lift, 0.0))
                || !boat.level().noCollision(boat, hull.move(x, lift, z))) return false;
        for (Entity passenger : boat.getPassengers()) {
            AABB headroom = passenger.getBoundingBox().expandTowards(0.0, lift, 0.0)
                    .expandTowards(x, 0.0, z).deflate(0.001);
            // Seated passengers extend below the hull. Only the space above the
            // raised hull must be clear; testing their feet would reject the ice itself.
            headroom = new AABB(headroom.minX, Math.max(headroom.minY, boat.getY() + lift), headroom.minZ,
                    headroom.maxX, Math.max(headroom.maxY, boat.getY() + lift + 0.001), headroom.maxZ);
            if (boat.level().getBlockCollisions(passenger, headroom).iterator().hasNext()) return false;
        }
        return true;
    }
}
