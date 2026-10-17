package com.rickandmorty.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generates the endless yellow rooms of the Backrooms (Level 0) from {@link BackroomsLayout}.
 * Blocks are placed in buildSurface because that's where the world seed is available.
 */
public class BackroomsChunkGenerator extends ChunkGenerator {
    public static final MapCodec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, instance.stable(BackroomsChunkGenerator::new)));

    private static final ResourceKey<LootTable> RICK_STASH = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("rickrooms", "chests/rick_stash"));

    /** Slot the Drop of Portal Fluid goes in (the middle of a chest). */
    private static final int DROP_SLOT = 13;
    private static final int ALMOND_SLOT = 4;

    public BackroomsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState randomState, ChunkAccess chunk) {
        List<BlockPos> containers = generate(region.getSeed(), chunk);
        registerBlockEntities(region, chunk, containers);
        placeChests(region, chunk);
    }

    /**
     * Fills a chunk with floors, walls, rooms, lights, ceilings and biome-patch decorations.
     * Returns where blocks with block entities (barrels, furnaces...) were put.
     */
    public List<BlockPos> generate(long seed, ChunkAccess chunk) {
        List<BlockPos> blockEntities = new ArrayList<>();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunkPos.getMinBlockX() + lx;
                int z = chunkPos.getMinBlockZ() + lz;
                BlockState[] column = BackroomsLayout.column(seed, x, z);
                // the pit room's walls reach far below the floor
                BlockState below = BackroomsZones.below(seed, x, z);
                if (below != null) {
                    for (int y = BackroomsZones.PIT_BOTTOM; y < 0; y++) chunk.setBlockState(pos.set(x, y, z), below, false);
                }
                for (int y = 0; y < column.length; y++) {
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

    /** Re-places furniture with block entities through the region so they get registered, and fills cupboards. */
    private void registerBlockEntities(WorldGenRegion region, ChunkAccess chunk, List<BlockPos> positions) {
        long seed = region.getSeed();
        for (BlockPos pos : positions) {
            BlockState state = chunk.getBlockState(pos);
            region.setBlock(pos, state, Block.UPDATE_CLIENTS);
            if (region.getBlockEntity(pos) instanceof com.rickandmorty.block.entity.ExitDoorBlockEntity door) {
                door.setDestination(com.rickandmorty.worldgen.level.BackroomsLevel.SUNLIT_LOBBY);   // the monolith's door
                continue;
            }
            if (!state.is(Blocks.BARREL)) continue;
            BackroomsRooms.Room room = BackroomsRooms.roomAt(seed, BackroomsLayout.levelAt(pos.getY()), pos.getX(), pos.getZ());
            var loot = room == null ? null : BackroomsRooms.containerLoot(room);
            if (loot != null && region.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel) {
                barrel.setLootTable(loot, seed ^ pos.asLong());
            }
        }
    }

    /**
     * Rare chests. Most use a random chest loot table from any loaded data pack or mod;
     * the rest are empty except for a Drop of Portal Fluid.
     */
    private void placeChests(WorldGenRegion region, ChunkAccess chunk) {
        long seed = region.getSeed();
        ChunkPos chunkPos = chunk.getPos();
        int minCx = Math.floorDiv(chunkPos.getMinBlockX(), BackroomsLayout.CELL);
        int maxCx = Math.floorDiv(chunkPos.getMaxBlockX(), BackroomsLayout.CELL);
        int minCz = Math.floorDiv(chunkPos.getMinBlockZ(), BackroomsLayout.CELL);
        int maxCz = Math.floorDiv(chunkPos.getMaxBlockZ(), BackroomsLayout.CELL);
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                BackroomsLayout.ChestKind kind = BackroomsLayout.chest(seed, cx, cz);
                BlockPos pos = BackroomsLayout.chestPos(cx, cz);
                if (kind == BackroomsLayout.ChestKind.NONE || !new ChunkPos(pos).equals(chunkPos)) continue;

                long hash = BackroomsLayout.chestHash(seed, cx, cz);
                Direction facing = Direction.from2DDataValue((int) Math.floorMod(hash, 4L));
                // Placed through the region (not the chunk) so the chest's block entity gets registered.
                region.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), Block.UPDATE_CLIENTS);
                if (!(region.getBlockEntity(pos) instanceof ChestBlockEntity chest)) continue;
                if (Math.floorMod(hash >>> 50, 2L) == 0) {   // half of all chests: a bottle of almond water
                    chest.setItem(ALMOND_SLOT, new ItemStack(ModItems.ALMOND_WATER.get(), 1 + (int) Math.floorMod(hash >>> 52, 2L)));
                }
                if (kind == BackroomsLayout.ChestKind.PORTAL_DROP) {
                    chest.setItem(DROP_SLOT, new ItemStack(ModItems.PORTAL_FLUID_DROP.get()));
                } else if (Math.floorMod(hash >>> 40, 10L) < 3) {
                    chest.setLootTable(RICK_STASH, hash);           // 30%: something Rick left behind
                } else {
                    chest.setLootTable(randomChestLootTable(region, hash >>> 2), hash);
                }
            }
        }
    }

    private static ResourceKey<LootTable> randomChestLootTable(WorldGenRegion region, long hash) {
        List<ResourceLocation> tables = region.getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE).stream()
                .filter(id -> id.getPath().startsWith("chests/"))
                .sorted()
                .toList();
        if (tables.isEmpty()) return BuiltInLootTables.SIMPLE_DUNGEON;
        return ResourceKey.create(Registries.LOOT_TABLE, tables.get((int) Math.floorMod(hash, (long) tables.size())));
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomes,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 128;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return BackroomsLayout.ROOF_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = i + level.getMinBuildHeight();
            states[i] = switch (y) {
                case 0, BackroomsLayout.ROOF_Y -> Blocks.BEDROCK.defaultBlockState();
                case BackroomsLayout.FLOOR_Y -> ModBlocks.BACKROOMS_CARPET.get().defaultBlockState();
                case BackroomsLayout.CEILING_Y -> ModBlocks.BACKROOMS_CEILING.get().defaultBlockState();
                default -> Blocks.AIR.defaultBlockState();
            };
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    }
}
