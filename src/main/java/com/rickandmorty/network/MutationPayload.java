package com.rickandmorty.network;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.cronenberg.MutationClientState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: how mutated a player is (yours drives the HUD; everyone's drives the lumps on their model). */
public record MutationPayload(int entityId, float value) implements CustomPacketPayload {
    public static final Type<MutationPayload> TYPE = new Type<>(RickAndMortyMod.id("mutation"));
    public static final StreamCodec<ByteBuf, MutationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MutationPayload::entityId,
            ByteBufCodecs.FLOAT, MutationPayload::value,
            MutationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MutationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MutationClientState.set(payload.entityId(), payload.value()));
    }
}
