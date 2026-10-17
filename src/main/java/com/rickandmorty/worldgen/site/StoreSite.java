package com.rickandmorty.worldgen.site;

import com.rickandmorty.block.StoreBlocks;
import com.rickandmorty.block.entity.ExitDoorBlockEntity;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * A perfectly normal, regular old IKEA, standing on its own in the Overworld: a big blue box
 * with yellow letters, a car park in front and a few showroom pieces just inside. The glass
 * doors don't open into the store you can see, though. They open into the one that never ends.
 */
final class StoreSite {
    static final int X0 = -15, X1 = 15, Z0 = -10, Z1 = 10, TOP = 10;
    /** 3x5 letters, top row first. */
    private static final String[][] LETTERS = {
            {"###", ".#.", ".#.", ".#.", "###"},
            {"#.#", "##.", "#..", "##.", "#.#"},
            {"###", "#..", "##.", "#..", "###"},
            {".#.", "#.#", "###", "#.#", "#.#"}};

    private StoreSite() {
    }

    static void build(SiteBuilder b) {
        BlockState blue = Blocks.BLUE_CONCRETE.defaultBlockState(), yellow = Blocks.YELLOW_CONCRETE.defaultBlockState();
        BlockState floor = ModBlocks.STORE_WALKWAY.get().defaultBlockState();
        b.foundation(X0 - 1, Z0 - 7, X1 + 1, Z1 + 1, Blocks.GRAY_CONCRETE.defaultBlockState(), Blocks.STONE.defaultBlockState(), TOP + 2);

        // --- the car park, with its white lines
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            for (int z = Z0 - 7; z < Z0; z++) {
                boolean line = Math.floorMod(x, 4) == 0 && z <= Z0 - 3;
                b.set(x, 0, z, line ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState());
            }
        }

        // --- the big blue box
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                b.set(x, 0, z, floor);
                b.set(x, TOP + 1, z, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            }
        }
        b.walls(X0, 1, Z0, X1, Z1, TOP, () -> blue);
        for (int x = X0; x <= X1; x++) {                                   // a yellow band round the top
            b.set(x, TOP, Z0, yellow);
            b.set(x, TOP, Z1, yellow);
        }
        for (int z = Z0; z <= Z1; z++) {
            b.set(X0, TOP, z, yellow);
            b.set(X1, TOP, z, yellow);
        }

        // --- the letters over the entrance
        for (int i = 0; i < LETTERS.length; i++) {
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if (LETTERS[i][row].charAt(col) == '#') b.set(7 - i * 4 - col, 8 - row, Z0 - 1, yellow);       // read from outside, facing south
                }
            }
        }

        // --- the glass front, and the doors
        for (int x = -4; x <= 4; x++) {
            for (int y = 1; y <= 3; y++) b.set(x, y, Z0, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        }
        door(b, -1, DoorHingeSide.LEFT);
        door(b, 0, DoorHingeSide.RIGHT);
        for (int x = -2; x <= 1; x++) b.set(x, 4, Z0, yellow);

        // --- just inside: a couple of showroom corners and the first stock crates
        BlockState sofa = ModBlocks.SOFA.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        for (int x = -11; x <= -9; x++) {
            StoreBlocks.SofaPart part = x == -11 ? StoreBlocks.SofaPart.RIGHT : x == -9 ? StoreBlocks.SofaPart.LEFT : StoreBlocks.SofaPart.MIDDLE;
            b.set(x, 1, Z1 - 6, sofa.setValue(StoreBlocks.PART, part).setValue(StoreBlocks.COLOR, 1));
        }
        b.set(-10, 1, Z1 - 3, ModBlocks.COFFEE_TABLE.get());
        b.set(-13, 1, Z1 - 6, ModBlocks.FLOOR_LAMP.get());
        for (int x = 8; x <= 12; x++) {
            b.set(x, 1, Z1 - 1, ModBlocks.CUBE_SHELF.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            b.set(x, 2, Z1 - 1, ModBlocks.CUBE_SHELF.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        }
        crate(b, 13, 1, Z1 - 1);
        crate(b, 7, 1, Z1 - 1);
        for (int x = X0 + 3; x <= X1 - 3; x += 6) {
            b.set(x, TOP - 1, Z1 - 1, ModBlocks.STORE_LIGHT.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            b.set(x, TOP - 1, Z0 + 1, ModBlocks.STORE_LIGHT.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        }
        b.sign(3, 2, Z0 - 1, Direction.NORTH, "", "WELCOME!", "We're always open.", "");
    }

    private static void door(SiteBuilder b, int x, DoorHingeSide hinge) {
        BlockState door = ModBlocks.STORE_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HINGE, hinge);
        b.set(x, 1, Z0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        b.set(x, 2, Z0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        b.set(x, 3, Z0, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        BlockPos pos = b.at(x, 1, Z0);
        if (b.box.isInside(pos) && b.level.getBlockEntity(pos) instanceof ExitDoorBlockEntity exit) exit.setDestination(BackroomsLevel.INFINITE_IKEA);
    }

    private static void crate(SiteBuilder b, int x, int y, int z) {
        BlockPos pos = b.at(x, y, z);
        long seed = b.random.nextLong();
        if (!b.box.isInside(pos)) return;
        b.level.setBlock(pos, ModBlocks.STOCK_CRATE.get().defaultBlockState(), 2);
        RandomizableContainer.setBlockEntityLootTable(b.level, RandomSource.create(seed), pos, SiteBuilder.loot("ikea_market"));
    }
}
