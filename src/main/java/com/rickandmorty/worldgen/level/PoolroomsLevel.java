package com.rickandmorty.worldgen.level;

import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.fill;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * Level 37, the Poolrooms: white tile, still warm water, and nobody. A patchwork of 40-block
 * rooms, each its own kind and its own depth: ankle-deep halls of fat pillars, winding pools
 * between terraces, domed baths, an abyss you can't see the bottom of, an empty water park,
 * stairwells climbing round pillars, a lounge with a ladder up to the way out, misty flooded
 * halls with drowned armchairs, and narrow tiled tunnels.
 * <p>
 * The water always tops out at the same height ({@link #SURFACE}); every column is solid or
 * water up to that height, so none of it can ever run anywhere.
 */
public final class PoolroomsLevel implements LevelGenerator {
    public static final int HEIGHT = 96;
    /** The top of the water, and the top of the dry floors. */
    public static final int SURFACE = 28;
    public static final int CELL = 40;
    private static final int S = SURFACE;
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            com.rickandmorty.RickAndMortyMod.id("chests/poolrooms"));

    public enum Room { PILLARS, TERRACES, DOME, ABYSS, SLIDES, STAIRWELL, LOUNGE, MISTY, TUNNELS }

    private static final int[] WEIGHTS = {22, 12, 10, 7, 10, 11, 11, 11, 6};

    private final BlockState bedrock = of(Blocks.BEDROCK), water = of(Blocks.WATER);
    private final BlockState tile = of(ModBlocks.POOL_TILE.get()), floorTile = of(ModBlocks.POOL_FLOOR_TILE.get());
    private final BlockState light = of(ModBlocks.POOL_LIGHT.get()), drain = of(ModBlocks.POOL_DRAIN.get());
    private final BlockState bars = of(Blocks.IRON_BARS);

    /** What kind of room fills cell (cx, cz). */
    public static Room room(long seed, int cx, int cz) {
        int total = 0;
        for (int w : WEIGHTS) total += w;
        int roll = (int) Math.floorMod(hash(seed, cx, cz, 700), (long) total);
        for (int i = 0; i < WEIGHTS.length; i++) {
            roll -= WEIGHTS[i];
            if (roll < 0) return Room.values()[i];
        }
        return Room.PILLARS;
    }

    /** Rooms of these kinds open straight into a neighbour of the same kind, with no wall between. */
    private static boolean merges(Room room) {
        return room == Room.PILLARS || room == Room.MISTY || room == Room.ABYSS || room == Room.TUNNELS;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return LOOT;
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        // up out of the water, and now and then out into a field that never ends
        double roll = unit(seed, pos.getX(), pos.getZ(), 81);
        return roll < 0.15 ? BackroomsLevel.STAIRCASE : roll < 0.45 ? BackroomsLevel.ENDLESS_FIELD : BackroomsLevel.SUNLIT_LOBBY;
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        int lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        Room room = room(seed, cx, cz);
        if (lx == 0 && lz == 0) {
            fill(c, 1, HEIGHT - 2, tile);
        } else if (lx == 0 || lz == 0) {
            boolean alongX = lz == 0;
            Room other = alongX ? room(seed, cx, cz - 1) : room(seed, cx - 1, cz);
            if (other == room && merges(room)) interior(c, seed, room, cx, cz, Math.max(lx, 1), Math.max(lz, 1), x, z);
            else wall(c, seed, cx, cz, alongX, alongX ? lx : lz, room, other);
        } else {
            interior(c, seed, room, cx, cz, lx, lz, x, z);
            approach(c, seed, cx, cz, lx, lz);
        }
        // nothing at or below the water line is ever left open, so the water can't go anywhere
        for (int y = 1; y <= S; y++) {
            if (c[y] == null) c[y] = tile;
        }
        c[0] = bedrock;
        c[HEIGHT - 1] = bedrock;
        return c;
    }

    // ------------------------------------------------------------------ walls

    private boolean doorway(long seed, int cx, int cz, boolean alongX) {
        return unit(hash(seed, cx, cz, alongX ? 1 : 2, 701)) < 0.75;
    }

    private void wall(BlockState[] c, long seed, int cx, int cz, boolean alongX, int along, Room room, Room other) {
        fill(c, 1, HEIGHT - 2, tile);
        boolean abyss = room == Room.ABYSS || other == Room.ABYSS;
        boolean stairs = room == Room.STAIRWELL || other == Room.STAIRWELL;
        boolean misty = room == Room.MISTY || other == Room.MISTY;
        if (abyss) {
            // rows and rows of little lights up the walls
            for (int y = S + 3; y <= S + 31; y += 4) {
                if (Math.floorMod(along, 3) == 1) c[y] = light;
            }
        } else if (stairs) {
            // tall pill-shaped windows, glowing
            int w = Math.floorMod(along, 8) - 3;
            if (Math.abs(w) <= 1) {
                for (int base : new int[]{S + 2, S + 9}) {
                    for (int rel = 0; rel < 6; rel++) {
                        if ((rel == 0 || rel == 5) && w != 0) continue;
                        c[base + rel] = light;
                    }
                }
            }
        } else if (misty) {
            // big windows full of white light
            if (Math.floorMod(along, 5) != 0) fill(c, S + 1, S + 7, light);
        }
        if (along >= 18 && along <= 20 && doorway(seed, cx, cz, alongX)) {
            c[S - 1] = tile;
            c[S] = water;
            fill(c, S + 1, S + 3, null);
        }
    }

    /** Keeps the way from each doorway into the room open, whatever the room is. */
    private void approach(BlockState[] c, long seed, int cx, int cz, int lx, int lz) {
        boolean near = lx <= 4 && lz >= 17 && lz <= 21 && doorway(seed, cx, cz, false)
                || lz <= 4 && lx >= 17 && lx <= 21 && doorway(seed, cx, cz, true)
                || lx >= CELL - 4 && lz >= 17 && lz <= 21 && doorway(seed, cx + 1, cz, false)
                || lz >= CELL - 4 && lx >= 17 && lx <= 21 && doorway(seed, cx, cz + 1, true);
        if (!near) return;
        boolean blocked = false;
        for (int y = S + 1; y <= S + 3; y++) {
            if (c[y] != null && c[y] != water) blocked = true;
        }
        if (!blocked) return;
        c[S - 1] = tile;
        c[S] = water;
        fill(c, S + 1, S + 3, null);
        if (c[S + 4] == null) c[S + 4] = tile;
    }

    // ------------------------------------------------------------------ rooms

    /** Floor at floorY (tile under water is the teal pool kind), water up to the surface, ceiling at ceilY, solid above. */
    private void base(BlockState[] c, int floorY, int ceilY) {
        fill(c, 1, floorY - 1, tile);
        c[floorY] = floorY < S ? floorTile : tile;
        for (int y = floorY + 1; y <= S; y++) c[y] = water;
        for (int y = Math.max(floorY, S) + 1; y < ceilY; y++) c[y] = null;
        c[ceilY] = tile;
        fill(c, ceilY + 1, HEIGHT - 2, tile);
    }

    private void interior(BlockState[] c, long seed, Room room, int cx, int cz, int lx, int lz, int x, int z) {
        switch (room) {
            case PILLARS -> pillars(c, seed, cx, cz, lx, lz, x, z);
            case TERRACES -> terraces(c, seed, x, z);
            case DOME -> dome(c, lx, lz);
            case ABYSS -> abyss(c, lx, lz);
            case SLIDES -> slides(c, seed, cx, cz, lx, lz);
            case STAIRWELL -> stairwell(c, lx, lz);
            case LOUNGE -> lounge(c, lx, lz);
            case MISTY -> misty(c, seed, lx, lz, x, z);
            case TUNNELS -> tunnels(c, lx, lz);
        }
    }

    /** Endless flooded halls of fat round tiled pillars; each hall its own depth, height and spacing. */
    private void pillars(BlockState[] c, long seed, int cx, int cz, int lx, int lz, int x, int z) {
        long h = hash(seed, cx, cz, 710);
        double u = unit(h);
        int depth = u < 0.55 ? 1 : u < 0.75 ? 2 : u < 0.9 ? 4 : 7;
        int ceil = S + new int[]{5, 6, 7, 9}[(int) Math.floorMod(h >>> 8, 4L)];
        int p = new int[]{8, 10, 12}[(int) Math.floorMod(h >>> 12, 3L)];
        double r = ((h >>> 16) & 1) == 0 ? 1.6 : 2.6;
        base(c, S - depth, ceil);
        int px = Math.floorMod(x, p) - p / 2, pz = Math.floorMod(z, p) - p / 2;
        if (px * px + pz * pz <= r * r) {
            fill(c, S - depth + 1, ceil - 1, tile);
            return;
        }
        int qx = Math.floorMod(x, p), qz = Math.floorMod(z, p);
        if (qx <= 1 && qz <= 1 && unit(seed, Math.floorDiv(x, p), Math.floorDiv(z, p), 711) < 0.55) c[ceil] = light;   // skylights
        if (depth >= 2 && qx == 0 && pz == 0) c[S - depth] = light;                                                  // lights in the pool floor
    }

    /** A winding pool between terraces of dry tile, one step apart. */
    private void terraces(BlockState[] c, long seed, int x, int z) {
        double n = noise(seed, x, z, 11.0, 720);
        double band = Math.abs(n - 0.5);
        int ceil = S + 12;
        if (band < 0.07) base(c, S - 3, ceil);
        else if (band < 0.14) base(c, S - 1, ceil);
        else base(c, S + (band < 0.21 ? 0 : band < 0.28 ? 1 : 2), ceil);
        if (noise(seed, x, z, 6.0, 722) > 0.7) c[ceil] = light;
    }

    private static final int DOME_R = 17;

    private int domeCeiling(double d) {
        return S + 5 + (int) Math.round(Math.sqrt(Math.max(0, DOME_R * DOME_R - d * d)) * 0.95);
    }

    /** A round bath under a tiled dome, glowing alcoves all round it. */
    private void dome(BlockState[] c, int lx, int lz) {
        int dx = lx - 20, dz = lz - 20;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d <= DOME_R - 0.5) {
            int ceil = domeCeiling(d);
            if (d < 11) {
                base(c, S - 5, ceil);
                if (Math.abs(d - 6) < 0.6) c[S - 5] = light;
            } else {
                base(c, S, ceil);
                if (d < 12 && dz < 0 && Math.abs(dx) == 1) c[S + 1] = bars;      // the rails of the pool ladder
            }
            if (d < 2.5) c[ceil] = light;                                         // the oculus
            return;
        }
        double a = Math.atan2(dz, dx), sector = Math.PI / 4;
        double lateral = d * Math.sin(a - Math.round(a / sector) * sector);
        if (Math.abs(lateral) <= 1.6 && d <= DOME_R + 2.5) {
            base(c, S, S + 5);
            if (Math.abs(lateral) > 0.9) c[S + 4] = tile;                         // the round of the arch
            if (d > DOME_R + 1.5) fill(c, S + 1, Math.abs(lateral) > 0.9 ? S + 3 : S + 4, light);
            return;
        }
        fill(c, 1, HEIGHT - 2, tile);
    }

    private static final BlockState[] PLAY = {of(Blocks.RED_CONCRETE), of(Blocks.YELLOW_CONCRETE), of(Blocks.BLUE_CONCRETE), of(Blocks.LIME_CONCRETE)};

    /** A tiled hall around water too deep to see the bottom of. A play structure stands out in it; at the bottom, the drain. */
    private void abyss(BlockState[] c, int lx, int lz) {
        int ceil = S + 34;
        base(c, 4, ceil);
        if (lx % 6 == 3 && lz % 6 == 3) c[4] = light;
        if (lx % 5 == 2 && lz % 5 == 2) c[ceil] = light;
        if ((lx == 20 || lx == 21) && (lz == 20 || lz == 21)) c[4] = drain;
        int sx = lx - 28, sz = lz - 12;
        if (Math.abs(sx) == 2 && Math.abs(sz) == 2) fill(c, 5, S + 8, PLAY[(sx > 0 ? 1 : 0) + (sz > 0 ? 2 : 0)]);
        if (Math.abs(sx) <= 2 && Math.abs(sz) <= 2) {
            c[S + 5] = of(Blocks.LIGHT_BLUE_CONCRETE);
            if ((Math.abs(sx) == 2 || Math.abs(sz) == 2) && !(sx == 2 && sz == 0)) c[S + 6] = of(Blocks.WHITE_CONCRETE);
            c[S + 9] = of(Blocks.RED_CONCRETE);
            if (Math.abs(sx) <= 1 && Math.abs(sz) <= 1) c[S + 10] = of(Blocks.RED_CONCRETE);
            if (sx == 0 && sz == 0) c[S + 11] = of(Blocks.YELLOW_CONCRETE);
        }
        if (sz == 0 && sx >= 3 && sx <= 9) c[S + 5 - (sx - 2) * 5 / 7] = of(Blocks.YELLOW_CONCRETE);          // the slide down into it
    }

    private static final BlockState[] RAINBOW = {of(Blocks.RED_WOOL), of(Blocks.ORANGE_WOOL), of(Blocks.YELLOW_WOOL), of(Blocks.LIME_WOOL),
            of(Blocks.LIGHT_BLUE_WOOL), of(Blocks.BLUE_WOOL), of(Blocks.PURPLE_WOOL)};

    /** An empty water park: pastel slides off a tower and tubes out of the walls, or a rainbow of slides down one wall. */
    private void slides(BlockState[] c, long seed, int cx, int cz, int lx, int lz) {
        boolean rainbow = unit(hash(seed, cx, cz, 740)) < 0.4;
        int ceil = S + 10;
        base(c, S - 1, ceil);
        if (lz % 6 == 3 && lx % 4 != 0) c[ceil] = LevelParts.fluorescent(false);
        if ((lx % 12 == 5 || lx % 12 == 6) && (lz % 12 == 5 || lz % 12 == 6) && !(lx >= 16 && lx <= 24 && lz >= 16 && lz <= 30)) {
            fill(c, S, ceil - 1, tile);
            return;
        }
        if (rainbow) {
            if (lz >= 33 && lx >= 8 && lx <= 31) fill(c, S, S + 6, tile);                              // the ledge the slides start from
            if (lx >= 6 && lx <= 7 && lz >= 27 && lz <= 32) fill(c, S, S + 6 - (32 - lz), tile);        // steps up to it
            if (lx >= 12 && lx <= 25 && lz >= 21 && lz <= 32) c[S + 6 - (32 - lz) / 2] = RAINBOW[(lx - 12) / 2];
            if ((lx == 11 || lx == 26) && lz >= 21 && lz <= 32) c[S + 7 - (32 - lz) / 2] = of(Blocks.WHITE_CONCRETE);
            if (lx == 4) {                                                                               // arches along the side
                if (lz % 4 == 0) fill(c, S, S + 7, tile);
                else {
                    c[S + 7] = tile;
                    if (lz % 4 != 2) c[S + 6] = tile;
                }
            }
            return;
        }
        // the tower in the middle, and steps up to it
        if (lx >= 18 && lx <= 22 && lz >= 18 && lz <= 22) {
            if ((lx == 18 || lx == 22) && (lz == 18 || lz == 22)) fill(c, S, S + 6, tile);
            else c[S + 6] = tile;
        }
        if (lx >= 19 && lx <= 21 && lz >= 23 && lz <= 28) fill(c, S, S + 28 - lz, tile);
        // three slides down off it
        if (lz >= 19 && lz <= 20 && lx >= 6 && lx <= 17) c[S + 6 - (17 - lx) / 2] = of(Blocks.PINK_WOOL);
        if ((lz == 18 || lz == 21) && lx >= 6 && lx <= 17) c[S + 7 - (17 - lx) / 2] = of(Blocks.PINK_WOOL);
        if (lx >= 19 && lx <= 20 && lz >= 6 && lz <= 17) c[S + 6 - (17 - lz) / 2] = of(Blocks.LIGHT_BLUE_WOOL);
        if ((lx == 18 || lx == 21) && lz >= 6 && lz <= 17) c[S + 7 - (17 - lz) / 2] = of(Blocks.LIGHT_BLUE_WOOL);
        if (lz >= 20 && lz <= 21 && lx >= 23 && lx <= 34) c[S + 6 - (lx - 23) / 2] = of(Blocks.LIME_WOOL);
        if ((lz == 19 || lz == 22) && lx >= 23 && lx <= 34) c[S + 7 - (lx - 23) / 2] = of(Blocks.LIME_WOOL);
        // tube slides coming out of the north wall
        if (lz <= 2) {
            int[] mouths = {8, 14, 26, 32};
            for (int i = 0; i < mouths.length; i++) {
                for (int y = S; y <= S + 4; y++) {
                    double r = Math.hypot(lx - mouths[i], y - (S + 2));
                    if (r <= 1.5 && lz == 1) c[y] = of(Blocks.BLACK_CONCRETE);
                    else if (r > 1.5 && r <= 2.5) c[y] = i % 2 == 0 ? of(Blocks.PINK_WOOL) : of(Blocks.LIME_WOOL);
                }
            }
        }
    }

    /** A tall tiled stairwell: steps winding up round a pillar, through a round hole into the floor above. */
    private void stairwell(BlockState[] c, int lx, int lz) {
        int ceil = S + 18, upper = S + 12;
        base(c, lx < 14 && lz < 14 ? S - 1 : S, ceil);
        int dx = lx - 20, dz = lz - 20;
        double r = Math.sqrt(dx * dx + dz * dz);
        boolean stairGap = lx >= 31 && lx <= 33 && lz >= 19 && lz <= 30;
        if (r <= 2.2) {
            fill(c, S + 1, ceil - 1, tile);
            return;
        }
        if (r <= 4.6) {
            double a = Math.atan2(dz, dx);
            if (a < 0) a += Math.PI * 2;
            int step = (int) (a / (Math.PI * 2) * 12);
            c[S + 1 + step] = tile;
            return;
        }
        if (stairGap) {
            int i = 30 - lz;
            fill(c, S + 1, S + 1 + i, tile);
            return;
        }
        if (r > 5.0) {
            c[upper] = lx % 6 == 3 && lz % 6 == 3 ? light : tile;
            boolean edge = r <= 5.9 || (lx == 30 || lx == 34) && lz >= 19 && lz <= 30;
            if (edge) c[upper + 1] = bars;
        }
        if (lx % 7 == 3 && lz % 7 == 3) c[ceil] = light;
    }

    private static final BlockState LOUNGER_BACK = Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
    private static final BlockState LOUNGER_SEAT = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);

    /** A tall, quiet room with a long pool, loungers, and a ladder up the wall to a little doorway marked EXIT. */
    private void lounge(BlockState[] c, int lx, int lz) {
        int ceil = S + 13;
        boolean pool = lx >= 8 && lx <= 31 && lz >= 12 && lz <= 27;
        base(c, pool ? S - 4 : S, ceil);
        if (pool && lz == 20 && lx % 4 == 0) c[S - 4] = light;
        if (lz == 20 && lx % 3 != 0 && lx > 2 && lx < 37) c[ceil] = LevelParts.fluorescent(false);
        if (lx % 4 == 1 && lx >= 5 && lx <= 33) {
            if (lz == 31) c[S + 1] = LOUNGER_BACK;
            if (lz == 30) c[S + 1] = LOUNGER_SEAT;
        }
        if (lx == 35 && lz == 31) c[S + 1] = of(Blocks.BARREL);
        // the way out: a ladder up the north wall to a small doorway high up
        if (lz == 1 && lx >= 9 && lx <= 11) {
            fill(c, S + 1, S + 11, tile);
            if (lx == 10) {
                c[S + 8] = tile;
                LevelParts.exitDoor(c, S + 9, Direction.SOUTH);
                c[S + 11] = light;
            }
        }
        if (lz == 2 && lx == 10) fill(c, S + 1, S + 8, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
    }

    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** A wide flooded hall, pale light through big windows, armchairs sunk in the water. */
    private void misty(BlockState[] c, long seed, int lx, int lz, int x, int z) {
        int ceil = S + 9;
        base(c, S - 1, ceil);
        if (lx % 8 == 4 && lz % 8 == 4) c[ceil] = light;
        if (lx % 7 == 3 && lz % 6 == 2 && unit(seed, x, z, 760) < 0.7) {
            Direction facing = FACINGS[GenMath.pick(seed, x, z, 761, 4)];
            c[S] = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.WATERLOGGED, true);
        }
    }

    /** Narrow tiled tunnels, knee-deep, twisting off in every direction. */
    private void tunnels(BlockState[] c, int lx, int lz) {
        boolean corridor = Math.floorMod(lx, 9) <= 2 || Math.floorMod(lz, 9) <= 2;
        if (!corridor) {
            fill(c, 1, HEIGHT - 2, tile);
            return;
        }
        base(c, S - 2, S + 4);
        if (Math.floorMod(lx, 9) == 1 && Math.floorMod(lz, 9) == 1) c[S + 4] = light;
    }

    /** For the tour and tests: the middle of the nearest room of a kind, searching outwards from a cell. */
    public static int @Nullable [] find(long seed, Room room, int fromCx, int fromCz) {
        for (int ring = 0; ring < 30; ring++) {
            for (int cx = fromCx - ring; cx <= fromCx + ring; cx++) {
                for (int cz = fromCz - ring; cz <= fromCz + ring; cz++) {
                    if (Math.max(Math.abs(cx - fromCx), Math.abs(cz - fromCz)) != ring) continue;
                    if (room(seed, cx, cz) == room) return new int[]{cx * CELL, cz * CELL};
                }
            }
        }
        return null;
    }
}
