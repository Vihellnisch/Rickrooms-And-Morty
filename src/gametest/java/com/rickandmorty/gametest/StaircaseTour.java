package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.worldgen.level.StaircaseLevel;
import com.rickandmorty.worldgen.level.StaircaseLevel.Node;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;

import static com.rickandmorty.worldgen.level.StaircaseLevel.H;
import static com.rickandmorty.worldgen.level.StaircaseLevel.V;

/** Tour of Staircase (./gradlew runTourClient -DtourStaircase=true). */
final class StaircaseTour {
    static final ResourceKey<Level> STAIRS = ResourceKey.create(Registries.DIMENSION, RickAndMortyMod.id("the_staircase"));

    private StaircaseTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("stairs_warmup", (p, l) -> view(p, l, 0, 0, 7, 40, 10, 30));
        stops.accept("stairs_wide", (p, l) -> view(p, l, 20, 20, 8, 60, 20, 50));
        stops.accept("stairs_wide_2", (p, l) -> view(p, l, 10, 4, 9, 40, -12, 20));
        stops.accept("stairs_below", (p, l) -> view(p, l, 4, 8, 5, 30, -30, -20));
        stops.accept("stairs_landing", StaircaseTour::onLanding);
        stops.accept("stairs_arch", StaircaseTour::arch);
        stops.accept("stairs_spiral", StaircaseTour::spiral);
        stops.accept("stairs_flight", StaircaseTour::flight);
    }

    private static ServerLevel level(ServerLevel overworld) {
        ServerLevel level = overworld.getServer().getLevel(STAIRS);
        if (level == null) throw new IllegalStateException("no Staircase preview");
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

    /** From out in the air beside grid cell (i, k) at node height j, {@code back} blocks off, {@code up} above, looking {@code side} across. */
    private static void view(ServerPlayer player, ServerLevel overworld, int i, int k, int j, double back, double up, double side) {
        ServerLevel level = level(overworld);
        double cx = i * H + H / 2.0, cz = k * H + H / 2.0, cy = j * V;
        look(player, level, cx - back, cy + up, cz - side, cx + 20, cy, cz + 20);
    }

    private static Node find(long seed, java.util.function.Predicate<Node> wanted) {
        for (int i = 0; i < 60; i++) {
            for (int k = 0; k < 60; k++) {
                for (int j = StaircaseLevel.LOWEST + 2; j <= StaircaseLevel.HIGHEST - 2; j++) {
                    Node n = StaircaseLevel.node(seed, i, k, j);
                    if (n != null && wanted.test(n)) return n;
                }
            }
        }
        throw new IllegalStateException("nothing found");
    }

    private static void onLanding(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        Node n = find(seed, x -> x.hx() >= 4 && x.hz() >= 4 && StaircaseLevel.flight(seed, Math.floorDiv(x.x(), H), Math.floorDiv(x.z(), H), x.y() / V, true) != 0);
        look(player, level, n.x() - 2 + 0.5, n.y() + 2.62, n.z() + 0.5, n.x() + 30, n.y() + 8, n.z() + 6);
    }

    private static void arch(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        Node n = find(level.getSeed(), Node::wall);
        look(player, level, n.x() + 0.5, n.y() + 2.62, n.z() + n.hz() - 0.5, n.x() + 0.5, n.y() + 3, n.z() - n.hz());
    }

    private static void spiral(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        Node n = find(seed, x -> x.spiral() && StaircaseLevel.node(seed, Math.floorDiv(x.x(), H), Math.floorDiv(x.z(), H), x.y() / V + 1) != null);
        int cx = n.x() + n.hx() - 2, cz = n.z() + n.hz() - 2;
        look(player, level, cx + 9, n.y() + 6, cz + 9, cx, n.y() + 8, cz);
    }

    private static void flight(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        Node n = find(seed, x -> StaircaseLevel.flight(seed, Math.floorDiv(x.x(), H), Math.floorDiv(x.z(), H), x.y() / V, true) == 1);
        look(player, level, n.x() + n.hx() - 0.5, n.y() + 2.62, n.z() + 0.5, n.x() + n.hx() + 16, n.y() + 12, n.z() + 0.5);
    }
}
