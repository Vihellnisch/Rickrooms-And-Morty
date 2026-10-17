package com.rickandmorty.gametest;

import com.mojang.logging.LogUtils;
import com.rickandmorty.block.entity.ExitDoorBlockEntity;
import com.rickandmorty.entity.cruiser.CruiserTier;
import com.rickandmorty.entity.cruiser.CruiserUpgrade;
import com.rickandmorty.entity.cruiser.CruiserWeapons;
import com.rickandmorty.entity.cruiser.SpaceCruiserEntity;
import com.rickandmorty.worldgen.ModDimensions;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** The drill marks and the way to Level Fun (./gradlew runTourClient -DtourV13=true). */
final class V13Tour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static BlockPos funDoor;

    private V13Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("cruiser_drill_mk1_back", (p, l) -> drill(p, l, 1));
        stops.accept("cruiser_drill_mk2_back", (p, l) -> drill(p, l, 2));
        stops.accept("cruiser_drill_mk3_back", (p, l) -> drill(p, l, 3));
        stops.accept("level1_door_to_level_fun", V13Tour::survey);
        stops.accept("level_fun_through_the_door", V13Tour::throughTheDoor);
    }

    /** From behind the cruiser: one shot of the drill into a wall of stone and obsidian. */
    private static void drill(ServerPlayer player, ServerLevel overworld, int mark) {
        SpaceCruiserEntity cruiser = V11Tour.ride(player, overworld, 13 + mark, false);
        cruiser.setTier(CruiserTier.DRILL, mark);
        BlockPos wall = cruiser.blockPosition().north(6);
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 8; y++) {
                overworld.setBlock(wall.offset(x, y, 0), (x + y) % 3 == 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                overworld.setBlock(wall.offset(x, y, -1), Blocks.WHITE_CONCRETE.defaultBlockState(), 2);
            }
        }
        Vec3 target = Vec3.atCenterOf(wall.above(3)).add(0, 0, 0.5);
        for (int t = 30; t <= 60; t += 6) {
            DevTour.LATER.add(Map.entry(DevTour.ticks + t, () -> {
                cruiser.setDeltaMovement(Vec3.ZERO);
                CruiserWeapons.shoot(cruiser, player, CruiserUpgrade.LASER_DRILL, target.subtract(cruiser.muzzle()).normalize());
            }));
        }
    }

    /** Level 1: where do its exit doors go? Then stand in front of one that leads to Level Fun. */
    private static void survey(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level1 = overworld.getServer().getLevel(BackroomsLevel.LEVEL_1.key());
        var landing = ModDimensions.randomDestination(overworld.getServer(), BackroomsLevel.LEVEL_1.key(), player.getRandom()).orElseThrow();
        int cx = BlockPos.containing(landing.x(), 0, landing.z()).getX() >> 4, cz = BlockPos.containing(landing.x(), 0, landing.z()).getZ() >> 4;
        Map<BackroomsLevel, Integer> counts = new EnumMap<>(BackroomsLevel.class);
        int home = 0;
        BlockPos best = null;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                var chunk = level1.getChunk(cx + dx, cz + dz);
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    if (!(entry.getValue() instanceof ExitDoorBlockEntity door)) continue;
                    var state = level1.getBlockState(entry.getKey());
                    if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) continue;
                    if (door.destination() == null) home++;
                    else counts.merge(door.destination(), 1, Integer::sum);
                    if (door.destination() == BackroomsLevel.LEVEL_FUN && best == null) best = entry.getKey().immutable();
                }
            }
        }
        LOGGER.info("TOUR Level 1 exit doors in 21x21 chunks: {} (home: {}); a Level Fun door at {}", counts, home, best);
        funDoor = best;
        if (best == null) {
            DevTour.go(player, level1, landing.x(), landing.y(), landing.z(), landing.yaw(), 0.0f);
            return;
        }
        var facing = level1.getBlockState(best).getValue(DoorBlock.FACING);
        Vec3 stand = Vec3.atBottomCenterOf(best).add(facing.getStepX() * 3.5, 0, facing.getStepZ() * 3.5);
        Vec3 to = Vec3.atBottomCenterOf(best).subtract(stand);
        DevTour.go(player, level1, stand.x, stand.y, stand.z, (float) Math.toDegrees(Math.atan2(-to.x, to.z)), 5.0f);
    }

    private static void throughTheDoor(ServerPlayer player, ServerLevel overworld) {
        if (funDoor == null) {
            LOGGER.error("TOUR no Level Fun door found");
            return;
        }
        ServerLevel level1 = overworld.getServer().getLevel(BackroomsLevel.LEVEL_1.key());
        if (level1.getBlockEntity(funDoor) instanceof ExitDoorBlockEntity door) {
            player.setPortalCooldown(0);
            LevelTravel.throughExitDoor(player, funDoor, door.destination());
            LOGGER.info("TOUR through the door: now in {} at {}", player.level().dimension().location(), player.position());
        }
    }
}
