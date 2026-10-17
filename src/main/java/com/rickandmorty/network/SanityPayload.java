package com.rickandmorty.network;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.sanity.SanityClientState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: your current sanity (drives the HUD and the hallucinations). */
public record SanityPayload(float value) implements CustomPacketPayload {
    public static final Type<SanityPayload> TYPE = new Type<>(RickAndMortyMod.id("sanity"));
    public static final StreamCodec<ByteBuf, SanityPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, SanityPayload::value, SanityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SanityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SanityClientState.value = payload.value());
    }
}
