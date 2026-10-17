package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.StoreBlocks;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.pick;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * The Infinite IKEA (SCP-3008): a perfectly normal store that never ends, under a grey sky, in
 * a fog you can't see the end of. White walkways run in a grid between departments laid out like
 * showrooms: living rooms, bedrooms (with real beds), kitchens, offices, the market hall, the
 * self-serve warehouse with racking up into the fog, and now and then a restaurant whose
 * counters fill up again every morning. The hanging EXIT signs point every which way; only near
 * one of the very rare checkouts do they tell the truth, and only the checkouts have a way out.
 */
public final class InfiniteIkeaLevel implements LevelGenerator {
    /** A department plus the walkway along its west and north sides. */
    public static final int CELL = 32;
    public static final int WALK = 5;
    public static final int FLOOR = 3;
    /** Tall enough that the pillars go up into the fog and never seem to reach the roof. */
    static final int HEIGHT = 128;
    static final int SIGN_Y = FLOOR + 8;
    /** How far (in cells) the signs know the way to a checkout. */
    static final int SIGNS_KNOW = 8;
    private static final ResourceKey<LootTable> WAREHOUSE_STOCK = loot("ikea_warehouse"), MARKET_STOCK = loot("ikea_market"), KIDS_STOCK = loot("ikea_kids");
    private static final ResourceKey<LootTable> HARDWARE_STOCK = loot("ikea_hardware"), GARDEN_STOCK = loot("ikea_garden"), SEASONAL_STOCK = loot("ikea_seasonal");

    /** In this order, they're also the icons on the hanging department signs. */
    public enum Department {
        LIVING, BEDROOM, KITCHEN, OFFICE, MARKET, WAREHOUSE, RESTAURANT, CHECKOUT, KIDS, BATHROOM, LIGHTING, PLANTS, TEXTILES, BARGAIN, HARDWARE,
        /** The big halls, four departments in one. */
        BACKYARD, NATURE, SEASONAL;

        public boolean hall() {
            return this == BACKYARD || this == NATURE || this == SEASONAL;
        }
    }

    private static final Department[] HALLS = {Department.BACKYARD, Department.NATURE, Department.SEASONAL};

    private static final Department[] ROLL = {Department.LIVING, Department.BEDROOM, Department.KITCHEN, Department.OFFICE, Department.MARKET,
            Department.WAREHOUSE, Department.RESTAURANT, Department.KIDS, Department.BATHROOM, Department.LIGHTING, Department.PLANTS,
            Department.TEXTILES, Department.BARGAIN, Department.HARDWARE};
    private static final double[] SHARE = {0.10, 0.09, 0.08, 0.07, 0.09, 0.12, 0.05, 0.07, 0.07, 0.06, 0.07, 0.06, 0.06, 0.07};
    private final IkeaHalls halls = new IkeaHalls();

    private final BlockState bedrock = of(Blocks.BEDROCK), stone = of(Blocks.SMOOTH_STONE);
    private final BlockState floor = of(ModBlocks.STORE_FLOOR.get()), walkway = of(ModBlocks.STORE_WALKWAY.get());
    private final BlockState partition = of(ModBlocks.STORE_PARTITION.get()), chain = of(Blocks.CHAIN);
    private final BlockState sign = of(ModBlocks.EXIT_SIGN.get()), light = of(ModBlocks.STORE_LIGHT.get());
    private final BlockState sofa = of(ModBlocks.SOFA.get()), chair = of(ModBlocks.STORE_CHAIR.get());
    private final BlockState diningTable = of(ModBlocks.DINING_TABLE.get()), coffeeTable = of(ModBlocks.COFFEE_TABLE.get());
    private final BlockState cubeShelf = of(ModBlocks.CUBE_SHELF.get()), floorLamp = of(ModBlocks.FLOOR_LAMP.get());
    private final BlockState cabinet = of(ModBlocks.KITCHEN_CABINET.get()), crate = of(ModBlocks.STOCK_CRATE.get());
    private final BlockState counter = of(ModBlocks.FOOD_COUNTER.get()), checkout = of(ModBlocks.CHECKOUT_COUNTER.get());
    private final BlockState aisleMarker = of(ModBlocks.AISLE_MARKER.get()), pallet = of(ModBlocks.STORE_PALLET.get());
    private final BlockState flatPack = of(ModBlocks.FLAT_PACK_BOX.get()), paperLamp = of(ModBlocks.PAPER_LAMP.get());
    private final BlockState upright = of(ModBlocks.RACK_UPRIGHT.get()), deck = of(ModBlocks.RACK_SHELF.get());
    private final BlockState pillar = of(ModBlocks.STORE_PILLAR.get()), arrow = of(ModBlocks.FLOOR_ARROW.get());
    private final BlockState departmentSign = of(ModBlocks.DEPARTMENT_SIGN.get()), loadedPallet = of(ModBlocks.LOADED_PALLET.get());
    private final BlockState ballPit = of(ModBlocks.BALL_PIT.get()), shark = of(ModBlocks.SHARK_PLUSH.get());
    private final BlockState toilet = of(ModBlocks.TOILET.get()), bathtub = of(ModBlocks.BATHTUB.get());
    private final BlockState ceiling = of(ModBlocks.STORE_CEILING.get()), ceilingLight = of(ModBlocks.STORE_CEILING_LIGHT.get());
    private static final Block[] MATERIALS = {Blocks.COBBLESTONE, Blocks.STONE, Blocks.BRICKS, Blocks.GLASS, Blocks.SAND, Blocks.STONE_BRICKS,
            Blocks.GRAVEL, Blocks.SMOOTH_STONE, Blocks.TERRACOTTA, Blocks.OAK_PLANKS, Blocks.CLAY, Blocks.DEEPSLATE_BRICKS};
    private static final Block[] WORKSTATIONS = {Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.ANVIL, Blocks.GRINDSTONE, Blocks.STONECUTTER,
            Blocks.SMITHING_TABLE, Blocks.LOOM, Blocks.CAULDRON, Blocks.COMPOSTER, Blocks.BLAST_FURNACE, Blocks.BARREL, Blocks.FLETCHING_TABLE};
    private static final Block[] KID_COLORS = {Blocks.RED_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.LIME_CONCRETE};
    private static final Block[] WOOLS = {Blocks.WHITE_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.GRAY_WOOL, Blocks.BLUE_WOOL, Blocks.CYAN_WOOL,
            Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.GREEN_WOOL, Blocks.BROWN_WOOL, Blocks.PINK_WOOL};
    private static final Block[] FLOWERS = {Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.FERN, Blocks.POPPY,
            Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.LILY_OF_THE_VALLEY, Blocks.AZALEA, Blocks.FLOWERING_AZALEA, Blocks.SHORT_GRASS};
    private static final Block[] CARPETS = {Blocks.LIGHT_GRAY_CARPET, Blocks.GRAY_CARPET, Blocks.BLUE_CARPET, Blocks.WHITE_CARPET, Blocks.BROWN_CARPET,
            Blocks.GREEN_CARPET, Blocks.RED_CARPET, Blocks.YELLOW_CARPET};
    private static final Block[] BEDS = {Blocks.WHITE_BED, Blocks.LIGHT_GRAY_BED, Blocks.BLUE_BED, Blocks.GRAY_BED, Blocks.YELLOW_BED, Blocks.GREEN_BED};
    private static final Block[] GOODS = {Blocks.FLOWER_POT, Blocks.POTTED_FERN, Blocks.POTTED_CACTUS, Blocks.CANDLE, Blocks.WHITE_CANDLE,
            Blocks.YELLOW_CANDLE, Blocks.LANTERN, Blocks.DECORATED_POT, Blocks.POTTED_AZALEA, Blocks.POTTED_BAMBOO};

    private static ResourceKey<LootTable> loot(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/" + name));
    }

    /** The big hall over super-cell (sx, sz) (2x2 departments), or null. */
    public static @Nullable Department hall(long seed, int sx, int sz) {
        if (unit(seed, sx, sz, 3030) >= 0.16) return null;
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) if (cellDepartment(seed, sx * 2 + dx, sz * 2 + dz) == Department.CHECKOUT) return null;
        }
        return HALLS[pick(seed, sx, sz, 3031, HALLS.length)];
    }

    public static Department department(long seed, int cx, int cz) {
        Department hall = hall(seed, Math.floorDiv(cx, 2), Math.floorDiv(cz, 2));
        return hall != null ? hall : cellDepartment(seed, cx, cz);
    }

    private static Department cellDepartment(long seed, int cx, int cz) {
        double u = unit(seed, cx, cz, 3008);
        if (u < 0.004) return Department.CHECKOUT;
        double total = 0.0;
        for (double share : SHARE) total += share;
        double roll = (u - 0.004) / 0.996 * total;
        for (int i = 0; i < ROLL.length; i++) {
            roll -= SHARE[i];
            if (roll < 0) return ROLL[i];
        }
        return Department.BARGAIN;
    }

    /** The nearest checkout within {@link #SIGNS_KNOW} cells, as {cx, cz}, or null. */
    public static int @Nullable [] nearestCheckout(long seed, int cx, int cz) {
        int[] best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -SIGNS_KNOW; dx <= SIGNS_KNOW; dx++) {
            for (int dz = -SIGNS_KNOW; dz <= SIGNS_KNOW; dz++) {
                int d = dx * dx + dz * dz;
                if (d < bestDistance && department(seed, cx + dx, cz + dz) == Department.CHECKOUT) {
                    bestDistance = d;
                    best = new int[]{cx + dx, cz + dz};
                }
            }
        }
        return best;
    }

    /** Which way the sign over the crossing at the corner of cell (cx, cz) points. */
    public static Direction signPoints(long seed, int cx, int cz) {
        int[] checkout = nearestCheckout(seed, cx, cz);
        if (checkout == null) return Direction.from2DDataValue(pick(seed, cx, cz, 3010, 4));     // it lies
        // the checkout's walkway corner is (checkout) itself; head for whichever way is further
        int dx = checkout[0] - cx, dz = checkout[1] - cz;
        if (dx == 0 && dz == 0) return Direction.SOUTH;                                             // it's right here
        return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    public static boolean hasSign(long seed, int cx, int cz) {
        return unit(seed, cx, cz, 3011) < 0.6;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public int maxStandY() {
        return FLOOR + 1;
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        return null;                       // the checkout's doors: out, home
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        Department d = department(seed, Math.floorDiv(pos.getX(), CELL), Math.floorDiv(pos.getZ(), CELL));
        return switch (d) {
            case WAREHOUSE, BARGAIN -> WAREHOUSE_STOCK;
            case MARKET, PLANTS, TEXTILES -> MARKET_STOCK;
            case KIDS -> KIDS_STOCK;
            case HARDWARE -> HARDWARE_STOCK;
            case BACKYARD, NATURE -> GARDEN_STOCK;
            case SEASONAL -> SEASONAL_STOCK;
            default -> null;
        };
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        c[0] = bedrock;
        c[1] = stone;
        c[2] = stone;
        c[FLOOR] = floor;
        // the roof, so high up the fog hides it, that lets the daylight through (and keeps shader skies out)
        c[HEIGHT - 1] = Math.floorMod(x, 12) == 6 && Math.floorMod(z, 12) == 6 ? ceilingLight : ceiling;
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL), lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        int sx = Math.floorDiv(x, 2 * CELL), sz = Math.floorDiv(z, 2 * CELL);
        Department hall = hall(seed, sx, sz);
        if (hall != null) {
            int hx = Math.floorMod(x, 2 * CELL) - WALK, hz = Math.floorMod(z, 2 * CELL) - WALK;
            if (hx >= 0 && hz >= 0) {
                halls.column(c, seed, hall, sx, sz, hx, hz, x, z);
                return c;
            }
        }
        if (lx < WALK || lz < WALK) {
            walkway(c, seed, cx, cz, lx, lz);
            return c;
        }
        int ix = lx - WALK, iz = lz - WALK;
        Department d = department(seed, cx, cz);
        if (d != Department.CHECKOUT && pillar(c, seed, cx, cz, ix, iz, x, z)) return c;
        switch (d) {
            case WAREHOUSE -> warehouse(c, seed, x, z, ix, iz);
            case RESTAURANT -> restaurant(c, seed, x, z, ix, iz);
            case CHECKOUT -> checkout(c, ix, iz);
            case BARGAIN -> bargain(c, seed, x, z);
            case HARDWARE -> hardware(c, seed, x, z, ix, iz);
            default -> booth(c, seed, cx, cz, d, ix, iz);
        }
        return c;
    }

    // ------------------------------------------------------------------ pillars

    /**
     * The pillar in a department, if it has one: {centre x, centre z, half-width, broken (0/1), top}.
     * Most have one, put anywhere: ordinary ones, big ones, now and then a huge one, and some broken
     * off, with their rubble lying round them.
     */
    public static int @Nullable [] pillarIn(long seed, int cx, int cz) {
        double roll = unit(seed, cx, cz, 3040);
        if (roll < 0.40) return null;
        boolean broken = roll >= 0.92;
        int half = roll < 0.68 ? 1 : roll < 0.86 ? 2 : !broken ? 4 : unit(seed, cx, cz, 3041) < 0.5 ? 1 : 2;
        int px = half + 1 + pick(seed, cx, cz, 3042, 25 - 2 * half), pz = half + 1 + pick(seed, cx, cz, 3043, 25 - 2 * half);
        int top = broken ? FLOOR + 6 + pick(seed, cx, cz, 3044, 22) : HEIGHT - 2;
        return new int[]{px, pz, half, broken ? 1 : 0, top};
    }

    private boolean pillar(BlockState[] c, long seed, int cx, int cz, int ix, int iz, int x, int z) {
        int[] p = pillarIn(seed, cx, cz);
        if (p == null) return false;
        int dx = Math.abs(ix - p[0]), dz = Math.abs(iz - p[1]), half = p[2];
        boolean broken = p[3] == 1;
        boolean inside = dx <= half && dz <= half && (half < 2 || dx + dz <= half + half / 2);
        long h = hash(seed, x, z, 3045);
        if (inside) {
            int top = broken ? p[4] - (int) Math.floorMod(h, 4L) : p[4];
            c[FLOOR + 1] = of(Blocks.GRAY_CONCRETE);
            LevelParts.fill(c, FLOOR + 2, top, pillar);
            if (broken && unit(Long.rotateLeft(h, 9)) < 0.35) c[top + 1] = of(Blocks.IRON_BARS);       // the bent rebar
            return true;
        }
        if (half >= 4 && dx <= half + 1 && dz <= half + 1 && dx + dz <= half + half / 2 + 2) {
            c[FLOOR + 1] = of(Blocks.LIGHT_GRAY_CONCRETE);                     // a plinth round the huge ones
            return true;
        }
        if (broken && Math.max(dx, dz) <= half + 3 && unit(Long.rotateLeft(h, 17)) < 0.3) {
            double u = unit(Long.rotateLeft(h, 23));
            c[FLOOR + 1] = u < 0.4 ? of(Blocks.COBBLESTONE) : u < 0.65 ? of(Blocks.ANDESITE) : u < 0.85 ? of(Blocks.GRAVEL) : of(Blocks.COBBLESTONE_SLAB);
            if (u < 0.2) c[FLOOR + 2] = of(Blocks.COBBLESTONE);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ walkways

    private void walkway(BlockState[] c, long seed, int cx, int cz, int lx, int lz) {
        boolean crossing = lx < WALK && lz < WALK;
        boolean edge = crossing ? false : lx < WALK ? (lx == 0 || lx == WALK - 1) : (lz == 0 || lz == WALK - 1);
        c[FLOOR] = edge ? floor : walkway;
        if (crossing && lx == 2 && lz == 2 && hasSign(seed, cx, cz)) {
            c[SIGN_Y] = StoreBlocks.ExitSign.pointing(sign, signPoints(seed, cx, cz));
            LevelParts.fill(c, SIGN_Y + 1, HEIGHT - 2, chain);
        }
        if (crossing) return;
        boolean alongX = lz < WALK;
        int across = alongX ? lz : lx, along = alongX ? lx : lz;
        // blue arrows down the middle of the walkway, showing you the way round (they don't agree with each other either)
        if (across == 2 && along % 8 == 1) {
            boolean forward = unit(seed, cx, cz, alongX ? 3020 : 3021) < 0.5;
            Direction way = alongX ? (forward ? Direction.EAST : Direction.WEST) : (forward ? Direction.SOUTH : Direction.NORTH);
            c[FLOOR + 1] = facing(arrow, way);
        }
        // the department's sign, hanging over the walkway beside it
        if (across == 2 && along == 16) {
            Department d = department(seed, cx, cz);
            c[SIGN_Y] = facing(departmentSign, alongX ? Direction.EAST : Direction.NORTH).setValue(StoreBlocks.DepartmentSign.ICON, d.ordinal());
            LevelParts.fill(c, SIGN_Y + 1, HEIGHT - 2, chain);
        }
    }

    // ------------------------------------------------------------------ showroom booths

    /** Four booths per department (two rows of two), with a cross-shaped aisle between them. */
    private void booth(BlockState[] c, long seed, int cx, int cz, Department d, int ix, int iz) {
        if (ix == 13 || iz == 13) return;                                   // the aisles
        int qx = ix < 13 ? 0 : 1, qz = iz < 13 ? 0 : 1;
        int bx = ix - qx * 14, bz = iz - qz * 14;
        int b = qz == 0 ? bz : 12 - bz;                                     // rows from the back wall
        Direction front = qz == 0 ? Direction.SOUTH : Direction.NORTH, back = front.getOpposite();
        long h = hash(seed, cx, cz, qx + qz * 2, 3012);
        c[FLOOR] = switch (d) {
            case LIVING -> of(Blocks.OAK_PLANKS);
            case BEDROOM -> of(Blocks.BIRCH_PLANKS);
            case KITCHEN -> (bx + bz) % 2 == 0 ? of(Blocks.WHITE_CONCRETE) : of(Blocks.LIGHT_GRAY_CONCRETE);
            case OFFICE -> of(Blocks.SPRUCE_PLANKS);
            case KIDS -> of(KID_COLORS[Math.floorMod(bx / 3 + bz / 3, KID_COLORS.length)]);
            case BATHROOM -> of(ModBlocks.POOL_TILE.get());
            case LIGHTING -> of(Blocks.GRAY_CONCRETE);
            case TEXTILES -> of(Blocks.DARK_OAK_PLANKS);
            default -> floor;
        };
        if (d != Department.MARKET && d != Department.PLANTS && b == 0) {
            // no walls round the store: just a free-standing panel behind some of the booths
            if (bx >= 2 && bx <= 10 && unit(Long.rotateLeft(h, 29)) < 0.6) backWall(c, bx, front);
            return;
        }
        switch (d) {
            case LIVING -> living(c, h, bx, b, front);
            case BEDROOM -> bedroom(c, h, bx, b, front, back);
            case KITCHEN -> kitchen(c, h, bx, b, front, back);
            case OFFICE -> office(c, h, bx, b, back);
            case MARKET -> market(c, h, seed, bx, b);
            case KIDS -> kids(c, h, bx, b, front, back);
            case BATHROOM -> bathroom(c, h, bx, b, front);
            case LIGHTING -> lighting(c, bx, b);
            case PLANTS -> plants(c, h, bx, b);
            case TEXTILES -> textiles(c, h, bx, b, front);
            default -> {
            }
        }
    }

    /** The partition panel behind a booth, with spotlights on top. */
    private void backWall(BlockState[] c, int bx, Direction front) {
        LevelParts.fill(c, FLOOR + 1, FLOOR + 4, partition);
        if (bx == 3 || bx == 9) c[FLOOR + 5] = facing(light, front);
    }

    private static BlockState facing(BlockState state, Direction facing) {
        return state.setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    /** A sofa piece in a row from x0 to x1, joined to its neighbours. */
    private BlockState sofaPiece(int x, int x0, int x1, Direction facing, int color) {
        int leftStep = facing.getCounterClockWise().getStepX();
        boolean left = x + leftStep >= x0 && x + leftStep <= x1, right = x - leftStep >= x0 && x - leftStep <= x1;
        StoreBlocks.SofaPart part = left && right ? StoreBlocks.SofaPart.MIDDLE : left ? StoreBlocks.SofaPart.RIGHT
                : right ? StoreBlocks.SofaPart.LEFT : StoreBlocks.SofaPart.SINGLE;
        return facing(sofa, facing).setValue(StoreBlocks.PART, part).setValue(StoreBlocks.COLOR, color);
    }

    private void living(BlockState[] c, long h, int bx, int b, Direction front) {
        int color = (int) Math.floorMod(h, 6L), rug = (int) Math.floorMod(h >>> 8, (long) CARPETS.length);
        int length = 3 + (int) Math.floorMod(h >>> 16, 2L);                 // a three- or four-seater
        if (b == 2 && bx >= 4 && bx < 4 + length) c[FLOOR + 1] = sofaPiece(bx, 4, 3 + length, front, color);
        else if (b == 6 && bx == 2) c[FLOOR + 1] = sofaPiece(0, 0, 0, Direction.EAST, (color + 3) % 6);
        else if (b == 6 && bx == 10) c[FLOOR + 1] = sofaPiece(0, 0, 0, Direction.WEST, (color + 3) % 6);
        else if (b == 5 && bx >= 5 && bx <= 6) c[FLOOR + 1] = coffeeTable;
        else if (b == 2 && bx == 9) c[FLOOR + 1] = floorLamp;
        else if (b == 2 && bx == 2) c[FLOOR + 1] = of(Blocks.POTTED_FERN);
        else if (b == 1 && bx >= 10 && bx <= 11) {
            c[FLOOR + 1] = facing(cubeShelf, front);
            c[FLOOR + 2] = facing(cubeShelf, front);
        } else if (bx >= 2 && bx <= 10 && b >= 3 && b <= 9) c[FLOOR + 1] = of(CARPETS[rug]);
    }

    private void bedroom(BlockState[] c, long h, int bx, int b, Direction front, Direction back) {
        Block bed = BEDS[(int) Math.floorMod(h, (long) BEDS.length)];
        int rug = (int) Math.floorMod(h >>> 8, (long) CARPETS.length);
        if ((bx == 4 || bx == 8) && (b == 1 || b == 2)) {
            // the head against the back wall
            c[FLOOR + 1] = bed.defaultBlockState().setValue(BedBlock.FACING, back).setValue(BedBlock.PART, b == 1 ? BedPart.HEAD : BedPart.FOOT);
        } else if ((bx == 3 || bx == 9) && b == 1) {
            c[FLOOR + 1] = facing(cubeShelf, front);                         // the bedside tables, with a lamp
            c[FLOOR + 2] = of(Blocks.LANTERN);
        } else if (bx >= 11 && b <= 2) {
            LevelParts.fill(c, FLOOR + 1, FLOOR + 3, facing(cubeShelf, front));   // a wardrobe
        } else if (bx >= 3 && bx <= 9 && b >= 3 && b <= 6) {
            c[FLOOR + 1] = of(CARPETS[rug]);
        }
    }

    private void kitchen(BlockState[] c, long h, int bx, int b, Direction front, Direction back) {
        if (b == 1 && bx >= 1 && bx <= 11) {
            if (bx == 11) {
                c[FLOOR + 1] = of(Blocks.IRON_BLOCK);                        // the fridge
                c[FLOOR + 2] = of(Blocks.IRON_BLOCK);
            } else if (bx == 5) {
                c[FLOOR + 1] = of(Blocks.CAULDRON);                          // the sink
            } else if (bx == 8) {
                c[FLOOR + 1] = facing(of(Blocks.SMOKER), front);             // the oven
            } else {
                c[FLOOR + 1] = facing(cabinet, front);
                if (unit(Long.rotateLeft(h, bx)) < 0.2) c[FLOOR + 2] = of(GOODS[(int) Math.floorMod(h >>> bx, (long) GOODS.length)]);
            }
        } else if (b >= 6 && b <= 7 && bx >= 5 && bx <= 7) {
            c[FLOOR + 1] = diningTable;
        } else if (b == 5 && bx >= 5 && bx <= 7) {
            c[FLOOR + 1] = facing(chair, front);
        } else if (b == 8 && bx >= 5 && bx <= 7) {
            c[FLOOR + 1] = facing(chair, back);
        } else if ((b == 6 || b == 7) && (bx == 4 || bx == 8)) {
            c[FLOOR + 1] = facing(chair, bx == 4 ? Direction.EAST : Direction.WEST);
        }
    }

    private void office(BlockState[] c, long h, int bx, int b, Direction back) {
        if (b == 1 && bx >= 1 && bx <= 11 && bx != 6) {
            c[FLOOR + 1] = of(Blocks.BOOKSHELF);
            c[FLOOR + 2] = of(Blocks.BOOKSHELF);
        } else if ((b == 4 || b == 8) && (bx == 3 || bx == 4 || bx == 8 || bx == 9)) {
            c[FLOOR + 1] = diningTable;                                     // desks
            if (bx == 4 && unit(Long.rotateLeft(h, b)) < 0.6) c[FLOOR + 2] = of(Blocks.LANTERN);
        } else if ((b == 5 || b == 9) && (bx == 3 || bx == 9)) {
            c[FLOOR + 1] = facing(chair, back);
        } else if (b == 4 && bx == 6) {
            c[FLOOR + 1] = floorLamp;
        }
    }

    private void market(BlockState[] c, long h, long seed, int bx, int b) {
        if ((b == 2 || b == 5 || b == 8 || b == 11) && bx >= 1 && bx <= 11) {
            long here = hash(h, bx, b, 3013);
            if ((bx == 1 || bx == 11) && unit(here) < 0.35) {
                c[FLOOR + 1] = crate;                                       // stock, filled up every morning
                return;
            }
            c[FLOOR + 1] = diningTable;                                     // display tables
            if (unit(Long.rotateLeft(here, 7)) < 0.7) c[FLOOR + 2] = of(GOODS[(int) Math.floorMod(here >>> 13, (long) GOODS.length)]);
        } else if (b == 0 && bx % 4 == 2) {
            c[FLOOR + 1] = of(Blocks.POTTED_AZALEA);
        }
    }

    /** Småland: a ball pit, little tables, and a great many cuddly sharks. */
    private void kids(BlockState[] c, long h, int bx, int b, Direction front, Direction back) {
        boolean rim = (bx == 2 || bx == 10) && b >= 2 && b <= 8 || (b == 2 || b == 8) && bx >= 2 && bx <= 10;
        if (rim) {
            c[FLOOR + 1] = of(KID_COLORS[Math.floorMod(bx + b, KID_COLORS.length)]);
        } else if (bx > 2 && bx < 10 && b > 2 && b < 8) {
            c[FLOOR + 1] = ballPit;
        } else if (b == 10 && bx >= 5 && bx <= 6) {
            c[FLOOR + 1] = coffeeTable;
        } else if (b == 11 && bx >= 5 && bx <= 6) {
            c[FLOOR + 1] = facing(chair, back);
        } else if (bx == 11 && b >= 9 && b <= 11) {
            c[FLOOR + 1] = facing(shark, Direction.from2DDataValue((int) Math.floorMod(h >>> b, 4L)));
            if (b == 10) c[FLOOR + 2] = facing(shark, front);
        } else if (bx == 1 && b == 10) {
            c[FLOOR + 1] = crate;                                           // a bin of toys
        } else if (bx == 1 && b == 1) {
            c[FLOOR + 1] = facing(cubeShelf, front);
            c[FLOOR + 2] = facing(shark, front);
        }
    }

    /** Bathrooms: a bath, a loo and a basin at the back, and a row of loos on display. */
    private void bathroom(BlockState[] c, long h, int bx, int b, Direction front) {
        if (b == 1) {
            if (bx == 2 || bx == 3) c[FLOOR + 1] = facing(bathtub, front);
            else if (bx == 6) c[FLOOR + 1] = facing(toilet, front);
            else if (bx == 9) c[FLOOR + 1] = Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3);
            else if (bx == 11) {
                c[FLOOR + 1] = facing(cubeShelf, front);                     // towels
                c[FLOOR + 2] = facing(cubeShelf, front);
            }
        } else if (b >= 3 && b <= 4 && bx >= 4 && bx <= 8) {
            c[FLOOR + 1] = of(Blocks.WHITE_CARPET);
        } else if (b == 7 && bx % 2 == 0 && bx >= 2 && bx <= 10) {
            c[FLOOR + 1] = facing(toilet, front);
        } else if (b == 10 && (bx == 2 || bx == 3 || bx == 8 || bx == 9)) {
            c[FLOOR + 1] = facing(bathtub, front);
        }
    }

    /** Lighting: lamps everywhere, standing and hanging. */
    private void lighting(BlockState[] c, int bx, int b) {
        if (b < 1) return;
        if (bx % 3 == 1 && b % 3 == 1) {
            c[FLOOR + 1] = floorLamp;
        } else if (bx % 3 == 2 && b % 3 == 2) {
            c[FLOOR + 4] = paperLamp;
            LevelParts.fill(c, FLOOR + 5, FLOOR + 20, chain);
        } else if (bx % 3 == 0 && b % 3 == 0) {
            c[FLOOR + 1] = diningTable;
            c[FLOOR + 2] = of(Blocks.LANTERN);
        }
    }

    /** Plants: beds of flowers and greenery, and tables of potted plants. */
    private void plants(BlockState[] c, long h, int bx, int b) {
        boolean bed = b >= 1 && b <= 4 && (bx >= 1 && bx <= 5 || bx >= 7 && bx <= 11);
        if (bed) {
            boolean edge = b == 1 || b == 4 || bx == 1 || bx == 5 || bx == 7 || bx == 11;
            if (edge) {
                c[FLOOR + 1] = of(Blocks.SPRUCE_SLAB);
            } else {
                c[FLOOR] = of(Blocks.MOSS_BLOCK);
                long hh = hash(h, bx, b, 3022);
                if (unit(hh) < 0.85) c[FLOOR + 1] = of(FLOWERS[(int) Math.floorMod(hh >>> 9, (long) FLOWERS.length)]);
            }
        } else if ((b == 7 || b == 10) && bx >= 1 && bx <= 11 && bx != 6) {
            c[FLOOR + 1] = diningTable;
            c[FLOOR + 2] = of(GOODS[(int) Math.floorMod(hash(h, bx, b, 3023), (long) GOODS.length)]);
        } else if (b == 12 && bx % 4 == 2) {
            c[FLOOR + 1] = of(Blocks.POTTED_FLOWERING_AZALEA);
        }
    }

    /** Textiles: rugs laid out to look at, rolls of them in piles, bedding on the shelves. */
    private void textiles(BlockState[] c, long h, int bx, int b, Direction front) {
        if (b >= 1 && b <= 4 && bx >= 1 && bx <= 11) {
            c[FLOOR + 1] = of(CARPETS[Math.floorMod(bx / 2 + b / 2 + (int) h, CARPETS.length)]);
        } else if ((b == 6 || b == 9) && bx >= 1 && bx <= 11 && bx != 6) {
            long hh = hash(h, bx, b, 3024);
            int pile = 1 + (int) Math.floorMod(hh, 2L);
            for (int i = 0; i < pile; i++) c[FLOOR + 1 + i] = of(WOOLS[(int) Math.floorMod(hh >>> (5 + i * 4), (long) WOOLS.length)]);
        } else if (b == 11 && bx >= 1 && bx <= 4) {
            c[FLOOR + 1] = facing(cubeShelf, front);
            c[FLOOR + 2] = facing(cubeShelf, front);
        }
    }

    /** The bargain corner: whatever got returned, dumped anywhere, any way up. */
    private void bargain(BlockState[] c, long seed, int x, int z) {
        c[FLOOR] = floor;
        long h = hash(seed, x, z, 3025);
        if (unit(h) > 0.2) return;
        Direction way = Direction.from2DDataValue((int) Math.floorMod(h >>> 8, 4L));
        int pick = (int) Math.floorMod(h >>> 12, 11L);
        c[FLOOR + 1] = switch (pick) {
            case 0 -> facing(sofa, way).setValue(StoreBlocks.COLOR, (int) Math.floorMod(h >>> 20, 6L));
            case 1 -> facing(chair, way);
            case 2 -> diningTable;
            case 3 -> coffeeTable;
            case 4 -> facing(cubeShelf, way);
            case 5 -> floorLamp;
            case 6 -> facing(cabinet, way);
            case 7 -> pallet;
            case 8 -> unit(Long.rotateLeft(h, 30)) < 0.3 ? crate : flatPack;
            case 9 -> facing(shark, way);
            default -> flatPack;
        };
        if (pick >= 8 && unit(Long.rotateLeft(h, 40)) < 0.5) c[FLOOR + 2] = flatPack;
    }

    /** Tools and building supplies: shelving of workstations and stock crates, and a yard of building materials. */
    private void hardware(BlockState[] c, long seed, int x, int z, int ix, int iz) {
        c[FLOOR] = floor;
        long h = hash(seed, x, z, 3130);
        if (iz >= 20) {
            if (ix % 3 != 0 && iz != 23) {
                int height = 1 + (int) Math.floorMod(h, 3L);
                LevelParts.fill(c, FLOOR + 1, FLOOR + height, of(MATERIALS[Math.floorMod(ix / 3 + (iz > 23 ? 5 : 0), MATERIALS.length)]));
            }
            return;
        }
        int row = iz % 5;
        if (row != 1 && row != 2) return;
        if (ix % 6 == 1) {
            LevelParts.fill(c, FLOOR + 1, FLOOR + 4, upright);
            return;
        }
        c[FLOOR + 3] = deck;
        Direction facing = row == 1 ? Direction.NORTH : Direction.SOUTH;
        if (unit(h) < 0.22) {
            c[FLOOR + 1] = crate;
        } else {
            BlockState station = of(WORKSTATIONS[(int) Math.floorMod(h >>> 8, (long) WORKSTATIONS.length)]);
            if (station.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) station = station.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            c[FLOOR + 1] = station;
        }
        if (unit(Long.rotateLeft(h, 20)) < 0.5) c[FLOOR + 4] = unit(Long.rotateLeft(h, 30)) < 0.5 ? of(Blocks.LANTERN) : crate;
    }

    // ------------------------------------------------------------------ the self-serve warehouse

    private void warehouse(BlockState[] c, long seed, int x, int z, int ix, int iz) {
        int row = iz % 6;                                                    // two deep, four across the aisle
        if (row != 1 && row != 2) return;
        boolean post = ix % 4 == 1;
        if (post) {
            LevelParts.fill(c, FLOOR + 1, FLOOR + 12, upright);
            if (ix == 1 && row == 1) c[FLOOR + 13] = aisleMarker;
            return;
        }
        c[FLOOR + 4] = deck;
        c[FLOOR + 8] = deck;
        c[FLOOR + 12] = deck;
        long h = hash(seed, x, z, 3014);
        // the floor level: a pallet, loaded or not, with more boxes on top
        int load = (int) Math.floorMod(h >>> 20, 4L);
        c[FLOOR + 1] = load == 0 ? pallet : loadedPallet;
        for (int i = 1; i < load; i++) c[FLOOR + 1 + i] = flatPack;
        // the shelves: flat-pack boxes, and now and then a stock crate
        for (int level = 0; level < 2; level++) {
            int y = FLOOR + 5 + level * 4;
            long hl = Long.rotateLeft(h, 11 + level * 17);
            if (unit(hl) < 0.08) c[y] = crate;
            else stack(c, y, 3, hl);
        }
    }

    private void stack(BlockState[] c, int y, int max, long h) {
        // (flat-pack boxes are whole blocks, so a stack sits flat)
        int n = (int) Math.floorMod(h >>> 20, (long) (max + 1));
        for (int i = 0; i < n; i++) c[y + i] = flatPack;
    }

    // ------------------------------------------------------------------ the restaurant

    private void restaurant(BlockState[] c, long seed, int x, int z, int ix, int iz) {
        c[FLOOR] = of(Blocks.LIGHT_GRAY_CONCRETE);
        if (iz == 0 && ix >= 2 && ix <= 24) {
            LevelParts.fill(c, FLOOR + 1, FLOOR + 4, partition);
            if (ix % 5 == 2) c[FLOOR + 5] = facing(light, Direction.SOUTH);
            return;
        }
        if (iz == 1 && ix >= 4 && ix <= 22) {
            c[FLOOR + 1] = facing(counter, Direction.SOUTH);
            return;
        }
        if (iz < 5) return;
        int tx = Math.floorMod(ix - 2, 5), tz = Math.floorMod(iz - 6, 5);
        if (ix < 2 || ix > 24 || iz > 23) return;
        if (tx == 0 && tz == 0) {
            c[FLOOR + 1] = diningTable;
            if (Math.floorMod(Math.floorDiv(ix - 2, 5) + Math.floorDiv(iz - 6, 5), 2) == 0) {   // a lamp over every other table
                c[FLOOR + 6] = paperLamp;
                LevelParts.fill(c, FLOOR + 7, HEIGHT - 2, chain);
            }
        } else if (tx == 0 && (tz == 1 || tz == 4)) {
            c[FLOOR + 1] = facing(chair, tz == 1 ? Direction.NORTH : Direction.SOUTH);
        } else if (tz == 0 && (tx == 1 || tx == 4)) {
            c[FLOOR + 1] = facing(chair, tx == 1 ? Direction.WEST : Direction.EAST);
        }
    }

    // ------------------------------------------------------------------ the checkout, and the way out

    /** Rows of tills, and a wall with the red doors out. */
    private void checkout(BlockState[] c, int ix, int iz) {
        if (iz == 20) {
            LevelParts.fill(c, FLOOR + 1, FLOOR + 5, partition);
            if (ix == 6 || ix == 13 || ix == 20) {
                LevelParts.exitDoor(c, FLOOR + 1, Direction.NORTH);
                c[FLOOR + 4] = StoreBlocks.ExitSign.pointing(sign, Direction.EAST);
            }
            return;
        }
        if (iz >= 6 && iz <= 12 && ix % 5 == 3) {
            c[FLOOR + 1] = facing(checkout, Direction.EAST);
            if (iz == 12) c[FLOOR + 2] = of(Blocks.LANTERN);
        }
        if (iz >= 6 && iz <= 12 && ix % 5 == 3 && iz == 6) c[FLOOR + 3] = of(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
    }

    // ------------------------------------------------------------------ the staff, on their rounds

    @Override
    public void populate(WorldGenRegion region, long seed) {
        ChunkPos chunk = region.getCenter();
        int sx = Math.floorDiv(chunk.getMinBlockX(), 2 * CELL), sz = Math.floorDiv(chunk.getMinBlockZ(), 2 * CELL);
        Department hall = hall(seed, sx, sz);
        if (hall != null) {
            halls.populate(region, hall, sx, sz);
            return;
        }
        for (int cx = Math.floorDiv(chunk.getMinBlockX(), CELL); cx <= Math.floorDiv(chunk.getMaxBlockX(), CELL); cx++) {
            for (int cz = Math.floorDiv(chunk.getMinBlockZ(), CELL); cz <= Math.floorDiv(chunk.getMaxBlockZ(), CELL); cz++) {
                if (unit(seed, cx, cz, 3015) > 0.35) continue;
                int x = cx * CELL + 2, z = cz * CELL + 8 + pick(seed, cx, cz, 3016, 20);
                if (x < chunk.getMinBlockX() || x > chunk.getMaxBlockX() || z < chunk.getMinBlockZ() || z > chunk.getMaxBlockZ()) continue;
                var staff = ModEntities.STAFF.get().create(region.getLevel());
                if (staff == null) continue;
                staff.moveTo(x + 0.5, FLOOR + 1, z + 0.5, region.getRandom().nextFloat() * 360.0f, 0.0f);
                staff.finalizeSpawn(region, region.getCurrentDifficultyAt(new BlockPos(x, FLOOR + 1, z)), MobSpawnType.STRUCTURE, null);
                region.addFreshEntity(staff);
            }
        }
    }
}
