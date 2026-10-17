package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModAdvancements;
import com.rickandmorty.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Opening hours of the Infinite IKEA, which keeps the Overworld's days. At dusk a chime and an
 * announcement: the store is now closed (and the staff stop being friendly). At dawn it opens
 * again, and anyone who was there all night has earned it.
 */
@EventBusSubscriber(modid = RickAndMortyMod.MOD_ID)
public final class StoreHours {
    private static @Nullable Boolean forced;
    private static @Nullable Boolean wasClosed;
    /** Who was in the store when it closed. */
    private static final Set<UUID> STAYED = new HashSet<>();

    private StoreHours() {
    }

    /** Night: the store is closed. */
    public static boolean closed(Level level) {
        if (forced != null) return forced;
        return !level.isDay();
    }

    /** For tests: pretend it's always night (true), always day (false), or go by the sky again (null). */
    public static void force(@Nullable Boolean closed) {
        forced = closed;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
        if (!level.dimension().equals(BackroomsLevel.INFINITE_IKEA.key())) return;
        boolean closed = closed(level);
        Boolean before = wasClosed;
        wasClosed = closed;
        if (before == null || before == closed) return;
        announce(level, closed);
    }

    private static void announce(ServerLevel level, boolean closed) {
        if (closed) STAYED.clear();
        for (ServerPlayer player : level.players()) {
            // ding, dong, ding
            for (float pitch : new float[]{1.0f, 0.8f, 0.6f}) {
                player.playNotifySound(ModSounds.STORE_CHIME.get(), SoundSource.AMBIENT, 1.0f, closed ? pitch : 1.6f - pitch);
            }
            player.displayClientMessage(Component.translatable(closed ? "message.rickrooms.store.closed" : "message.rickrooms.store.open")
                    .withStyle(closed ? ChatFormatting.RED : ChatFormatting.GREEN), false);
            if (closed) {
                STAYED.add(player.getUUID());
            } else if (STAYED.contains(player.getUUID()) && !player.isCreative()) {
                ModAdvancements.award(player, "store_closed");
            }
        }
        if (!closed) STAYED.clear();
    }
}
