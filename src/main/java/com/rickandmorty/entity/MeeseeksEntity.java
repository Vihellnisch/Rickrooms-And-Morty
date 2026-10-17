package com.rickandmorty.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * "I'm Mr. Meeseeks, look at me!" Summoned from a Meeseeks Box, he follows whoever pressed
 * the button, fights any monster nearby (and anything that hurts his owner), and poofs out
 * of existence once his time is up.
 */
public class MeeseeksEntity extends PathfinderMob {
    public static final int LIFETIME = 1200;  // one minute of existence is pain enough

    @Nullable
    private UUID owner;
    private int life = LIFETIME;

    public MeeseeksEntity(EntityType<? extends MeeseeksEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
    }

    public void setLife(int ticks) {
        this.life = ticks;
    }

    public @Nullable Player getOwner() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(3, new FollowOwnerGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (--life <= 0) {
            poof();
            return;
        }
        // Defend the owner from whatever just hit them.
        Player player = getOwner();
        if (player != null && getTarget() == null) {
            LivingEntity attacker = player.getLastHurtByMob();
            if (attacker != null && attacker != this && attacker.isAlive() && !(attacker instanceof MeeseeksEntity)) {
                setTarget(attacker);
            }
        }
    }

    private void poof() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0f, 0.6f);
        }
        Player player = getOwner();
        if (player != null && distanceTo(player) < 32) {
            player.displayClientMessage(Component.translatable("message.rickrooms.meeseeks.poof").withStyle(ChatFormatting.AQUA), true);
        }
        discard();
    }

    /** Meeseeks don't fight their owner, or each other. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof MeeseeksEntity || (owner != null && source.getEntity() != null && owner.equals(source.getEntity().getUUID()))) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target instanceof Enemy && super.canAttack(target);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_YES;  // "Ooh-wee!"
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.6f + random.nextFloat() * 0.2f;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Life", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        life = tag.contains("Life") ? tag.getInt("Life") : LIFETIME;
    }

    /** Stays near the owner; teleports back if left far behind. */
    private class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player player = getOwner();
            return player != null && getTarget() == null && distanceToSqr(player) > 36;
        }

        @Override
        public void tick() {
            Player player = getOwner();
            if (player == null) return;
            if (distanceToSqr(player) > 24 * 24) {
                moveTo(player.getX(), player.getY(), player.getZ(), getYRot(), getXRot());
                getNavigation().stop();
            } else {
                getNavigation().moveTo(player, 1.2);
            }
        }
    }
}
