package com.rickandmorty.worldgen.level;

import net.minecraft.util.Mth;

/** Deterministic hashing and smooth noise for the column-by-column level generators. */
public final class GenMath {
    private GenMath() {
    }

    public static long hash(long seed, int a, int b, int salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    public static long hash(long seed, int a, int b, int c, int salt) {
        return hash(seed ^ (c * 0xD6E8FEB86659FD93L), a, b, salt);
    }

    /** 0..1 from a hash. */
    public static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    public static double unit(long seed, int a, int b, int salt) {
        return unit(hash(seed, a, b, salt));
    }

    public static int pick(long seed, int a, int b, int salt, int bound) {
        return (int) Math.floorMod(hash(seed, a, b, salt), (long) bound);
    }

    /** Smooth 2D value noise (0..1), feature size {@code scale} blocks. */
    public static double noise(long seed, double x, double z, double scale, int salt) {
        double fx = x / scale, fz = z / scale;
        int ix = Mth.floor(fx), iz = Mth.floor(fz);
        double tx = smooth(fx - ix), tz = smooth(fz - iz);
        double a = unit(seed, ix, iz, salt), b = unit(seed, ix + 1, iz, salt);
        double c = unit(seed, ix, iz + 1, salt), d = unit(seed, ix + 1, iz + 1, salt);
        return Mth.lerp(tz, Mth.lerp(tx, a, b), Mth.lerp(tx, c, d));
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }
}
