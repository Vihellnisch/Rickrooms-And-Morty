package com.rickandmorty.registry;

import com.rickandmorty.RickAndMortyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** What flesh plants can grow on. */
    public static final TagKey<Block> FLESH_GROUND = TagKey.create(Registries.BLOCK, RickAndMortyMod.id("flesh_ground"));
    /** Everything in Cronenberg World that a Cronenberg won't attack each other over. */
    public static final TagKey<EntityType<?>> CRONENBERGS = TagKey.create(Registries.ENTITY_TYPE, RickAndMortyMod.id("cronenbergs"));
    public static final TagKey<Biome> IS_CRONENBERG = TagKey.create(Registries.BIOME, RickAndMortyMod.id("is_cronenberg"));

    private ModTags() {
    }
}
