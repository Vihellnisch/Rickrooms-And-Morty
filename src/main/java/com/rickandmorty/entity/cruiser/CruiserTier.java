package com.rickandmorty.entity.cruiser;

import com.rickandmorty.registry.ModItems;
import net.minecraft.world.item.Item;

/**
 * Upgrades for the Space Cruiser that come in marks, Mk I to Mk III. Fitting a better mark hands
 * back the one it replaces. The Laser Drill's Mk I is the plain Laser Drill upgrade.
 */
public enum CruiserTier {
    /** Microverse Booster: a bigger battery that charges faster, and flying and firing cost less. */
    BOOSTER("Booster"),
    /** Speed Thrusters: a higher top speed, quicker to get going and to climb. */
    THRUSTERS("Thrusters"),
    /** The Laser Drill: Mk I cuts one block, Mk II 3x3, Mk III 5x5 and through obsidian. */
    DRILL("Drill");

    public static final int MAX = 3;
    /** The save-data key. */
    public final String tag;

    CruiserTier(String tag) {
        this.tag = tag;
    }

    public Item item(int tier) {
        return switch (this) {
            case BOOSTER -> ModItems.CRUISER_BOOSTERS.get(tier - 1).get();
            case THRUSTERS -> ModItems.CRUISER_THRUSTERS.get(tier - 1).get();
            case DRILL -> tier == 1 ? ModItems.CRUISER_UPGRADES.get(CruiserUpgrade.LASER_DRILL).get() : ModItems.LASER_DRILLS.get(tier - 2).get();
        };
    }

    /** The item's translation key. */
    public String itemKey(int tier) {
        return switch (this) {
            case BOOSTER -> "item.rickrooms.cruiser_booster_mk" + tier;
            case THRUSTERS -> "item.rickrooms.cruiser_thrusters_mk" + tier;
            case DRILL -> tier == 1 ? "item.rickrooms.laser_drill_upgrade" : "item.rickrooms.laser_drill_mk" + tier;
        };
    }

    public static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "";
        };
    }

    // ------------------------------------------------------------------ what each mark does

    private static final int[] CAPACITY = {SpaceCruiserEntity.MAX_POWER, 9000, 12000, 18000};
    private static final float[] FLIGHT_DRAIN = {1.0f, 0.6f, 0.3f, 0.05f}, WEAPON_COST = {1.0f, 0.75f, 0.5f, 0.25f}, RECHARGE = {1.0f, 1.5f, 2.5f, 4.0f};
    private static final double[] TOP_SPEED = {1.1, 1.5, 2.0, 2.6}, ACCELERATION = {0.08, 0.108, 0.143, 0.185}, CLIMB = {0.06, 0.07, 0.085, 0.1};
    private static final int[] DRILL_SIZE = {1, 1, 3, 5};
    private static final float[] DRILL_HARDNESS = {25.0f, 25.0f, 25.0f, 55.0f};

    private static int clamp(int tier) {
        return Math.max(0, Math.min(MAX, tier));
    }

    public static int capacity(int booster) {
        return CAPACITY[clamp(booster)];
    }

    public static float flightDrain(int booster) {
        return FLIGHT_DRAIN[clamp(booster)];
    }

    public static float weaponCost(int booster) {
        return WEAPON_COST[clamp(booster)];
    }

    public static float recharge(int booster) {
        return RECHARGE[clamp(booster)];
    }

    public static double topSpeed(int thrusters) {
        return TOP_SPEED[clamp(thrusters)];
    }

    public static double acceleration(int thrusters) {
        return ACCELERATION[clamp(thrusters)];
    }

    public static double climb(int thrusters) {
        return CLIMB[clamp(thrusters)];
    }

    /** How wide a square the drill cuts. */
    public static int drillSize(int drill) {
        return DRILL_SIZE[clamp(drill)];
    }

    /** The hardest block the drill cuts (obsidian is 50). */
    public static float drillHardness(int drill) {
        return DRILL_HARDNESS[clamp(drill)];
    }
}
