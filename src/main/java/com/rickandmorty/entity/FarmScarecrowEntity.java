package com.rickandmorty.entity;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A scarecrow that's on your side. Stack two hay bales, put a Scarecrow Head on top, and it
 * climbs down off its post: it never hurts anyone, it keeps to its patch, and the crops around
 * it grow faster while it watches over them.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public class FarmScarecrowEntity extends PathfinderMob {
    /** How far from home it tends crops, and wanders. */
    public static final int RANGE = 8;
    private static final int RESCAN = 200, TEND = 20, TENDED = 2;

    private @Nullable BlockPos home;
    private final List<BlockPos> crops = new ArrayList<>();

    public FarmScarecrowEntity(EntityType<? extends FarmScarecrowEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6, 0.004f));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
        restrictTo(home, RANGE - 2);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home == null) setHome(blockPosition());
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount % RESCAN == 1) scan(server);
        if (tickCount % TEND == 0 && !crops.isEmpty()) {
            for (int i = 0; i < TENDED; i++) {
                BlockPos pos = crops.get(random.nextInt(crops.size()));
                BlockState state = server.getBlockState(pos);
                if (!isCrop(state) || !state.isRandomlyTicking()) continue;
                state.randomTick(server, pos, random);
                if (random.nextInt(4) == 0) {
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 2, 0.25, 0.2, 0.25, 0.0);
                }
            }
        }
    }

    /** Finds the crops around home. */
    private void scan(ServerLevel level) {
        crops.clear();
        BlockPos center = home == null ? blockPosition() : home;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RANGE, -2, -RANGE), center.offset(RANGE, 2, RANGE))) {
            if (isCrop(level.getBlockState(pos))) crops.add(pos.immutable());
        }
    }

    public static boolean isCrop(BlockState state) {
        return state.is(BlockTags.CROPS) || state.getBlock() instanceof StemBlock || state.getBlock() instanceof SweetBerryBushBlock
                || state.getBlock() instanceof NetherWartBlock || state.getBlock() instanceof CocoaBlock;
    }

    /** How many crops it's looking after (for tests). */
    public int tendedCrops() {
        return crops.size();
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
    }

    // ------------------------------------------------------------------ building one

    /** A Scarecrow Head set on two stacked hay bales comes to life. */
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getPlacedBlock().is(ModBlocks.SCARECROW_HEAD.get())) return;
        tryBuild(level, event.getPos(), event.getEntity() instanceof ServerPlayer player ? player : null);
    }

    /** Builds a farm scarecrow if the head at {@code head} sits on two hay bales. */
    public static @Nullable FarmScarecrowEntity tryBuild(ServerLevel level, BlockPos head, @Nullable ServerPlayer builder) {
        BlockState state = level.getBlockState(head);
        if (!state.is(ModBlocks.SCARECROW_HEAD.get()) || !level.getBlockState(head.below()).is(Blocks.HAY_BLOCK)
                || !level.getBlockState(head.below(2)).is(Blocks.HAY_BLOCK)) return null;
        FarmScarecrowEntity scarecrow = ModEntities.FARM_SCARECROW.get().create(level);
        if (scarecrow == null) return null;
        for (int i = 0; i <= 2; i++) {
            level.levelEvent(2001, head.below(i), net.minecraft.world.level.block.Block.getId(level.getBlockState(head.below(i))));
            level.setBlock(head.below(i), Blocks.AIR.defaultBlockState(), 3);
        }
        BlockPos feet = head.below(2);
        float yaw = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
        scarecrow.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, 0.0f);
        scarecrow.setYHeadRot(yaw);
        scarecrow.setYBodyRot(yaw);
        scarecrow.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), MobSpawnType.MOB_SUMMONED, null);
        scarecrow.setHome(feet);
        level.addFreshEntity(scarecrow);
        level.playSound(null, feet, SoundEvents.CROP_BREAK, SoundSource.NEUTRAL, 1.0f, 0.8f);
        level.playSound(null, feet, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 0.6f, 0.7f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, feet.getX() + 0.5, feet.getY() + 1.2, feet.getZ() + 0.5, 12, 0.4, 0.6, 0.4, 0.0);
        if (builder != null) ModAdvancements.award(builder, "farm_scarecrow");
        return scarecrow;
    }
}
