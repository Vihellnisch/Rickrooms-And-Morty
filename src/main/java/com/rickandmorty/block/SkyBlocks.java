package com.rickandmorty.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The blocks of the v0.23 levels: Drop Limit's cloud, the Facade's air conditioners, Froopyland's turf, and the red EXIT signs of Run For Your Life. */
public final class SkyBlocks {
    private SkyBlocks() {
    }

    /** Froopyland's ground: soft, so nobody gets hurt. Land on it from any height and you bounce, and feel nothing. */
    public static class FroopyTurf extends Block {
        public FroopyTurf(Properties properties) {
            super(properties);
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
            entity.causeFallDamage(distance, 0.0f, level.damageSources().fall());
        }

        @Override
        public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
            Vec3 v = entity.getDeltaMovement();
            if (entity.isSuppressingBounce() || v.y >= -0.2) {
                super.updateEntityAfterFallOn(level, entity);
                return;
            }
            entity.setDeltaMovement(v.x, -v.y * 0.75, v.z);
        }
    }

    /** A red EXIT sign over a doorway, lighting the hall red. Thin, flat against the wall it hangs on. */
    public static class RedExitSign extends net.minecraft.world.level.block.HorizontalDirectionalBlock {
        public static final com.mojang.serialization.MapCodec<RedExitSign> CODEC = simpleCodec(RedExitSign::new);
        private static final java.util.Map<net.minecraft.core.Direction, VoxelShape> SHAPES = java.util.Map.of(
                net.minecraft.core.Direction.NORTH, Block.box(1, 4, 14, 15, 12, 16), net.minecraft.core.Direction.SOUTH, Block.box(1, 4, 0, 15, 12, 2),
                net.minecraft.core.Direction.WEST, Block.box(14, 4, 1, 16, 12, 15), net.minecraft.core.Direction.EAST, Block.box(0, 4, 1, 2, 12, 15));

        public RedExitSign(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
        }

        @Override
        protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPES.get(state.getValue(FACING));
        }
    }

    /** A window air-conditioner box, bolted to the outside of a wall; its grille faces out. */
    public static class AirConditioner extends net.minecraft.world.level.block.HorizontalDirectionalBlock {
        public static final com.mojang.serialization.MapCodec<AirConditioner> CODEC = simpleCodec(AirConditioner::new);

        public AirConditioner(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
        }

        @Override
        protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
    }

    /**
     * A block of cloud: it looks solid, but you sink into it, slowly, and through it, and out the
     * bottom of the level. Opaque (so a sea of it is cheap to draw), but with nothing to stand on.
     */
    public static class Cloud extends Block {
        public Cloud(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            entity.makeStuckInBlock(state, new Vec3(0.85, 0.45, 0.85));
            entity.resetFallDistance();
        }
    }
}
