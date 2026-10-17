package com.rickandmorty.entity;

import com.rickandmorty.cronenberg.Mutation;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Cronenberg Behemoth: a whole family (and the house, and the dog) fused into one mountain
 * of meat. It sleeps at the bottom of a Flesh Pit until someone wanders too close. Awake, it
 * slams the ground, knocking everyone flying, and gives birth to Crawlers.
 */
public class BehemothEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_AWAKE = SynchedEntityData.defineId(BehemothEntity.class, EntityDataSerializers.BOOLEAN);
    /** Client: tick of the last slam, for the animation. */
    private static final EntityDataAccessor<Integer> DATA_SLAM = SynchedEntityData.defineId(BehemothEntity.class, EntityDataSerializers.INT);
    public static final double WAKE_DISTANCE = 18.0;
    private static final int SLAM_COOLDOWN = 140;
    private static final int BIRTH_COOLDOWN = 320;
    private static final int MAX_BROOD = 6;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int slamCooldown = 60;
    private int birthCooldown = 160;

    public BehemothEntity(EntityType<? extends BehemothEntity> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        bossBar.setVisible(false);
        bossBar.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    public boolean isAwake() {
        return entityData.get(DATA_AWAKE);
    }

    public int lastSlamTick() {
        return entityData.get(DATA_SLAM);
    }

    public void wake() {
        if (isAwake()) return;
        entityData.set(DATA_AWAKE, true);
        bossBar.setVisible(true);
        playSound(SoundEvents.WARDEN_ROAR, 4.0f, 0.55f);
        playSound(SoundEvents.RAVAGER_ROAR, 4.0f, 0.5f);
        Gore.splash((ServerLevel) level(), position().add(0, 2.5, 0), 60, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AWAKE, false);
        builder.define(DATA_SLAM, -100);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 20.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> isAwake() && CronenbergEntity.looksHuman(target)) {
        });
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        if (!isAwake()) {
            if (tickCount % 10 == 0) {
                Player player = level.getNearestPlayer(getX(), getY(), getZ(), WAKE_DISTANCE, e -> e instanceof Player p && p.canBeSeenAsEnemy());
                if (player != null && hasLineOfSight(player)) wake();
            }
            return;
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        if (slamCooldown > 0) slamCooldown--;
        if (birthCooldown > 0) birthCooldown--;
        if (target == null || !target.isAlive()) return;
        if (slamCooldown == 0 && onGround() && distanceToSqr(target) < 8.0 * 8.0) {
            slam(level);
            slamCooldown = SLAM_COOLDOWN;
        }
        if (birthCooldown == 0) {
            giveBirth(level);
            birthCooldown = BIRTH_COOLDOWN;
        }
    }

    /** Heaves itself up and slams down: everything nearby gets hurt and thrown. */
    private void slam(ServerLevel level) {
        entityData.set(DATA_SLAM, tickCount);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.5f);
        playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 3.0f, 0.6f);
        BlockState ground = level.getBlockState(blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.2, getZ(), 160, 3.0, 0.3, 3.0, 0.4);
        Gore.splash(level, position().add(0, 0.5, 0), 50, 2.5);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0, 2.0, 6.0),
                e -> e != this && !(e instanceof CronenbergEntity) && e.isAlive())) {
            if (victim instanceof Player player && !player.canBeSeenAsEnemy()) continue;
            Vec3 push = victim.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.6);
            victim.hurt(damageSources().mobAttack(this), 10.0f);
            victim.push(push.x, 0.8, push.z);
            victim.hurtMarked = true;
            if (victim instanceof ServerPlayer player) Mutation.expose(player, 5.0f);
        }
    }

    /** Something tears its way out of it. */
    private void giveBirth(ServerLevel level) {
        int brood = level.getEntitiesOfClass(CronenbergEntity.class, getBoundingBox().inflate(24.0)).size();
        if (brood >= MAX_BROOD) return;
        playSound(SoundEvents.SNIFFER_EGG_HATCH, 2.0f, 0.4f);
        playSound(SoundEvents.SLIME_SQUISH, 2.0f, 0.4f);
        for (int i = 0; i < 2; i++) {
            CronenbergEntity crawler = ModEntities.CRONENBERGS.get(CronenbergKind.CRAWLER).get().create(level);
            if (crawler == null) continue;
            double angle = random.nextDouble() * Math.PI * 2;
            crawler.moveTo(getX() + Math.cos(angle) * 2.0, getY() + 1.0, getZ() + Math.sin(angle) * 2.0, random.nextFloat() * 360.0f, 0.0f);
            crawler.finalizeSpawn(level, level.getCurrentDifficultyAt(crawler.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            crawler.setTarget(getTarget());
            level.addFreshEntity(crawler);
            Gore.splash(level, crawler.position().add(0, 0.5, 0), 25, 0.6);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            Gore.wound(living, 14.0f);
            if (living instanceof ServerPlayer player) Mutation.expose(player, 5.0f);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            wake();
            Gore.wound(this, amount);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) return;
        Gore.burst(this);
        Gore.splash((ServerLevel) level(), position().add(0, 1.5, 0), 120, 2.0);
        playSound(SoundEvents.WARDEN_DEATH, 4.0f, 0.5f);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void setCustomName(@org.jetbrains.annotations.Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 0.4f + random.nextFloat() * 0.1f;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isAwake() ? ModSounds.BEHEMOTH_AMBIENT.get() : SoundEvents.WARDEN_HEARTBEAT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRONENBERG_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRONENBERG_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 1.2f, 0.6f);
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
        bossBar.setVisible(isAwake());
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }
}
