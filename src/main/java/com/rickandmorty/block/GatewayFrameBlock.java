package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An invisible piece of the gateway ring that gives it collision. The top-middle piece
 * also renders the upper half of the ring. Breaking any piece breaks the whole gateway.
 */
public class GatewayFrameBlock extends Block {
    public static final MapCodec<GatewayFrameBlock> CODEC = simpleCodec(GatewayFrameBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, GatewayStructure.PARTS.length - 1);

    public GatewayFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(WarpGatewayBlock.RED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, WarpGatewayBlock.RED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == GatewayStructure.TOP_PART ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return GatewayStructure.frameShape(state.getValue(FACING), state.getValue(PART));
    }

    private static BlockPos controller(BlockState state, BlockPos pos) {
        return GatewayStructure.controllerPos(pos, state.getValue(FACING), state.getValue(PART));
    }

    private static boolean isOurController(Level level, BlockState frame, BlockPos controller) {
        BlockState state = level.getBlockState(controller);
        return state.is(ModBlocks.WARP_GATEWAY.get()) && state.getValue(WarpGatewayBlock.FACING) == frame.getValue(FACING);
    }

    /** In creative, break the controller without dropping the gateway item. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos controller = controller(state, pos);
        if (!level.isClientSide && player.isCreative() && isOurController(level, state, controller)) {
            level.setBlock(controller, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Losing any piece (player, explosion...) breaks the controller, which drops the item and clears the rest. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos controller = controller(state, pos);
            if (isOurController(level, state, controller)) {
                level.destroyBlock(controller, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        return stack.getItem() instanceof PortalGunItem
                ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos controller = controller(state, pos);
        return isOurController(level, state, controller) ? WarpGatewayBlock.showStatus(level, controller, player) : InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.WARP_GATEWAY.get());
    }
}
