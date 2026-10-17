package com.rickandmorty.block.entity;

import com.rickandmorty.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * A stock crate in the Infinite IKEA. It remembers what it's stocked with (its loot table), and
 * the first time anyone opens it on a new day it's emptied and filled up again, however much
 * was taken the day before.
 */
public class StockCrateBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private @Nullable ResourceKey<LootTable> stock;
    private long stockedOn = -1;

    public StockCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STOCK_CRATE.get(), pos, state);
    }

    @Override
    public void setLootTable(@Nullable ResourceKey<LootTable> table) {
        super.setLootTable(table);
        if (table != null) stock = table;
    }

    /** A new day since it was last stocked: empty it and fill it up again. */
    public void restock(ServerLevel level) {
        long day = level.getDayTime() / 24000L;
        if (day == stockedOn) return;
        stockedOn = day;
        if (stock == null) return;
        if (getLootTable() == null) {
            clearContent();
            setLootTable(stock, level.random.nextLong());
        }
        setChanged();
    }

    public long stockedOn() {
        return stockedOn;
    }

    /** For tests: pretend it was last stocked on {@code day}. */
    public void setStockedOn(long day) {
        stockedOn = day;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rickrooms.stock_crate");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items, registries);
        stock = tag.contains("Stock") ? ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(tag.getString("Stock"))) : getLootTable();
        stockedOn = tag.contains("StockedOn") ? tag.getLong("StockedOn") : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, registries);
        if (stock != null) tag.putString("Stock", stock.location().toString());
        tag.putLong("StockedOn", stockedOn);
    }
}
