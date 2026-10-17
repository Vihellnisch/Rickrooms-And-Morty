package com.rickandmorty.worldgen.level;

import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.unit;

/**
 * Froopyland, the world Rick made for Beth when she was little, so
 * she'd have somewhere safe to play. Soft pastel hills that bounce you back up (nobody can get
 * hurt here), a rainbow river you can breathe in, lollipop trees, puffball bushes, rainbows
 * arching over the grass, fat clouds, and a sun that never goes down. Somewhere, a hole in a
 * hillside that nobody built: Tommy's.
 * <p>
 * Ground at y 56-80; the river winds east-west on lanes 260 apart; trees on an 11-block grid;
 * rainbows one per 150-block square at most.
 */
public final class FroopylandLevel implements LevelGenerator {
    public static final int HEIGHT = 192, BASE = 60, RIVER_LANE = 260, TREE_CELL = 11, RAINBOW_CELL = 150, CAVE_CELL = 500, DOOR_CELL = 180;

    private static BlockState of(Block block) {
        return block.defaultBlockState();
    }

    private final BlockState soil = of(Blocks.PINK_TERRACOTTA), deep = of(Blocks.WHITE_TERRACOTTA), water = of(Blocks.WATER), stick = of(Blocks.WHITE_CONCRETE),
            stripe = of(Blocks.PINK_CONCRETE), bone = of(Blocks.BONE_BLOCK), dark = of(Blocks.GRAY_TERRACOTTA);
    private final BlockState[] bands = {of(Blocks.RED_CONCRETE), of(Blocks.ORANGE_CONCRETE), of(Blocks.YELLOW_CONCRETE), of(Blocks.LIME_CONCRETE),
            of(Blocks.LIGHT_BLUE_CONCRETE), of(Blocks.BLUE_CONCRETE), of(Blocks.PURPLE_CONCRETE)};
    private final BlockState[] candy = {of(Blocks.PINK_WOOL), of(Blocks.LIGHT_BLUE_WOOL), of(Blocks.YELLOW_WOOL), of(Blocks.MAGENTA_WOOL), of(Blocks.LIME_WOOL), of(Blocks.WHITE_WOOL)};
    private final BlockState[] flowers = {of(Blocks.ALLIUM), of(Blocks.PINK_TULIP), of(Blocks.AZURE_BLUET), of(Blocks.CORNFLOWER), of(Blocks.OXEYE_DAISY), of(Blocks.LILY_OF_THE_VALLEY)};

    @Override
    public int height() {
        return HEIGHT;
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Round, soft hills like a child would draw. */
    public static int ground(long seed, int x, int z) {
        double h = noise(seed, x, z, 90.0, 9500) * 14 + noise(seed, x, z, 34.0, 9501) * 6 + noise(seed, x, z, 12.0, 9502) * 1.5;
        int g = BASE - 4 + (int) Math.round(h);
        int river = riverAcross(seed, x, z);
        if (river != Integer.MIN_VALUE) {
            // the banks slope down to the water
            int d = Math.abs(river);
            if (d <= 12) g = Math.min(g, BASE - 2 + (int) Math.round(smooth((d - 5) / 7.0) * (g - BASE + 2)));
        }
        return g;
    }

    /** Signed distance across the nearest river (its middle 0), or MIN_VALUE if none within 12. */
    public static int riverAcross(long seed, int x, int z) {
        int lane = Math.floorDiv(z, RIVER_LANE);
        for (int l = lane - 1; l <= lane + 1; l++) {
            long h = hash(seed, l, 0, 9510);
            double amplitude = 14 + unit(h) * 30, period = 150 + unit(Long.rotateLeft(h, 20)) * 160, phase = unit(Long.rotateLeft(h, 40)) * Math.PI * 2;
            int middle = l * RIVER_LANE + RIVER_LANE / 2 + (int) Math.round(amplitude * Math.sin(x / period * Math.PI * 2 + phase)
                    + 6 * Math.sin(x / 37.0 + phase));
            if (Math.abs(z - middle) <= 12) return z - middle;
        }
        return Integer.MIN_VALUE;
    }

    /** Which colour of turf: big soft patches of each. */
    private static int colour(long seed, int x, int z) {
        double a = noise(seed, x, z, 70.0, 9520), b = noise(seed, x, z, 70.0, 9521);
        return (a > 0.5 ? 1 : 0) + (b > 0.5 ? 2 : 0);
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        int g = ground(seed, x, z);
        for (int y = 0; y < g; y++) c[y] = y < g - 4 ? deep : soil;
        int river = riverAcross(seed, x, z);
        boolean wet = river != Integer.MIN_VALUE && Math.abs(river) <= 4;
        if (wet) {
            // a rainbow river: clear water over a bed striped in seven colours
            int bed = BASE - 5;
            for (int y = bed; y <= g; y++) c[y] = null;
            c[bed] = bands[Math.min(6, river + 3 < 0 ? 0 : (river + 4) * 7 / 9)];
            for (int y = bed + 1; y <= BASE - 2; y++) c[y] = water;
        } else {
            c[g] = ModBlocks.turf(colour(seed, x, z));
            if (!tree(c, seed, x, z)) {
                double r = unit(hash(seed, x, z, 9530));
                if (r < 0.05) c[g + 1] = flowers[(int) (r * 1000) % flowers.length];
                else if (r < 0.054) puffball(c, seed, x, z, g);
            }
        }
        rainbow(c, seed, x, z);
        if (!wet && isDoor(seed, x, z)) {
            // a door standing on its own in the grass: Rick's way home
            BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.SOUTH);
            c[g + 1] = door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
            c[g + 2] = door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        }
        cave(c, seed, x, z, g);
        clouds(c, seed, x, z);
        return c;
    }

    private void puffball(BlockState[] c, long seed, int x, int z, int g) {
        c[g + 1] = candy[(int) Math.floorMod(hash(seed, x, z, 9531), (long) candy.length)];
    }

    /** Lollipop trees: a striped stick, a round sweet on top. */
    private boolean tree(BlockState[] c, long seed, int x, int z) {
        int gx = Math.floorDiv(x, TREE_CELL), gz = Math.floorDiv(z, TREE_CELL);
        boolean any = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long h = hash(seed, gx + dx, gz + dz, 9540);
                if (unit(h) > 0.22) continue;
                int tx = (gx + dx) * TREE_CELL + 3 + (int) Math.floorMod(h >>> 16, TREE_CELL - 6L), tz = (gz + dz) * TREE_CELL + 3 + (int) Math.floorMod(h >>> 24, TREE_CELL - 6L);
                if (riverAcross(seed, tx, tz) != Integer.MIN_VALUE) continue;
                int base = ground(seed, tx, tz) + 1, stickHeight = 4 + (int) Math.floorMod(h >>> 32, 4L), radius = 2 + (int) Math.floorMod(h >>> 36, 2L);
                BlockState sweet = candy[(int) Math.floorMod(h >>> 40, (long) candy.length)];
                int ox = x - tx, oz = z - tz;
                if (ox == 0 && oz == 0) {
                    for (int y = base; y < base + stickHeight; y++) c[y] = (y - base) % 2 == 0 ? stick : stripe;
                    any = true;
                }
                int cy = base + stickHeight + radius - 1;
                for (int y = cy - radius; y <= cy + radius; y++) {
                    double d = (ox * ox + oz * oz + (y - cy) * (y - cy)) / (double) (radius * radius + radius);
                    if (d <= 1.0 && c[y] == null) {
                        c[y] = sweet;
                        any = true;
                    }
                }
            }
        }
        return any;
    }

    /** A rainbow arching over the grass, east-west, seven bands, standing in the ground at both ends. */
    private void rainbow(BlockState[] c, long seed, int x, int z) {
        int cx = Math.floorDiv(x, RAINBOW_CELL), cz = Math.floorDiv(z, RAINBOW_CELL);
        long h = hash(seed, cx, cz, 9550);
        if (unit(h) > 0.35) return;
        int mx = cx * RAINBOW_CELL + 40 + (int) Math.floorMod(h >>> 16, RAINBOW_CELL - 80L), mz = cz * RAINBOW_CELL + 40 + (int) Math.floorMod(h >>> 28, RAINBOW_CELL - 80L);
        if (Math.abs(z - mz) > 1) return;
        int outer = 22 + (int) Math.floorMod(h >>> 40, 14L), foot = ground(seed, mx, mz) - 2;
        int dx = x - mx;
        for (int band = 0; band < 7; band++) {
            int r = outer - band;
            if (Math.abs(dx) > r) continue;
            int y = foot + (int) Math.round(Math.sqrt(r * r - dx * dx));
            int next = Math.abs(dx) + 1 > r ? y : foot + (int) Math.round(Math.sqrt(Math.max(0, r * r - (Math.abs(dx) + 1) * (Math.abs(dx) + 1))));
            // fill down to where the arch is at the next column, so the band has no gaps on its steep ends
            for (int yy = Math.min(y, next + 1); yy <= y; yy++) if (yy > 0 && yy < HEIGHT && (c[yy] == null || c[yy].is(Blocks.WATER))) c[yy] = bands[band];
        }
    }

    /** Fat clouds high in the sky. */
    private void clouds(BlockState[] c, long seed, int x, int z) {
        int gx = Math.floorDiv(x, 40), gz = Math.floorDiv(z, 40);
        BlockState cloud = ModBlocks.CLOUD.get().defaultBlockState();
        for (int i = gx - 1; i <= gx + 1; i++) {
            for (int k = gz - 1; k <= gz + 1; k++) {
                long h = hash(seed, i, k, 9560);
                if (unit(h) > 0.3) continue;
                double px = (i + unit(Long.rotateLeft(h, 11))) * 40, pz = (k + unit(Long.rotateLeft(h, 23))) * 40, py = 120 + unit(Long.rotateLeft(h, 35)) * 20;
                double r = 6 + unit(Long.rotateLeft(h, 47)) * 6, d2 = ((x + 0.5 - px) * (x + 0.5 - px) + (z + 0.5 - pz) * (z + 0.5 - pz)) / (r * r);
                if (d2 >= 1) continue;
                double half = (4 + unit(Long.rotateLeft(h, 5)) * 4) * Math.sqrt(1 - d2);
                for (int y = (int) Math.round(py - half * 0.7); y <= (int) Math.round(py + half); y++) c[y] = cloud;
            }
        }
    }

    /** The door home in this 180-block square, if it has one: {x, z}, or null. */
    public static int @Nullable [] door(long seed, int cx, int cz) {
        long h = hash(seed, cx, cz, 9580);
        if (unit(h) > 0.45) return null;
        return new int[]{cx * DOOR_CELL + 20 + (int) Math.floorMod(h >>> 16, DOOR_CELL - 40L), cz * DOOR_CELL + 20 + (int) Math.floorMod(h >>> 28, DOOR_CELL - 40L)};
    }

    private static boolean isDoor(long seed, int x, int z) {
        int[] d = door(seed, Math.floorDiv(x, DOOR_CELL), Math.floorDiv(z, DOOR_CELL));
        return d != null && d[0] == x && d[1] == z && riverAcross(seed, x, z) == Integer.MIN_VALUE;
    }

    /** Tommy's hole: its middle {x, z}, or null if this square has none. */
    public static int @Nullable [] cave(long seed, int cx, int cz) {
        long h = hash(seed, cx, cz, 9570);
        if (unit(h) > 0.5) return null;
        return new int[]{cx * CAVE_CELL + 100 + (int) Math.floorMod(h >>> 16, CAVE_CELL - 200L), cz * CAVE_CELL + 100 + (int) Math.floorMod(h >>> 28, CAVE_CELL - 200L)};
    }

    /** Tommy's hole: a dark tunnel into a hillside, and bones. Very rare. */
    private void cave(BlockState[] c, long seed, int x, int z, int g) {
        int cx = Math.floorDiv(x, CAVE_CELL), cz = Math.floorDiv(z, CAVE_CELL);
        long h = hash(seed, cx, cz, 9570);
        if (unit(h) > 0.5) return;
        int mx = cx * CAVE_CELL + 100 + (int) Math.floorMod(h >>> 16, CAVE_CELL - 200L), mz = cz * CAVE_CELL + 100 + (int) Math.floorMod(h >>> 28, CAVE_CELL - 200L);
        int dx = x - mx, dz = z - mz;
        if (Math.abs(dz) > 16 || Math.abs(dx) > 3) return;
        int floor = ground(seed, mx, mz - 16) + 1 - Math.max(0, dz + 16) / 3;
        for (int y = floor - 1; y <= floor + 4 && y < HEIGHT; y++) {
            if (y == floor - 1) c[y] = dark;
            else if (Math.abs(dx) == 3 || y == floor + 4) {
                if (c[y] != null) c[y] = dark;
            } else c[y] = null;
        }
        if (dz > 10 && unit(hash(seed, x, z, 9571)) < 0.3) c[floor] = bone;
        if (dz == 14 && dx == -1) c[floor] = of(Blocks.SKELETON_SKULL);
        if (dz == 16 && dx == 0) {
            // and at the very end, a door: it doesn't go anywhere nice
            BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.NORTH);
            c[floor] = door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
            c[floor + 1] = door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        }
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        // the doors in the grass go home; the one at the end of Tommy's tunnel goes back down
        int[] cave = cave(seed, Math.floorDiv(pos.getX(), CAVE_CELL), Math.floorDiv(pos.getZ(), CAVE_CELL));
        boolean tommy = cave != null && Math.abs(pos.getX() - cave[0]) <= 3 && Math.abs(pos.getZ() - cave[1]) <= 16;
        return tommy ? BackroomsLevel.LEVEL_0 : null;
    }

    @Override
    public int minStandY() {
        return BASE - 8;
    }

    @Override
    public Vec3 landing(long seed, RandomSource random) {
        for (int attempt = 0; attempt < 200; attempt++) {
            int x = random.nextIntBetweenInclusive(-3000, 3000), z = random.nextIntBetweenInclusive(-3000, 3000);
            if (riverAcross(seed, x, z) != Integer.MIN_VALUE) continue;
            BlockState[] col = column(seed, x, z);
            int g = ground(seed, x, z);
            if (LevelGenerator.clear(col[g + 1]) && LevelGenerator.clear(col[g + 2])) return new Vec3(x + 0.5, g + 1, z + 0.5);
        }
        return new Vec3(0.5, BASE + 10, 0.5);
    }
}
