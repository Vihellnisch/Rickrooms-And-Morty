package com.rickandmorty.gametest;

import com.mojang.logging.LogUtils;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.cronenberg.Mutation;
import com.rickandmorty.entity.CronenbergKind;
import com.rickandmorty.entity.PortalEntity;
import com.rickandmorty.entity.PortalTeleporter;
import com.rickandmorty.entity.SurvivorKind;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.ModDimensions;
import com.rickandmorty.worldgen.site.SiteKind;
import com.rickandmorty.worldgen.site.SitePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Dev-only scripted walkthrough for visual checks (./gradlew runTourClient). Each stop sets up
 * a scene; {@link DevTourClient} takes a screenshot (GUI hidden) near the end of each stop and
 * quits after the last. Stop names ending in "_front" are shot in third person, from the front.
 * Does nothing unless -Drickrooms.tour=true.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public class DevTour {
    static final boolean ENABLED = Boolean.getBoolean("rickrooms.tour");
    static final int FIRST_STOP = 40;
    static final int STOP_LENGTH = 130;
    static final Logger LOGGER = LogUtils.getLogger();

    private record Stop(String name, BiConsumer<ServerPlayer, ServerLevel> action) {
    }

    private static final List<Stop> STOPS = buildStops();
    static final List<String> STOP_NAMES = STOPS.stream().map(Stop::name).toList();
    static int ticks = -1;
    /** Things a stop wants done a little later (tick, action). */
    static final List<java.util.Map.Entry<Integer, Runnable>> LATER = new ArrayList<>();

    private static List<Stop> buildStops() {
        List<Stop> stops = new ArrayList<>();
        if (Boolean.getBoolean("rickrooms.tour.sites")) {
            // close-ups only
            stops.add(new Stop("evil_gateway_blend", (p, l) -> evilGateway(p, l, false)));
            stops.add(new Stop("evil_gateway_meltdown", (p, l) -> evilGateway(p, l, true)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.droplimit")) DropLimitTour.addStops((name, action) -> stops.add(new Stop(name, action)));
        if (Boolean.getBoolean("rickrooms.tour.facade")) FacadeTour.addStops((name, action) -> stops.add(new Stop(name, action)));
        if (Boolean.getBoolean("rickrooms.tour.staircase")) StaircaseTour.addStops((name, action) -> stops.add(new Stop(name, action)));
        if (Boolean.getBoolean("rickrooms.tour.froopyland")) FroopylandTour.addStops((name, action) -> stops.add(new Stop(name, action)));
        if (Boolean.getBoolean("rickrooms.tour.run")) RunTour.addStops((name, action) -> stops.add(new Stop(name, action)));
        if (Boolean.getBoolean("rickrooms.tour.gun")) GunTour.addStops((name, action) -> stops.add(new Stop(name, action)));
        if (!stops.isEmpty()) return stops;
        if (Boolean.getBoolean("rickrooms.tour.v22")) {
            V22Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.curios")) {
            CuriosTour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.city")) {
            CityTour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v19")) {
            V19Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v18")) {
            V18Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v17")) {
            V17Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v16")) {
            V16Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v15")) {
            V15Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v14")) {
            V14Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v13")) {
            V13Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v12")) {
            V12Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v11")) {
            V11Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v10")) {
            V10Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.v09")) {
            V9Tour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.shaders")) {
            ShaderTour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        if (Boolean.getBoolean("rickrooms.tour.levels")) {
            LevelTour.addStops((name, action) -> stops.add(new Stop(name, action)));
            return stops;
        }
        stops.add(new Stop("cronenberg_landing", (p, l) -> landing(p, l)));
        for (String biome : List.of("flesh_plains", "bone_forest", "blood_marsh", "tumor_wastes", "cronenberg_suburbs", "blood_sea")) {
            stops.add(new Stop("biome_" + biome, (p, l) -> biomeStop(p, l, biome)));
        }
        for (String site : List.of("survivor_camp", "high_school", "flesh_pit", "ruined_house")) {
            stops.add(new Stop("site_" + site, (p, l) -> siteStop(p, cronenberg(l), site, 26, 14)));
        }
        stops.add(new Stop("inside_smith_house", (p, l) -> insideStop(p, cronenberg(l), "survivor_camp")));
        stops.add(new Stop("site_council_chamber", (p, l) -> councilStop(p, l, false)));
        stops.add(new Stop("inside_council", (p, l) -> councilStop(p, l, true)));
        stops.add(new Stop("lineup_cronenbergs", (p, l) -> lineup(p, l, 0)));
        stops.add(new Stop("lineup_behemoth", (p, l) -> lineup(p, l, 1)));
        stops.add(new Stop("lineup_people", (p, l) -> lineup(p, l, 2)));
        stops.add(new Stop("lineup_council", (p, l) -> lineup(p, l, 3)));
        stops.add(new Stop("blender_portal", (p, l) -> blender(p, l)));
        stops.add(new Stop("blender_gun", (p, l) -> gunFrame(p, l)));
        stops.add(new Stop("mutated_player_front", (p, l) -> mutated(p, l)));
        return stops;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED) return;
        var players = event.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer player = players.get(0);
        ticks++;
        LATER.removeIf(task -> {
            if (task.getKey() > ticks) return false;
            task.getValue().run();
            return true;
        });
        if (ticks < FIRST_STOP || (ticks - FIRST_STOP) % STOP_LENGTH != 0) return;
        int stop = (ticks - FIRST_STOP) / STOP_LENGTH;
        if (stop == 0) LOGGER.info("TOUR world generation lifecycle: {}", event.getServer().getWorldData().worldGenSettingsLifecycle());
        if (stop >= STOPS.size()) return;
        player.setGameMode(GameType.CREATIVE);
        ServerLevel overworld = event.getServer().overworld();
        overworld.setDayTime(6000);
        try {
            STOPS.get(stop).action().accept(player, overworld);
        } catch (RuntimeException e) {
            LOGGER.error("TOUR stop {} failed", STOPS.get(stop).name(), e);
        }
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        LOGGER.info("TOUR stop {}: {} at {} in {}", stop, STOPS.get(stop).name(), player.position(), player.level().dimension().location());
    }

    private static ServerLevel cronenberg(ServerLevel overworld) {
        ServerLevel level = overworld.getServer().getLevel(ModDimensions.CRONENBERG);
        if (level == null) throw new IllegalStateException("no Cronenberg World");
        level.setDayTime(5000);
        return level;
    }

    private static BlockPos scene(ServerLevel overworld, int index) {
        int x = index * 48, z = -600;
        overworld.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
    }

    static void go(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        if (player.level() != level) PortalTeleporter.teleport(player, player.serverLevel(), level, new Vec3(x, y, z), yaw);
        player.connection.teleport(x, y, z, yaw, pitch);
    }

    private static int ground(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** A random portal-gun landing, looking out over the land from a little above it. */
    private static void landing(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = cronenberg(overworld);
        var dest = ModDimensions.randomDestination(level.getServer(), ModDimensions.CRONENBERG, level.random).orElseThrow();
        int y = ground(level, (int) dest.x(), (int) dest.z());
        go(player, level, dest.x(), y + 6, dest.z(), 30.0f, 12.0f);
    }

    private static void biomeStop(ServerPlayer player, ServerLevel overworld, String biome) {
        ServerLevel level = cronenberg(overworld);
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, RickAndMortyMod.id(biome));
        var found = level.findClosestBiome3d(h -> h.is(key), BlockPos.ZERO.atY(64), 6400, 32, 64);
        if (found == null) {
            LOGGER.error("TOUR: no {} found", biome);
            return;
        }
        BlockPos at = found.getFirst();
        int y = ground(level, at.getX(), at.getZ());
        go(player, level, at.getX() + 0.5, y + 4, at.getZ() + 0.5, 45.0f, 15.0f);
    }

    /**
     * The tour world was made with structures turned off, so find the nearest spot where the site
     * would generate (the same placement rules as real worlds) and build it there by hand.
     */
    static BlockPos locate(ServerLevel level, String site) {
        SiteKind kind = SiteKind.CODEC.byName(site, SiteKind.RUINED_HOUSE);
        Holder<Structure> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, RickAndMortyMod.id(site)));
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(RickAndMortyMod.id(site));
        var placement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement) set.placement();
        var state = level.getChunkSource().getGeneratorState();
        BlockPos best = null;
        for (int ring = 0; ring <= 6 && best == null; ring++) {
            for (int gx = -ring; gx <= ring; gx++) {
                for (int gz = -ring; gz <= ring; gz++) {
                    if (Math.max(Math.abs(gx), Math.abs(gz)) != ring) continue;
                    var chunk = placement.getPotentialStructureChunk(state.getLevelSeed(), gx * placement.spacing(), gz * placement.spacing());
                    var context = new Structure.GenerationContext(level.registryAccess(), level.getChunkSource().getGenerator(),
                            level.getChunkSource().getGenerator().getBiomeSource(), level.getChunkSource().randomState(),
                            level.getStructureManager(), state.getLevelSeed(), chunk, level, holder.value().biomes()::contains);
                    var stub = holder.value().findValidGenerationPoint(context);
                    if (stub.isPresent() && (best == null || stub.get().position().distSqr(BlockPos.ZERO) < best.distSqr(BlockPos.ZERO))) {
                        best = stub.get().position();
                    }
                }
            }
        }
        if (best == null) throw new IllegalStateException("no " + site + " found");
        if (BUILT.add(site)) {
            int r = kind.half / 16 + 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) level.getChunk((best.getX() >> 4) + dx, (best.getZ() >> 4) + dz);
            }
            SitePiece piece = new SitePiece(kind, best, best.asLong());
            var everywhere = new net.minecraft.world.level.levelgen.structure.BoundingBox(best.getX() - 40, level.getMinBuildHeight(), best.getZ() - 40,
                    best.getX() + 40, level.getMaxBuildHeight(), best.getZ() + 40);
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, everywhere,
                    new net.minecraft.world.level.ChunkPos(best), best);
            LOGGER.info("TOUR: built {} at {}", site, best);
        }
        return best;
    }

    private static final java.util.Set<String> BUILT = new java.util.HashSet<>();

    /** Stand south of the site, above it, looking back at it. */
    private static void siteStop(ServerPlayer player, ServerLevel level, String site, int back, int up) {
        BlockPos at = locate(level, site);
        go(player, level, at.getX() + 0.5, at.getY() + up, at.getZ() + back + 0.5, 180.0f, 28.0f);
        LOGGER.info("TOUR: {} at {}", site, at);
    }

    private static void insideStop(ServerPlayer player, ServerLevel level, String site) {
        BlockPos at = locate(level, site);
        List<? extends Mob> people = level.getEntitiesOfClass(Mob.class, new AABB(at).inflate(20));
        if (site.equals("survivor_camp")) {
            go(player, level, at.getX() + 0.5, at.getY() + 2.5, at.getZ() + 9.5, 180.0f, 10.0f);   // at the gate, looking at the house
        } else {
            go(player, level, at.getX() + 0.5, at.getY() + 2.6, at.getZ() + 9.0, 180.0f, 12.0f);   // in the doorway
        }
        LOGGER.info("TOUR: {} people around {}: {}", site, at, people.size());
    }

    /** The tour's Overworld is superflat (no natural structures), so the Council is built on a plot. */
    private static BlockPos council;

    private static void councilStop(ServerPlayer player, ServerLevel overworld, boolean inside) {
        if (council == null) {
            council = scene(overworld, 10).above();
            SitePiece piece = new SitePiece(SiteKind.COUNCIL_CHAMBER, council, 137L);
            var everywhere = new net.minecraft.world.level.levelgen.structure.BoundingBox(council.getX() - 20, overworld.getMinBuildHeight(),
                    council.getZ() - 20, council.getX() + 20, overworld.getMaxBuildHeight(), council.getZ() + 20);
            piece.postProcess(overworld, overworld.structureManager(), overworld.getChunkSource().getGenerator(), overworld.random, everywhere,
                    new net.minecraft.world.level.ChunkPos(council), council);
        }
        if (inside) go(player, overworld, council.getX() + 0.5, council.getY() + 2.6, council.getZ() + 9.0, 180.0f, 12.0f);
        else go(player, overworld, council.getX() + 0.5, council.getY() + 10, council.getZ() + 26.5, 180.0f, 20.0f);
    }

    /** Rows of creatures on a flesh floor in the Overworld, frozen in place, to look at up close. */
    private static void lineup(ServerPlayer player, ServerLevel overworld, int which) {
        BlockPos at = scene(overworld, which);
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                overworld.setBlock(at.offset(dx, 0, dz), ModBlocks.VEINED_FLESH.get().defaultBlockState(), 3);
                for (int y = 1; y <= 8; y++) overworld.setBlock(at.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        List<EntityType<? extends Mob>> types = new ArrayList<>();
        switch (which) {
            case 0 -> {
                for (CronenbergKind kind : CronenbergKind.values()) types.add(ModEntities.CRONENBERGS.get(kind).get());
                types.add(ModEntities.MANTIS.get());
            }
            case 1 -> types.add(ModEntities.BEHEMOTH.get());
            case 2 -> {
                for (SurvivorKind kind : SurvivorKind.values()) types.add(ModEntities.SURVIVORS.get(kind).get());
            }
            default -> {
                for (int i = 0; i < 5; i++) types.add(ModEntities.COUNCIL_MEMBER.get());
            }
        }
        double spacing = which == 1 ? 0 : which == 0 ? 3.2 : 2.0;
        double start = -(types.size() - 1) * spacing / 2;
        for (int i = 0; i < types.size(); i++) {
            Mob mob = types.get(i).create(overworld);
            if (mob == null) continue;
            mob.moveTo(at.getX() + 0.5 + start + i * spacing, at.getY() + 1, at.getZ() + 0.5, 0.0f, 0.0f);
            mob.setYHeadRot(0.0f);
            mob.setYBodyRot(0.0f);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            if (mob instanceof com.rickandmorty.entity.BehemothEntity behemoth) behemoth.wake();
            overworld.addFreshEntity(mob);
        }
        double distance = which == 1 ? 11 : which == 0 ? 8 : 6;
        go(player, overworld, at.getX() + 0.5, at.getY() + (which == 1 ? 3.5 : 2.2), at.getZ() + distance, 180.0f, which == 1 ? 8.0f : 12.0f);
    }

    /** One creature on a flesh plot, up close. */
    private static void closeup(ServerPlayer player, ServerLevel overworld, EntityType<? extends Mob> type, int slot, double distance, double eye) {
        BlockPos at = scene(overworld, 20 + slot);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 10; dz++) {
                overworld.setBlock(at.offset(dx, 0, dz), ModBlocks.VEINED_FLESH.get().defaultBlockState(), 3);
                for (int y = 1; y <= 8; y++) overworld.setBlock(at.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        Mob mob = type.create(overworld);
        if (mob == null) return;
        mob.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 20.0f, 0.0f);
        mob.setYHeadRot(20.0f);
        mob.setYBodyRot(20.0f);
        mob.setNoAi(true);
        if (mob instanceof com.rickandmorty.entity.BehemothEntity behemoth) behemoth.wake();
        overworld.addFreshEntity(mob);
        if (type == ModEntities.SURVIVORS.get(SurvivorKind.CRONENBERG_RICK).get()) {
            Mob morty = ModEntities.SURVIVORS.get(SurvivorKind.CRONENBERG_MORTY).get().create(overworld);
            if (morty != null) {
                morty.moveTo(at.getX() + 1.6, at.getY() + 1, at.getZ() + 0.8, -10.0f, 0.0f);
                morty.setYHeadRot(-10.0f);
                morty.setYBodyRot(-10.0f);
                morty.setNoAi(true);
                overworld.addFreshEntity(morty);
            }
        }
        go(player, overworld, at.getX() + 0.5 + distance * 0.35, at.getY() + 1 + eye, at.getZ() + 0.5 + distance, 160.0f, 12.0f);
    }

    /** An evil gateway, mid-blend (an iron golem wanders in), or mid-meltdown. */
    private static void evilGateway(ServerPlayer player, ServerLevel overworld, boolean meltdown) {
        BlockPos at = scene(overworld, meltdown ? 31 : 30);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 8; dz++) {
                overworld.setBlock(at.offset(dx, 0, dz), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                for (int y = 1; y <= 6; y++) overworld.setBlock(at.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        BlockPos pos = at.above();
        var state = ModBlocks.WARP_GATEWAY.get().defaultBlockState().setValue(com.rickandmorty.block.WarpGatewayBlock.FACING, net.minecraft.core.Direction.SOUTH);
        overworld.setBlock(pos, state, 3);
        state.getBlock().setPlacedBy(overworld, pos, state, null, new ItemStack(ModItems.WARP_GATEWAY.get()));
        var gateway = (com.rickandmorty.block.entity.WarpGatewayBlockEntity) overworld.getBlockEntity(pos);
        if (meltdown) {
            com.rickandmorty.ModConfig.GATEWAY_EXPLOSION.set(0.0);
            LATER.add(java.util.Map.entry(ticks + 25, () -> com.rickandmorty.block.entity.EvilGateway.forceMeltdown(overworld, gateway)));
        } else {
            com.rickandmorty.block.entity.EvilGateway.toggle(overworld, gateway, player);   // armed by the tour player, so it spares them
            Vec3 center = com.rickandmorty.block.entity.EvilGateway.center(pos, net.minecraft.core.Direction.SOUTH);
            LATER.add(java.util.Map.entry(ticks + 60, () -> {
                var golem = EntityType.IRON_GOLEM.create(overworld);
                if (golem != null) {
                    golem.moveTo(center.x, pos.getY(), center.z + 2.2, 180.0f, 0.0f);
                    golem.setNoAi(true);
                    overworld.addFreshEntity(golem);
                }
            }));
        }
        go(player, overworld, pos.getX() + 5.5, pos.getY() + 2.2, pos.getZ() + 7.5, 145.0f, 10.0f);
    }

    /** A blender portal holding an iron golem, mid-blend, when the screenshot is taken. */
    private static void blender(ServerPlayer player, ServerLevel overworld) {
        BlockPos at = scene(overworld, 5);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                overworld.setBlock(at.offset(dx, 0, dz), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                for (int y = 1; y <= 6; y++) overworld.setBlock(at.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        Vec3 center = Vec3.atCenterOf(at.above(2));
        PortalEntity.spawnInAir(overworld, center, 0.0f, null, player, true);
        // the golem wanders in later, so the screenshot catches it mid-blend
        LATER.add(java.util.Map.entry(ticks + 60, () -> {
            var golem = EntityType.IRON_GOLEM.create(overworld);
            if (golem != null) {
                golem.moveTo(center.x, at.getY() + 1, center.z + 1.8, 180.0f, 0.0f);
                golem.setNoAi(true);
                overworld.addFreshEntity(golem);
            }
        }));
        go(player, overworld, center.x + 3.6, at.getY() + 2.4, center.z + 3.4, 135.0f, 8.0f);
    }

    /** The upgraded gun in attack mode, on the wall in an item frame. */
    private static void gunFrame(ServerPlayer player, ServerLevel overworld) {
        BlockPos at = scene(overworld, 6);
        BlockPos wall = at.above(2);
        overworld.setBlock(wall, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 3);
        for (int dz = 1; dz <= 3; dz++) overworld.setBlock(wall.south(dz), Blocks.AIR.defaultBlockState(), 3);
        ItemStack gun = new ItemStack(ModItems.PORTAL_GUN.get());
        gun.set(ModDataComponents.BLENDER.get(), true);
        gun.set(ModDataComponents.ATTACK_MODE.get(), true);
        ItemFrame frame = new ItemFrame(overworld, wall.south(), net.minecraft.core.Direction.SOUTH);
        frame.setItem(gun);
        overworld.addFreshEntity(frame);
        go(player, overworld, wall.getX() + 0.5, wall.getY() - 0.1, wall.getZ() + 2.2, 180.0f, 0.0f);
    }

    /** You, fully Cronenberged (shot in third person from the front). */
    private static void mutated(ServerPlayer player, ServerLevel overworld) {
        BlockPos at = scene(overworld, 7);
        Mutation.set(player, 100.0f);
        player.setGameMode(GameType.CREATIVE);
        go(player, overworld, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 180.0f, 5.0f);
        player.getAbilities().flying = false;
    }
}
