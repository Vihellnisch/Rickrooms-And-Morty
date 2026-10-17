package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.worldgen.level.RunLevel;
import com.rickandmorty.worldgen.level.SkyLevelEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.function.BiConsumer;

import static com.rickandmorty.worldgen.level.RunLevel.CELLS;
import static com.rickandmorty.worldgen.level.RunLevel.FLOOR;

/** Tour of Run For Your Life (./gradlew runTourClient -DtourRun=true): the maze of halls. */
final class RunTour {
    static final ResourceKey<Level> RUN = ResourceKey.create(Registries.DIMENSION, RickAndMortyMod.id("run_for_your_life"));
    /** A maze far from the ones earlier tours left mobs in. */
    private static final int MX = 37, MZ = -41;

    private RunTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("run_warmup", (p, l) -> start(p, l));
        stops.accept("run_start", (p, l) -> start(p, l));
        stops.accept("run_fork", (p, l) -> before(p, l, 3));
        stops.accept("run_turn", (p, l) -> before(p, l, 2));
        stops.accept("run_exit", RunTour::exit);
        stops.accept("run_behind", RunTour::behind);
        stops.accept("run_door_bursts", RunTour::burst);
    }

    private static ServerLevel level(ServerLevel overworld) {
        ServerLevel level = overworld.getServer().getLevel(RUN);
        if (level == null) throw new IllegalStateException("no Run For Your Life");
        return level;
    }

    private static void go(ServerPlayer player, ServerLevel level, double x, double z, float yaw, float pitch) {
        V10Tour.clean(player);
        level.getChunk((int) x >> 4, (int) z >> 4);
        DevTour.go(player, level, x, FLOOR + 1, z, yaw, pitch);
    }

    /** Yaw looking along a heading bit. */
    private static float yaw(int dir) {
        return dir == RunLevel.EAST ? -90.0f : dir == RunLevel.WEST ? 90.0f : dir == RunLevel.SOUTH ? 0.0f : 180.0f;
    }

    private static void start(ServerPlayer player, ServerLevel overworld) {
        int[] m = RunLevel.middle(MX, MZ, RunLevel.START_I, RunLevel.START_K);
        go(player, level(overworld), m[0] - 1.5, m[1] + 0.5, -90.0f, 2.0f);
    }

    /** Two cells before a cell with {@code links} connections (3: a fork, 2 with a bend: a turn), looking at it. */
    private static void before(ServerPlayer player, ServerLevel overworld, int links) {
        ServerLevel level = level(overworld);
        RunLevel.Maze maze = RunLevel.maze(level.getSeed(), MX, MZ);
        for (int c = 0; c < CELLS * CELLS; c++) {
            int l = maze.links()[c];
            if (Integer.bitCount(l) != links || maze.depth()[c] < 3) continue;
            if (links == 2 && (l == (RunLevel.EAST | RunLevel.WEST) || l == (RunLevel.NORTH | RunLevel.SOUTH))) continue;
            int p = maze.parent()[c], pp = maze.parent()[p];
            int i = c / CELLS, k = c % CELLS, pi = pp / CELLS, pk = pp % CELLS;
            if (pi != i && pk != k) continue;           // want a straight run up to it
            int dir = i > pi ? RunLevel.EAST : i < pi ? RunLevel.WEST : k > pk ? RunLevel.SOUTH : RunLevel.NORTH;
            int[] m = RunLevel.middle(MX, MZ, pi, pk);
            go(player, level, m[0] + 0.5, m[1] + 0.5, yaw(dir), 2.0f);
            return;
        }
        start(player, overworld);
    }

    private static void exit(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        RunLevel.Maze maze = RunLevel.maze(level.getSeed(), MX, MZ);
        for (int c = 0; c < CELLS * CELLS; c++) {
            if (!maze.exit()[c]) continue;
            int p = maze.parent()[c];
            int i = c / CELLS, k = c % CELLS, pi = p / CELLS, pk = p % CELLS;
            int dir = i > pi ? RunLevel.EAST : i < pi ? RunLevel.WEST : k > pk ? RunLevel.SOUTH : RunLevel.NORTH;
            int[] m = RunLevel.middle(MX, MZ, pi, pk);
            go(player, level, m[0] + 0.5, m[1] + 0.5, yaw(dir), 4.0f);
            return;
        }
    }

    /** Looking back the way you came: they're coming. */
    private static void behind(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        int[] a = RunLevel.middle(MX, MZ, RunLevel.START_I + 3, RunLevel.START_K);
        go(player, level, a[0] + 0.5, a[1] + 0.5, 90.0f, 2.0f);
        for (int i = 0; i < 5; i++) {
            Mob mob = ModEntities.BACTERIA.get().create(level);
            mob.moveTo(a[0] - 8 - i * 2 + 0.5, FLOOR + 1, a[1] - 1.5 + (i % 3), -90.0f, 0.0f);
            mob.setNoAi(true);
            level.addFreshEntity(mob);
        }
    }

    /** A door ahead bursts open and throws something out. */
    private static void burst(ServerPlayer player, ServerLevel overworld) {
        start(player, overworld);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 104, () -> SkyLevelEvents.burstDoor(player)));
    }
}
