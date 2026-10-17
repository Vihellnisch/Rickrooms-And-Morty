package com.rickandmorty.block.entity;

import com.rickandmorty.block.MicroverseWireBlock;
import com.rickandmorty.registry.ModBlockEntities;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Keeps every connected furnace burning, forever, whenever it has something to cook.
 * The network (wires and the furnaces they touch) is re-scanned every couple of seconds.
 */
public class MicroverseBatteryBlockEntity extends BlockEntity {
    private static final int SCAN_INTERVAL = 40;
    private static final int FUEL_INTERVAL = 10;
    /**
     * How much burn the battery keeps in a furnace. Plenty more than {@link #FUEL_INTERVAL}, since a
     * furnace sped up by a Time Crystal burns through it several times as fast.
     */
    private static final int BURN_BUFFER = 200;
    private static final int MAX_WIRES = 256;
    private static final int COMPLAINTS = 6;

    private final List<BlockPos> furnaces = new ArrayList<>();

    public MicroverseBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MICROVERSE_BATTERY.get(), pos, state);
    }

    public List<BlockPos> connectedFurnaces() {
        return List.copyOf(furnaces);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MicroverseBatteryBlockEntity battery) {
        long time = level.getGameTime();
        if (time % SCAN_INTERVAL == 0) battery.scan();
        if (time % FUEL_INTERVAL == 0) {
            for (BlockPos furnacePos : battery.furnaces) {
                if (level.getBlockEntity(furnacePos) instanceof AbstractFurnaceBlockEntity furnace) {
                    fuel(level, furnace);
                }
            }
        }
        // The tiny people inside have opinions.
        if (level.random.nextInt(24000) == 0) {
            Component line = Component.translatable("message.rickrooms.microverse." + level.random.nextInt(COMPLAINTS))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            for (var player : level.players()) {
                if (player instanceof ServerPlayer sp && sp.blockPosition().closerThan(pos, 12)) sp.displayClientMessage(line, false);
            }
        }
    }

    /** Walks the wires from the battery and records every furnace they (or the battery) touch. */
    public void scan() {
        furnaces.clear();
        if (level == null) return;
        Set<BlockPos> seen = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        seen.add(worldPosition);
        while (!queue.isEmpty() && seen.size() < MAX_WIRES) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!seen.add(next) || !level.isLoaded(next)) continue;
                BlockState state = level.getBlockState(next);
                if (state.is(ModBlocks.MICROVERSE_WIRE.get())) {
                    queue.add(next);
                } else if (MicroverseWireBlock.connectsTo(state) && level.getBlockEntity(next) instanceof AbstractFurnaceBlockEntity) {
                    furnaces.add(next);
                }
            }
        }
    }

    /** What this kind of furnace would make from the input, or empty if it can't cook it. */
    private static ItemStack cookResult(Level level, AbstractFurnaceBlockEntity furnace, ItemStack input) {
        if (furnace instanceof BlastFurnaceBlockEntity) return cookResult(level, input, RecipeType.BLASTING);
        if (furnace instanceof SmokerBlockEntity) return cookResult(level, input, RecipeType.SMOKING);
        return cookResult(level, input, RecipeType.SMELTING);
    }

    private static <T extends AbstractCookingRecipe> ItemStack cookResult(Level level, ItemStack input, RecipeType<T> type) {
        return level.getRecipeManager().getRecipeFor(type, new SingleRecipeInput(input), level)
                .map(holder -> holder.value().getResultItem(level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    /** Whether the furnace has something it can cook, with room for the result. */
    private static boolean canCook(Level level, AbstractFurnaceBlockEntity furnace) {
        ItemStack input = furnace.getItem(0);
        ItemStack output = furnace.getItem(2);
        if (input.isEmpty() || output.getCount() >= output.getMaxStackSize()) return false;
        ItemStack result = cookResult(level, furnace, input);
        return !result.isEmpty() && (output.isEmpty() || ItemStack.isSameItemSameComponents(output, result));
    }

    /** Lights the furnace (without using its fuel) as long as there's something it can cook. */
    private static void fuel(Level level, AbstractFurnaceBlockEntity furnace) {
        if (!canCook(level, furnace)) {
            // Done: let the battery's burn die down soon (fuel the player burned is left alone).
            if (furnace.litDuration == BURN_BUFFER && furnace.litTime > FUEL_INTERVAL * 2) {
                furnace.litTime = FUEL_INTERVAL * 2;
                furnace.setChanged();
            }
            return;
        }
        if (furnace.litTime < BURN_BUFFER) {
            furnace.litTime = BURN_BUFFER;
            furnace.litDuration = BURN_BUFFER;
            furnace.setChanged();
        }
        // The furnace only switches its own "lit" look when it catches fire from fuel, so do it here.
        // (It turns itself off again when there's nothing left to cook.)
        BlockState state = furnace.getBlockState();
        if (state.hasProperty(AbstractFurnaceBlock.LIT) && !state.getValue(AbstractFurnaceBlock.LIT)) {
            level.setBlock(furnace.getBlockPos(), state.setValue(AbstractFurnaceBlock.LIT, true), Block.UPDATE_ALL);
        }
    }
}
