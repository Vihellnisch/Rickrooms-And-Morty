package com.rickandmorty.worldgen.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.unit;

/**
 * The Abandoned City. A grid of city blocks long after everyone left:
 * cracked roads with grass and saplings pushing through, rusting cars, apartment blocks wrapped in
 * vines, ones that have half fallen down into heaps of rubble, ones swallowed whole by moss, glass
 * towers with their windows blown out, parks gone wild, a flooded foundation, a rusting Ferris wheel,
 * and everywhere giant trees, through roads and roofs alike. No one, nothing alive but the plants.
 * <p>
 * Streets run along the low edge of every {@link #CELL}-block square; the rest of the square is one
 * lot. You stand at y = {@link #G} on the street.
 */
public final class AbandonedCityLevel implements LevelGenerator {
    public static final int HEIGHT = 192, G = 40, FH = 4;
    public static final int CELL = 56, STREET = 12, LOT = CELL - STREET;

    public enum Kind {
        APARTMENT(21), TOWER(9), CRUMBLED(18), MOSS(10), LOWRISE(9), PARK(9), RUBBLE(6), POND(5), PARKING(6), FERRIS(3);

        final int weight;

        Kind(int weight) {
            this.weight = weight;
        }
    }

    /** One lot: what's on it, and for buildings where (lot-local, inclusive) and how tall. */
    public record Lot(int cx, int cz, Kind kind, int x0, int z0, int x1, int z1, int floors, int palette,
                      double collapseAngle, double collapseDepth, int holeX, int holeY, int holeZ, int holeR) {
        public int worldX(int ax) {
            return cx * CELL + STREET + ax;
        }

        public int worldZ(int az) {
            return cz * CELL + STREET + az;
        }

        public int top() {
            return G - 1 + floors * FH;
        }

        boolean inside(int ax, int az) {
            return ax >= x0 && ax <= x1 && az >= z0 && az <= z1;
        }
    }

    /** A giant tree: trunk at (x, z), and the branches and leaf clouds it holds up. */
    public record Tree(int x, int z, double radius, int height, int species, double phase, double[][] branches, double[][] blobs, double[] roots) {
    }

    // ------------------------------------------------------------------------------------- blocks

    private static BlockState b(Block block) {
        return block.defaultBlockState();
    }

    private static final BlockState GRASS = b(Blocks.GRASS_BLOCK), MOSS = b(Blocks.MOSS_BLOCK), MOSS_CARPET = b(Blocks.MOSS_CARPET),
            DIRT = b(Blocks.DIRT), COARSE = b(Blocks.COARSE_DIRT), STONE = b(Blocks.STONE), WATER = b(Blocks.WATER),
            ASPHALT = b(Blocks.GRAY_CONCRETE), WORN = b(Blocks.ANDESITE), TUFF = b(Blocks.TUFF), YELLOW = b(Blocks.YELLOW_TERRACOTTA),
            WHITE = b(Blocks.WHITE_CONCRETE), CURB = b(Blocks.POLISHED_ANDESITE), SLAB = b(Blocks.SMOOTH_STONE_SLAB),
            POST = b(Blocks.ANDESITE_WALL), BLACK = b(Blocks.BLACK_CONCRETE), IRON = b(Blocks.IRON_BARS);
    private static final BlockState[] RUBBLE = {b(Blocks.COBBLESTONE), b(Blocks.MOSSY_COBBLESTONE), b(Blocks.GRAVEL), b(Blocks.ANDESITE),
            b(Blocks.STONE_BRICKS), b(Blocks.CRACKED_STONE_BRICKS), b(Blocks.COBBLESTONE), b(Blocks.TUFF)};
    private static final BlockState[] CAR_COLORS = {b(Blocks.RED_TERRACOTTA), b(Blocks.WHITE_CONCRETE), b(Blocks.LIGHT_GRAY_CONCRETE),
            b(Blocks.GRAY_CONCRETE), b(Blocks.BROWN_TERRACOTTA), b(Blocks.CYAN_TERRACOTTA), b(Blocks.BLUE_TERRACOTTA)};

    /** Building materials: wall, trim (corners, floor bands, parapet), floors, windows. */
    private record Palette(BlockState wall, BlockState trim, BlockState floor, BlockState glass) {
    }

    private static final Palette[] PALETTES = {
            new Palette(b(Blocks.BRICKS), b(Blocks.SMOOTH_STONE), b(Blocks.POLISHED_ANDESITE), b(Blocks.GLASS_PANE)),
            new Palette(b(Blocks.LIGHT_GRAY_CONCRETE), b(Blocks.GRAY_CONCRETE), b(Blocks.SMOOTH_STONE), b(Blocks.GRAY_STAINED_GLASS_PANE)),
            new Palette(b(Blocks.WHITE_TERRACOTTA), b(Blocks.LIGHT_GRAY_TERRACOTTA), b(Blocks.SMOOTH_STONE), b(Blocks.GLASS_PANE)),
            new Palette(b(Blocks.STONE_BRICKS), b(Blocks.POLISHED_ANDESITE), b(Blocks.STONE), b(Blocks.GLASS_PANE)),
            new Palette(b(Blocks.WHITE_CONCRETE), b(Blocks.CYAN_TERRACOTTA), b(Blocks.SMOOTH_STONE), b(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE)),
            new Palette(b(Blocks.TERRACOTTA), b(Blocks.BROWN_TERRACOTTA), b(Blocks.POLISHED_ANDESITE), b(Blocks.GLASS_PANE)),
            new Palette(b(Blocks.MUD_BRICKS), b(Blocks.PACKED_MUD), b(Blocks.POLISHED_ANDESITE), b(Blocks.BROWN_STAINED_GLASS_PANE)),
    };
    private static final Palette[] TOWER_PALETTES = {
            new Palette(b(Blocks.LIGHT_GRAY_STAINED_GLASS), b(Blocks.GRAY_CONCRETE), b(Blocks.SMOOTH_STONE), b(Blocks.LIGHT_GRAY_STAINED_GLASS)),
            new Palette(b(Blocks.CYAN_STAINED_GLASS), b(Blocks.LIGHT_GRAY_CONCRETE), b(Blocks.SMOOTH_STONE), b(Blocks.CYAN_STAINED_GLASS)),
            new Palette(b(Blocks.BLACK_STAINED_GLASS), b(Blocks.BLACK_CONCRETE), b(Blocks.SMOOTH_STONE), b(Blocks.BLACK_STAINED_GLASS)),
            new Palette(b(Blocks.LIGHT_BLUE_STAINED_GLASS), b(Blocks.WHITE_CONCRETE), b(Blocks.SMOOTH_STONE), b(Blocks.LIGHT_BLUE_STAINED_GLASS)),
    };
    private static final Palette MOSSY = new Palette(MOSS, MOSS, MOSS, b(Blocks.AZALEA_LEAVES));

    private static final Block[][] SPECIES = {
            {Blocks.OAK_WOOD, Blocks.OAK_LEAVES}, {Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_LEAVES},
            {Blocks.JUNGLE_WOOD, Blocks.JUNGLE_LEAVES}, {Blocks.DARK_OAK_WOOD, Blocks.AZALEA_LEAVES}};

    /** How squashed each species' leaf clouds are (dark oaks spread flat, oaks and azaleas dome). */
    private static final double[] FLATNESS = {0.8, 0.5, 0.62, 0.75};

    private static BlockState leaves(Block block) {
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static BlockState pane(BlockState pane, boolean alongZ) {
        if (!(pane.getBlock() instanceof CrossCollisionBlock)) return pane;
        return alongZ ? pane.setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true)
                : pane.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
    }

    private static BlockState vine(Direction towards) {
        return Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(towards), true);
    }

    private static final BlockState HANGING_VINE = Blocks.VINE.defaultBlockState().setValue(VineBlock.UP, true);

    // ------------------------------------------------------------------------------------- the plan

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public int minStandY() {
        return G - 2;
    }

    /** You arrive standing in a street, somewhere along a block. */
    @Override
    public Vec3 landing(long seed, RandomSource random) {
        for (int attempt = 0; attempt < 50; attempt++) {
            int cx = random.nextIntBetweenInclusive(-60, 60), cz = random.nextIntBetweenInclusive(-60, 60);
            boolean ns = random.nextBoolean();
            int along = STREET + 4 + random.nextInt(LOT - 8);
            int x = cx * CELL + (ns ? 6 : along), z = cz * CELL + (ns ? along : 6);
            Integer y = standY(seed, x, z, G);
            if (y != null && y <= G + 1) return new Vec3(x + 0.5, y, z + 0.5);
        }
        return new Vec3(6.5, G + 1, 6.5);
    }

    /** One apartment block or shop row in three still has a red exit door in its front entrance. */
    public static boolean hasExit(long seed, Lot lot) {
        return (lot.kind() == Kind.APARTMENT || lot.kind() == Kind.LOWRISE) && unit(seed, lot.cx(), lot.cz(), 7020) < 0.35;
    }

    /** Where an exit door goes: mostly home; sometimes back into the Backrooms. */
    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        double u = unit(seed, pos.getX(), pos.getZ(), 7021);
        return u < 0.4 ? null : u < 0.65 ? BackroomsLevel.LEVEL_0 : u < 0.85 ? BackroomsLevel.ENDLESS_FIELD : BackroomsLevel.DEAD_MALL;
    }

    public Lot lot(long seed, int cx, int cz) {
        int total = 0;
        for (Kind k : Kind.values()) total += k.weight;
        int roll = (int) Math.floorMod(hash(seed, cx, cz, 7001), (long) total);
        Kind kind = Kind.APARTMENT;
        for (Kind k : Kind.values()) {
            if (roll < k.weight) {
                kind = k;
                break;
            }
            roll -= k.weight;
        }
        double r1 = unit(seed, cx, cz, 7002), r2 = unit(seed, cx, cz, 7003), r3 = unit(seed, cx, cz, 7004), r4 = unit(seed, cx, cz, 7005);
        int palette = (int) Math.floorMod(hash(seed, cx, cz, 7006), (long) PALETTES.length);
        int x0, z0, x1, z1, floors;
        switch (kind) {
            case TOWER -> {
                int m = 9 + (int) (r1 * 5);
                x0 = m;
                z0 = 9 + (int) (r2 * 5);
                x1 = LOT - 1 - (9 + (int) (r3 * 5));
                z1 = LOT - 1 - m;
                floors = 18 + (int) (r4 * 13);
                palette = (int) Math.floorMod(hash(seed, cx, cz, 7007), (long) TOWER_PALETTES.length);
            }
            case LOWRISE -> {
                x0 = 3;
                z0 = 3;
                x1 = LOT - 4;
                z1 = LOT - 4;
                floors = 2 + (int) (r4 * 2);
            }
            case RUBBLE -> {
                x0 = 6 + (int) (r1 * 4);
                z0 = 6 + (int) (r2 * 4);
                x1 = LOT - 7 - (int) (r3 * 4);
                z1 = LOT - 7 - (int) (r1 * 4);
                floors = 4;
            }
            default -> {
                x0 = 4 + (int) (r1 * 6);
                z0 = 4 + (int) (r2 * 6);
                x1 = LOT - 5 - (int) (r3 * 6);
                z1 = LOT - 5 - (int) (unit(seed, cx, cz, 7008) * 6);
                floors = kind == Kind.MOSS ? 4 + (int) (r4 * 6) : kind == Kind.CRUMBLED ? 7 + (int) (r4 * 8) : 5 + (int) (r4 * 9);
            }
        }
        double angle = unit(seed, cx, cz, 7009) * Math.PI * 2;
        double depth = kind == Kind.CRUMBLED ? 0.55 + unit(seed, cx, cz, 7010) * 0.35
                : kind == Kind.RUBBLE ? 0.9 : kind == Kind.TOWER && unit(seed, cx, cz, 7011) < 0.5 ? 0.12 + unit(seed, cx, cz, 7012) * 0.2 : 0.0;
        // a shell hole in the side of a crumbled building (or some apartments)
        int holeR = 0, holeX = 0, holeY = 0, holeZ = 0;
        if (kind == Kind.CRUMBLED || (kind == Kind.APARTMENT && unit(seed, cx, cz, 7013) < 0.3)) {
            holeR = 3 + (int) (unit(seed, cx, cz, 7014) * 3);
            boolean xFace = unit(seed, cx, cz, 7015) < 0.5, low = unit(seed, cx, cz, 7016) < 0.5;
            holeX = xFace ? (low ? x0 : x1) : x0 + 2 + (int) (unit(seed, cx, cz, 7017) * Math.max(1, x1 - x0 - 4));
            holeZ = xFace ? z0 + 2 + (int) (unit(seed, cx, cz, 7018) * Math.max(1, z1 - z0 - 4)) : (low ? z0 : z1);
            holeY = G + 4 + (int) (unit(seed, cx, cz, 7019) * Math.max(1, floors * FH * 0.45));
        }
        return new Lot(cx, cz, kind, x0, z0, x1, z1, floors, palette, angle, depth, holeX, holeY, holeZ, holeR);
    }

    /** The giant trees whose roots are in this cell (a park always has one in the middle). */
    public List<Tree> trees(long seed, int cx, int cz) {
        List<Tree> trees = new ArrayList<>(2);
        Kind kind = lot(seed, cx, cz).kind();
        boolean park = kind == Kind.PARK;
        int count = park ? 1 : unit(seed, cx, cz, 7101) < 0.42 ? 1 : 0;
        if (!park && kind != Kind.FERRIS && unit(seed, cx, cz, 7102) < 0.12) count++;
        for (int i = 0; i < count; i++) {
            int salt = 7110 + i * 40;
            int x, z;
            if (park && i == 0) {
                x = cx * CELL + STREET + 16 + (int) (unit(seed, cx, cz, salt) * 12);
                z = cz * CELL + STREET + 16 + (int) (unit(seed, cx, cz, salt + 1) * 12);
            } else {
                x = cx * CELL + (int) (unit(seed, cx, cz, salt) * CELL);
                z = cz * CELL + (int) (unit(seed, cx, cz, salt + 1) * CELL);
            }
            boolean mega = unit(seed, cx, cz, salt + 7) < (park ? 0.5 : 0.15);
            double radius = mega ? 5 + unit(seed, cx, cz, salt + 2) * 1.5 : 2.4 + unit(seed, cx, cz, salt + 2) * 2.2;
            int height = mega ? 68 + (int) (unit(seed, cx, cz, salt + 3) * 20) : 34 + (int) (unit(seed, cx, cz, salt + 3) * 26);
            int species = (int) Math.floorMod(hash(seed, cx, cz, salt + 4), (long) SPECIES.length);
            double phase = unit(seed, cx, cz, salt + 5) * 6.28;
            int nb = 5 + (int) (unit(seed, cx, cz, salt + 6) * 3);
            double[][] branches = new double[nb][];
            double[][] blobs = new double[nb + 1][];
            for (int k = 0; k < nb; k++) {
                double a = phase + k * (6.2832 / nb) + (unit(seed, cx, cz, salt + 10 + k) - 0.5) * 0.8;
                double sy = G + height * (0.5 + unit(seed, cx, cz, salt + 20 + k) * 0.35);
                double len = (7 + unit(seed, cx, cz, salt + 30 + k) * 8) * (mega ? 1.5 : 1) + radius;
                double ex = x + Math.cos(a) * len, ez = z + Math.sin(a) * len, ey = sy + len * 0.45;
                branches[k] = new double[]{x, sy, z, ex, ey, ez};
                double rx = (5.5 + unit(seed, cx, cz, salt + 35 + k) * 3.5) * (mega ? 1.35 : 1);
                blobs[k] = new double[]{ex, ey + 1.5, ez, rx, rx * FLATNESS[species]};
            }
            blobs[nb] = new double[]{x, G + height + 2, z, 8.5, 8.5 * FLATNESS[species] + 0.5};
            double[] roots = new double[7];
            for (int k = 0; k < roots.length; k++) roots[k] = phase * 1.7 + k * (6.2832 / roots.length) + unit(seed, cx, cz, salt + 50 + k) * 0.5;
            trees.add(new Tree(x, z, radius, height, species, phase, branches, blobs, roots));
        }
        return trees;
    }

    // ------------------------------------------------------------------------------------- columns

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        c[0] = b(Blocks.BEDROCK);
        for (int y = 1; y < G - 4; y++) c[y] = STONE;
        for (int y = G - 4; y < G - 1; y++) c[y] = DIRT;
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL), lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        boolean ns = lx < STREET, ew = lz < STREET;
        if (ns || ew) street(seed, c, x, z, lx, lz, ns, ew);
        else lotColumn(seed, c, x, z, lot(seed, cx, cz), lx - STREET, lz - STREET);
        streetFurniture(seed, c, x, z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (Tree tree : trees(seed, cx + dx, cz + dz)) giantTree(seed, c, x, z, tree);
            }
        }
        plants(seed, c, x, z);
        return c;
    }

    // ------------------------------------------------------------------------------------- streets

    private void street(long seed, BlockState[] c, int x, int z, int lx, int lz, boolean ns, boolean ew) {
        boolean roadNs = ns && lx >= 3 && lx <= 8, roadEw = ew && lz >= 3 && lz <= 8;
        if (roadNs || roadEw) {
            c[G - 1] = asphalt(seed, x, z);
            if (c[G - 1] == ASPHALT || c[G - 1] == WORN) {
                // markings, worn away in places
                boolean worn = unit(hash(seed, x, z, 8110)) < 0.5;
                boolean nearCrossNs = roadNs && !ew && (lz < STREET + 3 || lz >= CELL - 3);
                boolean nearCrossEw = roadEw && !ns && (lx < STREET + 3 || lx >= CELL - 3);
                if (!worn && ((nearCrossNs && lx % 2 == 1) || (nearCrossEw && lz % 2 == 1))) c[G - 1] = WHITE;
            }
            if (c[G - 1] == WATER) c[G - 2] = b(Blocks.MUD);
            if (roadNs && !ew) car(seed, c, x, z, lx, lz - STREET, true);
            if (roadEw && !ns) car(seed, c, x, z, lz, lx - STREET, false);
            return;
        }
        // sidewalk: raised slabs, broken up by grass and moss
        double green = noise(seed, x, z, 9.0, 8120);
        if (green > 0.66) {
            c[G - 1] = unit(hash(seed, x, z, 8121)) < 0.7 ? GRASS : MOSS;
            return;
        }
        double r = unit(hash(seed, x, z, 8122));
        c[G - 1] = r < 0.55 ? CURB : r < 0.75 ? b(Blocks.STONE_BRICKS) : r < 0.85 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.MOSSY_STONE_BRICKS);
        if (unit(hash(seed, x, z, 8123)) < 0.82) c[G] = SLAB;
        else if (unit(hash(seed, x, z, 8124)) < 0.5) c[G] = MOSS_CARPET;
    }

    private static BlockState asphalt(long seed, int x, int z) {
        double over = noise(seed, x, z, 21.0, 8101);
        if (over > 0.64) return unit(hash(seed, x, z, 8104)) < 0.85 ? GRASS : COARSE;
        double crack = noise(seed, x, z, 6.0, 8102);
        if (Math.abs(crack - 0.5) < 0.04 + over * 0.06) {
            double r = unit(hash(seed, x, z, 8105));
            return r < 0.5 ? GRASS : r < 0.75 ? MOSS : COARSE;
        }
        if (noise(seed, x, z, 4.0, 8103) > 0.88) return WATER;
        double r = unit(hash(seed, x, z, 8106));
        return r < 0.1 ? WORN : r < 0.16 ? TUFF : ASPHALT;
    }

    /** Wrecks parked in the two lanes: cars four long, and now and then a bus. */
    private void car(long seed, BlockState[] c, int x, int z, int across, int along, boolean nsRoad) {
        int lane = across <= 4 ? 0 : across >= 7 ? 1 : -1;
        if (lane < 0 || along < 4) return;
        int side = across - (lane == 0 ? 3 : 7);
        int cellA = nsRoad ? Math.floorDiv(x, CELL) : Math.floorDiv(z, CELL), cellB = nsRoad ? Math.floorDiv(z, CELL) : Math.floorDiv(x, CELL);
        int k = (along - 4) / 8, off = (along - 4) % 8;
        if (k > 4) return;
        int salt = nsRoad ? 8200 : 8300;
        long h = hash(seed, cellA * 3 + lane, cellB, k, salt);
        boolean bus = k % 2 == 0 && k < 4 && unit(hash(seed, cellA * 3 + lane, cellB, k, salt + 1)) < 0.08;
        if (bus) {
            int boff = along - 4 - 8 * k;
            if (boff > 10) return;
            BlockState body = unit(h) < 0.5 ? YELLOW : b(Blocks.WHITE_TERRACOTTA);
            c[G] = body;
            c[G + 1] = boff == 0 || boff == 10 ? body : unit(hash(seed, x, z, salt + 2)) < 0.4 ? null : b(Blocks.GRAY_STAINED_GLASS);
            c[G + 2] = body;
            if (noise(seed, x, z, 3.0, salt + 3) > 0.45) c[G + 3] = MOSS_CARPET;
            return;
        }
        if (unit(h) > 0.2 || off > 3) return;
        BlockState body = CAR_COLORS[(int) Math.floorMod(hash(seed, cellA, cellB, k * 2 + lane, salt + 4), (long) CAR_COLORS.length)];
        c[G] = body;
        if (off == 1 || off == 2) {
            c[G + 1] = unit(hash(seed, x, z, salt + 5)) < 0.25 ? null : b(Blocks.GRAY_STAINED_GLASS);
            if (noise(seed, x, z, 2.5, salt + 6) > 0.5) c[G + 2] = MOSS_CARPET;
        } else if (unit(hash(seed, x, z, salt + 7)) < 0.55) {
            c[G + 1] = MOSS_CARPET;
        }
    }

    /** Trees in pits along the sidewalks, dead lamp posts, traffic lights at the corners. */
    private void streetFurniture(long seed, BlockState[] c, int x, int z) {
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        for (int i = cx - 1; i <= cx + 1; i++) {
            for (int line : new int[]{1, 10}) {
                int lineX = i * CELL + line;
                if (Math.abs(lineX - x) > 3) continue;
                for (int j = cz - 1; j <= cz + 1; j++) {
                    for (int k = 0; k < 3; k++) {
                        int treeZ = j * CELL + STREET + 7 + 14 * k;
                        if (Math.abs(treeZ - z) <= 3) streetTree(seed, c, x, z, lineX, treeZ);
                        if (k < 2) {
                            int lampZ = j * CELL + STREET + 14 + 14 * k;
                            lamp(seed, c, x, z, lineX, lampZ, line == 1 ? 1 : -1, true);
                        }
                    }
                }
            }
        }
        for (int j = cz - 1; j <= cz + 1; j++) {
            for (int line : new int[]{1, 10}) {
                int lineZ = j * CELL + line;
                if (Math.abs(lineZ - z) > 3) continue;
                for (int i = cx - 1; i <= cx + 1; i++) {
                    for (int k = 0; k < 3; k++) {
                        int treeX = i * CELL + STREET + 7 + 14 * k;
                        if (Math.abs(treeX - x) <= 3) streetTree(seed, c, x, z, treeX, lineZ);
                        if (k < 2) {
                            int lampX = i * CELL + STREET + 14 + 14 * k;
                            lamp(seed, c, x, z, lampX, lineZ, line == 1 ? 1 : -1, false);
                        }
                    }
                }
            }
        }
        // traffic lights on two corners of every crossing
        int lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        if ((lx == 1 && lz == 1) || (lx == 10 && lz == 10)) {
            long h = hash(seed, cx, cz, lx, 8400);
            if (unit(h) < 0.7) {
                int tall = unit(h) < 0.15 ? 2 : 5;
                for (int y = G; y < G + tall; y++) c[y] = POST;
                if (tall == 5) {
                    c[G + 5] = BLACK;
                    c[G + 6] = BLACK;
                }
            }
        }
    }

    private void streetTree(long seed, BlockState[] c, int x, int z, int tx, int tz) {
        long h = hash(seed, tx, tz, 8500);
        if (unit(h) > 0.75) return;
        boolean dead = unit(hash(seed, tx, tz, 8501)) < 0.18, birch = unit(hash(seed, tx, tz, 8502)) < 0.3;
        int trunk = 5 + (int) (unit(hash(seed, tx, tz, 8503)) * 3);
        if (x == tx && z == tz) {
            c[G - 1] = GRASS;
            c[G] = null;
            for (int y = G; y < G + trunk; y++) c[y] = b(birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG);
            return;
        }
        if (dead) return;
        BlockState leaf = leaves(birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES);
        double cy = G + trunk - 0.5;
        for (int y = G + trunk - 3; y <= G + trunk + 2; y++) {
            double d = (x - tx) * (x - tx) / 7.5 + (z - tz) * (z - tz) / 7.5 + (y - cy) * (y - cy) / 4.5;
            if (d < 1.0 - unit(hash(seed, x, y, z, 8504)) * 0.35 && c[y] == null) c[y] = leaf;
        }
    }

    private void lamp(long seed, BlockState[] c, int x, int z, int px, int pz, int towardsRoad, boolean nsLine) {
        long h = hash(seed, px, pz, 8600);
        if (unit(h) > 0.8) return;
        int tall = unit(h) < 0.2 ? 2 : 6;
        if (x == px && z == pz) {
            for (int y = G; y < G + tall; y++) c[y] = POST;
            if (tall == 6) c[G + 6] = CURB;
        } else if (tall == 6 && ((nsLine && x == px + towardsRoad && z == pz) || (!nsLine && z == pz + towardsRoad && x == px))) {
            c[G + 6] = b(Blocks.POLISHED_ANDESITE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            c[G + 5] = Blocks.IRON_TRAPDOOR.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
        }
    }

    // ------------------------------------------------------------------------------------- lots

    private void lotColumn(long seed, BlockState[] c, int x, int z, Lot lot, int ax, int az) {
        double ground = noise(seed, x, z, 11.0, 8700);
        c[G - 1] = ground > 0.78 ? MOSS : ground < 0.16 ? COARSE : ground < 0.22 ? b(Blocks.PODZOL) : GRASS;
        switch (lot.kind()) {
            case PARK -> park(seed, c, x, z, ax, az);
            case POND -> pond(seed, c, x, z, ax, az);
            case PARKING -> parking(seed, c, x, z, ax, az);
            case FERRIS -> ferris(seed, c, x, z, ax, az, lot);
            default -> building(seed, c, x, z, lot, ax, az);
        }
    }

    /** How high the building still stands here (crumbled ones are broken off on a slant, jagged). */
    private int standsTo(long seed, Lot lot, int ax, int az) {
        int top = lot.top() + 1;
        if (lot.collapseDepth() <= 0) return top;
        double mx = (lot.x0() + lot.x1()) / 2.0, mz = (lot.z0() + lot.z1()) / 2.0;
        double half = Math.max(4, Math.max(lot.x1() - lot.x0(), lot.z1() - lot.z0()) / 2.0);
        double p = ((ax - mx) * Math.cos(lot.collapseAngle()) + (az - mz) * Math.sin(lot.collapseAngle())) / half;   // -1..1
        double fall = Mth.clamp((p + 0.6) / 1.6, 0, 1);
        int x = lot.worldX(ax), z = lot.worldZ(az);
        double jag = noise(seed, x, z, 3.0, 8800) * 5 + noise(seed, x, z, 9.0, 8801) * 6;
        int span = top - G;
        return (int) (top - span * lot.collapseDepth() * fall - jag + (lot.kind() == Kind.RUBBLE ? 2 : 0));
    }

    private void building(long seed, BlockState[] c, int x, int z, Lot lot, int ax, int az) {
        Kind kind = lot.kind();
        boolean moss = kind == Kind.MOSS, tower = kind == Kind.TOWER, low = kind == Kind.LOWRISE;
        Palette p = tower ? TOWER_PALETTES[lot.palette()] : moss ? MOSSY : PALETTES[lot.palette()];
        int top = lot.top();
        if (!lot.inside(ax, az)) {
            outside(seed, c, x, z, lot, ax, az, p);
            return;
        }
        boolean edgeX = ax == lot.x0() || ax == lot.x1(), edgeZ = az == lot.z0() || az == lot.z1();
        boolean edge = edgeX || edgeZ, corner = edgeX && edgeZ;
        int along = edgeX ? az - lot.z0() : ax - lot.x0();
        int length = edgeX ? lot.z1() - lot.z0() : lot.x1() - lot.x0();
        int limit = standsTo(seed, lot, ax, az);
        boolean pillar = tower ? along % 5 == 0 : low ? along % 6 == 0 : along % 3 == 0;
        int door = length / 2;
        for (int y = G - 1; y <= top + 1 && y <= limit; y++) {
            int k = (y - (G - 1)) / FH, r = (y - (G - 1)) % FH;
            BlockState s = null;
            if (y == top + 1) {
                if (edge) s = moss ? leaves(Blocks.AZALEA_LEAVES) : p.trim();
            } else if (r == 0) {
                s = edge ? p.trim() : k == lot.floors() ? roof(seed, x, z, p, moss) : p.floor();
                if (!edge && k > 0 && k < lot.floors() && noise(seed, x, z, 5.0, 8900 + k) > 0.74) s = null;      // fallen-through floor
            } else if (edge) {
                if (corner || pillar) s = p.trim();
                else if (k == 0 && (low ? r <= 3 : Math.abs(along - door) <= 1 && r <= 2)) {
                    s = low && r <= 3 && unit(hash(seed, x, y, z, 8901)) > 0.55 ? pane(p.glass(), edgeX) : null;   // shopfront / doorway
                } else if (tower || r <= 2) {
                    double broken = tower ? (noise(seed, x + y * 3, z - y * 3, 6.0, 8902) > 0.6 ? 1 : 0.12) : moss ? 0.12 : 0.45;
                    double grownOver = unit(hash(seed, x, y, z, 8904));
                    if (unit(hash(seed, x, y, z, 8903)) < broken) s = null;
                    else if (moss) s = grownOver < 0.6 ? MOSS : grownOver < 0.85 ? leaves(Blocks.AZALEA_LEAVES) : leaves(Blocks.FLOWERING_AZALEA_LEAVES);
                    else s = tower ? p.glass() : pane(p.glass(), edgeX);
                } else {
                    s = p.wall();
                }
            } else if (r == 1 && unit(hash(seed, x, y, z, 8905)) < 0.12) {
                s = MOSS_CARPET;                                          // moss creeping across the floors
            } else if (r == 1 && k == 0 && noise(seed, x, z, 4.0, 8906) > 0.7) {
                s = RUBBLE[(int) Math.floorMod(hash(seed, x, y, z, 8907), (long) RUBBLE.length)];
            }
            if (s != null && !moss) s = weather(seed, s, x, y, z);
            if (s != null && moss && s == MOSS && unit(hash(seed, x, y, z, 8908)) < 0.12) s = b(Blocks.MOSSY_COBBLESTONE);
            c[y] = s;
        }
        // the red exit door in the middle of the front
        if (az == lot.z0() && ax == lot.x0() + door && !corner && hasExit(seed, lot) && limit > G + 2) {
            c[G] = null;
            c[G + 1] = null;
            LevelParts.exitDoor(c, G, Direction.NORTH);
        }
        // a shell hole punched through the side
        if (lot.holeR() > 0) {
            for (int y = Math.max(G, lot.holeY() - lot.holeR()); y <= lot.holeY() + lot.holeR(); y++) {
                double d = Math.sqrt(sq(ax - lot.holeX()) + sq(y - lot.holeY()) + sq(az - lot.holeZ()));
                if (d < lot.holeR() - noise(seed, x + y, z, 2.0, 8910) * 1.5) c[y] = null;
            }
        }
        if (limit <= top) {
            // broken off: rubble on the floors that are left, rebar sticking out of the edges
            int y = Math.min(limit, top);
            while (y > G - 1 && c[y] == null) y--;
            if (y > G && (edge || (y - (G - 1)) % FH == 0) && unit(hash(seed, x, z, 8911)) < 0.18) {
                for (int i = 1; i <= 2 && y + i < HEIGHT; i++) c[y + i] = IRON;
            } else if (y > G && !edge && unit(hash(seed, x, z, 8912)) < 0.4) {
                c[y + 1] = unit(hash(seed, x, z, 8913)) < 0.5 ? MOSS_CARPET : rubble(seed, x, y + 1, z);
            }
            if (kind == Kind.CRUMBLED || kind == Kind.RUBBLE) rubbleInside(seed, c, x, z, lot, ax, az);
        } else {
            roofTop(seed, c, x, z, lot, ax, az, moss);
        }
    }

    private static double sq(double v) {
        return v * v;
    }

    private static BlockState roof(long seed, int x, int z, Palette p, boolean moss) {
        if (moss) return MOSS;
        double n = noise(seed, x, z, 6.0, 9000);
        return n > 0.55 ? (unit(hash(seed, x, z, 9001)) < 0.6 ? GRASS : MOSS) : n > 0.42 ? COARSE : p.floor();
    }

    /** Small trees seeded on the roof (lots on a moss building), and a leafy cap over the moss ones. */
    private void roofTop(long seed, BlockState[] c, int x, int z, Lot lot, int ax, int az, boolean moss) {
        int top = lot.top();
        for (int i = lot.x0() + 3; i <= lot.x1() - 3; i += 7) {
            for (int j = lot.z0() + 3; j <= lot.z1() - 3; j += 7) {
                if (Math.abs(i - ax) > 2 || Math.abs(j - az) > 2) continue;
                if (unit(hash(seed, lot.worldX(i), lot.worldZ(j), 9100)) > (moss ? 0.6 : lot.kind() == Kind.TOWER ? 0.25 : 0.3)) continue;
                int trunk = 3 + (int) (unit(hash(seed, lot.worldX(i), lot.worldZ(j), 9101)) * 3);
                BlockState leaf = leaves(moss ? Blocks.AZALEA_LEAVES : Blocks.OAK_LEAVES);
                if (i == ax && j == az) {
                    c[top] = GRASS;
                    for (int y = top + 1; y <= top + trunk; y++) c[y] = b(Blocks.OAK_LOG);
                }
                for (int y = top + trunk - 1; y <= top + trunk + 2; y++) {
                    double d = (sq(ax - i) + sq(az - j)) / 5.0 + sq(y - top - trunk - 0.5) / 3.0;
                    if (d < 1.0 - unit(hash(seed, x, y, z, 9102)) * 0.3 && c[y] == null) {
                        c[y] = unit(hash(seed, x, y, z, 9103)) < 0.15 ? leaves(Blocks.FLOWERING_AZALEA_LEAVES) : leaf;
                    }
                }
            }
        }
        if (moss) mossCap(seed, c, x, z, lot);
    }

    /** The leaf mound that has grown over a moss building, spilling a little over its edges. */
    private void mossCap(long seed, BlockState[] c, int x, int z, Lot lot) {
        int top = lot.top();
        double h = noise(seed, x, z, 7.0, 9200) * 4 - 1;
        for (int y = top + 1; y <= top + 1 + h; y++) {
            if (c[y] == null) c[y] = unit(hash(seed, x, y, z, 9201)) < 0.2 ? leaves(Blocks.FLOWERING_AZALEA_LEAVES) : leaves(Blocks.AZALEA_LEAVES);
        }
    }

    /** Next to a building: vines down its walls, balconies, awnings, the rubble it shed, the leaf cap's overhang. */
    private void outside(long seed, BlockState[] c, int x, int z, Lot lot, int ax, int az, Palette p) {
        Kind kind = lot.kind();
        Direction towards = null;
        int wallAx = ax, wallAz = az;
        if (az >= lot.z0() && az <= lot.z1()) {
            if (ax == lot.x0() - 1) {
                towards = Direction.EAST;
                wallAx = lot.x0();
            } else if (ax == lot.x1() + 1) {
                towards = Direction.WEST;
                wallAx = lot.x1();
            }
        } else if (ax >= lot.x0() && ax <= lot.x1()) {
            if (az == lot.z0() - 1) {
                towards = Direction.SOUTH;
                wallAz = lot.z0();
            } else if (az == lot.z1() + 1) {
                towards = Direction.NORTH;
                wallAz = lot.z1();
            }
        }
        if (towards == Direction.SOUTH && hasExit(seed, lot) && Math.abs(ax - (lot.x0() + (lot.x1() - lot.x0()) / 2)) <= 1) {
            // a worn path up to the exit door, kept clear
            c[G - 1] = b(Blocks.DIRT_PATH);
            return;
        }
        if (towards == null && hasExit(seed, lot) && az < lot.z0() && az >= lot.z0() - 4
                && Math.abs(ax - (lot.x0() + (lot.x1() - lot.x0()) / 2)) <= 1) {
            c[G - 1] = b(Blocks.DIRT_PATH);
            return;
        }
        if (towards != null) {
            int stands = Math.min(standsTo(seed, lot, wallAx, wallAz), lot.top() + 1);
            boolean alongZ = towards.getAxis() == Direction.Axis.X;
            int along = alongZ ? az - lot.z0() : ax - lot.x0();
            // balconies on apartments: a slab to stand on and a railing, every other window pair
            if (kind == Kind.APARTMENT && Math.floorMod(along, 6) >= 1 && Math.floorMod(along, 6) <= 2) {
                for (int k = 1; k < lot.floors(); k++) {
                    int y = G - 1 + k * FH;
                    if (y + 1 > stands) break;
                    c[y] = weather(seed, p.trim(), x, y, z);
                    if (unit(hash(seed, x, y, z, 9300)) < 0.85) c[y + 1] = pane(IRON, alongZ);
                    if (unit(hash(seed, x, y, z, 9301)) < 0.35) c[y + 2] = leaves(Blocks.OAK_LEAVES);    // a balcony plant gone wild
                }
            }
            if (kind == Kind.LOWRISE) {
                // a torn awning over the shopfront
                BlockState cloth = b(Math.floorMod(along, 4) < 2 ? Blocks.WHITE_WOOL : woolFor(lot));
                if (unit(hash(seed, x, z, 9310)) < 0.7) c[G + 3] = cloth;
            }
            double vines = noise(seed, x, z, 4.0, 9320);
            if (kind != Kind.TOWER && kind != Kind.MOSS) {
                // bushes and saplings growing out of the windows
                for (int k = 1; k < lot.floors(); k++) {
                    int y = G - 1 + k * FH + 1;
                    if (y > stands || noise(seed, x, z + k * 31, 3.0, 9325) < 0.72) continue;
                    c[y] = leaves(Blocks.OAK_LEAVES);
                    if (unit(hash(seed, x, y, z, 9326)) < 0.5) c[y + 1] = leaves(Blocks.OAK_LEAVES);
                }
            }
            if (vines > (kind == Kind.MOSS ? 0.5 : kind == Kind.TOWER ? 0.55 : 0.42)) {
                int len = (int) ((0.3 + unit(hash(seed, x, z, 9321)) * 0.9) * (stands - G));
                for (int y = stands - 1; y >= Math.max(G, stands - 1 - len); y--) if (c[y] == null) c[y] = vine(towards);
            }
        }
        if (kind == Kind.LOWRISE && towards == null) {
            int d = Math.max(Math.max(lot.x0() - ax, ax - lot.x1()), Math.max(lot.z0() - az, az - lot.z1()));
            if (d == 2 && (az >= lot.z0() && az <= lot.z1() || ax >= lot.x0() && ax <= lot.x1()) && unit(hash(seed, x, z, 9311)) < 0.6) {
                c[G + 2] = b(woolFor(lot));
            }
        }
        if (kind == Kind.MOSS) {
            int d = Math.max(Math.max(lot.x0() - ax, ax - lot.x1()), Math.max(lot.z0() - az, az - lot.z1()));
            if (d <= 2) {
                // the moss bulges out of the walls in lumps
                for (int y = G; y <= lot.top(); y++) {
                    double lump = noise(seed, x + y * 1.3, z - y * 1.1, 4.0, 9333) - (d - 1) * 0.22;
                    if (lump > 0.52 && c[y] == null) c[y] = unit(hash(seed, x, y, z, 9334)) < 0.15 ? leaves(Blocks.AZALEA_LEAVES) : MOSS;
                }
                for (int y = lot.top() + 1 - d; y <= lot.top() + 2 - d; y++) {
                    if (c[y] == null && noise(seed, x, z, 3.0, 9330) > 0.35) c[y] = leaves(Blocks.AZALEA_LEAVES);
                }
                // hanging curtains of leaves down from the overhang
                if (noise(seed, x, z, 2.5, 9331) > 0.55) {
                    int len = 3 + (int) (unit(hash(seed, x, z, 9332)) * 9);
                    for (int y = lot.top() - d; y > lot.top() - d - len && y > G; y--) if (c[y] == null) c[y] = HANGING_VINE;
                }
            }
        }
        if (kind == Kind.CRUMBLED || kind == Kind.RUBBLE) rubbleOutside(seed, c, x, z, lot, ax, az);
    }

    private static Block woolFor(Lot lot) {
        Block[] wool = {Blocks.RED_WOOL, Blocks.GREEN_WOOL, Blocks.BLUE_WOOL, Blocks.ORANGE_WOOL, Blocks.BROWN_WOOL};
        return wool[Math.floorMod(lot.cx() * 31 + lot.cz() * 17, wool.length)];
    }

    /** The heap a broken building shed: deepest where it fell, spilling out into the yard. */
    private void rubbleOutside(long seed, BlockState[] c, int x, int z, Lot lot, int ax, int az) {
        double mx = (lot.x0() + lot.x1()) / 2.0, mz = (lot.z0() + lot.z1()) / 2.0;
        double dx = Math.max(Math.max(lot.x0() - ax, ax - lot.x1()), 0), dz = Math.max(Math.max(lot.z0() - az, az - lot.z1()), 0);
        double dist = Math.sqrt(dx * dx + dz * dz);
        double vx = ax - mx, vz = az - mz, len = Math.max(1, Math.sqrt(vx * vx + vz * vz));
        double facing = (vx * Math.cos(lot.collapseAngle()) + vz * Math.sin(lot.collapseAngle())) / len;
        double height = (lot.kind() == Kind.RUBBLE ? 5 : 7) * Math.max(0, facing * 0.8 + 0.3) * (1 - dist / 9.0)
                + noise(seed, x, z, 3.0, 9400) * 2 - 0.8;
        heap(seed, c, x, z, G, (int) height);
    }

    private void rubbleInside(long seed, BlockState[] c, int x, int z, Lot lot, int ax, int az) {
        double height = noise(seed, x, z, 5.0, 9410) * 4 + (lot.kind() == Kind.RUBBLE ? 2 : 0) - 1;
        heap(seed, c, x, z, G, (int) height);
    }

    private void heap(long seed, BlockState[] c, int x, int z, int from, int height) {
        if (height <= 0) return;
        int y = from;
        for (int i = 0; i < height && y < HEIGHT - 1; i++, y++) {
            if (c[y] == null) c[y] = rubble(seed, x, y, z);
        }
        if (c[y] == null && unit(hash(seed, x, z, 9420)) < 0.55) c[y] = unit(hash(seed, x, z, 9421)) < 0.6 ? MOSS_CARPET : b(Blocks.SHORT_GRASS);
        if (c[y - 1] != null && unit(hash(seed, x, z, 9422)) < 0.35 && c[y - 1] != IRON) c[y - 1] = unit(hash(seed, x, z, 9423)) < 0.5 ? MOSS : GRASS;
    }

    private static BlockState rubble(long seed, int x, int y, int z) {
        return RUBBLE[(int) Math.floorMod(hash(seed, x, y, z, 9430), (long) RUBBLE.length)];
    }

    /** Moss and cracks, thickest near the ground. */
    private static BlockState weather(long seed, BlockState s, int x, int y, int z) {
        double low = Math.max(0, 1 - (y - G) / 7.0) * 0.25;
        double n = noise(seed, x + y * 0.8, z - y * 0.8, 8.0, 9500) + low;
        double r = unit(hash(seed, x, y, z, 9501));
        if (n > 0.8) return r < 0.55 ? MOSS : mossy(s);
        if (n > 0.68 && r < 0.35) return mossy(s);
        if (r < 0.05) return cracked(s);
        return s;
    }

    private static BlockState mossy(BlockState s) {
        if (s.is(Blocks.STONE_BRICKS) || s.is(Blocks.CRACKED_STONE_BRICKS)) return b(Blocks.MOSSY_STONE_BRICKS);
        if (s.is(Blocks.COBBLESTONE)) return b(Blocks.MOSSY_COBBLESTONE);
        if (s.getBlock() instanceof CrossCollisionBlock) return s;
        return s.canOcclude() ? MOSS : s;
    }

    private static BlockState cracked(BlockState s) {
        if (s.is(Blocks.STONE_BRICKS)) return b(Blocks.CRACKED_STONE_BRICKS);
        if (s.is(Blocks.BRICKS)) return b(Blocks.GRANITE);
        return s;
    }

    // ------------------------------------------------------------------------------------- open lots

    private void park(long seed, BlockState[] c, int x, int z, int ax, int az) {
        int mid = LOT / 2;
        // overgrown paths crossing in the middle, round a dry fountain
        boolean path = Math.abs(ax - mid) <= 1 || Math.abs(az - mid) <= 1 || Math.abs(ax - az) <= 1;
        double d = Math.sqrt(sq(ax - mid) + sq(az - mid));
        if (path && unit(hash(seed, x, z, 9600)) < 0.7) c[G - 1] = unit(hash(seed, x, z, 9601)) < 0.75 ? b(Blocks.DIRT_PATH) : COARSE;
        if (d < 6.6) {
            c[G - 1] = b(Blocks.STONE_BRICKS);
            if (d > 5.4) c[G] = weather(seed, b(Blocks.STONE_BRICK_WALL), x, G, z);
            else if (d > 1.5) {
                c[G - 1] = WATER;
                c[G - 2] = b(Blocks.MUD);
                if (unit(hash(seed, x, z, 9602)) < 0.3) c[G] = b(Blocks.LILY_PAD);
            } else {
                c[G] = b(Blocks.MOSSY_STONE_BRICKS);
                c[G + 1] = b(Blocks.MOSSY_STONE_BRICK_WALL);
                c[G + 2] = b(Blocks.MOSSY_STONE_BRICKS);
            }
            return;
        }
        // benches facing the paths
        if ((Math.abs(ax - mid) == 3 && Math.floorMod(az, 9) == 4) && unit(hash(seed, x, z, 9603)) < 0.6) {
            c[G] = b(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, ax > mid ? Direction.EAST : Direction.WEST);
        }
        // a hedge gone wild round the edge
        if ((ax <= 1 || az <= 1 || ax >= LOT - 2 || az >= LOT - 2) && noise(seed, x, z, 4.0, 9604) > 0.35) {
            c[G] = leaves(Blocks.OAK_LEAVES);
            if (noise(seed, x, z, 3.0, 9605) > 0.6) c[G + 1] = leaves(Blocks.OAK_LEAVES);
        }
    }

    private void pond(long seed, BlockState[] c, int x, int z, int ax, int az) {
        int edge = Math.min(Math.min(ax, az), Math.min(LOT - 1 - ax, LOT - 1 - az));
        if (edge < 3) {
            if (edge == 2 && noise(seed, x, z, 4.0, 9700) > 0.3) {
                int h = (int) (noise(seed, x, z, 3.0, 9701) * 5);
                for (int y = G; y < G + h; y++) c[y] = weather(seed, b(Blocks.STONE_BRICKS), x, y, z);
            }
            return;
        }
        // the foundation pit, flooded: murky water, weed, lily pads, reeds round the edge
        int depth = edge < 5 ? 2 : 4;
        c[G - 1] = null;
        for (int y = G - 1 - depth; y < G - 1; y++) c[y] = WATER;
        c[G - 2 - depth] = b(Blocks.MUD);
        c[G - 1 - depth] = unit(hash(seed, x, z, 9702)) < 0.4 ? b(Blocks.SEAGRASS) : WATER;
        if (unit(hash(seed, x, z, 9703)) < 0.22) c[G - 1] = b(Blocks.LILY_PAD);
        if (edge == 3 && unit(hash(seed, x, z, 9704)) < 0.45) {
            c[G - 2] = GRASS;
            c[G - 1] = b(Blocks.SUGAR_CANE);
            c[G] = b(Blocks.SUGAR_CANE);
        }
        // a car that rolled in
        if (ax >= 18 && ax <= 19 && az >= 20 && az <= 23) c[G - 3] = CAR_COLORS[0];
        // stumps of the old foundations poking out
        if ((ax % 9 == 0 || az % 11 == 0) && noise(seed, x, z, 3.0, 9705) > 0.72) {
            for (int y = G - 1 - depth; y < G + 1; y++) c[y] = weather(seed, b(Blocks.STONE_BRICKS), x, y, z);
        }
    }

    private void parking(long seed, BlockState[] c, int x, int z, int ax, int az) {
        c[G - 1] = asphalt(seed, x, z);
        int row = -1;
        int[] rows = {4, 12, 22, 30};
        for (int i = 0; i < rows.length; i++) if (az >= rows[i] && az < rows[i] + 4) row = i;
        if (row < 0) return;
        if (Math.floorMod(ax, 3) == 2) {
            if (c[G - 1] == ASPHALT && unit(hash(seed, x, z, 9800)) < 0.7) c[G - 1] = WHITE;
            return;
        }
        int slot = Math.floorDiv(ax, 3), off = az - rows[row];
        long h = hash(seed, x - Math.floorMod(ax, 3), rows[row], z - off, 9801);
        if (ax < 2 || ax > LOT - 3 || unit(h) > 0.3) return;
        BlockState body = CAR_COLORS[(int) Math.floorMod(h, (long) CAR_COLORS.length)];
        c[G] = body;
        if (off == 1 || off == 2) {
            c[G + 1] = unit(hash(seed, x, z, 9802)) < 0.25 ? null : b(Blocks.GRAY_STAINED_GLASS);
            if (noise(seed, x, z, 2.5, 9803) > 0.45) c[G + 2] = MOSS_CARPET;
        } else if (unit(hash(seed, x, z, 9804)) < 0.5) {
            c[G + 1] = MOSS_CARPET;
        }
    }

    /** A rusting Ferris wheel, stopped where it was, its gondolas hanging under the rim. */
    private void ferris(long seed, BlockState[] c, int x, int z, int ax, int az, Lot lot) {
        int hx = LOT / 2, hz = LOT / 2, radius = 16, hubY = G + radius + 5;
        double turn = unit(seed, lot.cx(), lot.cz(), 9900) * 0.5;
        BlockState rim = b(Blocks.YELLOW_TERRACOTTA), rust = b(Blocks.ORANGE_TERRACOTTA), leg = b(Blocks.LIGHT_GRAY_TERRACOTTA);
        if (Math.abs(ax - hx) <= 19 && Math.abs(az - hz) <= 5) c[G - 1] = unit(hash(seed, x, z, 9901)) < 0.6 ? b(Blocks.SMOOTH_STONE) : MOSS;
        // the two rims and their spokes
        if (az == hz - 1 || az == hz + 1) {
            for (int y = G; y <= hubY + radius + 1; y++) {
                double dx = ax - hx, dy = y - hubY, d = Math.sqrt(dx * dx + dy * dy);
                boolean on = Math.abs(d - radius) < 0.6;
                if (!on && d < radius && d > 1.5) {
                    for (int s = 0; s < 16; s++) {
                        double a = turn + s * Math.PI / 8;
                        double across = Math.abs(dx * Math.sin(a) - dy * Math.cos(a)), ahead = dx * Math.cos(a) + dy * Math.sin(a);
                        if (across < 0.5 && ahead > 0) {
                            on = true;
                            break;
                        }
                    }
                }
                if (on) c[y] = unit(hash(seed, x, y, z, 9902)) < 0.2 ? rust : rim;
            }
        }
        // the axle through the hub
        if (ax == hx && Math.abs(az - hz) <= 4) c[hubY] = b(Blocks.POLISHED_ANDESITE);
        if (Math.abs(az - hz) == 1 && Math.abs(ax - hx) <= 1) {
            for (int y = hubY - 1; y <= hubY + 1; y++) c[y] = b(Blocks.POLISHED_ANDESITE);
        }
        // A-frame legs on both sides, vines climbing them
        if (Math.abs(az - hz) == 4 || Math.abs(az - hz) == 5) {
            double run = 12.0, rise = hubY - G;
            for (int side : new int[]{-1, 1}) {
                double t0 = (ax - hx - 0.5 * side) * side / run, t1 = (ax - hx + 0.5 * side) * side / run;
                if (Math.max(t0, t1) < 0 || Math.min(t0, t1) > 1) continue;
                int yA = (int) Math.floor(hubY - rise * Mth.clamp(Math.max(t0, t1), 0, 1)), yB = (int) Math.ceil(hubY - rise * Mth.clamp(Math.min(t0, t1), 0, 1));
                for (int y = Math.max(G, yA); y <= Math.min(hubY, yB); y++) {
                    if (Math.abs(az - hz) == 4) c[y] = leg;
                    else if (y < G + 12 && noise(seed, x, z, 3.0, 9905) > 0.45 && c[y] == null) c[y] = vine(az < hz ? Direction.SOUTH : Direction.NORTH);
                }
            }
        }
        // the gondolas
        for (int g = 0; g < 12; g++) {
            double a = turn + g * Math.PI / 6;
            int px = (int) Math.round(hx + radius * Math.cos(a)), py = (int) Math.round(hubY + radius * Math.sin(a));
            if (Math.abs(ax - px) > 1 || Math.abs(az - hz) > 1) continue;
            BlockState shell = b(g % 3 == 0 ? Blocks.RED_TERRACOTTA : g % 3 == 1 ? Blocks.YELLOW_TERRACOTTA : Blocks.BLUE_TERRACOTTA);
            int bottom = py - 5;
            if (bottom < G) continue;
            if (ax == px && az == hz) c[py - 1] = b(Blocks.CHAIN);
            for (int y = bottom; y <= py - 2; y++) {
                boolean floorOrRoof = y == bottom || y == py - 2;
                boolean cornerPost = Math.abs(ax - px) == 1 && Math.abs(az - hz) == 1;
                if (floorOrRoof || cornerPost) c[y] = unit(hash(seed, x, y, z, 9903)) < 0.15 ? rust : shell;
                else c[y] = null;
            }
        }
    }

    // ------------------------------------------------------------------------------------- giant trees

    private void giantTree(long seed, BlockState[] c, int x, int z, Tree t) {
        double reach = 44;
        if (Math.abs(x - t.x()) > reach || Math.abs(z - t.z()) > reach) return;
        BlockState wood = b(SPECIES[t.species()][0]);
        BlockState leaf = leaves(SPECIES[t.species()][1]);
        int topY = G + t.height();
        // the trunk, flared at the foot, leaning a little this way and that as it goes up
        for (int y = G - 3; y <= topY; y++) {
            double h = (y - G) / (double) t.height();
            double ox = Math.sin(y / 11.0 + t.phase()) * 1.6 * h, oz = Math.cos(y / 13.0 + t.phase()) * 1.6 * h;
            double r = t.radius() * (1 - 0.45 * Math.max(0, h)) + 2.2 * Math.max(0, 1 - (y - G + 3) / 6.0);
            double d = Math.sqrt(sq(x - t.x() - ox) + sq(z - t.z() - oz));
            if (d <= r) c[y] = wood;
        }
        // roots snaking out across the ground
        for (double a : t.roots()) {
            double len = t.radius() + 6 + (Math.sin(a * 3.1) + 1) * 3;
            double dx = x - t.x(), dz = z - t.z();
            double ahead = dx * Math.cos(a) + dz * Math.sin(a), across = Math.abs(-dx * Math.sin(a) + dz * Math.cos(a));
            double wiggle = Math.sin(ahead * 0.5 + a * 5) * 0.8;
            if (ahead < t.radius() * 0.5 || ahead > len || Math.abs(across - wiggle) > 1.3 - ahead / len * 0.8) continue;
            double tt = ahead / len;
            int rootTop = (int) Math.round(G - 1 + 3.5 * Math.pow(1 - tt, 1.5));
            for (int y = G - 2; y <= rootTop; y++) c[y] = wood;
        }
        // branches, then the clouds of leaves at their ends and over the top
        for (double[] br : t.branches()) {
            double bx = br[3] - br[0], bz = br[5] - br[2], l2 = bx * bx + bz * bz;
            double tt = Mth.clamp(((x - br[0]) * bx + (z - br[2]) * bz) / l2, 0, 1);
            double px = br[0] + bx * tt, pz = br[2] + bz * tt;
            double thick = 1.6 - tt * 0.9;
            if (sq(x - px) + sq(z - pz) <= thick * thick) {
                int y = (int) Math.round(br[1] + (br[4] - br[1]) * tt);
                for (int dy = -1; dy <= (tt < 0.5 ? 1 : 0); dy++) c[y + dy] = wood;
            }
        }
        int lowest = Integer.MAX_VALUE;
        for (double[] blob : t.blobs()) {
            double hd = (sq(x - blob[0]) + sq(z - blob[2])) / sq(blob[3]);
            if (hd >= 1) continue;
            int y0 = (int) Math.floor(blob[1] - blob[4]), y1 = (int) Math.ceil(blob[1] + blob[4]);
            for (int y = y0; y <= y1 && y < HEIGHT; y++) {
                double d = hd + sq(y - blob[1]) / sq(blob[4]);
                if (d < 1 - noise(seed, x + y * 2.3, z - y * 1.7, 3.0, 9950) * 0.45 && c[y] == null) {
                    c[y] = unit(hash(seed, x, y, z, 9951)) < 0.06 ? leaves(Blocks.FLOWERING_AZALEA_LEAVES) : leaf;
                    lowest = Math.min(lowest, y);
                }
            }
        }
        // vines hanging from the canopy
        if (lowest != Integer.MAX_VALUE && noise(seed, x, z, 2.0, 9952) > 0.55) {
            int len = 3 + (int) (unit(hash(seed, x, z, 9953)) * 10);
            for (int y = lowest - 1; y > lowest - 1 - len && y > G; y--) {
                if (c[y] != null) break;
                c[y] = HANGING_VINE;
            }
        }
    }

    // ------------------------------------------------------------------------------------- plants

    /** Grass, ferns, flowers and bushes on any grass or moss open to the sky (streets, yards, roofs, heaps). */
    private void plants(long seed, BlockState[] c, int x, int z) {
        for (int y = G; y < HEIGHT - 2; y++) {
            if (c[y] != null || c[y - 1] == null) continue;
            BlockState below = c[y - 1];
            boolean soil = below == GRASS || below == MOSS || below.is(Blocks.PODZOL) || below == COARSE;
            if (!soil) continue;
            double r = unit(hash(seed, x, y, z, 9990));
            double lush = noise(seed, x, z, 13.0, 9991);
            if (below == MOSS && r < 0.45) c[y] = MOSS_CARPET;
            else if (r < 0.28 + lush * 0.25) c[y] = b(Blocks.SHORT_GRASS);
            else if (r < 0.4 + lush * 0.25 && c[y + 1] == null) {
                c[y] = b(Blocks.TALL_GRASS);
                c[y + 1] = b(Blocks.TALL_GRASS).setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
            } else if (r < 0.48 + lush * 0.25) c[y] = b(Blocks.FERN);
            else if (r < 0.52 + lush * 0.25 && c[y + 1] == null) {
                c[y] = b(Blocks.LARGE_FERN);
                c[y + 1] = b(Blocks.LARGE_FERN).setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
            } else if (r < 0.545 + lush * 0.25) {
                Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.AZURE_BLUET};
                c[y] = b(flowers[(int) Math.floorMod(hash(seed, x, y, z, 9992), (long) flowers.length)]);
            } else if (r < 0.57 + lush * 0.25) {
                c[y] = leaves(unit(hash(seed, x, y, z, 9993)) < 0.5 ? Blocks.AZALEA_LEAVES : Blocks.OAK_LEAVES);
            }
            if (below == COARSE && c[y] != null && c[y].is(Blocks.TALL_GRASS)) c[y] = b(Blocks.DEAD_BUSH);
        }
    }
}
