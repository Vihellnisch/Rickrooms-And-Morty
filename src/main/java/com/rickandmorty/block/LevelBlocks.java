package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.worldgen.level.LevelTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The small special blocks of the new Backrooms levels. */
public final class LevelBlocks {
    private LevelBlocks() {
    }

    /**
     * The inside of the hole in the wall on Level 0: pure black, and not really there. Walk into it
     * and you noclip somewhere else.
     */
    public static class NoclipVoid extends Block {
        public static final MapCodec<NoclipVoid> CODEC = simpleCodec(NoclipVoid::new);

        public NoclipVoid(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (entity instanceof ServerPlayer player) LevelTravel.throughTheHole(player);
        }
    }

    /** A cable dangling from the ceiling; {@code tip} is the end with the plug on it. */
    public static class HangingCable extends Block {
        public static final MapCodec<HangingCable> CODEC = simpleCodec(HangingCable::new);
        public static final BooleanProperty TIP = BooleanProperty.create("tip");
        private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

        public HangingCable(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(TIP, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TIP);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            BlockState above = level.getBlockState(pos.above());
            return above.is(this) || above.isFaceSturdy(level, pos.above(), Direction.DOWN);
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            if (direction == Direction.UP && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
            if (direction == Direction.DOWN) return state.setValue(TIP, !neighbor.is(this));
            return state;
        }
    }

    /** A thin pipe running along the ceiling. */
    public static class Pipe extends RotatedPillarBlock {
        public static final MapCodec<Pipe> CODEC = simpleCodec(Pipe::new);
        private static final VoxelShape X = Block.box(0, 5, 5, 16, 11, 11);
        private static final VoxelShape Y = Block.box(5, 0, 5, 11, 16, 11);
        private static final VoxelShape Z = Block.box(5, 5, 0, 11, 11, 16);

        public Pipe(Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<? extends RotatedPillarBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return switch (state.getValue(AXIS)) {
                case X -> X;
                case Y -> Y;
                case Z -> Z;
            };
        }
    }

    /** Two-block-tall dead corn. Grows on nothing, dies on nothing, just stands there. */
    public static class DriedCorn extends DoublePlantBlock {
        public static final MapCodec<DriedCorn> CODEC = simpleCodec(DriedCorn::new);

        public DriedCorn(Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<? extends DoublePlantBlock> codec() {
            return CODEC;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
        }
    }
}
