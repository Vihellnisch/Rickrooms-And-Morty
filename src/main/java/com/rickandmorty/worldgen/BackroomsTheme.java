package com.rickandmorty.worldgen;

import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import org.jetbrains.annotations.Nullable;

/**
 * Patches where the Backrooms tries (and slightly fails) to imitate a real biome. The maze
 * layout stays the same; only the materials change, blended with leftover wallpaper at the edges.
 * Palettes repeat entries to weight them.
 */
public enum BackroomsTheme {
    PLAINS(
            blocks(Blocks.GRASS_BLOCK),
            blocks(Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG),
            states(ceilingTile(), leaves(Blocks.OAK_LEAVES)),
            null, 0.0f, 0.35f,
            (floor, h) -> floor.is(Blocks.GRASS_BLOCK) ? pickPlant(h, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
                    Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.TALL_GRASS) : null,
            null),
    JUNGLE(
            blocks(Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.PODZOL),
            states(Blocks.JUNGLE_LOG.defaultBlockState(), leaves(Blocks.JUNGLE_LEAVES), leaves(Blocks.JUNGLE_LEAVES),
                    Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.JUNGLE_PLANKS.defaultBlockState()),
            states(leaves(Blocks.JUNGLE_LEAVES), Blocks.MOSS_BLOCK.defaultBlockState()),
            Blocks.OCHRE_FROGLIGHT.defaultBlockState(), 0.0f, 0.5f,
            (floor, h) -> floor.is(BlockTags.DIRT) ? pickPlant(h, Blocks.FERN, Blocks.FERN, Blocks.LARGE_FERN, Blocks.SHORT_GRASS, Blocks.TALL_GRASS) : null,
            (ceiling, h) -> Blocks.VINE.defaultBlockState().setValue(VineBlock.UP, true)),
    DESERT(
            blocks(Blocks.SAND, Blocks.SAND, Blocks.SANDSTONE),
            blocks(Blocks.SANDSTONE, Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CHISELED_SANDSTONE),
            states(ceilingTile(), Blocks.SMOOTH_SANDSTONE.defaultBlockState()),
            null, 0.1f, 0.2f,
            (floor, h) -> floor.is(Blocks.SAND) ? new BlockState[]{Blocks.DEAD_BUSH.defaultBlockState()} : null,
            null),
    SNOWY(
            blocks(Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE),
            blocks(Blocks.PACKED_ICE, Blocks.PACKED_ICE, Blocks.SNOW_BLOCK, Blocks.SPRUCE_PLANKS, Blocks.BLUE_ICE),
            blocks(Blocks.PACKED_ICE, Blocks.SNOW_BLOCK),
            Blocks.SEA_LANTERN.defaultBlockState(), 0.0f, 0.6f,
            (floor, h) -> new BlockState[]{Blocks.SNOW.defaultBlockState()},
            null),
    NETHER(
            blocks(Blocks.NETHERRACK, Blocks.NETHERRACK, Blocks.CRIMSON_NYLIUM, Blocks.CRIMSON_NYLIUM, Blocks.SOUL_SOIL, Blocks.MAGMA_BLOCK),
            blocks(Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.CRIMSON_HYPHAE),
            blocks(Blocks.NETHERRACK, Blocks.NETHER_WART_BLOCK),
            Blocks.GLOWSTONE.defaultBlockState(), 0.0f, 0.4f,
            (floor, h) -> floor.is(Blocks.CRIMSON_NYLIUM) ? pickPlant(h, Blocks.CRIMSON_ROOTS, Blocks.CRIMSON_ROOTS, Blocks.CRIMSON_FUNGUS) : null,
            (ceiling, h) -> Blocks.WEEPING_VINES.defaultBlockState()),
    END(
            blocks(Blocks.END_STONE),
            blocks(Blocks.END_STONE_BRICKS, Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.OBSIDIAN),
            blocks(Blocks.END_STONE, Blocks.PURPUR_BLOCK),
            Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), 0.0f, 0.12f,
            (floor, h) -> new BlockState[]{Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP)},
            null),
    MUSHROOM(
            blocks(Blocks.MYCELIUM),
            blocks(Blocks.MUSHROOM_STEM, Blocks.MUSHROOM_STEM, Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK),
            blocks(Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK),
            Blocks.SHROOMLIGHT.defaultBlockState(), 0.0f, 0.4f,
            (floor, h) -> pickPlant(h, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM),
            null),
    CHERRY(
            blocks(Blocks.GRASS_BLOCK),
            states(Blocks.CHERRY_PLANKS.defaultBlockState(), Blocks.CHERRY_PLANKS.defaultBlockState(), Blocks.CHERRY_LOG.defaultBlockState(),
                    leaves(Blocks.CHERRY_LEAVES)),
            states(ceilingTile(), leaves(Blocks.CHERRY_LEAVES)),
            null, 0.0f, 0.55f,
            (floor, h) -> floor.is(Blocks.GRASS_BLOCK) ? new BlockState[]{Blocks.PINK_PETALS.defaultBlockState()
                    .setValue(PinkPetalsBlock.AMOUNT, 1 + (int) Math.floorMod(h, 4L))
                    .setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue((int) Math.floorMod(h >>> 8, 4L)))} : null,
            null),
    CAVE(
            blocks(Blocks.STONE, Blocks.STONE, Blocks.COBBLESTONE, Blocks.GRAVEL),
            blocks(Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE,
                    Blocks.COAL_ORE, Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.GOLD_ORE, Blocks.STONE, Blocks.STONE,
                    Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.STONE, Blocks.DIAMOND_ORE),
            blocks(Blocks.STONE),
            null, 0.45f, 0.12f,
            (floor, h) -> new BlockState[]{Blocks.COBWEB.defaultBlockState()},
            (ceiling, h) -> Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                    .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP)),
    OCEAN(
            // flooded rooms: a knee-deep layer of water with seagrass
            blocks(Blocks.WATER, Blocks.WATER, Blocks.WATER, Blocks.SEAGRASS, Blocks.SAND),
            blocks(Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE),
            states(ceilingTile(), Blocks.PRISMARINE_BRICKS.defaultBlockState()),
            Blocks.SEA_LANTERN.defaultBlockState(), 0.0f, 0.0f,
            null, null),
    DEEP_DARK(
            blocks(Blocks.SCULK, Blocks.SCULK, Blocks.DEEPSLATE),
            blocks(Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.COBBLED_DEEPSLATE, Blocks.SCULK),
            blocks(Blocks.DEEPSLATE, Blocks.SCULK),
            Blocks.SCULK_CATALYST.defaultBlockState(), 0.5f, 0.0f,
            null, null);

    /** Places a floor decoration: [block at floor+1] or [lower, upper] for tall plants. Null = nothing. */
    @FunctionalInterface
    public interface FloorDecoration {
        BlockState @Nullable [] place(BlockState floor, long hash);
    }

    /** Places something hanging from the ceiling. Null = nothing. */
    @FunctionalInterface
    public interface CeilingDecoration {
        @Nullable BlockState place(BlockState ceiling, long hash);
    }

    final BlockState[] floors;
    final BlockState[] walls;
    final BlockState[] ceilings;
    /** Replaces the fluorescent lights; null keeps (fluorescent) lights. */
    final @Nullable BlockState light;
    /** Extra chance for kept fluorescent lights to be broken. */
    final float extraBroken;
    final float decorationChance;
    final @Nullable FloorDecoration floorDecoration;
    final @Nullable CeilingDecoration ceilingDecoration;

    BackroomsTheme(BlockState[] floors, BlockState[] walls, BlockState[] ceilings, @Nullable BlockState light, float extraBroken,
                   float decorationChance, @Nullable FloorDecoration floorDecoration, @Nullable CeilingDecoration ceilingDecoration) {
        this.floors = floors;
        this.walls = walls;
        this.ceilings = ceilings;
        this.light = light;
        this.extraBroken = extraBroken;
        this.decorationChance = decorationChance;
        this.floorDecoration = floorDecoration;
        this.ceilingDecoration = ceilingDecoration;
    }

    static BlockState pick(BlockState[] palette, long hash) {
        return palette[(int) Math.floorMod(hash, (long) palette.length)];
    }

    private static BlockState[] blocks(Block... blocks) {
        BlockState[] states = new BlockState[blocks.length];
        for (int i = 0; i < blocks.length; i++) states[i] = blocks[i].defaultBlockState();
        return states;
    }

    private static BlockState[] states(BlockState... states) {
        return states;
    }

    /** Placeholder for "keep the normal ceiling tile" (resolved by the layout, since it's a mod block). */
    private static BlockState ceilingTile() {
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState leaves(Block leaves) {
        return leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** Picks a plant; double-tall plants return both halves. */
    private static BlockState[] pickPlant(long hash, Block... plants) {
        Block plant = plants[(int) Math.floorMod(hash, (long) plants.length)];
        if (plant instanceof DoublePlantBlock) {
            return new BlockState[]{
                    plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER),
                    plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)};
        }
        return new BlockState[]{plant.defaultBlockState()};
    }
}
