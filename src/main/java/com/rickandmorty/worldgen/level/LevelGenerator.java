package com.rickandmorty.worldgen.level;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One Backrooms level's terrain, as a pure function of (seed, x, z) like Level 0's: the chunk
 * generator, random landings, mob spawns and the tests all read the same columns.
 */
public interface LevelGenerator {
    /** How tall the columns are (never more than the dimension's height). */
    int height();

    /** Every block of the column at (x, z), from y = 0 up; null means air. */
    BlockState[] column(long seed, int x, int z);

    /** The loot for a container the columns placed at {@code pos}, or null to leave it empty. */
    default @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return null;
    }

    /** Where an exit door at {@code pos} leads (null: back to the Overworld). */
    default @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        return BackroomsLevel.LEVEL_0;
    }

    /** A block entity the columns placed (after its loot and exit door are set up): for signs and the like. */
    default void blockEntity(WorldGenRegion region, long seed, BlockPos pos, BlockState state) {
    }

    /** Creatures placed when a chunk is generated (region is centred on the new chunk). */
    default void populate(WorldGenRegion region, long seed) {
    }

    /** Lowest and highest y where anyone can stand in this level. */
    default int minStandY() {
        return 1;
    }

    default int maxStandY() {
        return height() - 3;
    }

    /** Feet position at (x, z) closest to {@code nearY}: solid underfoot, two air blocks above. Null if none. */
    default @Nullable Integer standY(long seed, int x, int z, int nearY) {
        BlockState[] column = column(seed, x, z);
        Integer best = null;
        for (int y = Math.max(1, minStandY()); y <= Math.min(maxStandY(), column.length - 2); y++) {
            if (!solid(column[y - 1]) || !clear(column[y]) || !clear(column[y + 1])) continue;
            if (best == null || Math.abs(y - nearY) < Math.abs(best - nearY)) best = y;
        }
        return best;
    }

    /** A random spot to arrive at: somewhere to stand, far from everything, different every time. */
    default Vec3 landing(long seed, RandomSource random) {
        for (int attempt = 0; attempt < 200; attempt++) {
            int x = random.nextIntBetweenInclusive(-3000, 3000), z = random.nextIntBetweenInclusive(-3000, 3000);
            Integer y = standY(seed, x, z, random.nextInt(Math.max(1, maxStandY())));
            if (y != null && clearAround(seed, x, y, z)) return new Vec3(x + 0.5, y, z + 0.5);
        }
        Integer y = standY(seed, 0, 0, 2);
        return new Vec3(0.5, y == null ? 2 : y, 0.5);
    }

    /** The four neighbours are walkable too (so a landing isn't wedged in a gap). */
    private boolean clearAround(long seed, int x, int y, int z) {
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            BlockState[] column = column(seed, x + d[0], z + d[1]);
            if (y + 1 >= column.length || !clear(column[y]) || !clear(column[y + 1])) return false;
        }
        return true;
    }

    static boolean solid(@Nullable BlockState state) {
        return state != null && !state.isAir() && state.blocksMotion() && state.getFluidState().isEmpty();
    }

    static boolean clear(@Nullable BlockState state) {
        return state == null || (!state.blocksMotion() && state.getFluidState().isEmpty());
    }
}
