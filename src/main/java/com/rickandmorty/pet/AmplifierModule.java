package com.rickandmorty.pet;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The upgrades a Cognition Amplifier takes, one of each, kept as bits. */
public enum AmplifierModule {
    /** Double damage, armour, faster strikes. */
    COMBAT,
    /** Hunts monsters near it (guards the spot when sitting) and warns its owner. */
    GUARDIAN,
    /** Heals itself fast, and its owner when they're badly hurt. */
    MEDIC,
    /** Collects dropped items and brings them to its owner. */
    FETCH;

    public static final int ALL = (1 << values().length) - 1;

    public int bit() {
        return 1 << ordinal();
    }

    public boolean in(int modules) {
        return (modules & bit()) != 0;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("module.rickrooms." + id());
    }

    /** "Combat, Medic", or null for none. */
    public static Component list(int modules) {
        List<Component> names = new ArrayList<>();
        for (AmplifierModule module : values()) {
            if (module.in(modules)) names.add(module.displayName());
        }
        if (names.isEmpty()) return null;
        var joined = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) joined.append(", ");
            joined.append(names.get(i));
        }
        return joined;
    }
}
