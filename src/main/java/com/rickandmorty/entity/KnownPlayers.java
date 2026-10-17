package com.rickandmorty.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Everyone who has ever played on this world: whoever is online, plus every player with a save
 * file in the world's playerdata folder. The Council of Yous is made of alternate versions of them.
 */
public final class KnownPlayers {
    private KnownPlayers() {
    }

    public static List<UUID> all(MinecraftServer server) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) ids.add(player.getUUID());
        Path folder = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if (Files.isDirectory(folder)) {
            try (Stream<Path> files = Files.list(folder)) {
                files.map(p -> p.getFileName().toString())
                        .filter(name -> name.endsWith(".dat"))
                        .map(name -> name.substring(0, name.length() - 4))
                        .forEach(name -> {
                            try {
                                ids.add(UUID.fromString(name));
                            } catch (IllegalArgumentException ignored) {
                                // not a player file
                            }
                        });
            } catch (IOException ignored) {
                // no saved players yet: online players only
            }
        }
        return new ArrayList<>(ids);
    }

    /**
     * A random known player's profile, with their skin. Online players already carry it; anyone
     * else is looked up (asynchronously) the same way player heads are. Falls back to a bare
     * profile (default skin) if the lookup fails, e.g. offline.
     */
    public static CompletableFuture<Optional<GameProfile>> pickRandom(MinecraftServer server, RandomSource random) {
        List<UUID> ids = all(server);
        if (ids.isEmpty()) return CompletableFuture.completedFuture(Optional.empty());
        return lookup(server, ids.get(random.nextInt(ids.size())));
    }

    /** A particular player's profile, with their skin (looked up if they're not online with it). */
    public static CompletableFuture<Optional<GameProfile>> lookup(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null && online.getGameProfile().getProperties().containsKey("textures")) {
            return CompletableFuture.completedFuture(Optional.of(online.getGameProfile()));
        }
        String cachedName = server.getProfileCache() == null ? null
                : server.getProfileCache().get(id).map(GameProfile::getName).orElse(null);
        GameProfile fallback = online != null ? online.getGameProfile()
                : new GameProfile(id, cachedName != null ? cachedName : "Traveler");
        return SkullBlockEntity.fetchGameProfile(id)
                .thenApply(found -> Optional.of(found.orElse(fallback)))
                .exceptionally(error -> Optional.of(fallback));
    }
}
