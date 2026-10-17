package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.worldgen.BackroomsLayout;
import com.rickandmorty.worldgen.BackroomsRooms;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;
import java.util.Map;

/** Backrooms rooms: every type shows up, has walls and a way in, and its furniture. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class RoomGameTests {
    private static final long SEED = 987654321L;

    /** Each kind of room must contain at least one of these (a doorway may clear a piece or two). */
    private static final Map<BackroomsRooms.Type, Block[]> SIGNATURE = new EnumMap<>(Map.of(
            BackroomsRooms.Type.KITCHEN, new Block[]{Blocks.IRON_BLOCK, Blocks.SMOKER, Blocks.FURNACE, Blocks.WATER_CAULDRON},
            BackroomsRooms.Type.DINING, new Block[]{Blocks.DARK_OAK_SLAB, Blocks.LANTERN},
            BackroomsRooms.Type.LIVING, new Block[]{Blocks.BLACK_CONCRETE, Blocks.SPRUCE_STAIRS, Blocks.BOOKSHELF},
            BackroomsRooms.Type.OFFICE, new Block[]{Blocks.BARREL, Blocks.BIRCH_SLAB},
            BackroomsRooms.Type.BATHROOM, new Block[]{Blocks.WATER_CAULDRON, Blocks.SMOOTH_QUARTZ_STAIRS},
            BackroomsRooms.Type.POOL, new Block[]{Blocks.WATER},
            BackroomsRooms.Type.CHAIRS, new Block[]{Blocks.OAK_STAIRS},
            BackroomsRooms.Type.DOOR, new Block[]{Blocks.IRON_DOOR},
            BackroomsRooms.Type.CLOSET, new Block[]{Blocks.OAK_STAIRS, Blocks.COBWEB}));

    @GameTest(template = "empty")
    public static void everyRoomTypeGenerates(GameTestHelper helper) {
        Map<BackroomsRooms.Type, BackroomsRooms.Room> found = new EnumMap<>(BackroomsRooms.Type.class);
        BackroomsRooms.Room atrium = null;
        int squares = 0, rooms = 0;
        for (int gx = -30; gx < 30; gx++) {
            for (int gz = -30; gz < 30; gz++) {
                squares++;
                BackroomsRooms.Room room = BackroomsRooms.roomIn(SEED, 0, gx, gz);
                if (room == null) continue;
                rooms++;
                found.putIfAbsent(room.type(), room);
                if (room.atrium() && atrium == null) atrium = room;
            }
        }
        helper.assertTrue(found.size() == BackroomsRooms.Type.values().length, "room types found: " + found.keySet());
        helper.assertTrue(rooms > squares * 0.25 && rooms < squares * 0.65, "rooms " + rooms + " of " + squares);

        for (BackroomsRooms.Room room : found.values()) checkRoom(room, 0);
        helper.assertTrue(atrium != null, "no atrium hall");
        checkAtrium(atrium);
        helper.succeed();
    }

    private static void checkRoom(BackroomsRooms.Room room, int level) {
        int floorY = BackroomsLayout.floorY(level);
        int doorway = 0;
        Block[] wanted = SIGNATURE.get(room.type());
        for (int x = room.x0(); x <= room.x1(); x++) {
            for (int z = room.z0(); z <= room.z1(); z++) {
                BlockState[] column = BackroomsLayout.column(SEED, x, z);
                boolean wall = BackroomsLayout.isWall(SEED, level, x, z);
                if (room.onWall(x, z)) {
                    if (!wall) {
                        doorway++;
                        // the block just inside a doorway must be walkable
                        int ix = x == room.x0() ? x + 1 : x == room.x1() ? x - 1 : x;
                        int iz = z == room.z0() ? z + 1 : z == room.z1() ? z - 1 : z;
                        BlockState inside = BackroomsLayout.column(SEED, ix, iz)[floorY + 1];
                        check(inside == null || inside.is(net.minecraft.tags.BlockTags.WOOL_CARPETS), room, "doorway blocked by " + inside, ix, iz);
                    }
                    else check(column[floorY + 2] != null, room, "hole in the wall", x, z);
                } else {
                    boolean pillar = Math.floorMod(x, BackroomsLayout.CELL) == 0 && Math.floorMod(z, BackroomsLayout.CELL) == 0;
                    check(!wall || (room.type() == BackroomsRooms.Type.HALL && pillar), room, "maze wall inside", x, z);
                }
            }
        }
        check(doorway >= 2, room, "no doorway", room.x0(), room.z0());
        check(wanted == null || containsAny(room, level, wanted), room, "has none of its furniture", room.x0(), room.z0());
    }

    private static boolean containsAny(BackroomsRooms.Room room, int level, Block... blocks) {
        int floorY = BackroomsLayout.floorY(level);
        for (int x = room.x0(); x <= room.x1(); x++) {
            for (int z = room.z0(); z <= room.z1(); z++) {
                BlockState[] column = BackroomsLayout.column(SEED, x, z);
                for (int y = floorY; y <= floorY + 3; y++) {
                    for (Block b : blocks) if (column[y] != null && column[y].is(b)) return true;
                }
            }
        }
        return false;
    }

    private static void checkAtrium(BackroomsRooms.Room hall) {
        int cx = (hall.x0() + hall.x1()) / 2 + 1, cz = (hall.z0() + hall.z1()) / 2 + 1;
        BlockState[] middle = BackroomsLayout.column(SEED, cx, cz);
        check(middle[BackroomsLayout.CEILING_Y] == null && middle[BackroomsLayout.floorY(1)] == null, hall, "atrium not open", cx, cz);
        boolean railing = false;
        for (int x = hall.x0(); x <= hall.x1(); x++) {
            BlockState[] column = BackroomsLayout.column(SEED, x, hall.z0() + 3);
            if (column[BackroomsLayout.standY(1)] != null && column[BackroomsLayout.standY(1)].is(Blocks.IRON_BARS)) railing = true;
        }
        check(railing, hall, "no balcony railing", hall.x0(), hall.z0());
        check(BackroomsLayout.isOpenCell(SEED, 0, Math.floorDiv(cx, 6), Math.floorDiv(cz, 6)), hall, "can't land in a hall", cx, cz);
        check(!BackroomsLayout.isOpenCell(SEED, 1, Math.floorDiv(cx, 6), Math.floorDiv(cz, 6)), hall, "landing over the atrium hole", cx, cz);
    }

    private static void check(boolean ok, BackroomsRooms.Room room, String what, int x, int z) {
        if (!ok) throw new GameTestAssertException(room.type() + " room: " + what + " at " + x + ", " + z);
    }
}
