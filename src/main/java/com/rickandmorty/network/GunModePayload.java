package com.rickandmorty.network;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.PortalGunItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the mode key (or the dial's mode button) was pressed for the gun in {@code hand}. */
public record GunModePayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<GunModePayload> TYPE = new Type<>(RickAndMortyMod.id("gun_mode"));
    public static final StreamCodec<ByteBuf, GunModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.map(main -> main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, hand -> hand == InteractionHand.MAIN_HAND),
            GunModePayload::hand,
            GunModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getItemInHand(payload.hand());
            if (stack.getItem() instanceof PortalGunItem) PortalGunItem.toggleMode(player, stack);
        });
    }
}
