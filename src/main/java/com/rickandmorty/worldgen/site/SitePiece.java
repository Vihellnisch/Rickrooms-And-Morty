package com.rickandmorty.worldgen.site;

import com.rickandmorty.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Builds one {@link SiteKind}. Like the anomalies, postProcess runs once per chunk the site
 * touches and replays every random choice from the stored seed, so the pieces line up.
 */
public class SitePiece extends StructurePiece {
    private final SiteKind kind;
    private final BlockPos origin;
    private final long seed;

    public SitePiece(SiteKind kind, BlockPos origin, long seed) {
        super(ModStructures.SITE_PIECE.get(), 0, bounds(kind, origin));
        this.kind = kind;
        this.origin = origin;
        this.seed = seed;
    }

    public SitePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.SITE_PIECE.get(), tag);
        this.kind = SiteKind.CODEC.byName(tag.getString("Kind"), SiteKind.RUINED_HOUSE);
        this.origin = NbtUtils.readBlockPos(tag, "Origin").orElse(BlockPos.ZERO);
        this.seed = tag.getLong("Seed");
    }

    private static BoundingBox bounds(SiteKind kind, BlockPos o) {
        int h = kind.half;
        return new BoundingBox(o.getX() - h, o.getY() - kind.depth, o.getZ() - h, o.getX() + h, o.getY() + kind.height, o.getZ() + h);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", kind.getSerializedName());
        tag.put("Origin", NbtUtils.writeBlockPos(origin));
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        SiteBuilder b = new SiteBuilder(level, chunkBox, RandomSource.create(seed), origin);
        switch (kind) {
            case RUINED_HOUSE -> CronenbergSites.ruinedHouse(b);
            case SURVIVOR_CAMP -> CronenbergSites.survivorCamp(b);
            case HIGH_SCHOOL -> CronenbergSites.highSchool(b);
            case FLESH_PIT -> CronenbergSites.fleshPit(b);
            case COUNCIL_CHAMBER -> CouncilSite.build(b);
            case MONOLITH -> MonolithSite.build(b);
            case STORE -> StoreSite.build(b);
        }
    }
}
