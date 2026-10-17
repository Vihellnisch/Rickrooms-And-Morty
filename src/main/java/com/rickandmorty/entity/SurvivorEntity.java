package com.rickandmorty.entity;

import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.trading.ItemCost;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * A survivor of Cronenberg World (see {@link SurvivorKind}). They trade for what the wasteland
 * has plenty of: Cronenberg flesh, bone plates and mutagen. Camp survivors defend their home;
 * Cronenberg Rick and Morty wander up to you, chat, trade, and leave through a portal.
 */
public class SurvivorEntity extends AbstractVillager {
    /** Wanderers stay this long before portalling out. */
    private static final int WANDER_LIFETIME = 6000;
    private static final int RESTOCK_INTERVAL = 24000;

    private final SurvivorKind kind;
    @Nullable
    private BlockPos home;
    private long lastRestock;
    private int lastChat = -1000;
    private boolean greeted;

    public SurvivorEntity(EntityType<? extends SurvivorEntity> type, Level level, SurvivorKind kind) {
        super(type, level);
        this.kind = kind;
        if (!level.isClientSide) registerKindGoals();   // registerGoals runs before kind is set
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(kind.held.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public SurvivorKind kind() {
        return kind;
    }

    /** Keep them around their camp. */
    public void setHome(BlockPos pos, int radius) {
        home = pos;
        restrictTo(pos, radius);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.8, false) {
            @Override
            public boolean canUse() {
                return !isTrading() && super.canUse();
            }
        });
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.5));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.4));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(SurvivorEntity.class));
    }

    /** The goals that depend on who this is. */
    private void registerKindGoals() {
        if (kind.wanderer) goalSelector.addGoal(3, new ApproachPlayerGoal());
        if (!kind.wanderer) {
            // camp survivors go after whatever comes over the wall
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                    e -> (e instanceof CronenbergEntity || e instanceof MantisEntity) && (home == null || e.blockPosition().closerThan(home, 14))));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 40 == 0 && getHealth() < getMaxHealth()) heal(1.0f);
        long time = level().getGameTime();
        if (time - lastRestock > RESTOCK_INTERVAL && offers != null) {
            offers.forEach(MerchantOffer::resetUses);
            lastRestock = time;
        }
        if (kind.wanderer) {
            if (!greeted && tickCount % 10 == 0) {
                Player player = level().getNearestPlayer(this, 6.0);
                if (player != null && !player.isSpectator()) greet((ServerLevel) level());
            }
            if (tickCount > WANDER_LIFETIME && !isTrading()) leaveThroughPortal((ServerLevel) level());
        }
    }

    /** Cronenberg Rick (or Morty) notices you. */
    private void greet(ServerLevel level) {
        greeted = true;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(12.0))) {
            player.sendSystemMessage(chatLine("greeting"));
            ModAdvancements.award(player, "close_rick_counters");
        }
    }

    private void leaveThroughPortal(ServerLevel level) {
        float yaw = getYRot();
        PortalEntity.spawnExitPortal(level, new PortalDestinations.Destination("", level.dimension(), getX(), getY(), getZ(), yaw), false);
        PortalEntity.burst(level, position().add(0, 1, 0));
        playSound(com.rickandmorty.registry.ModSounds.PORTAL_TRAVEL.get(), 1.0f, 1.0f);
        discard();
    }

    private Component chatLine(String line) {
        return Component.translatable("npc.rickrooms.chat", getDisplayName().copy().withStyle(ChatFormatting.GOLD),
                Component.translatable("npc.rickrooms." + kind.id() + "." + line));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof net.minecraft.world.item.SpawnEggItem || !isAlive() || isTrading()) {
            return super.mobInteract(player, hand);
        }
        if (hand == InteractionHand.MAIN_HAND) player.awardStat(Stats.TALKED_TO_VILLAGER);
        if (!level().isClientSide) {
            if (tickCount - lastChat > 200) {
                lastChat = tickCount;
                player.sendSystemMessage(chatLine(String.valueOf(1 + random.nextInt(kind.lines))));
            }
            if (!getOffers().isEmpty()) {
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        switch (kind) {
            case JERRY -> {
                offers.add(offer(ModItems.BONE_PLATE.get(), 5, new ItemStack(ModItems.SURVIVOR_HELMET.get()), 3));
                offers.add(offer(ModItems.BONE_PLATE.get(), 8, new ItemStack(ModItems.SURVIVOR_CHESTPLATE.get()), 3));
                offers.add(offer(ModItems.BONE_PLATE.get(), 7, new ItemStack(ModItems.SURVIVOR_LEGGINGS.get()), 3));
                offers.add(offer(ModItems.BONE_PLATE.get(), 4, new ItemStack(ModItems.SURVIVOR_BOOTS.get()), 3));
                offers.add(offer(ModItems.CRONENBERG_FLESH.get(), 6, new ItemStack(Items.BREAD, 4), 12));
                offers.add(offer(ModItems.CRONENBERG_FLESH.get(), 8, new ItemStack(Items.COOKED_BEEF, 4), 12));
                offers.add(offer(ModItems.BONE_PLATE.get(), 2, new ItemStack(Items.TORCH, 16), 12));
                offers.add(offer(ModItems.MUTAGEN_SAC.get(), 1, new ItemStack(Items.IRON_INGOT, 3), 12));
            }
            case BETH -> {
                offers.add(new MerchantOffer(new ItemCost(ModItems.MUTAGEN_SAC.get(), 3), Optional.of(new ItemCost(ModItems.BONE_PLATE.get(), 4)),
                        new ItemStack(ModItems.CURE_SERUM.get()), 4, 10, 0.05f));
                offers.add(offer(ModItems.CRONENBERG_FLESH.get(), 6, PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING), 8));
                offers.add(offer(ModItems.CRONENBERG_FLESH.get(), 10, PotionContents.createItemStack(Items.POTION, Potions.REGENERATION), 6));
                offers.add(offer(ModItems.MUTAGEN_SAC.get(), 2, new ItemStack(Items.GOLDEN_APPLE), 6));
                offers.add(offer(ModItems.BONE_PLATE.get(), 6, new ItemStack(ModItems.ALMOND_WATER.get()), 4));
            }
            case SUMMER -> {
                offers.add(offer(ModItems.BONE_PLATE.get(), 3, new ItemStack(Items.ARROW, 12), 12));
                offers.add(new MerchantOffer(new ItemCost(ModItems.MUTAGEN_SAC.get(), 2), Optional.of(new ItemCost(ModItems.BONE_PLATE.get(), 6)),
                        new ItemStack(Items.CROSSBOW), 3, 10, 0.05f));
                offers.add(offer(ModItems.MUTAGEN_SAC.get(), 2, new ItemStack(Items.TNT, 3), 6));
                offers.add(offer(ModItems.CRONENBERG_FLESH.get(), 20, new ItemStack(ModItems.MANTIS_SCYTHE.get()), 2));
                offers.add(offer(ModItems.BONE_PLATE.get(), 10, new ItemStack(Items.IRON_SWORD), 3));
            }
            case CRONENBERG_RICK -> {
                offers.add(offer(ModItems.MUTAGEN_SAC.get(), 4, new ItemStack(ModItems.PORTAL_FLUID.get()), 6));
                offers.add(offer(ModItems.CRONENBERG_FLESH.get(), 16, new ItemStack(ModItems.PORTAL_FLUID.get()), 6));
                offers.add(new MerchantOffer(new ItemCost(ModItems.MUTAGEN_SAC.get(), 10), Optional.of(new ItemCost(Items.IRON_BLOCK, 1)),
                        new ItemStack(ModItems.BLENDER_CHIP.get()), 1, 20, 0.0f));
                offers.add(offer(ModItems.MUTAGEN_SAC.get(), 3, new ItemStack(ModItems.CURE_SERUM.get()), 3));
                offers.add(offer(ModItems.BONE_PLATE.get(), 5, new ItemStack(ModItems.PLUMBUS.get()), 2));
            }
            case CRONENBERG_MORTY -> {
                offers.add(offer(ModItems.CRONENBERG_FLESH.get(), 6, new ItemStack(ModItems.SZECHUAN_SAUCE.get()), 4));
                offers.add(offer(ModItems.BONE_PLATE.get(), 10, new ItemStack(ModItems.RICKS_FLASK.get()), 2));
                offers.add(offer(ModItems.MUTAGEN_SAC.get(), 2, new ItemStack(ModItems.MEESEEKS_BOX.get()), 2));
            }
        }
    }

    private static MerchantOffer offer(ItemLike cost, int count, ItemStack result, int uses) {
        return new MerchantOffer(new ItemCost(cost, count), result, uses, 5, 0.05f);
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer);
        if (getTradingPlayer() instanceof ServerPlayer player) {
            ModAdvancements.award(player, kind.wanderer ? "close_rick_counters" : "survivors");
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
        return SoundEvents.ITEM_PICKUP;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean accepted) {
        return accepted ? SoundEvents.ITEM_PICKUP : SoundEvents.VILLAGER_NO;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putLong("LastRestock", lastRestock);
        tag.putBoolean("Greeted", greeted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(pos -> setHome(pos, 12));
        lastRestock = tag.getLong("LastRestock");
        greeted = tag.getBoolean("Greeted");
    }

    /** Wanderers walk up to the nearest player and stop at chatting distance. */
    private class ApproachPlayerGoal extends Goal {
        @Nullable
        private Player player;

        ApproachPlayerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            player = level().getNearestPlayer(SurvivorEntity.this, 32.0);
            return player != null && !player.isSpectator() && distanceToSqr(player) > 16.0 && !isTrading();
        }

        @Override
        public void tick() {
            if (player == null) return;
            getLookControl().setLookAt(player, 30.0f, 30.0f);
            if (tickCount % 10 == 0) getNavigation().moveTo(player, 0.55);
        }

        @Override
        public boolean canContinueToUse() {
            return player != null && player.isAlive() && distanceToSqr(player) > 9.0 && !isTrading();
        }

        @Override
        public void stop() {
            getNavigation().stop();
            player = null;
        }
    }
}
