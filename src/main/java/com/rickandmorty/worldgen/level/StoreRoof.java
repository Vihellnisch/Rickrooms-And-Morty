package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Parts of the Infinite IKEA generated before it had a roof get one put on as they load, a few
 * chunks a tick, so shader packs never show the sky through them.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class StoreRoof {
    private static final Set<Long> WAITING = new LinkedHashSet<>();

    private StoreRoof() {
    }

    private static boolean isStore(Object level) {
        return level instanceof ServerLevel server && server.dimension().equals(BackroomsLevel.INFINITE_IKEA.key());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (isStore(event.getLevel()) && event.getChunk() instanceof LevelChunk chunk) WAITING.add(chunk.getPos().toLong());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!isStore(event.getLevel()) || WAITING.isEmpty()) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        Iterator<Long> it = WAITING.iterator();
        for (int done = 0; done < 4 && it.hasNext(); done++) {
            ChunkPos pos = new ChunkPos(it.next());
            it.remove();
            if (level.hasChunk(pos.x, pos.z)) roof(level, pos);
        }
    }

    /** Puts the roof on a chunk that hasn't got one. */
    public static void roof(ServerLevel level, ChunkPos pos) {
        int top = level.getMaxBuildHeight() - 1;
        if (!level.getBlockState(new BlockPos(pos.getMinBlockX(), top, pos.getMinBlockZ())).isAir()) return;
        BlockState ceiling = ModBlocks.STORE_CEILING.get().defaultBlockState(), light = ModBlocks.STORE_CEILING_LIGHT.get().defaultBlockState();
        for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) {
            for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++) {
                BlockPos at = new BlockPos(x, top, z);
                if (!level.getBlockState(at).isAir()) continue;
                level.setBlock(at, Math.floorMod(x, 12) == 6 && Math.floorMod(z, 12) == 6 ? light : ceiling,
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
}
