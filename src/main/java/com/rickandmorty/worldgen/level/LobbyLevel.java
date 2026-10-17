package com.rickandmorty.worldgen.level;

import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * The Sunlit Lobby: long, quiet halls with a wall of tall windows down one side and golden
 * light pouring through them, laying the shapes of the window panes across the tiled floor.
 * Metal pillars, a plastic chair, a fire hose cabinet. No one here. The red doors in the far
 * wall lead home. (The one safe level: your mind recovers here.)
 * <pre>
 *  band of 22 along z:  0-1 wall | 2-15 hall | 16 windows | 17-19 sunlight | 20-21 wall
 *  y 13 bedrock   y 12 ceiling   y 1 tile floor
 * </pre>
 */
final class LobbyLevel implements LevelGenerator {
    static final int BAND = 22;
    private static final int CROSS = 96;

    private final BlockState bedrock = of(Blocks.BEDROCK), tile = of(ModBlocks.LOBBY_TILE.get()), sun = of(ModBlocks.SUNLIT_TILE.get());
    private final BlockState wall = of(Blocks.GRAY_CONCRETE), ceiling = of(Blocks.GRAY_CONCRETE), glass = of(Blocks.GLASS), mullion = of(Blocks.BLACK_CONCRETE);
    private final BlockState light = of(Blocks.OCHRE_FROGLIGHT), pillar = of(Blocks.QUARTZ_PILLAR).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

    @Override
    public int height() {
        return 14;
    }

    @Override
    public int maxStandY() {
        return 2;
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[height()];
        c[0] = bedrock;
        c[13] = bedrock;
        c[12] = ceiling;
        c[1] = tile;
        int lz = Math.floorMod(z, BAND), lx = Math.floorMod(x, CROSS);
        int band = Math.floorDiv(z, BAND);

        // cross corridors join the halls every so often
        if (lx >= 44 && lx <= 50 && (lz <= 1 || lz >= 16)) {
            if (lx == 44 || lx == 50) LevelParts.fill(c, 2, 11, wall);
            return c;
        }
        if (lz <= 1 || lz >= 20) {
            LevelParts.fill(c, 1, 11, wall);
            // the red exit doors, set into the dark wall
            if (lz == 1 && Math.floorMod(x, 48) == 20 && unit(seed, Math.floorDiv(x, 48), band, 90) < 0.8) {
                c[1] = tile;
                LevelParts.exitDoor(c, 2, Direction.SOUTH);
            }
            return c;
        }
        if (lz == 16) {
            // the window wall: tall panes between black mullions
            for (int y = 1; y <= 11; y++) c[y] = Math.floorMod(x, 6) == 0 || y == 1 || y == 7 ? mullion : glass;
            return c;
        }
        if (lz >= 17) {
            LevelParts.fill(c, 1, 11, light);            // outside: nothing but sunlight
            return c;
        }
        // the hall: sunlight falling through the panes onto the floor, at a slant
        if (lz >= 8) {
            int shift = (16 - lz) / 2;
            boolean paneShadow = Math.floorMod(x + shift, 6) == 0 || lz == 12;
            if (!paneShadow) c[1] = sun;
        }
        // pillars along the window side
        if (lz == 13 && Math.floorMod(x, 12) == 6) {
            LevelParts.fill(c, 2, 11, pillar);
            c[1] = tile;
        }
        // against the back wall: a plastic chair, a fire hose cabinet
        if (lz == 2) {
            int m = Math.floorMod(x, 48);
            if (m == 12) c[2] = of(Blocks.SMOOTH_QUARTZ_STAIRS).setValue(StairBlock.FACING, Direction.NORTH);
            if (m == 8) c[3] = of(Blocks.RED_CONCRETE);
            if (m == 9) c[3] = of(Blocks.RED_CANDLE);
            if (m >= 30 && m <= 33 && unit(seed, Math.floorDiv(x, 48), band, 91) < 0.4) c[2] = of(Blocks.POTTED_FERN);
        }
        if (lz == 15 && Math.floorMod(x, 24) >= 3 && Math.floorMod(x, 24) <= 4 && unit(seed, Math.floorDiv(x, 24), band, 92) < 0.5) {
            LevelParts.fill(c, 2, 11, of(Blocks.MOSS_BLOCK));    // a wall of creepers growing up a column
            c[2] = of(Blocks.MOSS_BLOCK);
        }
        return c;
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        // the way home; or, one door in four, somewhere softer still
        return GenMath.unit(seed, pos.getX(), pos.getZ(), 1030) < 0.25 ? BackroomsLevel.FROOPYLAND : null;
    }
}
