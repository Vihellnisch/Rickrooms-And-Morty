package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The drain at the very bottom of the Poolrooms' deepest pool. Sink all the way down and touch
 * it, and the water takes you somewhere else: the Glowstone Cavern, far below.
 */
public class PoolDrainBlock extends Block {
    public static final MapCodec<PoolDrainBlock> CODEC = simpleCodec(PoolDrainBlock::new);

    public PoolDrainBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof ServerPlayer player && !player.isSpectator()) LevelTravel.throughTheDrain(player);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // a thin stream of bubbles marks it from above
        if (random.nextInt(3) == 0 && level.getFluidState(pos.above()).isSource()) {
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0.0, 0.04, 0.0);
        }
    }

    /** Where the drain leads. */
    public static BackroomsLevel destination() {
        return BackroomsLevel.CAVERN;
    }
}
