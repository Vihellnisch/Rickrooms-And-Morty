package com.rickandmorty.entity;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.worldgen.BackroomsLayout;
import com.rickandmorty.worldgen.BackroomsTheme;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Populates the Backrooms around players, on whichever floor they're on:
 * <ul>
 *   <li>now and then a Bacteria appears out of sight (never more than a couple nearby);</li>
 *   <li>inside biome patches, "Still Life" copies of the mobs that biome would have.</li>
 * </ul>
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public class BackroomsSpawner {
    private static final int CHECK_INTERVAL = 200;          // Bacteria: every 10 seconds
    private static final int MIMIC_INTERVAL = 100;          // Still Lifes: every 5 seconds
    private static final float BACTERIA_CHANCE = 0.15f;     // per player per check
    private static final int MAX_BACTERIA = 2;
    private static final float MIMIC_CHANCE_IN_PATCH = 0.65f;
    private static final float MIMIC_CHANCE_ELSEWHERE = 0.15f;
    private static final int MAX_MIMICS = 7;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ModDimensions.BACKROOMS) return;
        if (level.getGameTime() % MIMIC_INTERVAL != 0 || level.getDifficulty() == Difficulty.PEACEFUL) return;
        boolean bacteriaCheck = level.getGameTime() % CHECK_INTERVAL == 0;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;
            AABB nearby = player.getBoundingBox().inflate(64);
            RandomSource random = level.random;

            if (bacteriaCheck && random.nextFloat() < BACTERIA_CHANCE && level.getEntitiesOfClass(BacteriaEntity.class, nearby).size() < MAX_BACTERIA) {
                trySpawnNear(level, player, ModEntities.BACTERIA.get(), 20, 36);
            }

            var hit = BackroomsLayout.themeAt(level.getSeed(), player.getBlockX(), player.getBlockZ());
            List<MimicKind> kinds = hit == null ? List.of(MimicKind.VILLAGER, MimicKind.ZOMBIE) : mimicsFor(hit.theme());
            float chance = hit == null ? MIMIC_CHANCE_ELSEWHERE : MIMIC_CHANCE_IN_PATCH;
            if (!kinds.isEmpty() && random.nextFloat() < chance
                    && level.getEntitiesOfClass(MimicEntity.class, nearby).size() < MAX_MIMICS) {
                MimicKind kind = kinds.get(random.nextInt(kinds.size()));
                trySpawnNear(level, player, ModEntities.MIMICS.get(kind).get(), 14, 30);
            }
            // now and then, someone who looks like someone you know
            if (random.nextFloat() < 0.05f && level.getEntitiesOfClass(StillLifeEntity.class, nearby).size() < 2) {
                trySpawnNear(level, player, ModEntities.STILL_LIVES.get(StillLifeForm.values()[random.nextInt(StillLifeForm.values().length)]).get(), 16, 32);
            }
        }
    }

    /** What each imitation biome tries (and fails) to copy. */
    public static List<MimicKind> mimicsFor(BackroomsTheme theme) {
        return switch (theme) {
            case PLAINS -> List.of(MimicKind.SHEEP, MimicKind.COW, MimicKind.PIG, MimicKind.CHICKEN, MimicKind.VILLAGER);
            case CHERRY -> List.of(MimicKind.SHEEP, MimicKind.PIG, MimicKind.VILLAGER);
            case JUNGLE -> List.of(MimicKind.CHICKEN, MimicKind.PIG);
            case DESERT, SNOWY -> List.of(MimicKind.VILLAGER, MimicKind.ZOMBIE);
            case MUSHROOM -> List.of(MimicKind.COW);
            case CAVE, DEEP_DARK, NETHER -> List.of(MimicKind.ZOMBIE);
            case END -> List.of(MimicKind.VILLAGER);
            case OCEAN -> List.of();
        };
    }

    /** Spawns in the middle of a cell (open floor) on the player's floor, some distance away. */
    public static <T extends Mob> @Nullable T trySpawnNear(ServerLevel level, ServerPlayer player, EntityType<T> type,
                                                           int minDistance, int maxDistance) {
        RandomSource random = level.random;
        long seed = level.getSeed();
        int floor = BackroomsLayout.levelAt(player.getY());
        int y = BackroomsLayout.standY(floor);
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
            int cx = Math.floorDiv((int) (player.getX() + Math.cos(angle) * distance), BackroomsLayout.CELL);
            int cz = Math.floorDiv((int) (player.getZ() + Math.sin(angle) * distance), BackroomsLayout.CELL);
            if (!BackroomsLayout.isOpenCell(seed, floor, cx, cz)) continue;
            double x = BackroomsLayout.cellCenter(cx), z = BackroomsLayout.cellCenter(cz);
            BlockPos feet = BlockPos.containing(x, y, z);
            if (!level.isLoaded(feet) || !level.noCollision(type.getSpawnAABB(x, y, z))) continue;

            T mob = type.create(level);
            if (mob == null) return null;
            mob.moveTo(x, y, z, random.nextFloat() * 360.0f, 0.0f);
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(feet), MobSpawnType.EVENT, null);
            level.addFreshEntity(mob);
            return mob;
        }
        return null;
    }
}
