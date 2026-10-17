package com.rickandmorty.worldgen.anomaly;

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
 * A rare Overworld anomaly (see {@link AnomalyKind}). Placement rarity lives in the structure
 * set JSON; this just picks a dry spot on the surface. Findable with /locate structure.
 */
public class AnomalyStructure extends Structure {
    public static final MapCodec<AnomalyStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            AnomalyKind.CODEC.fieldOf("kind").forGetter(s -> s.kind)
    ).apply(instance, AnomalyStructure::new));

    private final AnomalyKind kind;

    public AnomalyStructure(StructureSettings settings, AnomalyKind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        int ground = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        int surface = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (surface > ground + 1 || ground <= context.heightAccessor().getMinBuildHeight() + 5) {
            return Optional.empty();  // under water, or no real ground
        }
        BlockPos origin = new BlockPos(x, ground, z);
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new AnomalyPiece(kind, origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ANOMALY.get();
    }
}
