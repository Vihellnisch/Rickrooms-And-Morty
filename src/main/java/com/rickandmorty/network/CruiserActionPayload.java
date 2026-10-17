package com.rickandmorty.network;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.cruiser.CruiserWeapons;
import com.rickandmorty.entity.cruiser.SpaceCruiserEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the pilot of a Space Cruiser pressed one of its controls. */
public record CruiserActionPayload(int action) implements CustomPacketPayload {
    public static final int FIRE = 0, CEASE_FIRE = 1, NEXT_WEAPON = 2, REPULSE = 3, PORTAL = 4, DIAL = 5;
    public static final Type<CruiserActionPayload> TYPE = new Type<>(RickAndMortyMod.id("cruiser_action"));
    public static final StreamCodec<ByteBuf, CruiserActionPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(CruiserActionPayload::new, CruiserActionPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CruiserActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.getVehicle() instanceof SpaceCruiserEntity cruiser)
                    || cruiser.pilot() != player) return;
            switch (payload.action()) {
                case FIRE -> CruiserWeapons.firePressed(cruiser, player);
                case CEASE_FIRE -> CruiserWeapons.fireReleased(cruiser);
                case NEXT_WEAPON -> cruiser.cycleWeapon();
                case REPULSE -> CruiserWeapons.repulse(cruiser, player);
                case PORTAL -> CruiserWeapons.portalJump(cruiser, player);
                case DIAL -> CruiserWeapons.gunToHand(player);
                default -> {
                }
            }
        });
    }
}
