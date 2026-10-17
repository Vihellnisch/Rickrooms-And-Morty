package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * Level 1, the Habitable Zone: an endless underground car park. Low concrete ceilings, a grid of
 * pillars, pipes, flickering tube lights, puddles and tyre tracks on the wet floor, partition
 * walls of concrete and corrugated metal, and supply crates left by whoever was here before.
 * <pre>
 *  y 8  bedrock
 *  y 7  concrete ceiling, lights
 *  y 6  pipes
 *  y 2-6 open
 *  y 1  wet concrete floor (puddles)
 *  y 0  bedrock
 * </pre>
 */
final class GarageLevel implements LevelGenerator {
    private static final int GRID = 10;
    private static final int WALL_GRID = 40;
    private static final ResourceKey<LootTable> SUPPLIES = ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/level_1_supplies"));

    private final BlockState bedrock = of(Blocks.BEDROCK), floor = of(ModBlocks.GARAGE_CONCRETE.get()), track = of(Blocks.GRAY_CONCRETE);
    private final BlockState ceiling = of(Blocks.LIGHT_GRAY_CONCRETE), pillar = of(Blocks.LIGHT_GRAY_CONCRETE), stripe = of(Blocks.YELLOW_CONCRETE);
    private final BlockState wall = of(Blocks.GRAY_CONCRETE), metal = of(ModBlocks.CORRUGATED_METAL.get()), water = of(Blocks.WATER);
    private final BlockState pipe = ModBlocks.PIPE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);

    @Override
    public int height() {
        return 9;
    }

    @Override
    public int maxStandY() {
        return 2;
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[height()];
        c[0] = bedrock;
        c[8] = bedrock;
        c[7] = ceiling;
        int lx = Math.floorMod(x, GRID), lz = Math.floorMod(z, GRID);
        boolean puddle = noise(seed, x, z, 7, 1) > 0.7 && noise(seed, x, z, 3, 2) > 0.45;
        c[1] = puddle ? water : (lx == 3 || lx == 7) && GenMath.unit(seed, x, z, 3) < 0.85 ? track : floor;

        // pillars: 2x2 on the grid, with a yellow band at the bottom
        if (lx <= 1 && lz <= 1) {
            c[1] = floor;
            LevelParts.fill(c, 2, 6, pillar);
            c[2] = stripe;
            return c;
        }
        // partition walls on a coarse grid, with openings
        if (wall(seed, x, z, c)) return c;

        // tube lights in strips, whole regions of them dead
        boolean dark = unit(seed, Math.floorDiv(x, 60), Math.floorDiv(z, 60), 9) < 0.25;
        if (lx == 5 && lz >= 3 && lz <= 6) c[7] = LevelParts.fluorescent(dark || unit(seed, x / GRID, z / GRID, 10) < 0.15);
        if (lz == 8 && unit(seed, Math.floorDiv(x, 32), Math.floorDiv(z, GRID), 11) < 0.7) c[6] = pipe;

        // supply crates against the pillars
        if (lx == 2 && lz == 0 && unit(seed, Math.floorDiv(x, GRID), Math.floorDiv(z, GRID), 12) < 0.1) {
            c[1] = floor;
            c[2] = of(Blocks.BARREL).setValue(BarrelBlock.FACING, Direction.EAST);
        }
        return c;
    }

    /** Walls along x = 40k or z = 40k: concrete (with the odd exit door) or corrugated metal, always with gaps. */
    private boolean wall(long seed, int x, int z, BlockState[] c) {
        for (int axis = 0; axis < 2; axis++) {
            int across = axis == 0 ? x : z, along = axis == 0 ? z : x;
            if (Math.floorMod(across, WALL_GRID) != 5) continue;
            int segment = Math.floorDiv(along, WALL_GRID);
            long h = hash(seed, Math.floorDiv(across, WALL_GRID), segment, 20 + axis);
            double kind = unit(h);
            if (kind < 0.35) continue;                       // no wall here
            int local = Math.floorMod(along, WALL_GRID);
            if (local >= 17 && local <= 20) continue;         // always a way through
            boolean concrete = kind < 0.7;
            c[1] = floor;
            LevelParts.fill(c, 2, 6, concrete ? wall : metal);
            if (concrete && local == 30 && unit(Long.rotateLeft(h, 7)) < 0.6) {
                LevelParts.exitDoor(c, 2, axis == 0 ? Direction.EAST : Direction.SOUTH);
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return SUPPLIES;
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        // most lead on to the offices; about a third, to a party
        double roll = GenMath.unit(seed, pos.getX(), pos.getZ(), 33);
        return roll < 0.1 ? BackroomsLevel.RUN_FOR_YOUR_LIFE : roll < 0.4 ? BackroomsLevel.LEVEL_FUN : BackroomsLevel.LEVEL_4;
    }
}
