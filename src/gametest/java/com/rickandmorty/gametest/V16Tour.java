package com.rickandmorty.gametest;

import com.rickandmorty.command.GotoCommand;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.EndlessFieldLevel;
import com.rickandmorty.worldgen.level.EndlessFieldLevel.Feature;
import com.rickandmorty.worldgen.level.EndlessFieldLevel.Placed;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/** The Endless Field tour (./gradlew runTourClient -DtourV16=true). */
final class V16Tour {
    private V16Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("field_by_command", (p, l) -> {
            V10Tour.clean(p);
            GotoCommand.go(p, "endless_field");
        });
        stops.accept("field_hilltop", V16Tour::hilltop);
        for (Feature f : Feature.values()) stops.accept("field_" + f.name().toLowerCase(), (p, l) -> feature(p, l, f));
        stops.accept("field_stairwell_inside", V16Tour::inside);
    }

    private static ServerLevel field(ServerLevel overworld) {
        return overworld.getServer().getLevel(BackroomsLevel.ENDLESS_FIELD.key());
    }

    private static Placed find(long seed, Feature feature) {
        for (int ring = 0; ring < 80; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    Placed p = EndlessFieldLevel.placed(seed, 200 + dx, 200 + dz);
                    if (p != null && p.feature() == feature) return p;
                }
            }
        }
        throw new IllegalStateException("no " + feature);
    }

    private static Vec3 local(Placed p, double x, double y, double z) {
        Rotation r = Rotation.values()[p.rotation()];
        BlockPos q = new BlockPos((int) Math.round(x * 16), 0, (int) Math.round(z * 16)).rotate(r);
        return new Vec3(p.x() + 0.5 + q.getX() / 16.0, p.y0() + y, p.z() + 0.5 + q.getZ() / 16.0);
    }

    private static void look(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 at) {
        Vec3 d = at.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        DevTour.go(player, level, eye.x, eye.y - 1.62, eye.z, yaw, pitch);
    }

    private static void hilltop(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level = field(overworld);
        long seed = level.getSeed();
        int bx = 19000, bz = 19000, best = -1;
        for (int x = 19000; x < 19600; x += 8) {
            for (int z = 19000; z < 19600; z += 8) {
                int g = EndlessFieldLevel.groundAt(seed, x, z);
                if (g > best) {
                    best = g;
                    bx = x;
                    bz = z;
                }
            }
        }
        DevTour.go(player, level, bx + 0.5, best + 1, bz + 0.5, 30.0f, 4.0f);
    }

    private static void feature(ServerPlayer player, ServerLevel overworld, Feature f) {
        V10Tour.clean(player);
        ServerLevel level = field(overworld);
        Placed p = find(level.getSeed(), f);
        double[] view = switch (f) {           // distance in front, height, and what to look at (local x, y, z)
            case TREE -> new double[]{14, 2, 0, 4, 0};
            case HOUSE -> new double[]{17, 3, 0, 4, 0};
            case SUNKEN_HOUSE -> new double[]{24, 9, 0, 3, 0};
            case WHITE_BOX -> new double[]{22, 3, 0, 4, 0};
            case DOOR -> new double[]{7, 1.6, -0.5, 1.5, 0};
            case PLAYGROUND -> new double[]{13, 2, 2, 2, 0};
            case PICNIC -> new double[]{5, 2, 0, 1, 0};
            case POOL -> new double[]{12, 4, 0, 0, 0};
            case POND -> new double[]{18, 4, 4, 1, 0};
            case VOID_DOOR -> new double[]{11, 2, 0, 3, 0};
            case STAIRWELL -> new double[]{12, 5, -0.5, -2, 3};
            case WATER_TOWER -> new double[]{20, 3, 0, 9, 0};
            case LADDER -> new double[]{12, 2, 0, 7, 0};
            case BARN -> new double[]{24, 4, 0, 4, 0};
        };
        look(player, level, local(p, 0, view[1] + 1.62, view[0]), local(p, view[2], view[3], view[4]));
    }

    /** Halfway down the stairwell, on the landing, looking down the second flight. */
    private static void inside(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level = field(overworld);
        Placed p = find(level.getSeed(), Feature.STAIRWELL);
        look(player, level, local(p, 3.5, -11 + 1.62, -6.5), local(p, 3.5, -18, 4));
    }
}
