package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.MimicKind;
import com.rickandmorty.registry.ModEntities;
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

/**
 * What finds you in the levels past Level 0 (which has BackroomsSpawner): the Bacteria in the
 * garage and the offices, Still Lifes among the desks, more scarecrows in the Harvest, and far
 * (The Watcher has its own spawner, for every world.) Nothing ever comes to the Sunlit Lobby.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class LevelSpawner {
    private static final int INTERVAL = 200;

    private LevelSpawner() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 0) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        BackroomsLevel here = BackroomsLevel.of(level.dimension());
        if (here == null || here == BackroomsLevel.LEVEL_0 || !(level.getChunkSource().getGenerator() instanceof LevelChunkGenerator generator)) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;
            RandomSource random = level.random;
            AABB near = player.getBoundingBox().inflate(64);
            switch (here) {
                case LEVEL_1 -> {
                    if (random.nextFloat() < 0.15f && level.getEntitiesOfClass(com.rickandmorty.entity.BacteriaEntity.class, near).size() < 2) {
                        spawn(level, generator.generator(), player, ModEntities.BACTERIA.get(), 20, 36);
                    }
                }
                case LEVEL_4 -> {
                    if (random.nextFloat() < 0.1f && level.getEntitiesOfClass(com.rickandmorty.entity.BacteriaEntity.class, near).size() < 2) {
                        spawn(level, generator.generator(), player, ModEntities.BACTERIA.get(), 20, 36);
                    }
                    stillLife(level, generator.generator(), player, near, 0.08f);
                    if (random.nextFloat() < 0.2f && level.getEntitiesOfClass(com.rickandmorty.entity.MimicEntity.class, near).size() < 5) {
                        MimicKind kind = random.nextBoolean() ? MimicKind.VILLAGER : MimicKind.ZOMBIE;
                        spawn(level, generator.generator(), player, ModEntities.MIMICS.get(kind).get(), 14, 30);
                    }
                }
                case DEAD_MALL, WOODEN_HOTEL -> stillLife(level, generator.generator(), player, near, here == BackroomsLevel.DEAD_MALL ? 0.2f : 0.12f);
                case FACADE -> stillLife(level, generator.generator(), player, near, 0.08f);
                case LEVEL_FUN -> {
                    if (random.nextFloat() < 0.25f && level.getEntitiesOfClass(com.rickandmorty.entity.PartygoerEntity.class, near).size() < 5) {
                        spawn(level, generator.generator(), player, ModEntities.PARTYGOER.get(), 16, 34);
                    }
                }
                case INFINITE_IKEA -> {
                    // a few on their rounds by day; after closing, a lot more
                    boolean closed = StoreHours.closed(level);
                    int crowd = level.getEntitiesOfClass(com.rickandmorty.entity.StaffEntity.class, near).size();
                    if (random.nextFloat() < (closed ? 0.6f : 0.15f) && crowd < (closed ? 10 : 4)) {
                        spawn(level, generator.generator(), player, ModEntities.STAFF.get(), closed ? 18 : 24, 44);
                    }
                }
                case HARVEST -> {
                    if (random.nextFloat() < 0.15f && level.getEntitiesOfClass(com.rickandmorty.entity.ScarecrowEntity.class, near).size() < 5) {
                        spawn(level, generator.generator(), player, ModEntities.SCARECROW.get(), 24, 40);
                    }
                }
                default -> {
                }
            }
        }
    }

    /** A Still Life of a person, sometimes. */
    private static void stillLife(ServerLevel level, LevelGenerator generator, ServerPlayer player, AABB near, float chance) {
        if (level.random.nextFloat() >= chance || level.getEntitiesOfClass(com.rickandmorty.entity.StillLifeEntity.class, near).size() >= 3) return;
        var forms = com.rickandmorty.entity.StillLifeForm.values();
        spawn(level, generator, player, ModEntities.STILL_LIVES.get(forms[level.random.nextInt(forms.length)]).get(), 16, 34);
    }

    /** Somewhere to stand, out of the way, some distance from the player. */
    private static <T extends Mob> void spawn(ServerLevel level, LevelGenerator generator, ServerPlayer player, EntityType<T> type, int min, int max) {
        RandomSource random = level.random;
        long seed = level.getSeed();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, distance = min + random.nextDouble() * (max - min);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance), z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            Integer y = generator.standY(seed, x, z, (int) player.getY());
            if (y == null || Math.abs(y - player.getY()) > 24) continue;
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.isLoaded(feet) || !level.noCollision(type.getSpawnAABB(x + 0.5, y, z + 0.5))) continue;
            T mob = type.create(level);
            if (mob == null) return;
            mob.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0f, 0.0f);
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(feet), MobSpawnType.EVENT, null);
            level.addFreshEntity(mob);
            return;
        }
    }
}
