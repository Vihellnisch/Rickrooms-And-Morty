package com.rickandmorty.block.entity;

import com.rickandmorty.registry.ModBlockEntities;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Where an exit door leads: a Backrooms level, or (null) back out to the Overworld. */
public class ExitDoorBlockEntity extends BlockEntity {
    /** Where in the destination you come out: anywhere, or a particular place. */
    public enum Arrival { ANYWHERE, MONOLITH }

    @Nullable
    private BackroomsLevel destination = BackroomsLevel.LEVEL_0;
    private Arrival arrival = Arrival.ANYWHERE;

    public ExitDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXIT_DOOR.get(), pos, state);
    }

    public @Nullable BackroomsLevel destination() {
        return destination;
    }

    public void setDestination(@Nullable BackroomsLevel destination) {
        this.destination = destination;
        setChanged();
    }

    public Arrival arrival() {
        return arrival;
    }

    public void setArrival(Arrival arrival) {
        this.arrival = arrival;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Destination", destination == null ? "overworld" : destination.getSerializedName());
        if (arrival != Arrival.ANYWHERE) tag.putString("Arrival", arrival.name().toLowerCase());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String name = tag.getString("Destination");
        destination = name.equals("overworld") ? null : BackroomsLevel.CODEC.byName(name, BackroomsLevel.LEVEL_0);
        arrival = tag.getString("Arrival").equals("monolith") ? Arrival.MONOLITH : Arrival.ANYWHERE;
    }
}
