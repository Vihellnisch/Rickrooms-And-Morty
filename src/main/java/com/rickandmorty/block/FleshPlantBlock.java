package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Things that grow on flesh instead of dirt: sinew tufts, and eyes on stalks. */
public class FleshPlantBlock extends BushBlock {
    public static final MapCodec<FleshPlantBlock> CODEC = simpleCodec(p -> new FleshPlantBlock(p, false));
    private static final VoxelShape LOW = Block.box(2, 0, 2, 14, 10, 14);
    private static final VoxelShape TALL = Block.box(5, 0, 5, 11, 15, 11);
    private final boolean tall;

    public FleshPlantBlock(Properties properties, boolean tall) {
        super(properties);
        this.tall = tall;
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.FLESH_GROUND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return tall ? TALL : LOW;
    }
}
