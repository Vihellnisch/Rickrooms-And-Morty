package com.rickandmorty.worldgen.level;

import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.unit;

/**
 * the Staircase. Escher's Relativity, in the sky: white landings
 * hanging in the air at every height, joined by flights of stairs and spiral stairs, walls with
 * arches standing on them that open onto more sky, stairs hanging upside down off their
 * undersides, and clouds drifting through all of it, above, below and in between. It's quiet and
 * bright and easy to get lost in. Fall, and you fall out of the bottom of the level.
 * <p>
 * Landings are nodes on a grid: 32 blocks apart across, 16 up. A node joins the one beside it
 * one step up or down the grid with a straight flight (16 steps, then flat), and some nodes have a
 * spiral stair up to the node above. Everything is quartz white.
 */
public final class StaircaseLevel implements LevelGenerator {
    public static final int HEIGHT = 256, H = 32, V = 16, LOWEST = 2, HIGHEST = 13, CLOUD_CELL = 44, CLOUD_V = 36;

    private static BlockState of(Block block) {
        return block.defaultBlockState();
    }

    private final BlockState white = of(Blocks.SMOOTH_QUARTZ), wallBlock = of(Blocks.WHITE_CONCRETE), slab = of(Blocks.SMOOTH_QUARTZ_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);

    private static BlockState stair(Direction facing, Half half) {
        return Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    // ------------------------------------------------------------------ the landings

    /** A landing: its middle, half its size each way, its height; and whether it has a wall (along its north edge) with an arch. */
    public record Node(int x, int z, int hx, int hz, int y, boolean wall, boolean door, boolean spiral) {
    }

    public static @Nullable Node node(long seed, int i, int k, int j) {
        if (j < LOWEST || j > HIGHEST) return null;
        long h = hash(seed, i, k, j, 9400);
        if (unit(h) > 0.32) return null;
        int x = i * H + H / 2 + (int) Math.floorMod(h >>> 12, 7L) - 3, z = k * H + H / 2 + (int) Math.floorMod(h >>> 18, 7L) - 3;
        int hx = 3 + (int) Math.floorMod(h >>> 24, 3L), hz = 3 + (int) Math.floorMod(h >>> 28, 3L);
        boolean wall = unit(hash(seed, i, k, j, 9401)) < 0.4;
        return new Node(x, z, hx, hz, j * V, wall, wall && unit(hash(seed, i, k, j, 9402)) < 0.2, unit(hash(seed, i, k, j, 9403)) < 0.18);
    }

    /** The flight from node (i,k,j) eastwards (east) or southwards: to the node one step across and dj = +1 or -1 up, or 0 for none. */
    public static int flight(long seed, int i, int k, int j, boolean east) {
        Node a = node(seed, i, k, j);
        if (a == null) return 0;
        long h = hash(seed, i, k, j, east ? 9410 : 9411);
        if (unit(h) > 0.75) return 0;
        int dj = (h >>> 20 & 1) == 0 ? 1 : -1;
        Node b = east ? node(seed, i + 1, k, j + dj) : node(seed, i, k + 1, j + dj);
        if (b == null) {
            dj = -dj;
            b = east ? node(seed, i + 1, k, j + dj) : node(seed, i, k + 1, j + dj);
        }
        if (b == null) return 0;
        // they must overlap across the flight by 3, and be far enough apart to fit 16 steps
        int lo = east ? Math.max(a.z - a.hz, b.z - b.hz) : Math.max(a.x - a.hx, b.x - b.hx);
        int hi = east ? Math.min(a.z + a.hz, b.z + b.hz) : Math.min(a.x + a.hx, b.x + b.hx);
        int gap = east ? (b.x - b.hx) - (a.x + a.hx) - 1 : (b.z - b.hz) - (a.z + a.hz) - 1;
        return hi - lo >= 2 && gap >= V ? dj : 0;
    }

    // ------------------------------------------------------------------ the column

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        clouds(c, seed, x, z);
        int ci = Math.floorDiv(x, H), ck = Math.floorDiv(z, H);
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int k = ck - 1; k <= ck + 1; k++) {
                for (int j = LOWEST; j <= HIGHEST; j++) {
                    Node n = node(seed, i, k, j);
                    if (n == null) continue;
                    landing(c, seed, x, z, n);
                    int east = flight(seed, i, k, j, true), south = flight(seed, i, k, j, false);
                    if (east != 0) flight(c, x, z, n, node(seed, i + 1, k, j + east), true);
                    if (south != 0) flight(c, x, z, n, node(seed, i, k + 1, j + south), false);
                    if (n.spiral && node(seed, i, k, j + 1) != null) spiral(c, x, z, n);
                }
            }
        }
        return c;
    }

    /** Cloud puffs: flattened blobs on a loose 3D grid, and a sea of it far below. */
    private void clouds(BlockState[] c, long seed, int x, int z) {
        BlockState cloud = ModBlocks.CLOUD.get().defaultBlockState();
        int sea = 8 + (int) Math.round(2.5 * Math.sin(x * 0.11 + Math.cos(z * 0.07) * 2) + 2 * Math.cos(z * 0.13));
        for (int y = 2; y <= sea; y++) c[y] = cloud;
        int gx = Math.floorDiv(x, CLOUD_CELL), gz = Math.floorDiv(z, CLOUD_CELL);
        for (int i = gx - 1; i <= gx + 1; i++) {
            for (int k = gz - 1; k <= gz + 1; k++) {
                for (int j = 1; j * CLOUD_V < HEIGHT - 10; j++) {
                    long h = hash(seed, i, k, j, 9420);
                    if (unit(h) > 0.4) continue;
                    double px = (i + unit(Long.rotateLeft(h, 11))) * CLOUD_CELL, pz = (k + unit(Long.rotateLeft(h, 23))) * CLOUD_CELL;
                    double py = j * CLOUD_V + unit(Long.rotateLeft(h, 35)) * CLOUD_V * 0.6;
                    double rx = 6 + unit(Long.rotateLeft(h, 47)) * 9, ry = 2.5 + unit(Long.rotateLeft(h, 5)) * 3;
                    double dx = (x + 0.5 - px) / rx, dz = (z + 0.5 - pz) / rx, d2 = dx * dx + dz * dz;
                    if (d2 >= 1) continue;
                    double half = ry * Math.sqrt(1 - d2);
                    // flat-bottomed, domed on top, like a real cumulus
                    for (int y = (int) Math.round(py - half * 0.4); y <= (int) Math.round(py + half); y++) {
                        if (y > 0 && y < HEIGHT && c[y] == null) c[y] = cloud;
                    }
                }
            }
        }
    }

    private void landing(BlockState[] c, long seed, int x, int z, Node n) {
        int dx = x - n.x, dz = z - n.z;
        if (Math.abs(dx) > n.hx || Math.abs(dz) > n.hz) {
            hanging(c, seed, x, z, n);
            return;
        }
        c[n.y] = white;
        c[n.y - 1] = Math.abs(dx) == n.hx || Math.abs(dz) == n.hz ? white : null;
        if (n.wall && dz == -n.hz) {
            // a wall along the north edge, nine high, with a round-topped arch through the middle
            boolean opening = Math.abs(dx) <= 1;
            for (int y = n.y + 1; y <= n.y + 9; y++) {
                int up = y - n.y;
                boolean arch = opening && (up <= 4 || up == 5 && dx == 0);
                if (!arch) c[y] = wallBlock;
            }
            if (opening && dx != 0) c[n.y + 5] = stair(dx < 0 ? Direction.EAST : Direction.WEST, Half.TOP);
            if (n.door && dx == 0) {
                BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
                c[n.y + 1] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
                c[n.y + 2] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            }
            // and two narrow arched windows either side, higher up
            if (Math.abs(Math.abs(dx) - 3) == 0 && n.hx >= 4) {
                c[n.y + 5] = null;
                c[n.y + 6] = null;
            }
        }
    }

    /** Some landings have a flight hanging upside down from their west edge, going nowhere. */
    private void hanging(BlockState[] c, long seed, int x, int z, Node n) {
        if (unit(hash(seed, n.x, n.z, n.y, 9430)) > 0.35) return;
        int out = (n.x - n.hx) - x, dz = z - n.z;
        if (out < 1 || out > 7 || Math.abs(dz) > 1) return;
        int y = n.y - out;
        c[y] = stair(Direction.WEST, Half.TOP);
        c[y + 1] = white;
    }

    private void flight(BlockState[] c, int x, int z, Node a, @Nullable Node b, boolean east) {
        if (b == null) return;
        int along = east ? x : z, across = east ? z : x;
        int start = east ? a.x + a.hx + 1 : a.z + a.hz + 1, end = east ? b.x - b.hx - 1 : b.z - b.hz - 1;
        if (along < start || along > end) return;
        int lo = east ? Math.max(a.z - a.hz, b.z - b.hz) : Math.max(a.x - a.hx, b.x - b.hx);
        int hi = east ? Math.min(a.z + a.hz, b.z + b.hz) : Math.min(a.x + a.hx, b.x + b.hx);
        int middle = (lo + hi) / 2;
        if (Math.abs(across - middle) > 1) return;
        boolean up = b.y > a.y;
        int step = along - start;
        // up flights climb straight away, then run flat; down flights run flat, then descend onto the far landing
        int y;
        Direction facing;
        if (up) {
            y = Math.min(a.y + 1 + step, b.y);
            facing = east ? Direction.EAST : Direction.SOUTH;
        } else {
            int fromEnd = end - along;
            y = Math.min(b.y + 1 + fromEnd, a.y);
            facing = east ? Direction.WEST : Direction.NORTH;
        }
        boolean flat = up ? y == b.y : y == a.y;
        c[y] = flat ? white : stair(facing, Half.BOTTOM);
        c[y - 1] = white;
        if (!flat && Math.abs(across - middle) == 1 && c[y - 2] == null) c[y - 2] = slab;
    }

    /** A spiral stair round a post at the landing's south-east corner, sixteen steps to the landing above. */
    private void spiral(BlockState[] c, int x, int z, Node n) {
        int cx = n.x + n.hx - 2, cz = n.z + n.hz - 2;
        int dx = x - cx, dz = z - cz;
        if (Math.abs(dx) > 2 || Math.abs(dz) > 2) return;
        if (dx == 0 && dz == 0) {
            for (int y = n.y + 1; y <= n.y + V; y++) c[y] = white;
            return;
        }
        if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
            for (int y = n.y + 1; y < n.y + V; y++) c[y] = null;
            return;
        }
        // round the 5x5 ring, clockwise from the north-west corner: 16 cells, one step up each
        int p;
        if (dz == -2) p = dx + 2;
        else if (dx == 2) p = 4 + dz + 2;
        else if (dz == 2) p = 8 + (2 - dx);
        else p = 12 + (2 - dz);
        p = Math.floorMod(p, 16);
        int y = n.y + 1 + p;
        Direction facing = dz == -2 ? Direction.EAST : dx == 2 ? Direction.SOUTH : dz == 2 ? Direction.WEST : Direction.NORTH;
        for (int yy = n.y + 1; yy < n.y + V; yy++) if (yy != y && yy != y - 1) c[yy] = null;
        c[y] = stair(facing, Half.BOTTOM);
        c[y - 1] = white;
    }

    // ------------------------------------------------------------------ travel

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        double r = unit(hash(seed, pos.getX(), pos.getY(), pos.getZ(), 9440));
        return r < 0.55 ? BackroomsLevel.DROP_LIMIT : r < 0.8 ? null : BackroomsLevel.ENDLESS_FIELD;
    }

    @Override
    public int minStandY() {
        return LOWEST * V;
    }

    /** You arrive on a landing. */
    @Override
    public Vec3 landing(long seed, RandomSource random) {
        for (int attempt = 0; attempt < 2000; attempt++) {
            int i = random.nextIntBetweenInclusive(-80, 80), k = random.nextIntBetweenInclusive(-80, 80), j = random.nextIntBetweenInclusive(LOWEST + 2, HIGHEST - 2);
            Node n = node(seed, i, k, j);
            if (n != null && !n.spiral) return new Vec3(n.x + 0.5, n.y + 1, n.z + 1.5);
        }
        return new Vec3(0.5, 120, 0.5);
    }
}
