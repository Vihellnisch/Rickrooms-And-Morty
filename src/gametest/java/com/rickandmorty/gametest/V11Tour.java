package com.rickandmorty.gametest;

import com.rickandmorty.entity.cruiser.CruiserUpgrade;
import com.rickandmorty.entity.cruiser.CruiserWeapons;
import com.rickandmorty.entity.cruiser.SpaceCruiserEntity;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/** The v0.11 tour (./gradlew runTourClient -DtourV11=true): Rick's Space Cruiser. */
final class V11Tour {
    private V11Tour() {
    }

    static void addStops(BiConsumer<String, BiConsumer<ServerPlayer, ServerLevel>> stops) {
        stops.accept("warmup", (p, l) -> V10Tour.clean(p));
        stops.accept("cruiser_front_view", (p, l) -> look(p, l, 0, -30.0, 0.35));
        stops.accept("cruiser_side", (p, l) -> look(p, l, 1, 90.0, 0.2));
        stops.accept("cruiser_above", (p, l) -> look(p, l, 2, 150.0, 1.6));
        stops.accept("cruiser_cockpit_hand", (p, l) -> ride(p, l, 3, false));
        stops.accept("cruiser_flying_back", (p, l) -> ride(p, l, 4, true));
        stops.accept("cruiser_flamethrowers", (p, l) -> weapon(p, l, 5, CruiserUpgrade.FLAMETHROWER));
        stops.accept("cruiser_disintegrator", (p, l) -> weapon(p, l, 6, CruiserUpgrade.DISINTEGRATOR));
        stops.accept("cruiser_minigun", (p, l) -> weapon(p, l, 7, CruiserUpgrade.MINIGUN));
        stops.accept("cruiser_rockets", (p, l) -> weapon(p, l, 8, CruiserUpgrade.ROCKETS));
    }

    static SpaceCruiserEntity park(ServerLevel level, int slot, float yaw, double lift) {
        BlockPos at = V10Tour.ground(level, 3000 + slot * 40, 3000);
        // clear away cruisers left from earlier runs
        level.getEntitiesOfClass(SpaceCruiserEntity.class, new net.minecraft.world.phys.AABB(at).inflate(40)).forEach(net.minecraft.world.entity.Entity::discard);
        SpaceCruiserEntity cruiser = ModEntities.SPACE_CRUISER.get().create(level);
        cruiser.moveTo(at.getX() + 0.5, at.getY() + lift, at.getZ() + 0.5, yaw, 0.0f);
        for (CruiserUpgrade upgrade : CruiserUpgrade.values()) cruiser.install(upgrade);
        level.addFreshEntity(cruiser);
        return cruiser;
    }

    /** The cruiser parked, seen from {@code angle} degrees round from its front, {@code height} of the way up. */
    private static void look(ServerPlayer player, ServerLevel overworld, int slot, double angle, double height) {
        V10Tour.clean(player);
        overworld.setDayTime(5000);
        SpaceCruiserEntity cruiser = park(overworld, slot, 0.0f, 0.0);
        Vec3 dir = Vec3.directionFromRotation(0.0f, (float) angle);        // yaw 0 faces south, so its front is at +z
        Vec3 eye = cruiser.position().add(dir.scale(7.0)).add(0, 1.0 + height * 6, 0);
        Vec3 to = cruiser.position().add(0, 0.9, 0).subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        DevTour.go(player, overworld, eye.x, eye.y - 1.62, eye.z, yaw, pitch);
    }

    static SpaceCruiserEntity ride(ServerPlayer player, ServerLevel overworld, int slot, boolean flying) {
        V10Tour.clean(player);
        overworld.setDayTime(5000);
        SpaceCruiserEntity cruiser = park(overworld, slot, 180.0f, flying ? 6.0 : 0.0);
        DevTour.go(player, overworld, cruiser.getX(), cruiser.getY(), cruiser.getZ() + 1, 180.0f, 5.0f);
        DevTour.LATER.add(java.util.Map.entry(DevTour.ticks + 5, () -> {
            player.getAbilities().flying = false;
            player.startRiding(cruiser, true);
        }));
        return cruiser;
    }

    /** A hovering cruiser (nobody at the controls) firing at a row of zombies, seen from the side. */
    private static void weapon(ServerPlayer player, ServerLevel overworld, int slot, CruiserUpgrade weapon) {
        V10Tour.clean(player);
        overworld.setDayTime(5000);
        SpaceCruiserEntity cruiser = park(overworld, slot, 180.0f, 5.0);
        cruiser.setNoGravity(true);
        BlockPos ground = V10Tour.ground(overworld, (int) cruiser.getX(), (int) cruiser.getZ() - 11);
        for (int i = 0; i < 4; i++) {
            var zombie = EntityType.ZOMBIE.create(overworld);
            zombie.moveTo(cruiser.getX() + (i - 1.5) * 2, ground.getY(), cruiser.getZ() - 11, 180.0f, 0.0f);
            zombie.setNoAi(true);
            overworld.addFreshEntity(zombie);
        }
        DevTour.go(player, overworld, cruiser.getX() + 11, cruiser.getY() - 1.5, cruiser.getZ() - 5, 90.0f, 8.0f);
        for (int t = 70; t < 116; t += weapon.continuous ? 1 : 20) {
            DevTour.LATER.add(java.util.Map.entry(DevTour.ticks + t, () -> {
                if (cruiser.isRemoved()) return;
                cruiser.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                Vec3 target = new Vec3(cruiser.getX(), ground.getY() + 1.0, cruiser.getZ() - 11);
                CruiserWeapons.shoot(cruiser, null, weapon, target.subtract(cruiser.muzzle()).normalize());
            }));
        }
    }
}
