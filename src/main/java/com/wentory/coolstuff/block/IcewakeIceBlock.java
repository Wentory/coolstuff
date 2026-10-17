package com.wentory.coolstuff.block;

import com.wentory.coolstuff.server.BoatEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class IcewakeIceBlock extends Block {
    public IcewakeIceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Only retained to clean up ice placed by earlier development builds.
        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Boat boat && BoatEnchantments.activeIcewake(boat)
                && boat.getBoundingBox().minY < pos.getY() + 0.999) {
            // A delayed block update must not trap a hull that has already dipped into the water.
            // Icewake lifts it onto the top again on the next tick.
            return Shapes.empty();
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Lifetime is governed by scheduled ticks, including across chunk saves.
    }
}
