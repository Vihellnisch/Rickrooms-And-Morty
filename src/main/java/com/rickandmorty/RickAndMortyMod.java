package com.rickandmorty;

import com.rickandmorty.network.ModNetwork;
import com.rickandmorty.registry.ModArmorMaterials;
import com.rickandmorty.registry.ModAttachments;
import com.rickandmorty.registry.ModBlockEntities;
import com.rickandmorty.registry.ModBlocks;
import com.rickandmorty.registry.ModCreativeTabs;
import com.rickandmorty.registry.ModDataComponents;
import com.rickandmorty.registry.ModEffects;
import com.rickandmorty.registry.ModEntities;
import com.rickandmorty.registry.ModItems;
import com.rickandmorty.registry.ModSounds;
import com.rickandmorty.registry.ModStructures;
import com.rickandmorty.registry.ModWorldgen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(RickAndMortyMod.MOD_ID)
public class RickAndMortyMod {
    public static final String MOD_ID = "rickrooms";

    public RickAndMortyMod(IEventBus modBus, ModContainer container) {
        modBus.addListener(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class,
                event -> event.enqueueWork(com.rickandmorty.compat.Curios::setup));
        ModDataComponents.COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModWorldgen.CHUNK_GENERATORS.register(modBus);
        ModWorldgen.LOOT_MODIFIERS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModEffects.POTIONS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModStructures.STRUCTURE_TYPES.register(modBus);
        ModStructures.PIECES.register(modBus);
        ModArmorMaterials.MATERIALS.register(modBus);
        ModWorldgen.FEATURES.register(modBus);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModEntities::registerSpawnPlacements);
        modBus.addListener(ModCreativeTabs::addToVanillaTabs);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
