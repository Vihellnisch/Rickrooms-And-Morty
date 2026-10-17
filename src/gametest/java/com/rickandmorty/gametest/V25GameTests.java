package com.rickandmorty.gametest;

import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.registry.ModSounds;
import com.rickandmorty.worldgen.level.BackroomsLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** v0.25: every level has its own theme. */
@GameTestHolder(RickAndMortyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class V25GameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void everyLevelHasItsTheme(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var sounds = helper.getLevel().registryAccess().registryOrThrow(Registries.SOUND_EVENT);
        for (String level : ModSounds.MUSIC) {
            helper.assertTrue(sounds.containsKey(RickAndMortyMod.id("music." + level)), "no sound event for " + level + "'s music");
        }
        for (BackroomsLevel level : BackroomsLevel.values()) {
            var biome = biomes.get(RickAndMortyMod.id(level.id()));
            helper.assertTrue(biome != null, "no biome " + level.id());
            var music = biome.getBackgroundMusic();
            helper.assertTrue(music.isPresent(), level + " has no music");
            var sound = music.get().getEvent().unwrapKey().orElseThrow().location();
            helper.assertValueEqual(sound, RickAndMortyMod.id("music." + level.id()), level + "'s music");
            helper.assertTrue(music.get().replaceCurrentMusic(), level + "'s theme should take over from whatever was playing");
        }
        helper.succeed();
    }
}
