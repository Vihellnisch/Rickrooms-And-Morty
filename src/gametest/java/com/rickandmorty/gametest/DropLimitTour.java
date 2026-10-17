package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.worldgen.level.DropLimitLevel;
import com.rickandmorty.worldgen.level.DropLimitLevel.Gallery;
import com.rickandmorty.worldgen.level.DropLimitLevel.Tower;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;

import static com.rickandmorty.worldgen.level.DropLimitLevel.GALLERY;
import static com.rickandmorty.worldgen.level.DropLimitLevel.island;
import static com.rickandmorty.worldgen.level.DropLimitLevel.islandTop;

/** Tour of Drop Limit (./gradlew runTourClient -DtourDropLimit=true). */
final class DropLimitTour {
    static final ResourceKey<Level> DROP = ResourceKey.create(Registries.DIMENSION, RickAndMortyMod.id("drop_limit"));

    private DropLimitTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> overview(p, l, 0));
        stops.accept("warmup_2", (p, l) -> overview(p, l, 1));
        stops.accept("drop_overview", (p, l) -> overview(p, l, 0));
        stops.accept("drop_road_end", (p, l) -> roadEnd(p, l, 0, false));
        stops.accept("drop_road_end_2", (p, l) -> roadEnd(p, l, 1, false));
        stops.accept("drop_sign_close", (p, l) -> roadEnd(p, l, 0, true));
        stops.accept("drop_cliff_edge", (p, l) -> cliff(p, l, 0));
        stops.accept("drop_cliff_face", (p, l) -> cliff(p, l, 1));
        stops.accept("drop_gallery_inside", (p, l) -> galleryInside(p, l, 0));
        stops.accept("drop_gallery_inside_2", (p, l) -> galleryInside(p, l, 1));
        stops.accept("drop_gallery_outside", (p, l) -> galleryOutside(p, l, 0));
        stops.accept("drop_gallery_door", (p, l) -> galleryDoor(p, l));
        stops.accept("drop_tower", (p, l) -> tower(p, l, 0, false));
        stops.accept("drop_tower_inside", (p, l) -> tower(p, l, 0, true));
        stops.accept("drop_cloud_sea", (p, l) -> cloudSea(p, l));
    }

    private static ServerLevel level(ServerLevel overworld) {
        ServerLevel level = overworld.getServer().getLevel(DROP);
        if (level == null) throw new IllegalStateException("no Drop Limit preview");
        return level;
    }

    private static void look(ServerPlayer player, ServerLevel level, double x, double eyeY, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - eyeY, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        level.getChunk((int) x >> 4, (int) z >> 4);
        DevTour.go(player, level, x, eyeY - 1.62, z, yaw, pitch);
    }

    /** The n-th island found walking east from the origin along z = row. */
    private static int[] findIsland(long seed, int n, int row) {
        int found = 0;
        boolean was = false;
        for (int x = 0; x < 200000; x += 4) {
            boolean is = island(seed, x, row) && island(seed, x + 30, row) && island(seed, x + 60, row);
            if (is && !was && found++ == n) return new int[]{x + 30, row};
            was = is;
        }
        return new int[]{0, row};
    }

    private static void overview(ServerPlayer player, ServerLevel overworld, int n) {
        V10Tour.clean(player);
        ServerLevel level = level(overworld);
        int[] at = findIsland(level.getSeed(), n, 300 * n);
        look(player, level, at[0] - 130, DropLimitLevel.ISLAND_TOP + 40, at[1] - 130, at[0] + 40, DropLimitLevel.ISLAND_TOP - 10, at[1] + 40);
    }

    /** Where an east-west road runs off a cliff (the n-th found), standing on its verge looking along it to the sign. */
    private static void roadEnd(ServerPlayer player, ServerLevel overworld, int n, boolean close) {
        V10Tour.clean(player);
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        int found = 0;
        for (int lane = 0; lane < 400; lane++) {
            if (DropLimitLevel.roadZ(seed, lane, 0) == Integer.MIN_VALUE) continue;
            for (int x = 0; x < 6000; x++) {
                int z = DropLimitLevel.roadZ(seed, lane, x) + 5;
                if (!DropLimitLevel.endsAhead(seed, lane, x, true, 1) || !island(seed, x, z) || !island(seed, x - 22, z - 1) || !island(seed, x - 7, z - 3)) continue;
                if (found++ < n) continue;
                int top = islandTop(seed, x, z);
                if (close) look(player, level, x - 7, top + 2.62, z - 3, x, top + 3.5, z);
                else look(player, level, x - 22, top + 2.62, z - 1, x + 10, top - 2, z + 3);
                return;
            }
        }
    }

    /** At the very edge of an island, looking out over the clouds (or down the cliff). */
    private static void cliff(ServerPlayer player, ServerLevel overworld, int n) {
        V10Tour.clean(player);
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        int[] at = findIsland(seed, 2 + n, 900);
        int x = at[0];
        while (island(seed, x + 1, at[1])) x++;
        int top = islandTop(seed, x, at[1]);
        if (n == 0) look(player, level, x - 2, top + 2.62, at[1], x + 40, top - 6, at[1] + 30);
        else look(player, level, x + 22, top + 6, at[1] - 20, x, top - 12, at[1]);
    }

    private static Gallery findGallery(long seed, int n, boolean door) {
        int found = 0;
        for (int lane = 0; lane < 200; lane++) {
            for (int segment = 0; segment < 30; segment++) {
                Gallery g = DropLimitLevel.gallery(seed, true, lane, segment);
                if (g == null || door && !g.door()) continue;
                if (found++ == n) return g;
            }
        }
        throw new IllegalStateException("no gallery");
    }

    /** Inside a gallery, looking along it: windows on the left, pillars on the right, meadow underfoot. */
    private static void galleryInside(ServerPlayer player, ServerLevel overworld, int n) {
        V10Tour.clean(player);
        ServerLevel level = level(overworld);
        Gallery g = findGallery(level.getSeed(), n, false);
        int x = g.from() + 40;
        // facing west (-x), the window side (+z) is on the left
        look(player, level, x + 0.5, GALLERY + 3.6, g.line() + 2.5, x - 30, GALLERY + 3.2, g.line() + 0.5);
    }

    private static void galleryOutside(ServerPlayer player, ServerLevel overworld, int n) {
        V10Tour.clean(player);
        ServerLevel level = level(overworld);
        Gallery g = findGallery(level.getSeed(), n, false);
        int x = g.from() + 80;
        look(player, level, x - 30, GALLERY + 14, g.line() + 34, x + 20, GALLERY + 3, g.line());
    }

    private static void galleryDoor(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level = level(overworld);
        Gallery g = findGallery(level.getSeed(), 0, true);
        look(player, level, g.to() - 12.5, GALLERY + 3.6, g.line() + 0.5, g.to(), GALLERY + 3.2, g.line() + 0.5);
    }

    private static void tower(ServerPlayer player, ServerLevel overworld, int n, boolean inside) {
        V10Tour.clean(player);
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        int found = 0;
        for (int cx = 0; cx < 60; cx++) {
            Tower t = DropLimitLevel.tower(seed, cx, 0);
            if (t == null || found++ < n) continue;
            if (inside) look(player, level, t.x() + 3.5, t.top() - 9.4, t.z() + 4.5, t.x() - 20, t.top() - 14, t.z() - 10);
            else look(player, level, t.x() - 40, t.top() - 4, t.z() - 34, t.x(), t.top() - 20, t.z());
            return;
        }
    }

    /** Low over the cloud sea, nothing but billows to the horizon. */
    private static void cloudSea(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level = level(overworld);
        long seed = level.getSeed();
        int x = 0;
        while (island(seed, x, -700) || island(seed, x + 80, -700)) x += 16;
        look(player, level, x, DropLimitLevel.CLOUD + 12, -700, x + 100, DropLimitLevel.CLOUD + 6, -650);
    }
}
