package com.rickandmorty.registry;

import com.mojang.serialization.Codec;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.PortalDestinations;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data stored on item stacks (the 1.21 replacement for item NBT).
 */
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RickAndMortyMod.MOD_ID);

    /** Portal fluid charges left in a portal gun. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PORTAL_CHARGES =
            COMPONENTS.register("portal_charges", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 1000))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Saved destinations and which one the dial is set to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortalDestinations>> PORTAL_DESTINATIONS =
            COMPONENTS.register("portal_destinations", () -> DataComponentType.<PortalDestinations>builder()
                    .persistent(PortalDestinations.CODEC)
                    .networkSynchronized(PortalDestinations.STREAM_CODEC)
                    .build());

    /** The first gateway clicked with this gun, waiting to be linked to a second one. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> GATEWAY_LINK =
            COMPONENTS.register("gateway_link", () -> DataComponentType.<GlobalPos>builder()
                    .persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC)
                    .build());

    /** Installed by the Targeting Upgrade: shots that hit a creature teleport it instead of opening a portal. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TARGETING =
            COMPONENTS.register("targeting", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** Installed by the Blender Upgrade: the gun can be switched to attack mode. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BLENDER =
            COMPONENTS.register("blender", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** Installed by the DNA Lock Upgrade: the gun stays with its owner when they die. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DNA_LOCK =
            COMPONENTS.register("dna_lock", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** The modules fitted to a Cognition Amplifier (bits, see AmplifierModule), kept when it's taken off a pet. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMPLIFIER_MODULES =
            COMPONENTS.register("amplifier_modules", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 255))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Attack mode is on: shots open blender portals instead of travel portals. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ATTACK_MODE =
            COMPONENTS.register("attack_mode", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());
}
