package com.rickandmorty.gametest;

import com.rickandmorty.worldgen.level.AbandonedCityLevel;
import com.rickandmorty.worldgen.level.AbandonedCityLevel.Kind;
import com.rickandmorty.worldgen.level.AbandonedCityLevel.Lot;
import com.rickandmorty.worldgen.level.AbandonedCityLevel.Tree;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelGenerators;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static com.rickandmorty.worldgen.level.AbandonedCityLevel.CELL;
import static com.rickandmorty.worldgen.level.AbandonedCityLevel.G;

/** The Abandoned City (./gradlew runTourClient -DtourCity=true). */
final class CityTour {
    static final net.minecraft.resources.ResourceKey<Level> CITY = BackroomsLevel.ABANDONED_CITY.key();

    private CityTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> aerial(p, l, 0, 0, 70, 1500));
        stops.accept("warmup_2", (p, l) -> aerial(p, l, 3, 3, 70, 1500));
        stops.accept("city_skyline", (p, l) -> aerial(p, l, 0, 0, 58, 1500));
        stops.accept("city_avenue", (p, l) -> avenue(p, l, 1, 1, 2000));
        stops.accept("city_apartment", (p, l) -> building(p, l, Kind.APARTMENT, 0));
        stops.accept("city_apartment_2", (p, l) -> building(p, l, Kind.APARTMENT, 1));
        stops.accept("city_crumbled", (p, l) -> building(p, l, Kind.CRUMBLED, 0));
        stops.accept("city_crumbled_2", (p, l) -> building(p, l, Kind.CRUMBLED, 1));
        stops.accept("city_moss", (p, l) -> building(p, l, Kind.MOSS, 0));
        stops.accept("city_moss_2", (p, l) -> building(p, l, Kind.MOSS, 1));
        stops.accept("city_giant_tree", (p, l) -> tree(p, l, 0, false));
        stops.accept("city_giant_tree_up", (p, l) -> tree(p, l, 1, true));
        stops.accept("city_tower", (p, l) -> building(p, l, Kind.TOWER, 0));
        stops.accept("city_lowrise", (p, l) -> building(p, l, Kind.LOWRISE, 0));
        stops.accept("city_park", (p, l) -> building(p, l, Kind.PARK, 0));
        stops.accept("city_pond", (p, l) -> building(p, l, Kind.POND, 0));
        stops.accept("city_parking", (p, l) -> building(p, l, Kind.PARKING, 0));
        stops.accept("city_rubble", (p, l) -> building(p, l, Kind.RUBBLE, 0));
        stops.accept("city_ferris", (p, l) -> building(p, l, Kind.FERRIS, 0));
        stops.accept("city_interior", (p, l) -> interior(p, l, 0));
        stops.accept("city_rooftop", (p, l) -> rooftop(p, l, 0));
        stops.accept("city_tree_through_building", (p, l) -> treeInBuilding(p, l));
        stops.accept("city_sunset", (p, l) -> aerial(p, l, 2, -1, 46, 11700));
        stops.accept("city_exit_door", CityTour::exitDoor);
    }

    private static ServerLevel city(ServerLevel overworld) {
        ServerLevel level = overworld.getServer().getLevel(CITY);
        if (level == null) throw new IllegalStateException("no Abandoned City dimension");
        return level;
    }

    private static AbandonedCityLevel gen() {
        return (AbandonedCityLevel) LevelGenerators.get(BackroomsLevel.ABANDONED_CITY);
    }

    /** The nth lot of this kind, nearest the middle first. */
    static Lot find(long seed, Kind kind, int nth) {
        int found = 0;
        for (int ring = 0; ring < 14; ring++) {
            for (int cx = -ring; cx <= ring; cx++) {
                for (int cz = -ring; cz <= ring; cz++) {
                    if (Math.max(Math.abs(cx), Math.abs(cz)) != ring) continue;
                    Lot lot = gen().lot(seed, cx, cz);
                    if (lot.kind() == kind && found++ == nth) return lot;
                }
            }
        }
        throw new IllegalStateException("no " + kind);
    }

    static Tree findTree(long seed, int nth) {
        List<Tree> all = new ArrayList<>();
        for (int ring = 0; ring < 8 && all.size() <= nth; ring++) {
            for (int cx = -ring; cx <= ring; cx++) {
                for (int cz = -ring; cz <= ring; cz++) {
                    if (Math.max(Math.abs(cx), Math.abs(cz)) == ring) all.addAll(gen().trees(seed, cx, cz));
                }
            }
        }
        return all.get(nth);
    }

    private static void look(ServerPlayer player, ServerLevel level, double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        DevTour.go(player, level, ex, ey - 1.62, ez, yaw, pitch);
    }

    private static void time(ServerLevel overworld, long time) {
        overworld.setDayTime(time);
        overworld.setWeatherParameters(6000, 0, false, false);
    }

    /** High over a crossing, looking out across the blocks. */
    private static void aerial(ServerPlayer player, ServerLevel overworld, int cx, int cz, int above, long time) {
        V10Tour.clean(player);
        time(overworld, time);
        ServerLevel level = city(overworld);
        double x = cx * CELL + 6, z = cz * CELL + 6;
        look(player, level, x, G + above, z, x + 120, G + 10, z + 160);
    }

    /** Standing in the road at a crossing, looking down the street. */
    private static void avenue(ServerPlayer player, ServerLevel overworld, int cx, int cz, long time) {
        V10Tour.clean(player);
        time(overworld, time);
        ServerLevel level = city(overworld);
        double x = cx * CELL + 6, z = cz * CELL + 4;
        look(player, level, x, G + 7, z, x, G + 6, z + 100);
    }

    /** A lot seen from its street corner, raised a little, the whole thing in view. */
    private static void building(ServerPlayer player, ServerLevel overworld, Kind kind, int nth) {
        V10Tour.clean(player);
        time(overworld, 2000);
        ServerLevel level = city(overworld);
        Lot lot = find(level.getSeed(), kind, nth);
        boolean open = kind == Kind.PARK || kind == Kind.POND || kind == Kind.PARKING;
        double tx = (lot.worldX(lot.x0()) + lot.worldX(lot.x1())) / 2.0 + 0.5, tz = (lot.worldZ(lot.z0()) + lot.worldZ(lot.z1())) / 2.0 + 0.5;
        if (open) {
            tx = lot.worldX(22);
            tz = lot.worldZ(22);
        }
        int tall = open ? 6 : kind == Kind.FERRIS ? 40 : lot.top() - G;
        if (kind == Kind.TOWER) {
            // above the rooftops round it, far enough back to see it whole
            double a = Math.atan2(lot.cz() * CELL + 6 - tz, lot.cx() * CELL + 6 - tx);
            look(player, level, tx + Math.cos(a) * 48, G + tall * 0.62, tz + Math.sin(a) * 48, tx, G + tall * 0.48, tz);
            return;
        }
        double ty = G + tall * 0.42;
        // from the crossing at the lot's low corner (a crumbled one from the side it fell), pulled back far enough to fit it in
        double cx = lot.cx() * CELL + 6, cz = lot.cz() * CELL + 6;
        if (kind == Kind.CRUMBLED || kind == Kind.RUBBLE) {
            cx = tx + Math.cos(lot.collapseAngle()) * 34;
            cz = tz + Math.sin(lot.collapseAngle()) * 34;
        }
        double back = Math.max(0, tall * 0.9 - 28);
        double dx = cx - tx, dz = cz - tz, len = Math.hypot(dx, dz);
        double ex = cx + dx / len * back, ez = cz + dz / len * back;
        double ey = open ? G + 16 : G + Math.min(14 + tall * 0.25, 40);
        look(player, level, ex, ey, ez, tx, ty, tz);
    }

    /** Inside a crumbled building, on the second floor, looking out where the wall fell away. */
    private static void interior(ServerPlayer player, ServerLevel overworld, int nth) {
        V10Tour.clean(player);
        time(overworld, 2000);
        ServerLevel level = city(overworld);
        Lot lot = find(level.getSeed(), Kind.CRUMBLED, nth);
        double tx = (lot.worldX(lot.x0()) + lot.worldX(lot.x1())) / 2.0 + 0.5, tz = (lot.worldZ(lot.z0()) + lot.worldZ(lot.z1())) / 2.0 + 0.5;
        double a = lot.collapseAngle() + Math.PI;
        double ex = tx + Math.cos(a) * 4, ez = tz + Math.sin(a) * 4, ey = G + AbandonedCityLevel.FH + 1.7;
        look(player, level, ex, ey, ez, tx - Math.cos(a) * 30, ey + 5, tz - Math.sin(a) * 30);
    }

    /** In the street in front of an entrance with a red exit door. */
    private static void exitDoor(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        time(overworld, 2000);
        ServerLevel level = city(overworld);
        long seed = level.getSeed();
        for (int ring = 0; ring < 10; ring++) {
            for (int cx = -ring; cx <= ring; cx++) {
                for (int cz = -ring; cz <= ring; cz++) {
                    if (Math.max(Math.abs(cx), Math.abs(cz)) != ring) continue;
                    Lot lot = gen().lot(seed, cx, cz);
                    if (!AbandonedCityLevel.hasExit(seed, lot) || lot.holeR() > 0 || lot.kind() != Kind.APARTMENT) continue;
                    double x = lot.worldX(lot.x0() + (lot.x1() - lot.x0()) / 2) + 0.5, z = lot.worldZ(lot.z0());
                    look(player, level, x - 3, G + 3.2, z - 8, x, G + 1.2, z);
                    return;
                }
            }
        }
    }

    /** Standing on an apartment roof, looking out over the city. */
    private static void rooftop(ServerPlayer player, ServerLevel overworld, int nth) {
        V10Tour.clean(player);
        time(overworld, 1600);
        ServerLevel level = city(overworld);
        Lot lot = find(level.getSeed(), Kind.APARTMENT, nth + 2);
        double ex = lot.worldX(lot.x1()) + 0.5, ez = lot.worldZ(lot.z1()) + 0.5, ey = lot.top() + 7.5;
        look(player, level, ex, ey, ez, ex + 90, ey - 14, ez + 70);
    }

    /** A giant tree whose trunk came up through a building. */
    private static void treeInBuilding(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        time(overworld, 2000);
        ServerLevel level = city(overworld);
        long seed = level.getSeed();
        for (int ring = 0; ring < 10; ring++) {
            for (int cx = -ring; cx <= ring; cx++) {
                for (int cz = -ring; cz <= ring; cz++) {
                    if (Math.max(Math.abs(cx), Math.abs(cz)) != ring) continue;
                    for (Tree tree : gen().trees(seed, cx, cz)) {
                        Lot lot = gen().lot(seed, Math.floorDiv(tree.x(), CELL), Math.floorDiv(tree.z(), CELL));
                        int ax = Math.floorMod(tree.x(), CELL) - AbandonedCityLevel.STREET, az = Math.floorMod(tree.z(), CELL) - AbandonedCityLevel.STREET;
                        boolean building = lot.kind() == Kind.APARTMENT || lot.kind() == Kind.CRUMBLED || lot.kind() == Kind.LOWRISE;
                        if (!building || ax < lot.x0() + 2 || ax > lot.x1() - 2 || az < lot.z0() + 2 || az > lot.z1() - 2) continue;
                        look(player, level, tree.x() - 24, lot.top() + 16, tree.z() - 20, tree.x() + 0.5, lot.top() + 2, tree.z() + 0.5);
                        return;
                    }
                }
            }
        }
        LOGGER_FALLBACK(player, level);
    }

    private static void LOGGER_FALLBACK(ServerPlayer player, ServerLevel level) {
        look(player, level, 0, G + 30, 0, 50, G, 50);
    }

    private static void tree(ServerPlayer player, ServerLevel overworld, int nth, boolean up) {
        V10Tour.clean(player);
        time(overworld, 2000);
        ServerLevel level = city(overworld);
        Tree tree = findTree(level.getSeed(), nth);
        if (up) {
            look(player, level, tree.x() + 9.5, G + 2.5, tree.z() + 7.5, tree.x() + 0.5, G + tree.height() * 0.75, tree.z() + 0.5);
        } else {
            Lot park = find(level.getSeed(), Kind.PARK, nth);
            Tree parkTree = gen().trees(level.getSeed(), park.cx(), park.cz()).get(0);
            look(player, level, parkTree.x() - 38, G + 16, parkTree.z() - 30, parkTree.x() + 0.5, G + parkTree.height() * 0.5, parkTree.z() + 0.5);
        }
    }
}
