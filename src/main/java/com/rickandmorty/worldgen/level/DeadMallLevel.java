package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.MallBlocks;
import com.rickandmorty.block.StoreBlocks;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * The Dead Mall: a shopping mall that closed years ago and kept going. Two floors of curving
 * concourses wander between courts: atriums under skylights with escalators and glass railings,
 * fountain courts with palms, food courts, and the huge dark anchor department stores. Both sides
 * are lined with empty shops, some still lit, some shuttered. The music stopped a long time ago.
 * <p>
 * Laid out as a network: a court in each 72-block square (jittered), joined to its neighbours by
 * curved concourses; whatever is near a concourse or court is a shop, and beyond that is solid.
 */
public final class DeadMallLevel implements LevelGenerator {
    static final int CELL = 72;
    public static final int FLOOR = 2, LOW = 3, LOW_CEIL = 9, UP_FLOOR = 10, UP = 11, UP_CEIL = 18, ROOF = 24, HEIGHT = 32;
    /** How deep the shops go behind their fronts, and the anchors' half-width. */
    static final int SHOP_DEPTH = 11, ANCHOR = 17, SHOP_GRID = 14;
    private static final int SAMPLES = 20;
    private static final ResourceKey<LootTable> SHOP_LOOT = ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/mall_shop"));

    public enum Court { ATRIUM, FOUNTAIN, FOOD, ANCHOR }

    public enum Kind { CORRIDOR, COURT, ANCHOR, ANCHOR_WALL, STOREFRONT, SHOP, BACK }

    /** A court: where, how big, what kind. */
    public record Node(int x, int z, int radius, Court court, long hash) {
    }

    /** What's at a column, and the details needed to fill it in. */
    public record Spot(Kind kind, double outside, @Nullable Node node, double s, int width, double along, double nx, double nz, long edge, boolean well) {
    }

    private final BlockState bedrock = of(Blocks.BEDROCK), base = of(Blocks.SMOOTH_STONE), wall = of(ModBlocks.MALL_WALL.get());
    private final BlockState ceiling = of(ModBlocks.MALL_CEILING.get()), skylight = of(ModBlocks.MALL_SKYLIGHT.get());
    private final BlockState railing = of(ModBlocks.MALL_RAILING.get()), glass = of(Blocks.GLASS), shutter = of(ModBlocks.CORRUGATED_METAL.get());
    private final BlockState bench = of(ModBlocks.MALL_BENCH.get()), trash = of(ModBlocks.MALL_TRASH_CAN.get()), gumball = of(ModBlocks.GUMBALL_MACHINE.get());
    private final BlockState escalator = of(ModBlocks.MALL_ESCALATOR.get()), crate = of(ModBlocks.STOCK_CRATE.get());
    private final BlockState table = of(ModBlocks.DINING_TABLE.get()), chair = of(ModBlocks.STORE_CHAIR.get()), counter = of(ModBlocks.FOOD_COUNTER.get());
    private final BlockState exitSign = of(ModBlocks.EXIT_SIGN.get());
    private static final Block[] SIGNS = {Blocks.RED_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.LIME_CONCRETE,
            Blocks.BLUE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.BROWN_CONCRETE};
    private static final Block[] CARPETS = {Blocks.RED_WOOL, Blocks.BROWN_WOOL, Blocks.GRAY_WOOL, Blocks.BLUE_WOOL, Blocks.CYAN_WOOL, Blocks.LIGHT_GRAY_WOOL};
    /** Corridor tiles: main, border, accent (tones). */
    private static final int[][] PALETTES = {{0, 1, 5}, {0, 2, 3}, {1, 0, 5}, {4, 3, 2}, {0, 1, 2}};

    // ------------------------------------------------------------------ the plan

    public static Node node(long seed, int i, int j) {
        long h = hash(seed, i, j, 4000);
        int x = i * CELL + 18 + (int) Math.floorMod(h, 36L), z = j * CELL + 18 + (int) Math.floorMod(h >>> 8, 36L);
        double u = unit(Long.rotateLeft(h, 21));
        Court court = u < 0.42 ? Court.ATRIUM : u < 0.62 ? Court.FOUNTAIN : u < 0.8 ? Court.FOOD : Court.ANCHOR;
        int radius = court == Court.ANCHOR ? ANCHOR : 11 + (int) Math.floorMod(h >>> 16, 5L);
        return new Node(x, z, radius, court, h);
    }

    /** Is there a concourse from node (i, j) eastwards (dir 0) or southwards (dir 1)? Every court has at least one of the two. */
    public static boolean edge(long seed, int i, int j, int dir) {
        double east = unit(seed, i, j, 4001), south = unit(seed, i, j, 4002);
        return dir == 0 ? east < 0.8 || south >= 0.8 : south < 0.8;
    }

    private static int width(long h) {
        return switch ((int) Math.floorMod(h >>> 5, 4L)) {
            case 0 -> 3;
            case 1 -> 5;
            default -> 8;
        };
    }

    /** The point a fraction {@code t} along the concourse from (i, j) east (0) or south (1): {x, z, direction x, direction z, half-width}. */
    public static double[] edgePoint(long seed, int i, int j, int dir, double t) {
        Node a = node(seed, i, j), b = node(seed, i + (dir == 0 ? 1 : 0), j + (dir == 1 ? 1 : 0));
        long eh = hash(seed, i, j, dir, 4003);
        double mx = (a.x + b.x) / 2.0, mz = (a.z + b.z) / 2.0, len = Math.hypot(b.x - a.x, b.z - a.z);
        double bend = (unit(eh) - 0.5) * 44.0;
        double px = mx - (b.z - a.z) / len * bend, pz = mz + (b.x - a.x) / len * bend, it = 1 - t;
        double x = it * it * a.x + 2 * it * t * px + t * t * b.x, z = it * it * a.z + 2 * it * t * pz + t * t * b.z;
        double tx = 2 * it * (px - a.x) + 2 * t * (b.x - px), tz = 2 * it * (pz - a.z) + 2 * t * (b.z - pz), tl = Math.hypot(tx, tz);
        return new double[]{x, z, tx / tl, tz / tl, width(eh)};
    }

    public Spot spot(long seed, int x, int z) {
        int ci = Math.floorDiv(x, CELL), cj = Math.floorDiv(z, CELL);
        // anchors first: their walls cut across everything
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int j = cj - 1; j <= cj + 1; j++) {
                Node n = node(seed, i, j);
                if (n.court != Court.ANCHOR) continue;
                int dx = Math.abs(x - n.x), dz = Math.abs(z - n.z);
                if (dx < ANCHOR && dz < ANCHOR) return new Spot(Kind.ANCHOR, -1, n, 0, 0, 0, 0, 0, 0, false);
                if (dx <= ANCHOR && dz <= ANCHOR) {
                    Spot through = corridors(seed, x, z, ci, cj, true);
                    return new Spot(Kind.ANCHOR_WALL, through.outside, n, through.s, through.width, through.along, through.nx, through.nz, through.edge, false);
                }
            }
        }
        double outside = Double.MAX_VALUE;
        Node court = null;
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int j = cj - 1; j <= cj + 1; j++) {
                Node n = node(seed, i, j);
                if (n.court == Court.ANCHOR) continue;
                double d = Math.hypot(x - n.x, z - n.z) - n.radius;
                if (d < outside) {
                    outside = d;
                    court = n;
                }
            }
        }
        if (outside <= 0) return new Spot(Kind.COURT, outside, court, 0, 0, 0, 0, 0, 0, false);
        Spot corridor = corridors(seed, x, z, ci, cj, false);
        if (corridor.kind == Kind.CORRIDOR) return corridor;
        outside = Math.min(outside, corridor.outside);
        Kind kind = outside <= 1 ? Kind.STOREFRONT : outside <= SHOP_DEPTH + 1 ? Kind.SHOP : Kind.BACK;
        return new Spot(kind, outside, court, corridor.s, corridor.width, corridor.along, corridor.nx, corridor.nz, corridor.edge, false);
    }

    /** The nearest concourse: whether we're on it, and how far outside it. */
    private Spot corridors(long seed, int x, int z, int ci, int cj, boolean anyway) {
        double best = Double.MAX_VALUE, bestS = 0, bestAlong = 0, bnx = 0, bnz = 0;
        int bestW = 0;
        long bestEdge = 0;
        boolean well = false;
        for (int i = ci - 2; i <= ci + 1; i++) {
            for (int j = cj - 2; j <= cj + 1; j++) {
                for (int dir = 0; dir < 2; dir++) {
                    if (!edge(seed, i, j, dir)) continue;
                    Node a = node(seed, i, j), b = node(seed, i + (dir == 0 ? 1 : 0), j + (dir == 1 ? 1 : 0));
                    long eh = hash(seed, i, j, dir, 4003);
                    int w = width(eh);
                    double mx = (a.x + b.x) / 2.0, mz = (a.z + b.z) / 2.0, len = Math.hypot(b.x - a.x, b.z - a.z);
                    double bend = (unit(eh) - 0.5) * 44.0;
                    double px = mx - (b.z - a.z) / len * bend, pz = mz + (b.x - a.x) / len * bend;     // the control point, off to one side
                    double reach = w + SHOP_DEPTH + 3;
                    if (x < Math.min(Math.min(a.x, b.x), px) - reach || x > Math.max(Math.max(a.x, b.x), px) + reach
                            || z < Math.min(Math.min(a.z, b.z), pz) - reach || z > Math.max(Math.max(a.z, b.z), pz) + reach) continue;
                    double lx = a.x, lz = a.z, along = 0;
                    for (int k = 1; k <= SAMPLES; k++) {
                        double t = k / (double) SAMPLES, it = 1 - t;
                        double qx = it * it * a.x + 2 * it * t * px + t * t * b.x, qz = it * it * a.z + 2 * it * t * pz + t * t * b.z;
                        double sx = qx - lx, sz = qz - lz, sl2 = sx * sx + sz * sz;
                        double f = sl2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - lx) * sx + (z - lz) * sz) / sl2));
                        double cx = lx + sx * f, cz = lz + sz * f;
                        double d = Math.hypot(x - cx, z - cz);
                        if (d - w < best) {
                            best = d - w;
                            bestS = d;
                            bestW = w;
                            bestAlong = along + Math.sqrt(sl2) * f;
                            bnx = cx - x;
                            bnz = cz - z;
                            bestEdge = eh;
                            // no light well right by the courts
                            well = w >= 8 && Math.hypot(cx - a.x, cz - a.z) > a.radius + 4 && Math.hypot(cx - b.x, cz - b.z) > b.radius + 4;
                        }
                        along += Math.sqrt(sl2);
                        lx = qx;
                        lz = qz;
                    }
                }
            }
        }
        Kind kind = best <= 0 ? Kind.CORRIDOR : Kind.BACK;
        return new Spot(kind, best, null, bestS, bestW, bestAlong, bnx, bnz, bestEdge, well);
    }

    // ------------------------------------------------------------------ the generator

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public int minStandY() {
        return LOW;
    }

    @Override
    public int maxStandY() {
        return UP;
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return SHOP_LOOT;
    }

    /** The mall's few exits: back to the lobby, home to the Infinite IKEA, out into the dead city, or round to the start. */
    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        double u = GenMath.unit(seed, pos.getX(), pos.getZ(), 4004);
        return u < 0.3 ? BackroomsLevel.SUNLIT_LOBBY : u < 0.45 ? BackroomsLevel.INFINITE_IKEA : u < 0.6 ? BackroomsLevel.ENDLESS_FIELD
                : u < 0.75 ? BackroomsLevel.ABANDONED_CITY : BackroomsLevel.LEVEL_0;
    }

    private BlockState tile(int tone) {
        return ModBlocks.MALL_TILE.get().defaultBlockState().setValue(MallBlocks.TONE, tone);
    }

    private static BlockState facing(BlockState state, Direction facing) {
        return state.setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        c[0] = bedrock;
        c[1] = base;
        LevelParts.fill(c, UP_CEIL + 1, HEIGHT - 2, wall);
        c[HEIGHT - 1] = bedrock;
        Spot spot = spot(seed, x, z);
        switch (spot.kind) {
            case CORRIDOR -> corridor(c, seed, x, z, spot);
            case COURT -> court(c, seed, x, z, spot);
            case ANCHOR -> anchor(c, seed, x, z, spot.node);
            case ANCHOR_WALL -> anchorWall(c, seed, x, z, spot);
            case STOREFRONT -> storefront(c, seed, x, z);
            case SHOP -> shop(c, seed, x, z, spot);
            default -> LevelParts.fill(c, FLOOR, UP_CEIL, wall);
        }
        return c;
    }

    /** Both floors, both ceilings, lit in a grid (some lights dead). */
    private void floors(BlockState[] c, long seed, int x, int z, int lowTone, int upTone, double dead) {
        c[FLOOR] = tile(lowTone);
        c[UP_FLOOR] = tile(upTone);
        boolean light = Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 2;
        long h = hash(seed, x, z, 4010);
        c[LOW_CEIL] = light ? LevelParts.fluorescent(unit(h) < dead) : ceiling;
        c[UP_CEIL] = light ? LevelParts.fluorescent(unit(Long.rotateLeft(h, 13)) < dead) : ceiling;
    }

    /** Open from the lower floor up to the skylights. */
    private void well(BlockState[] c, int x, int z) {
        c[LOW_CEIL] = null;
        c[UP_FLOOR] = null;
        for (int y = UP_CEIL; y < ROOF; y++) c[y] = null;
        c[ROOF] = Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0 ? ceiling : skylight;
    }

    // ------------------------------------------------------------------ concourses

    private void corridor(BlockState[] c, long seed, int x, int z, Spot spot) {
        int[] palette = PALETTES[(int) Math.floorMod(spot.edge >>> 9, (long) PALETTES.length)];
        double s = spot.s, w = spot.width;
        int tone = s > w - 1.2 ? palette[1] : Math.floorMod((int) Math.floor(spot.along), 12) < 2 ? palette[2] : palette[0];
        double dead = unit(Long.rotateLeft(spot.edge, 33)) < 0.3 ? 0.7 : 0.08;          // some stretches are mostly dark
        floors(c, seed, x, z, tone, s > w - 1.2 ? palette[1] : palette[0], dead);
        int along = (int) Math.floor(spot.along);
        Direction toMiddle = Direction.getNearest(spot.nx, 0, spot.nz);
        if (spot.well && s <= w - 4) {
            int bridge = Math.floorMod(along, 40);
            if (bridge < 4) return;                                             // a bridge across the light well
            if (s <= w - 5 && bridge != 4 && bridge != 39) {
                well(c, x, z);
                decor(c, seed, x, z, LOW, s, along, toMiddle, true);
                return;
            }
            c[UP] = railing;
            c[UP_CEIL] = null;
            for (int y = UP_CEIL; y < ROOF; y++) c[y] = null;
            c[ROOF] = ceiling;
            decor(c, seed, x, z, LOW, s, along, toMiddle, true);
            return;
        }
        decor(c, seed, x, z, LOW, s, along, toMiddle, spot.well);
        decor(c, seed, x, z, UP, s, along + 7, toMiddle, spot.well);
    }

    /** Benches, planters, bins and the odd gumball machine, spaced down the concourse. */
    private void decor(BlockState[] c, long seed, int x, int z, int y, double s, int along, Direction toMiddle, boolean wide) {
        int slot = Math.floorMod(along, 18);
        long h = hash(seed, Math.floorDiv(along, 18), y, 4011);
        if (!wide && slot >= 8 && slot <= 10) {
            int kind = (int) Math.floorMod(h, 4L);
            if (kind == 0 && s < 1.6) planter(c, y, s < 0.6 && slot == 9);
            else if (kind == 1 && s < 0.6 && slot == 9) c[y] = gumball;
            else if (kind == 2 && s >= 1.0 && s < 2.0) c[y] = facing(bench, toMiddle.getOpposite());
            else if (kind == 3 && s < 0.6 && slot == 9) c[y] = trash;
            return;
        }
        if (wide && Math.abs(s - 2.5) < 0.5 && slot == 9) c[y] = facing(bench, toMiddle.getOpposite());
        if (wide && Math.abs(s - 2.5) < 0.5 && slot == 12) c[y] = trash;
    }

    private void planter(BlockState[] c, int y, boolean middle) {
        if (middle) {
            c[y - 1] = of(Blocks.GRASS_BLOCK);
            c[y] = of(Blocks.FLOWERING_AZALEA);
        } else {
            c[y] = of(Blocks.SMOOTH_SANDSTONE);
        }
    }

    // ------------------------------------------------------------------ courts

    private void court(BlockState[] c, long seed, int x, int z, Spot spot) {
        Node n = spot.node;
        int dx = x - n.x, dz = z - n.z;
        double d = Math.hypot(dx, dz), r = n.radius;
        // a pattern on the floor: rings and rays out from the middle
        int ring = (int) Math.floor(d / 3.0);
        double angle = Math.atan2(dz, dx);
        boolean ray = Math.floorMod((int) Math.floor((angle + Math.PI) / (Math.PI / 6)), 2) == 0;
        int tone = d > r - 1.5 ? 1 : ring % 2 == 0 ? (ray ? 0 : 4) : (ray ? 5 : 0);
        floors(c, seed, x, z, n.court == Court.FOOD ? 1 : tone, 0, 0.1);
        double hole = n.court == Court.FOOD ? r - 6 : r - 4;
        boolean escalator = n.court == Court.ATRIUM && dx >= r - 13 && dx <= r - 5 && dz >= -2 && dz <= 1;
        if (escalator) {
            escalator(c, dx - (int) (r - 12), dz);
            return;
        }
        boolean landing = n.court == Court.ATRIUM && dx >= r - 5 && dx <= r - 2 && dz >= -1 && dz <= 0;
        if (d <= hole && !landing) {
            well(c, x, z);
        } else if (d <= hole + 1 && !landing) {
            c[UP] = railing;
            for (int y = UP_CEIL; y < ROOF; y++) c[y] = null;
            c[ROOF] = ceiling;
        }
        switch (n.court) {
            case FOUNTAIN -> fountain(c, d, dx, dz, r);
            case FOOD -> foodCourt(c, x, z, d, dx, dz, r);
            default -> atrium(c, d, dx, dz, r);
        }
    }

    /** Eight steps up from the lower floor to the upper one, with glass sides. dz -1 and 0 are the steps. */
    private void escalator(BlockState[] c, int i, int dz) {
        for (int y = LOW_CEIL; y < ROOF; y++) c[y] = null;
        c[ROOF] = ceiling;
        if (i < 0) return;
        int top = LOW + i;
        if (dz == -2 || dz == 1) {
            LevelParts.fill(c, LOW, Math.min(top, UP_FLOOR), of(Blocks.BLACK_CONCRETE));
            if (top + 1 <= UP) c[top + 1] = railing;
            return;
        }
        LevelParts.fill(c, LOW, top - 1, of(Blocks.BLACK_CONCRETE));
        c[top] = escalator.setValue(StairBlock.FACING, Direction.EAST);
    }

    private void atrium(BlockState[] c, double d, int dx, int dz, double r) {
        // planters and benches in a ring round the lower court, under the upper walkway
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            long px = Math.round(Math.cos(a) * (r - 2.5)), pz = Math.round(Math.sin(a) * (r - 2.5));
            double near = Math.hypot(dx - px, dz - pz);
            if (k % 2 == 0 && near <= 1.2) {
                planter(c, LOW, near < 0.5);
                return;
            }
            if (k % 2 == 1 && near < 0.5) {
                c[LOW] = facing(bench, Direction.getNearest(-px, 0, -pz));
                return;
            }
        }
        if (d < 1.5) c[LOW] = gumball;
    }

    /** A dry-ish fountain under the skylights, ringed with palms. */
    private void fountain(BlockState[] c, double d, int dx, int dz, double r) {
        if (d < 3.5) {
            c[FLOOR] = of(Blocks.WATER);
            if (d < 1) {
                LevelParts.fill(c, FLOOR, LOW + 1, of(Blocks.QUARTZ_PILLAR));
                c[LOW + 2] = of(Blocks.WATER);
            }
            return;
        }
        if (d < 4.5) {
            c[LOW] = of(Blocks.QUARTZ_BLOCK);
            return;
        }
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2, px = Math.cos(a) * 7, pz = Math.sin(a) * 7;
            double near = Math.hypot(dx - Math.round(px), dz - Math.round(pz));
            if (near < 0.5) {
                c[FLOOR] = of(Blocks.GRASS_BLOCK);
                LevelParts.fill(c, LOW, LOW + 9, of(Blocks.JUNGLE_LOG));
                c[LOW + 10] = leaves();
                return;
            }
            if (near < 1.5) {
                c[FLOOR] = of(Blocks.GRASS_BLOCK);
                c[LOW] = of(Blocks.FERN);
                return;
            }
            // the fronds
            int fx = Math.abs(dx - (int) Math.round(px)), fz = Math.abs(dz - (int) Math.round(pz));
            if ((fx == 0 && fz <= 3 || fz == 0 && fx <= 3 || fx == fz && fx <= 2) && fx + fz > 0) {
                c[LOW + 9 - Math.max(fx, fz) / 2] = leaves();
            }
        }
    }

    private static BlockState leaves() {
        return Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private void foodCourt(BlockState[] c, int x, int z, double d, int dx, int dz, double r) {
        if (d > r - 1.5) {
            // the counters round the edge, facing in
            if (Math.floorMod((int) Math.round(Math.atan2(dz, dx) * r), 5) != 0) {
                c[LOW] = facing(counter, Direction.getNearest(-dx, 0, -dz));
            }
            return;
        }
        if (d > r - 3) return;
        int tx = Math.floorMod(x, 4), tz = Math.floorMod(z, 4);
        if (tx == 0 && tz == 0) c[LOW] = table;
        else if (tx == 0 && (tz == 1 || tz == 3)) c[LOW] = facing(chair, tz == 1 ? Direction.NORTH : Direction.SOUTH);
        else if (tz == 0 && (tx == 1 || tx == 3)) c[LOW] = facing(chair, tx == 1 ? Direction.WEST : Direction.EAST);
    }

    // ------------------------------------------------------------------ the anchor stores

    private void anchor(BlockState[] c, long seed, int x, int z, Node n) {
        int dx = x - n.x, dz = z - n.z;
        double patch = noise(seed, x, z, 11.0, 4020);
        int carpet = (int) Math.floorMod(hash(seed, Math.floorDiv(x, 11), Math.floorDiv(z, 11), 4021), (long) CARPETS.length);
        c[FLOOR] = patch > 0.45 ? of(CARPETS[carpet]) : tile(Math.abs(dx) + Math.abs(dz) < 6 ? 5 : 0);
        boolean light = Math.floorMod(x, 6) == 0 && Math.floorMod(z, 6) == 3;
        c[UP_CEIL] = light ? LevelParts.fluorescent(unit(hash(seed, x, z, 4022)) < 0.65) : ceiling;    // mostly dark
        if (Math.floorMod(dx, 8) == 4 && Math.floorMod(dz, 8) == 4) {
            // pillars, with a coloured band
            LevelParts.fill(c, LOW, UP_CEIL - 1, of(Blocks.QUARTZ_BLOCK));
            c[LOW + 5] = of(SIGNS[(int) Math.floorMod(n.hash, (long) SIGNS.length)]);
            c[LOW + 6] = of(SIGNS[(int) Math.floorMod(n.hash >>> 4, (long) SIGNS.length)]);
            return;
        }
        // glass display counters here and there
        long h = hash(seed, Math.floorDiv(x, 7), Math.floorDiv(z, 7), 4023);
        int lx = Math.floorMod(x, 7), lz = Math.floorMod(z, 7);
        if (unit(h) < 0.25 && (lx == 2 || lx == 4) && lz >= 2 && lz <= 4) {
            c[LOW] = of(Blocks.SMOOTH_QUARTZ);
            c[LOW + 1] = of(Blocks.GLASS);
        }
        // the way out, at the back
        if (dx == ANCHOR - 1 && dz == 0) c[LOW + 3] = StoreBlocks.ExitSign.pointing(exitSign, Direction.SOUTH);
    }

    private void anchorWall(BlockState[] c, long seed, int x, int z, Spot spot) {
        Node n = spot.node;
        int dx = x - n.x, dz = z - n.z;
        c[FLOOR] = tile(1);
        LevelParts.fill(c, LOW, UP_CEIL, wall);
        if (spot.outside <= 0) {
            // where a concourse meets it: a wide opening on the lower floor, a balcony above
            for (int y = LOW; y <= LOW + 4; y++) c[y] = null;
            c[LOW + 5] = of(SIGNS[(int) Math.floorMod(n.hash >>> 8, (long) SIGNS.length)]);
            c[LOW_CEIL] = ceiling;
            c[UP_FLOOR] = tile(1);
            for (int y = UP; y < UP_CEIL; y++) c[y] = null;
            c[UP] = railing;
            return;
        }
        if (dx == ANCHOR && dz == 0) LevelParts.exitDoor(c, LOW, Direction.WEST);
    }

    // ------------------------------------------------------------------ shops

    private static long shopHash(long seed, int x, int z, int floor) {
        return hash(seed, Math.floorDiv(x, SHOP_GRID), Math.floorDiv(z, SHOP_GRID), floor, 4030);
    }

    private void storefront(BlockState[] c, long seed, int x, int z) {
        c[FLOOR] = tile(1);
        c[UP_FLOOR] = tile(1);
        for (int level = 0; level < 2; level++) {
            int y0 = level == 0 ? LOW : UP, top = level == 0 ? LOW_CEIL - 1 : UP_CEIL - 1;
            long h = shopHash(seed, x, z, level);
            boolean closed = unit(h) < 0.3;
            boolean door = Math.floorMod(x, SHOP_GRID) >= 5 && Math.floorMod(x, SHOP_GRID) <= 8 || Math.floorMod(z, SHOP_GRID) >= 5 && Math.floorMod(z, SHOP_GRID) <= 8;
            for (int y = y0; y < top; y++) c[y] = closed ? shutter : door && y < y0 + 4 ? null : glass;
            c[top] = of(SIGNS[(int) Math.floorMod(h >>> 8, (long) SIGNS.length)]);     // the sign band
        }
        c[LOW_CEIL] = wall;
        c[UP_CEIL] = wall;
    }

    private void shop(BlockState[] c, long seed, int x, int z, Spot spot) {
        int gx = Math.floorMod(x, SHOP_GRID), gz = Math.floorMod(z, SHOP_GRID);
        boolean partition = gx == 0 || gz == 0;
        for (int level = 0; level < 2; level++) {
            int floorY = level == 0 ? FLOOR : UP_FLOOR, y0 = floorY + 1, ceil = level == 0 ? LOW_CEIL : UP_CEIL;
            long h = shopHash(seed, x, z, level);
            int floorKind = (int) Math.floorMod(h >>> 12, 4L);
            c[floorY] = floorKind == 0 ? of(CARPETS[(int) Math.floorMod(h >>> 16, (long) CARPETS.length)]) : floorKind == 1 ? of(Blocks.DARK_OAK_PLANKS)
                    : floorKind == 2 ? tile((int) Math.floorMod(h >>> 16, 6L)) : of(Blocks.GRAY_WOOL);
            boolean dark = unit(Long.rotateLeft(h, 40)) < 0.5;
            boolean light = Math.floorMod(x, 5) == 2 && Math.floorMod(z, 5) == 2;
            c[ceil] = light ? LevelParts.fluorescent(dark || unit(hash(seed, x, z, level, 4031)) < 0.3) : ceiling;
            if (partition || spot.outside > SHOP_DEPTH) {
                LevelParts.fill(c, y0, ceil - 1, wall);
                // now and then a way out, at the very back of a shop on the lower floor
                if (level == 0 && !partition && (gx == 7 || gz == 7) && unit(h) > 0.94) LevelParts.exitDoor(c, y0, doorFacing(seed, x, z));
                continue;
            }
            int kind = (int) Math.floorMod(h >>> 20, 6L);
            boolean byWall = gx == 1 || gz == 1 || gx == SHOP_GRID - 1 || gz == SHOP_GRID - 1;
            switch (kind) {
                case 0, 1 -> {
                }                                                                // empty
                case 2 -> {
                    if (byWall) {
                        c[y0] = of(Blocks.CHISELED_BOOKSHELF);
                        c[y0 + 1] = of(Blocks.CHISELED_BOOKSHELF);
                    } else if (gx % 4 == 2 && gz % 3 == 1) {
                        c[y0] = table;
                    }
                }
                case 3 -> {
                    if (gx % 4 == 3 && gz % 4 == 3) c[y0] = table;                // the clothes shops (the mannequins come later)
                }
                case 4 -> {
                    if ((gx == 3 || gz == 3) && gx >= 3 && gz >= 3 && gx <= 10 && gz <= 10) {
                        c[y0] = of(Blocks.SMOOTH_QUARTZ);                       // glass display counters
                        c[y0 + 1] = glass;
                    }
                }
                default -> {
                    if (byWall && gx % 3 == 1) c[y0] = gumball;                   // a sweet shop
                    else if (gx == 7 && gz == 7) c[y0] = crate;
                }
            }
            if (kind != 5 && gx == 12 && gz == 12 && unit(Long.rotateLeft(h, 50)) < 0.4) c[y0] = crate;   // stock left behind
        }
    }

    /** An exit door at the back of a shop faces into the shop (towards the concourse). */
    private Direction doorFacing(long seed, int x, int z) {
        Direction best = Direction.NORTH;
        double lowest = Double.MAX_VALUE;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            double o = spot(seed, x + d.getStepX(), z + d.getStepZ()).outside;
            if (o < lowest) {
                lowest = o;
                best = d;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ mannequins

    @Override
    public void populate(WorldGenRegion region, long seed) {
        ChunkPos chunk = region.getCenter();
        for (int x = chunk.getMinBlockX() + 1; x <= chunk.getMaxBlockX(); x += 4) {
            for (int z = chunk.getMinBlockZ() + 1; z <= chunk.getMaxBlockZ(); z += 4) {
                Spot spot = spot(seed, x, z);
                if (spot.kind != Kind.SHOP || spot.outside > SHOP_DEPTH) continue;
                int gx = Math.floorMod(x, SHOP_GRID), gz = Math.floorMod(z, SHOP_GRID);
                if (gx == 0 || gz == 0) continue;
                for (int level = 0; level < 2; level++) {
                    long h = shopHash(seed, x, z, level);
                    if (Math.floorMod(h >>> 20, 6L) != 3) continue;
                    int y = level == 0 ? LOW : UP;
                    BlockPos feet = new BlockPos(x, y, z);
                    if (!region.getBlockState(feet).isAir() || !region.getBlockState(feet.above()).isAir()) continue;
                    if (unit(hash(seed, x, z, level, 4042)) < 0.18) {
                        var forms = com.rickandmorty.entity.StillLifeForm.values();
                        var form = forms[(int) Math.floorMod(hash(seed, x, z, level, 4043), (long) forms.length)];
                        var still = com.rickandmorty.registry.ModEntities.STILL_LIVES.get(form).get().create(region.getLevel());
                        if (still != null && form != com.rickandmorty.entity.StillLifeForm.CAPTAIN) {
                            still.moveTo(x + 0.5, y, z + 0.5, (float) Math.floorMod(hash(seed, x, z, 4040), 360L), 0.0f);
                            still.setPersistenceRequired();
                            region.addFreshEntity(still);
                            continue;
                        }
                    }
                    ArmorStand stand = EntityType.ARMOR_STAND.create(region.getLevel());
                    if (stand == null) continue;
                    stand.moveTo(x + 0.5, y, z + 0.5, (float) Math.floorMod(hash(seed, x, z, 4040), 360L), 0.0f);
                    long look = hash(seed, x, z, level, 4041);
                    if (unit(look) < 0.6) stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                    if (unit(Long.rotateLeft(look, 9)) < 0.5) stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                    region.addFreshEntity(stand);
                }
            }
        }
    }
}
