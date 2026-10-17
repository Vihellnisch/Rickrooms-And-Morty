package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.block.entity.ExitDoorBlockEntity;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The glass entrance doors of a perfectly normal store in the Overworld. Like an exit door, it
 * doesn't open onto what's behind it: walk in and you're somewhere in the Infinite IKEA.
 */
public class StoreDoorBlock extends ExitDoorBlock {
    public static final MapCodec<StoreDoorBlock> CODEC = simpleCodec(StoreDoorBlock::new);

    public StoreDoorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends DoorBlock> codec() {
        return CODEC;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ExitDoorBlockEntity door) {
            door.setDestination(BackroomsLevel.isBackrooms(level.dimension()) ? null : BackroomsLevel.INFINITE_IKEA);
        }
    }
}
