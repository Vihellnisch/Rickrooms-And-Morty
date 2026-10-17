package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.ExitDoorBlock;
import com.rickandmorty.block.entity.ExitDoorBlockEntity;
import com.rickandmorty.entity.ScarecrowEntity;
import com.rickandmorty.entity.WatcherEntity;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelGenerators;
import com.rickandmorty.worldgen.level.PoolroomsLevel;
import com.rickandmorty.worldgen.site.SiteKind;
import com.rickandmorty.worldgen.site.SitePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.Set;

/** Tests for v0.9: the Poolrooms, the Overworld Monolith, the Scarecrow Lantern and the Watcher everywhere. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V9GameTests {
    private static final String EMPTY = "empty";
    private static final long SEED = 55501L;

    /** The water can never go anywhere: every column is solid or water up to the surface, and nothing but air above water. */
    @GameTest(template = EMPTY)
    public static void poolroomsWaterIsSealedIn(GameTestHelper helper) {
        var generator = LevelGenerators.get(BackroomsLevel.POOLROOMS);
        RandomSource random = RandomSource.create(9);
        int wet = 0, dry = 0;
        for (int i = 0; i < 4000; i++) {
            int x = random.nextIntBetweenInclusive(-4000, 4000), z = random.nextIntBetweenInclusive(-4000, 4000);
            BlockState[] c = generator.column(SEED, x, z);
            for (int y = 1; y <= PoolroomsLevel.SURFACE; y++) {
                helper.assertTrue(c[y] != null, "open space at or below the water line at " + x + "," + y + "," + z);
            }
            for (int y = PoolroomsLevel.SURFACE + 1; y < c.length; y++) {
                helper.assertFalse(c[y] != null && c[y].is(Blocks.WATER), "water above the surface at " + x + "," + y + "," + z);
            }
            if (c[PoolroomsLevel.SURFACE].is(Blocks.WATER)) wet++;
            else dry++;
        }
        helper.assertTrue(wet > 800 && dry > 800, "wet " + wet + ", dry " + dry);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void poolroomsHaveEveryKindOfRoom(GameTestHelper helper) {
        Set<PoolroomsLevel.Room> seen = EnumSet.noneOf(PoolroomsLevel.Room.class);
        for (int cx = -15; cx < 15; cx++) {
            for (int cz = -15; cz < 15; cz++) seen.add(PoolroomsLevel.room(SEED, cx, cz));
        }
        helper.assertTrue(seen.size() == PoolroomsLevel.Room.values().length, "rooms seen: " + seen);
        var generator = LevelGenerators.get(BackroomsLevel.POOLROOMS);
        // depths vary: shallow, deep and bottomless all exist
        Set<Integer> depths = new java.util.TreeSet<>();
        for (int i = 0; i < 3000; i++) {
            BlockState[] c = generator.column(SEED, i * 37 % 6000 - 3000, i * 101 % 6000 - 3000);
            int y = PoolroomsLevel.SURFACE;
            while (y > 0 && c[y].is(Blocks.WATER)) y--;
            depths.add(PoolroomsLevel.SURFACE - y);
        }
        helper.assertTrue(depths.contains(1) && depths.stream().anyMatch(d -> d >= 4 && d <= 7) && depths.stream().anyMatch(d -> d >= 20),
                "water depths: " + depths);
        // the abyss has its drain, and the lounge its way out
        int[] abyss = PoolroomsLevel.find(SEED, PoolroomsLevel.Room.ABYSS, 0, 0);
        BlockState[] bottom = generator.column(SEED, abyss[0] + 20, abyss[1] + 20);
        helper.assertTrue(bottom[4].is(ModBlocks.POOL_DRAIN.get()), "no drain at the bottom of the abyss: " + bottom[4]);
        int[] lounge = PoolroomsLevel.find(SEED, PoolroomsLevel.Room.LOUNGE, 0, 0);
        BlockState[] exit = generator.column(SEED, lounge[0] + 10, lounge[1] + 1);
        helper.assertTrue(exit[PoolroomsLevel.SURFACE + 9].getBlock() instanceof ExitDoorBlock, "no exit door up the ladder");
        helper.assertTrue(generator.column(SEED, lounge[0] + 10, lounge[1] + 2)[PoolroomsLevel.SURFACE + 3].is(Blocks.LADDER), "no ladder");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void monolithIsRareAndLocatable(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(RickAndMortyMod.id("monolith")), "no monolith structure");
        var set = access.registryOrThrow(Registries.STRUCTURE_SET).get(RickAndMortyMod.id("monolith"));
        helper.assertTrue(set != null && set.placement() instanceof RandomSpreadStructurePlacement spread && spread.spacing() >= 150,
                "the monolith should be very rare");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void monolithBuilds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // the test world is kept between runs: build somewhere new each time, on the natural ground
        int x = 9000 + (int) (System.currentTimeMillis() / 1000 % 500) * 160, z = 9000;
        level.getChunk(x >> 4, z >> 4);
        BlockPos origin = new BlockPos(x, level.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.MOTION_BLOCKING, level,
                level.getChunkSource().randomState()), z);
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) level.getChunk((origin.getX() >> 4) + cx, (origin.getZ() >> 4) + cz);
        }
        SitePiece piece = new SitePiece(SiteKind.MONOLITH, origin, 777L);
        BoundingBox everywhere = new BoundingBox(x - 40, level.getMinBuildHeight(), z - 40, x + 40, level.getMaxBuildHeight(), z + 40);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1), everywhere,
                new ChunkPos(origin), origin);
        int monolith = 0, carpet = 0, top = 0;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-30, -3, -30), origin.offset(30, 116, 30))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.MONOLITH.get())) {
                monolith++;
                top = Math.max(top, p.getY() - origin.getY());
            }
            if (s.is(ModBlocks.BACKROOMS_CARPET.get())) carpet++;
        }
        helper.assertTrue(monolith > 4000, "only " + monolith + " monolith blocks");
        helper.assertTrue(top > 80, "only " + top + " high");
        helper.assertTrue(carpet > 30, "the Backrooms aren't leaking out: " + carpet + " carpet");
        BlockPos door = origin.offset(0, 1, 4);
        helper.assertTrue(level.getBlockEntity(door) instanceof ExitDoorBlockEntity exit && exit.destination() == BackroomsLevel.LEVEL_0
                && exit.arrival() == ExitDoorBlockEntity.Arrival.MONOLITH, "the door doesn't lead to Level 0's monolith hall");
        helper.assertTrue(level.getBlockState(origin.offset(1, 1, 1)).is(Blocks.CHEST), "no vault chest");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void lanternKeepsScarecrowsAway(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ScarecrowEntity scarecrow = helper.spawn(ModEntities.SCARECROW.get(), new BlockPos(1, 2, 1));
        ServerPlayer player = survivalPlayer(helper, "test-lantern");
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2, 5.5));
        player.teleportTo(level, at.x, at.y, at.z, 0.0f, 0.0f);   // facing away from it: it's free to move
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARECROW_LANTERN.get()));
        float health = player.getHealth();
        helper.runAfterDelay(80, () -> {
            helper.assertFalse(scarecrow.getTarget() == player, "a scarecrow went for someone holding a lantern");
            helper.assertTrue(player.getHealth() >= health, "the lantern holder got hurt");
            level.getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void scarecrowHeadIsWornAndCraftsALantern(GameTestHelper helper) {
        ItemStack head = new ItemStack(ModItems.SCARECROW_HEAD.get());
        Equipable equipable = Equipable.get(head);
        helper.assertTrue(equipable != null && equipable.getEquipmentSlot() == EquipmentSlot.HEAD, "you can't wear the head");
        var recipes = helper.getLevel().getRecipeManager();
        var recipe = recipes.byKey(RickAndMortyMod.id("scarecrow_lantern"));
        helper.assertTrue(recipe.isPresent() && recipe.get().value().getResultItem(helper.getLevel().registryAccess()).is(ModItems.SCARECROW_LANTERN.get()),
                "no lantern recipe");
        helper.succeed();
    }

    /** Ignored long enough, it's right behind you; turn round and it's gone. It never hurts you. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void watcherSlipsBehindYouAndVanishes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 20; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        WatcherEntity watcher = helper.spawn(ModEntities.WATCHER.get(), new BlockPos(3, 2, 18));
        ServerPlayer player = survivalPlayer(helper, "test-ignored");
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 2, 3.5));
        player.teleportTo(level, at.x, at.y, at.z, 180.0f, 0.0f);   // looking north, the other way from it
        float health = player.getHealth();
        watcher.loseInterest();
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(watcher.isBehindSomeone(), "it didn't slip round behind the player");
            helper.assertTrue(watcher.distanceTo(player) < 2.5, "it's " + watcher.distanceTo(player) + " away, not right behind");
            player.setYRot(0.0f);                                   // turn round
            player.setYHeadRot(0.0f);
            helper.runAfterDelay(15, () -> {
                helper.assertTrue(watcher.isRemoved(), "it's still there after turning round");
                helper.assertTrue(player.getHealth() >= health, "the Watcher hurt someone");
                level.getServer().getPlayerList().remove(player);
                helper.succeed();
            });
        });
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
