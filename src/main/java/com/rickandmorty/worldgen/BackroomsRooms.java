package com.rickandmorty.worldgen;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.FluorescentLightBlock;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import static com.rickandmorty.worldgen.BackroomsLayout.CELL;

/**
 * Rooms carved into the Backrooms maze: big empty halls (some two floors tall with a balcony),
 * closets, and eerie copies of rooms from a house. Each 4x4-cell square of the maze may hold
 * one room. A room has solid walls with doorways, no maze inside, and its own furniture.
 */
public final class BackroomsRooms {
    /** Rooms are placed on a grid of squares this many cells wide. */
    public static final int GRID = 4;
    private static final double ROOM_CHANCE = 0.5;

    public enum Type {
        HALL(3, 3, 16), CLOSET(1, 1, 8), KITCHEN(2, 1, 10), DINING(2, 1, 8), LIVING(2, 2, 10), OFFICE(2, 1, 10),
        BATHROOM(1, 1, 7), POOL(2, 2, 5), CHAIRS(2, 2, 5), DOOR(1, 1, 4);

        final int length, width, weight;

        Type(int length, int width, int weight) {
            this.length = length;
            this.width = width;
            this.weight = weight;
        }
    }

    /**
     * A room covering cells [cx0, cx0 + cellsX) x [cz0, cz0 + cellsZ). Its walls stand on the
     * cell lines around it. {@code alongX} says whether its long side runs along x.
     */
    public record Room(Type type, int cx0, int cz0, int cellsX, int cellsZ, boolean alongX, boolean atrium, long hash) {
        public int x0() { return cx0 * CELL; }
        public int z0() { return cz0 * CELL; }
        public int x1() { return (cx0 + cellsX) * CELL; }
        public int z1() { return (cz0 + cellsZ) * CELL; }

        public boolean containsCell(int cx, int cz) {
            return cx >= cx0 && cx < cx0 + cellsX && cz >= cz0 && cz < cz0 + cellsZ;
        }

        /** Inside the room or on its walls. */
        public boolean covers(int x, int z) {
            return x >= x0() && x <= x1() && z >= z0() && z <= z1();
        }

        public boolean onWall(int x, int z) {
            return covers(x, z) && (x == x0() || x == x1() || z == z0() || z == z1());
        }

        /** Two-wide doorways in the middle of some walls (halls get all four). */
        public boolean isDoorway(int x, int z) {
            int midX = (x0() + x1()) / 2, midZ = (z0() + z1()) / 2;
            boolean alongXWall = z == z0() || z == z1();
            boolean inGap = alongXWall ? (x == midX || x == midX - 1) : (z == midZ || z == midZ - 1);
            if (!inGap) return false;
            return hasDoor(z == z0() ? 0 : z == z1() ? 1 : x == x0() ? 2 : 3);
        }

        /** Sides: 0 = north wall (z0), 1 = south (z1), 2 = west (x0), 3 = east (x1). Halls open on every side. */
        public boolean hasDoor(int side) {
            if (type == Type.HALL) return true;
            int doors = (int) Math.floorMod(hash >>> 8, 15L) + 1;   // a non-empty 4-bit mask
            return (doors & (1 << side)) != 0;
        }
    }

    private BackroomsRooms() {
    }

    // ------------------------------------------------------------------ placement

    /** The room in grid square (gx, gz) on a floor, or null. The upper floor above an atrium shares its hall. */
    public static @Nullable Room roomIn(long seed, int level, int gx, int gz) {
        if (level == 1) {
            Room below = roomIn(seed, 0, gx, gz);
            if (below != null && below.atrium()) return below;
        }
        long h = hash(seed + level * 0x632BE59BD9B4E019L, gx, gz, 600);
        if (unit(h) >= ROOM_CHANCE) return null;
        Type type = pickType(hash(seed + level * 0x632BE59BD9B4E019L, gx, gz, 601));
        boolean alongX = (h >>> 30 & 1) == 0;
        int cellsX = alongX ? type.length : type.width;
        int cellsZ = alongX ? type.width : type.length;
        if (type == Type.HALL && (h >>> 31 & 1) == 0) {   // some halls are 3x2
            if (alongX) cellsZ = 2; else cellsX = 2;
        }
        int cx0 = gx * GRID + (int) Math.floorMod(h >>> 36, (long) (GRID - cellsX + 1));
        int cz0 = gz * GRID + (int) Math.floorMod(h >>> 44, (long) (GRID - cellsZ + 1));
        boolean atrium = type == Type.HALL && level == 0 && (h >>> 52 & 1) == 0;
        Room room = new Room(type, cx0, cz0, cellsX, cellsZ, alongX, atrium, h);
        // rooms stay in the classic maze (they're smaller than a zone, so checking the corners is enough)
        for (int x : new int[]{room.x0() - 1, room.x1() + 1}) {
            for (int z : new int[]{room.z0() - 1, room.z1() + 1}) {
                if (BackroomsZones.zoneAt(seed, x, z) != BackroomsZones.Zone.CLASSIC) return null;
            }
        }
        return room;
    }

    private static Type pickType(long h) {
        int total = 0;
        for (Type t : Type.values()) total += t.weight;
        int roll = (int) Math.floorMod(h, (long) total);
        for (Type t : Type.values()) {
            roll -= t.weight;
            if (roll < 0) return t;
        }
        return Type.HALL;
    }

    /** The room whose cells include (cx, cz), or null. */
    public static @Nullable Room roomOfCell(long seed, int level, int cx, int cz) {
        Room room = roomIn(seed, level, Math.floorDiv(cx, GRID), Math.floorDiv(cz, GRID));
        return room != null && room.containsCell(cx, cz) ? room : null;
    }

    /** The room covering block (x, z), walls included, or null. */
    public static @Nullable Room roomAt(long seed, int level, int x, int z) {
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        for (int dx = 0; dx >= -1; dx--) {
            for (int dz = 0; dz >= -1; dz--) {
                Room room = roomIn(seed, level, Math.floorDiv(cx + dx, GRID), Math.floorDiv(cz + dz, GRID));
                if (room != null && room.covers(x, z)) return room;
            }
        }
        return null;
    }

    /** Wall inside the maze rules: the room's walls (minus doorways), and hall pillars. */
    public static boolean wallInRoom(Room room, int x, int z) {
        if (room.onWall(x, z)) return !room.isDoorway(x, z);
        return room.type() == Type.HALL && Math.floorMod(x, CELL) == 0 && Math.floorMod(z, CELL) == 0;
    }

    /** What the containers in a room hold. */
    public static @Nullable ResourceKey<LootTable> containerLoot(Room room) {
        String table = switch (room.type()) {
            case KITCHEN -> "chests/backrooms_kitchen";
            case OFFICE -> "chests/backrooms_office";
            default -> null;
        };
        return table == null ? null : ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id(table));
    }

    // ------------------------------------------------------------------ furnishing

    /**
     * Local coordinates: u along the room's long side, v across it. The layout is mirrored when
     * needed so furniture "against the back wall" (v = 1) ends up on a wall without a door.
     */
    private record Local(int u, int v, int length, int width, boolean alongX, boolean flipU, boolean flipV) {
        static Local of(Room room, int rx, int rz, int w, int h) {
            boolean alongX = room.alongX();
            int north = alongX ? 0 : 2, south = alongX ? 1 : 3, west = alongX ? 2 : 0, east = alongX ? 3 : 1;
            boolean flipV = room.hasDoor(north) && !room.hasDoor(south);
            boolean flipU = room.hasDoor(west) && !room.hasDoor(east);
            int length = alongX ? w : h, width = alongX ? h : w;
            int u = alongX ? rx : rz, v = alongX ? rz : rx;
            return new Local(flipU ? length - u : u, flipV ? width - v : v, length, width, alongX, flipU, flipV);
        }

        /** Maps a direction in local space (EAST = +u, SOUTH = +v) to the world. */
        Direction dir(Direction local) {
            if (flipU && local.getAxis() == Direction.Axis.X) local = local.getOpposite();
            if (flipV && local.getAxis() == Direction.Axis.Z) local = local.getOpposite();
            if (alongX) return local;
            return switch (local) {
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case WEST -> Direction.NORTH;
                case NORTH -> Direction.WEST;
                default -> local;
            };
        }
    }

    /**
     * Furnishes one column of a room on one floor (walls and floor are already filled in).
     * {@code out} is the column from {@link BackroomsLayout#column}.
     */
    static void furnish(BlockState[] out, Room room, int level, int x, int z) {
        int floorY = BackroomsLayout.floorY(level), y = floorY + 1;
        int rx = x - room.x0(), rz = z - room.z0();
        int w = room.x1() - room.x0(), h = room.z1() - room.z0();
        Local l = Local.of(room, rx, rz, w, h);
        boolean wall = room.onWall(x, z);
        long hash = room.hash();

        // tiled rooms get tiled walls too
        if (wall && !room.isDoorway(x, z) && (room.type() == Type.POOL || room.type() == Type.BATHROOM)) {
            for (int i = 0; i < 3; i++) out[y + i] = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        }
        if (wall) return;

        // Furnished rooms and atriums are lit properly; closets and the door room stay dark on purpose.
        if (room.type() != Type.CLOSET && room.type() != Type.DOOR && (room.type() != Type.HALL || room.atrium())) {
            fixLights(out, floorY);
        }

        // Keep the way in clear: no furniture right inside a doorway (floors still change).
        BlockState[] before = {out[y], out[y + 1], out[y + 2]};
        furnishInterior(out, room, level, x, z, floorY, y, rx, rz, w, h, l, hash);
        if (nearDoorway(room, x, z) && room.type() != Type.HALL && room.type() != Type.POOL) {
            out[y] = before[0];
            out[y + 1] = before[1];
            out[y + 2] = before[2];
        }
    }

    /** Within two blocks of one of the room's doorways. */
    static boolean nearDoorway(Room room, int x, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (room.onWall(x + dx, z + dz) && room.isDoorway(x + dx, z + dz)) return true;
            }
        }
        return false;
    }

    private static void furnishInterior(BlockState[] out, Room room, int level, int x, int z, int floorY, int y,
                                        int rx, int rz, int w, int h, Local l, long hash) {
        switch (room.type()) {
            case HALL -> hall(out, room, level, x, z, rx, rz, w, h);
            case CLOSET -> {
                breakLights(out, floorY);
                if (l.u() == 3 && l.v() == 1) out[y] = stairs(Blocks.OAK_STAIRS, l.dir(Direction.NORTH));
                if (l.u() == 5 && l.v() == 1) out[y + 2] = Blocks.COBWEB.defaultBlockState();
            }
            case KITCHEN -> kitchen(out, floorY, y, l);
            case DINING -> {
                out[floorY] = Blocks.SPRUCE_PLANKS.defaultBlockState();
                int mid = l.width() / 2;
                if (l.v() == mid && l.u() >= 3 && l.u() <= l.length() - 3) {
                    out[y] = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
                }
                if ((l.v() == mid - 1 || l.v() == mid + 1) && l.u() >= 4 && l.u() <= l.length() - 4 && l.u() % 2 == 0) {
                    out[y] = stairs(Blocks.OAK_STAIRS, l.dir(l.v() < mid ? Direction.NORTH : Direction.SOUTH));
                }
                if (l.v() == mid && l.u() == l.length() / 2) {   // a chandelier over the table
                    out[y + 2] = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
                    out[y + 1] = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
                }
            }
            case LIVING -> living(out, y, l);
            case OFFICE -> office(out, y, l);
            case BATHROOM -> {
                out[floorY] = (l.u() + l.v()) % 2 == 0 ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                if (l.u() == 4 && l.v() == 1) out[y] = stairs(Blocks.SMOOTH_QUARTZ_STAIRS, l.dir(Direction.NORTH));   // toilet
                if (l.u() == 2 && l.v() == 1) out[y] = waterCauldron();                                              // sink
                if (l.u() == 1 && (l.v() == 4 || l.v() == 5)) out[y] = waterCauldron();                              // bathtub
            }
            case POOL -> {
                int m = Math.min(Math.min(rx, w - rx), Math.min(rz, h - rz));
                out[floorY] = m >= 3 ? Blocks.WATER.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState();
                BlockState ceiling = out[floorY + 4];
                if (ceiling != null && ceiling.is(ModBlocks.FLUORESCENT_LIGHT.get())) out[floorY + 4] = Blocks.SEA_LANTERN.defaultBlockState();
            }
            case CHAIRS -> {
                if (l.u() % 2 == 1 && l.v() % 2 == 0 && l.u() >= 2 && l.u() <= l.length() - 2 && l.v() >= 2 && l.v() <= l.width() - 2) {
                    // rows of chairs all facing the same wall... except one
                    boolean odd = l.u() == 3 + 2 * (int) Math.floorMod(hash >>> 3, (long) (l.length() / 2 - 2))
                            && l.v() == 2 + 2 * (int) Math.floorMod(hash >>> 11, (long) (l.width() / 2 - 1));
                    out[y] = stairs(Blocks.OAK_STAIRS, l.dir(odd ? Direction.NORTH : Direction.SOUTH));
                }
            }
            case DOOR -> {
                breakLights(out, floorY);
                if (l.u() == 3 && l.v() == 3) {   // a door standing on its own, going nowhere
                    Direction facing = l.dir((hash & 1) == 0 ? Direction.NORTH : Direction.EAST);
                    out[y] = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
                    out[y + 1] = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
                }
            }
        }
    }

    /** Big and empty. An atrium opens both floors into one tall hall, with a railed balcony upstairs. */
    private static void hall(BlockState[] out, Room room, int level, int x, int z, int rx, int rz, int w, int h) {
        if (!room.atrium()) return;
        int m = Math.min(Math.min(rx, w - rx), Math.min(rz, h - rz));
        boolean pillar = Math.floorMod(x, CELL) == 0 && Math.floorMod(z, CELL) == 0;
        int upperFloor = BackroomsLayout.floorY(1);
        if (level == 0) {
            if (pillar) {
                out[BackroomsLayout.CEILING_Y] = ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState();
                out[upperFloor] = ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState();
            } else if (m >= 4) {
                out[BackroomsLayout.CEILING_Y] = null;
                out[upperFloor] = null;
            }
        } else if (m == 3 && !pillar) {
            out[upperFloor + 1] = Blocks.IRON_BARS.defaultBlockState();   // railing at the edge of a 2-wide balcony
        }
    }

    private static void kitchen(BlockState[] out, int floorY, int y, Local l) {
        out[floorY] = (l.u() + l.v()) % 2 == 0 ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState();
        Direction front = l.dir(Direction.SOUTH);
        if (l.v() == 1) {   // counters along the back wall
            BlockState counter = Blocks.POLISHED_ANDESITE.defaultBlockState();
            out[y] = switch (l.u()) {
                case 1, 7 -> Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
                case 2 -> Blocks.SMOKER.defaultBlockState().setValue(FurnaceBlock.FACING, front);
                case 4 -> waterCauldron();
                case 6 -> Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, front);
                case 10, 11 -> Blocks.IRON_BLOCK.defaultBlockState();          // the fridge...
                default -> counter;
            };
            if (l.u() >= 10) out[y + 1] = Blocks.IRON_BLOCK.defaultBlockState();   // ...two blocks tall
        }
        if (l.v() == 4) {   // a little table with two chairs
            if (l.u() == 6) {
                out[y] = Blocks.OAK_FENCE.defaultBlockState();
                out[y + 1] = Blocks.OAK_PRESSURE_PLATE.defaultBlockState();
            }
            if (l.u() == 5) out[y] = stairs(Blocks.OAK_STAIRS, l.dir(Direction.WEST));
            if (l.u() == 7) out[y] = stairs(Blocks.OAK_STAIRS, l.dir(Direction.EAST));
        }
    }

    private static void living(BlockState[] out, int y, Local l) {
        int u = l.u(), v = l.v();
        if (u >= 4 && u <= 8 && v >= 4 && v <= 8) {   // a rug
            out[y] = ((u + v) % 2 == 0 ? Blocks.RED_CARPET : Blocks.WHITE_CARPET).defaultBlockState();
        }
        if (v == 2 && u >= 4 && u <= 8) out[y] = stairs(Blocks.SPRUCE_STAIRS, l.dir(Direction.NORTH));   // couch
        if (v == 10 && u >= 5 && u <= 7) {   // the TV on its stand
            out[y] = Blocks.DARK_OAK_SLAB.defaultBlockState();
            out[y + 1] = Blocks.BLACK_CONCRETE.defaultBlockState();
        }
        if ((u == 1 || u == 11) && v == 1) {
            out[y] = Blocks.BOOKSHELF.defaultBlockState();
            out[y + 1] = Blocks.BOOKSHELF.defaultBlockState();
        }
        if ((u == 3 || u == 9) && v == 2) {   // lamps either side of the couch
            out[y] = Blocks.OAK_FENCE.defaultBlockState();
            out[y + 1] = Blocks.LANTERN.defaultBlockState();
        }
        if ((u == 1 || u == 11) && v == 11) out[y] = Blocks.POTTED_FERN.defaultBlockState();
    }

    private static void office(BlockState[] out, int y, Local l) {
        int u = l.u(), v = l.v();
        if (v == 3 && (u == 2 || u == 5 || u == 8)) {
            out[y] = Blocks.BIRCH_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);   // desk
            if (u == 5) out[y + 1] = Blocks.CANDLE.defaultBlockState();
        }
        if (v == 4 && (u == 2 || u == 5 || u == 8)) out[y] = stairs(Blocks.SPRUCE_STAIRS, l.dir(Direction.SOUTH));
        if (v == 1 && u >= 10) {   // filing cabinets
            out[y] = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
            out[y + 1] = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
        }
        if (v == 1 && u <= 3) {
            out[y] = Blocks.BOOKSHELF.defaultBlockState();
            out[y + 1] = Blocks.BOOKSHELF.defaultBlockState();
        }
    }

    private static void fixLights(BlockState[] out, int floorY) {
        BlockState ceiling = out[floorY + 4];
        if (ceiling != null && ceiling.is(ModBlocks.FLUORESCENT_LIGHT.get()) && ceiling.getValue(FluorescentLightBlock.BROKEN)) {
            out[floorY + 4] = ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState();
        }
    }

    private static void breakLights(BlockState[] out, int floorY) {
        BlockState ceiling = out[floorY + 4];
        if (ceiling != null && ceiling.is(ModBlocks.FLUORESCENT_LIGHT.get())) out[floorY + 4] = FluorescentLightBlock.broken(ceiling);
    }

    private static BlockState stairs(Block block, Direction facing) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static BlockState waterCauldron() {
        return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
    }

    // ------------------------------------------------------------------ hashing (same scheme as BackroomsLayout)

    private static long hash(long seed, int a, int b, int salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }
}
