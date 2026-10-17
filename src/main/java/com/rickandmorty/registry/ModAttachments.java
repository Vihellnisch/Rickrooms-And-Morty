package com.rickandmorty.registry;

import com.mojang.serialization.Codec;
import com.rickandmorty.RickAndMortyMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Data stored on players. */
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, RickAndMortyMod.MOD_ID);

    /** 0..100. Not copied on death, so you respawn with a clear head. */
    public static final Supplier<AttachmentType<Float>> SANITY = ATTACHMENTS.register("sanity",
            () -> AttachmentType.builder(() -> 100.0f).serialize(Codec.floatRange(0.0f, 100.0f)).build());

    /** Cronenberg mutation, 0..100. Also not copied on death: dying is one (grim) cure. */
    public static final Supplier<AttachmentType<Float>> MUTATION = ATTACHMENTS.register("mutation",
            () -> AttachmentType.builder(() -> 0.0f).serialize(Codec.floatRange(0.0f, 100.0f)).build());
}
