package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The stuff the monoliths are made of. Unbreakable, and every so often, from somewhere inside, a low hum. */
public class MonolithBlock extends Block {
    public static final MapCodec<MonolithBlock> CODEC = simpleCodec(MonolithBlock::new);

    public MonolithBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // there are thousands of these in a monolith, so each one hums very rarely
        if (random.nextInt(12000) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.MONOLITH_HUM.get(), SoundSource.AMBIENT,
                    1.2f, 0.45f + random.nextFloat() * 0.15f, false);
        }
    }
}
