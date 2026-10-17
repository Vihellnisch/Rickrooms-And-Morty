package com.rickandmorty.entity;

import com.mojang.authlib.GameProfile;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A member of the Council: an alternate-dimension version of a player who has been on this
 * world, wearing their skin but not quite their shape (see {@link CouncilLook}), and usually a
 * silly hat. They trade the rarest things in the mod for Portal Fluid. Hit one and the whole
 * Council turns on you.
 */
public class CouncilMemberEntity extends AbstractVillager {
    private static final EntityDataAccessor<CompoundTag> DATA_PROFILE = SynchedEntityData.defineId(CouncilMemberEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> DATA_LOOK = SynchedEntityData.defineId(CouncilMemberEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_DESIGNATION = SynchedEntityData.defineId(CouncilMemberEntity.class, EntityDataSerializers.STRING);
    private static final int RESTOCK_INTERVAL = 24000;
    private static final String[] GREEK = {"Alpha", "Beta", "Gamma", "Delta", "Zeta", "Theta", "Omega", "Prime"};

    @Nullable
    private GameProfile profile;
    private boolean lookingUpProfile;
    @Nullable
    private BlockPos home;
    private long lastRestock;
    private int lastChat = -1000;

    public CouncilMemberEntity(EntityType<? extends CouncilMemberEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // ------------------------------------------------------------ who they are

    @Nullable
    public GameProfile profile() {
        return profile;
    }

    public CouncilLook look() {
        return CouncilLook.unpack(entityData.get(DATA_LOOK));
    }

    public void setProfile(GameProfile profile) {
        this.profile = profile;
        Tag tag = ExtraCodecs.GAME_PROFILE.encodeStart(NbtOps.INSTANCE, profile).result().orElse(null);
        entityData.set(DATA_PROFILE, tag instanceof CompoundTag compound ? compound : new CompoundTag());
    }

    /** Like "Steve C-137" or "Alex J-19-Zeta". */
    public static String randomDesignation(RandomSource random) {
        char letter = (char) ('A' + random.nextInt(26));
        String code = letter + "-" + (1 + random.nextInt(199));
        return random.nextFloat() < 0.35f ? code + "-" + GREEK[random.nextInt(GREEK.length)] : code;
    }

    @Override
    public Component getName() {
        if (hasCustomName() || profile == null) return super.getName();
        return Component.literal(profile.getName() + " " + entityData.get(DATA_DESIGNATION));
    }

    @Override
    public boolean shouldShowName() {
        return profile != null || super.shouldShowName();
    }

    public void setHome(BlockPos pos, int radius) {
        home = pos;
        restrictTo(pos, radius);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFILE, new CompoundTag());
        builder.define(DATA_LOOK, 0);
        builder.define(DATA_DESIGNATION, "C-137");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_PROFILE.equals(key) && level().isClientSide) {
            profile = ExtraCodecs.GAME_PROFILE.parse(NbtOps.INSTANCE, entityData.get(DATA_PROFILE)).result().orElse(null);
        } else if (DATA_LOOK.equals(key)) {
            refreshDimensions();
        }
    }

    /** The hitbox follows the odd body shape. */
    @Override
    protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        if (entityData == null) return super.getDefaultDimensions(pose);   // still being constructed
        CouncilLook.Shape shape = look().shape();
        float height = shape.scaleY * (0.75f + 0.25f * shape.head);
        return super.getDefaultDimensions(pose).scale(Math.max(shape.scaleX, shape.scaleZ), height);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (entityData.get(DATA_LOOK) == 0) {
                entityData.set(DATA_LOOK, CouncilLook.random(random).pack());
                entityData.set(DATA_DESIGNATION, randomDesignation(random));
            }
            if (profile == null && !lookingUpProfile) {
                lookingUpProfile = true;
                KnownPlayers.pickRandom(server.getServer(), random).thenAccept(found ->
                        server.getServer().execute(() -> {
                            lookingUpProfile = false;
                            if (!isRemoved() && profile == null) found.ifPresent(this::setProfile);
                        }));
            }
        }
    }

    // ------------------------------------------------------------ behaviour

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.9, false) {
            @Override
            public boolean canUse() {
                return !isTrading() && super.canUse();
            }
        });
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.45));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.35));
        // They stare at you. Especially the one wearing your face.
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0f, 0.9f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(CouncilMemberEntity.class));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long time = level().getGameTime();
        if (time - lastRestock > RESTOCK_INTERVAL && offers != null) {
            offers.forEach(MerchantOffer::resetUses);
            lastRestock = time;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof net.minecraft.world.item.SpawnEggItem || !isAlive() || isTrading()
                || getTarget() == player) {
            return super.mobInteract(player, hand);
        }
        if (hand == InteractionHand.MAIN_HAND) player.awardStat(Stats.TALKED_TO_VILLAGER);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "council");
            if (tickCount - lastChat > 200) {
                lastChat = tickCount;
                boolean original = profile != null && profile.getId().equals(player.getUUID());
                String line = original ? "original" : String.valueOf(1 + random.nextInt(6));
                player.sendSystemMessage(Component.translatable("npc.rickrooms.chat", getDisplayName().copy().withStyle(ChatFormatting.AQUA),
                        Component.translatable("npc.rickrooms.council." + line, player.getDisplayName())));
            }
            if (!getOffers().isEmpty()) {
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** What the Council has to offer (for Portal Fluid, the only currency that matters across dimensions). */
    private static final List<Supplier<MerchantOffer>> POOL = List.of(
            () -> fluid(2, new ItemStack(ModItems.ALMOND_WATER.get(), 2), 6),
            () -> fluid(3, new ItemStack(ModItems.SZECHUAN_SAUCE.get()), 4),
            () -> fluid(4, new ItemStack(ModItems.MEESEEKS_BOX.get()), 3),
            () -> fluid(6, new ItemStack(ModItems.TARGETING_CHIP.get()), 1),
            () -> fluid(8, new ItemStack(ModItems.BLENDER_CHIP.get()), 1),
            () -> fluid(6, new ItemStack(ModItems.BACKROOMS_COORDINATES.get()), 1),
            () -> fluid(10, new ItemStack(ModItems.CRONENBERG_COORDINATES.get()), 1),
            () -> fluid(5, new ItemStack(ModItems.CURE_SERUM.get()), 3),
            () -> fluid(8, new ItemStack(ModItems.MICROVERSE_BATTERY.get()), 2),
            () -> fluid(3, new ItemStack(ModItems.PLUMBUS.get()), 3),
            () -> fluid(12, new ItemStack(Items.NETHERITE_SCRAP, 2), 2),
            () -> new MerchantOffer(new ItemCost(ModItems.MUTAGEN_SAC.get(), 3), new ItemStack(ModItems.PORTAL_FLUID.get()), 8, 5, 0.05f),
            () -> new MerchantOffer(new ItemCost(Items.DIAMOND, 2), new ItemStack(ModItems.PORTAL_FLUID.get()), 8, 5, 0.05f),
            () -> new MerchantOffer(new ItemCost(ModItems.PORTAL_FLUID_DROP.get(), 6), new ItemStack(ModItems.PORTAL_FLUID.get(), 2), 6, 5, 0.05f));

    private static MerchantOffer fluid(int count, ItemStack result, int uses) {
        return new MerchantOffer(new ItemCost(ModItems.PORTAL_FLUID.get(), count), result, uses, 10, 0.05f);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        List<Supplier<MerchantOffer>> pool = new ArrayList<>(POOL);
        int count = 4 + random.nextInt(3);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            offers.add(pool.remove(random.nextInt(pool.size())).get());
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean accepted) {
        return accepted ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.VILLAGER_NO;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Profile", entityData.get(DATA_PROFILE).copy());
        tag.putInt("Look", entityData.get(DATA_LOOK));
        tag.putString("Designation", entityData.get(DATA_DESIGNATION));
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putLong("LastRestock", lastRestock);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        CompoundTag saved = tag.getCompound("Profile");
        ExtraCodecs.GAME_PROFILE.parse(NbtOps.INSTANCE, saved).result().ifPresent(this::setProfile);
        if (tag.contains("Look")) entityData.set(DATA_LOOK, tag.getInt("Look"));
        if (tag.contains("Designation")) entityData.set(DATA_DESIGNATION, tag.getString("Designation"));
        NbtUtils.readBlockPos(tag, "Home").ifPresent(pos -> setHome(pos, 8));
        lastRestock = tag.getLong("LastRestock");
    }
}
