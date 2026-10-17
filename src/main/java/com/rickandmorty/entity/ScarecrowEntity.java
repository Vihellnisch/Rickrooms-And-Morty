package com.rickandmorty.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.rickandmorty.item.ScarecrowLanternItem;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A scarecrow in the Harvest: black coat, sackcloth face, arms out. While anyone is looking at
 * it, it is exactly what it looks like. The moment nobody is, it comes for you, quickly and
 * without a sound except the rustle of the corn.
 */
public class ScarecrowEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_WATCHED = SynchedEntityData.defineId(ScarecrowEntity.class, EntityDataSerializers.BOOLEAN);
    /** How wide a player's gaze is, as the cosine of the half-angle. */
    private static final double GAZE = Math.cos(Math.toRadians(62));
    private float frozenYaw;

    public ScarecrowEntity(EntityType<? extends ScarecrowEntity> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public boolean isWatched() {
        return entityData.get(DATA_WATCHED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WATCHED, true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return !isWatched() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isWatched() && super.canContinueToUse();
            }
        });
        // whoever holds a Scarecrow Lantern is left alone: they back away from its light
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, (float) ScarecrowLanternItem.RANGE, 1.1, 1.4,
                p -> p instanceof Player player && ScarecrowLanternItem.isHeldBy(player)));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                p -> !(p instanceof Player player && ScarecrowLanternItem.isHeldBy(player))));
    }

    /** Whether any player can see it right now (in their field of view, with nothing in the way). */
    public boolean seenByAnyone() {
        for (Player player : level().players()) {
            if (player.isSpectator() || player.distanceToSqr(this) > 96 * 96) continue;
            Vec3 toMe = getEyePosition().subtract(player.getEyePosition()).normalize();
            if (player.getViewVector(1.0f).dot(toMe) > GAZE && player.hasLineOfSight(this)) return true;
        }
        return false;
    }

    @Override
    protected void customServerAiStep() {
        boolean watched = seenByAnyone() || inTorchlight();
        if (watched != isWatched()) {
            entityData.set(DATA_WATCHED, watched);
            if (watched) frozenYaw = getYRot();
        }
        if (watched) {
            // exactly still: no walking, no turning
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            setYRot(frozenYaw);
            setYBodyRot(frozenYaw);
            setYHeadRot(frozenYaw);
            return;
        }
        if (getTarget() instanceof Player player && ScarecrowLanternItem.isHeldBy(player)) setTarget(null);
        super.customServerAiStep();
        // the corn rustles as it pushes through: the only warning you get
        if (tickCount % 8 == 0 && getDeltaMovement().horizontalDistanceSqr() > 0.0004) {
            playSound(SoundEvents.GRASS_STEP, 1.0f, 0.55f + random.nextFloat() * 0.25f);
        }
    }

    /** Scarecrows can't move in bright torchlight. */
    public boolean inTorchlight() {
        return level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, blockPosition().above()) >= 10;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isWatched() || target instanceof Player player && ScarecrowLanternItem.isHeldBy(player)) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit) playSound(SoundEvents.CROP_BREAK, 1.0f, 0.5f);
        return hit;
    }

    @Override
    public boolean isPushable() {
        return !isWatched();
    }

    @Override
    public void travel(Vec3 input) {
        if (!level().isClientSide && isWatched()) {
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(input);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOL_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOL_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CROP_BREAK, 0.25f, 0.6f + random.nextFloat() * 0.3f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("FrozenYaw", frozenYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        frozenYaw = tag.getFloat("FrozenYaw");
    }
}
