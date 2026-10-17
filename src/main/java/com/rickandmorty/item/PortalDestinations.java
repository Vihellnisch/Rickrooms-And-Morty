package com.rickandmorty.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The portal gun's "dial": named saved locations, plus special dimensions the gun has
 * learned (like the Backrooms), which land somewhere random every trip.
 * Either a saved location ({@code selected}) or a known dimension ({@code selectedDimension}) is the target.
 * Immutable, as data components must be — every change returns a new instance.
 */
public record PortalDestinations(List<Destination> destinations, int selected,
                                 List<ResourceKey<Level>> knownDimensions, Optional<ResourceKey<Level>> selectedDimension) {
    public static final int MAX_DESTINATIONS = 8;
    /** How many dimensions a dial can know; the network codec must allow this many (it used to stop at 16, and every level plus Cronenberg World is 21). */
    public static final int MAX_KNOWN = 64;
    public static final int MAX_NAME_LENGTH = 24;
    public static final PortalDestinations EMPTY = new PortalDestinations(List.of(), -1, List.of(), Optional.empty());

    public static final Codec<PortalDestinations> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Destination.CODEC.listOf().fieldOf("destinations").forGetter(PortalDestinations::destinations),
            Codec.INT.fieldOf("selected").forGetter(PortalDestinations::selected),
            Level.RESOURCE_KEY_CODEC.listOf().optionalFieldOf("known_dimensions", List.of()).forGetter(PortalDestinations::knownDimensions),
            Level.RESOURCE_KEY_CODEC.optionalFieldOf("selected_dimension").forGetter(PortalDestinations::selectedDimension)
    ).apply(instance, PortalDestinations::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortalDestinations> STREAM_CODEC = StreamCodec.composite(
            Destination.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_DESTINATIONS)), PortalDestinations::destinations,
            ByteBufCodecs.VAR_INT, PortalDestinations::selected,
            ResourceKey.streamCodec(Registries.DIMENSION).apply(ByteBufCodecs.list(MAX_KNOWN)), PortalDestinations::knownDimensions,
            ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.DIMENSION)), PortalDestinations::selectedDimension,
            PortalDestinations::new);

    public PortalDestinations {
        destinations = List.copyOf(destinations);
        knownDimensions = List.copyOf(knownDimensions);
    }

    /** The selected saved location, if a saved location (not a dimension) is selected. */
    public Optional<Destination> selectedDestination() {
        return selectedDimension.isEmpty() && isValidIndex(selected) ? Optional.of(destinations.get(selected)) : Optional.empty();
    }

    public boolean hasTarget() {
        return selectedDimension.isPresent() || selectedDestination().isPresent();
    }

    public boolean isSavedSelected(int index) {
        return selectedDimension.isEmpty() && index == selected;
    }

    public boolean knows(ResourceKey<Level> dimension) {
        return knownDimensions.contains(dimension);
    }

    public boolean isValidIndex(int index) {
        return index >= 0 && index < destinations.size();
    }

    public boolean isFull() {
        return destinations.size() >= MAX_DESTINATIONS;
    }

    public PortalDestinations withSelected(int index) {
        return isValidIndex(index) ? new PortalDestinations(destinations, index, knownDimensions, Optional.empty()) : this;
    }

    public PortalDestinations withSelectedDimension(int index) {
        if (index < 0 || index >= knownDimensions.size()) return this;
        return new PortalDestinations(destinations, selected, knownDimensions, Optional.of(knownDimensions.get(index)));
    }

    /** Permanently learns a dimension and selects it. */
    public PortalDestinations withKnownDimension(ResourceKey<Level> dimension) {
        List<ResourceKey<Level>> known = new ArrayList<>(knownDimensions);
        if (!known.contains(dimension)) known.add(dimension);
        return new PortalDestinations(destinations, selected, known, Optional.of(dimension));
    }

    /** Learns a dimension without changing what's selected (e.g. a Backrooms level you've just walked into). */
    public PortalDestinations withLearnedDimension(ResourceKey<Level> dimension) {
        if (knownDimensions.contains(dimension) || knownDimensions.size() >= MAX_KNOWN) return this;
        List<ResourceKey<Level>> known = new ArrayList<>(knownDimensions);
        known.add(dimension);
        return new PortalDestinations(destinations, selected, known, selectedDimension);
    }

    /** Adds a destination and selects it. */
    public PortalDestinations withAdded(Destination destination) {
        if (isFull()) return this;
        List<Destination> list = new ArrayList<>(destinations);
        list.add(destination);
        return new PortalDestinations(list, list.size() - 1, knownDimensions, Optional.empty());
    }

    public PortalDestinations withRemoved(int index) {
        if (!isValidIndex(index)) return this;
        List<Destination> list = new ArrayList<>(destinations);
        list.remove(index);
        int newSelected = selected;
        if (index < selected) newSelected--;
        else if (index == selected) newSelected = list.isEmpty() ? -1 : Math.min(selected, list.size() - 1);
        return new PortalDestinations(list, newSelected, knownDimensions, selectedDimension);
    }

    public record Destination(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw) {
        public static final Codec<Destination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Destination::name),
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Destination::dimension),
                Codec.DOUBLE.fieldOf("x").forGetter(Destination::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Destination::y),
                Codec.DOUBLE.fieldOf("z").forGetter(Destination::z),
                Codec.FLOAT.fieldOf("yaw").forGetter(Destination::yaw)
        ).apply(instance, Destination::new));

        public static final StreamCodec<ByteBuf, Destination> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH * 4), Destination::name,
                ResourceKey.streamCodec(Registries.DIMENSION), Destination::dimension,
                ByteBufCodecs.DOUBLE, Destination::x,
                ByteBufCodecs.DOUBLE, Destination::y,
                ByteBufCodecs.DOUBLE, Destination::z,
                ByteBufCodecs.FLOAT, Destination::yaw,
                Destination::new);

        public Vec3 position() {
            return new Vec3(x, y, z);
        }

        /** e.g. "Overworld (120, 64, -35)" */
        public MutableComponent describe() {
            return Component.translatable("dial.rickrooms.location",
                    dimensionName(dimension), (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        }
    }

    public static Component dimensionName(ResourceKey<Level> dimension) {
        if (dimension == Level.OVERWORLD) return Component.translatable("dimension.rickrooms.overworld");
        if (dimension == Level.NETHER) return Component.translatable("dimension.rickrooms.nether");
        if (dimension == Level.END) return Component.translatable("dimension.rickrooms.end");
        if (dimension.location().getNamespace().equals("rickrooms")) {
            return Component.translatable("dimension.rickrooms." + dimension.location().getPath());
        }
        return Component.literal(dimension.location().getPath());
    }
}
