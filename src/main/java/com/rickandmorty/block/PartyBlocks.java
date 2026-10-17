package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.PartygoerEntity;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The decorations of Level Fun. */
public final class PartyBlocks {
    /** Red, yellow, blue, green. */
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 3);

    private PartyBlocks() {
    }

    /** A balloon bobbing against the ceiling, its string hanging down. You can walk right through the string. */
    public static class Balloon extends Block {
        public static final MapCodec<Balloon> CODEC = simpleCodec(Balloon::new);
        private static final VoxelShape SHAPE = Block.box(4, 5, 4, 12, 16, 12);

        public Balloon(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(COLOR, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(COLOR);
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
        public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
            return defaultBlockState().setValue(COLOR, context.getLevel().getRandom().nextInt(4));   // a colour at random
        }

        /** Pop! */
        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 0.6f, 1.6f);
            return super.playerWillDestroy(level, pos, state, player);
        }
    }

    /** A wrapped present. Open it (use it) to see what's inside. Usually something nice. */
    public static class Present extends Block {
        public static final MapCodec<Present> CODEC = simpleCodec(Present::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 12, 14), Block.box(5, 12, 5, 11, 15, 11));
        public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, RickAndMortyMod.id("chests/party_present"));
        /** How often there's a Partygoer in the box instead. */
        public static final float SURPRISE = 0.15f;

        public Present(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(COLOR, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(COLOR);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
            return defaultBlockState().setValue(COLOR, context.getLevel().getRandom().nextInt(4));   // a colour at random
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) open(server, pos, serverPlayer);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        public static void open(ServerLevel level, BlockPos pos, ServerPlayer player) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.08);
            level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 0.8f, 1.2f);
            if (level.random.nextFloat() < SURPRISE) {
                PartygoerEntity partygoer = ModEntities.PARTYGOER.get().create(level);
                if (partygoer != null) {
                    partygoer.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
                    partygoer.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                    level.addFreshEntity(partygoer);
                    partygoer.turn(false);
                    player.displayClientMessage(Component.translatable("message.rickrooms.present.surprise").withStyle(ChatFormatting.RED), true);
                    ModAdvancements.award(player, "surprise");
                    return;
                }
            }
            LootTable table = level.getServer().reloadableRegistries().getLootTable(LOOT);
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) Block.popResource(level, pos, stack);
        }
    }
}
