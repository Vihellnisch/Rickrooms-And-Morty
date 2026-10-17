package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The fittings of the Dead Mall: patterned floor tiles, glass railings, the gumball machine. */
public final class MallBlocks {
    /** Cream, grey, teal, purple, yellow, brick red. */
    public static final IntegerProperty TONE = IntegerProperty.create("tone", 0, 5);

    private MallBlocks() {
    }

    /** Floor tiles, in six colours. */
    public static class Tile extends Block {
        public static final MapCodec<Tile> CODEC = simpleCodec(Tile::new);

        public Tile(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(TONE, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TONE);
        }
    }

    /** A glass balustrade with a steel handrail, too high to climb over. */
    public static class Railing extends Block {
        public static final MapCodec<Railing> CODEC = simpleCodec(Railing::new);
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16), COLLISION = Block.box(0, 0, 0, 16, 24, 16);

        public Railing(Properties properties) {
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
            return COLLISION;
        }

        @Override
        protected boolean skipRendering(BlockState state, BlockState neighbor, net.minecraft.core.Direction direction) {
            return neighbor.is(this) || super.skipRendering(state, neighbor, direction);
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }
    }

    /** A gumball machine. Put a coin in (an iron or gold nugget) and turn the handle. */
    public static class GumballMachine extends StoreBlocks.Fixed {
        public GumballMachine(Properties properties) {
            super(properties, Block.box(3, 0, 3, 13, 16, 13));
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(Items.IRON_NUGGET) && !stack.is(Items.GOLD_NUGGET)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.rickrooms.gumball.coin").withStyle(ChatFormatting.GRAY), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                ItemStack gumball = new ItemStack(ModItems.GUMBALL.get(), stack.is(Items.GOLD_NUGGET) ? 3 : 1);
                if (!player.getInventory().add(gumball)) player.drop(gumball, false);
                level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.8f, 1.6f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
