package com.rickandmorty.worldgen;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.PortalDestinations;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

/** The mod's dimensions (defined in data/rickrooms/dimension) and how the portal gun lands in them. */
public final class ModDimensions {
    public static final ResourceKey<Level> BACKROOMS = ResourceKey.create(Registries.DIMENSION, RickAndMortyMod.id("backrooms"));
    public static final ResourceKey<Level> CRONENBERG = ResourceKey.create(Registries.DIMENSION, RickAndMortyMod.id("cronenberg"));
    public static final ResourceKey<Biome> BLOOD_SEA = ResourceKey.create(Registries.BIOME, RickAndMortyMod.id("blood_sea"));
    /** Cronenberg World landings are picked within this many blocks of the origin. */
    private static final int CRONENBERG_RANGE = 4000;

    /** Random landing spots are picked within this many cells of the origin. */
    private static final int BACKROOMS_RANGE_CELLS = 5000;

    private ModDimensions() {
    }

    /**
     * Picks a fresh destination in a dimension the gun knows about. Different every time.
     * Empty if the dimension doesn't exist on this server.
     */
    public static Optional<PortalDestinations.Destination> randomDestination(MinecraftServer server, ResourceKey<Level> dimension,
                                                                             RandomSource random) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return Optional.empty();
        if (dimension == BACKROOMS) {
            long seed = level.getSeed();
            int floor = random.nextInt(BackroomsLayout.LEVELS);
            int cx, cz;
            do {  // skip cells with stairs, ladders, pits or furniture in the middle
                cx = random.nextIntBetweenInclusive(-BACKROOMS_RANGE_CELLS, BACKROOMS_RANGE_CELLS);
                cz = random.nextIntBetweenInclusive(-BACKROOMS_RANGE_CELLS, BACKROOMS_RANGE_CELLS);
            } while (!BackroomsLayout.isOpenCell(seed, floor, cx, cz));
            float yaw = random.nextInt(4) * 90.0f;
            return Optional.of(new PortalDestinations.Destination("The Backrooms", dimension,
                    BackroomsLayout.cellCenter(cx), BackroomsLayout.standY(floor), BackroomsLayout.cellCenter(cz), yaw));
        }
        if (dimension == CRONENBERG) {
            // anywhere on dry land; the exact height is settled when you arrive (see PortalTeleporter)
            var generator = level.getChunkSource().getGenerator();
            var randomState = level.getChunkSource().randomState();
            int x = 0, z = 0;
            for (int attempt = 0; attempt < 30; attempt++) {
                x = random.nextIntBetweenInclusive(-CRONENBERG_RANGE, CRONENBERG_RANGE);
                z = random.nextIntBetweenInclusive(-CRONENBERG_RANGE, CRONENBERG_RANGE);
                var biome = generator.getBiomeSource().getNoiseBiome(x >> 2, 16, z >> 2, randomState.sampler());
                int ground = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
                if (!biome.is(BLOOD_SEA) && ground > generator.getSeaLevel()) break;
            }
            int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
            return Optional.of(new PortalDestinations.Destination("Cronenberg World", dimension, x + 0.5, y + 0.1, z + 0.5,
                    random.nextInt(4) * 90.0f));
        }
        var backrooms = com.rickandmorty.worldgen.level.BackroomsLevel.of(dimension);
        if (backrooms != null && level.getChunkSource().getGenerator() instanceof com.rickandmorty.worldgen.level.LevelChunkGenerator generator) {
            var spot = generator.generator().landing(level.getSeed(), random);
            return Optional.of(new PortalDestinations.Destination(backrooms.getSerializedName(), dimension, spot.x, spot.y, spot.z,
                    random.nextInt(4) * 90.0f));
        }
        return Optional.empty();
    }

    /** Dimensions where a random landing has to be moved on top of whatever grew there after the terrain. */
    public static boolean needsSafeLanding(ResourceKey<Level> dimension) {
        return dimension == CRONENBERG;
    }
}
