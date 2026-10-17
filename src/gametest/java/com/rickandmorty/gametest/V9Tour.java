package com.rickandmorty.gametest;

import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.PoolroomsLevel;
import com.rickandmorty.worldgen.site.SiteKind;
import com.rickandmorty.worldgen.site.SitePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.function.BiConsumer;

/** The v0.9 tour (./gradlew runTourClient -DtourV09=true). */
final class V9Tour {
    private static final int S = PoolroomsLevel.SURFACE;
    private static BlockPos monolith;

    private V9Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> clean(p));
        for (PoolroomsLevel.Room room : PoolroomsLevel.Room.values()) {
            stops.accept("poolrooms_" + room.name().toLowerCase(), (p, l) -> poolroom(p, l, room));
        }
        stops.accept("monolith_far", (p, l) -> monolith(p, l, false));
        stops.accept("monolith_base", (p, l) -> monolith(p, l, true));
        stops.accept("monolith_door_to_level0", V9Tour::throughTheDoor);
        stops.accept("scarecrow_head_front", V9Tour::wearHead);
        stops.accept("scarecrow_lantern_hand", V9Tour::holdLantern);
        stops.accept("watcher_overworld", V9Tour::watcher);
    }

    private static void clean(ServerPlayer player) {
        com.rickandmorty.cronenberg.Mutation.set(player, 0.0f);
        player.removeAllEffects();
        player.getInventory().clearContent();
    }

    /** Stand at (x, y, z) looking at (tx, ty, tz). */
    private static void look(ServerPlayer player, ServerLevel level, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - (y + 1.62), dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        DevTour.go(player, level, x, y, z, yaw, pitch);
    }

    private static void poolroom(ServerPlayer player, ServerLevel overworld, PoolroomsLevel.Room room) {
        clean(player);
        ServerLevel level = overworld.getServer().getLevel(BackroomsLevel.POOLROOMS.key());
        int[] cell = PoolroomsLevel.find(level.getSeed(), room, 3, 3);
        double x0 = cell[0], z0 = cell[1];
        switch (room) {
            case PILLARS -> look(player, level, x0 + 1.5, S + 0.6, z0 + 1.5, x0 + 30, S + 2, z0 + 30);
            case TERRACES -> look(player, level, x0 + 6.5, S + 3.5, z0 + 6.5, x0 + 26, S, z0 + 26);
            case DOME -> look(player, level, x0 + 20.5, S + 2, z0 + 35.5, x0 + 20.5, S + 6, z0 + 14);
            case ABYSS -> look(player, level, x0 + 8.5, S + 3, z0 + 32.5, x0 + 28, S + 4, z0 + 12);
            case SLIDES -> look(player, level, x0 + 30.5, S + 3, z0 + 9.5, x0 + 18, S + 3, z0 + 24);
            case STAIRWELL -> look(player, level, x0 + 11.5, S + 1, z0 + 29.5, x0 + 20, S + 8, z0 + 20);
            case LOUNGE -> look(player, level, x0 + 30.5, S + 3, z0 + 36.5, x0 + 10, S + 6, z0 + 2);
            case MISTY -> look(player, level, x0 + 4.5, S + 0.8, z0 + 20.5, x0 + 36, S + 2, z0 + 24);
            case TUNNELS -> look(player, level, x0 + 10.5, S + 0.5, z0 + 3.5, x0 + 10.5, S + 1.5, z0 + 30);
        }
    }

    private static void buildMonolith(ServerLevel overworld) {
        if (monolith != null) return;
        int x = 2400, z = 2400;
        overworld.getChunk(x >> 4, z >> 4);
        monolith = new BlockPos(x, overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) overworld.getChunk((x >> 4) + cx, (z >> 4) + cz);
        }
        SitePiece piece = new SitePiece(SiteKind.MONOLITH, monolith, 4242L);
        BoundingBox everywhere = new BoundingBox(x - 40, overworld.getMinBuildHeight(), z - 40, x + 40, overworld.getMaxBuildHeight(), z + 40);
        piece.postProcess(overworld, overworld.structureManager(), overworld.getChunkSource().getGenerator(), RandomSource.create(1),
                everywhere, new ChunkPos(monolith), monolith);
    }

    private static void monolith(ServerPlayer player, ServerLevel overworld, boolean close) {
        clean(player);
        buildMonolith(overworld);
        overworld.setDayTime(10500);   // late afternoon
        BlockPos m = monolith;
        if (close) look(player, overworld, m.getX() + 9.5, m.getY() + 2, m.getZ() + 15.5, m.getX(), m.getY() + 3, m.getZ() + 4);
        else look(player, overworld, m.getX() - 26.5, m.getY() + 2, m.getZ() + 58.5, m.getX() + 4, m.getY() + 50, m.getZ());
    }

    private static void throughTheDoor(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        player.setPortalCooldown(0);
        com.rickandmorty.worldgen.level.LevelTravel.throughMonolithDoor(player, monolith == null ? BlockPos.ZERO : monolith);
        player.removeAllEffects();
        // stand back a little and look up at the tower they came out of
        DevTour.go(player, player.serverLevel(), player.getX(), player.getY(), player.getZ() + 6, 180.0f, -25.0f);
    }

    private static BlockPos ground(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
    }

    private static void wearHead(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SCARECROW_HEAD.get()));
        overworld.setDayTime(6000);
        BlockPos at = ground(overworld, 2300, 2330);
        DevTour.go(player, overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0f, 0.0f);
        player.getAbilities().flying = false;
    }

    private static void holdLantern(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARECROW_LANTERN.get()));
        ServerLevel harvest = overworld.getServer().getLevel(BackroomsLevel.HARVEST.key());
        DevTour.go(player, harvest, 12.5, 2, 12.5, 0.0f, 5.0f);
        DevTour.LATER.add(java.util.Map.entry(DevTour.ticks + 30, () -> {
            for (int i = 0; i < 3; i++) {
                var scarecrow = ModEntities.SCARECROW.get().create(harvest);
                scarecrow.moveTo(10.5 + i * 2.5, 2, 20.5 + i, 180.0f, 0.0f);
                harvest.addFreshEntity(scarecrow);
            }
        }));
    }

    private static void watcher(ServerPlayer player, ServerLevel overworld) {
        clean(player);
        overworld.setDayTime(11500);
        BlockPos at = ground(overworld, 2200, 2200);
        ground(overworld, 2200, 2170);
        DevTour.go(player, overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0f, 0.0f);
        DevTour.LATER.add(java.util.Map.entry(DevTour.ticks + 20, () -> {
            var watcher = ModEntities.WATCHER.get().create(overworld);
            watcher.moveTo(at.getX() + 8.5, at.getY(), at.getZ() - 18.5, 0.0f, 0.0f);   // off to one side, or it would notice being looked at and vanish
            overworld.addFreshEntity(watcher);
        }));
    }
}
