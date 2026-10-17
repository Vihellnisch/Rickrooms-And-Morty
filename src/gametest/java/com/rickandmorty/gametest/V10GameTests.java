package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.PartyBlocks;
import com.rickandmorty.entity.FarmScarecrowEntity;
import com.rickandmorty.entity.PartygoerEntity;
import com.rickandmorty.entity.ScarecrowEntity;
import com.rickandmorty.entity.WatcherSpawner;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.sanity.StrangeEvents;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests for v0.10: the farm scarecrow, Level Fun and its Partygoers, strange events, and the small tweaks. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V10GameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void farmScarecrowIsBuiltFromHayAndAHead(GameTestHelper helper) {
        BlockPos base = new BlockPos(2, 2, 2);
        helper.setBlock(base, Blocks.HAY_BLOCK);
        helper.setBlock(base.above(), Blocks.HAY_BLOCK);
        helper.setBlock(base.above(2), ModBlocks.SCARECROW_HEAD.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        FarmScarecrowEntity scarecrow = FarmScarecrowEntity.tryBuild(helper.getLevel(), helper.absolutePos(base.above(2)), null);
        helper.assertTrue(scarecrow != null, "nothing came to life");
        helper.assertBlockPresent(Blocks.AIR, base);
        helper.assertBlockPresent(Blocks.AIR, base.above(2));
        helper.assertTrue(FarmScarecrowEntity.tryBuild(helper.getLevel(), helper.absolutePos(new BlockPos(5, 2, 5)), null) == null, "built from thin air");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 900)
    public static void farmScarecrowMindsTheCrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.setDayTime(6000);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                if (x == 3 && z == 3) continue;
                helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WHEAT.defaultBlockState());
            }
        }
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.WATER);
        FarmScarecrowEntity scarecrow = helper.spawn(ModEntities.FARM_SCARECROW.get(), new BlockPos(3, 2, 8));
        helper.runAfterDelay(800, () -> {
            helper.assertTrue(scarecrow.tendedCrops() >= 40, "it's only minding " + scarecrow.tendedCrops() + " crops");
            int grown = 0;
            for (int x = 0; x < 7; x++) {
                for (int z = 0; z < 7; z++) {
                    BlockState state = level.getBlockState(helper.absolutePos(new BlockPos(x, 2, z)));
                    if (state.is(Blocks.WHEAT)) grown += state.getValue(CropBlock.AGE);
                }
            }
            helper.assertTrue(grown >= 4, "the crops barely grew: " + grown + " stages");
            helper.assertTrue(scarecrow.getTarget() == null, "a farm scarecrow went for someone");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void partygoersAreFriendlyUntilTheyArent(GameTestHelper helper) {
        PartygoerEntity first = helper.spawn(ModEntities.PARTYGOER.get(), new BlockPos(1, 2, 1));
        PartygoerEntity second = helper.spawn(ModEntities.PARTYGOER.get(), new BlockPos(4, 2, 4));
        helper.assertFalse(first.isHostile() || second.isHostile(), "hostile straight away");
        ServerPlayer player = survivalPlayer(helper, "test-guest");
        helper.assertFalse(first.doHurtTarget(player), "a friendly Partygoer hit someone");
        first.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(first.isHostile() && second.isHostile(), "hitting one didn't set them all off");
            helper.getLevel().getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void presentsOpen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, "test-birthday");
        int surprises = 0, gifts = 0;
        for (int i = 0; i < 20; i++) {
            BlockPos pos = new BlockPos(i % 5 + 1, 2, i / 5 + 1);
            helper.setBlock(pos, ModBlocks.PRESENT.get().defaultBlockState().setValue(PartyBlocks.COLOR, i % 4));
            PartyBlocks.Present.open(level, helper.absolutePos(pos), player);
            helper.assertBlockPresent(Blocks.AIR, pos);
        }
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12);
        surprises = level.getEntitiesOfClass(PartygoerEntity.class, box).size();
        gifts = level.getEntitiesOfClass(ItemEntity.class, box).size();
        helper.assertTrue(gifts > 0, "no presents had anything in them");
        helper.assertTrue(surprises < 20, "every present was a Partygoer");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void scarecrowsFreezeInTorchlight(GameTestHelper helper) {
        ScarecrowEntity scarecrow = helper.spawn(ModEntities.SCARECROW.get(), new BlockPos(2, 2, 2));
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.GLOWSTONE);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(scarecrow.inTorchlight() && scarecrow.isWatched(), "a scarecrow moves about in bright light");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void watcherLeavesYourHomeAlone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper, "test-homebody");
        player.setRespawnPosition(level.dimension(), player.blockPosition(), 0.0f, true, false);
        helper.assertTrue(WatcherSpawner.safeAtHome(level, player), "the Watcher would come right up to your bed");
        player.setRespawnPosition(level.dimension(), player.blockPosition().offset(500, 0, 0), 0.0f, true, false);
        helper.assertFalse(WatcherSpawner.safeAtHome(level, player), "safe anywhere at all");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void strangeEventsHappen(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, "test-uneasy");
        for (StrangeEvents.Kind kind : StrangeEvents.Kind.values()) StrangeEvents.happen(player, kind);
        helper.assertTrue(StrangeEvents.chance(helper.getLevel()) >= 0, "no chance at all");
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void levelFunIsAParty(GameTestHelper helper) {
        var generator = LevelGenerators.get(BackroomsLevel.LEVEL_FUN);
        int balloons = 0, presents = 0, confetti = 0, cakes = 0, doors = 0;
        for (int x = 0; x < 200; x++) {
            for (int z = 0; z < 200; z++) {
                BlockState[] c = generator.column(4242L, x, z);
                helper.assertTrue(c[0].is(Blocks.BEDROCK) && c[c.length - 1].is(Blocks.BEDROCK), "not sealed in");
                for (BlockState state : c) {
                    if (state == null) continue;
                    if (state.is(ModBlocks.BALLOON.get())) balloons++;
                    if (state.is(ModBlocks.PRESENT.get())) presents++;
                    if (state.is(ModBlocks.CONFETTI.get())) confetti++;
                    if (state.is(Blocks.CAKE)) cakes++;
                    if (state.getBlock() instanceof com.rickandmorty.block.ExitDoorBlock
                            && state.getValue(net.minecraft.world.level.block.DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) doors++;
                }
            }
        }
        helper.assertTrue(balloons > 200 && presents > 30 && confetti > 2000 && cakes > 10,
                "a poor party: " + balloons + " balloons, " + presents + " presents, " + confetti + " confetti, " + cakes + " cakes");
        // not too much of a party: balloons in bunches, not a solid ceiling of them, and exit doors rare
        helper.assertTrue(balloons < 200 * 200 / 4, "the ceiling is solid balloons: " + balloons);
        int cells = (200 / 12) * (200 / 12);
        helper.assertTrue(doors > 0 && doors < cells / 3, doors + " exit doors in " + cells + " rooms");
        helper.assertTrue(ModItems.FIELD_GUIDE.get() != null, "no field guide");
        helper.succeed();
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper, String name) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }
}
