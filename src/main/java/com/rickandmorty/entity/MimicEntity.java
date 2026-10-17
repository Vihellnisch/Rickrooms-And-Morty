package com.rickandmorty.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A "Still Life": the Backrooms' imperfect copy of a vanilla mob. It stands frozen like a
 * statue, silently turning its head to follow you. Get too close (or hit it) and it wakes
 * up, trembling, and hunts you down.
 */
public class MimicEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_AWAKE = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);
    public static final double WAKE_DISTANCE = 7.0;

    private final MimicKind kind;

    public MimicEntity(EntityType<? extends MimicEntity> type, Level level, MimicKind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes(MimicKind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.health)
                .add(Attributes.ATTACK_DAMAGE, kind.damage)
                .add(Attributes.MOVEMENT_SPEED, kind.speed)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public MimicKind kind() {
        return kind;
    }

    public boolean isAwake() {
        return entityData.get(DATA_AWAKE);
    }

    public void wake() {
        if (!isAwake()) {
            entityData.set(DATA_AWAKE, true);
            playSound(kind.hurt, 1.5f, 0.3f);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AWAKE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        // Asleep or not, it never takes its eyes off you.
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0f, 1.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isAwake() && tickCount % 5 == 0) {
            // same rule vanilla monsters use to pick targets: no creative or spectator players
            Player player = level().getNearestPlayer(getX(), getY(), getZ(), WAKE_DISTANCE,
                    e -> e instanceof Player p && p.canBeSeenAsEnemy());
            if (player != null && hasLineOfSight(player)) {
                wake();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) wake();
        return super.hurt(source, amount);
    }

    /** Deep, wrong-sounding voices. */
    @Override
    public float getVoicePitch() {
        return 0.4f + random.nextFloat() * 0.15f;
    }

    /** Statues are silent. */
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isAwake() ? kind.ambient : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return kind.hurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return kind.death;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awake", isAwake());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_AWAKE, tag.getBoolean("Awake"));
    }
}
