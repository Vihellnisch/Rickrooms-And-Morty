package com.rickandmorty.cronenberg;

import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A lumpy growth bulging out of the ground: overlapping blobs of tumor and flesh, eyes here and
 * there, pustules and teeth sprouting from its skin.
 */
public class TumorMoundFeature extends Feature<NoneFeatureConfiguration> {
    public TumorMoundFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBlockState(origin.below()).is(ModTags.FLESH_GROUND)) return false;
        int blobs = 2 + random.nextInt(3);
        for (int b = 0; b < blobs; b++) {
            BlockPos center = origin.offset(random.nextInt(5) - 2, random.nextInt(2) - 1, random.nextInt(5) - 2);
            float radius = 1.5f + random.nextFloat() * 1.8f;
            int r = (int) Math.ceil(radius);
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
                double dx = p.getX() - center.getX(), dy = (p.getY() - center.getY()) * 1.3, dz = p.getZ() - center.getZ();
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz) + random.nextFloat() * 0.6;
                if (d > radius) continue;
                BlockState there = level.getBlockState(p);
                if (!there.isAir() && !there.canBeReplaced() && !there.is(ModTags.FLESH_GROUND)) continue;
                float roll = random.nextFloat();
                BlockState state = roll < 0.08f ? ModBlocks.WATCHING_FLESH.get().defaultBlockState()
                        : roll < 0.7f ? ModBlocks.TUMOR.get().defaultBlockState() : ModBlocks.FLESH.get().defaultBlockState();
                level.setBlock(p.immutable(), state, 2);
            }
        }
        // things growing on it
        for (int i = 0; i < 10; i++) {
            BlockPos p = origin.offset(random.nextInt(9) - 4, 4, random.nextInt(9) - 4);
            while (p.getY() > origin.getY() - 3 && level.isEmptyBlock(p.below())) p = p.below();
            if (!level.isEmptyBlock(p) || !level.getBlockState(p.below()).is(ModTags.FLESH_GROUND)) continue;
            BlockState growth = random.nextFloat() < 0.55f ? ModBlocks.PUSTULE.get().defaultBlockState()
                    : random.nextFloat() < 0.5f ? ModBlocks.TOOTH_SPIKE.get().defaultBlockState() : ModBlocks.EYESTALK.get().defaultBlockState();
            level.setBlock(p, growth, 2);
        }
        return true;
    }
}
