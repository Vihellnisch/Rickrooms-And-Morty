package com.rickandmorty.entity;

import com.rickandmorty.cronenberg.Mutation;
import com.rickandmorty.registry.ModEffects;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Cronenberg. Crawlers and Stalkers hunt anything that still looks human; Blobs only lash out
 * at whoever comes too close. Players far enough gone (mutation stage 3+) look like family, and
 * are left alone unless they start a fight. Their blows spread the mutation.
 */
public class CronenbergEntity extends Monster {
    private final CronenbergKind kind;

    public CronenbergEntity(EntityType<? extends CronenbergEntity> type, Level level, CronenbergKind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = kind == CronenbergKind.BLOB ? 10 : 7;
        if (!level.isClientSide) registerKindGoals();   // registerGoals runs before kind is set
    }

    public static AttributeSupplier.Builder createAttributes(CronenbergKind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.health)
                .add(Attributes.ATTACK_DAMAGE, kind.damage)
                .add(Attributes.MOVEMENT_SPEED, kind.speed)
                .add(Attributes.ARMOR, kind == CronenbergKind.BLOB ? 4.0 : 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind == CronenbergKind.BLOB ? 0.6 : 0.1)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    public CronenbergKind kind() {
        return kind;
    }

    /** Whether a player still looks like prey to a Cronenberg. */
    public static boolean looksHuman(LivingEntity entity) {
        return !(entity instanceof Player player) || Mutation.stage(player) < 3;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, CronenbergEntity.class).setAlertOthers(CronenbergEntity.class));
    }

    /** The goals that depend on the kind: crawlers leap, blobs only defend their patch. */
    private void registerKindGoals() {
        if (kind == CronenbergKind.CRAWLER) goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, kind == CronenbergKind.BLOB ? 1.0 : 1.2, false));
        double reach = kind == CronenbergKind.BLOB ? 4.5 : 64.0;
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> looksHuman(target) && target.distanceToSqr(this) < reach * reach));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            Gore.wound(living, (float) kind.damage);
            if (living instanceof ServerPlayer player) Mutation.expose(player, 2.0f);
            if (kind == CronenbergKind.STALKER) living.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 60), this);
            if (kind == CronenbergKind.BLOB) living.addEffect(new MobEffectInstance(MobEffects.POISON, 60), this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) Gore.wound(this, amount);
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) return;
        Gore.burst(this);
        if (kind == CronenbergKind.BLOB) {
            // it pops, and what was inside it hangs in the air
            AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
            cloud.setRadius(2.5f);
            cloud.setRadiusOnUse(-0.3f);
            cloud.setWaitTime(5);
            cloud.setDuration(160);
            cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
            cloud.setPotionContents(new PotionContents(Potions.POISON));
            level().addFreshEntity(cloud);
        }
    }

    /**
     * Cronenbergs spawn in daylight too (the world belongs to them now), though far fewer than at night.
     */
    public static boolean checkSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType spawnType,
                                          BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (spawnType == MobSpawnType.NATURAL && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) > 10
                && level.getLevel().isDay() && random.nextFloat() > 0.3f) {
            return false;
        }
        return checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public float getVoicePitch() {
        return (kind == CronenbergKind.CRAWLER ? 0.9f : kind == CronenbergKind.BLOB ? 0.5f : 0.7f) + random.nextFloat() * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRONENBERG_AMBIENT.get();
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
        playSound(ModSounds.CRONENBERG_STEP.get(), 0.4f, getVoicePitch());
    }
}
