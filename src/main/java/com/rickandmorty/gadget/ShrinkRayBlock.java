package com.rickandmorty.gadget;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModEffects;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Rick's Shrink Ray: a wheeled base, a jointed arm, and an emitter hanging over the block in front
 * of it. Stand under the emitter and press the button (use it, or give it a redstone pulse): you
 * shrink to half size for a few minutes. With a Growth Lens fitted it can make you twice the size
 * instead; sneak-use it to switch. Firing it again undoes it.
 */
public class ShrinkRayBlock extends HorizontalDirectionalBlock implements net.minecraft.world.level.block.EntityBlock {
    public static final MapCodec<ShrinkRayBlock> CODEC = simpleCodec(ShrinkRayBlock::new);
    public static final BooleanProperty UPGRADED = BooleanProperty.create("upgraded");
    public static final BooleanProperty GROW = BooleanProperty.create("grow");
    public static final BooleanProperty COOLING = BooleanProperty.create("cooling");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final int DURATION = 3600;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0.5, 0, 0.5, 15.5, 5, 15.5), Block.box(6.5, 5, 9, 9.5, 16, 15));
    private static final DustParticleOptions SHRINK_BEAM = new DustParticleOptions(new Vector3f(0.45f, 1.0f, 0.3f), 1.1f);
    private static final DustParticleOptions GROW_BEAM = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.15f), 1.1f);

    public ShrinkRayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(UPGRADED, false).setValue(GROW, false)
                .setValue(COOLING, false).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, UPGRADED, GROW, COOLING, POWERED);
    }

    /** It points the way you're looking when you place it: the emitter hangs over the block in front. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShrinkRayBlockEntity(pos, state);
    }

    /** Drawn by its block entity renderer, at full size. */
    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Where it fires: the block in front, from the floor up. */
    public static AABB target(BlockPos pos, BlockState state) {
        BlockPos front = pos.relative(state.getValue(FACING));
        return new AABB(front).expandTowards(0, 1.9, 0).inflate(0.15, 0, 0.15);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) {
            if (!state.getValue(UPGRADED)) {
                player.displayClientMessage(Component.translatable("message.rickrooms.shrink_ray.needs_lens").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            BlockState next = state.setValue(GROW, !state.getValue(GROW));
            level.setBlock(pos, next, Block.UPDATE_ALL);
            player.displayClientMessage(Component.translatable(next.getValue(GROW) ? "message.rickrooms.shrink_ray.grow"
                    : "message.rickrooms.shrink_ray.shrink").withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }
        fire((ServerLevel) level, pos, state);
        return InteractionResult.CONSUME;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.GROWTH_LENS.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(UPGRADED)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(UPGRADED, true).setValue(GROW, true), Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.3f);
            player.displayClientMessage(Component.translatable("message.rickrooms.shrink_ray.lens_fitted").withStyle(ChatFormatting.GREEN), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered && level instanceof ServerLevel server) fire(server, pos, state.setValue(POWERED, true));
    }

    /** Zap whoever is standing under the emitter. Returns how many it hit. */
    public static int fire(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(COOLING)) return 0;
        level.setBlock(pos, state.setValue(COOLING, true), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, state.getBlock(), 30);
        boolean grow = state.getValue(GROW) && state.getValue(UPGRADED);
        Direction facing = state.getValue(FACING);
        Vec3 emitter = Vec3.atBottomCenterOf(pos).add(facing.getStepX(), 1.97, facing.getStepZ());
        level.playSound(null, pos, ModSounds.SHRINK_RAY_FIRE.get(), SoundSource.BLOCKS, 1.0f, grow ? 0.7f : 1.6f);
        for (int i = 0; i < 24; i++) {
            double t = i / 24.0;
            level.sendParticles(grow ? GROW_BEAM : SHRINK_BEAM, emitter.x, emitter.y - t * 1.95, emitter.z, 2, 0.2 * t, 0.02, 0.2 * t, 0.0);
        }
        int hit = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, target(pos, state), LivingEntity::isAlive)) {
            if (grow) {
                if (target.hasEffect(ModEffects.SHRUNK)) target.removeEffect(ModEffects.SHRUNK);
                else target.addEffect(new MobEffectInstance(ModEffects.ENLARGED, DURATION, 0));
            } else {
                if (target.hasEffect(ModEffects.ENLARGED)) target.removeEffect(ModEffects.ENLARGED);
                else {
                    target.addEffect(new MobEffectInstance(ModEffects.SHRUNK, DURATION, 0));
                    if (target instanceof ServerPlayer player) ModAdvancements.award(player, "honey_i_shrunk");
                }
            }
            hit++;
        }
        return hit;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(COOLING)) level.setBlock(pos, state.setValue(COOLING, false), Block.UPDATE_CLIENTS);
    }
}
