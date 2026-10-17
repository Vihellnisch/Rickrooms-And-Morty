package com.rickandmorty;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Settings in config/rickrooms-common.toml (also editable from the Mods screen in-game).
 */
public final class ModConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue NOCLIP_ENABLED;
    public static final ModConfigSpec.DoubleValue NOCLIP_CHANCE;

    public static final ModConfigSpec.BooleanValue SANITY_ENABLED;
    public static final ModConfigSpec.DoubleValue SANITY_DRAIN;
    public static final ModConfigSpec.BooleanValue JUMPSCARES;
    public static final ModConfigSpec.BooleanValue STRANGE_EVENTS;

    public static final ModConfigSpec.BooleanValue MUTATION_ENABLED;
    public static final ModConfigSpec.DoubleValue MUTATION_RATE;

    public static final ModConfigSpec.DoubleValue GATEWAY_EXPLOSION;

    public static final ModConfigSpec.BooleanValue WATCHER_ENABLED;
    public static final ModConfigSpec.DoubleValue WATCHER_FREQUENCY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Noclipping: taking fall damage can (very rarely) drop you out of reality into the Backrooms.").push("noclip");
        NOCLIP_ENABLED = builder.comment("Allow noclipping into the Backrooms.")
                .define("enabled", true);
        NOCLIP_CHANCE = builder.comment("Chance for each fall that hurts you. 0.002 = 1 in 500.")
                .defineInRange("chance", 0.002, 0.0, 1.0);
        builder.pop();

        builder.comment("Sanity drains while you're in the Backrooms, and the lower it gets, the stranger things become.").push("sanity");
        SANITY_ENABLED = builder.comment("Enable the sanity meter.")
                .define("enabled", true);
        SANITY_DRAIN = builder.comment("How fast sanity drains. 1.0 = empty after about 10 minutes in a lit room.")
                .defineInRange("drainMultiplier", 1.0, 0.0, 10.0);
        JUMPSCARES = builder.comment("Allow jumpscares at very low sanity.")
                .define("jumpscares", true);
        STRANGE_EVENTS = builder.comment("Strange things that happen now and then whatever your sanity: footsteps behind you, knocking, lights going out.")
                .define("strangeEvents", true);
        builder.pop();

        builder.comment("Cronenberg World gets into you: mutation builds up while you're there.").push("cronenberg");
        MUTATION_ENABLED = builder.comment("Enable mutation.")
                .define("mutation", true);
        MUTATION_RATE = builder.comment("How fast the world itself mutates you. 1.0 = fully mutated after about 50 minutes (faster in its blood).")
                .defineInRange("mutationRate", 1.0, 0.0, 10.0);
        builder.pop();

        builder.comment("The Blender Upgrade.").push("blender");
        GATEWAY_EXPLOSION = builder.comment("How big the blast is when an evil Warp Gateway eats a player and melts down. 4 = TNT, 0 = no blast (it just vanishes).")
                .defineInRange("gatewayExplosion", 12.0, 0.0, 30.0);
        builder.pop();

        builder.comment("The Watcher: a harmless figure that watches you from far away, in any world.").push("watcher");
        WATCHER_ENABLED = builder.comment("Let the Watcher appear.")
                .define("enabled", true);
        WATCHER_FREQUENCY = builder.comment("How often it appears. 1.0 = in the Overworld, about once every 13 minutes; more often deep in the Backrooms.")
                .defineInRange("frequency", 1.0, 0.0, 10.0);
        builder.pop();

        SPEC = builder.build();
    }

    private ModConfig() {
    }
}
