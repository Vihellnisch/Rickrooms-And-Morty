package com.rickandmorty.worldgen.level;

import com.rickandmorty.block.FluorescentLightBlock;
import com.rickandmorty.block.LevelBlocks;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Shared pieces for the level generators. */
final class LevelParts {
    private LevelParts() {
    }

    static BlockState of(Block block) {
        return block.defaultBlockState();
    }

    /** An exit door standing on {@code y - 1}, facing {@code facing}. */
    static void exitDoor(BlockState[] column, int y, Direction facing) {
        BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, facing);
        column[y] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        column[y + 1] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
    }

    static BlockState fluorescent(boolean broken) {
        BlockState light = ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState();
        return broken ? FluorescentLightBlock.broken(light) : light;
    }

    /** A cable hanging down from {@code top} (just under the ceiling), {@code length} blocks long. */
    static void cable(BlockState[] column, int top, int length) {
        for (int i = 0; i < length; i++) {
            column[top - i] = ModBlocks.HANGING_CABLE.get().defaultBlockState().setValue(LevelBlocks.HangingCable.TIP, i == length - 1);
        }
    }

    static void fill(BlockState[] column, int from, int to, BlockState state) {
        for (int y = Math.max(0, from); y <= to && y < column.length; y++) column[y] = state;
    }
}
