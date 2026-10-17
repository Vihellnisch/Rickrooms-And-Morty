package com.rickandmorty.gametest;

import com.rickandmorty.command.GotoCommand;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.DeadMallLevel;
import com.rickandmorty.worldgen.level.DeadMallLevel.Court;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

/** The Dead Mall tour (./gradlew runTourClient -DtourV15=true). */
final class V15Tour {
    private static final int L = DeadMallLevel.LOW, U = DeadMallLevel.UP;

    private V15Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("mall_by_command", (p, l) -> {
            V10Tour.clean(p);
            GotoCommand.go(p, "dead_mall");
        });
        stops.accept("mall_concourse", (p, l) -> concourse(p, l, false));
        stops.accept("mall_upper_walkway", (p, l) -> concourse(p, l, true));
        stops.accept("mall_atrium", (p, l) -> court(p, l, Court.ATRIUM, true));
        stops.accept("mall_atrium_below", (p, l) -> court(p, l, Court.ATRIUM, false));
        stops.accept("mall_fountain", (p, l) -> court(p, l, Court.FOUNTAIN, true));
        stops.accept("mall_food_court", (p, l) -> court(p, l, Court.FOOD, false));
        stops.accept("mall_anchor_store", (p, l) -> court(p, l, Court.ANCHOR, false));
        stops.accept("mall_shopfronts", V15Tour::shopfronts);
    }

    private static ServerLevel mall(ServerLevel overworld) {
        return overworld.getServer().getLevel(BackroomsLevel.DEAD_MALL.key());
    }

    private static float yaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Partway along a wide concourse, looking down it. */
    private static void concourse(ServerPlayer player, ServerLevel overworld, boolean upstairs) {
        V10Tour.clean(player);
        ServerLevel level = mall(overworld);
        long seed = level.getSeed();
        for (int i = 20; i < 60; i++) {
            if (!DeadMallLevel.edge(seed, i, 20, 0)) continue;
            double[] p = DeadMallLevel.edgePoint(seed, i, 20, 0, 0.3);
            if (p[4] < 8) continue;
            double side = upstairs ? p[4] - 2 : 0;
            double x = p[0] - p[3] * side, z = p[1] + p[2] * side;
            DevTour.go(player, level, x, upstairs ? U : L, z, yaw(p[2], p[3]), upstairs ? 12.0f : 0.0f);
            return;
        }
    }

    private static void court(ServerPlayer player, ServerLevel overworld, Court kind, boolean upstairs) {
        V10Tour.clean(player);
        ServerLevel level = mall(overworld);
        long seed = level.getSeed();
        for (int i = 20; i < 80; i++) {
            DeadMallLevel.Node n = DeadMallLevel.node(seed, i, 24);
            if (n.court() != kind) continue;
            if (kind == Court.ANCHOR) {
                DevTour.go(player, level, n.x() - 13.5, L + 3, n.z() - 13.5, -45.0f, 10.0f);
            } else if (upstairs) {
                DevTour.go(player, level, n.x() + 0.5, U, n.z() - n.radius() + 1.5, 0.0f, 22.0f);
            } else {
                DevTour.go(player, level, n.x() - n.radius() + 2.5, L, n.z() + 0.5, -90.0f, -8.0f);
            }
            return;
        }
    }

    private static void shopfronts(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level = mall(overworld);
        long seed = level.getSeed();
        for (int i = 30; i < 60; i++) {
            if (!DeadMallLevel.edge(seed, i, 26, 0)) continue;
            double[] p = DeadMallLevel.edgePoint(seed, i, 26, 0, 0.5);
            if (p[4] > 5) continue;
            DevTour.go(player, level, p[0], L, p[1], yaw(p[2], p[3]) + 35.0f, 0.0f);
            return;
        }
    }
}
