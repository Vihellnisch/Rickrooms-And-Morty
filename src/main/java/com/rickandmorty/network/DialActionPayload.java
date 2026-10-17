package com.rickandmorty.network;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from the dial screen to the server: select, save, or delete a destination, or pick a known dimension,
 * on the portal gun held in {@code hand}.
 */
public record DialActionPayload(Action action, InteractionHand hand, int index, String name) implements CustomPacketPayload {
    public enum Action { SELECT, ADD, REMOVE, SELECT_DIMENSION }

    public static final Type<DialActionPayload> TYPE = new Type<>(RickAndMortyMod.id("dial_action"));

    public static final StreamCodec<ByteBuf, DialActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal), DialActionPayload::action,
            ByteBufCodecs.BOOL.map(main -> main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                    hand -> hand == InteractionHand.MAIN_HAND), DialActionPayload::hand,
            ByteBufCodecs.VAR_INT, DialActionPayload::index,
            ByteBufCodecs.stringUtf8(PortalDestinations.MAX_NAME_LENGTH * 4), DialActionPayload::name,
            DialActionPayload::new);

    public static DialActionPayload select(InteractionHand hand, int index) {
        return new DialActionPayload(Action.SELECT, hand, index, "");
    }

    public static DialActionPayload add(InteractionHand hand, String name) {
        return new DialActionPayload(Action.ADD, hand, -1, name);
    }

    public static DialActionPayload selectDimension(InteractionHand hand, int index) {
        return new DialActionPayload(Action.SELECT_DIMENSION, hand, index, "");
    }

    public static DialActionPayload remove(InteractionHand hand, int index) {
        return new DialActionPayload(Action.REMOVE, hand, index, "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the server. Never trust the client: re-check everything. */
    public static void handle(DialActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getItemInHand(payload.hand());
            if (!(stack.getItem() instanceof PortalGunItem)) return;

            PortalDestinations current = PortalGunItem.getDestinations(stack);
            PortalDestinations updated = switch (payload.action()) {
                case SELECT -> current.withSelected(payload.index());
                case SELECT_DIMENSION -> current.withSelectedDimension(payload.index());
                case REMOVE -> current.withRemoved(payload.index());
                case ADD -> {
                    String name = payload.name().strip();
                    if (name.isEmpty()) name = "Location " + (current.destinations().size() + 1);
                    if (name.length() > PortalDestinations.MAX_NAME_LENGTH) {
                        name = name.substring(0, PortalDestinations.MAX_NAME_LENGTH);
                    }
                    yield current.withAdded(new PortalDestinations.Destination(name, player.level().dimension(),
                            player.getX(), player.getY(), player.getZ(), player.getYRot()));
                }
            };
            stack.set(ModDataComponents.PORTAL_DESTINATIONS.get(), updated);
        });
    }
}
