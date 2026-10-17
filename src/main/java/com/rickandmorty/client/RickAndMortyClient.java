package com.rickandmorty.client;

import com.rickandmorty.RickAndMortyMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only setup: adds a "Config" button for the mod on the Mods screen. */
@Mod(value = RickAndMortyMod.MOD_ID, dist = Dist.CLIENT)
public class RickAndMortyClient {
    public RickAndMortyClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
