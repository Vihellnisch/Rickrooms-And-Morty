package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.worldgen.level.AbandonedCityLevel;
import com.rickandmorty.worldgen.level.AbandonedCityLevel.Kind;
import com.rickandmorty.worldgen.level.AbandonedCityLevel.Lot;
import com.rickandmorty.worldgen.level.AbandonedCityLevel.Tree;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
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

import static com.rickandmorty.worldgen.level.AbandonedCityLevel.CELL;
import static com.rickandmorty.worldgen.level.AbandonedCityLevel.G;
import static com.rickandmorty.worldgen.level.AbandonedCityLevel.STREET;

/** Tests for the Abandoned City. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V20GameTests {
    private static final String EMPTY = "empty";
    private static final long SEED = 4242L;

    private static AbandonedCityLevel city() {
        return (AbandonedCityLevel) LevelGenerators.get(BackroomsLevel.ABANDONED_CITY);
    }

    @GameTest(template = EMPTY)
    public static void theCityHasEverything(GameTestHelper helper) {
        Map<Kind, Integer> kinds = new EnumMap<>(Kind.class);
        int trees = 0, megaTrees = 0;
        for (int cx = 0; cx < 30; cx++) {
            for (int cz = 0; cz < 30; cz++) {
                kinds.merge(city().lot(SEED, cx, cz).kind(), 1, Integer::sum);
                for (Tree tree : city().trees(SEED, cx, cz)) {
                    trees++;
                    if (tree.height() >= 68) megaTrees++;
                }
            }
        }
        for (Kind kind : Kind.values()) helper.assertTrue(kinds.getOrDefault(kind, 0) > 0, "no " + kind + " in " + kinds);
        helper.assertTrue(trees > 300 && megaTrees > 20, trees + " giant trees, " + megaTrees + " huge ones");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void youArriveInTheStreet(GameTestHelper helper) {
        RandomSource random = RandomSource.create(9);
        for (int i = 0; i < 30; i++) {
            Vec3 at = city().landing(SEED, random);
            int x = (int) Math.floor(at.x), y = (int) at.y, z = (int) Math.floor(at.z);
            helper.assertTrue(Math.floorMod(x, CELL) < STREET || Math.floorMod(z, CELL) < STREET, "landed off the street at " + at);
            BlockState[] column = city().column(SEED, x, z);
            helper.assertTrue(y <= G + 1 && !column[y - 1].isAir() && (column[y] == null || !column[y].blocksMotion())
                    && (column[y + 1] == null || !column[y + 1].blocksMotion()), "nowhere to stand at " + at);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void someEntrancesHaveTheExitDoor(GameTestHelper helper) {
        int doors = 0;
        for (int cx = 0; cx < 20 && doors < 5; cx++) {
            for (int cz = 0; cz < 20 && doors < 5; cz++) {
                Lot lot = city().lot(SEED, cx, cz);
                if (!AbandonedCityLevel.hasExit(SEED, lot) || lot.holeR() > 0) continue;
                int x = lot.worldX(lot.x0() + (lot.x1() - lot.x0()) / 2), z = lot.worldZ(lot.z0());
                BlockState[] column = city().column(SEED, x, z);
                helper.assertTrue(column[G] != null && column[G].is(ModBlocks.EXIT_DOOR.get()) && column[G].getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                        && column[G + 1].is(ModBlocks.EXIT_DOOR.get()), "no exit door in the " + lot.kind() + " at " + x + "," + z);
                var to = city().exitDestination(SEED, new BlockPos(x, G, z));
                helper.assertTrue(to != BackroomsLevel.ABANDONED_CITY, "an exit door that goes nowhere");
                doors++;
            }
        }
        helper.assertTrue(doors >= 5, "only " + doors + " exit doors");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void mossBuildingsAreMoss(GameTestHelper helper) {
        Lot lot = null;
        for (int cx = 0; cx < 20 && lot == null; cx++) {
            for (int cz = 0; cz < 20 && lot == null; cz++) {
                if (city().lot(SEED, cx, cz).kind() == Kind.MOSS) lot = city().lot(SEED, cx, cz);
            }
        }
        helper.assertTrue(lot != null, "no moss building");
        int moss = 0, other = 0;
        for (int ax = lot.x0(); ax <= lot.x1(); ax++) {
            BlockState[] column = city().column(SEED, lot.worldX(ax), lot.worldZ(lot.z0()));
            for (int y = G; y < lot.top(); y++) {
                if (column[y] == null) continue;
                if (column[y].is(Blocks.MOSS_BLOCK) || column[y].is(BlockTags.LEAVES) || column[y].is(Blocks.MOSSY_COBBLESTONE)) moss++;
                else other++;
            }
        }
        helper.assertTrue(moss > other * 9, "a moss building of " + moss + " moss and " + other + " other blocks");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void giantTreesHaveTrunks(GameTestHelper helper) {
        int checked = 0;
        for (int cx = 0; cx < 10; cx++) {
            for (Tree tree : city().trees(SEED, cx, 3)) {
                BlockState[] column = city().column(SEED, tree.x(), tree.z());
                for (int y = G; y < G + tree.height() / 2; y++) {
                    helper.assertTrue(column[y] != null && column[y].is(BlockTags.LOGS), "no trunk at " + tree.x() + "," + y + "," + tree.z() + ": " + column[y]);
                }
                checked++;
            }
        }
        helper.assertTrue(checked > 2, "only " + checked + " trees to look at");
        helper.succeed();
    }
}
