package com.rickandmorty.block;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.block.entity.ExitDoorBlockEntity;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A red metal door with an EXIT sign. It never opens onto what's behind it: step through and
 * you're somewhere else. Where it leads is stored in the lower half's block entity (the
 * generators choose; a door placed by hand leads into the Backrooms, or out of them).
 */
public class ExitDoorBlock extends DoorBlock implements EntityBlock {
    public static final MapCodec<ExitDoorBlock> CODEC = simpleCodec(ExitDoorBlock::new);

    public ExitDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    public MapCodec<? extends DoorBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(lower) instanceof ExitDoorBlockEntity door) {
            if (door.arrival() == ExitDoorBlockEntity.Arrival.MONOLITH) LevelTravel.throughMonolithDoor(serverPlayer, lower);
            else LevelTravel.throughExitDoor(serverPlayer, lower, door.destination());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ExitDoorBlockEntity door) {
            // placed by hand: a way in (from outside) or a way out (from inside)
            door.setDestination(BackroomsLevel.isBackrooms(level.dimension()) ? null : BackroomsLevel.LEVEL_0);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // both halves get one (a block that has block entities has to make one for every state,
        // or the game complains when the chunk loads), but only the lower half's is ever read
        return new ExitDoorBlockEntity(pos, state);
    }
}
