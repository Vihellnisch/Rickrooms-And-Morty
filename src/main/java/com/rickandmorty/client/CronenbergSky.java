package com.rickandmorty.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Cronenberg World's sky: Overworld-like, with a sun and moon, but the air is thick with a
 * reddish haze (the biomes' fog colour), close in like the Nether's. Nights are very dark.
 */
public class CronenbergSky extends DimensionSpecialEffects {
    public CronenbergSky() {
        super(192.0f, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        float f = brightness * 0.85f + 0.08f;
        return fogColor.multiply(f, f * 0.9f, f * 0.9f);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }
}
