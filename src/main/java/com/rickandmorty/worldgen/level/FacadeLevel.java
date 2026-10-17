package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.noise;
import static com.rickandmorty.worldgen.level.GenMath.unit;

/**
 * the Facade. Endless slabs of building, forty blocks thick, that
 * run east-west forever and go up and down into the fog further than you can see, with narrow
 * canyons of air between them. Their faces are grey concrete, window after window, and an air
 * conditioner under almost every one, with a little rain-shelf over it. You get about on the
 * outside: along the ledges that run under some floors, from box to box, up the maintenance
 * ladders, across the odd catwalk to the building opposite. Some windows are open, and behind
 * them is a tiny room, all the same, where someone may have left something. Fall, and you fall
 * out of the bottom of the level.
 * <p>
 * Every 64 blocks of z: building z 0-39 (faces at 0, looking north, and 39, looking south),
 * canyon 40-63. Bays are 6 wide (window at 2-3), floors 4 tall (floor slab at y = 4k).
 */
public final class FacadeLevel implements LevelGenerator {
    public static final int HEIGHT = 384, PERIOD = 64, DEPTH = 40, BAY = 6, FLOOR = 4, ROOM_DEPTH = 5, LADDER_RUN = 30, BRIDGE_RUN = 90;
    private static final ResourceKey<LootTable> ROOM_LOOT = ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/field_house"));

    private static BlockState of(Block block) {
        return block.defaultBlockState();
    }

    private final BlockState wall = of(Blocks.LIGHT_GRAY_CONCRETE), pier = of(Blocks.STONE), stain = of(Blocks.GRAY_CONCRETE), inner = of(Blocks.GRAY_CONCRETE),
            glass = of(Blocks.GRAY_STAINED_GLASS), ledge = of(Blocks.SMOOTH_STONE), shelf = of(Blocks.SMOOTH_STONE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM),
            plaster = of(Blocks.WHITE_TERRACOTTA), floorboards = of(Blocks.SPRUCE_PLANKS), bars = of(Blocks.IRON_BARS).setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH, true).setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH, true);

    @Override
    public int height() {
        return HEIGHT;
    }

    /** Where a face is: +1 for the south face (z % 64 = 39, canyon to +z), -1 for the north face (0, canyon to -z). */
    public static int localZ(int z) {
        return Math.floorMod(z, PERIOD);
    }

    /** Is there an air conditioner on this face (side: -1 north, +1 south) at (x, floor)? Two per bay, on either side of the window. */
    public static boolean airCon(long seed, int x, int floor, int face) {
        int b = Math.floorMod(x, BAY);
        if (b != 0 && b != 4) return false;
        return unit(hash(seed, x, floor, face, 9300)) < 0.72;
    }

    /** Does this floor have a ledge running along the face here? (in long stretches, with gaps) */
    public static boolean ledge(long seed, int x, int floor, int face) {
        if (noise(seed, x, floor * 31 + face * 7, 40.0, 9301) < 0.42) return false;
        return unit(hash(seed, x, floor, face, 9302)) > 0.04;
    }

    /** A maintenance ladder up the face at this x, for these floors? (one every 30 blocks, a few floors long) */
    public static boolean ladder(long seed, int x, int floor, int face) {
        int run = Math.floorDiv(x, LADDER_RUN), at = run * LADDER_RUN + 3 + (int) Math.floorMod(hash(seed, run, face, 9303), LADDER_RUN - 6L);
        if (x != at || Math.floorMod(at, BAY) == 2 || Math.floorMod(at, BAY) == 3) return false;
        return noise(seed, run * 13, floor + face * 500, 6.0, 9304) > 0.35;
    }

    /** The open window into a room, on this face, bay, floor? */
    public static boolean room(long seed, int bay, int floor, int face) {
        return unit(hash(seed, bay, floor, face, 9305)) < 0.07;
    }

    /** A catwalk across the canyon south of building {@code row}, at x (its middle), on floor {@code floor}? Returns the floor or -1. */
    public static int bridgeFloor(long seed, int row, int x) {
        int run = Math.floorDiv(x, BRIDGE_RUN);
        long h = hash(seed, run, row, 9306);
        if (unit(h) > 0.55) return -1;
        int middle = run * BRIDGE_RUN + 10 + (int) Math.floorMod(h >>> 16, BRIDGE_RUN - 20L);
        if (Math.abs(x - middle) > 1) return -1;
        return 10 + (int) Math.floorMod(h >>> 30, HEIGHT / FLOOR - 20L);
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[HEIGHT];
        int lz = localZ(z), row = Math.floorDiv(z, PERIOD);
        if (lz < DEPTH) building(c, seed, x, z, lz);
        else canyon(c, seed, x, lz, row);
        return c;
    }

    private void building(BlockState[] c, long seed, int x, int z, int lz) {
        int side = lz < DEPTH / 2 ? -1 : 1, depth = side < 0 ? lz : DEPTH - 1 - lz;
        int b = Math.floorMod(x, BAY), bay = Math.floorDiv(x, BAY), face = Math.floorDiv(z, PERIOD) * 2 + (side > 0 ? 1 : 0);
        for (int y = 0; y < HEIGHT; y++) {
            int floor = Math.floorDiv(y, FLOOR), fy = Math.floorMod(y, FLOOR);
            boolean open = room(seed, bay, floor, face);
            if (depth == 0) {
                // the face itself
                boolean window = (b == 2 || b == 3) && (fy == 1 || fy == 2);
                if (window) c[y] = open ? null : glass;
                else if (b == 0) c[y] = pier;
                else if (fy == 0 || fy == 3 && airCon(seed, x, floor, face) && unit(hash(seed, x, y, z, 9310)) < 0.5) c[y] = stain;
                else c[y] = wall;
            } else if (open && depth <= ROOM_DEPTH && b >= 1 && b <= 4 && fy >= 1) {
                c[y] = depth == ROOM_DEPTH ? plaster : null;
                if (depth == ROOM_DEPTH && b == 2 && fy <= 2 && hasDoor(seed, bay, floor, face)) {
                    BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, side < 0 ? Direction.NORTH : Direction.SOUTH);
                    c[y] = door.setValue(DoorBlock.HALF, fy == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                }
                if (fy == 1 && depth < ROOM_DEPTH) furnish(c, seed, x, y, z, bay, floor, face, side, b, depth);
            } else if (open && depth <= ROOM_DEPTH + 1 && b >= 0 && b <= 5 && fy == 0) {
                c[y] = floorboards;
            } else {
                c[y] = depth <= ROOM_DEPTH + 1 ? wall : inner;
            }
        }
    }

    /** One room in six has a red door in its back wall. */
    public static boolean hasDoor(long seed, int bay, int floor, int face) {
        return unit(hash(seed, bay, floor, face, 9321)) < 0.18;
    }

    /** A room the same as all the others: a rug, a lamp, sometimes a chest. */
    private void furnish(BlockState[] c, long seed, int x, int y, int z, int bay, int floor, int face, int side, int b, int depth) {
        long h = hash(seed, bay, floor, face, 9320);
        Direction out = side < 0 ? Direction.NORTH : Direction.SOUTH;
        if (b == 1 && depth == 3) c[y] = of(Blocks.WHITE_CARPET);
        if (b == 1 && depth == ROOM_DEPTH - 1 && unit(h) < 0.5) {
            c[y] = of(Blocks.CHEST).setValue(ChestBlock.FACING, out);
        }
        if (b == 3 && depth == 2) {
            // a little table with a lamp on it, and a chair
            c[y] = of(Blocks.OAK_FENCE);
            c[y + 1] = of(Blocks.LANTERN);
        }
        if (b == 4 && depth == 2) c[y] = of(Blocks.OAK_STAIRS).setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.WEST);
        if (b == 4 && depth == ROOM_DEPTH - 1) c[y] = of(Blocks.WHITE_CARPET);
    }

    private void canyon(BlockState[] c, long seed, int x, int lz, int row) {
        boolean northOfNext = lz == PERIOD - 1, southOfThis = lz == DEPTH;
        int bridge = bridgeFloor(seed, row, x);
        if (bridge >= 0) {
            // a catwalk: a strip of slab with a railing of bars down each side
            int y = bridge * FLOOR;
            c[y] = ledge;
            if (bridgeFloor(seed, row, x - 1) < 0 || bridgeFloor(seed, row, x + 1) < 0) c[y + 1] = bars;
        }
        if (!northOfNext && !southOfThis) return;
        int side = southOfThis ? 1 : -1, face = southOfThis ? row * 2 + 1 : (row + 1) * 2;
        Direction out = side > 0 ? Direction.SOUTH : Direction.NORTH;
        for (int floor = 0; floor < HEIGHT / FLOOR; floor++) {
            int y = floor * FLOOR;
            if (ladder(seed, x, floor, face)) {
                for (int k = 0; k < FLOOR; k++) if (c[y + k] == null) c[y + k] = of(Blocks.LADDER).setValue(LadderBlock.FACING, out);
                continue;
            }
            if (ledge(seed, x, floor, face)) c[y] = ledge;
            if (airCon(seed, x, floor, face)) {
                c[y + 1] = ModBlocks.AIR_CONDITIONER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, out);
                c[y + 2] = shelf;
            }
        }
    }

    @Override
    public @Nullable ResourceKey<LootTable> containerLoot(long seed, BlockPos pos) {
        return ROOM_LOOT;
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        // home, back to Level 0, or out into a hall where you'll have to run
        double r = unit(hash(seed, pos.getX(), pos.getY(), pos.getZ(), 9330));
        return r < 0.4 ? null : r < 0.75 ? BackroomsLevel.LEVEL_0 : BackroomsLevel.RUN_FOR_YOUR_LIFE;
    }

    @Override
    public int minStandY() {
        return 8;
    }

    /** You arrive in one of the little rooms, somewhere up the face. */
    @Override
    public Vec3 landing(long seed, RandomSource random) {
        for (int attempt = 0; attempt < 2000; attempt++) {
            int bay = random.nextIntBetweenInclusive(-500, 500), floor = random.nextIntBetweenInclusive(20, HEIGHT / FLOOR - 20), row = random.nextIntBetweenInclusive(-30, 30);
            int side = random.nextBoolean() ? 1 : -1;
            if (!room(seed, bay, floor, row * 2 + (side > 0 ? 1 : 0))) continue;
            int z = row * PERIOD + (side < 0 ? 2 : DEPTH - 3);
            return new Vec3(bay * BAY + 2.5, floor * FLOOR + 1, z + 0.5);
        }
        return new Vec3(2.5, 101, 2.5);
    }
}
