package com.rickandmorty.gametest;

import com.rickandmorty.block.ExitDoorBlock;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.worldgen.BackroomsLayout;
import com.rickandmorty.worldgen.BackroomsZones;
import com.rickandmorty.worldgen.ModDimensions;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelGenerator;
import com.rickandmorty.worldgen.level.LevelGenerators;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** The tour of the Backrooms levels (./gradlew runTourClient -DtourLevels=true). */
final class LevelTour {
    private LevelTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("level0_classic", (p, l) -> level0(p, l, BackroomsZones.Zone.CLASSIC));
        stops.accept("level0_kane", (p, l) -> level0(p, l, BackroomsZones.Zone.KANE));
        stops.accept("level0_open_hall", (p, l) -> level0(p, l, BackroomsZones.Zone.OPEN_HALL));
        stops.accept("level0_pits", (p, l) -> level0(p, l, BackroomsZones.Zone.PITS));
        stops.accept("level0_monolith", (p, l) -> level0(p, l, BackroomsZones.Zone.MONOLITH));
        stops.accept("level0_hole", LevelTour::hole);
        stops.accept("level1_garage", (p, l) -> landing(p, l, BackroomsLevel.LEVEL_1, 0));
        stops.accept("level4_office", (p, l) -> landing(p, l, BackroomsLevel.LEVEL_4, 0));
        stops.accept("level4_office_b", (p, l) -> landing(p, l, BackroomsLevel.LEVEL_4, 1));
        stops.accept("level188_courtyard", LevelTour::courtyard);
        stops.accept("harvest_field", LevelTour::harvest);
        stops.accept("harvest_farmhouse", (p, l) -> door(p, l, BackroomsLevel.HARVEST, 18, 8));
        stops.accept("sunlit_lobby", LevelTour::lobby);
        stops.accept("wooden_hotel", LevelTour::hotel);
        stops.accept("scaffold_void", (p, l) -> landing(p, l, BackroomsLevel.SCAFFOLD, 2));
        stops.accept("glowstone_cavern", (p, l) -> landing(p, l, BackroomsLevel.CAVERN, 3));
        stops.accept("cavern_castle", (p, l) -> door(p, l, BackroomsLevel.CAVERN, 26, 10));
    }

    private static ServerLevel level(ServerLevel overworld, BackroomsLevel level) {
        ServerLevel dim = overworld.getServer().getLevel(level.key());
        if (dim == null) throw new IllegalStateException("no " + level);
        return dim;
    }

    private static void go(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        // no leftover mutation (its night vision would wash out every level's darkness)
        com.rickandmorty.cronenberg.Mutation.set(player, 0.0f);
        player.removeAllEffects();
        DevTour.go(player, level, x, y, z, yaw, pitch);
    }

    // ------------------------------------------------------------------ Level 0

    private static int[] findZone(long seed, BackroomsZones.Zone zone) {
        for (int ring = 1; ring < 60; ring++) {
            for (int zx = -ring; zx <= ring; zx++) {
                for (int zz = -ring; zz <= ring; zz++) {
                    if (Math.max(Math.abs(zx), Math.abs(zz)) != ring) continue;
                    if (BackroomsZones.zoneAt(seed, zx * BackroomsZones.ZONE + 36, zz * BackroomsZones.ZONE + 36) == zone) return new int[]{zx, zz};
                }
            }
        }
        throw new IllegalStateException("no " + zone + " zone");
    }

    private static void level0(ServerPlayer player, ServerLevel overworld, BackroomsZones.Zone zone) {
        ServerLevel level = overworld.getServer().getLevel(ModDimensions.BACKROOMS);
        long seed = level.getSeed();
        if (zone == BackroomsZones.Zone.CLASSIC) {
            var d = ModDimensions.randomDestination(level.getServer(), ModDimensions.BACKROOMS, level.random).orElseThrow();
            go(player, level, d.x(), d.y(), d.z(), d.yaw(), 0.0f);
            return;
        }
        int[] z = findZone(seed, zone);
        int x0 = z[0] * BackroomsZones.ZONE, z0 = z[1] * BackroomsZones.ZONE;
        switch (zone) {
            // the ceiling steps up and down along this walk
            case KANE -> standIn(player, level, seed, x0 + 8, z0 + 8, -45.0f, 4.0f);
            case OPEN_HALL -> standIn(player, level, seed, x0 + 6, z0 + 6, -45.0f, 2.0f);
            // on a walkway between the pits, looking down into them
            case PITS -> go(player, level, x0 + 11.5, 2.0, z0 + 6.5, -30.0f, 38.0f);
            // across the hall at the two towers, looking up
            case MONOLITH -> go(player, level, x0 + 35.5, 2.0, z0 + 6.5, 0.0f, -22.0f);
            default -> {
            }
        }
    }

    private static void standIn(ServerPlayer player, ServerLevel level, long seed, int x, int z, float yaw, float pitch) {
        for (int d = 0; d < 20; d++) {
            if (BackroomsZones.standable(seed, x + d, z + d)) {
                go(player, level, x + d + 0.5, 2.0, z + d + 0.5, yaw, pitch);
                return;
            }
        }
        go(player, level, x + 0.5, 2.0, z + 0.5, yaw, pitch);
    }

    private static void hole(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = overworld.getServer().getLevel(ModDimensions.BACKROOMS);
        long seed = level.getSeed();
        int[] hole = null;
        int best = Integer.MAX_VALUE;
        for (int rx = -12; rx < 12; rx++) {
            for (int rz = -12; rz < 12; rz++) {
                int[] h = BackroomsZones.hole(seed, rx, rz);
                if (h != null && Math.abs(h[0]) + Math.abs(h[1]) < best) {
                    hole = h;
                    best = Math.abs(h[0]) + Math.abs(h[1]);
                }
            }
        }
        if (hole == null) throw new IllegalStateException("no Hole");
        // standing in the next cell east, looking west at it
        go(player, level, hole[0] + 3.5, BackroomsLayout.standY(0), hole[1] + 0.5, 90.0f, 0.0f);
    }

    // ------------------------------------------------------------------ the other levels

    private static void landing(ServerPlayer player, ServerLevel overworld, BackroomsLevel which, int salt) {
        ServerLevel level = level(overworld, which);
        LevelGenerator generator = LevelGenerators.get(which);
        Vec3 at = generator.landing(level.getSeed() + salt, net.minecraft.util.RandomSource.create(which.ordinal() * 31L + salt));
        Integer y = generator.standY(level.getSeed(), (int) Math.floor(at.x), (int) Math.floor(at.z), (int) at.y);
        go(player, level, at.x, y == null ? at.y : y, at.z, 45.0f + salt * 70, which == BackroomsLevel.SCAFFOLD ? 20.0f : 5.0f);
        if (which == BackroomsLevel.CAVERN) player.connection.teleport(at.x, (y == null ? at.y : y) + 6, at.z, 45.0f, 12.0f);
    }

    /** Somewhere near an exit door, standing {@code back} blocks in front of it and {@code up} blocks up. */
    private static void door(ServerPlayer player, ServerLevel overworld, BackroomsLevel which, int back, int up) {
        ServerLevel level = level(overworld, which);
        LevelGenerator generator = LevelGenerators.get(which);
        long seed = level.getSeed();
        int[] found = find(generator, seed, s -> s.getBlock() instanceof ExitDoorBlock, 900, 2);
        BlockState door = generator.column(seed, found[0], found[2])[found[1]];
        var facing = door.getValue(net.minecraft.world.level.block.DoorBlock.FACING);
        double x = found[0] + 0.5 + facing.getStepX() * back, z = found[2] + 0.5 + facing.getStepZ() * back;
        go(player, level, x, found[1] + up, z, facing.getOpposite().toYRot(), (float) Math.toDegrees(Math.atan2(up - 1, back)));
    }

    private static int[] find(LevelGenerator generator, long seed, Predicate<BlockState> what, int range, int stride) {
        for (int r = 0; r < range; r += 16) {
            for (int x = -r; x <= r; x += stride) {
                for (int z = -r; z <= r; z += stride) {
                    if (Math.max(Math.abs(x), Math.abs(z)) < r - 16) continue;
                    BlockState[] c = generator.column(seed, x, z);
                    for (int y = 0; y < c.length; y++) {
                        if (c[y] != null && what.test(c[y])) return new int[]{x, y, z};
                    }
                }
            }
        }
        throw new IllegalStateException("nothing found");
    }

    private static void courtyard(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld, BackroomsLevel.LEVEL_188);
        // standing in a courtyard, looking up at the far wall of windows
        go(player, level, 72 + 36.5, 3.0, 72 + 44.5, 180.0f, -32.0f);
    }

    private static void harvest(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld, BackroomsLevel.HARVEST);
        LevelGenerator generator = LevelGenerators.get(BackroomsLevel.HARVEST);
        long seed = level.getSeed();
        int[] path = find(generator, seed, s -> s.is(net.minecraft.world.level.block.Blocks.SMOOTH_SANDSTONE), 400, 1);
        int y = path[1] + 1;
        go(player, level, path[0] + 0.5, y + 1.5, path[2] + 0.5, -60.0f, 12.0f);
        // three scarecrows out in the corn, where you're looking
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(-60.0 + (i - 1) * 18), d = 7 + i * 4;
            int sx = (int) Math.floor(path[0] + 0.5 - Math.sin(a) * d), sz = (int) Math.floor(path[2] + 0.5 + Math.cos(a) * d);
            Integer sy = generator.standY(seed, sx, sz, y);
            spawn(level, ModEntities.SCARECROW.get(), sx + 0.5, sy == null ? y : sy, sz + 0.5, (float) Math.toDegrees(Math.atan2(path[0] - sx, sz - path[2])));
        }
    }

    private static void lobby(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld, BackroomsLevel.SUNLIT_LOBBY);
        // in the hall, looking across at the tall windows and the light coming through them
        go(player, level, 30.5, 1.0 + 1, 22 * 3 + 5.5, -20.0f, -8.0f);
    }

    private static void hotel(ServerPlayer player, ServerLevel overworld) {
        ServerLevel level = level(overworld, BackroomsLevel.WOODEN_HOTEL);
        int storey = 6, y = 4 + 6 * storey + 1;
        // on a balcony, looking across the shaft at the balconies opposite (and whoever's on them)
        go(player, level, 44 + 8.5, y + 0.6, 44 + 20.5, -90.0f, 8.0f);
        spawn(level, ModEntities.WATCHER.get(), 44 + 35.5, y, 44 + 30.5, 90.0f);    // off to one side, so we're not staring at it
    }

    private static <T extends Mob> void spawn(ServerLevel level, EntityType<T> type, double x, double y, double z, float yaw) {
        level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        T mob = type.create(level);
        if (mob == null) return;
        mob.moveTo(x, y, z, yaw, 0.0f);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }
}
