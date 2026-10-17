package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * Level 4, the Abandoned Office. Suites of 24x24 blocks, each one of:
 * <ul>
 *   <li>open plan: everything stripped out, cables dangling from the ceiling over debris;</li>
 *   <li>offices: a grid of small rooms with desks, chairs and filing cabinets;</li>
 *   <li>a dead department store: tile walkways through grey carpet, white pillars, empty shelves.</li>
 * </ul>
 * <pre>
 *  y 6  bedrock      y 5  drop ceiling, panel lights      y 2-4  rooms      y 1  carpet
 * </pre>
 */
final class OfficeLevel implements LevelGenerator {
    private static final int SUITE = 24;
    private static final ResourceKey<LootTable> FILES = ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/level_4_office"));

    private enum Suite { OPEN_PLAN, OFFICES, STORE }

    private final BlockState bedrock = of(Blocks.BEDROCK), carpet = of(ModBlocks.OFFICE_CARPET.get()), wall = of(ModBlocks.OFFICE_WALL.get());
    private final BlockState ceiling = of(ModBlocks.BACKROOMS_CEILING.get()), light = of(ModBlocks.PANEL_LIGHT.get());
    private final BlockState debris = of(ModBlocks.OFFICE_DEBRIS.get()), tile = of(Blocks.SMOOTH_QUARTZ), pillar = of(Blocks.QUARTZ_BLOCK);
    private final BlockState shelf = of(Blocks.IRON_BARS), shelfTop = of(Blocks.BIRCH_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
    private final BlockState desk = of(Blocks.BIRCH_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);

    @Override
    public int height() {
        return 7;
    }

    @Override
    public int maxStandY() {
        return 2;
    }

    private Suite suite(long seed, int sx, int sz) {
        double u = unit(seed, sx, sz, 40);
        return u < 0.4 ? Suite.OPEN_PLAN : u < 0.75 ? Suite.OFFICES : Suite.STORE;
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[height()];
        c[0] = bedrock;
        c[6] = bedrock;
        c[5] = ceiling;
        c[1] = carpet;
        int sx = Math.floorDiv(x, SUITE), sz = Math.floorDiv(z, SUITE);
        int lx = Math.floorMod(x, SUITE), lz = Math.floorMod(z, SUITE);
        Suite suite = suite(seed, sx, sz);

        // suite walls, two doorways in each (and now and then an exit door)
        if (lx == 0 || lz == 0) {
            int along = lx == 0 ? lz : lx;
            boolean doorway = (along >= 5 && along <= 6) || (along >= 16 && along <= 17);
            if (!doorway) {
                LevelParts.fill(c, 2, 4, wall);
                if (along == 11 && unit(seed, sx, sz, lx == 0 ? 41 : 42) < 0.18) {
                    LevelParts.exitDoor(c, 2, lx == 0 ? Direction.EAST : Direction.SOUTH);
                }
            }
            return c;
        }
        if (lx % 6 == 3 && lz % 6 == 3) c[5] = light;
        switch (suite) {
            case OPEN_PLAN -> openPlan(seed, x, z, c);
            case OFFICES -> offices(seed, x, z, lx, lz, c);
            case STORE -> store(seed, x, z, lx, lz, c);
        }
        return c;
    }

    /** Stripped bare: only the cables are left, hanging in rows where the desks were. */
    private void openPlan(long seed, int x, int z, BlockState[] c) {
        if (Math.floorMod(x, 3) == 1 && Math.floorMod(z, 4) == 1 && c[5] != light && unit(seed, x, z, 43) < 0.7) {
            LevelParts.cable(c, 4, 1 + (int) (unit(seed, x, z, 44) * 2.4));
        }
        if (unit(seed, x, z, 45) < 0.14 && c[2] == null) c[2] = debris;
        if (unit(seed, x, z, 46) < 0.01) c[2] = of(Blocks.WHITE_CARPET);       // dust sheets
    }

    /** Small offices on a 6-grid: a desk, a chair, sometimes a filing cabinet. */
    private void offices(long seed, int x, int z, int lx, int lz, BlockState[] c) {
        int ox = lx % 6, oz = lz % 6;
        if (ox == 0 || oz == 0) {
            boolean doorway = (ox == 0 && oz == 3) || (oz == 0 && ox == 3);
            if (!doorway) LevelParts.fill(c, 2, 4, wall);
            return;
        }
        int ix = lx / 6, iz = lz / 6;
        long h = hash(seed, Math.floorDiv(x, 6), Math.floorDiv(z, 6), 47);
        if (ox == 2 && (oz == 2 || oz == 3)) c[2] = desk;
        if (ox == 1 && oz == 2) c[2] = of(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.WEST);
        if (ox == 5 && oz == 5 && unit(h) < 0.35) c[2] = of(Blocks.BARREL).setValue(BarrelBlock.FACING, Direction.NORTH);
        if (ox == 4 && oz == 4 && unit(Long.rotateLeft(h, 9)) < 0.4) LevelParts.cable(c, 4, 1);
        if (unit(seed, x, z, 48) < 0.06 && c[2] == null) c[2] = debris;
        if ((ix + iz) % 5 == 0 && unit(Long.rotateLeft(h, 20)) < 0.5) c[5] = c[5] == light ? LevelParts.fluorescent(true) : c[5];   // dead lights
    }

    /** A department store gone out of business: walkways of white tile, pillars, bare shelving round the walls. */
    private void store(long seed, int x, int z, int lx, int lz, BlockState[] c) {
        boolean ring = (lx >= 4 && lx <= 6) || (lx >= 17 && lx <= 19) || (lz >= 4 && lz <= 6) || (lz >= 17 && lz <= 19);
        boolean inside = lx >= 4 && lx <= 19 && lz >= 4 && lz <= 19;
        if (inside && ring) c[1] = tile;
        if (lx % 8 == 4 && lz % 8 == 4 && !(lx == 12 && lz == 12)) LevelParts.fill(c, 2, 4, pillar);
        if (lx == 1 || lz == 1 || lx == 23 || lz == 23) {
            c[2] = shelf;
            c[3] = shelfTop;
        }
        if (lx % 4 == 2 && lz % 4 == 2) c[5] = light;
        if (unit(seed, x, z, 49) < 0.02 && c[2] == null) c[2] = debris;
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return FILES;
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        double roll = unit(seed, pos.getX(), pos.getZ(), 50);
        // the offices were a department store once: some doors lead to one that never ends
        return roll < 0.45 ? BackroomsLevel.LEVEL_188 : roll < 0.7 ? BackroomsLevel.HARVEST : roll < 0.85 ? BackroomsLevel.LEVEL_FUN
                : BackroomsLevel.INFINITE_IKEA;
    }
}
