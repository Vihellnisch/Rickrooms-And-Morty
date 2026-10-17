package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.DropLimitLevel;
import com.rickandmorty.worldgen.level.FacadeLevel;
import com.rickandmorty.worldgen.level.FroopylandLevel;
import com.rickandmorty.worldgen.level.LevelGenerator;
import com.rickandmorty.worldgen.level.LevelGenerators;
import com.rickandmorty.worldgen.level.RunLevel;
import com.rickandmorty.worldgen.level.StaircaseLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** v0.23: Drop Limit, the Facade, the Staircase, Froopyland and Run For Your Life. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V23GameTests {
    private static final String EMPTY = "empty";
    private static final long SEED = 23023023L;
    private static final List<BackroomsLevel> LEVELS = List.of(BackroomsLevel.DROP_LIMIT, BackroomsLevel.FACADE, BackroomsLevel.STAIRCASE,
            BackroomsLevel.FROOPYLAND, BackroomsLevel.RUN_FOR_YOUR_LIFE);

    @GameTest(template = EMPTY)
    public static void youArriveSomewhereToStand(GameTestHelper helper) {
        for (BackroomsLevel level : LEVELS) {
            LevelGenerator generator = LevelGenerators.get(level);
            RandomSource random = RandomSource.create(5);
            for (int i = 0; i < 6; i++) {
                Vec3 at = generator.landing(SEED, random);
                BlockState[] c = generator.column(SEED, (int) Math.floor(at.x), (int) Math.floor(at.z));
                int y = (int) Math.floor(at.y);
                helper.assertTrue(LevelGenerator.solid(c[y - 1]) && LevelGenerator.clear(c[y]) && LevelGenerator.clear(c[y + 1]),
                        level + " landing at " + at + " has nothing to stand on");
                helper.assertFalse(c[y - 1].is(ModBlocks.CLOUD.get()), level + " lands you on a cloud");
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void cloudsHoldNothingUp(GameTestHelper helper) {
        BlockState cloud = ModBlocks.CLOUD.get().defaultBlockState();
        helper.assertTrue(cloud.getCollisionShape(helper.getLevel(), BlockPos.ZERO).isEmpty(), "clouds have a collision shape");
        helper.assertFalse(LevelGenerator.solid(cloud), "clouds count as solid ground");
        helper.assertTrue(cloud.canOcclude(), "the cloud sea should hide its insides (it's opaque)");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theLevelsFallIntoEachOther(GameTestHelper helper) {
        helper.assertValueEqual(BackroomsLevel.STAIRCASE.fallTarget(), BackroomsLevel.DROP_LIMIT, "off the Staircase");
        helper.assertValueEqual(BackroomsLevel.DROP_LIMIT.fallTarget(), BackroomsLevel.FACADE, "through the clouds");
        helper.assertValueEqual(BackroomsLevel.FACADE.fallTarget(), BackroomsLevel.LEVEL_0, "down the Facade");
        helper.assertTrue(BackroomsLevel.FROOPYLAND.sanity < 0, "Froopyland should clear your head");
        helper.assertTrue(BackroomsLevel.RUN_FOR_YOUR_LIFE.sanity > 1, "the hall should be frightening");
        helper.succeed();
    }

    /** One DROP LIMIT sign per road end, not a row of them along a winding road. */
    @GameTest(template = EMPTY)
    public static void roadsEndAtOneSign(GameTestHelper helper) {
        int signs = 0;
        for (int lane = 0; lane < 60 && signs < 3; lane++) {
            if (DropLimitLevel.roadZ(SEED, lane, 0) == Integer.MIN_VALUE) continue;
            for (int x = 0; x < 4000 && signs < 3; x++) {
                if (!DropLimitLevel.endsAhead(SEED, lane, x, true, 1)) continue;
                int z = DropLimitLevel.roadZ(SEED, lane, x) + 5;
                if (!DropLimitLevel.island(SEED, x, z)) continue;
                BlockState[] c = new DropLimitLevel().column(SEED, x, z);
                int top = DropLimitLevel.islandTop(SEED, x, z);
                helper.assertTrue(c[top + 3] != null && c[top + 3].is(Blocks.BIRCH_SIGN), "no sign where the road at " + x + "," + z + " runs out");
                for (int k = 1; k <= 6; k++) {
                    helper.assertFalse(DropLimitLevel.endsAhead(SEED, lane, x + k, true, 1), "a second sign " + k + " past the one at " + x);
                }
                signs++;
            }
        }
        helper.assertTrue(signs > 0, "no road ends found");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theFacadeHasRoomsBehindOpenWindows(GameTestHelper helper) {
        FacadeLevel facade = new FacadeLevel();
        int found = 0;
        for (int bay = 0; bay < 400 && found < 3; bay++) {
            int floor = 50;
            if (!FacadeLevel.room(SEED, bay, floor, 1)) continue;
            int x = bay * FacadeLevel.BAY + 2, y = floor * FacadeLevel.FLOOR + 1;
            BlockState[] face = facade.column(SEED, x, FacadeLevel.DEPTH - 1), inside = facade.column(SEED, x, FacadeLevel.DEPTH - 3);
            helper.assertTrue(face[y] == null && face[y + 1] == null, "the window of the room at bay " + bay + " isn't open");
            helper.assertTrue(LevelGenerator.solid(inside[y - 1]) && LevelGenerator.clear(inside[y + 1]), "the room at bay " + bay + " has no floor or no space");
            found++;
        }
        helper.assertTrue(found > 0, "no rooms found");
        helper.succeed();
    }

    /** The hall is a maze of straight runs that turn and fork, with the way out at the end of many dead ends. */
    @GameTest(template = EMPTY)
    public static void theHallIsAMazeWithWaysOut(GameTestHelper helper) {
        RunLevel run = new RunLevel();
        int forks = 0;
        for (int m = 0; m < 6; m++) {
            RunLevel.Maze maze = RunLevel.maze(SEED, m, -m);
            int cells = 0, exits = 0;
            for (int i = 0; i < RunLevel.CELLS; i++) {
                for (int k = 0; k < RunLevel.CELLS; k++) {
                    int links = maze.links(i, k);
                    if (links == 0) continue;
                    cells++;
                    if (Integer.bitCount(links) >= 3) forks++;
                    if (!maze.exit()[i * RunLevel.CELLS + k]) continue;
                    exits++;
                    helper.assertTrue(Integer.bitCount(links) == 1, "an exit that isn't at a dead end");
                    // the door is in the middle of the wall facing the way in
                    int[] mid = RunLevel.middle(m, -m, i, k);
                    int wall = RunLevel.exitWall(links), half = RunLevel.CELL / 2;
                    int x = mid[0] + (wall == RunLevel.EAST ? half : wall == RunLevel.WEST ? -half : 0);
                    int z = mid[1] + (wall == RunLevel.SOUTH ? half : wall == RunLevel.NORTH ? -half : 0);
                    helper.assertTrue(run.column(SEED, x, z)[RunLevel.FLOOR + 1].is(ModBlocks.EXIT_DOOR.get()), "no exit door at " + x + "," + z);
                    helper.assertValueEqual(run.exitDestination(SEED, new BlockPos(x, RunLevel.FLOOR + 1, z)), BackroomsLevel.SUNLIT_LOBBY, "where it leads");
                }
            }
            helper.assertTrue(cells >= 60, "maze " + m + " has only " + cells + " cells of hall");
            helper.assertTrue(exits >= 1, "maze " + m + " has no way out");
        }
        helper.assertTrue(forks > 0, "the hall never forks");
        // the side doors are locked iron ones
        boolean locked = false;
        int[] start = RunLevel.middle(0, 0, RunLevel.START_I, RunLevel.START_K);
        for (int x = start[0] - 3; x <= start[0] + 3; x++) {
            for (int dz : new int[]{-3, 3}) locked |= run.column(SEED, x, start[1] + dz)[RunLevel.FLOOR + 1].is(Blocks.IRON_DOOR);
        }
        helper.assertTrue(locked, "no locked doors by the start");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void froopylandIsSoftAndHasAWayHome(GameTestHelper helper) {
        BlockState turf = ModBlocks.turf(0);
        helper.assertTrue(turf.getBlock() instanceof com.rickandmorty.block.SkyBlocks.FroopyTurf, "the ground isn't Froopy turf");
        FroopylandLevel froopy = new FroopylandLevel();
        int doors = 0;
        for (int cx = 0; cx < 10; cx++) {
            for (int cz = 0; cz < 10; cz++) {
                int[] d = FroopylandLevel.door(SEED, cx, cz);
                if (d == null || FroopylandLevel.riverAcross(SEED, d[0], d[1]) != Integer.MIN_VALUE) continue;
                int g = FroopylandLevel.ground(SEED, d[0], d[1]);
                helper.assertTrue(froopy.column(SEED, d[0], d[1])[g + 1].is(ModBlocks.EXIT_DOOR.get()), "no door at " + d[0] + "," + d[1]);
                helper.assertTrue(froopy.exitDestination(SEED, new BlockPos(d[0], g + 1, d[1])) == null, "the grass doors should go home");
                doors++;
            }
        }
        helper.assertTrue(doors > 0, "no doors home");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theStaircaseIsJoinedUp(GameTestHelper helper) {
        int flights = 0;
        for (int i = 0; i < 20; i++) {
            for (int k = 0; k < 20; k++) {
                for (int j = StaircaseLevel.LOWEST; j <= StaircaseLevel.HIGHEST; j++) {
                    if (StaircaseLevel.flight(SEED, i, k, j, true) != 0 || StaircaseLevel.flight(SEED, i, k, j, false) != 0) flights++;
                }
            }
        }
        helper.assertTrue(flights > 100, "only " + flights + " flights of stairs");
        helper.succeed();
    }
}
