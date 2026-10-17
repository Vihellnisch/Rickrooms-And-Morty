package com.rickandmorty.worldgen.anomaly;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.block.FluorescentLightBlock;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds one anomaly. postProcess runs once per chunk the anomaly touches, so every random
 * choice is drawn in the same order from the stored seed each time (whether or not the block
 * lands in this chunk). That keeps pieces that cross chunk borders consistent.
 */
public class AnomalyPiece extends StructurePiece {
    private static final ResourceKey<LootTable> REMNANT_LOOT = ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/backrooms_remnant"));

    private final AnomalyKind kind;
    private final BlockPos origin;
    private final long seed;

    public AnomalyPiece(AnomalyKind kind, BlockPos origin, long seed) {
        super(ModStructures.ANOMALY_PIECE.get(), 0, bounds(kind, origin));
        this.kind = kind;
        this.origin = origin;
        this.seed = seed;
    }

    public AnomalyPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.ANOMALY_PIECE.get(), tag);
        this.kind = AnomalyKind.CODEC.byName(tag.getString("Kind"), AnomalyKind.GLITCHED_PATCH);
        this.origin = NbtUtils.readBlockPos(tag, "Origin").orElse(BlockPos.ZERO);
        this.seed = tag.getLong("Seed");
    }

    private static BoundingBox bounds(AnomalyKind kind, BlockPos o) {
        return switch (kind) {
            case BACKROOMS_REMNANT -> new BoundingBox(o.getX() - 7, o.getY() - 5, o.getZ() - 7, o.getX() + 7, o.getY() + 6, o.getZ() + 7);
            case INVERTED_TREE -> new BoundingBox(o.getX() - 4, o.getY() - 2, o.getZ() - 4, o.getX() + 4, o.getY() + 12, o.getZ() + 4);
            case GLITCHED_PATCH -> new BoundingBox(o.getX() - 9, o.getY() - 12, o.getZ() - 9, o.getX() + 9, o.getY() + 20, o.getZ() + 9);
        };
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
        RandomSource r = RandomSource.create(seed);
        switch (kind) {
            case BACKROOMS_REMNANT -> remnant(level, chunkBox, r);
            case INVERTED_TREE -> invertedTree(level, chunkBox, r);
            case GLITCHED_PATCH -> glitchedPatch(level, chunkBox, r);
        }
    }

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) level.setBlock(pos, state, 2);
    }

    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    /** Top solid block of a column. */
    private static BlockPos ground(WorldGenLevel level, int x, int z) {
        Heightmap.Types type = level instanceof WorldGenRegion ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.OCEAN_FLOOR;
        return new BlockPos(x, level.getHeight(type, x, z) - 1, z);
    }

    // ------------------------------------------------------------------ remnant

    /** A 9x9 room of Level 0 with crumbling walls, a half-collapsed ceiling and one working light. */
    private void remnant(WorldGenLevel level, BoundingBox box, RandomSource r) {
        int half = 4;
        BlockState carpet = ModBlocks.BACKROOMS_CARPET.get().defaultBlockState();
        BlockState wall = ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState();
        BlockState ceiling = ModBlocks.BACKROOMS_CEILING.get().defaultBlockState();
        BlockState light = ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                float floorRoll = r.nextFloat(), ceilingRoll = r.nextFloat();
                float[] wallRolls = {r.nextFloat(), r.nextFloat(), r.nextFloat()};
                boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
                boolean doorway = dz == half && (dx == 0 || dx == 1);
                BlockPos floor = origin.offset(dx, 0, dz);
                if (!box.isInside(floor)) continue;
                set(level, box, floor, floorRoll < 0.1f ? Blocks.COARSE_DIRT.defaultBlockState() : carpet);
                for (int d = 1; d <= 4 && replaceable(level.getBlockState(floor.below(d))); d++) {
                    set(level, box, floor.below(d), Blocks.DIRT.defaultBlockState());   // don't float over dips
                }
                for (int y = 1; y <= 3; y++) {
                    boolean standing = edge && !doorway && wallRolls[y - 1] > 0.1f + 0.18f * y;  // tops crumble more
                    set(level, box, floor.above(y), standing ? wall : air);
                }
                BlockState top = dx == 0 && dz == 0 ? light : ceilingRoll > 0.45f ? ceiling : air;
                set(level, box, floor.above(4), top);
            }
        }
        boolean hasChest = r.nextFloat() < 0.6f;
        BlockPos chest = origin.offset(-3, 1, -3);
        if (hasChest && box.isInside(chest)) {
            set(level, box, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
            RandomizableContainer.setBlockEntityLootTable(level, r, chest, REMNANT_LOOT);
        }
        // a few scraps of carpet bleeding into the land around it
        for (int i = 0; i < 8; i++) {
            int dx = r.nextInt(15) - 7, dz = r.nextInt(15) - 7;
            if (Math.abs(dx) <= half && Math.abs(dz) <= half) continue;
            BlockPos column = origin.offset(dx, 0, dz);
            if (box.isInside(column)) set(level, box, ground(level, column.getX(), column.getZ()), carpet);
        }
    }

    // ------------------------------------------------------------------ inverted tree

    private void invertedTree(WorldGenLevel level, BoundingBox box, RandomSource r) {
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        // canopy squashed against the ground
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    float roll = r.nextFloat();
                    double d = Math.sqrt(dx * dx + (dy - 1) * (dy - 1) * 1.4 + dz * dz);
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (d < 2.9 && roll > (d > 2.2 ? 0.45f : 0.0f) && box.isInside(p) && replaceable(level.getBlockState(p))) {
                        set(level, box, p, leaves);
                    }
                }
            }
        }
        // trunk reaching up...
        for (int y = 1; y <= 8; y++) set(level, box, origin.above(y), Blocks.OAK_LOG.defaultBlockState());
        // ...into the clump of ground it should have grown from
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                float roll = r.nextFloat(), rootRoll = r.nextFloat();
                if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                set(level, box, origin.offset(dx, 9, dz), roll < 0.5f ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) set(level, box, origin.offset(dx, 10, dz), Blocks.GRASS_BLOCK.defaultBlockState());
                if ((dx != 0 || dz != 0) && rootRoll < 0.5f) set(level, box, origin.offset(dx, 8, dz), Blocks.HANGING_ROOTS.defaultBlockState());
            }
        }
    }

    // ------------------------------------------------------------------ glitched patch

    private enum Glitch {
        DESERT(Blocks.SAND, Blocks.SANDSTONE, 0.12f, new BlockState[]{Blocks.DEAD_BUSH.defaultBlockState()}, Blocks.SAND, Blocks.SANDSTONE, Blocks.CUT_SANDSTONE),
        SNOW(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, 0.5f, new BlockState[]{Blocks.SNOW.defaultBlockState()}, Blocks.ICE, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE),
        NETHER(Blocks.CRIMSON_NYLIUM, Blocks.NETHERRACK, 0.2f, new BlockState[]{Blocks.CRIMSON_ROOTS.defaultBlockState(), Blocks.CRIMSON_FUNGUS.defaultBlockState()},
                Blocks.NETHERRACK, Blocks.MAGMA_BLOCK, Blocks.GLOWSTONE),
        END(Blocks.END_STONE, Blocks.END_STONE, 0.05f, new BlockState[]{Blocks.END_ROD.defaultBlockState()}, Blocks.END_STONE, Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN),
        MUSHROOM(Blocks.MYCELIUM, Blocks.DIRT, 0.15f, new BlockState[]{Blocks.RED_MUSHROOM.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState()},
                Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM),
        BADLANDS(Blocks.RED_SAND, Blocks.TERRACOTTA, 0.1f, new BlockState[]{Blocks.DEAD_BUSH.defaultBlockState()},
                Blocks.ORANGE_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.YELLOW_TERRACOTTA),
        LEVEL_ZERO(null, null, 0.0f, new BlockState[0], null, null, null);

        final BlockState top, under;
        final float decorationChance;
        final BlockState[] decorations;
        final BlockState[] floaters;

        Glitch(net.minecraft.world.level.block.Block top, net.minecraft.world.level.block.Block under, float decorationChance,
               BlockState[] decorations, net.minecraft.world.level.block.Block... floaters) {
            this.top = top == null ? null : top.defaultBlockState();
            this.under = under == null ? null : under.defaultBlockState();
            this.decorationChance = decorationChance;
            this.decorations = decorations;
            this.floaters = new BlockState[floaters.length];
            for (int i = 0; i < floaters.length; i++) this.floaters[i] = floaters[i] == null ? null : floaters[i].defaultBlockState();
        }

        BlockState top() {
            return this == LEVEL_ZERO ? ModBlocks.BACKROOMS_CARPET.get().defaultBlockState() : top;
        }

        BlockState under() {
            return this == LEVEL_ZERO ? ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState() : under;
        }

        BlockState floater(int i) {
            if (this == LEVEL_ZERO) {
                return switch (i) {
                    case 0 -> ModBlocks.BACKROOMS_WALLPAPER.get().defaultBlockState();
                    case 1 -> ModBlocks.BACKROOMS_CEILING.get().defaultBlockState();
                    default -> ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState();
                };
            }
            return floaters[i];
        }
    }

    private void glitchedPatch(WorldGenLevel level, BoundingBox box, RandomSource r) {
        Glitch glitch = Glitch.values()[r.nextInt(Glitch.values().length)];
        int radius = 5 + r.nextInt(4);
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                float edgeNoise = r.nextFloat(), decorationRoll = r.nextFloat();
                int pick = r.nextInt(8);
                if (Math.hypot(dx, dz) > radius + edgeNoise * 2 - 1) continue;
                BlockPos column = origin.offset(dx, 0, dz);
                if (!box.isInside(column)) continue;
                BlockPos top = ground(level, column.getX(), column.getZ());
                if (!level.getFluidState(top.above()).isEmpty()) continue;
                set(level, box, top, glitch.top());
                set(level, box, top.below(), glitch.under());
                set(level, box, top.below(2), glitch.under());
                if (replaceable(level.getBlockState(top.above()))) set(level, box, top.above(), Blocks.AIR.defaultBlockState());
                if (glitch.decorations.length > 0 && decorationRoll < glitch.decorationChance) {
                    set(level, box, top.above(), glitch.decorations[pick % glitch.decorations.length]);
                }
            }
        }
        int floating = 4 + r.nextInt(4);
        for (int i = 0; i < floating; i++) {
            int dx = r.nextInt(2 * radius + 1) - radius, dz = r.nextInt(2 * radius + 1) - radius;
            int height = 3 + r.nextInt(6), pick = r.nextInt(3);
            BlockPos column = origin.offset(dx, 0, dz);
            if (box.isInside(column)) set(level, box, ground(level, column.getX(), column.getZ()).above(height), glitch.floater(pick));
        }
        // and, sometimes, right in the middle: something that really doesn't belong
        if (r.nextFloat() < 0.5f && box.isInside(origin)) {
            set(level, box, ground(level, origin.getX(), origin.getZ()),
                    ModBlocks.FLUORESCENT_LIGHT.get().defaultBlockState().setValue(FluorescentLightBlock.LIT, true));
        }
    }
}
