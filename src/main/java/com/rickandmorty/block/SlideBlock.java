package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Yellow plastic playground slide. Shaped like stairs, slippery as blue ice, and it shoves
 * anything standing on it downhill (away from the way the stairs climb). Walking up one is hard.
 */
public class SlideBlock extends StairBlock {
    public static final MapCodec<SlideBlock> CODEC = simpleCodec(SlideBlock::new);
    private static final double PUSH = 0.06;
    private static final double MAX_SPEED = 0.6;

    public SlideBlock(Properties properties) {
        super(Blocks.YELLOW_CONCRETE.defaultBlockState(), properties);
    }

    @Override
    public MapCodec<? extends StairBlock> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        Direction down = state.getValue(FACING).getOpposite();
        Vec3 motion = entity.getDeltaMovement();
        double along = motion.x * down.getStepX() + motion.z * down.getStepZ();
        if (along < MAX_SPEED && !entity.isShiftKeyDown()) {
            entity.setDeltaMovement(motion.add(down.getStepX() * PUSH, 0, down.getStepZ() * PUSH));
        }
        super.stepOn(level, pos, state, entity);
    }
}
