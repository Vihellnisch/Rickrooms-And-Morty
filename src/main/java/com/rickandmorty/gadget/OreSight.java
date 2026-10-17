package com.rickandmorty.gadget;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

/** What the Interdimensional Goggles pick out: every ore nearby, and a colour for each kind. */
public final class OreSight {
    public static final int RADIUS = 16;

    public record Ore(BlockPos pos, int color) {
    }

    private OreSight() {
    }

    /** The ore's colour, or -1 if it isn't one. */
    public static int color(BlockState state) {
        if (state.is(BlockTags.DIAMOND_ORES)) return 0x5CF2F2;
        if (state.is(BlockTags.EMERALD_ORES)) return 0x3AE070;
        if (state.is(BlockTags.GOLD_ORES)) return 0xFFD23A;
        if (state.is(BlockTags.IRON_ORES)) return 0xE8B898;
        if (state.is(BlockTags.REDSTONE_ORES)) return 0xFF3A3A;
        if (state.is(BlockTags.LAPIS_ORES)) return 0x3A6CFF;
        if (state.is(BlockTags.COPPER_ORES)) return 0xFF8A3A;
        if (state.is(BlockTags.COAL_ORES)) return 0x9A9A9A;
        if (state.is(Blocks.ANCIENT_DEBRIS)) return 0xB06A4A;
        if (state.is(Blocks.NETHER_QUARTZ_ORE)) return 0xF2F2F2;
        if (state.is(Tags.Blocks.ORES)) return 0xFF6AE0;
        return -1;
    }

    /** Every ore within {@code radius} of {@code center}. */
    public static List<Ore> scan(BlockGetter level, BlockPos center, int radius) {
        List<Ore> found = new ArrayList<>();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    at.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    int color = color(level.getBlockState(at));
                    if (color >= 0) found.add(new Ore(at.immutable(), color));
                }
            }
        }
        return found;
    }
}
