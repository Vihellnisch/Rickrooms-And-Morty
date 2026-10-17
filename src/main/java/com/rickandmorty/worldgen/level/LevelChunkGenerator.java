package com.rickandmorty.worldgen.level;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rickandmorty.block.entity.ExitDoorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** The chunk generator shared by every Backrooms level past Level 0; the terrain itself comes from its {@link LevelGenerator}. */
public class LevelChunkGenerator extends ChunkGenerator {
    public static final MapCodec<LevelChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            BackroomsLevel.CODEC.fieldOf("level").forGetter(g -> g.level)
    ).apply(instance, instance.stable(LevelChunkGenerator::new)));

    private final BackroomsLevel level;
    private final LevelGenerator generator;

    public LevelChunkGenerator(BiomeSource biomeSource, BackroomsLevel level) {
        super(biomeSource);
        this.level = level;
        this.generator = LevelGenerators.get(level);
    }

    public BackroomsLevel level() {
        return level;
    }

    public LevelGenerator generator() {
        return generator;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    /** Blocks go in here because this is where the world seed is known. */
    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState randomState, ChunkAccess chunk) {
        long seed = region.getSeed();
        List<BlockPos> blockEntities = generate(seed, chunk);
        for (BlockPos pos : blockEntities) {
            BlockState state = chunk.getBlockState(pos);
            // through the region, so the block entity gets created and registered
            region.setBlock(pos, state, Block.UPDATE_CLIENTS);
            if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && region.getBlockEntity(pos) instanceof ExitDoorBlockEntity door) {
                door.setDestination(generator.exitDestination(seed, pos));
            }
            var loot = generator.containerLoot(seed, pos);
            if (loot != null) RandomizableContainer.setBlockEntityLootTable(region, net.minecraft.util.RandomSource.create(seed ^ pos.asLong()), pos, loot);
            generator.blockEntity(region, seed, pos, state);
        }
    }

    /** Fills a chunk from the level's columns. Returns where blocks with block entities went. */
    public List<BlockPos> generate(long seed, ChunkAccess chunk) {
        List<BlockPos> blockEntities = new ArrayList<>();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int top = chunk.getMaxBuildHeight();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunkPos.getMinBlockX() + lx, z = chunkPos.getMinBlockZ() + lz;
                BlockState[] column = generator.column(seed, x, z);
                for (int y = 0; y < column.length && y < top; y++) {
                    if (column[y] == null) continue;
                    chunk.setBlockState(pos.set(x, y, z), column[y], false);
                    if (column[y].hasBlockEntity()) blockEntities.add(pos.immutable());
                    oceanFloor.update(lx, y, lz, column[y]);
                    worldSurface.update(lx, y, lz, column[y]);
                }
            }
        }
        return blockEntities;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        generator.populate(region, region.getSeed());
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public int getGenDepth() {
        return generator.height();
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        return generator.height();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(heightAccessor.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Backrooms: " + level.getSerializedName());
    }
}
