package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.worldgen.level.FroopylandLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;

import static com.rickandmorty.worldgen.level.FroopylandLevel.ground;
import static com.rickandmorty.worldgen.level.FroopylandLevel.riverAcross;

/** Tour of Froopyland (./gradlew runTourClient -DtourFroopyland=true). */
final class FroopylandTour {
    static final ResourceKey<Level> FROOPY = ResourceKey.create(Registries.DIMENSION, RickAndMortyMod.id("froopyland"));

    private FroopylandTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("froopy_warmup", (p, l) -> hills(p, l, 0, 30));
        stops.accept("froopy_hills", (p, l) -> hills(p, l, 0, 14));
        stops.accept("froopy_hills_2", (p, l) -> hills(p, l, 900, 6));
        stops.accept("froopy_river", FroopylandTour::river);
        stops.accept("froopy_rainbow", FroopylandTour::rainbow);
        stops.accept("froopy_trees", (p, l) -> hills(p, l, 2400, 3));
        stops.accept("froopy_tommy", FroopylandTour::tommy);
    }

    private static ServerLevel level(ServerLevel overworld) {
        ServerLevel level = overworld.getServer().getLevel(FROOPY);
        if (level == null) throw new IllegalStateException("no Froopyland preview");
        return level;
    }

    private static void look(ServerPlayer player, ServerLevel level, double x, double eyeY, double z, double tx, double ty, double tz) {
        V10Tour.clean(player);
        double dx = tx - x, dy = ty - eyeY, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        level.getChunk((int) x >> 4, (int) z >> 4);
        DevTour.go(player, level, x, eyeY - 1.62, z, yaw, pitch);
    }

    private static void hills(ServerPlayer player, ServerLevel overworld, int x, int up) {
        ServerLevel level = level(overworld);
        int g = ground(level.getSeed(), x, 40);
        look(player, level, x, g + up + 1.62, 40, x + 40, g + up - 4, 80);
    }

    private static void river(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        for (int z = 0; z < 2000; z++) {
            if (riverAcross(seed, 300, z) != 0) continue;
            look(player, level, 300, Math.max(ground(seed, 300, z - 18), FroopylandLevel.BASE) + 9, z - 18, 326, FroopylandLevel.BASE - 3, z);
            return;
        }
    }

    private static void rainbow(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        for (int cx = 0; cx < 30; cx++) {
            for (int cz = 0; cz < 30; cz++) {
                long h = com.rickandmorty.worldgen.level.GenMath.hash(seed, cx, cz, 9550);
                if (com.rickandmorty.worldgen.level.GenMath.unit(h) > 0.35) continue;
                int mx = cx * FroopylandLevel.RAINBOW_CELL + 40 + (int) Math.floorMod(h >>> 16, FroopylandLevel.RAINBOW_CELL - 80L);
                int mz = cz * FroopylandLevel.RAINBOW_CELL + 40 + (int) Math.floorMod(h >>> 28, FroopylandLevel.RAINBOW_CELL - 80L);
                int g = ground(seed, mx, mz - 50);
                look(player, level, mx, g + 6, mz - 50, mx, g + 14, mz);
                return;
            }
        }
    }

    private static void tommy(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        for (int cx = 0; cx < 20; cx++) {
            for (int cz = 0; cz < 20; cz++) {
                long h = com.rickandmorty.worldgen.level.GenMath.hash(seed, cx, cz, 9570);
                if (com.rickandmorty.worldgen.level.GenMath.unit(h) > 0.5) continue;
                int mx = cx * FroopylandLevel.CAVE_CELL + 100 + (int) Math.floorMod(h >>> 16, FroopylandLevel.CAVE_CELL - 200L);
                int mz = cz * FroopylandLevel.CAVE_CELL + 100 + (int) Math.floorMod(h >>> 28, FroopylandLevel.CAVE_CELL - 200L);
                int g = ground(seed, mx, mz - 26);
                look(player, level, mx + 0.5, g + 3.6, mz - 26, mx + 0.5, g - 2, mz);
                return;
            }
        }
    }
}
