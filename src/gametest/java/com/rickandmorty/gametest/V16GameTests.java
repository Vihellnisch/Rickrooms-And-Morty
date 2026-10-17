package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.EndlessFieldLevel;
import com.rickandmorty.worldgen.level.EndlessFieldLevel.Feature;
import com.rickandmorty.worldgen.level.EndlessFieldLevel.Placed;
import com.rickandmorty.worldgen.level.LevelGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;
import java.util.Map;

/** Tests for the Endless Field. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V16GameTests {
    private static final String EMPTY = "empty";
    private static final long SEED = 2001L;

    private static EndlessFieldLevel field() {
        return (EndlessFieldLevel) LevelGenerators.get(BackroomsLevel.ENDLESS_FIELD);
    }

    private static Placed find(Feature feature) {
        for (int cx = 0; cx < 200; cx++) {
            for (int cz = 0; cz < 40; cz++) {
                Placed p = EndlessFieldLevel.placed(SEED, cx, cz);
                if (p != null && p.feature() == feature) return p;
            }
        }
        throw new IllegalStateException("no " + feature);
    }

    @GameTest(template = EMPTY)
    public static void everythingTurnsUpNowAndThen(GameTestHelper helper) {
        Map<Feature, Integer> counts = new EnumMap<>(Feature.class);
        int empty = 0;
        for (int cx = 0; cx < 80; cx++) {
            for (int cz = 0; cz < 80; cz++) {
                Placed p = EndlessFieldLevel.placed(SEED, cx, cz);
                if (p == null) empty++;
                else counts.merge(p.feature(), 1, Integer::sum);
            }
        }
        for (Feature f : Feature.values()) helper.assertTrue(counts.getOrDefault(f, 0) > 0, "no " + f + " in " + counts);
        helper.assertTrue(empty > 80 * 80 / 2, "the field is too full: only " + empty + " empty squares");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theHillsAreSmooth(GameTestHelper helper) {
        int steep = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int z = 0; z < 4000; z += 250) {
            for (int x = 0; x < 3000; x++) {
                int a = EndlessFieldLevel.groundAt(SEED, x, z), b = EndlessFieldLevel.groundAt(SEED, x + 1, z);
                helper.assertTrue(Math.abs(a - b) <= 2, "a cliff at " + x + "," + z + ": " + a + " to " + b);
                if (Math.abs(a - b) == 2) steep++;
                min = Math.min(min, a);
                max = Math.max(max, a);
            }
        }
        helper.assertTrue(steep < 60, steep + " steep steps");
        helper.assertTrue(max - min >= 10, "it's just flat: " + min + ".." + max);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theStairsGoDownAndTheDoorGoesHome(GameTestHelper helper) {
        EndlessFieldLevel field = field();
        Placed well = find(Feature.STAIRWELL);
        boolean door = false, sign = false;
        for (int x = well.x() - 10; x <= well.x() + 10; x++) {
            for (int z = well.z() - 10; z <= well.z() + 10; z++) {
                BlockState[] c = field.column(SEED, x, z);
                for (int y = 1; y < c.length; y++) {
                    if (c[y] == null) continue;
                    if (c[y].is(Blocks.BIRCH_WALL_SIGN)) sign = true;
                    if (c[y].is(ModBlocks.EXIT_DOOR.get()) && c[y].getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                        door = true;
                        helper.assertTrue(y < well.y0() - 20, "the door isn't far down: " + y + " vs " + well.y0());
                        helper.assertTrue(field.exitDestination(SEED, new BlockPos(x, y, z)) == BackroomsLevel.LEVEL_0, "the door at the bottom doesn't go down");
                    }
                }
            }
        }
        helper.assertTrue(door && sign, "door " + door + ", sign " + sign);
        Placed lone = find(Feature.DOOR);
        BlockState[] c = field.column(SEED, lone.x(), lone.z());
        helper.assertTrue(c[lone.y0() + 1] != null && c[lone.y0() + 1].is(ModBlocks.LONE_DOOR.get()), "no lone door");
        helper.assertTrue(field.exitDestination(SEED, new BlockPos(lone.x(), lone.y0() + 1, lone.z())) == null, "the lone door doesn't go home");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void youArriveStandingInTheGrass(GameTestHelper helper) {
        EndlessFieldLevel field = field();
        RandomSource random = RandomSource.create(9);
        for (int i = 0; i < 25; i++) {
            Vec3 at = field.landing(SEED, random);
            BlockState[] c = field.column(SEED, (int) Math.floor(at.x), (int) Math.floor(at.z));
            int y = (int) at.y;
            helper.assertTrue(c[y - 1] != null && c[y - 1].is(Blocks.GRASS_BLOCK) && c[y + 1] == null, "a bad landing at " + at);
        }
        helper.succeed();
    }
}
