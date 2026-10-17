package com.rickandmorty.registry;

import com.mojang.serialization.MapCodec;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.cronenberg.RibcageFeature;
import com.rickandmorty.cronenberg.TumorMoundFeature;
import com.rickandmorty.loot.AddItemModifier;
import net.minecraft.world.level.levelgen.feature.Feature;
import com.rickandmorty.worldgen.BackroomsChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Codecs for the Backrooms chunk generator, Cronenberg World features, and the loot modifier that hides the journal pages. */
public class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, RickAndMortyMod.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RickAndMortyMod.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<BackroomsChunkGenerator>> BACKROOMS =
            CHUNK_GENERATORS.register("backrooms", () -> BackroomsChunkGenerator.CODEC);
    /** Every Backrooms level after Level 0 (which one is a field in the dimension JSON). */
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<com.rickandmorty.worldgen.level.LevelChunkGenerator>> BACKROOMS_LEVEL =
            CHUNK_GENERATORS.register("backrooms_level", () -> com.rickandmorty.worldgen.level.LevelChunkGenerator.CODEC);

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, RickAndMortyMod.MOD_ID);

    /** Cronenberg World features (configured and placed in data/rickrooms/worldgen). */
    public static final DeferredHolder<Feature<?>, RibcageFeature> RIBCAGE = FEATURES.register("ribcage", RibcageFeature::new);
    public static final DeferredHolder<Feature<?>, TumorMoundFeature> TUMOR_MOUND = FEATURES.register("tumor_mound", TumorMoundFeature::new);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AddItemModifier>> ADD_ITEM =
            LOOT_MODIFIERS.register("add_item", () -> AddItemModifier.CODEC);
}
