package com.rickandmorty.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.entity.BacteriaEntity;
import com.rickandmorty.entity.MeeseeksEntity;
import com.rickandmorty.entity.MimicEntity;
import com.rickandmorty.entity.MimicKind;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.worldgen.BackroomsChunkGenerator;
import com.rickandmorty.worldgen.BackroomsTheme;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import com.rickandmorty.block.FluorescentLightBlock;
import com.rickandmorty.block.GatewayStructure;
import com.rickandmorty.block.WarpGatewayBlock;
import com.rickandmorty.block.entity.WarpGatewayBlockEntity;
import com.rickandmorty.entity.PortalEntity;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.BackroomsLayout;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** In-game tests. Run with: ./gradlew runGameTestServer */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class RickAndMortyGameTests {
    private static final String EMPTY = "empty";

    // ------------------------------------------------------------------ Backrooms

    /**
     * The game test server only creates the overworld, so instead of visiting the Backrooms we
     * decode its dimension JSON exactly like a real world does, then run its generator on chunks.
     */
    @GameTest(template = EMPTY)
    public static void backroomsDimensionGeneratesMaze(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RegistryAccess registries = server.registryAccess();
        LevelStem stem;
        try (var reader = server.getResourceManager().getResourceOrThrow(RickAndMortyMod.id("dimension/backrooms.json")).openAsReader()) {
            stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), JsonParser.parseReader(reader)).getOrThrow();
        } catch (Exception e) {
            throw new GameTestAssertException("Backrooms dimension JSON failed to load: " + e);
        }
        helper.assertTrue(stem.generator() instanceof BackroomsChunkGenerator, "wrong generator " + stem.generator());
        DimensionType type = stem.type().value();
        helper.assertTrue(type.minY() == -64 && type.height() == 128 && !type.hasSkyLight() && type.hasCeiling(), "bad dimension type");

        BackroomsChunkGenerator generator = (BackroomsChunkGenerator) stem.generator();
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        LevelHeightAccessor height = LevelHeightAccessor.create(type.minY(), type.height());
        long seed = 1234567L;

        // Find biome patches: several themes should show up, covering a fair share of the grid.
        Set<BackroomsTheme> themes = EnumSet.noneOf(BackroomsTheme.class);
        BackroomsLayout.Patch firstPatch = null;
        int patches = 0;
        for (int gx = -20; gx < 20; gx++) {
            for (int gz = -20; gz < 20; gz++) {
                BackroomsLayout.Patch patch = BackroomsLayout.patch(seed, gx, gz);
                if (patch == null) continue;
                patches++;
                themes.add(patch.theme());
                if (firstPatch == null && com.rickandmorty.worldgen.BackroomsZones.zoneAt(seed, patch.x(), patch.z())
                        == com.rickandmorty.worldgen.BackroomsZones.Zone.CLASSIC) firstPatch = patch;
            }
        }
        helper.assertTrue(themes.size() == BackroomsTheme.values().length, "only saw themes " + themes);
        helper.assertTrue(patches > 1600 * 0.3 && patches < 1600 * 0.6, "patch count " + patches);

        // Generate a plain area and the area around a patch, and check the rules hold everywhere.
        int walls = 0, workingLights = 0, columns = 0, themedWalls = 0;
        ChunkPos patchChunk = new ChunkPos(BlockPos.containing(firstPatch.x(), 0, firstPatch.z()));
        List<ChunkPos> chunks = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                chunks.add(new ChunkPos(dx, dz));
                chunks.add(new ChunkPos(patchChunk.x + dx, patchChunk.z + dz));
            }
        }
        for (ChunkPos cp : chunks) {
            ProtoChunk chunk = new ProtoChunk(cp, UpgradeData.EMPTY, height, biomes, null);
            generator.generate(seed, chunk);
            for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++) {
                for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
                    columns++;
                    BlockState[] expected = BackroomsLayout.column(seed, x, z);
                    for (int y = 0; y < expected.length; y++) {
                        BlockState actual = chunk.getBlockState(new BlockPos(x, y, z));
                        check(expected[y] == null ? actual.isAir() : actual == expected[y], "block at y=" + y, x, z);
                    }
                    if (com.rickandmorty.worldgen.BackroomsZones.zoneAt(seed, x, z) != com.rickandmorty.worldgen.BackroomsZones.Zone.CLASSIC) continue;
                    check(expected[0].is(Blocks.BEDROCK) && expected[BackroomsLayout.ROOF_Y].is(Blocks.BEDROCK), "bedrock", x, z);
                    int cx = Math.floorDiv(x, BackroomsLayout.CELL), cz = Math.floorDiv(z, BackroomsLayout.CELL);
                    if (!BackroomsLayout.isOpenCell(seed, cx, cz)) continue;  // connectors are checked separately below

                    int lx = Math.floorMod(x, BackroomsLayout.CELL), lz = Math.floorMod(z, BackroomsLayout.CELL);
                    for (int level = 0; level < BackroomsLayout.LEVELS; level++) {
                        int floorY = BackroomsLayout.floorY(level);
                        check(expected[floorY] != null && expected[floorY + 4] != null, "floor/ceiling on level " + level, x, z);
                        boolean wall = BackroomsLayout.isWall(seed, level, x, z);
                        for (int y = floorY + 1; y <= floorY + 3; y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (wall) {
                                check(!chunk.getBlockState(pos).getCollisionShape(chunk, pos).isEmpty(), "wall should be solid", x, z);
                                if (!chunk.getBlockState(pos).is(ModBlocks.BACKROOMS_WALLPAPER.get())) themedWalls++;
                            } else if (lx >= 2 && lx <= 4 && lz >= 2 && lz <= 4) {
                                check(chunk.getBlockState(pos).isAir(), "middle of a cell should be empty (level " + level + ")", x, z);
                            }
                        }
                        BlockState ceiling = expected[floorY + 4];
                        if (ceiling.is(ModBlocks.FLUORESCENT_LIGHT.get())) {
                            boolean broken = ceiling.getValue(FluorescentLightBlock.BROKEN);
                            check(broken != ceiling.getValue(FluorescentLightBlock.LIT), "light state", x, z);
                            if (!broken) workingLights++;
                        }
                        if (wall) walls++;
                    }
                }
            }
        }
        helper.assertTrue(walls > columns / 10 && walls < columns, "unexpected wall count " + walls + "/" + columns);
        helper.assertTrue(workingLights > columns / 20, "too few working lights: " + workingLights);
        helper.assertTrue(themedWalls > 0, "patch at " + firstPatch + " changed no walls");
        helper.assertFalse(BackroomsLayout.column(seed, firstPatch.x(), firstPatch.z())[BackroomsLayout.FLOOR_Y]
                .is(ModBlocks.BACKROOMS_CARPET.get()), "patch centre still has carpet: " + firstPatch);

        // The two floors have different mazes.
        int differences = 0;
        for (int x = 0; x < 300; x++) {
            if (BackroomsLayout.isWall(seed, 0, x, 0) != BackroomsLayout.isWall(seed, 1, x, 0)) differences++;
        }
        helper.assertTrue(differences > 10, "upper floor copies the lower one");

        // Connectors: find one of each kind and check it really links the floors.
        Set<BackroomsLayout.ConnectorKind> seen = EnumSet.noneOf(BackroomsLayout.ConnectorKind.class);
        for (int cx = -60; cx < 60 && seen.size() < 4; cx++) {
            for (int cz = -60; cz < 60; cz++) {
                BackroomsLayout.Connector c = BackroomsLayout.connector(seed, cx, cz);
                if (c.kind() == BackroomsLayout.ConnectorKind.NONE || !seen.add(c.kind())) continue;
                checkConnector(seed, cx, cz, c);
            }
        }
        helper.assertTrue(seen.size() == 4, "connector kinds found: " + seen);

        // Open cells have an empty middle on both floors (that's where you land and mobs spawn).
        for (int i = 0; i < 2000; i++) {
            int cx = i * 7919 % 5000 - 2500, cz = i * 104729 % 5000 - 2500;
            if (!BackroomsLayout.isOpenCell(seed, cx, cz)) continue;
            int x = (int) Math.floor(BackroomsLayout.cellCenter(cx)), z = (int) Math.floor(BackroomsLayout.cellCenter(cz));
            if (com.rickandmorty.worldgen.BackroomsZones.zoneAt(seed, x, z) != com.rickandmorty.worldgen.BackroomsZones.Zone.CLASSIC) continue;
            BlockState[] column = BackroomsLayout.column(seed, x, z);
            for (int level = 0; level < BackroomsLayout.LEVELS; level++) {
                int stand = BackroomsLayout.standY(level);
                check(column[stand] == null && column[stand + 1] == null && column[stand + 2] == null, "blocked landing on level " + level, x, z);
            }
        }

        // Mega patches: rare, big, with a solid core.
        int megas = 0;
        for (int gx = -20; gx < 20; gx++) {
            for (int gz = -20; gz < 20; gz++) {
                BackroomsLayout.Patch mega = BackroomsLayout.megaPatch(seed, gx, gz);
                if (mega == null) continue;
                megas++;
                helper.assertTrue(mega.radius() >= 50, "small mega patch " + mega);
                var core = BackroomsLayout.themeAt(seed, mega.x() + mega.radius() / 2, mega.z());
                helper.assertTrue(core != null && core.strength() >= 1.0, "mega patch core not solid: " + mega);
            }
        }
        helper.assertTrue(megas > 1600 * 0.2 && megas < 1600 * 0.4, "mega patch count " + megas);

        // Chests: rare, but there.
        int chests = 0;
        for (int cx = -100; cx < 100; cx++) {
            for (int cz = -100; cz < 100; cz++) {
                if (BackroomsLayout.chest(seed, cx, cz) != BackroomsLayout.ChestKind.NONE) chests++;
            }
        }
        helper.assertTrue(chests > 40000 / 150 && chests < 40000 / 30, "chest count " + chests + " in 40000 cells");
        helper.succeed();
    }

    /** Checks the blocks of a connector cell through the pure layout function. */
    private static void checkConnector(long seed, int cx, int cz, BackroomsLayout.Connector c) {
        int ox = cx * BackroomsLayout.CELL, oz = cz * BackroomsLayout.CELL;
        int upperFloor = BackroomsLayout.floorY(1);
        switch (c.kind()) {
            case STAIRS, SLIDE -> {
                Direction up = c.direction();
                for (int step = 1; step <= 5; step++) {
                    // walk the run from its low end towards "up"
                    int lx = up == Direction.EAST ? step : up == Direction.WEST ? BackroomsLayout.CELL - step : 3;
                    int lz = up == Direction.SOUTH ? step : up == Direction.NORTH ? BackroomsLayout.CELL - step : 3;
                    BlockState[] col = BackroomsLayout.column(seed, ox + lx, oz + lz);
                    BlockState stair = col[BackroomsLayout.FLOOR_Y + step];
                    check(stair != null && stair.getBlock() instanceof StairBlock && stair.getValue(StairBlock.FACING) == up,
                            "stair step " + step, ox + lx, oz + lz);
                    check(col[BackroomsLayout.FLOOR_Y + step + 1] == null && col[BackroomsLayout.FLOOR_Y + step + 2] == null,
                            "no head room above step " + step, ox + lx, oz + lz);
                }
            }
            case LADDER -> {
                BlockState[] col = BackroomsLayout.column(seed, ox + 3, oz + 2);
                BlockState[] pillar = BackroomsLayout.column(seed, ox + 2, oz + 2);
                for (int y = BackroomsLayout.ROOM_MIN_Y; y <= upperFloor + 2; y++) {
                    check(col[y] != null && col[y].is(Blocks.LADDER), "ladder at y=" + y, ox + 3, oz + 2);
                    check(pillar[y] != null, "ladder pillar at y=" + y, ox + 2, oz + 2);
                }
            }
            case PIT -> {
                BlockState[] col = BackroomsLayout.column(seed, ox + 3, oz + 3);
                check(col[BackroomsLayout.CEILING_Y] == null && col[upperFloor] == null, "pit not open", ox + 3, oz + 3);
            }
            case NONE -> {
            }
        }
    }

    private static void check(boolean ok, String what, int x, int z) {
        if (!ok) throw new GameTestAssertException("Bad " + what + " at " + x + ", " + z);
    }

    /** Coordinates item programs the gun, and firing never wastes fluid if the target can't be reached. */
    @GameTest(template = EMPTY)
    public static void coordinatesProgramThePortalGun(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        ItemStack coordinates = new ItemStack(ModItems.BACKROOMS_COORDINATES.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, coordinates);
        player.setItemInHand(InteractionHand.OFF_HAND, gun);

        coordinates.use(level, player, InteractionHand.MAIN_HAND);
        PortalDestinations dial = PortalGunItem.getDestinations(gun);
        helper.assertTrue(dial.knows(ModDimensions.BACKROOMS), "gun did not learn the Backrooms");
        helper.assertTrue(dial.selectedDimension().filter(ModDimensions.BACKROOMS::equals).isPresent(), "Backrooms not selected");
        helper.assertTrue(coordinates.isEmpty(), "coordinates were not consumed");

        // This test server has no Backrooms level, so the shot must be refused without using a charge.
        gun.use(level, player, InteractionHand.OFF_HAND);
        helper.assertTrue(PortalGunItem.getCharges(gun) == PortalGunItem.MAX_CHARGES, "charge wasted on an unreachable dimension");
        helper.succeed();
    }

    /** Full portal gun trip within the overworld: fire into the air, a pig walks in and arrives at the saved spot. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void portalGunTeleportsToSavedDestination(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 start = helper.absoluteVec(new Vec3(8, 1, 8));
        player.moveTo(start.x, start.y, start.z, 0.0f, -90.0f);  // looking straight up at nothing
        Vec3 target = helper.absoluteVec(new Vec3(2.5, 1, 2.5));
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.PORTAL_DESTINATIONS.get(), PortalDestinations.EMPTY.withAdded(
                new PortalDestinations.Destination("Test", level.dimension(), target.x, target.y, target.z, 0.0f)));
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);

        gun.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(PortalGunItem.getCharges(gun) == PortalGunItem.MAX_CHARGES - 1, "charge not used");

        Pig[] pig = new Pig[1];
        helper.runAfterDelay(8, () -> {
            List<PortalEntity> portals = level.getEntitiesOfClass(PortalEntity.class, new AABB(start, start).inflate(8));
            helper.assertTrue(portals.size() == 1, "expected one portal, found " + portals.size());
            Vec3 c = portals.get(0).position();
            pig[0] = EntityType.PIG.create(level);
            pig[0].setNoGravity(true);
            pig[0].moveTo(c.x, c.y - 0.4, c.z, 0, 0);
            level.addFreshEntity(pig[0]);
        });
        helper.succeedWhen(() -> helper.assertTrue(pig[0] != null && pig[0].position().distanceTo(target) < 0.5,
                "pig not at destination"));
    }

    // ------------------------------------------------------------------ Warp Gateway

    private static void placeGateway(ServerLevel level, BlockPos pos, Direction facing) {
        BlockState state = ModBlocks.WARP_GATEWAY.get().defaultBlockState().setValue(WarpGatewayBlock.FACING, facing);
        level.setBlock(pos, state, 3);
        state.getBlock().setPlacedBy(level, pos, state, null, new ItemStack(ModItems.WARP_GATEWAY.get()));
    }

    private static void useGunOn(Player player, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static boolean active(ServerLevel level, BlockPos controller) {
        BlockState state = level.getBlockState(controller);
        return state.is(ModBlocks.WARP_GATEWAY.get()) && state.getValue(WarpGatewayBlock.ACTIVE);
    }

    @GameTest(template = EMPTY)
    public static void gatewayLinksWithPortalGunAndTeleports(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos b = helper.absolutePos(new BlockPos(12, 1, 11));
        placeGateway(level, a, Direction.NORTH);
        placeGateway(level, b, Direction.EAST);
        for (int i = 0; i < GatewayStructure.PARTS.length; i++) {
            helper.assertTrue(level.getBlockState(GatewayStructure.partPos(a, Direction.NORTH, i)).is(ModBlocks.GATEWAY_FRAME.get()), "missing frame " + i);
            helper.assertTrue(level.getBlockState(GatewayStructure.partPos(b, Direction.EAST, i)).is(ModBlocks.GATEWAY_FRAME.get()), "missing frame " + i);
        }
        helper.assertFalse(active(level, a) || active(level, b), "gateways active before linking");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PORTAL_GUN.get()));
        useGunOn(player, GatewayStructure.partPos(a, Direction.NORTH, GatewayStructure.TOP_PART)); // clicking any piece works
        helper.assertFalse(active(level, a), "linked after one click");
        useGunOn(player, b);
        helper.assertTrue(active(level, a) && active(level, b), "gateways did not link");

        Vec3 inside = GatewayStructure.toWorld(a, Direction.NORTH, GatewayStructure.RING_X, 0.3, 0.5);
        Pig pig = EntityType.PIG.create(level);
        pig.moveTo(inside.x, inside.y, inside.z, 0, 0);
        level.addFreshEntity(pig);
        Vec3 arrival = GatewayStructure.arrivalPoint(b, Direction.EAST);
        helper.succeedWhen(() -> helper.assertTrue(pig.position().distanceTo(arrival) < 1.0,
                "pig at " + pig.position() + " not at " + arrival));
    }

    @GameTest(template = EMPTY)
    public static void breakingGatewayDropsItOnceAndClosesPartner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos b = helper.absolutePos(new BlockPos(12, 1, 11));
        placeGateway(level, a, Direction.SOUTH);
        placeGateway(level, b, Direction.WEST);
        WarpGatewayBlockEntity.link(level.getServer(), GlobalPos.of(level.dimension(), a), GlobalPos.of(level.dimension(), b));
        helper.assertTrue(active(level, a) && active(level, b), "not linked");

        level.destroyBlock(GatewayStructure.partPos(a, Direction.SOUTH, 3), true); // break a side piece
        helper.assertTrue(level.getBlockState(a).isAir(), "controller survived");
        for (int i = 0; i < GatewayStructure.PARTS.length; i++) {
            helper.assertTrue(level.getBlockState(GatewayStructure.partPos(a, Direction.SOUTH, i)).isAir(), "frame " + i + " survived");
        }
        helper.assertFalse(active(level, b), "partner still open");
        long drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(a).inflate(4)).stream()
                .filter(e -> e.getItem().is(ModItems.WARP_GATEWAY.get())).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(drops == 1, "expected exactly one gateway drop, got " + drops);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void gatewayRefusesBlockedSpace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(6, 1, 6));
        // Block the top of the ring whichever way the gateway would face.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            level.setBlock(GatewayStructure.partPos(pos, dir, GatewayStructure.TOP_PART), ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState(), 3);
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModItems.WARP_GATEWAY.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
        helper.assertFalse(level.getBlockState(pos).is(ModBlocks.WARP_GATEWAY.get()), "gateway placed into blocked space");
        helper.succeed();
    }

    // ------------------------------------------------------------------ data & loot

    @GameTest(template = EMPTY)
    public static void dialDataLoadsOldAndNewFormats(GameTestHelper helper) {
        CompoundTag old = new CompoundTag();
        old.put("destinations", new ListTag());
        old.putInt("selected", -1);
        PortalDestinations parsed = PortalDestinations.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
        helper.assertTrue(parsed.knownDimensions().isEmpty() && parsed.selectedDimension().isEmpty(), "old data parsed wrong");

        PortalDestinations learned = parsed.withKnownDimension(ModDimensions.BACKROOMS);
        Tag encoded = PortalDestinations.CODEC.encodeStart(NbtOps.INSTANCE, learned).getOrThrow();
        PortalDestinations back = PortalDestinations.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(back.equals(learned) && back.knows(ModDimensions.BACKROOMS), "round trip failed: " + back);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void journalPagesAppearInChests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int[] found = new int[3];
        var tables = List.of(BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.DESERT_PYRAMID, BuiltInLootTables.SHIPWRECK_MAP);
        var pages = List.of(ModItems.JOURNAL_PAGE_1.get(), ModItems.JOURNAL_PAGE_2.get(), ModItems.JOURNAL_PAGE_3.get());
        for (int t = 0; t < 3; t++) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(tables.get(t));
            for (int i = 0; i < 200; i++) {
                LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
                for (ItemStack stack : table.getRandomItems(params)) {
                    if (stack.is(pages.get(t))) found[t]++;
                }
            }
        }
        helper.assertTrue(found[0] > 20 && found[1] > 20 && found[2] > 20,
                "pages found in 200 chests each: " + found[0] + ", " + found[1] + ", " + found[2]);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void lightsObeyRedstone(GameTestHelper helper) {
        BlockPos light = new BlockPos(2, 2, 2);
        BlockPos power = new BlockPos(3, 2, 2);
        helper.setBlock(light, ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState());
        helper.assertTrue(helper.getBlockState(light).getLightEmission() == 15, "new light is dark");

        helper.setBlock(power, Blocks.REDSTONE_BLOCK);
        BlockState powered = helper.getBlockState(light);
        helper.assertTrue(!powered.getValue(FluorescentLightBlock.LIT) && powered.getValue(FluorescentLightBlock.POWERED), "power didn't switch it off");
        helper.assertFalse(powered.isRandomlyTicking(), "powered light still flickers");
        powered.tick(helper.getLevel(), helper.absolutePos(light), helper.getLevel().random); // a pending flicker tick must not relight it
        helper.assertFalse(helper.getBlockState(light).getValue(FluorescentLightBlock.LIT), "relit while powered");

        helper.setBlock(power, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(light).getValue(FluorescentLightBlock.LIT), "didn't turn back on");

        // Broken lights never come on, powered or not.
        helper.setBlock(light, FluorescentLightBlock.broken(ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState()));
        helper.setBlock(power, Blocks.REDSTONE_BLOCK);
        helper.setBlock(power, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(light).getLightEmission() == 0, "broken light came on");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the Bacteria & the drop

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bacteriaHuntsPlayers(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 start = helper.absoluteVec(new Vec3(2.5, 1, 2.5));
        player.moveTo(start.x, start.y, start.z);
        BacteriaEntity bacteria = helper.spawn(ModEntities.BACTERIA.get(), new BlockPos(13, 1, 13));
        helper.assertTrue(bacteria.getMaxHealth() == 40.0f, "health " + bacteria.getMaxHealth());
        helper.succeedWhen(() -> helper.assertTrue(bacteria.getTarget() == player, "not hunting the player"));
    }

    @GameTest(template = EMPTY)
    public static void bacteriaLootIsRare(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BacteriaEntity bacteria = helper.spawn(ModEntities.BACTERIA.get(), new BlockPos(2, 1, 2));
        Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(bacteria.getLootTable());
        int runs = 4000, drops = 0, fluid = 0;
        for (int i = 0; i < runs; i++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, bacteria)
                    .withParameter(LootContextParams.ORIGIN, bacteria.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                    .create(LootContextParamSets.ENTITY);
            List<ItemStack> loot = table.getRandomItems(params);
            if (!loot.isEmpty()) drops++;
            if (loot.stream().anyMatch(s -> s.is(ModItems.PORTAL_FLUID.get()))) fluid++;
        }
        helper.assertTrue(drops > runs * 0.18 && drops < runs * 0.32, "drop rate " + drops + "/" + runs);
        helper.assertTrue(fluid > runs * 0.02 && fluid < runs * 0.1, "portal fluid rate " + fluid + "/" + runs);
        bacteria.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void portalFluidDropReturnsToSavedLocation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL); // creative players don't use up items
        Vec3 home = helper.absoluteVec(new Vec3(2.5, 1, 2.5));
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.PORTAL_DESTINATIONS.get(), PortalDestinations.EMPTY.withAdded(
                new PortalDestinations.Destination("Home", level.dimension(), home.x, home.y, home.z, 0.0f)));
        player.getInventory().setItem(5, gun);
        Vec3 away = helper.absoluteVec(new Vec3(13.5, 1, 13.5));
        player.moveTo(away.x, away.y, away.z);

        ItemStack drop = new ItemStack(ModItems.PORTAL_FLUID_DROP.get());
        drop.finishUsingItem(level, player);
        helper.assertTrue(player.position().distanceTo(home) < 0.5, "player at " + player.position() + ", expected " + home);
        helper.assertTrue(drop.isEmpty(), "drop was not consumed");

        // Without any saved locations it still gets you out: to the world spawn.
        player.getInventory().clearContent();
        new ItemStack(ModItems.PORTAL_FLUID_DROP.get()).finishUsingItem(level, player);
        BlockPos spawn = level.getServer().overworld().getSharedSpawnPos();
        helper.assertTrue(player.blockPosition().getX() == spawn.getX() && player.blockPosition().getZ() == spawn.getZ(),
                "not sent to spawn: " + player.blockPosition());
        helper.succeed();
    }

    // ------------------------------------------------------------------ v0.4: mimics, Meeseeks, gadgets

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stillLifeWaitsThenAttacks(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 far = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        player.moveTo(far.x, far.y, far.z);
        MimicEntity mimic = helper.spawn(ModEntities.MIMICS.get(MimicKind.VILLAGER).get(), new BlockPos(14, 1, 14));
        Vec3 start = mimic.position();
        helper.runAfterDelay(40, () -> {
            helper.assertFalse(mimic.isAwake(), "woke up while the player was far away");
            double moved = mimic.position().subtract(start).horizontalDistance();  // (it may settle onto the floor)
            helper.assertTrue(mimic.getTarget() == null && moved < 0.1, "a statue moved " + moved + " from " + start + " to " + mimic.position());
            Vec3 near = helper.absoluteVec(new Vec3(11.5, 1, 14.5));
            player.moveTo(near.x, near.y, near.z);
        });
        helper.runAfterDelay(41, () -> helper.succeedWhen(() -> {
            helper.assertTrue(mimic.isAwake(), "didn't wake with the player close by");
            helper.assertTrue(mimic.getTarget() == player, "awake but not hunting");
        }));
    }

    @GameTest(template = EMPTY)
    public static void allStillLivesExist(GameTestHelper helper) {
        for (MimicKind kind : MimicKind.values()) {
            MimicEntity mimic = helper.spawn(ModEntities.MIMICS.get(kind).get(), new BlockPos(2 + kind.ordinal() * 2, 1, 8));
            helper.assertTrue(mimic.getMaxHealth() == (float) kind.health, kind + " health");
            mimic.hurt(helper.getLevel().damageSources().generic(), 1.0f);
            helper.assertTrue(mimic.isAwake(), kind + " didn't wake when hit");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void meeseeksFightsMonstersThenPoofs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 1, 4.5));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        ItemStack box = new ItemStack(ModItems.MEESEEKS_BOX.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        box.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(box.getDamageValue() == 1, "box not used");
        List<MeeseeksEntity> found = level.getEntitiesOfClass(MeeseeksEntity.class, new AABB(at, at).inflate(4));
        helper.assertTrue(found.size() == 1, "expected one Meeseeks, found " + found.size());
        MeeseeksEntity meeseeks = found.get(0);
        helper.assertTrue(meeseeks.getOwner() == player, "wrong owner");
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(10, 1, 10));
        helper.runAfterDelay(1, () -> helper.succeedWhen(() -> {
            helper.assertTrue(meeseeks.getTarget() == zombie, "not fighting the zombie");
            meeseeks.setLife(1);
            zombie.discard();
        }));
    }

    @GameTest(template = EMPTY)
    public static void meeseeksPoofsWhenTimeIsUp(GameTestHelper helper) {
        MeeseeksEntity meeseeks = helper.spawn(ModEntities.MEESEEKS.get(), new BlockPos(3, 1, 3));
        meeseeks.setLife(5);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(meeseeks.isRemoved(), "Meeseeks outstayed his welcome");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void portalFluidCraftsFromDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack drop = new ItemStack(ModItems.PORTAL_FLUID_DROP.get());
        List<ItemStack> grid = List.of(drop, drop, drop, drop, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE), drop, drop, drop, drop);
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        var recipe = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), "no recipe for 8 drops + bottle");
        helper.assertTrue(recipe.get().value().assemble(input, level.registryAccess()).is(ModItems.PORTAL_FLUID.get()), "wrong result");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rickGadgetsWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        ItemStack sauce = new ItemStack(ModItems.SZECHUAN_SAUCE.get());
        sauce.finishUsingItem(level, player);
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)
                && player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED), "sauce gave no buffs");

        player.removeAllEffects();
        ItemStack plumbus = new ItemStack(ModItems.PLUMBUS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, plumbus);
        plumbus.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertFalse(player.getActiveEffects().isEmpty(), "plumbus did nothing");
        helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.PLUMBUS.get()), "plumbus has no cooldown");
        helper.assertFalse(plumbus.isEmpty(), "plumbus was used up");

        player.removeAllEffects();
        ItemStack flask = new ItemStack(ModItems.RICKS_FLASK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, flask);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        flask.finishUsingItem(level, player);
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "flask didn't heal");
        helper.assertTrue(flask.getDamageValue() == 1, "flask swig not counted");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rickStashHasLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var key = net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/rick_stash"));
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        List<ItemStack> loot = table.getRandomItems(params);
        helper.assertTrue(loot.size() >= 3, "stash too small: " + loot);
        helper.succeed();
    }
}
