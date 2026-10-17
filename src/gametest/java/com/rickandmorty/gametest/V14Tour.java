package com.rickandmorty.gametest;

import com.rickandmorty.entity.SeatEntity;
import com.rickandmorty.entity.StaffEntity;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.InfiniteIkeaLevel;
import com.rickandmorty.worldgen.level.InfiniteIkeaLevel.Department;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.function.BiConsumer;

/** The Infinite IKEA tour (./gradlew runTourClient -DtourV14=true). */
final class V14Tour {
    private static final int C = InfiniteIkeaLevel.CELL, F = InfiniteIkeaLevel.FLOOR;

    private V14Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("ikea_walkway", V14Tour::walkway);
        stops.accept("ikea_looking_up", V14Tour::lookingUp);
        stops.accept("ikea_pillars_from_above", (p, l) -> {
            V10Tour.clean(p);
            ServerLevel level = ikea(l);
            DevTour.go(p, level, 760 * C + 0.5, F + 30, 760 * C + 0.5, -45.0f, 35.0f);
        });
        for (Department d : Department.values()) stops.accept("ikea_" + d.name().toLowerCase(), (p, l) -> department(p, l, d));
        stops.accept("ikea_staff_lineup", (p, l) -> staff(p, l, false));
        stops.accept("ikea_staff_after_closing", (p, l) -> staff(p, l, true));
        stops.accept("ikea_sitting_front", V14Tour::sitting);
        stops.accept("store_in_the_overworld", V14Tour::store);
    }

    private static ServerLevel ikea(ServerLevel overworld) {
        return overworld.getServer().getLevel(BackroomsLevel.INFINITE_IKEA.key());
    }

    /** The nearest cell of a department to (40, 40) in cells. */
    private static int[] find(long seed, Department department) {
        for (int ring = 0; ring < 120; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int cx = 720 + dx, cz = 720 + dz;
                    if (InfiniteIkeaLevel.department(seed, cx, cz) == department) return new int[]{cx, cz};
                }
            }
        }
        throw new IllegalStateException("no " + department);
    }

    private static void walkway(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level = ikea(overworld);
        long seed = level.getSeed();
        int cx = 720, cz = 720;
        while (!InfiniteIkeaLevel.hasSign(seed, cx, cz)) cx++;
        DevTour.go(player, level, cx * C + 2.5, F + 1, cz * C + 14.5, 180.0f, -12.0f);
    }

    /** Next to a pillar, looking up at where the roof must be. */
    private static void lookingUp(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level = ikea(overworld);
        int[] cell = find(level.getSeed(), Department.LIVING);
        DevTour.go(player, level, cell[0] * C + 5 + 9.5, F + 1, cell[1] * C + 5 + 9.5, -45.0f, -55.0f);
    }

    /** From above the walkway corner, looking into the department. */
    private static void department(ServerPlayer player, ServerLevel overworld, Department department) {
        V10Tour.clean(player);
        ServerLevel level = ikea(overworld);
        int[] cell = find(level.getSeed(), department);
        if (department.hall()) {
            int x = Math.floorDiv(cell[0], 2) * 2 * C, z = Math.floorDiv(cell[1], 2) * 2 * C;
            DevTour.go(player, level, x + 2.5, F + 16, z + 2.5, -45.0f, 30.0f);
            return;
        }
        DevTour.go(player, level, cell[0] * C + 2.5, F + 7, cell[1] * C + 2.5, -45.0f, 28.0f);
    }

    private static void staff(ServerPlayer player, ServerLevel overworld, boolean night) {
        V10Tour.clean(player);
        ServerLevel level = ikea(overworld);
        overworld.setDayTime(night ? 18000 : 6000);
        int[] cell = find(level.getSeed(), Department.LIVING);
        int x = cell[0] * C + 2, z = cell[1] * C + 12;
        level.getEntitiesOfClass(StaffEntity.class, new net.minecraft.world.phys.AABB(new BlockPos(x, F, z)).inflate(30)).forEach(StaffEntity::discard);
        for (int i = 0; i < 3; i++) {
            StaffEntity staff = ModEntities.STAFF.get().create(level);
            staff.moveTo(x - 1.5 + i * 2.0 + 0.5, F + 1, z + (night ? i % 2 : 0) + 0.5, 0.0f, 0.0f);
            staff.setVariant(i);
            staff.setNoAi(true);
            staff.setYHeadRot(0.0f);
            staff.setYBodyRot(0.0f);
            staff.setAggressive(night);
            level.addFreshEntity(staff);
        }
        if (night) level.setBlock(new BlockPos(x + 3, F + 1, z + 2), com.rickandmorty.registry.ModBlocks.FLOOR_LAMP.get().defaultBlockState(), 3);
        DevTour.go(player, level, x + 0.5, F + 1.5, z + 6.5, 180.0f, -8.0f);
    }

    private static void sitting(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level = ikea(overworld);
        overworld.setDayTime(6000);
        int[] cell = find(level.getSeed(), Department.LIVING);
        BlockPos sofa = new BlockPos(cell[0] * C + 5 + 5, F + 1, cell[1] * C + 5 + 2);
        DevTour.go(player, level, sofa.getX() + 0.5, sofa.getY() + 1, sofa.getZ() + 0.5, 0.0f, 0.0f);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 10, () -> {
            player.getAbilities().flying = false;
            SeatEntity seat = new SeatEntity(level, sofa, 0.4);
            level.addFreshEntity(seat);
            player.startRiding(seat, true);
        }));
    }

    private static void store(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        overworld.setDayTime(6000);
        // (the flat tour world has no spot the real placement likes, so build one by hand)
        int x = 6000, z = 6000;
        overworld.getChunk(x >> 4, z >> 4);
        BlockPos at = new BlockPos(x, overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) overworld.getChunk((x >> 4) + cx, (z >> 4) + cz);
        }
        new com.rickandmorty.worldgen.site.SitePiece(com.rickandmorty.worldgen.site.SiteKind.STORE, at, 77L).postProcess(overworld,
                overworld.structureManager(), overworld.getChunkSource().getGenerator(), overworld.random,
                new net.minecraft.world.level.levelgen.structure.BoundingBox(x - 40, overworld.getMinBuildHeight(), z - 40, x + 40,
                        overworld.getMaxBuildHeight(), z + 40), new net.minecraft.world.level.ChunkPos(at), at);
        DevTour.go(player, overworld, at.getX() + 6.5, at.getY() + 7, at.getZ() - 34.5, 0.0f, 8.0f);
    }
}
