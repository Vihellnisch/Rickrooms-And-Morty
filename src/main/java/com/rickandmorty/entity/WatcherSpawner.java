package com.rickandmorty.entity;

import com.rickandmorty.ModConfig;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import com.rickandmorty.worldgen.level.LevelChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Now and then, in any world, the Watcher turns up: far off, somewhere it can see you from,
 * and never somewhere you're already looking. Most often in the wooden levels of the Backrooms.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class WatcherSpawner {
    private static final int INTERVAL = 400;
    private static final double VIEW = Math.cos(Math.toRadians(70));

    private WatcherSpawner() {
    }

    /** Chance per check (every 20 seconds, per player). */
    public static double chance(ServerLevel level) {
        BackroomsLevel here = BackroomsLevel.of(level.dimension());
        double base = here == null ? 0.025
                : switch (here) {
                    case WOODEN_HOTEL, SCAFFOLD, CAVERN -> 0.08;
                    case SUNLIT_LOBBY -> 0.0;
                    default -> 0.03;
                };
        return base * ModConfig.WATCHER_FREQUENCY.get();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 0 || !ModConfig.WATCHER_ENABLED.get()) return;
        double chance = chance(level);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            BlockPos monolith = level.dimension() == net.minecraft.world.level.Level.OVERWORLD ? monolithTop(level, player.blockPosition()) : null;
            if (level.random.nextDouble() >= (monolith != null ? chance * 4 : chance)) continue;
            if (monolith == null && safeAtHome(level, player)) continue;
            if (!level.getEntitiesOfClass(WatcherEntity.class, player.getBoundingBox().inflate(160)).isEmpty()) continue;
            if (monolith != null && level.random.nextBoolean()) spawnAt(level, player, monolith);
            else spawnNear(level, player);
        }
    }

    /** It never comes near your bed, or into a village. */
    public static boolean safeAtHome(ServerLevel level, ServerPlayer player) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
        if (level.isVillage(player.blockPosition())) return true;
        BlockPos bed = player.getRespawnPosition();
        return bed != null && player.getRespawnDimension() == level.dimension() && bed.distSqr(player.blockPosition()) < 48 * 48;
    }

    /** The top of a Monolith near the player (within about 40 blocks), or null. */
    public static @Nullable BlockPos monolithTop(ServerLevel level, BlockPos near) {
        var structures = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var monolith = structures.get(RickAndMortyMod.id("monolith"));
        if (monolith == null) return null;
        for (int[] d : new int[][]{{0, 0}, {40, 0}, {-40, 0}, {0, 40}, {0, -40}}) {
            var start = level.structureManager().getStructureWithPieceAt(near.offset(d[0], 0, d[1]), monolith);
            if (!start.isValid()) continue;
            BlockPos center = start.getBoundingBox().getCenter();
            BlockPos best = null;
            for (int x = center.getX() - 6; x <= center.getX() + 14; x++) {
                for (int z = center.getZ() - 6; z <= center.getZ() + 6; z++) {
                    if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                    if (level.getBlockState(new BlockPos(x, y - 1, z)).is(com.rickandmorty.registry.ModBlocks.MONOLITH.get())
                            && (best == null || y > best.getY())) best = new BlockPos(x, y, z);
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** Stands a Watcher right on top of the Monolith, looking down. */
    public static @Nullable WatcherEntity spawnAt(ServerLevel level, ServerPlayer player, BlockPos feet) {
        WatcherEntity watcher = ModEntities.WATCHER.get().create(level);
        if (watcher == null) return null;
        float yaw = (float) Math.toDegrees(Math.atan2(-(player.getX() - feet.getX() - 0.5), player.getZ() - feet.getZ() - 0.5));
        watcher.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, 0.0f);
        watcher.setYHeadRot(yaw);
        watcher.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), MobSpawnType.EVENT, null);
        level.addFreshEntity(watcher);
        return watcher;
    }

    /** Puts a Watcher 32 to 64 blocks from the player, out of their sight but able to see them. */
    public static @Nullable WatcherEntity spawnNear(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, distance = 32 + random.nextDouble() * 32;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance), z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            Integer y = standY(level, x, z, player.getBlockY());
            if (y == null) continue;
            Vec3 feet = new Vec3(x + 0.5, y, z + 0.5), eye = feet.add(0, 1.6, 0);
            Vec3 toIt = eye.subtract(player.getEyePosition()).normalize();
            if (player.getViewVector(1.0f).dot(toIt) > VIEW) continue;           // it would pop in right in front of them
            if (level.clip(new ClipContext(eye, player.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() != HitResult.Type.MISS) continue;                    // it has to be able to see them
            WatcherEntity watcher = ModEntities.WATCHER.get().create(level);
            if (watcher == null) return null;
            if (!level.noCollision(watcher, watcher.getDimensions(watcher.getPose()).makeBoundingBox(feet))) continue;
            float yaw = (float) Math.toDegrees(Math.atan2(-(player.getX() - feet.x), player.getZ() - feet.z));
            watcher.moveTo(feet.x, feet.y, feet.z, yaw, 0.0f);
            watcher.setYHeadRot(yaw);
            watcher.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(feet)), MobSpawnType.EVENT, null);
            level.addFreshEntity(watcher);
            return watcher;
        }
        return null;
    }

    private static @Nullable Integer standY(ServerLevel level, int x, int z, int nearY) {
        if (!level.isLoaded(new BlockPos(x, nearY, z))) return null;
        if (level.getChunkSource().getGenerator() instanceof LevelChunkGenerator generator) {
            return generator.generator().standY(level.getSeed(), x, z, nearY);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 24; dy++) {
            for (int sign : new int[]{1, -1}) {
                int y = nearY + dy * sign;
                if (y <= level.getMinBuildHeight() || y + 2 >= level.getMaxBuildHeight()) continue;
                pos.set(x, y - 1, z);
                if (!level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) continue;
                pos.set(x, y, z);
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) continue;
                pos.set(x, y + 1, z);
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) continue;
                return y;
            }
        }
        return null;
    }
}
