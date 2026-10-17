package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.worldgen.level.FacadeLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;

import static com.rickandmorty.worldgen.level.FacadeLevel.BAY;
import static com.rickandmorty.worldgen.level.FacadeLevel.DEPTH;
import static com.rickandmorty.worldgen.level.FacadeLevel.FLOOR;

/** Tour of Facade (./gradlew runTourClient -DtourFacade=true). */
final class FacadeTour {
    static final ResourceKey<Level> FACADE = ResourceKey.create(Registries.DIMENSION, RickAndMortyMod.id("the_facade"));

    private FacadeTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("facade_warmup", (p, l) -> along(p, l, 0, 200, 0.0));
        stops.accept("facade_up", (p, l) -> along(p, l, 0, 160, -38.0));
        stops.accept("facade_along", (p, l) -> along(p, l, 600, 200, 0.0));
        stops.accept("facade_down", (p, l) -> along(p, l, 1200, 260, 55.0));
        stops.accept("facade_ledge", FacadeTour::ledge);
        stops.accept("facade_room", (p, l) -> room(p, l, false));
        stops.accept("facade_room_door", (p, l) -> room(p, l, true));
        stops.accept("facade_ladder", FacadeTour::ladder);
        stops.accept("facade_catwalk", FacadeTour::catwalk);
    }

    private static ServerLevel level(ServerLevel overworld) {
        ServerLevel level = overworld.getServer().getLevel(FACADE);
        if (level == null) throw new IllegalStateException("no Facade preview");
        return level;
    }

    private static void go(ServerPlayer player, ServerLevel level, double x, double feetY, double z, float yaw, float pitch) {
        V10Tour.clean(player);
        level.getChunk((int) x >> 4, (int) z >> 4);
        DevTour.go(player, level, x, feetY, z, yaw, pitch);
    }

    /** Hanging in the canyon near the south face of row 0, looking along it (west), tilted by {@code pitch}. */
    private static void along(ServerPlayer player, ServerLevel overworld, int x, int y, double pitch) {
        go(player, level(overworld), x + 0.5, y, DEPTH + 3.5, 120.0f, (float) pitch);
    }

    /** Standing on a ledge, looking along the face past the boxes. */
    private static void ledge(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        for (int floor = 40; floor < 80; floor++) {
            for (int x = 0; x < 3000; x += BAY) {
                boolean run = true;
                for (int k = 0; k < 14 && run; k++) run = FacadeLevel.ledge(seed, x - k, floor, 1);
                if (!run) continue;
                go(player, level, x + 0.5, floor * FLOOR + 1, DEPTH + 0.5, 90.0f, 8.0f);
                return;
            }
        }
    }

    private static int[] findRoom(long seed, boolean door) {
        for (int floor = 40; floor < 90; floor++) {
            for (int bay = 0; bay < 400; bay++) {
                if (!FacadeLevel.room(seed, bay, floor, 1)) continue;
                if (door && !FacadeLevel.hasDoor(seed, bay, floor, 1)) continue;
                return new int[]{bay, floor};
            }
        }
        return new int[]{0, 50};
    }

    /** In one of the rooms: looking out of its window at the building opposite, or back at its red door. */
    private static void room(ServerPlayer player, ServerLevel overworld, boolean door) {
        ServerLevel level = level(overworld);
        int[] r = findRoom(level.getSeed(), door);
        int x = r[0] * BAY + 3, y = r[1] * FLOOR + 1;
        if (door) go(player, level, x + 0.5, y, DEPTH - 1.5, 180.0f, 8.0f);
        else go(player, level, x + 0.5, y, DEPTH - 4.5, 0.0f, -4.0f);
    }

    private static void ladder(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        for (int x = 0; x < 3000; x++) {
            if (!FacadeLevel.ladder(seed, x, 60, 1) || !FacadeLevel.ladder(seed, x, 61, 1)) continue;
            go(player, level, x - 4.5, 60 * FLOOR + 3, DEPTH + 6.5, -150.0f, -25.0f);
            return;
        }
    }

    private static void catwalk(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        for (int x = 0; x < 20000; x++) {
            int floor = FacadeLevel.bridgeFloor(seed, 0, x);
            if (floor < 0) continue;
            go(player, level, x + 1.5, floor * FLOOR + 1, DEPTH + 1.5, 0.0f, 6.0f);
            return;
        }
    }
}
