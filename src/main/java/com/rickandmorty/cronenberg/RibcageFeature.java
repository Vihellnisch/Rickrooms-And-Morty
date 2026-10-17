package com.rickandmorty.cronenberg;

import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The "trees" of the Bone Forest: either a spine standing upright with ribs curling out and up
 * from it like branches, or the ribcage of something enormous lying half-buried on its side.
 */
public class RibcageFeature extends Feature<NoneFeatureConfiguration> {
    public RibcageFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBlockState(origin.below()).is(ModTags.FLESH_GROUND)) return false;
        if (random.nextFloat() < 0.6f) boneTree(level, origin, random);
        else carcass(level, origin, random);
        return true;
    }

    private static BlockState bone(Direction.Axis axis) {
        return ModBlocks.CALCIFIED_BONE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static void put(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockState there = level.getBlockState(pos);
        if (there.isAir() || there.canBeReplaced() || there.is(ModTags.FLESH_GROUND)) level.setBlock(pos, state, 2);
    }

    /** A vertebral column with ribs sprouting in pairs, bending up at the tips; flesh clinging to the top. */
    private static void boneTree(WorldGenLevel level, BlockPos origin, RandomSource random) {
        int height = 6 + random.nextInt(7);
        for (int y = -1; y < height; y++) put(level, origin.above(y), bone(Direction.Axis.Y));
        float twist = random.nextFloat() * Mth.TWO_PI;
        for (int y = 3; y < height - 1; y += 2) {
            int length = 2 + random.nextInt(3) + (height - y) / 3;
            for (int side = 0; side < 2; side++) {
                float angle = twist + side * Mth.PI + y * 0.6f;
                double dx = Mth.cos(angle), dz = Mth.sin(angle);
                for (int i = 1; i <= length; i++) {
                    int lift = i * i / (length + 1);                 // ribs curve upwards
                    BlockPos p = origin.offset((int) Math.round(dx * i), y + lift, (int) Math.round(dz * i));
                    put(level, p, bone(Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z));
                    if (i == length && random.nextFloat() < 0.35f) put(level, p.above(), ModBlocks.EYESTALK.get().defaultBlockState());
                }
            }
        }
        // a lump of flesh (and something watching) on top
        BlockPos top = origin.above(height);
        for (BlockPos p : BlockPos.betweenClosed(top.offset(-1, 0, -1), top.offset(1, 1, 1))) {
            if (random.nextFloat() < 0.7f) put(level, p, random.nextFloat() < 0.2f ? ModBlocks.WATCHING_FLESH.get().defaultBlockState()
                    : random.nextFloat() < 0.5f ? ModBlocks.TUMOR.get().defaultBlockState() : ModBlocks.FLESH.get().defaultBlockState());
        }
    }

    /** A giant ribcage lying on the ground: a spine along the ground and arched ribs over it. */
    private static void carcass(WorldGenLevel level, BlockPos origin, RandomSource random) {
        boolean alongX = random.nextBoolean();
        int length = 7 + random.nextInt(8);
        int radius = 3 + random.nextInt(3);
        Direction.Axis spineAxis = alongX ? Direction.Axis.X : Direction.Axis.Z;
        for (int i = 0; i < length; i++) {
            BlockPos spine = alongX ? origin.offset(i, 0, 0) : origin.offset(0, 0, i);
            put(level, spine, bone(spineAxis));
            put(level, spine.below(), bone(spineAxis));
            if (i % 2 != 0 || i == 0 || i >= length - 1) continue;
            int r = radius - Math.abs(i - length / 2) / 3;
            for (int step = 0; step <= 12; step++) {
                double t = Math.PI * step / 12.0;                   // a half circle over the spine
                int side = (int) Math.round(Math.cos(t) * r);
                int up = (int) Math.round(Math.sin(t) * (r + 1));
                if (random.nextFloat() < 0.12f && step > 2 && step < 10) continue;   // broken ribs
                BlockPos p = alongX ? spine.offset(0, up, side) : spine.offset(side, up, 0);
                put(level, p, bone(Direction.Axis.Y));
            }
        }
        // the skull at one end, with its eyes still there
        BlockPos skull = alongX ? origin.offset(-2, 0, 0) : origin.offset(0, 0, -2);
        for (BlockPos p : BlockPos.betweenClosed(skull.offset(-1, 0, -1), skull.offset(1, 2, 1))) {
            put(level, p, ModBlocks.CALCIFIED_BONE.get().defaultBlockState());
        }
        put(level, alongX ? skull.offset(-2, 1, 0) : skull.offset(0, 1, -2), ModBlocks.WATCHING_FLESH.get().defaultBlockState());
    }
}
