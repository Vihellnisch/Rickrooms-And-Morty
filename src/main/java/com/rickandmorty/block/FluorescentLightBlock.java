package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A humming ceiling light.
 * <ul>
 *   <li>Lit lights occasionally flicker off for a moment.</li>
 *   <li>A redstone signal switches it off until the signal is gone (no flickering meanwhile).</li>
 *   <li>Broken lights (placed by the Backrooms generator) never turn on.</li>
 * </ul>
 */
public class FluorescentLightBlock extends Block {
    public static final MapCodec<FluorescentLightBlock> CODEC = simpleCodec(FluorescentLightBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    public FluorescentLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, true).setValue(POWERED, false).setValue(BROKEN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, POWERED, BROKEN);
    }

    public static BlockState broken(BlockState state) {
        return state.setValue(BROKEN, true).setValue(LIT, false);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return defaultBlockState().setValue(POWERED, powered).setValue(LIT, !powered);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState updated = state.setValue(POWERED, powered);
            if (!state.getValue(BROKEN)) {
                updated = updated.setValue(LIT, !powered);
            }
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    private static boolean canFlicker(BlockState state) {
        return state.getValue(LIT) && !state.getValue(POWERED) && !state.getValue(BROKEN);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return canFlicker(state);
    }

    /** Flicker off briefly; {@link #tick} turns it back on. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 2 + random.nextInt(8));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) && !state.getValue(POWERED) && !state.getValue(BROKEN)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (canFlicker(state) && random.nextInt(120) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ModSounds.LIGHT_BUZZ.get(),
                    SoundSource.BLOCKS, 0.25f, 0.9f + random.nextFloat() * 0.2f, false);
        }
    }
}
