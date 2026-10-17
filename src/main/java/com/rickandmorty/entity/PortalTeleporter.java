package com.rickandmorty.entity;

import com.rickandmorty.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** Shared teleport logic for portal gun portals and warp gateways. */
public final class PortalTeleporter {
    /** Ticks before the same entity can use a portal again. */
    public static final int TRAVEL_COOLDOWN = 40;

    private PortalTeleporter() {
    }

    public static boolean canTeleport(Entity entity, Level from, ServerLevel target) {
        return !(entity instanceof PortalEntity)
                && !(entity instanceof PortalProjectile)
                && !entity.isSpectator()
                && !entity.isVehicle()
                && !entity.isOnPortalCooldown()
                && entity.canUsePortal(false)
                && entity.canChangeDimensions(from, target);
    }

    /**
     * In dimensions whose landings are picked from the terrain alone (Cronenberg World), bone
     * trees and tumors may have grown on the spot: stand on top of them instead of inside.
     */
    private static Vec3 safeLanding(Entity entity, ServerLevel target, Vec3 destination) {
        if (!ModDimensions.needsSafeLanding(target.dimension())) return destination;
        if (target.noCollision(entity, entity.getDimensions(entity.getPose()).makeBoundingBox(destination))) return destination;
        int top = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(destination.x), (int) Math.floor(destination.z));
        return new Vec3(destination.x, top + 0.1, destination.z);
    }

    public static void teleport(Entity entity, ServerLevel from, ServerLevel target, Vec3 destination, float yaw) {
        from.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.PORTAL_TRAVEL.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);

        entity.setPortalCooldown(TRAVEL_COOLDOWN);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.resetFallDistance();
        target.getChunkAt(BlockPos.containing(destination));
        destination = safeLanding(entity, target, destination);
        entity.teleportTo(target, destination.x, destination.y, destination.z, Set.of(), yaw, entity.getXRot());

        target.playSound(null, destination.x, destination.y, destination.z, ModSounds.PORTAL_TRAVEL.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
    }
}
