package com.rickandmorty.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "4";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(DialActionPayload.TYPE, DialActionPayload.STREAM_CODEC, DialActionPayload::handle);
        registrar.playToClient(SanityPayload.TYPE, SanityPayload.STREAM_CODEC, SanityPayload::handle);
        registrar.playToServer(GunModePayload.TYPE, GunModePayload.STREAM_CODEC, GunModePayload::handle);
        registrar.playToClient(MutationPayload.TYPE, MutationPayload.STREAM_CODEC, MutationPayload::handle);
        registrar.playToClient(PetAmplifierPayload.TYPE, PetAmplifierPayload.STREAM_CODEC, PetAmplifierPayload::handle);
        registrar.playToClient(PetSpeechPayload.TYPE, PetSpeechPayload.STREAM_CODEC, PetSpeechPayload::handle);
        registrar.playToServer(CruiserActionPayload.TYPE, CruiserActionPayload.STREAM_CODEC, CruiserActionPayload::handle);
    }
}
