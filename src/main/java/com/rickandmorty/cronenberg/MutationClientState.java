package com.rickandmorty.cronenberg;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players' mutation as last sent by the server, by entity id. Plain Java on purpose, so the
 * network handler can set it without loading client classes on a dedicated server.
 */
public final class MutationClientState {
    private static final Map<Integer, Float> VALUES = new ConcurrentHashMap<>();

    private MutationClientState() {
    }

    public static void set(int entityId, float value) {
        if (value <= 0.0f) VALUES.remove(entityId);
        else VALUES.put(entityId, value);
    }

    public static float get(int entityId) {
        return VALUES.getOrDefault(entityId, 0.0f);
    }

    public static void clear() {
        VALUES.clear();
    }
}
