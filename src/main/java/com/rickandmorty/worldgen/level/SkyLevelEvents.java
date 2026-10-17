package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.BacteriaEntity;
import com.rickandmorty.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * What the v0.23 levels do to you. Froopyland: you can breathe in its rainbow river (falling on
 * its turf can't hurt you, that's the turf itself). Run For Your Life: they come out of the hall
 * behind you, the way you came, and keep coming, fewer the deeper you get into the maze but
 * faster; and now and then one of the locked doors ahead bangs open, throws something into the
 * hall in front of you, and slams shut again.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class SkyLevelEvents {
    /** How many cells back down the hall they come out. */
    static final int CELLS_BEHIND = 3;
    /** Doors that burst open, waiting to slam shut again: the lower half, and the game time it shuts. */
    private static final List<Object[]> SLAMMING = new ArrayList<>();
    private static final BlockState[] THROWN = {
            Blocks.BARREL.defaultBlockState(), Blocks.QUARTZ_STAIRS.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState(),
            Blocks.CAULDRON.defaultBlockState(), Blocks.SMOOTH_QUARTZ.defaultBlockState()};

    private SkyLevelEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator() || player.tickCount % 20 != 0) return;
        BackroomsLevel here = BackroomsLevel.of(player.level().dimension());
        if (here == BackroomsLevel.FROOPYLAND && player.isInWater()) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
        }
        if (here == BackroomsLevel.RUN_FOR_YOUR_LIFE && !player.isCreative()) {
            int depth = depth(player);
            if (depth < 0) return;
            // keep the horde topped up behind you: four near the start, one deep in
            int want = Math.max(1, 4 - depth / 10);
            int have = player.level().getEntitiesOfClass(BacteriaEntity.class, player.getBoundingBox().inflate(40, 6, 40)).size();
            if (have < want && depth >= 2 && player.getRandom().nextFloat() < 0.4f) chaser(player, have);
            if (player.getRandom().nextFloat() < 0.1f) burstDoor(player);
        }
    }

    /** How many cells into their maze the player is (-1: not in the hall). */
    static int depth(ServerPlayer player) {
        int[] at = RunLevel.locate(player.getBlockX(), player.getBlockZ());
        RunLevel.Maze maze = RunLevel.maze(player.serverLevel().getSeed(), at[0], at[1]);
        return maze.hall(at[2], at[3]) ? maze.depth()[at[2] * RunLevel.CELLS + at[3]] : -1;
    }

    /** One of them, a few cells back toward the start of the player's maze, already hunting them. */
    static void chaser(ServerPlayer player, int index) {
        ServerLevel level = player.serverLevel();
        int[] at = RunLevel.locate(player.getBlockX(), player.getBlockZ());
        RunLevel.Maze maze = RunLevel.maze(level.getSeed(), at[0], at[1]);
        int cell = at[2] * RunLevel.CELLS + at[3];
        for (int n = 0; n < CELLS_BEHIND && maze.parent()[cell] >= 0; n++) cell = maze.parent()[cell];
        if (cell == at[2] * RunLevel.CELLS + at[3]) return;
        int[] m = RunLevel.middle(at[0], at[1], cell / RunLevel.CELLS, cell % RunLevel.CELLS);
        BacteriaEntity chaser = ModEntities.BACTERIA.get().create(level);
        if (chaser == null) return;
        chaser.moveTo(m[0] + 0.5 + (index % 3 - 1), RunLevel.FLOOR + 1, m[1] + 0.5, 0.0f, 0.0f);
        chaser.finalizeSpawn(level, level.getCurrentDifficultyAt(chaser.blockPosition()), MobSpawnType.EVENT, null);
        // faster the deeper you are
        int speed = maze.depth()[at[2] * RunLevel.CELLS + at[3]] > 20 ? 2 : 1;
        chaser.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60 * 10, speed, false, false));
        chaser.setTarget(player);
        level.addFreshEntity(chaser);
    }

    /**
     * A locked door a few blocks ahead of the player bangs open with a thud, something comes flying
     * out of it into the hall, and two seconds later it slams shut.
     */
    public static boolean burstDoor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int ahead = 4; ahead <= 10; ahead++) {
            for (int side = -3; side <= 3; side++) {
                double x = player.getX() + look.x * ahead - look.z * side, z = player.getZ() + look.z * ahead + look.x * side;
                pos.set(Math.floor(x), RunLevel.FLOOR + 1, Math.floor(z));
                BlockState state = level.getBlockState(pos);
                if (!state.is(Blocks.IRON_DOOR) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER || state.getValue(DoorBlock.OPEN)) continue;
                BlockPos door = pos.immutable();
                Direction out = state.getValue(DoorBlock.FACING);
                BlockPos front = door.relative(out);
                if (!level.getBlockState(front).isAir()) continue;
                ((DoorBlock) Blocks.IRON_DOOR).setOpen(null, level, state, door, true);
                level.playSound(null, door, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0f, 0.6f);
                level.playSound(null, door, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.7f, 0.5f);
                BlockState thrown = THROWN[level.random.nextInt(THROWN.length)];
                if (thrown.hasProperty(StairBlock.FACING)) thrown = thrown.setValue(StairBlock.FACING, out.getClockWise());
                FallingBlockEntity flying = FallingBlockEntity.fall(level, front, thrown);
                flying.setDeltaMovement(out.getStepX() * 0.45, 0.25, out.getStepZ() * 0.45);
                flying.setHurtsEntities(1.5f, 20);
                SLAMMING.add(new Object[]{level, door, level.getGameTime() + 40});
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (SLAMMING.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) return;
        SLAMMING.removeIf(entry -> {
            if (entry[0] != level || level.getGameTime() < (long) entry[2]) return false;
            BlockPos door = (BlockPos) entry[1];
            BlockState state = level.getBlockState(door);
            if (state.is(Blocks.IRON_DOOR) && state.getValue(DoorBlock.OPEN)) {
                ((DoorBlock) Blocks.IRON_DOOR).setOpen(null, level, state, door, false);
                level.playSound(null, door, SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 1.5f, 0.7f);
            }
            return true;
        });
    }
}
