package com.rickandmorty.entity;

import com.mojang.authlib.GameProfile;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A Still Life of a person: the Backrooms' copy of someone who came through, in their skin, but
 * wrong (see {@link StillLifeForm}). Like the other Still Lifes it stands frozen, its head
 * turning to follow you, until you come too close or hit it; then it comes for you.
 * <p>
 * Whose skin: usually the nearest player's when it first appears, otherwise anyone who's ever
 * played in this world.
 */
public class StillLifeEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_AWAKE = SynchedEntityData.defineId(StillLifeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> DATA_PROFILE = SynchedEntityData.defineId(StillLifeEntity.class, EntityDataSerializers.COMPOUND_TAG);
    public static final double WAKE_DISTANCE = 7.0;

    private final StillLifeForm form;
    private @Nullable GameProfile profile;
    private boolean lookingUp;

    public StillLifeEntity(EntityType<? extends StillLifeEntity> type, Level level, StillLifeForm form) {
        super(type, level);
        this.form = form;
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes(StillLifeForm form) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, form.health)
                .add(Attributes.ATTACK_DAMAGE, form.damage)
                .add(Attributes.MOVEMENT_SPEED, form.speed)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, form == StillLifeForm.TWICE ? 0.8 : 0.2);
    }

    public StillLifeForm form() {
        return form;
    }

    public boolean isAwake() {
        return entityData.get(DATA_AWAKE);
    }

    public void wake() {
        if (!isAwake()) {
            entityData.set(DATA_AWAKE, true);
            playSound(ModSounds.STILL_LIFE_WAKE.get(), 1.4f, 0.6f);
        }
    }

    public @Nullable GameProfile profile() {
        return profile;
    }

    public void setProfile(GameProfile profile) {
        this.profile = profile;
        Tag tag = ExtraCodecs.GAME_PROFILE.encodeStart(NbtOps.INSTANCE, profile).result().orElse(null);
        entityData.set(DATA_PROFILE, tag instanceof CompoundTag compound ? compound : new CompoundTag());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AWAKE, false);
        builder.define(DATA_PROFILE, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_PROFILE.equals(key) && level().isClientSide) {
            profile = ExtraCodecs.GAME_PROFILE.parse(NbtOps.INSTANCE, entityData.get(DATA_PROFILE)).result().orElse(null);
        }
    }

    // ------------------------------------------------------------------ behaviour

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
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && profile == null && !lookingUp) {
            // usually, it's you
            lookingUp = true;
            Player near = server.getNearestPlayer(this, 64.0);
            var lookup = near != null && random.nextFloat() < 0.7f ? KnownPlayers.lookup(server.getServer(), near.getUUID())
                    : KnownPlayers.pickRandom(server.getServer(), random);
            lookup.thenAccept(found -> server.getServer().execute(() -> {
                lookingUp = false;
                if (!isRemoved() && profile == null) found.ifPresent(this::setProfile);
            }));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isAwake() && tickCount % 5 == 0) {
            Player player = level().getNearestPlayer(getX(), getY(), getZ(), WAKE_DISTANCE, e -> e instanceof Player p && p.canBeSeenAsEnemy());
            if (player != null && hasLineOfSight(player)) wake();
        }
        if (isAwake() && form == StillLifeForm.CAPTAIN && tickCount % 7 == 0 && random.nextInt(3) == 0) {
            playSound(ModSounds.STILL_LIFE_CLICK.get(), 0.8f, 0.6f + random.nextFloat() * 0.3f);
        }
    }

    /** The Captain moves in jerks: a few frames of motion, then a few of nothing. */
    public boolean frozenFrame() {
        return form == StillLifeForm.CAPTAIN && Math.floorMod(tickCount + getId() * 7, 14) < 5;
    }

    @Override
    public void travel(Vec3 input) {
        super.travel(frozenFrame() ? Vec3.ZERO : input);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            switch (form) {
                case MANY_HANDS -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));       // it holds on
                case THREE_FACES -> living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                case TWICE, PLANK_FACE -> living.knockback(1.2, getX() - living.getX(), getZ() - living.getZ());
                case COMMUTER -> living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                case NOODLE_ARMS -> {
                    // it reels you in
                    Vec3 pull = position().subtract(living.position()).normalize().scale(0.6);
                    living.push(pull.x, 0.15, pull.z);
                    living.hurtMarked = true;
                }
                case GAPING -> {
                    living.addEffect(new MobEffectInstance(com.rickandmorty.registry.ModEffects.BLEEDING, 100, 0));
                    living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1));
                }
                default -> {
                }
            }
        }
        return hit;
    }

    /** The long-armed ones reach further. */
    @Override
    protected net.minecraft.world.phys.AABB getAttackBoundingBox() {
        net.minecraft.world.phys.AABB box = super.getAttackBoundingBox();
        return form == StillLifeForm.NOODLE_ARMS || form == StillLifeForm.LOPSIDED ? box.inflate(1.2, 0.0, 1.2) : box;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) wake();
        return super.hurt(source, amount);
    }

    /** Now and then it leaves behind a head: the face it was wearing. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (form == StillLifeForm.PLANK_FACE) spawnAtLocation(new ItemStack(Items.OAK_PLANKS, 2 + random.nextInt(3)));
        if (form == StillLifeForm.COMMUTER && random.nextFloat() < 0.3f) spawnAtLocation(new ItemStack(Items.PAPER, 1 + random.nextInt(4)));
        if (profile != null && recentlyHit && random.nextFloat() < 0.15f) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(profile));
            spawnAtLocation(head);
        }
    }

    @Override
    public float getVoicePitch() {
        return 0.45f + random.nextFloat() * 0.15f;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isAwake() ? ModSounds.STILL_LIFE_VOICE.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STILL_LIFE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STILL_LIFE_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awake", isAwake());
        tag.put("Profile", entityData.get(DATA_PROFILE).copy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_AWAKE, tag.getBoolean("Awake"));
        ExtraCodecs.GAME_PROFILE.parse(NbtOps.INSTANCE, tag.getCompound("Profile")).result().ifPresent(this::setProfile);
    }
}
