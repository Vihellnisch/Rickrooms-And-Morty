package com.rickandmorty.entity.cruiser;

import net.minecraft.util.StringRepresentable;

/**
 * What can be fitted to Rick's Space Cruiser, one of each. Weapons are cycled with the weapon key
 * and fired with the attack button; the repulsor and the portal drive have keys of their own;
 * auto park just works.
 */
public enum CruiserUpgrade implements StringRepresentable {
    /** A green beam that takes whatever it hits apart. */
    DISINTEGRATOR(true, false, 180, 24),
    /** Streams of little red laser bolts. Hold to fire. */
    MINIGUN(true, true, 6, 3),
    /** Two jets of fire out of the front. Hold to fire. */
    FLAMETHROWER(true, true, 3, 1),
    /** A pair of rockets, one from each side. */
    ROCKETS(true, false, 250, 30),
    /** Cuts a tunnel through whatever you're looking at. Hold to fire. */
    LASER_DRILL(true, true, 20, 4),
    /** A shockwave that throws everything around the cruiser away from it. */
    REPULSOR(false, false, 400, 80),
    /** It parks itself when you get out, comes when you call it with the remote, and keeps its passengers safe. */
    AUTO_PARK(false, false, 0, 0),
    /** Opens a portal ahead and flies the whole cruiser through it, to wherever your Portal Gun is set. */
    PORTAL_DRIVE(false, false, 600, 100);

    public final boolean weapon;
    /** Keeps firing while the button is held. */
    public final boolean continuous;
    /** Power used per shot (per tick of firing for continuous ones), and ticks between shots. */
    public final int cost, cooldown;

    CruiserUpgrade(boolean weapon, boolean continuous, int cost, int cooldown) {
        this.weapon = weapon;
        this.continuous = continuous;
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public String itemId() {
        return name().toLowerCase() + "_upgrade";
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
