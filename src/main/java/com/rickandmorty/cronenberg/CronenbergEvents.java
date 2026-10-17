package com.rickandmorty.cronenberg;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.PortalEntity;
import com.rickandmorty.entity.SurvivorEntity;
import com.rickandmorty.entity.SurvivorKind;
import com.rickandmorty.item.PortalDestinations;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModSounds;
import com.rickandmorty.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Things that happen around players in Cronenberg World: screams carried on the wind at night,
 * and every so often Cronenberg Rick and Morty stepping out of a portal nearby.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class CronenbergEvents {
    private static final int WANDERER_INTERVAL = 1200;
    private static final float WANDERER_CHANCE = 0.04f;
    private static final int AMBIENCE_INTERVAL = 400;

    private CronenbergEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ModDimensions.CRONENBERG) return;
        long time = level.getGameTime();
        RandomSource random = level.random;
        if (time % AMBIENCE_INTERVAL == 0 && level.isNight()) {
            for (ServerPlayer player : level.players()) {
                if (random.nextFloat() > 0.35f) continue;
                double angle = random.nextDouble() * Math.PI * 2;
                level.playSound(null, player.getX() + Math.cos(angle) * 28, player.getY() + 4, player.getZ() + Math.sin(angle) * 28,
                        ModSounds.CRONENBERG_DISTANT.get(), SoundSource.AMBIENT, 6.0f, 0.6f + random.nextFloat() * 0.3f);
            }
        }
        if (time % WANDERER_INTERVAL != 0) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || random.nextFloat() > WANDERER_CHANCE) continue;
            boolean around = !level.getEntitiesOfClass(SurvivorEntity.class, new AABB(player.blockPosition()).inflate(256),
                    s -> s.kind().wanderer).isEmpty();
            if (!around) spawnWanderers(level, player);
        }
    }

    /** Cronenberg Rick and Morty come through a portal somewhere nearby. */
    @Nullable
    public static SurvivorEntity spawnWanderers(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 18 + random.nextDouble() * 10;
            int x = (int) (player.getX() + Math.cos(angle) * distance), z = (int) (player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.getFluidState(feet.below()).isEmpty() || !level.getFluidState(feet).isEmpty()) continue;
            SurvivorEntity rick = spawn(level, SurvivorKind.CRONENBERG_RICK, x + 0.5, y, z + 0.5);
            spawn(level, SurvivorKind.CRONENBERG_MORTY, x + 1.5, y, z + 0.5);
            float yaw = (float) Math.toDegrees(Math.atan2(player.getZ() - z, player.getX() - x)) - 90.0f;
            PortalEntity.spawnExitPortal(level, new PortalDestinations.Destination("", level.dimension(), x + 0.5, y, z + 0.5, yaw), false);
            return rick;
        }
        return null;
    }

    private static SurvivorEntity spawn(ServerLevel level, SurvivorKind kind, double x, double y, double z) {
        SurvivorEntity npc = ModEntities.SURVIVORS.get(kind).get().create(level);
        if (npc == null) return null;
        npc.moveTo(x, y, z, level.random.nextFloat() * 360.0f, 0.0f);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.EVENT, null);
        level.addFreshEntity(npc);
        return npc;
    }
}
