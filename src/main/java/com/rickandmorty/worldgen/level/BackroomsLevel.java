package com.rickandmorty.worldgen.level;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The levels of the Backrooms, each its own dimension. Level 0 is the original ("backrooms");
 * the rest are reached by pits, falls, holes in the wall and red exit doors, like in the lore.
 */
public enum BackroomsLevel implements StringRepresentable {
    LEVEL_0("backrooms", 1.0f),
    /** The Habitable Zone: an endless wet parking garage. */
    LEVEL_1("level_1", 1.2f),
    /** Abandoned offices, dangling cables, a dead department store. */
    LEVEL_4("level_4", 1.0f),
    /** The Courtyard of Windows. */
    LEVEL_188("level_188", 0.8f),
    /** An indoor cornfield under a grid of lights. The scarecrows move when you aren't looking. */
    HARVEST("the_harvest", 1.5f),
    /** Golden light through the windows. The one safe place, and the way out. */
    SUNLIT_LOBBY("sunlit_lobby", -1.0f),
    /** Shafts of wooden balconies around a drop into nothing. */
    WOODEN_HOTEL("wooden_hotel", 1.0f),
    /** A black void full of wooden walkways. */
    SCAFFOLD("scaffold_void", 1.5f),
    /** A cave the size of a country, with a sky of glowstone. */
    CAVERN("glowstone_cavern", 0.6f),
    /** Level 37: white tile and still water, forever, and nobody. */
    POOLROOMS("poolrooms", 0.5f),
    /** Level Fun =) : a party that never ended. Don't stay too long. */
    LEVEL_FUN("level_fun", 1.3f),
    /** The Infinite IKEA (SCP-3008): a store with no end and no way out, and staff who don't like customers after closing. */
    INFINITE_IKEA("infinite_ikea", 0.8f),
    /** The Dead Mall: two floors of empty shops round skylit courts, and the music stopped long ago. */
    DEAD_MALL("dead_mall", 0.6f),
    /** The Endless Field: grass and sky, forever. Your mind neither frays nor mends here. */
    ENDLESS_FIELD("endless_field", 0.0f),
    /** The Abandoned City: a whole city, long empty, and nature taking it back. Quiet. Too quiet. */
    ABANDONED_CITY("abandoned_city", 0.7f),
    /** Drop Limit: grass islands over a sea of cloud, roads that run off the edge, galleries hung in the sky. */
    DROP_LIMIT("drop_limit", 0.3f),
    /** The Facade: one endless wall of windows and air conditioners, fog above and below. */
    FACADE("the_facade", 1.2f),
    /** The Staircase: Escher's stairs, in the sky. */
    STAIRCASE("the_staircase", 0.5f),
    /** Froopyland: the soft, bright world Rick made for Beth. Your head clears here. */
    FROOPYLAND("froopyland", -0.6f),
    /** Run For Your Life (Level !): one red-lit hall, and something behind you. */
    RUN_FOR_YOUR_LIFE("run_for_your_life", 2.5f);

    public static final StringRepresentable.EnumCodec<BackroomsLevel> CODEC = StringRepresentable.fromEnum(BackroomsLevel::values);

    private final String id;
    /** Sanity drain multiplier; negative means you recover here. */
    public final float sanity;
    private final ResourceKey<Level> key;

    BackroomsLevel(String id, float sanity) {
        this.id = id;
        this.sanity = sanity;
        this.key = ResourceKey.create(Registries.DIMENSION, RickAndMortyMod.id(id));
    }

    public ResourceKey<Level> key() {
        return key;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    /** Where you end up if you fall out of the bottom of this level. */
    public BackroomsLevel fallTarget() {
        return switch (this) {
            case LEVEL_0 -> LEVEL_1;
            case WOODEN_HOTEL -> SCAFFOLD;
            case SCAFFOLD -> CAVERN;
            case STAIRCASE -> DROP_LIMIT;
            case DROP_LIMIT -> FACADE;
            default -> LEVEL_0;
        };
    }

    public static @Nullable BackroomsLevel of(ResourceKey<Level> dimension) {
        for (BackroomsLevel level : values()) {
            if (level.key.equals(dimension)) return level;
        }
        return null;
    }

    public static boolean isBackrooms(ResourceKey<Level> dimension) {
        return of(dimension) != null;
    }
}
