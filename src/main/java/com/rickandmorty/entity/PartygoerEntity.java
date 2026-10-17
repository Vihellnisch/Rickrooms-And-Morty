package com.rickandmorty.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A Partygoer of Level Fun: bright yellow, with a smile far too wide. At first it just wants you
 * to join the party. It follows you around at a polite distance, giggling, asking you to stay.
 * Stay near them long enough, or hurt one, and the smile changes, and they all come for you. =)
 */
public class PartygoerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_HOSTILE = SynchedEntityData.defineId(PartygoerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String[] INVITES = {"partygoer.invite.0", "partygoer.invite.1", "partygoer.invite.2", "partygoer.invite.3", "partygoer.invite.4"};

    /** Ticks spent near someone, and how long it will pretend. */
    private int company;
    private int patience = 500 + (int) (Math.random() * 700);

    public PartygoerEntity(EntityType<? extends PartygoerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    public boolean isHostile() {
        return entityData.get(DATA_HOSTILE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HOSTILE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true) {
            @Override
            public boolean canUse() {
                return isHostile() && super.canUse();
            }
        });
        goalSelector.addGoal(3, new Tag(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> isHostile()));
    }

    /** While friendly: tag along after the nearest player, never quite touching them. */
    private static final class Tag extends Goal {
        private final PartygoerEntity mob;
        private @Nullable Player player;

        Tag(PartygoerEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.isHostile()) return false;
            player = mob.level().getNearestPlayer(mob, 16.0);
            return player != null && !player.isSpectator() && mob.distanceToSqr(player) > 9;
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.isHostile() && player != null && player.isAlive() && mob.distanceToSqr(player) > 9 && mob.distanceToSqr(player) < 400;
        }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(player, 30.0f, 30.0f);
            if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(player, 0.9);
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isHostile()) return;
        Player near = level().getNearestPlayer(this, 8.0);
        if (near == null || near.isSpectator() || near.isCreative()) return;
        company++;
        if (near instanceof ServerPlayer player && random.nextInt(160) == 0) {
            player.displayClientMessage(Component.translatable("message.rickrooms." + INVITES[random.nextInt(INVITES.length)])
                    .withStyle(ChatFormatting.YELLOW), true);
            playSound(SoundEvents.WITCH_CELEBRATE, 0.5f, 1.7f + random.nextFloat() * 0.2f);
        }
        if (company > patience) turn(true);
    }

    /** The smile changes. The ones nearby follow soon after. */
    public void turn(boolean spread) {
        if (isHostile()) return;
        entityData.set(DATA_HOSTILE, true);
        playSound(SoundEvents.WITCH_CELEBRATE, 1.2f, 0.55f);
        playSound(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.8f, 0.7f);
        Player near = level().getNearestPlayer(this, 24.0);
        if (near instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.rickrooms.partygoer.turn").withStyle(ChatFormatting.RED), true);
        }
        if (spread) {
            for (PartygoerEntity other : level().getEntitiesOfClass(PartygoerEntity.class, getBoundingBox().inflate(24))) {
                if (other != this) other.patience = Math.min(other.patience, other.company + 20 + random.nextInt(60));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof Player) {
            turn(false);
            for (PartygoerEntity other : level().getEntitiesOfClass(PartygoerEntity.class, getBoundingBox().inflate(24))) other.turn(false);
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return isHostile() && super.doHurtTarget(target);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isHostile() ? SoundEvents.WITCH_AMBIENT : null;
    }

    @Override
    public float getVoicePitch() {
        return isHostile() ? 0.6f : 1.6f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FIREWORK_ROCKET_BLAST;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hostile", isHostile());
        tag.putInt("Company", company);
        tag.putInt("Patience", patience);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_HOSTILE, tag.getBoolean("Hostile"));
        company = tag.getInt("Company");
        if (tag.contains("Patience")) patience = tag.getInt("Patience");
    }
}
