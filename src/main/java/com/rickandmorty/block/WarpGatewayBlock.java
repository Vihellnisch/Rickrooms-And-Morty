package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.block.entity.WarpGatewayBlockEntity;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModBlockEntities;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * The bottom-middle block of a Warp Gateway. It owns the block entity (the link to the
 * other gateway), renders the platform and lower ring, and places/removes the frame blocks.
 */
public class WarpGatewayBlock extends BaseEntityBlock {
    public static final MapCodec<WarpGatewayBlock> CODEC = simpleCodec(WarpGatewayBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** An evil gateway that has sprung: its green lights turn red (the frame blocks follow it). */
    public static final BooleanProperty RED = BooleanProperty.create("red");
    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.36f, 1.0f, 0.23f), 1.0f);

    public WarpGatewayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false).setValue(RED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, RED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return GatewayStructure.controllerShape(state.getValue(FACING));
    }

    // ------------------------------------------------------------------ placing & breaking

    /** The ring faces the player placing it. Refuses to place if the 3x3 area isn't free. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (int i = 0; i < GatewayStructure.PARTS.length; i++) {
            BlockPos part = GatewayStructure.partPos(pos, facing, i);
            if (level.isOutsideBuildHeight(part) || !level.getBlockState(part).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        Direction facing = state.getValue(FACING);
        BlockState frame = ModBlocks.GATEWAY_FRAME.get().defaultBlockState().setValue(GatewayFrameBlock.FACING, facing);
        for (int i = 0; i < GatewayStructure.PARTS.length; i++) {
            level.setBlock(GatewayStructure.partPos(pos, facing, i), frame.setValue(GatewayFrameBlock.PART, i), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof WarpGatewayBlockEntity gateway) {
                gateway.breakLink(serverLevel.getServer());
            }
            Direction facing = state.getValue(FACING);
            for (int i = 0; i < GatewayStructure.PARTS.length; i++) {
                BlockPos partPos = GatewayStructure.partPos(pos, facing, i);
                BlockState part = level.getBlockState(partPos);
                if (part.is(ModBlocks.GATEWAY_FRAME.get()) && part.getValue(GatewayFrameBlock.FACING) == facing
                        && part.getValue(GatewayFrameBlock.PART) == i) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ------------------------------------------------------------------ interaction

    /** Let the portal gun handle clicks itself (it links gateways). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        return stack.getItem() instanceof PortalGunItem
                ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Empty hand: tell the player where this gateway leads. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return showStatus(level, pos, player);
    }

    static InteractionResult showStatus(Level level, BlockPos controller, Player player) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(controller) instanceof WarpGatewayBlockEntity gateway) {
            // an evil gateway looks like any other, except to whoever armed it
            player.displayClientMessage(gateway.isEvil() && gateway.isOwner(player)
                    ? net.minecraft.network.chat.Component.translatable("message.rickrooms.gateway.armed_status").withStyle(net.minecraft.ChatFormatting.DARK_RED)
                    : gateway.describeLink(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------ block entity

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarpGatewayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WARP_GATEWAY.get(), WarpGatewayBlockEntity::serverTick);
    }

    // ------------------------------------------------------------------ effects

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        Direction facing = state.getValue(FACING);
        for (int i = 0; i < 4; i++) {
            Vec3 p = GatewayStructure.rimPoint(pos, facing, random.nextDouble() * Math.PI * 2);
            level.addParticle(GREEN_DUST, p.x, p.y, p.z, 0, 0, 0);
        }
        if (random.nextInt(40) == 0) {
            Vec3 c = GatewayStructure.toWorld(pos, facing, GatewayStructure.RING_X, GatewayStructure.RING_Y, 0.5);
            level.playLocalSound(c.x, c.y, c.z, ModSounds.GATEWAY_HUM.get(), SoundSource.BLOCKS, 0.6f, 1.0f, false);
        }
    }
}
