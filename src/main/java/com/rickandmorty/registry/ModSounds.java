package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound events. For now sounds.json points them at vanilla sounds; drop real
 * .ogg files into assets/rickrooms/sounds/ and update sounds.json to replace them.
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RickAndMortyMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_GUN_FIRE = register("portal_gun.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_GUN_EMPTY = register("portal_gun.empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_GUN_RELOAD = register("portal_gun.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_OPEN = register("portal.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_TRAVEL = register("portal.travel");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_HUM = register("gateway.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHT_BUZZ = register("backrooms.light_buzz");
    /** Looping ambience of the Backrooms biome (referenced from its biome JSON). */
    public static final DeferredHolder<SoundEvent, SoundEvent> BACKROOMS_HUM = register("backrooms.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> BACTERIA_AMBIENT = register("bacteria.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BACTERIA_HURT = register("bacteria.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BACTERIA_DEATH = register("bacteria.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BACTERIA_STEP = register("bacteria.step");

    // --- Cronenberg World
    public static final DeferredHolder<SoundEvent, SoundEvent> CRONENBERG_AMBIENT = register("cronenberg.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRONENBERG_HURT = register("cronenberg.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRONENBERG_DEATH = register("cronenberg.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRONENBERG_STEP = register("cronenberg.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEHEMOTH_AMBIENT = register("behemoth.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_AMBIENT = register("mantis.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_HURT = register("mantis.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_DEATH = register("mantis.death");
    /** Biome ambience (referenced from the Cronenberg biome JSONs). */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRONENBERG_WIND = register("cronenberg.wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRONENBERG_HEARTBEAT = register("cronenberg.heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRONENBERG_DISTANT = register("cronenberg.distant");
    // --- the blender
    public static final DeferredHolder<SoundEvent, SoundEvent> BLENDER_GRAB = register("blender.grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLENDER_SAW = register("blender.saw");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLENDER_GORE = register("blender.gore");

    public static final DeferredHolder<SoundEvent, SoundEvent> WATCHER_WHISTLE = register("watcher.whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATCHER_WHISPER = register("watcher.whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> MONOLITH_HUM = register("monolith.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> POOLROOMS_DRIP = register("poolrooms.drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUISER_ENGINE = register("space_cruiser.engine");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(RickAndMortyMod.id(name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_AMBIENT = register("staff.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_CLOSED = register("staff.closed");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_HURT = register("staff.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_DEATH = register("staff.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORE_CHIME = register("store.chime");

    public static final DeferredHolder<SoundEvent, SoundEvent> STILL_LIFE_VOICE = register("still_life.voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> STILL_LIFE_WAKE = register("still_life.wake");
    public static final DeferredHolder<SoundEvent, SoundEvent> STILL_LIFE_HURT = register("still_life.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> STILL_LIFE_DEATH = register("still_life.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> STILL_LIFE_CLICK = register("still_life.click");

    public static final DeferredHolder<SoundEvent, SoundEvent> SHRINK_RAY_FIRE = register("shrink_ray.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> FREEZE_RAY_HIT = register("freeze_ray.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIME_CRYSTAL_HUM = register("time_crystal.hum");

    /** Each level's theme ("music.<level>"); the biomes play them (tools/generate_music.py). Music by Kevin MacLeod, CC BY 4.0. */
    public static final java.util.List<String> MUSIC = java.util.List.of("backrooms", "level_1", "level_4", "level_188", "the_harvest", "sunlit_lobby",
            "wooden_hotel", "scaffold_void", "glowstone_cavern", "poolrooms", "level_fun", "infinite_ikea", "dead_mall", "endless_field", "abandoned_city",
            "drop_limit", "the_facade", "the_staircase", "froopyland", "run_for_your_life", "cronenberg");

    static {
        for (String level : MUSIC) register("music." + level);
    }
}
