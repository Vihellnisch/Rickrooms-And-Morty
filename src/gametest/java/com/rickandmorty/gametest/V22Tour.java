package com.rickandmorty.gametest;

import com.rickandmorty.entity.cruiser.SpaceCruiserEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.function.BiConsumer;

/** The v0.22 tour (./gradlew runTourClient -DtourV22=true): the cruiser cockpit, first person. */
final class V22Tour {
    private V22Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("warmup_2", (p, l) -> V10Tour.clean(p));
        stops.accept("cockpit_parked_hand", (p, l) -> cockpit(p, l, 0, false, 5.0f, 180.0f));
        stops.accept("cockpit_flying", (p, l) -> cockpit(p, l, 1, true, 10.0f, 180.0f));
        stops.accept("cockpit_look_down", (p, l) -> cockpit(p, l, 2, true, 55.0f, 180.0f));
        stops.accept("full_gun_dial_screen", V22Tour::fullGun);
    }

    /** What /rickrooms gun gives: every level already on the dial. */
    private static void fullGun(ServerPlayer player, ServerLevel level) {
        V10Tour.clean(player);
        level.setDayTime(6000);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, com.rickandmorty.item.PortalGunItem.fullyUpgraded(player.server));
    }

    /** Sitting in the pilot's seat (slots far from the v0.11 tour's, the tour world keeps old builds). */
    private static void cockpit(ServerPlayer player, ServerLevel level, int slot, boolean flying, float pitch, float yaw) {
        SpaceCruiserEntity cruiser = V11Tour.ride(player, level, 20 + slot, flying);
        if (flying) cruiser.setNoGravity(true);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 15, () -> {
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
            com.mojang.logging.LogUtils.getLogger().info("[tour] cockpit eye {} above the cruiser", player.getEyeY() - cruiser.getY());
        }));
    }
}
