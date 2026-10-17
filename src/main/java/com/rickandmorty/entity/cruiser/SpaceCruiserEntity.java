package com.rickandmorty.entity.cruiser;

import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Rick's Space Cruiser. Two seats under a glass dome: the pilot (the first one in) steers with
 * the movement keys and the mouse, goes up with jump and down with the descend key. It runs on
 * the Microverse Battery built into it: flying uses power, sitting still (or parked) lets the
 * battery top it back up. Upgrades are fitted by using them on it.
 * <p>
 * Like a boat, the pilot's own game moves it, and the server just follows along.
 */
public class SpaceCruiserEntity extends Entity {
    /** The built-in battery; a Microverse Booster makes it bigger. */
    public static final int MAX_POWER = 6000;
    /** Charging per tick parked on the ground, and while in the air; flying costs per tick and per block. */
    private static final float REGEN_GROUND = 6.0f, REGEN_AIR = 2.0f, HOVER_COST = 0.4f, SPEED_COST = 2.0f;
    public static final int MAX_BOOSTER = CruiserTier.MAX;
    private static final double LOW_POWER_SPEED = 0.35;
    private static final int LOW_POWER = 100;

    private static final EntityDataAccessor<Integer> DATA_POWER = SynchedEntityData.defineId(SpaceCruiserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(SpaceCruiserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WEAPON = SynchedEntityData.defineId(SpaceCruiserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(SpaceCruiserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HURT_TIME = SynchedEntityData.defineId(SpaceCruiserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FIRING = SynchedEntityData.defineId(SpaceCruiserEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOOSTER = SynchedEntityData.defineId(SpaceCruiserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_THRUSTERS = SynchedEntityData.defineId(SpaceCruiserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DRILL = SynchedEntityData.defineId(SpaceCruiserEntity.class, EntityDataSerializers.INT);

    private float power = MAX_POWER;
    private final int[] cooldowns = new int[CruiserUpgrade.values().length];
    private @Nullable UUID owner;
    /** Where the remote called it to, and the portal drive's countdown. */
    private @Nullable Vec3 summonedTo;
    private int portalCountdown = -1;
    private @Nullable Runnable portalJump;
    private Vec3 lastPosition = Vec3.ZERO;
    private int keptSafeAt = -1000;

    // client-side smoothing for cruisers someone else is flying (like a boat's)
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot, lerpXRot;

    public SpaceCruiserEntity(EntityType<? extends SpaceCruiserEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setNoGravity(true);   // it does its own (and the server won't kick its pilot for "flying")
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_POWER, MAX_POWER);
        builder.define(DATA_UPGRADES, 0);
        builder.define(DATA_WEAPON, -1);
        builder.define(DATA_DAMAGE, 0.0f);
        builder.define(DATA_HURT_TIME, 0);
        builder.define(DATA_FIRING, false);
        builder.define(DATA_BOOSTER, 0);
        builder.define(DATA_THRUSTERS, 0);
        builder.define(DATA_DRILL, 0);
    }

    // ------------------------------------------------------------------ state

    public int power() {
        return entityData.get(DATA_POWER);
    }

    public int maxPower() {
        return capacity(booster());
    }

    /** Which mark of {@code line} is fitted: 0 for none, up to {@link CruiserTier#MAX}. The Laser Drill is Mk I once fitted. */
    public int tier(CruiserTier line) {
        return switch (line) {
            case BOOSTER -> Mth.clamp(entityData.get(DATA_BOOSTER), 0, CruiserTier.MAX);
            case THRUSTERS -> Mth.clamp(entityData.get(DATA_THRUSTERS), 0, CruiserTier.MAX);
            case DRILL -> has(CruiserUpgrade.LASER_DRILL) ? Mth.clamp(entityData.get(DATA_DRILL), 1, CruiserTier.MAX) : 0;
        };
    }

    public void setTier(CruiserTier line, int tier) {
        int t = Mth.clamp(tier, 0, CruiserTier.MAX);
        switch (line) {
            case BOOSTER -> entityData.set(DATA_BOOSTER, t);
            case THRUSTERS -> entityData.set(DATA_THRUSTERS, t);
            case DRILL -> {
                entityData.set(DATA_DRILL, t);
                if (t > 0 && !has(CruiserUpgrade.LASER_DRILL)) install(CruiserUpgrade.LASER_DRILL);
            }
        }
    }

    public int booster() {
        return tier(CruiserTier.BOOSTER);
    }

    public void setBooster(int tier) {
        setTier(CruiserTier.BOOSTER, tier);
    }

    public static int capacity(int tier) {
        return CruiserTier.capacity(tier);
    }

    public static float flightDrain(int tier) {
        return CruiserTier.flightDrain(tier);
    }

    public static float weaponCost(int tier) {
        return CruiserTier.weaponCost(tier);
    }

    public static float recharge(int tier) {
        return CruiserTier.recharge(tier);
    }

    /** The translation key for an upgrade as it's fitted (the Laser Drill by its mark). */
    public String upgradeKey(CruiserUpgrade upgrade) {
        return upgrade == CruiserUpgrade.LASER_DRILL ? CruiserTier.DRILL.itemKey(tier(CruiserTier.DRILL)) : "item.rickrooms." + upgrade.itemId();
    }

    public boolean has(CruiserUpgrade upgrade) {
        return (entityData.get(DATA_UPGRADES) & upgrade.bit()) != 0;
    }

    public void install(CruiserUpgrade upgrade) {
        entityData.set(DATA_UPGRADES, entityData.get(DATA_UPGRADES) | upgrade.bit());
        if (upgrade.weapon && selectedWeapon() == null) entityData.set(DATA_WEAPON, upgrade.ordinal());
    }

    public List<CruiserUpgrade> installed() {
        List<CruiserUpgrade> list = new ArrayList<>();
        for (CruiserUpgrade upgrade : CruiserUpgrade.values()) if (has(upgrade)) list.add(upgrade);
        return list;
    }

    public @Nullable CruiserUpgrade selectedWeapon() {
        int i = entityData.get(DATA_WEAPON);
        return i >= 0 && i < CruiserUpgrade.values().length && has(CruiserUpgrade.values()[i]) ? CruiserUpgrade.values()[i] : null;
    }

    public void cycleWeapon() {
        CruiserUpgrade[] all = CruiserUpgrade.values();
        int start = Math.max(0, entityData.get(DATA_WEAPON));
        for (int step = 1; step <= all.length; step++) {
            CruiserUpgrade next = all[(start + step) % all.length];
            if (next.weapon && has(next)) {
                entityData.set(DATA_WEAPON, next.ordinal());
                return;
            }
        }
    }

    public boolean isFiring() {
        return entityData.get(DATA_FIRING);
    }

    public void setFiring(boolean firing) {
        entityData.set(DATA_FIRING, firing);
    }

    /** Spends power (less with a booster fitted); false (and nothing spent) if there isn't enough. */
    public boolean usePower(int amount) {
        float cost = amount * weaponCost(booster());
        if (power < cost) return false;
        power -= cost;
        return true;
    }

    public boolean ready(CruiserUpgrade upgrade) {
        return cooldowns[upgrade.ordinal()] <= 0;
    }

    public void cool(CruiserUpgrade upgrade) {
        cooldowns[upgrade.ordinal()] = upgrade.cooldown;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public @Nullable Player pilot() {
        return getControllingPassenger() instanceof Player player ? player : null;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    /** Where it's facing, flat. */
    public Vec3 forward() {
        return Vec3.directionFromRotation(0.0f, getYRot());
    }

    /** The front of the hull, where the guns are. */
    public Vec3 muzzle() {
        return position().add(forward().scale(2.1)).add(0.0, 0.9, 0.0);
    }

    // ------------------------------------------------------------------ riding

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int seat = getPassengers().indexOf(passenger);
        double side = seat <= 0 ? -0.375 : 0.375;                   // the pilot sits on the right
        return new Vec3(side, 0.95, 0.1).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        super.positionRider(passenger, move);
        if (passenger instanceof LivingEntity living && getControllingPassenger() == passenger) {
            living.setYBodyRot(getYRot());
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 side = forward().yRot(Mth.HALF_PI * (getPassengers().indexOf(passenger) <= 0 ? -1 : 1)).scale(2.6);
        return position().add(side).add(0.0, 0.5, 0.0);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (stack.getItem() instanceof CruiserUpgradeItem upgradeItem) {
            CruiserUpgrade upgrade = upgradeItem.upgrade();
            if (has(upgrade)) {
                player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.already", stack.getHoverName()).withStyle(ChatFormatting.GRAY), true);
            } else {
                install(upgrade);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.NEUTRAL, 1.0f, 1.2f);
                player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.installed", stack.getHoverName()).withStyle(ChatFormatting.GREEN), true);
                if (player instanceof ServerPlayer serverPlayer) {
                    ModAdvancements.award(serverPlayer, "cruiser_upgrade");
                    if (installed().size() == CruiserUpgrade.values().length) ModAdvancements.award(serverPlayer, "fully_loaded");
                }
            }
            return InteractionResult.CONSUME;
        }
        if (stack.getItem() instanceof CruiserTierItem tierItem) {
            fitTier(player, stack, tierItem.line(), tierItem.tier());
            return InteractionResult.CONSUME;
        }
        if (stack.getItem() instanceof CruiserRemoteItem) {
            CruiserRemoteItem.bind(stack, this);
            player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.remote_bound").withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }
        if (player.isSecondaryUseActive()) {
            player.displayClientMessage(status(), false);
            return InteractionResult.CONSUME;
        }
        if (!canAddPassenger(player) || player.isPassenger()) return InteractionResult.PASS;
        if (player.startRiding(this)) {
            summonedTo = null;
            if (player instanceof ServerPlayer serverPlayer) ModAdvancements.award(serverPlayer, "space_cruiser");
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** Fits a better mark of a booster, thrusters or drill, handing back the one it replaces. */
    private void fitTier(Player player, ItemStack stack, CruiserTier line, int tier) {
        int old = tier(line);
        if (tier <= old) {
            player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.tier_already",
                    Component.translatable(line.itemKey(old))).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Component name = stack.getHoverName();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        if (old > 0 && !player.getAbilities().instabuild) {
            ItemStack back = new ItemStack(line.item(old));
            if (!player.getInventory().add(back)) player.drop(back, false);
        }
        setTier(line, tier);
        level().playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 1.0f, 1.4f);
        player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.installed", name).withStyle(ChatFormatting.GREEN), true);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "cruiser_upgrade");
            if (tier == CruiserTier.MAX) {
                ModAdvancements.award(serverPlayer, switch (line) {
                    case BOOSTER -> "unlimited_power";
                    case THRUSTERS -> "ludicrous_speed";
                    case DRILL -> "diamond_drill";
                });
            }
        }
    }

    public Component status() {
        var line = Component.translatable("message.rickrooms.cruiser.status", power() * 100 / maxPower()).withStyle(ChatFormatting.AQUA);
        for (CruiserTier tierLine : new CruiserTier[]{CruiserTier.BOOSTER, CruiserTier.THRUSTERS}) {
            int t = tier(tierLine);
            if (t > 0) line.append(Component.literal("\n - ").append(Component.translatable(tierLine.itemKey(t))).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        for (CruiserUpgrade upgrade : installed()) {
            line.append(Component.literal("\n - ").append(Component.translatable(upgradeKey(upgrade))).withStyle(ChatFormatting.GRAY));
        }
        if (installed().isEmpty()) line.append(Component.translatable("message.rickrooms.cruiser.no_upgrades").withStyle(ChatFormatting.GRAY));
        return line;
    }

    // ------------------------------------------------------------------ taking damage

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide || isRemoved()) return false;
        if (source.getEntity() instanceof Player player && hasPassenger(player)) return false;     // you can't punch it from inside
        entityData.set(DATA_HURT_TIME, 10);
        entityData.set(DATA_DAMAGE, entityData.get(DATA_DAMAGE) + amount * 5.0f);
        markHurt();
        boolean creative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
        if (creative || entityData.get(DATA_DAMAGE) > 60.0f) {
            if (!creative) {
                spawnAtLocation(new ItemStack(ModItems.SPACE_CRUISER.get()));
                for (CruiserUpgrade upgrade : installed()) {
                    if (upgrade != CruiserUpgrade.LASER_DRILL) spawnAtLocation(new ItemStack(ModItems.CRUISER_UPGRADES.get(upgrade).get()));
                }
                for (CruiserTier tierLine : CruiserTier.values()) {
                    int t = tier(tierLine);
                    if (t > 0) spawnAtLocation(new ItemStack(tierLine.item(t)));
                }
            }
            ejectPassengers();
            discard();
        }
        return true;
    }

    public int hurtTime() {
        return entityData.get(DATA_HURT_TIME);
    }

    // ------------------------------------------------------------------ flying

    @Override
    public void tick() {
        if (entityData.get(DATA_HURT_TIME) > 0) entityData.set(DATA_HURT_TIME, entityData.get(DATA_HURT_TIME) - 1);
        if (entityData.get(DATA_DAMAGE) > 0) entityData.set(DATA_DAMAGE, Math.max(0, entityData.get(DATA_DAMAGE) - 0.5f));
        super.tick();
        tickLerp();
        if (isControlledByLocalInstance()) {
            // the pilot's own game flies it; with nobody at the controls, the server does
            if (level().isClientSide && pilot() != null) com.rickandmorty.client.cruiser.CruiserPiloting.steer(this, pilot());
            else coast();
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (!level().isClientSide) serverTick();
        else if (power() > 0 && !onGround()) exhaust();
    }

    /** No one at the controls: park, come when called, or (without Auto Park) just drop. */
    private void coast() {
        Vec3 v = getDeltaMovement();
        if (summonedTo != null && has(CruiserUpgrade.AUTO_PARK) && power() > LOW_POWER) {
            Vec3 to = summonedTo.subtract(position());
            if (to.lengthSqr() < 4.0) {
                summonedTo = null;
            } else {
                Vec3 want = to.normalize().scale(Math.min(0.9, to.length() * 0.15));
                setDeltaMovement(v.add(want.subtract(v).scale(0.2)));
                setYRot(Mth.approachDegrees(getYRot(), (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG), 6.0f));
                return;
            }
        }
        boolean gentle = has(CruiserUpgrade.AUTO_PARK) && power() > 0;
        double fall = onGround() ? 0.0 : Math.max(v.y - (gentle ? 0.01 : 0.06), gentle ? -0.18 : -1.2);
        setDeltaMovement(v.x * 0.8, fall, v.z * 0.8);
    }

    /** Called on the pilot's client each tick, from their input. */
    public void fly(float forward, float strafe, boolean up, boolean down, float targetYaw) {
        boolean low = power() < LOW_POWER;
        setYRot(Mth.approachDegrees(getYRot(), targetYaw, 7.0f));
        Vec3 look = forward(), right = look.yRot(-Mth.HALF_PI);
        Vec3 push = look.scale(forward).add(right.scale(-strafe * 0.8));
        if (push.lengthSqr() > 1) push = push.normalize();
        int thrusters = tier(CruiserTier.THRUSTERS);
        double max = low ? LOW_POWER_SPEED : CruiserTier.topSpeed(thrusters);
        double climb = CruiserTier.climb(thrusters);
        Vec3 v = getDeltaMovement();
        Vec3 horizontal = new Vec3(v.x, 0, v.z).scale(0.93).add(push.scale(CruiserTier.acceleration(thrusters) * (low ? 0.5 : 1.0)));
        if (horizontal.length() > max) horizontal = horizontal.normalize().scale(max);
        double vy = v.y * 0.8;
        if (up && !low) vy += climb;
        else if (down) vy -= climb;
        else if (low) vy -= 0.02;
        setDeltaMovement(horizontal.x, Mth.clamp(vy, -0.6, 0.6), horizontal.z);
    }

    private void exhaust() {
        if (tickCount % 2 != 0) return;
        if (getDeltaMovement().lengthSqr() < 0.2) return;                 // only a flicker when it's really moving
        Vec3 back = forward().scale(-1.75);
        for (double side : new double[]{-1.24, 1.24}) {
            Vec3 at = position().add(back).add(forward().yRot(Mth.HALF_PI).scale(side)).add(0, 1.2, 0);
            level().addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, -forward().x * 0.05, 0.0, -forward().z * 0.05);
        }
    }

    private void serverTick() {
        // the battery tops it up; flying and hovering cost power
        double moved = position().distanceTo(lastPosition);
        lastPosition = position();
        boolean airborne = !onGround();
        int tier = booster();
        float use = ((airborne ? HOVER_COST : 0.0f) + (float) Math.min(moved, 1.5) * SPEED_COST) * flightDrain(tier);
        float regen = (airborne ? REGEN_AIR : REGEN_GROUND) * recharge(tier);
        power = Mth.clamp(power + regen - use, 0.0f, capacity(tier));
        entityData.set(DATA_POWER, Math.round(power));
        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;

        Player pilot = pilot();
        if (pilot == null && isFiring()) setFiring(false);
        if (pilot instanceof ServerPlayer serverPilot && isFiring()) CruiserWeapons.fireHeld(this, serverPilot);
        if (pilot == null && getPassengers().size() > 0 && has(CruiserUpgrade.AUTO_PARK)) CruiserWeapons.keepSafe(this);
        if (portalCountdown > 0 && --portalCountdown == 0 && portalJump != null) {
            Runnable jump = portalJump;
            portalJump = null;
            portalCountdown = -1;
            jump.run();
        }
    }

    /** The portal drive: in {@code ticks}, do {@code jump}. */
    public void schedulePortalJump(int ticks, Runnable jump) {
        portalCountdown = ticks;
        portalJump = jump;
    }

    /** For tests: the exact power left. */
    public float exactPower() {
        return power;
    }

    public boolean portalJumpPending() {
        return portalCountdown > 0;
    }

    /** The remote: fly over to {@code target} and park there. */
    public boolean summon(Vec3 target) {
        if (!has(CruiserUpgrade.AUTO_PARK) || pilot() != null) return false;
        summonedTo = target;
        return true;
    }

    /** "Keep Summer safe." Tells whoever's inside, now and then. */
    void announceKeepingSafe() {
        if (tickCount - keptSafeAt < 1200) return;
        keptSafeAt = tickCount;
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.rickrooms.cruiser.keeping_safe", player.getDisplayName())
                        .withStyle(ChatFormatting.GREEN), false);
            }
        }
    }

    public boolean isSummoned() {
        return summonedTo != null;
    }

    public List<LivingEntity> threats(double range) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(range),
                e -> e instanceof Enemy && e.isAlive() && !hasPassenger(e));
    }

    // ------------------------------------------------------------------ syncing, like a boat

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, lerpXRot);
            lerpSteps--;
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (distanceToSqr(x, y, z) > 64 * 64) {
            // a portal jump: no gliding the whole way there
            lerpSteps = 0;
            setPos(x, y, z);
            setYRot(yRot);
            setXRot(xRot);
            return;
        }
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = 10;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    @Override
    public float lerpTargetYRot() {
        return lerpSteps > 0 ? (float) lerpYRot : getYRot();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.SPACE_CRUISER.get());
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setBooster(tag.getInt("Booster"));
        entityData.set(DATA_THRUSTERS, Mth.clamp(tag.getInt("Thrusters"), 0, CruiserTier.MAX));
        entityData.set(DATA_DRILL, Mth.clamp(tag.getInt("Drill"), 0, CruiserTier.MAX));
        power = tag.contains("Power") ? tag.getFloat("Power") : maxPower();
        entityData.set(DATA_POWER, Math.round(power));
        entityData.set(DATA_UPGRADES, tag.getInt("Upgrades"));
        entityData.set(DATA_WEAPON, tag.contains("Weapon") ? tag.getInt("Weapon") : -1);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Power", power);
        tag.putInt("Upgrades", entityData.get(DATA_UPGRADES));
        tag.putInt("Weapon", entityData.get(DATA_WEAPON));
        tag.putInt("Booster", booster());
        tag.putInt("Thrusters", tier(CruiserTier.THRUSTERS));
        tag.putInt("Drill", entityData.get(DATA_DRILL));
        if (owner != null) tag.putUUID("Owner", owner);
    }

    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    /** For tests: set how much power it has. */
    public void setPower(float value) {
        power = Mth.clamp(value, 0, maxPower());
        entityData.set(DATA_POWER, Math.round(power));
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return getType().getDimensions();
    }
}
