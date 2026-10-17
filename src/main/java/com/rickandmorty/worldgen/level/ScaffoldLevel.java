package com.rickandmorty.worldgen.level;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * The Scaffold Void: pitch black in every direction, and hanging in it a lattice of wooden
 * platforms joined by narrow walkways, ladders between the layers, little lantern-lit stalls,
 * and trunks as thick as towers vanishing up and down into the dark. There is no floor.
 * <pre>
 *  nodes every 16 blocks across and 14 up, from y 8; a platform is 5x5 around its node
 * </pre>
 */
final class ScaffoldLevel implements LevelGenerator {
    static final int CELL = 16, LAYER = 14, FIRST = 8, LAYERS = 9;

    private final BlockState plank = of(Blocks.OAK_PLANKS), fence = of(Blocks.OAK_FENCE), torch = of(Blocks.TORCH);
    private final BlockState trunk = of(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    private final BlockState support = of(Blocks.SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    private final BlockState roof = of(Blocks.OAK_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);

    @Override
    public int height() {
        return FIRST + LAYERS * LAYER + 4;
    }

    static int layerY(int k) {
        return FIRST + k * LAYER;
    }

    static boolean node(long seed, int i, int j, int k) {
        return k >= 0 && k < LAYERS && unit(hash(seed, i, j, k, 110)) < 0.5;
    }

    private static boolean link(long seed, int i, int j, int k, int dir) {
        return unit(hash(seed, i, j, k, 111 + dir)) < 0.7;
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[height()];
        int i = Math.floorDiv(x, CELL), j = Math.floorDiv(z, CELL);
        int lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        // the huge trunks, top to bottom
        if (lx <= 1 && lz <= 1 && unit(hash(seed, i, j, 112)) < 0.25) {
            LevelParts.fill(c, 0, c.length - 1, trunk);
            return c;
        }
        for (int k = 0; k < LAYERS; k++) {
            int y = layerY(k);
            boolean here = node(seed, i, j, k);
            boolean onPlatform = here && lx >= 6 && lx <= 10 && lz >= 6 && lz <= 10;
            if (onPlatform) {
                platform(c, seed, i, j, k, lx, lz, y);
                continue;
            }
            // walkways, 2 wide, to the next node east and south
            boolean eastWalk = (lz == 8 || lz == 9) && (lx >= 11 ? here && node(seed, i + 1, j, k) && link(seed, i, j, k, 0)
                    : lx <= 5 && node(seed, i - 1, j, k) && here && link(seed, i - 1, j, k, 0));
            boolean southWalk = (lx == 8 || lx == 9) && (lz >= 11 ? here && node(seed, i, j + 1, k) && link(seed, i, j, k, 1)
                    : lz <= 5 && node(seed, i, j - 1, k) && here && link(seed, i, j - 1, k, 1));
            if (eastWalk || southWalk) {
                c[y] = plank;
                if (unit(hash(seed, x, z, k, 113)) < 0.08) c[y + 1] = fence;
            }
            // a ladder beside the platform, up to the one above
            if (here && node(seed, i, j, k + 1) && unit(hash(seed, i, j, k, 114)) < 0.55) {
                if (lx == 10 && lz == 11) LevelParts.fill(c, y, y + LAYER, support);
            }
        }
        return c;
    }

    private void platform(BlockState[] c, long seed, int i, int j, int k, int lx, int lz, int y) {
        c[y] = plank;
        boolean ladderUp = node(seed, i, j, k + 1) && unit(hash(seed, i, j, k, 114)) < 0.55;
        boolean ladderDown = node(seed, i, j, k - 1) && unit(hash(seed, i, j, k - 1, 114)) < 0.55;
        if (lx == 10 && lz == 10) {
            if (ladderDown) c[y] = null;                        // the hole the ladder from below comes up through
            if (ladderUp) {
                for (int yy = y + 1; yy < y + LAYER; yy++) c[yy] = of(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH);
            } else if (ladderDown) {
                c[y] = of(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH);
            }
            return;
        }
        boolean corner = (lx == 6 || lx == 10) && (lz == 6 || lz == 10);
        if (corner && unit(hash(seed, i, j, k, 115)) < 0.6) {
            c[y + 1] = fence;
            c[y + 2] = torch;
        }
        // a little stall: four posts and a roof, a lantern hanging inside
        if (unit(hash(seed, i, j, k, 116)) < 0.35 && lx >= 6 && lx <= 8 && lz >= 6 && lz <= 8) {
            boolean postHere = (lx == 6 || lx == 8) && (lz == 6 || lz == 8);
            if (postHere) LevelParts.fill(c, y + 1, y + 3, fence);
            if (lx == 7 && lz == 7) c[y + 3] = of(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
            c[y + 4] = roof;
        }
    }

    @Override
    public int minStandY() {
        return FIRST + 1;
    }
}
