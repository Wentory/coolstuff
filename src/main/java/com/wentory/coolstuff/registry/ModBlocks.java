package com.wentory.coolstuff.registry;

import com.wentory.coolstuff.Coolstuff;
import com.wentory.coolstuff.block.IcewakeIceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Coolstuff.MODID);
    public static final DeferredBlock<Block> ICEWAKE_ICE = BLOCKS.register("icewake_ice",
            () -> new IcewakeIceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FROSTED_ICE).noLootTable()));

    private ModBlocks() {}
}
