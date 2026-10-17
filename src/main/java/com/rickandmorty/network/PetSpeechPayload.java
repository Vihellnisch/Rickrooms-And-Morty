package com.rickandmorty.network;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.pet.AmplifierClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: an amplified pet said something; show it in a bubble over its head. */
public record PetSpeechPayload(int entityId, Component line) implements CustomPacketPayload {
    public static final Type<PetSpeechPayload> TYPE = new Type<>(RickAndMortyMod.id("pet_speech"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PetSpeechPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PetSpeechPayload::entityId,
            ComponentSerialization.TRUSTED_STREAM_CODEC, PetSpeechPayload::line,
            PetSpeechPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PetSpeechPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AmplifierClientState.said(payload.entityId(), payload.line()));
    }
}
