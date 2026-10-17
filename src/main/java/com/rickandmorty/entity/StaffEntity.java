package com.rickandmorty.entity;

import com.rickandmorty.registry.ModItems;
import com.rickandmorty.registry.ModSounds;
import com.rickandmorty.worldgen.level.StoreHours;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Staff of the Infinite IKEA: very tall, faceless, in the yellow striped shirt, with arms far too
 * long and fingers far too many. While the store is open they shuffle about restocking shelves
 * and leave you alone (hit one and its colleagues come to help). When the store closes for the
 * night, they come looking for the customers who didn't leave. Holding a Staff Badge, they take
 * you for one of them.
 */
public class StaffEntity extends Monster {
    public static final int VARIANTS = 3;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(StaffEntity.class, EntityDataSerializers.INT);
    private static final ResourceLocation NIGHT_SPEED = com.rickandmorty.RickAndMortyMod.id("store_closed");
    private static final int LINES = 4;

    /** Ticks left of being angry about being hit (by day). */
    private int anger;
    private @Nullable LivingEntity angryAt;

    public StaffEntity(EntityType<? extends StaffEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public int variant() {
        return Math.floorMod(entityData.get(DATA_VARIANT), VARIANTS);
    }

    public void setVariant(int variant) {
        entityData.set(DATA_VARIANT, Math.floorMod(variant, VARIANTS));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
    }

    /** The store is closed (it's night), so customers are fair game. */
    public boolean storeClosed() {
        return StoreHours.closed(level());
    }

    /** A Staff Badge in either hand: one of us. */
    public static boolean wearsBadge(Player player) {
        return player.getMainHandItem().is(ModItems.STAFF_BADGE.get()) || player.getOffhandItem().is(ModItems.STAFF_BADGE.get());
    }

    private boolean hunts(LivingEntity target) {
        if (!(target instanceof Player player) || player.isCreative() || player.isSpectator()) return false;
        if (anger > 0 && target == angryAt) return true;
        return storeClosed() && !wearsBadge(player);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new Restock(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f) {
            @Override
            public boolean canUse() {
                return !storeClosed() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, StaffEntity.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, this::hunts));
    }

    /** By day: stand at a shelf for a while, as if putting things out on it. */
    private static final class Restock extends Goal {
        private final StaffEntity mob;
        private int time;

        Restock(StaffEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !mob.storeClosed() && mob.getTarget() == null && mob.getRandom().nextInt(200) == 0;
        }

        @Override
        public void start() {
            time = 100 + mob.getRandom().nextInt(200);
            mob.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return time > 0 && mob.getTarget() == null && !mob.storeClosed();
        }

        @Override
        public void tick() {
            time--;
            if (time % 40 == 0) mob.setYRot(mob.getYRot() + (mob.getRandom().nextFloat() - 0.5f) * 30.0f);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (anger > 0) anger--;
        boolean closed = storeClosed();
        // at night they move faster
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (closed && !speed.hasModifier(NIGHT_SPEED)) {
                speed.addTransientModifier(new AttributeModifier(NIGHT_SPEED, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else if (!closed && speed.hasModifier(NIGHT_SPEED)) {
                speed.removeModifier(NIGHT_SPEED);
            }
        }
        // the store opens: back to work, unless someone hit them
        LivingEntity target = getTarget();
        if (target != null && !hunts(target)) setTarget(null);
        // they talk to customers
        if (tickCount % 20 == 0 && random.nextInt(closed ? 6 : 12) == 0) {
            Player near = level().getNearestPlayer(this, closed ? 14.0 : 7.0);
            if (near instanceof ServerPlayer player && !player.isSpectator()) {
                String key = "message.rickrooms.staff." + (closed ? "night." : "day.") + random.nextInt(LINES);
                player.displayClientMessage(Component.translatable(key).withStyle(closed ? ChatFormatting.RED : ChatFormatting.YELLOW), true);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof Player attacker && !attacker.isCreative()) {
            // the colleagues come to help
            for (StaffEntity staff : level().getEntitiesOfClass(StaffEntity.class, getBoundingBox().inflate(20))) {
                staff.anger = 600;
                staff.angryAt = attacker;
                staff.setTarget(attacker);
            }
        }
        return hurt;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        setVariant(random.nextInt(VARIANTS));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return storeClosed() ? ModSounds.STAFF_CLOSED.get() : ModSounds.STAFF_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STAFF_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STAFF_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
        tag.putInt("Anger", anger);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
        anger = tag.getInt("Anger");
    }

    public int anger() {
        return anger;
    }
}
