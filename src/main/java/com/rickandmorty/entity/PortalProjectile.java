package com.rickandmorty.entity;

import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.item.PortalGunItem;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The green blob shot by the portal gun. Opens a portal where it hits a block,
 * or in mid-air once it has flown its max distance. Flies straight through mobs, unless the gun
 * has the Targeting Upgrade: then a creature it hits is sent straight to the destination.
 */
public class PortalProjectile extends ThrowableProjectile {
    private static final int MAX_LIFETIME = 60;

    @Nullable
    private PortalDestinations.Destination destination;
    private double maxDistance = PortalGunItem.MAX_RANGE;
    /** Targeting Upgrade: hit creatures and send them straight to the destination. */
    private boolean targeting;
    @Nullable
    private Vec3 startPos;
    /** Attack mode: opens a blender portal instead of a travel portal. */
    private boolean blender;

    public PortalProjectile(EntityType<? extends PortalProjectile> type, Level level) {
        super(type, level);
    }

    /** A shot that opens a blender portal (the gun's attack mode) where it lands. */
    public static PortalProjectile blender(Level level, LivingEntity shooter, double maxDistance) {
        PortalProjectile shot = new PortalProjectile(level, shooter, null, maxDistance, false);
        shot.blender = true;
        return shot;
    }

    public PortalProjectile(Level level, LivingEntity shooter, @Nullable PortalDestinations.Destination destination, double maxDistance,
                            boolean targeting) {
        super(ModEntities.PORTAL_PROJECTILE.get(), shooter, level);
        this.destination = destination;
        this.maxDistance = maxDistance;
        this.targeting = targeting;
        this.startPos = position();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    /** Normal shots fly through creatures; upgraded ones hit anything alive except the shooter. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return targeting && target instanceof LivingEntity && target != getOwner() && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel from) || destination == null) return;
        Entity entity = result.getEntity();
        ServerLevel target = from.getServer().getLevel(destination.dimension());
        if (target == null || !PortalTeleporter.canTeleport(entity, from, target)) return;
        // No portals: a burst of portal fluid where they stood, and they're simply gone.
        PortalEntity.burst(from, entity.position().add(0, entity.getBbHeight() / 2, 0));
        PortalTeleporter.teleport(entity, from, target, destination.position(), destination.yaw());
        PortalEntity.burst(target, destination.position().add(0, 1, 0));
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        if (level().isClientSide) {
            PortalEntity.spawnGreenParticles(level(), position(), 2, 0.1);
            return;
        }
        if (startPos == null) startPos = position();
        if (position().distanceTo(startPos) >= maxDistance) {
            Vec3 direction = getDeltaMovement().normalize();
            openPortalInAir(startPos.add(direction.scale(maxDistance)));
            discard();
        } else if (tickCount > MAX_LIFETIME) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel serverLevel && (destination != null || blender)) {
            PortalEntity.spawnOnSurface(serverLevel, result, shooterYaw(), destination, getOwner(), blender);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }

    private void openPortalInAir(Vec3 point) {
        if (level() instanceof ServerLevel serverLevel && (destination != null || blender)) {
            PortalEntity.spawnInAir(serverLevel, point, shooterYaw(), destination, getOwner(), blender);
        }
    }

    private float shooterYaw() {
        Entity owner = getOwner();
        return owner != null ? owner.getYRot() : getYRot();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
