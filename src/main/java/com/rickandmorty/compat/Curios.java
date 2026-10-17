package com.rickandmorty.compat;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

/**
 * Curios support, if it's installed: the Interdimensional Goggles also go in its head slot, so they
 * can be worn over a helmet. This class never touches Curios itself; {@link CuriosBridge} (and the
 * client's {@code CuriosClientBridge}) do, and are only loaded when Curios is there.
 */
public final class Curios {
    private static Boolean loaded;

    private Curios() {
    }

    public static boolean loaded() {
        if (loaded == null) loaded = ModList.get() != null && ModList.get().isLoaded("curios");
        return loaded;
    }

    /** Wearing the goggles in a Curios slot? (Always false without Curios.) */
    public static boolean wearingGoggles(LivingEntity entity) {
        return loaded() && CuriosBridge.wearingGoggles(entity);
    }

    /** Tells Curios what the goggles do in its slot (common setup). */
    public static void setup() {
        if (loaded()) CuriosBridge.register();
    }
}
