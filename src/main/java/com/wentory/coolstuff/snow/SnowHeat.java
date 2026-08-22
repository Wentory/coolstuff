package com.wentory.coolstuff.snow;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class SnowHeat {
    private static final int RADIUS = 4;

    private SnowHeat() {
    }

    public static int meltMultiplier(Level level, BlockPos center) {
        int multiplier = 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    cursor.setWithOffset(center, x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(Blocks.CAMPFIRE) && isLit(state)) return 5;
                    if ((state.is(Blocks.SMOKER) || state.is(Blocks.BLAST_FURNACE)) && isLit(state)) {
                        multiplier = Math.max(multiplier, 4);
                    } else if (state.is(Blocks.FURNACE) && isLit(state)) {
                        multiplier = Math.max(multiplier, 3);
                    } else if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                        multiplier = Math.max(multiplier, 2);
                    }
                }
            }
        }
        return multiplier;
    }

    private static boolean isLit(BlockState state) {
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }
}
