package com.rickandmorty.gadget;

import com.rickandmorty.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Nothing stored: it's here so the Shrink Ray can be drawn at its full size (see ShrinkRayRenderer). */
public class ShrinkRayBlockEntity extends BlockEntity {
    public ShrinkRayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHRINK_RAY.get(), pos, state);
    }
}
