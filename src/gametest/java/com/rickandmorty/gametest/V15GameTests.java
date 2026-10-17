package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.command.GotoCommand;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.DeadMallLevel;
import com.rickandmorty.worldgen.level.DeadMallLevel.Court;
import com.rickandmorty.worldgen.level.DeadMallLevel.Kind;
import com.rickandmorty.worldgen.level.LevelGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Tests for the Dead Mall and the goto command. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V15GameTests {
    private static final String EMPTY = "empty";
    private static final long SEED = 1985L;

    private static DeadMallLevel mall() {
        return (DeadMallLevel) LevelGenerators.get(BackroomsLevel.DEAD_MALL);
    }

    @GameTest(template = EMPTY)
    public static void theMallHangsTogether(GameTestHelper helper) {
        // every court has a concourse, and most of them join up into one mall
        int n = 12;
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{0, 0});
        seen.add(0L);
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            int i = at[0], j = at[1];
            int[][] next = {{i + 1, j, i, j, 0}, {i, j + 1, i, j, 1}, {i - 1, j, i - 1, j, 0}, {i, j - 1, i, j - 1, 1}};
            for (int[] e : next) {
                if (e[0] < 0 || e[1] < 0 || e[0] >= n || e[1] >= n || !DeadMallLevel.edge(SEED, e[2], e[3], e[4])) continue;
                if (seen.add((long) e[0] * 1000 + e[1])) queue.add(new int[]{e[0], e[1]});
            }
        }
        helper.assertTrue(seen.size() > n * n * 0.6, "only " + seen.size() + " of " + n * n + " courts join up");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theMallHasEverything(GameTestHelper helper) {
        DeadMallLevel mall = mall();
        Map<Kind, Integer> kinds = new EnumMap<>(Kind.class);
        Map<Court, Integer> courts = new EnumMap<>(Court.class);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) courts.merge(DeadMallLevel.node(SEED, i, j).court(), 1, Integer::sum);
        }
        for (int x = 0; x < 400; x += 3) {
            for (int z = 0; z < 400; z += 3) kinds.merge(mall.spot(SEED, x, z).kind(), 1, Integer::sum);
        }
        for (Kind k : Kind.values()) helper.assertTrue(kinds.getOrDefault(k, 0) > 0, "no " + k + " in " + kinds);
        for (Court c : Court.values()) helper.assertTrue(courts.getOrDefault(c, 0) > 0, "no " + c + " court");
        // you can stand on both floors of a concourse
        int both = 0;
        for (int x = 0; x < 400 && both < 20; x += 2) {
            for (int z = 0; z < 400 && both < 20; z += 2) {
                if (mall.spot(SEED, x, z).kind() != Kind.CORRIDOR) continue;
                if (mall.standY(SEED, x, z, DeadMallLevel.LOW) == DeadMallLevel.LOW && mall.standY(SEED, x, z, DeadMallLevel.UP) == DeadMallLevel.UP) both++;
            }
        }
        helper.assertTrue(both >= 20, "hardly anywhere to stand on both floors: " + both);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void escalatorsGoUpstairs(GameTestHelper helper) {
        DeadMallLevel mall = mall();
        for (int i = 0; i < 20; i++) {
            DeadMallLevel.Node n = DeadMallLevel.node(SEED, i, 0);
            if (n.court() != Court.ATRIUM) continue;
            int start = n.x() + n.radius() - 12;
            for (int step = 0; step < 8; step++) {
                BlockState[] column = mall.column(SEED, start + step, n.z());
                BlockState stair = column[DeadMallLevel.LOW + step];
                helper.assertTrue(stair != null && stair.is(ModBlocks.MALL_ESCALATOR.get()), "no step " + step + " at height " + (DeadMallLevel.LOW + step));
            }
            BlockState[] landing = mall.column(SEED, start + 8, n.z());
            helper.assertTrue(landing[DeadMallLevel.UP_FLOOR] != null && landing[DeadMallLevel.UP] == null, "no landing at the top");
            helper.succeed();
            return;
        }
        helper.fail("no atrium found");
    }

    @GameTest(template = EMPTY)
    public static void gotoKnowsEveryLevel(GameTestHelper helper) {
        for (BackroomsLevel level : BackroomsLevel.values()) {
            helper.assertTrue(GotoCommand.places().contains(level.getSerializedName()), "goto doesn't know " + level);
        }
        helper.assertTrue(GotoCommand.places().contains("overworld") && GotoCommand.places().contains("cronenberg"), "goto doesn't know the way home");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theGumballMachineTakesCoins(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.GUMBALL_MACHINE.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_NUGGET, 2));
        BlockPos abs = helper.absolutePos(pos);
        helper.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "it didn't take the coin");
        helper.assertTrue(player.getInventory().countItem(ModItems.GUMBALL.get()) == 1, "no gumball");
        helper.succeed();
    }
}
