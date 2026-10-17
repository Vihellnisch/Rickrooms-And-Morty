package com.rickandmorty.entity;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * A swirling green portal. Anything that walks into it is sent to its destination.
 * It is an ellipse WIDTH x HEIGHT, either standing up (in the air / on a wall) or
 * lying flat (on a floor / ceiling). The entity position is the centre of the ellipse.
 * <p>
 * Portals without a destination are "exit" portals: purely visual, they appear
 * behind whoever arrives and close quickly.
 * <p>
 * Blender portals (the gun's attack mode) look just like any other portal. But when something
 * alive comes close, mechanical tentacles shoot out, grab it and hold it up while saw arms come
 * out after them and blend it. Then they pull back in and wait for the next one.
 */
public class PortalEntity extends Entity {
    // Nearly round, like the show's portals.
    public static final float WIDTH = 1.9f;
    public static final float HEIGHT = 2.35f;
    /** How far the hitbox reaches out from each side of the portal surface. */
    private static final float DEPTH = 0.4f;

    public static final int LIFETIME = 300;
    public static final int EXIT_LIFETIME = 40;
    public static final int OPEN_TICKS = 10;
    public static final int CLOSE_TICKS = 10;
    /** After the shooter walks through, the portal lingers this long so friends can follow. */
    private static final int LINGER_AFTER_OWNER = 40;

    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.36f, 1.0f, 0.23f), 1.0f);
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.78f, 0.2f), 1.0f);
    /** One portal in this many opens gold, like Evil Morty's. */
    public static final int GOLD_ODDS = 50;

    private static final EntityDataAccessor<Boolean> DATA_FLAT = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_CLOSING = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GOLD = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BLENDER = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VICTIM = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);

    // --- blender portals
    public static final int BLENDER_LIFETIME = 600;
    public static final int IDLE = 0, GRAB = 1, BLEND = 2, RETRACT = 3;
    public static final int GRAB_TICKS = 16;
    public static final int BLEND_TICKS = 70;
    public static final int RETRACT_TICKS = 14;
    /** How close something has to come before the arms reach out. */
    public static final double BLENDER_REACH = 3.2;
    private static final int BLENDER_VICTIMS = 4;
    private static final float BLENDER_DAMAGE = 3.0f;
    public static final ResourceKey<DamageType> BLENDER_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, RickAndMortyMod.id("blender"));

    // Server only
    @Nullable
    private PortalDestinations.Destination destination;
    @Nullable
    private UUID owner;
    private int life = LIFETIME;
    private int phaseTicks;
    private int victims;

    // Client only: tick at which the closing animation started
    private int closingStartTick = -1;
    // Client only: tick at which the blender's current phase started
    private int clientPhaseStart;

    public PortalEntity(EntityType<? extends PortalEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // ---------------------------------------------------------------- spawning

    /** Opens a portal on the face of a block that the projectile hit. */
    public static void spawnOnSurface(ServerLevel level, BlockHitResult hit, float shooterYaw,
                                      PortalDestinations.Destination destination, @Nullable Entity owner) {
        spawnOnSurface(level, hit, shooterYaw, destination, owner, false);
    }

    /** Opens a portal (a blender portal if {@code blender}, which needs no destination) on a block face. */
    public static PortalEntity spawnOnSurface(ServerLevel level, BlockHitResult hit, float shooterYaw,
                                              @Nullable PortalDestinations.Destination destination, @Nullable Entity owner, boolean blender) {
        Direction face = hit.getDirection();
        Vec3 location = hit.getLocation();
        if (face.getAxis() == Direction.Axis.Y) {
            Vec3 center = location.add(0, face.getStepY() * 0.02, 0);
            return spawn(level, center, true, shooterYaw, destination, owner, blender);
        } else {
            double x = location.x + face.getStepX() * 0.02;
            double z = location.z + face.getStepZ() * 0.02;
            double y = restOnGround(level, hit.getBlockPos().relative(face), location.y);
            return spawn(level, new Vec3(x, y, z), false, face.toYRot(), destination, owner, blender);
        }
    }

    /** Opens a standing portal in mid-air, facing the shooter. */
    public static void spawnInAir(ServerLevel level, Vec3 point, float shooterYaw,
                                  PortalDestinations.Destination destination, @Nullable Entity owner) {
        spawnInAir(level, point, shooterYaw, destination, owner, false);
    }

    public static PortalEntity spawnInAir(ServerLevel level, Vec3 point, float shooterYaw,
                                          @Nullable PortalDestinations.Destination destination, @Nullable Entity owner, boolean blender) {
        double y = restOnGround(level, BlockPos.containing(point), point.y);
        return spawn(level, new Vec3(point.x, y, point.z), false, shooterYaw + 180.0f, destination, owner, blender);
    }

    private static PortalEntity spawn(ServerLevel level, Vec3 center, boolean flat, float yaw,
                                      @Nullable PortalDestinations.Destination destination, @Nullable Entity owner, boolean blender) {
        PortalEntity portal = new PortalEntity(ModEntities.PORTAL.get(), level);
        portal.setup(center, flat, yaw, blender ? null : destination, owner != null ? owner.getUUID() : null, blender ? BLENDER_LIFETIME : LIFETIME);
        // a blender portal looks like any other portal: that's the point
        portal.setGold(!blender && level.random.nextInt(GOLD_ODDS) == 0);
        portal.entityData.set(DATA_BLENDER, blender);
        level.addFreshEntity(portal);
        level.playSound(null, center.x, center.y, center.z, ModSounds.PORTAL_OPEN.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
        return portal;
    }

    private void setup(Vec3 center, boolean flat, float yaw, @Nullable PortalDestinations.Destination destination,
                       @Nullable UUID owner, int life) {
        entityData.set(DATA_FLAT, flat);
        entityData.set(DATA_YAW, Mth.wrapDegrees(yaw));
        this.destination = destination;
        this.owner = owner;
        this.life = life;
        setYRot(yaw);
        setPos(center);
    }

    /**
     * If there's ground a little below the portal's bottom edge, move the portal down
     * (or up) so it stands on it, which makes it easy to walk into. Returns the centre Y.
     */
    private static double restOnGround(Level level, BlockPos column, double centerY) {
        int top = Mth.floor(centerY);
        int lowest = Mth.floor(centerY - HEIGHT / 2 - 1.5);
        for (int y = top; y >= lowest; y--) {
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return y + shape.max(Direction.Axis.Y) + HEIGHT / 2 + 0.05;
            }
        }
        return centerY;
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnRimParticles();
            if (isBlender() && getPhase() == BLEND) {
                LivingEntity victim = getVictim();
                if (victim != null) Gore.clientSpray(level(), victim.position().add(0, victim.getBbHeight() * 0.5, 0), 3, victim.getBbWidth());
            }
            return;
        }

        if (!isBlender() || getPhase() == IDLE) life--;
        if (life <= CLOSE_TICKS && !isClosing()) {
            entityData.set(DATA_CLOSING, true);
        }
        if (life <= 0) {
            discard();
            return;
        }
        if (isBlender()) {
            tickBlender((ServerLevel) level());
        } else if (destination != null && tickCount >= OPEN_TICKS / 2 && !isClosing()) {
            teleportTouchingEntities((ServerLevel) level());
        }
    }

    // ---------------------------------------------------------------- the blender

    public boolean isBlender() {
        return entityData.get(DATA_BLENDER);
    }

    public int getPhase() {
        return entityData.get(DATA_PHASE);
    }

    /** Client: ticks (with partial tick) since the blender's current phase began. */
    public float phaseAge(float partialTick) {
        return tickCount - clientPhaseStart + partialTick;
    }

    @Nullable
    public LivingEntity getVictim() {
        int id = entityData.get(DATA_VICTIM);
        return id >= 0 && level().getEntity(id) instanceof LivingEntity living ? living : null;
    }

    private void setPhase(int phase) {
        entityData.set(DATA_PHASE, phase);
        phaseTicks = 0;
    }

    /** Where a victim is held: just out in front of the portal, on whichever side they came from. */
    public Vec3 holdPoint(Entity victim) {
        return BlenderGrip.holdPoint(position(), normal(), isFlat(), victim);
    }

    private boolean canBlend(LivingEntity e) {
        return BlenderGrip.canBlend(e, owner);
    }

    private void tickBlender(ServerLevel level) {
        int phase = getPhase();
        LivingEntity victim = getVictim();
        boolean lost = victim == null || !canBlend(victim) || victim.position().distanceToSqr(position()) > 64.0;
        switch (phase) {
            case IDLE -> {
                if (isClosing() || tickCount < OPEN_TICKS || tickCount % 2 != 0) break;
                LivingEntity prey = null;
                double best = BLENDER_REACH * BLENDER_REACH;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BLENDER_REACH), this::canBlend)) {
                    double d = e.position().add(0, e.getBbHeight() / 2, 0).distanceToSqr(position());
                    if (d < best) {
                        best = d;
                        prey = e;
                    }
                }
                if (prey != null) {
                    entityData.set(DATA_VICTIM, prey.getId());
                    setPhase(GRAB);
                    level.playSound(null, getX(), getY(), getZ(), ModSounds.BLENDER_GRAB.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
                }
            }
            case GRAB -> {
                if (lost) {
                    setPhase(RETRACT);
                    break;
                }
                hold(victim, Math.min(1.0f, (phaseTicks + 1) / (float) GRAB_TICKS));
                if (phaseTicks >= GRAB_TICKS) {
                    setPhase(BLEND);
                    level.playSound(null, getX(), getY(), getZ(), ModSounds.BLENDER_SAW.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
                }
            }
            case BLEND -> {
                if (lost) {
                    setPhase(RETRACT);
                    break;
                }
                hold(victim, 1.0f);
                if (phaseTicks % 6 == 0) {
                    Entity cause = owner != null ? level.getEntity(owner) : null;
                    var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLENDER_DAMAGE_TYPE);
                    victim.invulnerableTime = 0;
                    victim.hurt(new net.minecraft.world.damagesource.DamageSource(type, this, cause), BLENDER_DAMAGE);
                    Gore.splash(level, victim.position().add(0, victim.getBbHeight() * 0.5, 0), 14, victim.getBbWidth() * 0.4);
                }
                if (phaseTicks % 10 == 0) {
                    level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.BLENDER_SAW.get(), SoundSource.HOSTILE, 1.2f,
                            0.9f + random.nextFloat() * 0.3f);
                    level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.BLENDER_GORE.get(), SoundSource.HOSTILE, 1.0f,
                            0.7f + random.nextFloat() * 0.4f);
                }
                if (!victim.isAlive()) {
                    Gore.burst(victim);
                    level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.BLENDER_GORE.get(), SoundSource.HOSTILE, 2.0f, 0.5f);
                    if (owner != null && level.getEntity(owner) instanceof net.minecraft.server.level.ServerPlayer shooter) {
                        com.rickandmorty.registry.ModAdvancements.award(shooter, "blended");
                    }
                    setPhase(RETRACT);
                } else if (phaseTicks >= BLEND_TICKS) {
                    setPhase(RETRACT);   // something too big to finish: spit it back out
                }
            }
            case RETRACT -> {
                if (phaseTicks >= RETRACT_TICKS) {
                    entityData.set(DATA_VICTIM, -1);
                    victims++;
                    if (victims >= BLENDER_VICTIMS) life = Math.min(life, CLOSE_TICKS);
                    setPhase(IDLE);
                }
            }
            default -> setPhase(IDLE);
        }
        phaseTicks++;
    }

    /** Pull the victim towards the hold point ({@code grip} 0..1) and keep them there, shaking while blending. */
    private void hold(LivingEntity victim, float grip) {
        BlenderGrip.hold(victim, holdPoint(victim), grip, getPhase() == BLEND, random);
    }

    private void teleportTouchingEntities(ServerLevel from) {
        ServerLevel target = from.getServer().getLevel(destination.dimension());
        if (target == null) return;

        for (Entity entity : from.getEntities(this, getBoundingBox(), e -> PortalTeleporter.canTeleport(e, from, target))) {
            PortalTeleporter.teleport(entity, from, target, destination.position(), destination.yaw());
            spawnExitPortal(target, destination, isGold());
            if (isGold() && entity instanceof net.minecraft.server.level.ServerPlayer player) {
                com.rickandmorty.registry.ModAdvancements.award(player, "evil_morty");
            }
            if (entity.getUUID().equals(owner)) {
                life = Math.min(life, CLOSE_TICKS + LINGER_AFTER_OWNER);
            }
        }
    }

    /** A short-lived portal behind the arriving entity, so it looks like they stepped out of it. */
    public static void spawnExitPortal(ServerLevel level, PortalDestinations.Destination dest, boolean gold) {
        Vec3 behind = forward(dest.yaw()).scale(-0.6);
        Vec3 center = dest.position().add(behind.x, HEIGHT / 2 + 0.05, behind.z);
        boolean alreadyOpen = !level.getEntitiesOfClass(PortalEntity.class, new AABB(center, center).inflate(2.0),
                portal -> portal.destination == null).isEmpty();
        if (alreadyOpen) return;

        PortalEntity exit = new PortalEntity(ModEntities.PORTAL.get(), level);
        exit.setup(center, false, dest.yaw(), null, null, EXIT_LIFETIME);
        exit.setGold(gold);
        level.addFreshEntity(exit);
    }

    private void spawnRimParticles() {
        float open = getOpenness(0.0f);
        if (open <= 0.0f) return;
        RandomSource random = level().random;
        Vec3 right = right();
        Vec3 up = up();
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double x = Math.cos(angle) * WIDTH / 2 * open;
            double y = Math.sin(angle) * HEIGHT / 2 * open;
            Vec3 p = position().add(right.scale(x)).add(up.scale(y));
            level().addParticle(isGold() ? GOLD_DUST : GREEN_DUST, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    /** A splash of portal-green particles (server side), e.g. where a Targeting shot zaps something away. */
    public static void burst(ServerLevel level, Vec3 pos) {
        level.sendParticles(GREEN_DUST, pos.x, pos.y, pos.z, 40, 0.4, 0.6, 0.4, 0.0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 10, 0.4, 0.6, 0.4, 0.0);
    }

    public static void spawnGreenParticles(Level level, Vec3 pos, int count, double spread) {
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            level.addParticle(GREEN_DUST,
                    pos.x + (random.nextDouble() - 0.5) * spread,
                    pos.y + (random.nextDouble() - 0.5) * spread,
                    pos.z + (random.nextDouble() - 0.5) * spread,
                    0, 0, 0);
        }
    }

    // ---------------------------------------------------------------- shape & orientation

    public boolean isFlat() {
        return entityData.get(DATA_FLAT);
    }

    public float getPortalYaw() {
        return entityData.get(DATA_YAW);
    }

    public boolean isGold() {
        return entityData.get(DATA_GOLD);
    }

    public void setGold(boolean gold) {
        entityData.set(DATA_GOLD, gold);
    }

    public boolean isClosing() {
        return entityData.get(DATA_CLOSING);
    }

    /** 0 = closed, 1 = fully open. Drives the grow/shrink animation. */
    public float getOpenness(float partialTick) {
        float age = tickCount + partialTick;
        float open = Mth.clamp(age / OPEN_TICKS, 0.0f, 1.0f);
        if (closingStartTick >= 0) {
            open = Math.min(open, Mth.clamp(1.0f - (age - closingStartTick) / CLOSE_TICKS, 0.0f, 1.0f));
        }
        return open;
    }

    private static Vec3 forward(float yaw) {
        float rad = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(rad), 0, Mth.cos(rad));
    }

    /** Direction the portal surface faces. */
    public Vec3 normal() {
        return isFlat() ? new Vec3(0, 1, 0) : forward(getPortalYaw());
    }

    /** Direction of the ellipse's long (HEIGHT) axis. */
    public Vec3 up() {
        return isFlat() ? forward(getPortalYaw()) : new Vec3(0, 1, 0);
    }

    /** Direction of the ellipse's short (WIDTH) axis. */
    public Vec3 right() {
        float rad = getPortalYaw() * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(rad), 0, Mth.sin(rad));
    }

    @Override
    protected AABB makeBoundingBox() {
        Vec3 r = right(), u = up(), n = normal();
        double ex = Math.abs(r.x) * WIDTH / 2 + Math.abs(u.x) * HEIGHT / 2 + Math.abs(n.x) * DEPTH;
        double ey = Math.abs(r.y) * WIDTH / 2 + Math.abs(u.y) * HEIGHT / 2 + Math.abs(n.y) * DEPTH;
        double ez = Math.abs(r.z) * WIDTH / 2 + Math.abs(u.z) * HEIGHT / 2 + Math.abs(n.z) * DEPTH;
        Vec3 p = position();
        return new AABB(p.x - ex, p.y - ey, p.z - ez, p.x + ex, p.y + ey, p.z + ez);
    }

    // ---------------------------------------------------------------- entity plumbing

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FLAT, false);
        builder.define(DATA_YAW, 0.0f);
        builder.define(DATA_CLOSING, false);
        builder.define(DATA_GOLD, false);
        builder.define(DATA_BLENDER, false);
        builder.define(DATA_VICTIM, -1);
        builder.define(DATA_PHASE, IDLE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_FLAT.equals(key) || DATA_YAW.equals(key)) {
            setBoundingBox(makeBoundingBox());
        } else if (DATA_CLOSING.equals(key) && isClosing() && closingStartTick < 0) {
            closingStartTick = tickCount;
        } else if (DATA_PHASE.equals(key)) {
            clientPhaseStart = tickCount;
        }
    }

    /** A blender portal's arms reach well outside the portal itself: don't cull them. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return isBlender() ? getBoundingBox().inflate(BLENDER_REACH + 1.0) : super.getBoundingBoxForCulling();
    }

    /** Portals are temporary, so they are never written to the world save. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
