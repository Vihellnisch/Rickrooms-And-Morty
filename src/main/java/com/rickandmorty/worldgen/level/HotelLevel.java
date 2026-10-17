package com.rickandmorty.worldgen.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.level.GenMath.hash;
import static com.rickandmorty.worldgen.level.GenMath.unit;
import static com.rickandmorty.worldgen.level.LevelParts.of;

/**
 * The Wooden Hotel: blocks of rooms stacked thirteen storeys high around an open shaft. Every
 * storey has a balcony of planks with a fence railing, torches along it, and a wall of
 * cobblestone and timber with a door or a window for every room. Ladders in the corners, the
 * odd bridge across the shaft. The shaft has no bottom: fall in, and you'll find out where it goes.
 * <pre>
 *  per 44 x 44 block: 0 outer wall | 1-5 rooms | 6 room wall | 7-9 balcony | 10-33 shaft | ... mirrored
 *  storeys every 6 from y 4; roof y 82; bedrock 83
 * </pre>
 */
final class HotelLevel implements LevelGenerator {
    static final int BLOCK = 44;
    static final int SHAFT_MIN = 10, SHAFT_MAX = 33;
    static final int STOREYS = 13, FIRST = 4, STOREY = 6, ROOF = 82;

    private final BlockState bedrock = of(Blocks.BEDROCK), plank = of(Blocks.OAK_PLANKS), cobble = of(Blocks.COBBLESTONE);
    private final BlockState fence = of(Blocks.OAK_FENCE), torch = of(Blocks.TORCH), glass = of(Blocks.GLASS);
    private final BlockState post = of(Blocks.STRIPPED_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

    @Override
    public int height() {
        return ROOF + 2;
    }

    @Override
    public int minStandY() {
        return FIRST + 1;
    }

    static int floorY(int storey) {
        return FIRST + storey * STOREY;
    }

    /** Distance "in" from the nearest outer edge of the block, and which side (0 west, 1 east, 2 north, 3 south). */
    private static int[] ring(int lx, int lz) {
        int dx = Math.min(lx, BLOCK - 1 - lx), dz = Math.min(lz, BLOCK - 1 - lz);
        if (dx <= dz) return new int[]{dx, lx < BLOCK / 2 ? 0 : 1};
        return new int[]{dz, lz < BLOCK / 2 ? 2 : 3};
    }

    private static Direction outward(int side) {
        return switch (side) {
            case 0 -> Direction.EAST;       // the west rooms' doors open east, onto the balcony
            case 1 -> Direction.WEST;
            case 2 -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
    }

    @Override
    public BlockState[] column(long seed, int x, int z) {
        BlockState[] c = new BlockState[height()];
        c[ROOF + 1] = bedrock;
        c[ROOF] = plank;
        int bx = Math.floorDiv(x, BLOCK), bz = Math.floorDiv(z, BLOCK);
        int lx = Math.floorMod(x, BLOCK), lz = Math.floorMod(z, BLOCK);
        boolean shaftX = lx >= SHAFT_MIN && lx <= SHAFT_MAX, shaftZ = lz >= SHAFT_MIN && lz <= SHAFT_MAX;
        if (shaftX && shaftZ) {
            shaft(c, seed, bx, bz, lx, lz);
            return c;
        }
        int[] ring = ring(lx, lz);
        int depth = ring[0], side = ring[1];
        int along = side <= 1 ? lz : lx;
        LevelParts.fill(c, 0, FIRST, plank);                   // the foundations
        boolean corridor = along >= 21 && along <= 22;         // through to the next block, every storey
        for (int s = 0; s < STOREYS; s++) {
            int f = floorY(s);
            c[f] = plank;
            if (depth == 0) {                                  // outer wall
                if (!corridor) LevelParts.fill(c, f + 1, f + STOREY - 1, cobble);
            } else if (depth <= 5) {                           // rooms
                room(c, seed, bx, bz, s, side, along, depth, f, corridor);
            } else if (depth == 6) {                           // the wall between the rooms and the balcony
                roomWall(c, seed, bx, bz, s, side, along, f, corridor);
            } else if (depth == 9) {                           // railing, with a torch every few posts
                c[f + 1] = fence;
                if (Math.floorMod(along, 6) == 0) c[f + 2] = torch;
            }
            // ladders in the balcony corners, through a hole in each floor
            if (depth == 7 && Math.floorMod(along, BLOCK) == 7 && s > 0) c[f] = null;
            if (depth == 7 && Math.floorMod(along, BLOCK) == 7) {
                for (int y = f + 1; y <= f + STOREY && y < ROOF; y++) {
                    c[y] = of(Blocks.LADDER).setValue(LadderBlock.FACING, outward(side));
                }
            }
        }
        return c;
    }

    /** The open shaft: air all the way down, except for the odd bridge across. */
    private void shaft(BlockState[] c, long seed, int bx, int bz, int lx, int lz) {
        for (int s = 1; s < STOREYS; s++) {
            if (unit(seed, bx * 64 + s, bz, 100) > 0.22) continue;
            boolean alongX = unit(seed, bx, bz * 64 + s, 101) < 0.5;
            int across = alongX ? lz : lx;
            if (across < 21 || across > 23) continue;
            int f = floorY(s);
            c[f] = of(Blocks.SPRUCE_PLANKS);
            if (across == 21 || across == 23) c[f + 1] = fence;
        }
    }

    private void room(BlockState[] c, long seed, int bx, int bz, int s, int side, int along, int depth, int f, boolean corridor) {
        if (corridor) return;
        int m = Math.floorMod(along, 6);
        if (m == 0) {                                          // walls between the rooms
            LevelParts.fill(c, f + 1, f + STOREY - 1, plank);
            return;
        }
        long h = hash(seed, bx * 8 + side, bz * 8 + s, Math.floorDiv(along, 6), 102);
        if (depth == 1 && m == 2) {
            double u = unit(h);
            if (u < 0.2) c[f + 1] = of(Blocks.CHEST);
            else if (u < 0.35) c[f + 1] = of(Blocks.CRAFTING_TABLE);
            else if (u < 0.45) c[f + 1] = of(Blocks.BOOKSHELF);
        }
        if (depth == 1 && m == 4 && unit(Long.rotateLeft(h, 12)) < 0.3) c[f + 1] = torch;
    }

    /** Cobblestone panels framed in timber, a door or a window for every room. */
    private void roomWall(BlockState[] c, long seed, int bx, int bz, int s, int side, int along, int f, boolean corridor) {
        if (corridor) return;
        int m = Math.floorMod(along, 6);
        for (int y = f + 1; y <= f + STOREY - 1; y++) c[y] = m == 0 || y == f + STOREY - 1 ? post : cobble;
        int room = Math.floorDiv(along, 6);
        long h = hash(seed, bx * 8 + side, bz * 8 + s, room, 103);
        if (m == 3) {
            if (room % 2 == 0) {
                if (unit(h) < 0.03) {
                    LevelParts.exitDoor(c, f + 1, outward(side));
                } else {
                    BlockState door = of(Blocks.OAK_DOOR).setValue(DoorBlock.FACING, outward(side));
                    c[f + 1] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
                    c[f + 2] = door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
                }
            } else {
                c[f + 2] = glass;
                c[f + 3] = glass;
            }
        }
        if (m == 5 && room % 2 == 1) {
            c[f + 2] = glass;
            c[f + 3] = glass;
        }
    }

    @Override
    public @Nullable net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> containerLoot(long seed, BlockPos pos) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                com.rickandmorty.RickAndMortyMod.id("chests/wooden_hotel_room"));
    }

    @Override
    public @Nullable BackroomsLevel exitDestination(long seed, BlockPos pos) {
        return BackroomsLevel.LEVEL_188;
    }
}
