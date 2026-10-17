package com.rickandmorty.network;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.pet.AmplifierClientState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: whether a pet wears a Cognition Amplifier (modules, or -1 for none), so it's drawn. */
public record PetAmplifierPayload(int entityId, int modules) implements CustomPacketPayload {
    public static final Type<PetAmplifierPayload> TYPE = new Type<>(RickAndMortyMod.id("pet_amplifier"));
    public static final StreamCodec<ByteBuf, PetAmplifierPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PetAmplifierPayload::entityId,
            ByteBufCodecs.VAR_INT, PetAmplifierPayload::modules,
            PetAmplifierPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PetAmplifierPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AmplifierClientState.set(payload.entityId(), payload.modules()));
    }
}
