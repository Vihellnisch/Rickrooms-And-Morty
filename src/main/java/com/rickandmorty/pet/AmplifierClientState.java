package com.rickandmorty.pet;

import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the client knows about amplified pets, by entity id: which wear a helmet (and its modules),
 * and what each last said. Plain Java on purpose, so the network handlers can set it without loading
 * client classes on a dedicated server.
 */
public final class AmplifierClientState {
    private static final Map<Integer, Integer> MODULES = new ConcurrentHashMap<>();
    private static final Map<Integer, Said> SAID = new ConcurrentHashMap<>();

    public record Said(Component line, long until) {
    }

    private AmplifierClientState() {
    }

    /** modules, or -1 for no helmet. */
    public static void set(int entityId, int modules) {
        if (modules < 0) MODULES.remove(entityId);
        else MODULES.put(entityId, modules);
    }

    /** The pet's modules, or -1 if it isn't wearing a helmet. */
    public static int get(int entityId) {
        return MODULES.getOrDefault(entityId, -1);
    }

    /** Show a speech bubble over the pet for a while: longer lines stay up longer. */
    public static void said(int entityId, Component line) {
        long millis = Math.min(9000, 2500 + 55L * line.getString().length());
        SAID.put(entityId, new Said(line, System.currentTimeMillis() + millis));
    }

    /** What the pet is saying right now, or null. */
    public static Component saying(int entityId) {
        Said said = SAID.get(entityId);
        if (said == null) return null;
        if (System.currentTimeMillis() > said.until()) {
            SAID.remove(entityId);
            return null;
        }
        return said.line();
    }

    public static void clear() {
        MODULES.clear();
        SAID.clear();
    }
}
