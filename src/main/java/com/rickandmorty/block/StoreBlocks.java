package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.block.entity.FoodCounterBlockEntity;
import com.rickandmorty.block.entity.StockCrateBlockEntity;
import com.rickandmorty.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The furniture and fittings of the Infinite IKEA: sofas (that join up and take a dye), chairs,
 * tables, lamps, shelves, signs, pallets, the stock crates that fill up again every morning and
 * the restaurant's food counters. Shapes are given for a block facing north and turned to suit.
 */
public final class StoreBlocks {
    /** Grey, blue, red, green, yellow, beige. */
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 5);
    public static final EnumProperty<SofaPart> PART = EnumProperty.create("part", SofaPart.class);

    private StoreBlocks() {
    }

    public enum SofaPart implements StringRepresentable {
        SINGLE, LEFT, MIDDLE, RIGHT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    /** Boxes (in sixteenths, for facing north) turned to each horizontal facing. */
    static Map<Direction, VoxelShape> shapes(double[]... boxes) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
                double[] r = switch (facing) {
                    case EAST -> new double[]{16 - z1, x0, 16 - z0, x1};
                    case SOUTH -> new double[]{16 - x1, 16 - z1, 16 - x0, 16 - z0};
                    case WEST -> new double[]{z0, 16 - x1, z1, 16 - x0};
                    default -> new double[]{x0, z0, x1, z1};
                };
                shape = Shapes.or(shape, Block.box(r[0], b[1], r[1], r[2], b[4], r[3]));
            }
            map.put(facing, shape.optimize());
        }
        return map;
    }

    /** Sit down: an invisible seat at this height (blocks above the floor of the block) for the player to ride. */
    static InteractionResult sit(Level level, BlockPos pos, Player player, double height) {
        if (player.isSecondaryUseActive() || player.isPassenger()) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            if (!server.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()) return InteractionResult.CONSUME;
            SeatEntity seat = new SeatEntity(server, pos, height);
            server.addFreshEntity(seat);
            player.startRiding(seat);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------ simple facing blocks

    /** A full block with a front: kitchen cabinets, cube shelves, checkout counters. */
    public static class Facing extends HorizontalDirectionalBlock {
        public static final MapCodec<Facing> CODEC = simpleCodec(Facing::new);

        public Facing(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
    }

    /** A facing block with a smaller shape: chairs, lamps on the partitions, the hanging EXIT signs. */
    public static class Shaped extends Facing {
        private final Map<Direction, VoxelShape> shape;
        private final boolean solid;

        public Shaped(Properties properties, boolean solid, double[]... boxes) {
            super(properties);
            this.shape = shapes(boxes);
            this.solid = solid;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shape.get(state.getValue(FACING));
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return solid ? shape.get(state.getValue(FACING)) : Shapes.empty();
        }
    }

    /** Something with a front that you can sit on: chairs, and the toilets in the bathroom showrooms. */
    public static class Sittable extends Shaped {
        private final double seat;

        public Sittable(Properties properties, double seat, double[]... boxes) {
            super(properties, true, boxes);
            this.seat = seat;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return sit(level, pos, player, seat);
        }
    }

    /** A chair: sit on it. */
    public static class Chair extends Sittable {
        public Chair(Properties properties) {
            super(properties, 0.45, new double[]{3, 0, 3, 13, 9, 13}, new double[]{3, 9, 11, 13, 20, 13});
        }
    }

    /** A hanging department sign: an icon for what's in that department. */
    public static class DepartmentSign extends Shaped {
        public static final IntegerProperty ICON = IntegerProperty.create("icon", 0, 17);

        public DepartmentSign(Properties properties) {
            super(properties, false, new double[]{0, 2, 7, 16, 14, 9});
            registerDefaultState(defaultBlockState().setValue(ICON, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ICON);
        }
    }

    /** The hanging EXIT sign. Its arrow points to the left of the way its front faces (see {@link #arrow}). */
    public static class ExitSign extends Shaped {
        public ExitSign(Properties properties) {
            super(properties, false, new double[]{0, 3, 7, 16, 13, 9});
        }

        /** Which way the arrow points. */
        public static Direction arrow(BlockState state) {
            return state.getValue(FACING).getCounterClockWise();
        }

        /** The sign whose arrow points {@code way}. */
        public static BlockState pointing(BlockState sign, Direction way) {
            return sign.setValue(FACING, way.getClockWise());
        }
    }

    // ------------------------------------------------------------------ the sofa

    /** A sofa piece. Pieces side by side facing the same way join into one long sofa; use a dye on it to recolour it. */
    public static class Sofa extends HorizontalDirectionalBlock {
        public static final MapCodec<Sofa> CODEC = simpleCodec(Sofa::new);
        private static final Map<Direction, VoxelShape> SEAT = shapes(new double[]{0, 0, 0, 16, 8, 16}, new double[]{0, 8, 11, 16, 16, 16});
        private static final Map<Direction, VoxelShape> LEFT_ARM = shapes(new double[]{0, 8, 0, 3, 12, 16});
        private static final Map<Direction, VoxelShape> RIGHT_ARM = shapes(new double[]{13, 8, 0, 16, 12, 16});

        public Sofa(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, SofaPart.SINGLE).setValue(COLOR, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, PART, COLOR);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            Direction facing = state.getValue(FACING);
            VoxelShape shape = SEAT.get(facing);
            SofaPart part = state.getValue(PART);
            // facing north, the sitter's left is west, at low x
            if (part == SofaPart.SINGLE || part == SofaPart.LEFT) shape = Shapes.or(shape, LEFT_ARM.get(facing));
            if (part == SofaPart.SINGLE || part == SofaPart.RIGHT) shape = Shapes.or(shape, RIGHT_ARM.get(facing));
            return shape;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
            return state.setValue(PART, part(state, context.getLevel(), context.getClickedPos()));
        }

        /** Joined to the pieces either side of it? */
        public static SofaPart part(BlockState state, BlockGetter level, BlockPos pos) {
            Direction facing = state.getValue(FACING);
            boolean left = joins(level.getBlockState(pos.relative(facing.getCounterClockWise())), facing);
            boolean right = joins(level.getBlockState(pos.relative(facing.getClockWise())), facing);
            return left && right ? SofaPart.MIDDLE : left ? SofaPart.RIGHT : right ? SofaPart.LEFT : SofaPart.SINGLE;
        }

        private static boolean joins(BlockState other, Direction facing) {
            return other.getBlock() instanceof Sofa && other.getValue(FACING) == facing;
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            return direction.getAxis().isHorizontal() ? state.setValue(PART, part(state, level, pos)) : state;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return sit(level, pos, player, 0.4);
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!(stack.getItem() instanceof DyeItem dye)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            int color = color(dye.getDyeColor());
            if (state.getValue(COLOR) == color) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            level.setBlock(pos, state.setValue(COLOR, color), Block.UPDATE_ALL);
            level.playSound(player, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        static int color(DyeColor dye) {
            return switch (dye) {
                case BLUE, LIGHT_BLUE, CYAN -> 1;
                case RED, PINK, MAGENTA -> 2;
                case GREEN, LIME -> 3;
                case YELLOW, ORANGE -> 4;
                case WHITE, BROWN -> 5;
                default -> 0;
            };
        }
    }

    // ------------------------------------------------------------------ simple shaped blocks

    /** A block with one fixed shape: tables, pallets, flat-pack boxes, paper lamps. */
    public static class Fixed extends Block {
        public static final MapCodec<Fixed> CODEC = simpleCodec(p -> new Fixed(p, Shapes.block()));
        private final VoxelShape shape;

        public Fixed(Properties properties, VoxelShape shape) {
            super(properties);
            this.shape = shape;
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shape;
        }
    }

    // ------------------------------------------------------------------ stock crates and food counters

    /** A cardboard crate of stock. Whatever you take, it's full again the next morning. */
    public static class StockCrate extends BaseEntityBlock {
        public static final MapCodec<StockCrate> CODEC = simpleCodec(StockCrate::new);

        public StockCrate(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StockCrateBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof StockCrateBlockEntity crate) {
                crate.restock(server);
                player.openMenu(crate);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            Containers.dropContentsOnDestroy(state, newState, level, pos);
            super.onRemove(state, level, pos, newState, moved);
        }
    }

    /** The restaurant's hot counter: a plate of meatballs or a juice each, until it's sold out for the day. */
    public static class FoodCounter extends BaseEntityBlock {
        public static final MapCodec<FoodCounter> CODEC = simpleCodec(FoodCounter::new);

        public FoodCounter(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HorizontalDirectionalBlock.FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new FoodCounterBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof FoodCounterBlockEntity counter) {
                counter.serve(server, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
