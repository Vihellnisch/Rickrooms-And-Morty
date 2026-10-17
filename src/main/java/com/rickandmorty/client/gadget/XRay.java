package com.rickandmorty.client.gadget;

import com.rickandmorty.gadget.OreSight;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Interdimensional Goggles' x-ray: while they're on, the world is drawn with nothing in it but
 * ores (every block tagged as an ore, so other mods' ores too). Entities still show. The chunk
 * mixins (vanilla's, and Sodium's when it's installed) ask this what to leave out.
 */
public final class XRay {
    private static volatile boolean active;

    private XRay() {
    }

    public static boolean active() {
        return active;
    }

    static void setActive(boolean on) {
        active = on;
    }

    /** Drawn while the x-ray's on? */
    public static boolean shows(BlockState state) {
        return OreSight.color(state) >= 0;
    }

    /** Should this block be left out of the world right now? */
    public static boolean hides(BlockState state) {
        return active && !shows(state);
    }
}
