package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.StoreBlocks;
import com.rickandmorty.block.entity.ExitDoorBlockEntity;
import com.rickandmorty.block.entity.FoodCounterBlockEntity;
import com.rickandmorty.block.entity.StockCrateBlockEntity;
import com.rickandmorty.entity.StaffEntity;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.InfiniteIkeaLevel;
import com.rickandmorty.worldgen.level.InfiniteIkeaLevel.Department;
import com.rickandmorty.worldgen.level.LevelGenerators;
import com.rickandmorty.worldgen.level.StoreHours;
import com.rickandmorty.worldgen.site.SiteKind;
import com.rickandmorty.worldgen.site.SitePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;
import java.util.Map;

/** Tests for the Infinite IKEA. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V14GameTests {
    private static final String EMPTY = "empty";
    private static final long SEED = 3008L;

    private static InfiniteIkeaLevel ikea() {
        return (InfiniteIkeaLevel) LevelGenerators.get(BackroomsLevel.INFINITE_IKEA);
    }

    @GameTest(template = EMPTY)
    public static void everyDepartmentTurnsUp(GameTestHelper helper) {
        Map<Department, Integer> counts = new EnumMap<>(Department.class);
        for (int cx = -60; cx < 60; cx++) {
            for (int cz = -60; cz < 60; cz++) counts.merge(InfiniteIkeaLevel.department(SEED, cx, cz), 1, Integer::sum);
        }
        for (Department d : Department.values()) helper.assertTrue(counts.getOrDefault(d, 0) > 0, "no " + d + " in " + counts);
        int checkouts = counts.get(Department.CHECKOUT);
        helper.assertTrue(checkouts >= 20 && checkouts <= 120, "checkouts should be rare, not this: " + checkouts);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void exitSignsTellTheTruthNearACheckout(GameTestHelper helper) {
        // from every crossing within a few cells of a checkout, following the signs gets you there
        int tried = 0;
        for (int cx = -40; cx < 40; cx++) {
            for (int cz = -40; cz < 40; cz++) {
                if (InfiniteIkeaLevel.department(SEED, cx, cz) != Department.CHECKOUT) continue;
                for (int[] start : new int[][]{{cx + 5, cz + 2}, {cx - 4, cz - 3}, {cx + 1, cz - 6}}) {
                    int x = start[0], z = start[1];
                    int steps = 0;
                    while (InfiniteIkeaLevel.department(SEED, x, z) != Department.CHECKOUT && steps < 40) {
                        Direction way = InfiniteIkeaLevel.signPoints(SEED, x, z);
                        x += way.getStepX();
                        z += way.getStepZ();
                        steps++;
                    }
                    helper.assertTrue(steps < 40, "the signs from " + start[0] + "," + start[1] + " went round in circles");
                    tried++;
                }
            }
        }
        helper.assertTrue(tried > 0, "no checkout to try");
        BlockState sign = ModBlocks.EXIT_SIGN.get().defaultBlockState();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            helper.assertTrue(StoreBlocks.ExitSign.arrow(StoreBlocks.ExitSign.pointing(sign, d)) == d, "a sign doesn't point " + d);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theStoreIsWalkableAndTheCheckoutsHaveDoors(GameTestHelper helper) {
        InfiniteIkeaLevel ikea = ikea();
        RandomSource random = RandomSource.create(5);
        for (int i = 0; i < 20; i++) {
            Vec3 at = ikea.landing(SEED, random);
            helper.assertTrue((int) at.y == InfiniteIkeaLevel.FLOOR + 1, "a landing off the floor: " + at);
        }
        // the walkways: floor below, open above
        for (int x = 0; x < 64; x++) {
            BlockState[] column = ikea.column(SEED, x, 2);
            // (floor arrows are painted on: you walk over them)
            helper.assertTrue(column[InfiniteIkeaLevel.FLOOR] != null && com.rickandmorty.worldgen.level.LevelGenerator.clear(column[InfiniteIkeaLevel.FLOOR + 1])
                    && column[InfiniteIkeaLevel.FLOOR + 2] == null, "the walkway is blocked at x " + x);
        }
        // the roof over everything, that lets the daylight through
        BlockState[] any = ikea.column(SEED, 7, 9);
        helper.assertTrue(any[any.length - 1] != null && any[any.length - 1].getBlock() instanceof net.minecraft.world.level.block.TransparentBlock,
                "no roof");
        helper.assertTrue(any[any.length - 1].propagatesSkylightDown(helper.getLevel(), BlockPos.ZERO), "the roof keeps the daylight out");
        int[] checkout = null;
        for (int cx = 0; cx < 200 && checkout == null; cx++) {
            for (int cz = 0; cz < 40 && checkout == null; cz++) {
                if (InfiniteIkeaLevel.department(SEED, cx, cz) == Department.CHECKOUT) checkout = new int[]{cx, cz};
            }
        }
        helper.assertTrue(checkout != null, "no checkout found");
        int doors = 0;
        for (int ix = 0; ix < 27; ix++) {
            BlockState[] column = ikea.column(SEED, checkout[0] * InfiniteIkeaLevel.CELL + InfiniteIkeaLevel.WALK + ix,
                    checkout[1] * InfiniteIkeaLevel.CELL + InfiniteIkeaLevel.WALK + 20);
            BlockState door = column[InfiniteIkeaLevel.FLOOR + 1];
            if (door != null && door.is(ModBlocks.EXIT_DOOR.get()) && door.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) doors++;
        }
        helper.assertTrue(doors == 3, "the checkout has " + doors + " doors");
        helper.assertTrue(ikea.exitDestination(SEED, BlockPos.ZERO) == null, "the checkout doors should lead home");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void pillarsComeInAllSizes(GameTestHelper helper) {
        InfiniteIkeaLevel ikea = ikea();
        int none = 0, huge = 0, broken = 0, tall = 0;
        for (int cx = 0; cx < 30; cx++) {
            for (int cz = 0; cz < 30; cz++) {
                int[] p = InfiniteIkeaLevel.pillarIn(SEED, cx, cz);
                if (p == null) {
                    none++;
                    continue;
                }
                if (p[2] >= 4) huge++;
                if (p[3] == 1) broken++;
                if (p[3] == 0 && tall < 3 && InfiniteIkeaLevel.department(SEED, cx, cz) != Department.CHECKOUT
                        && !InfiniteIkeaLevel.department(SEED, cx, cz).hall()) {
                    BlockState[] column = ikea.column(SEED, cx * InfiniteIkeaLevel.CELL + InfiniteIkeaLevel.WALK + p[0],
                            cz * InfiniteIkeaLevel.CELL + InfiniteIkeaLevel.WALK + p[1]);
                    helper.assertTrue(column[column.length - 2] != null && column[column.length - 2].is(ModBlocks.STORE_PILLAR.get()),
                            "a pillar at " + cx + "," + cz + " doesn't reach the roof");
                    tall++;
                }
            }
        }
        helper.assertTrue(none > 200 && none < 500, none + " of 900 departments have no pillar");
        helper.assertTrue(huge > 10 && broken > 10, huge + " huge, " + broken + " broken");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theBigHallsHaveNoWalkwaysInside(GameTestHelper helper) {
        InfiniteIkeaLevel ikea = ikea();
        Map<Department, Integer> found = new EnumMap<>(Department.class);
        int hallSize = 2 * InfiniteIkeaLevel.CELL;
        for (int sx = 0; sx < 40; sx++) {
            for (int sz = 0; sz < 40; sz++) {
                Department hall = InfiniteIkeaLevel.hall(SEED, sx, sz);
                if (hall == null || found.containsKey(hall)) continue;
                found.put(hall, 1);
                // where the walkway between its departments would be
                BlockState[] middle = ikea.column(SEED, sx * hallSize + InfiniteIkeaLevel.CELL + 2, sz * hallSize + 20);
                helper.assertFalse(middle[InfiniteIkeaLevel.FLOOR].is(ModBlocks.STORE_WALKWAY.get()), hall + " has a walkway through it");
                java.util.Set<net.minecraft.world.level.block.Block> blocks = new java.util.HashSet<>();
                for (int x = 0; x < hallSize; x += 1) {
                    for (int z = 0; z < hallSize; z += 1) {
                        for (BlockState state : ikea.column(SEED, sx * hallSize + x, sz * hallSize + z)) if (state != null) blocks.add(state.getBlock());
                    }
                }
                var wanted = switch (hall) {
                    case BACKYARD -> java.util.List.of(net.minecraft.world.level.block.Blocks.FARMLAND, net.minecraft.world.level.block.Blocks.WHEAT,
                            ModBlocks.STOCK_CRATE.get());
                    case NATURE -> java.util.List.of(net.minecraft.world.level.block.Blocks.OAK_LOG, net.minecraft.world.level.block.Blocks.WATER,
                            net.minecraft.world.level.block.Blocks.COAL_ORE);
                    default -> java.util.List.of(net.minecraft.world.level.block.Blocks.PACKED_ICE, net.minecraft.world.level.block.Blocks.SPRUCE_LOG,
                            ModBlocks.STOCK_CRATE.get());
                };
                for (var block : wanted) helper.assertTrue(blocks.contains(block), hall + " has no " + block);
            }
        }
        helper.assertTrue(found.size() == 3, "only found halls " + found.keySet());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void stockCratesFillUpEveryMorning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.STOCK_CRATE.get());
        StockCrateBlockEntity crate = (StockCrateBlockEntity) helper.getBlockEntity(pos);
        crate.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/ikea_market")), 1L);
        crate.restock(level);
        crate.unpackLootTable(null);
        helper.assertFalse(crate.isEmpty(), "a new crate is empty");
        crate.clearContent();
        crate.restock(level);
        crate.unpackLootTable(null);
        helper.assertTrue(crate.isEmpty(), "it filled up again the same day");
        crate.setStockedOn(crate.stockedOn() - 1);                   // the next morning
        crate.restock(level);
        crate.unpackLootTable(null);
        helper.assertFalse(crate.isEmpty(), "it didn't fill up the next day");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theFoodCounterSellsOut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.FOOD_COUNTER.get());
        FoodCounterBlockEntity counter = (FoodCounterBlockEntity) helper.getBlockEntity(pos);
        ServerPlayer player = survivalPlayer(helper, "test-hungry");
        for (int i = 0; i < FoodCounterBlockEntity.SERVINGS; i++) helper.assertTrue(counter.serve(level, player), "no serving " + i);
        helper.assertFalse(counter.serve(level, player), "it didn't sell out");
        int plates = player.getInventory().countItem(ModItems.MEATBALL_PLATE.get()), juice = player.getInventory().countItem(ModItems.LINGONBERRY_JUICE.get());
        helper.assertTrue(plates + juice == FoodCounterBlockEntity.SERVINGS && plates > 0 && juice > 0, plates + " plates, " + juice + " juices");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void sofaPiecesJoinUp(GameTestHelper helper) {
        BlockState sofa = ModBlocks.SOFA.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        for (int x = 1; x <= 3; x++) {
            BlockPos pos = new BlockPos(x, 2, 2);
            helper.setBlock(pos, sofa.setValue(StoreBlocks.PART, StoreBlocks.Sofa.part(sofa, helper.getLevel(), helper.absolutePos(pos))));
        }
        // facing north, the sitter's left is to the west
        helper.assertBlockProperty(new BlockPos(1, 2, 2), StoreBlocks.PART, StoreBlocks.SofaPart.LEFT);
        helper.assertBlockProperty(new BlockPos(2, 2, 2), StoreBlocks.PART, StoreBlocks.SofaPart.MIDDLE);
        helper.assertBlockProperty(new BlockPos(3, 2, 2), StoreBlocks.PART, StoreBlocks.SofaPart.RIGHT);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void theStaffKeepStoreHours(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper, "test-shopper");
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        player.teleportTo(helper.getLevel(), at.x, at.y, at.z, 0.0f, 0.0f);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2000, 10));
        StaffEntity staff = helper.spawn(ModEntities.STAFF.get(), new BlockPos(5, 2, 5));
        StaffEntity colleague = helper.spawn(ModEntities.STAFF.get(), new BlockPos(1, 2, 6));
        StoreHours.force(false);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(staff.getTarget() == null, "it went for a customer in opening hours");
            StoreHours.force(true);
            helper.runAfterDelay(50, () -> {
                helper.assertTrue(staff.getTarget() == player, "it left a customer alone after closing");
                // a badge: one of us
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STAFF_BADGE.get()));
                helper.runAfterDelay(10, () -> {
                    helper.assertTrue(staff.getTarget() == null && colleague.getTarget() == null, "they saw through the badge");
                    // by day, hit one and its colleague comes too
                    StoreHours.force(false);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    staff.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0f);
                    helper.runAfterDelay(10, () -> {
                        helper.assertTrue(staff.getTarget() == player && colleague.getTarget() == player, "they didn't stick together");
                        StoreHours.force(null);
                        helper.getLevel().getServer().getPlayerList().remove(player);
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void theStoreInTheOverworldLeadsIn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int x = 9000, z = 3000 + (int) (System.currentTimeMillis() / 1000 % 20000) * 16;
        level.getChunk(x >> 4, z >> 4);
        BlockPos origin = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) level.getChunk((origin.getX() >> 4) + cx, (origin.getZ() >> 4) + cz);
        }
        SitePiece piece = new SitePiece(SiteKind.STORE, origin, 77L);
        BoundingBox everywhere = new BoundingBox(origin.getX() - 32, level.getMinBuildHeight(), origin.getZ() - 32,
                origin.getX() + 32, level.getMaxBuildHeight(), origin.getZ() + 32);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1), everywhere,
                new net.minecraft.world.level.ChunkPos(origin), origin);
        for (int dx : new int[]{-1, 0}) {
            BlockPos door = origin.offset(dx, 1, -10);
            helper.assertTrue(level.getBlockState(door).is(ModBlocks.STORE_DOOR.get()), "no door at " + dx);
            helper.assertTrue(level.getBlockEntity(door) instanceof ExitDoorBlockEntity exit && exit.destination() == BackroomsLevel.INFINITE_IKEA,
                    "the door doesn't lead into the store");
        }
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
