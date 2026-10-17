package com.rickandmorty.block.entity;

import com.rickandmorty.registry.ModBlockEntities;
import com.rickandmorty.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The restaurant counter: a few servings a day, back in the morning. */
public class FoodCounterBlockEntity extends BlockEntity {
    public static final int SERVINGS = 6;
    private int servings = SERVINGS;
    private long day = -1;

    public FoodCounterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOOD_COUNTER.get(), pos, state);
    }

    public int servings() {
        return servings;
    }

    /** A plate of meatballs (or, every third serving, a lingonberry juice), if there's any left today. */
    public boolean serve(ServerLevel level, Player player) {
        long today = level.getDayTime() / 24000L;
        if (today != day) {
            day = today;
            servings = SERVINGS;
        }
        if (servings <= 0) {
            player.displayClientMessage(Component.translatable("message.rickrooms.food_counter.sold_out").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        servings--;
        ItemStack food = new ItemStack(servings % 3 == 0 ? ModItems.LINGONBERRY_JUICE.get() : ModItems.MEATBALL_PLATE.get());
        if (!player.getInventory().add(food)) player.drop(food, false);
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.2f);
        player.displayClientMessage(Component.translatable("message.rickrooms.food_counter.served", servings).withStyle(ChatFormatting.YELLOW), true);
        setChanged();
        return true;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        servings = tag.contains("Servings") ? tag.getInt("Servings") : SERVINGS;
        day = tag.contains("Day") ? tag.getLong("Day") : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Servings", servings);
        tag.putLong("Day", day);
    }
}
