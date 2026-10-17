package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.entity.Gore;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.cronenberg.Mutation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A swollen, glistening sac growing out of the flesh. Walk into it and it bursts all over you:
 * poison, nausea, and a dose of whatever it was full of (mutation). Sometimes leaves a Mutagen Sac.
 */
public class PustuleBlock extends Block {
    public static final MapCodec<PustuleBlock> CODEC = simpleCodec(PustuleBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 9, 13);

    public PustuleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living) {
            burst(server, pos, living);
        }
    }

    /** Pops the pustule over whoever touched it. */
    public static void burst(ServerLevel level, BlockPos pos, LivingEntity victim) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        Vec3 center = Vec3.atBottomCenterOf(pos).add(0, 0.4, 0);
        Gore.splash(level, center, 30, 0.35);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME, center.x, center.y, center.z, 20, 0.3, 0.3, 0.3, 0.15);
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.2f, 0.6f);
        level.playSound(null, pos, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 0.5f);
        victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80));
        victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120));
        if (victim instanceof ServerPlayer player) Mutation.add(player, 5.0f);
        if (level.random.nextFloat() < 0.25f) popResource(level, pos, new ItemStack(ModItems.MUTAGEN_SAC.get()));
    }

    /** It drips. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(Gore.BLOOD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.5, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0, 0);
        }
    }
}
