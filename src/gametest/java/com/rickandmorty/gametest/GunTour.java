package com.rickandmorty.gametest;

import com.rickandmorty.entity.PortalEntity;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.worldgen.ModDimensions;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.function.BiConsumer;

/** The Portal Gun as it is now, in Level 0 (./gradlew runTourClient -DtourGun=true): in your hand, a portal open down the hall. */
final class GunTour {
    private GunTour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("gun_warmup", GunTour::hold);
        stops.accept("gun_warmup_2", GunTour::hold);
        stops.accept("gun_portal_bare", GunTour::hold);
        stops.accept("gun_portal_back", GunTour::hold);
    }

    /** A spot in Level 0 with eight clear blocks south of it, three wide: somewhere to open a portal. */
    private static Vec3 openHall(ServerLevel level) {
        for (int attempt = 0; attempt < 400; attempt++) {
            var dest = ModDimensions.randomDestination(level.getServer(), BackroomsLevel.LEVEL_0.key(), level.random).orElseThrow();
            BlockPos feet = BlockPos.containing(dest.x(), dest.y(), dest.z());
            level.getChunk(feet.getX() >> 4, feet.getZ() >> 4);
            level.getChunk(feet.getX() >> 4, (feet.getZ() + 9) >> 4);
            boolean clear = true;
            for (int dz = 0; dz <= 8 && clear; dz++) {
                for (int dx = -1; dx <= 1 && clear; dx++) {
                    for (int dy = 0; dy <= 2 && clear; dy++) clear = level.getBlockState(feet.offset(dx, dy, dz)).isAir();
                }
            }
            if (clear) return Vec3.atBottomCenterOf(feet);
        }
        throw new IllegalStateException("no open hall in Level 0");
    }

    private static Vec3 spot;

    private static void hold(ServerPlayer player, ServerLevel overworld) {
        V10Tour.clean(player);
        ServerLevel level = overworld.getServer().getLevel(BackroomsLevel.LEVEL_0.key());
        if (spot == null) spot = openHall(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, PortalGunItem.fullyUpgraded(player.server));
        DevTour.go(player, level, spot.x, spot.y, spot.z, 0.0f, 6.0f);
        DevTour.LATER.add(Map.entry(DevTour.ticks + 40, () -> {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            level.getEntitiesOfClass(PortalEntity.class, player.getBoundingBox().inflate(20)).forEach(PortalEntity::discard);
            // yaw 0 faces south (+z): open it five blocks down the hall
            PortalEntity.spawnInAir(level, spot.add(0, 1.15, 5.5), 0.0f, null, player, false);
        }));
    }
}
