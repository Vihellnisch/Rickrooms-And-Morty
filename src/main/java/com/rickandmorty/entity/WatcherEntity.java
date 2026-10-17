package com.rickandmorty.entity;

import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModSounds;
import com.rickandmorty.sanity.Sanity;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Someone standing far off, in a plain shirt and trousers, with blank white eyes. It turns up
 * anywhere, now and then. It never comes closer, and it can't hurt you. It just watches.
 * <p>
 * Notice it (look straight at it for a moment) and it's gone. Walk up to it or hit it and it's
 * gone. But if you never notice it, it gets bored of waiting: it's suddenly right behind you,
 * whistling softly in your ear, and gone before you can turn round.
 */
public class WatcherEntity extends PathfinderMob {
    private static final double NOTICE_GAZE = Math.cos(Math.toRadians(10));
    private static final double TURNED_ROUND = Math.cos(Math.toRadians(40));
    private static final int NOTICE_TICKS = 20;
    private static final int BEHIND_TICKS = 40;
    private static final double TOO_CLOSE = 12.0;

    private int patience = 600 + (int) (Math.random() * 600);
    private int noticed;
    /** Set once it's standing behind someone: who, and for how long it has been. */
    private @Nullable UUID behind;
    private int behindFor;

    public WatcherEntity(EntityType<? extends WatcherEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 128.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 128.0f, 1.0f));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (behind != null) {
            whistling();
            return;
        }
        Player near = level().getNearestPlayer(this, TOO_CLOSE);
        if (near != null && !near.isSpectator()) {
            vanish(null);
            return;
        }
        // looked at for long enough: noticed, and gone
        Player looking = null;
        for (Player player : level().players()) {
            if (!player.isSpectator() && looksAt(player, NOTICE_GAZE) && player.hasLineOfSight(this)) looking = player;
        }
        noticed = looking != null ? noticed + 1 : Math.max(0, noticed - 1);
        if (noticed >= NOTICE_TICKS) {
            vanish(looking);
            return;
        }
        if (tickCount >= patience) {
            Player target = level().getNearestPlayer(this, 160.0);
            if (target instanceof ServerPlayer player && !player.isSpectator() && stepBehind(player)) return;
            vanish(null);
        }
    }

    private boolean looksAt(Player player, double gaze) {
        Vec3 toMe = getEyePosition().subtract(player.getEyePosition()).normalize();
        return player.getViewVector(1.0f).dot(toMe) > gaze;
    }

    /** Right behind them, facing the back of their head. */
    private boolean stepBehind(ServerPlayer player) {
        Vec3 back = Vec3.directionFromRotation(0.0f, player.getYRot()).scale(-1.3);
        for (double side : new double[]{0.0, 0.6, -0.6}) {
            Vec3 spot = player.position().add(back).add(back.z * side, 0.0, -back.x * side);
            if (!level().noCollision(this, getDimensions(getPose()).makeBoundingBox(spot))) continue;
            float yaw = player.getYRot();
            moveTo(spot.x, spot.y, spot.z, yaw, 0.0f);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
            behind = player.getUUID();
            behindFor = 0;
            return true;
        }
        return false;
    }

    private void whistling() {
        Player player = level().getPlayerByUUID(behind);
        behindFor++;
        if (!(player instanceof ServerPlayer target) || target.isSpectator() || target.level() != level() || distanceToSqr(target) > 36) {
            vanish(null);
            return;
        }
        getLookControl().setLookAt(target, 180.0f, 180.0f);
        // an evil whoosh, and a breath of a whisper, right at their ear
        Vec3 ear = target.getEyePosition().add(position().subtract(target.position()).normalize().scale(0.5));
        if (behindFor == 2) level().playSound(null, ear.x, ear.y, ear.z, ModSounds.WATCHER_WHISTLE.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
        if (behindFor == 6) level().playSound(null, ear.x, ear.y, ear.z, ModSounds.WATCHER_WHISPER.get(), SoundSource.HOSTILE, 0.8f, 1.3f);
        if (behindFor == 11) {
            ModAdvancements.award(target, "behind_you");
            Sanity.add(target, -6.0f);
        }
        // turn round and there's nothing there
        if (behindFor > 12 && looksAt(target, TURNED_ROUND) || behindFor >= BEHIND_TICKS) vanish(null);
    }

    /** Gone. In the Backrooms, whoever caught its eye pays for it. */
    private void vanish(@Nullable Player noticedBy) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            if (noticedBy instanceof ServerPlayer player) {
                server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMBIENT_CAVE.value(), getSoundSource(), 1.5f, 0.5f);
                if (BackroomsLevel.isBackrooms(level().dimension())) Sanity.add(player, -8.0f);
                ModAdvancements.award(player, "it_saw_you");
            }
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isRemoved()) vanish(null);
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return distanceSqr > 96 * 96;   // it stands 32 to 64 blocks away on purpose: only despawn if everyone's gone
    }

    @Override
    public boolean requiresCustomPersistence() {
        return false;
    }

    /** Whether it has already slipped round behind someone (for tests). */
    public boolean isBehindSomeone() {
        return behind != null;
    }

    /** Makes it lose patience now (for tests and the tour). */
    public void loseInterest() {
        patience = Mth.clamp(tickCount, 0, patience);
    }
}
