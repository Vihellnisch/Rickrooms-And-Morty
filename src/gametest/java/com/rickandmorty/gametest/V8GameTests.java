package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.ExitDoorBlock;
import com.rickandmorty.entity.ScarecrowEntity;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.worldgen.BackroomsLayout;
import com.rickandmorty.worldgen.BackroomsZones;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelChunkGenerator;
import com.rickandmorty.worldgen.level.LevelGenerator;
import com.rickandmorty.worldgen.level.LevelGenerators;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tests for v0.8: the new Backrooms levels and the reworked Level 0. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V8GameTests {
    private static final String EMPTY = "empty";
    private static final long SEED = 987654321L;
    private static final List<BackroomsLevel> NEW_LEVELS = List.of(BackroomsLevel.values()).subList(1, BackroomsLevel.values().length);

    /** (The test server doesn't create datapack dimensions, so this reads their JSON like the Level 0 test does.) */
    @GameTest(template = EMPTY)
    public static void everyLevelLoads(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess());
        for (BackroomsLevel level : BackroomsLevel.values()) {
            net.minecraft.world.level.dimension.LevelStem stem;
            try (var reader = server.getResourceManager().getResourceOrThrow(RickAndMortyMod.id("dimension/" + level.id() + ".json")).openAsReader()) {
                stem = net.minecraft.world.level.dimension.LevelStem.CODEC.parse(ops, com.google.gson.JsonParser.parseReader(reader)).getOrThrow();
            } catch (Exception e) {
                throw new net.minecraft.gametest.framework.GameTestAssertException(level + " failed to load: " + e);
            }
            var type = stem.type().value();
            // all of them shut in, except the Infinite IKEA, under its grey sky
            boolean sky = level == BackroomsLevel.INFINITE_IKEA || level == BackroomsLevel.ENDLESS_FIELD || level == BackroomsLevel.ABANDONED_CITY
                    || level == BackroomsLevel.DROP_LIMIT || level == BackroomsLevel.FACADE || level == BackroomsLevel.STAIRCASE || level == BackroomsLevel.FROOPYLAND;
            helper.assertTrue(type.hasCeiling() != sky && type.hasSkyLight() == sky && type.height() % 16 == 0, "bad dimension type for " + level);
            helper.assertTrue(server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                    .containsKey(RickAndMortyMod.id(level.id())), "no biome for " + level);
            if (level == BackroomsLevel.LEVEL_0) {
                helper.assertTrue(type.minY() == -64 && type.height() == 128, "Level 0 should span -64..64");
                continue;
            }
            helper.assertTrue(stem.generator() instanceof LevelChunkGenerator g && g.level() == level, "wrong generator in " + level);
            LevelGenerator generator = LevelGenerators.get(level);
            helper.assertTrue(type.minY() == 0 && type.height() >= generator.height(),
                    level + " is " + type.height() + " tall, its columns " + generator.height());
        }
        for (String chest : List.of("level_1_supplies", "level_4_office", "harvest_farmhouse", "wooden_hotel_room", "cavern_castle")) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, RickAndMortyMod.id("chests/" + chest));
            helper.assertTrue(server.reloadableRegistries().getLootTable(key) != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "no loot table " + chest);
        }
        helper.succeed();
    }

    /** Our dimensions don't make a world "experimental" (no warning screen); anyone else's still do. */
    @GameTest(template = EMPTY)
    public static void ourDimensionsAreNotExperimental(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess());
        var stemKey = net.minecraft.core.registries.Registries.LEVEL_STEM;
        java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.dimension.LevelStem>, net.minecraft.world.level.dimension.LevelStem> vanilla = new java.util.LinkedHashMap<>();
        var serverStems = server.registryAccess().registryOrThrow(stemKey);
        for (var stem : serverStems) vanilla.put(serverStems.getResourceKey(stem).orElseThrow(), stem);
        var base = new net.minecraft.world.level.levelgen.WorldDimensions(vanilla);
        helper.assertTrue(base.bake(new net.minecraft.core.MappedRegistry<>(stemKey, com.mojang.serialization.Lifecycle.stable()).freeze()).lifecycle()
                == com.mojang.serialization.Lifecycle.stable(), "the plain test world is already experimental");

        java.util.function.Function<Boolean, net.minecraft.world.level.levelgen.WorldDimensions.Complete> bake = foreign -> {
            var datapack = new net.minecraft.core.MappedRegistry<>(stemKey, com.mojang.serialization.Lifecycle.stable());
            net.minecraft.world.level.dimension.LevelStem any = null;
            for (BackroomsLevel level : BackroomsLevel.values()) {
                try (var reader = server.getResourceManager().getResourceOrThrow(RickAndMortyMod.id("dimension/" + level.id() + ".json")).openAsReader()) {
                    any = net.minecraft.world.level.dimension.LevelStem.CODEC.parse(ops, com.google.gson.JsonParser.parseReader(reader)).getOrThrow();
                } catch (Exception e) {
                    throw new net.minecraft.gametest.framework.GameTestAssertException(e.toString());
                }
                datapack.register(net.minecraft.resources.ResourceKey.create(stemKey, RickAndMortyMod.id(level.id())), any, net.minecraft.core.RegistrationInfo.BUILT_IN);
            }
            if (foreign) {
                // a separate stem object, as another mod's dimension would be
                var theirs = new net.minecraft.world.level.dimension.LevelStem(any.type(), any.generator());
                datapack.register(net.minecraft.resources.ResourceKey.create(stemKey, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("othermod", "elsewhere")),
                        theirs, net.minecraft.core.RegistrationInfo.BUILT_IN);
            }
            return base.bake(datapack.freeze());
        };
        var ours = bake.apply(false);
        helper.assertTrue(ours.dimensions().containsKey(RickAndMortyMod.id("backrooms")) && ours.dimensions().size() == vanilla.size() + BackroomsLevel.values().length,
                "dimensions went missing: " + ours.dimensions().keySet());
        helper.assertTrue(ours.lifecycle() == com.mojang.serialization.Lifecycle.stable(), "our dimensions still make the world " + ours.lifecycle());
        helper.assertTrue(bake.apply(true).lifecycle() != com.mojang.serialization.Lifecycle.stable(), "another mod's dimension should still be experimental");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void levelColumnsAreSound(GameTestHelper helper) {
        RandomSource random = RandomSource.create(5);
        for (BackroomsLevel level : NEW_LEVELS) {
            LevelGenerator generator = LevelGenerators.get(level);
            int standable = 0;
            for (int i = 0; i < 400; i++) {
                int x = random.nextIntBetweenInclusive(-5000, 5000), z = random.nextIntBetweenInclusive(-5000, 5000);
                BlockState[] c = generator.column(SEED, x, z);
                helper.assertTrue(c.length == generator.height(), level + " column length " + c.length);
                // and it's the same every time
                BlockState[] again = generator.column(SEED, x, z);
                for (int y = 0; y < c.length; y++) helper.assertTrue(c[y] == again[y], level + " isn't deterministic at " + x + "," + y + "," + z);
                if (generator.standY(SEED, x, z, 2) != null) standable++;
            }
            helper.assertTrue(standable > 40, level + ": only " + standable + "/400 columns anywhere to stand");
            for (int i = 0; i < 5; i++) {
                Vec3 landing = generator.landing(SEED, random);
                int x = (int) Math.floor(landing.x), y = (int) Math.floor(landing.y), z = (int) Math.floor(landing.z);
                BlockState[] c = generator.column(SEED, x, z);
                helper.assertTrue(LevelGenerator.solid(c[y - 1]) && LevelGenerator.clear(c[y]) && LevelGenerator.clear(c[y + 1]),
                        level + " landed somewhere bad: " + landing);
            }
        }
        helper.succeed();
    }

    /** "Unlike before, some parts can have a lower ceiling and some a higher one." */
    @GameTest(template = EMPTY)
    public static void ceilingsVary(GameTestHelper helper) {
        // Level 0: every kind of zone exists, and the Kane halls change height as you walk
        Set<BackroomsZones.Zone> zones = EnumSet.noneOf(BackroomsZones.Zone.class);
        int[] kane = null, monolith = null, pits = null;
        for (int zx = -40; zx < 40; zx++) {
            for (int zz = -40; zz < 40; zz++) {
                int x = zx * BackroomsZones.ZONE + 36, z = zz * BackroomsZones.ZONE + 36;
                BackroomsZones.Zone zone = BackroomsZones.zoneAt(SEED, x, z);
                zones.add(zone);
                if (zone == BackroomsZones.Zone.KANE && kane == null) kane = new int[]{zx, zz};
                if (zone == BackroomsZones.Zone.MONOLITH && monolith == null) monolith = new int[]{zx, zz};
                if (zone == BackroomsZones.Zone.PITS && pits == null) pits = new int[]{zx, zz};
            }
        }
        helper.assertTrue(zones.size() == BackroomsZones.Zone.values().length, "zones seen: " + zones);
        Set<Integer> heights = new HashSet<>();
        for (int lx = 1; lx < BackroomsZones.ZONE - 1; lx++) {
            BlockState[] c = BackroomsLayout.column(SEED, kane[0] * BackroomsZones.ZONE + lx, kane[1] * BackroomsZones.ZONE + 20);
            for (int y = 2; y < c.length; y++) {
                if (c[y] != null) {
                    heights.add(y);
                    break;
                }
            }
        }
        helper.assertTrue(heights.size() >= 3, "Kane hall ceiling heights: " + heights);
        // most of the Kane lights work, and some benches face each way
        int lights = 0, lit = 0, benches = 0;
        for (int lx = 1; lx < BackroomsZones.ZONE - 1; lx++) {
            for (int lz = 1; lz < BackroomsZones.ZONE - 1; lz++) {
                BlockState[] c = BackroomsLayout.column(SEED, kane[0] * BackroomsZones.ZONE + lx, kane[1] * BackroomsZones.ZONE + lz);
                for (BlockState state : c) {
                    if (state != null && state.getBlock() instanceof com.rickandmorty.block.FluorescentLightBlock) {
                        lights++;
                        if (state.getValue(com.rickandmorty.block.FluorescentLightBlock.LIT)) lit++;
                    }
                }
                if (c[2] != null && c[2].is(ModBlocks.CARPET_LEDGE.get())) benches++;
            }
        }
        helper.assertTrue(lights > 100 && lit > lights * 0.7, "Kane lights: " + lit + " of " + lights + " work");
        helper.assertTrue(benches > 10 && benches < 72 * 72 / 8, "Kane benches: " + benches);
        BlockState[] hall = BackroomsLayout.column(SEED, monolith[0] * BackroomsZones.ZONE + 10, monolith[1] * BackroomsZones.ZONE + 10);
        int ceiling = 2;
        while (ceiling < hall.length && hall[ceiling] == null) ceiling++;
        helper.assertTrue(ceiling >= 40, "monolith hall is only " + ceiling + " high");
        BlockState[] door = BackroomsLayout.column(SEED, monolith[0] * BackroomsZones.ZONE + 28, monolith[1] * BackroomsZones.ZONE + 39);
        helper.assertTrue(door[2].getBlock() instanceof ExitDoorBlock, "no door in the monolith");
        BlockState[] pit = BackroomsLayout.column(SEED, pits[0] * BackroomsZones.ZONE + 8, pits[1] * BackroomsZones.ZONE + 8);
        helper.assertTrue(pit[0] == null && pit[1] == null, "the pit has a floor");
        helper.assertTrue(BackroomsZones.below(SEED, pits[0] * BackroomsZones.ZONE + 6, pits[1] * BackroomsZones.ZONE + 6) != null, "pit walls don't go down");

        // the other levels: the tall ones really are tall
        helper.assertTrue(LevelGenerators.get(BackroomsLevel.LEVEL_4).height() < 10, "offices should be low");
        helper.assertTrue(LevelGenerators.get(BackroomsLevel.LEVEL_188).height() > 50, "courtyards should be high");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theHoleExists(GameTestHelper helper) {
        int[] hole = null;
        for (int rx = -30; rx < 30 && hole == null; rx++) {
            for (int rz = -30; rz < 30 && hole == null; rz++) hole = BackroomsZones.hole(SEED, rx, rz);
        }
        helper.assertTrue(hole != null, "no Hole anywhere");
        BlockState[] c = BackroomsLayout.column(SEED, hole[0], hole[1]);
        helper.assertTrue(c[3].is(ModBlocks.NOCLIP_VOID.get()), "the Hole isn't black inside: " + c[3]);
        helper.succeed();
    }

    /** Every level has red doors somewhere, and the Sunlit Lobby's lead home. */
    @GameTest(template = EMPTY)
    public static void exitDoorsEverywhere(GameTestHelper helper) {
        for (BackroomsLevel level : NEW_LEVELS) {
            if (level == BackroomsLevel.SCAFFOLD) continue;             // no doors in the void; you fall out of it
            if (level == BackroomsLevel.ENDLESS_FIELD) continue;        // its few doors are far apart (V16GameTests checks them)
            LevelGenerator generator = LevelGenerators.get(level);
            BlockPos found = null;
            search:
            for (int x = -300; x < 300; x++) {
                for (int z = -300; z < 300; z += 1) {
                    BlockState[] c = generator.column(SEED, x, z);
                    for (int y = 0; y < c.length; y++) {
                        if (c[y] != null && c[y].getBlock() instanceof ExitDoorBlock && c[y].getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                            helper.assertTrue(c[y + 1] != null && c[y + 1].getBlock() instanceof ExitDoorBlock, "half a door in " + level);
                            found = new BlockPos(x, y, z);
                            break search;
                        }
                    }
                }
                if (level != BackroomsLevel.CAVERN && x > 0) x += 3;           // most levels are dense with doors
            }
            helper.assertTrue(found != null, "no exit door in " + level);
            BackroomsLevel to = generator.exitDestination(SEED, found);
            if (level == BackroomsLevel.SUNLIT_LOBBY) helper.assertTrue(to == null || to == BackroomsLevel.FROOPYLAND, "the Lobby's doors should lead home (or to Froopyland)");
            else helper.assertTrue(to != level, level + "'s door leads back to itself");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void scarecrowsOnlyMoveUnwatched(GameTestHelper helper) {
        ScarecrowEntity scarecrow = helper.spawn(ModEntities.SCARECROW.get(), new BlockPos(1, 2, 6));
        ServerPlayer player = survivalPlayer(helper, "test-watcher");
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        player.teleportTo(helper.getLevel(), at.x, at.y, at.z, 0.0f, 0.0f);      // yaw 0 faces south, at it
        player.setYHeadRot(0.0f);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(scarecrow.seenByAnyone() && scarecrow.isWatched(), "a scarecrow in plain view isn't watched");
            player.setYRot(180.0f);
            player.setYHeadRot(180.0f);
            helper.runAfterDelay(5, () -> {
                helper.assertFalse(scarecrow.isWatched(), "nobody's looking, but the scarecrow thinks it's watched");
                helper.getLevel().getServer().getPlayerList().remove(player);
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
