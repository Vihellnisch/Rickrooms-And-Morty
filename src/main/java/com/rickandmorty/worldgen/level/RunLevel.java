package com.rickandmorty.worldgen.level;

import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.unit;

/**
 * Run For Your Life (Level !). A white hospital hall lit only by red EXIT signs that flicker,
 * locked doors down both sides, beds and chairs left in the way, and the moment you arrive,
 * they come out behind you. Run. The hall is a maze of long straight runs: now and then it turns
 * left, or right, or splits both ways and you have to choose. Many of its dead ends finish in the
 * one kind of door that opens. Some of the others are just dead ends.
 * <p>
 * The level is cut into mazes 20 x 20 cells (7 blocks each, a 5-wide hall inside 1-thick walls).
 * Each maze is closed off from the others, so everyone who arrives gets one of their own. A maze
 * starts at its west cell (1, 10) heading east; floor y 40, hall y 41-44.
 */
public final class RunLevel implements LevelGenerator {
    public static final int HEIGHT = 64, FLOOR = 40, CELL = 7, CELLS = 20, MAZE = CELL * CELLS, START_I = 1, START_K = CELLS / 2;
    /** Connection bits: east, west, south, north. */
    public static final int EAST = 1, WEST = 2, SOUTH = 4, NORTH = 8;
    private static final int MAX_CARVED = 170, MIN_CARVED = 90;

    private static BlockState of(Block block) {
        return block.defaultBlockState();
    }

    private final BlockState wall = of(Blocks.WHITE_CONCRETE), skirting = of(Blocks.LIGHT_GRAY_CONCRETE), tile = of(Blocks.SMOOTH_QUARTZ), tileDark = of(Blocks.WHITE_CONCRETE),
            ceiling = of(Blocks.WHITE_CONCRETE), rock = of(Blocks.STONE);

    @Override
    public int height() {
        return HEIGHT;
    }

    // ------------------------------------------------------------------ the maze

    /**
     * One maze: for each cell, its connections (0 = not part of the hall), the cell it was reached
     * from (toward the start; -1 at the start), how many cells from the start it is, and whether a
     * dead end finishes in the exit door.
     */
    public record Maze(int[] links, int[] parent, int[] depth, boolean[] exit) {
        public boolean hall(int i, int k) {
            return i >= 0 && k >= 0 && i < CELLS && k < CELLS && links[i * CELLS + k] != 0;
        }

        public int links(int i, int k) {
            return hall(i, k) ? links[i * CELLS + k] : 0;
        }

        /** How many cells of hall. */
        public int size() {
            int count = 0;
            for (int l : links) if (l != 0) count++;
            return count;
        }
    }

    private static final Map<Long, Maze> MAZES = new ConcurrentHashMap<>();

    public static Maze maze(long seed, int mx, int mz) {
        long key = hash(seed, mx, mz, 9700);
        Maze cached = MAZES.get(key);
        if (cached != null) return cached;
        if (MAZES.size() > 512) MAZES.clear();
        Maze built = build(key);
        MAZES.put(key, built);
        return built;
    }

    private static int[] step(int dir) {
        return switch (dir) {
            case EAST -> new int[]{1, 0};
            case WEST -> new int[]{-1, 0};
            case SOUTH -> new int[]{0, 1};
            default -> new int[]{0, -1};
        };
    }

    private static int opposite(int dir) {
        return dir == EAST ? WEST : dir == WEST ? EAST : dir == SOUTH ? NORTH : SOUTH;
    }

    /** Turning left / right from a heading (looking down at the map, north up). */
    private static int left(int dir) {
        return dir == EAST ? NORTH : dir == NORTH ? WEST : dir == WEST ? SOUTH : EAST;
    }

    private static int right(int dir) {
        return opposite(left(dir));
    }

    /**
     * Grows the hall from the start: each branch runs straight for 2-6 cells, then turns left, turns
     * right, or splits both ways. A branch that runs into the edge or into another part of the
     * hall stops: a dead end. Then one dead end in two (and always the furthest) gets the exit door.
     */
    private static Maze build(long key) {
        // a maze that boxed itself in early is grown again, a different way
        Maze best = null;
        for (int attempt = 0; attempt < 12; attempt++) {
            Maze maze = grow(key + attempt * 0x9E3779B97F4A7C15L);
            if (best == null || maze.size() > best.size()) best = maze;
            if (best.size() >= MIN_CARVED) break;
        }
        return best;
    }

    private static Maze grow(long key) {
        int n = CELLS * CELLS;
        int[] links = new int[n], parent = new int[n], depth = new int[n];
        boolean[] exit = new boolean[n];
        java.util.Arrays.fill(parent, -2);
        RandomSource random = RandomSource.create(key);
        int start = START_I * CELLS + START_K;
        parent[start] = -1;
        ArrayDeque<int[]> tips = new ArrayDeque<>();      // {cell, heading, cells left in this run}
        tips.add(new int[]{start, EAST, 3 + random.nextInt(3)});
        int carved = 1;
        while (!tips.isEmpty() && carved < MAX_CARVED) {
            int[] tip = random.nextFloat() < 0.5f ? tips.pollFirst() : tips.pollLast();
            int cell = tip[0], dir = tip[1];
            int i = cell / CELLS, k = cell % CELLS;
            int[] d = step(dir);
            int ni = i + d[0], nk = k + d[1];
            int next = ni * CELLS + nk;
            if (ni < 0 || nk < 0 || ni >= CELLS || nk >= CELLS || parent[next] != -2) {
                // blocked: half the time the hall bends away instead of stopping dead
                if (tip.length == 3 && random.nextFloat() < 0.6f) tips.add(new int[]{cell, random.nextBoolean() ? left(dir) : right(dir), 2 + random.nextInt(3), 1});
                continue;
            }
            parent[next] = cell;
            depth[next] = depth[cell] + 1;
            links[cell] |= dir;
            links[next] |= opposite(dir);
            carved++;
            if (tip[2] > 1) {
                tips.add(new int[]{next, dir, tip[2] - 1});
                continue;
            }
            float r = random.nextFloat();
            if (r < 0.36f || r >= 0.72f) tips.add(new int[]{next, left(dir), 2 + random.nextInt(5)});
            if (r >= 0.36f) tips.add(new int[]{next, right(dir), 2 + random.nextInt(5)});
        }
        int furthest = -1;
        for (int c = 0; c < n; c++) {
            if (c == start || links[c] == 0 || Integer.bitCount(links[c]) != 1) continue;
            if (furthest < 0 || depth[c] > depth[furthest]) furthest = c;
            exit[c] = random.nextFloat() < 0.5f;
        }
        if (furthest >= 0) exit[furthest] = true;
        return new Maze(links, parent, depth, exit);
    }

    // ------------------------------------------------------------------ where things are

    /** Which maze, cell and spot in the cell (x, z) is in: {mx, mz, i, k, lx, lz}. */
    public static int[] locate(int x, int z) {
        int mx = Math.floorDiv(x, MAZE), mz = Math.floorDiv(z, MAZE);
        int ox = x - mx * MAZE, oz = z - mz * MAZE;
        return new int[]{mx, mz, ox / CELL, oz / CELL, ox % CELL, oz % CELL};
    }

    /** The middle of cell (i, k) of maze (mx, mz), in block coordinates {x, z}. */
    public static int[] middle(int mx, int mz, int i, int k) {
        return new int[]{mx * MAZE + i * CELL + CELL / 2, mz * MAZE + k * CELL + CELL / 2};
    }

    /** The wall a dead end's exit door is in: the one facing the way in. */
    public static int exitWall(int links) {
        return opposite(links);
    }

    /** What's in the way in this cell: 0 nothing, 1 a bed along a wall, 2 chairs, 3 a bed out across the hall. */
    static int obstacle(long seed, int mx, int mz, int i, int k) {
        if (i == START_I && k == START_K) return 0;
        double r = unit(hash(seed, mx * CELLS + i, mz * CELLS + k, 9710));
        return r < 0.12 ? 1 : r < 0.22 ? 2 : r < 0.3 ? 3 : 0;
    }

    // ------------------------------------------------------------------ the column

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        for (int y = FLOOR - 6; y <= FLOOR + 10; y++) c[y] = rock;
        int[] at = locate(x, z);
        int mx = at[0], mz = at[1], i = at[2], k = at[3], lx = at[4], lz = at[5];
        Maze maze = maze(seed, mx, mz);
        if (!maze.hall(i, k)) return c;
        int links = maze.links(i, k);
        boolean westWall = lx == 0, eastWall = lx == CELL - 1, northWall = lz == 0, southWall = lz == CELL - 1;
        boolean corner = (westWall || eastWall) && (northWall || southWall);
        // the edge of the cell is open where the hall carries on that way
        boolean open = !corner && (westWall && (links & WEST) != 0 || eastWall && (links & EAST) != 0
                || northWall && (links & NORTH) != 0 || southWall && (links & SOUTH) != 0);
        boolean edge = westWall || eastWall || northWall || southWall;
        if (edge && !open) {
            wall(c, seed, x, z, maze, mx, mz, i, k, links, westWall ? WEST : eastWall ? EAST : northWall ? NORTH : SOUTH, corner,
                    westWall || eastWall ? lz : lx);
            return c;
        }
        c[FLOOR] = (Math.floorMod(x, 2) + Math.floorMod(z, 2)) % 2 == 0 ? tile : tileDark;
        for (int y = FLOOR + 1; y <= FLOOR + 4; y++) c[y] = null;
        c[FLOOR + 5] = ceiling;
        if (!edge) inside(c, seed, maze, mx, mz, i, k, lx, lz, links);
        return c;
    }

    /** A wall of the hall: skirting, plaster, a locked door now and then, a red EXIT sign over some, or (in a lucky dead end) the way out. */
    private void wall(BlockState[] c, long seed, int x, int z, Maze maze, int mx, int mz, int i, int k, int links, int side, boolean corner, int along) {
        c[FLOOR] = wall;
        c[FLOOR + 1] = skirting;
        for (int y = FLOOR + 2; y <= FLOOR + 4; y++) c[y] = wall;
        c[FLOOR + 5] = ceiling;
        if (corner) return;
        Direction in = switch (side) {
            case WEST -> Direction.EAST;
            case EAST -> Direction.WEST;
            case NORTH -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
        boolean deadEnd = Integer.bitCount(links) == 1 && !(i == START_I && k == START_K);
        if (deadEnd && side == exitWall(links) && maze.exit()[i * CELLS + k]) {
            if (along == CELL / 2) door(c, in, true);
            return;
        }
        // locked hospital doors: two to a wall, most walls
        if ((along == 2 || along == 5) && unit(hash(seed, x, z, 9720)) < 0.7) door(c, in, false);
    }

    /** Inside a cell: red EXIT signs on the walls, beds and chairs in the way. */
    private void inside(BlockState[] c, long seed, Maze maze, int mx, int mz, int i, int k, int lx, int lz, int links) {
        // a sign over the middle of each closed side, hung on the wall from the hall side
        int[][] sides = {{WEST, 1, CELL / 2}, {EAST, CELL - 2, CELL / 2}, {NORTH, CELL / 2, 1}, {SOUTH, CELL / 2, CELL - 2}};
        for (int[] s : sides) {
            if ((links & s[0]) != 0 || lx != s[1] || lz != s[2]) continue;
            boolean exitHere = Integer.bitCount(links) == 1 && maze.exit()[i * CELLS + k] && s[0] == exitWall(links);
            if (!exitHere && unit(hash(seed, mx * CELLS + i, mz * CELLS + k, s[0] + 9730)) > 0.55) continue;
            Direction facing = s[0] == WEST ? Direction.EAST : s[0] == EAST ? Direction.WEST : s[0] == NORTH ? Direction.SOUTH : Direction.NORTH;
            c[FLOOR + 3] = ModBlocks.RED_EXIT_SIGN.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
        }
        if (Integer.bitCount(links) == 1) return;          // keep dead ends clear, you'll be turning round in them
        if ((links & (EAST | WEST)) != 0 && (links & (NORTH | SOUTH)) != 0) return;      // and corners and junctions
        int kind = obstacle(seed, mx, mz, i, k);
        boolean eastWest = (links & (EAST | WEST)) != 0 && (links & (NORTH | SOUTH)) == 0;
        // along the hall's length, a, and across it, b (1..5)
        int a = eastWest ? lx : lz, b = eastWest ? lz : lx;
        Direction along = eastWest ? Direction.EAST : Direction.SOUTH, across = eastWest ? Direction.SOUTH : Direction.EAST;
        switch (kind) {
            case 1 -> {
                if (b == 1 && (a == 2 || a == 3)) c[FLOOR + 1] = bed(along, a == 3);
            }
            case 2 -> {
                if (b == 4 && (a == 2 || a == 4)) c[FLOOR + 1] = of(Blocks.QUARTZ_STAIRS).setValue(StairBlock.FACING, across.getOpposite());
            }
            case 3 -> {
                if (a == 3 && (b == 2 || b == 3)) c[FLOOR + 1] = bed(across, b == 3);
            }
            default -> {
            }
        }
    }

    private void door(BlockState[] c, Direction facing, boolean exit) {
        BlockState door = (exit ? ModBlocks.EXIT_DOOR.get().defaultBlockState() : of(Blocks.IRON_DOOR)).setValue(DoorBlock.FACING, facing);
        c[FLOOR + 1] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        c[FLOOR + 2] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
    }

    private static BlockState bed(Direction facing, boolean head) {
        return Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, head ? BedPart.HEAD : BedPart.FOOT);
    }

    // ------------------------------------------------------------------ travel

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        return BackroomsLevel.SUNLIT_LOBBY;      // you made it
    }

    @Override
    public int minStandY() {
        return FLOOR + 1;
    }

    @Override
    public int maxStandY() {
        return FLOOR + 1;
    }

    /** At the start of a maze of your own, the dead end it grows from behind you. */
    @Override
    public Vec3 landing(long seed, RandomSource random) {
        int mx = random.nextIntBetweenInclusive(-2000, 2000), mz = random.nextIntBetweenInclusive(-2000, 2000);
        int[] m = middle(mx, mz, START_I, START_K);
        return new Vec3(m[0] - 1.5, FLOOR + 1, m[1] + 0.5);
    }
}
