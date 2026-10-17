package com.rickandmorty.worldgen.site;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rickandmorty.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * One {@link SiteKind}, placed on dry, fairly level ground. Rarity lives in the structure set
 * JSON; findable with /locate structure.
 */
public class SiteStructure extends Structure {
    public static final MapCodec<SiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            SiteKind.CODEC.fieldOf("kind").forGetter(s -> s.kind)
    ).apply(instance, SiteStructure::new));

    private final SiteKind kind;

    public SiteStructure(StructureSettings settings, SiteKind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        int h = kind == SiteKind.MONOLITH ? 8 : kind.half * 3 / 4;   // the corners get levelled by the terrain adaptation anyway
        // terrain samples are expensive (and /locate tries many spots), so: the corners and the middle,
        // ground only, plus one check for water in the middle
        int center = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        int surface = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (surface > center + 1 || center < generator.getSeaLevel() - 1 || center <= context.heightAccessor().getMinBuildHeight() + 8) {
            return Optional.empty();   // under water, or no real ground
        }
        int lowest = center, highest = center;
        for (int[] c : new int[][]{{-h, -h}, {h, -h}, {-h, h}, {h, h}}) {
            int ground = generator.getFirstOccupiedHeight(x + c[0], z + c[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            if (ground < generator.getSeaLevel() - 1) return Optional.empty();
            lowest = Math.min(lowest, ground);
            highest = Math.max(highest, ground);
        }
        if (highest - lowest > (kind == SiteKind.FLESH_PIT ? 16 : 12)) return Optional.empty();   // a cliff is no place to build
        BlockPos origin = new BlockPos(x, kind == SiteKind.FLESH_PIT ? center : (center + lowest + highest) / 3, z);
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new SitePiece(kind, origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SITE.get();
    }
}
